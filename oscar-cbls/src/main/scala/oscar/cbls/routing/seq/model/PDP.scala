package oscar.cbls.routing.seq.model

import oscar.cbls.constraints.core.ConstraintSystem
import oscar.cbls.constraints.lib.basic.{GE, EQ, LE}
import oscar.cbls.invariants.core.computation._
import oscar.cbls.invariants.lib.logic.{IntInt2Int, IntITE}
import oscar.cbls.invariants.lib.minmax.Max2
import oscar.cbls.invariants.lib.routing.{RouteSuccessorAndPredecessors, VehicleOfNodes}
import oscar.cbls.invariants.lib.seq.Precedence
import oscar.cbls.modeling.Algebra._
import oscar.cbls.objective.IntVarObjective

/**
  * Created by fabian on 04-07-16.
  */
class PDP(override val n:Int, override val v:Int, override val m:Store, maxPivotPerValuePercent:Int = 4)
  extends VRP(n,v,m,maxPivotPerValuePercent) with NextAndPrev{

  /**
    * The array that maintains pickup and delivery node.
    * Each value (representing a node) contains the load value of the node
    * (negative value for a delivery node, positive for a pickup node and 0 for a depot)
    * and the value of the related node.
    */
  private val pickupDeliveryNodes:Array[(Int,Int)] = new Array[(Int, Int)](n)
  for(i <- 0 until v)pickupDeliveryNodes(i) = (0,i)

  val pickupNodes:Array[Int] = new Array[Int]((n-v)/2)
  private val deliveryNodes:Array[Int] = new Array[Int]((n-v)/2)

  val vehicleOfNodes = VehicleOfNodes(routes,v)

  val fastConstraints = new ConstraintSystem(m)
  val slowConstraints = new ConstraintSystem(m)
  var precedenceObj:IntVarObjective = null


  /**
    * Add a new PickupAndDelivery couple and set this two basic constraints :
    * A strong constraint that specified that each pickup node has to be before his related delivery node
    * A strong constraint that specified that each couple of point (pickup and delivery) has to be in the same route.
    *
    * @param p the pickup point
    * @param d the delivery point
    */
  def addPickupDeliveryCouple(p:Int, d:Int, coupleNumber:Int, value:Int = 1): Unit ={
    pickupDeliveryNodes(p) = (value,d)
    pickupDeliveryNodes(d) = (-value,p)
    pickupNodes(coupleNumber) = p
    deliveryNodes(coupleNumber) = d
    fastConstraints.post(EQ(vehicleOfNodes(p),vehicleOfNodes(d)))
  }

  /**
    * Add defined couples.
    * The user has to specify two arrays. One containing the pickup points and
    * another one containing the related delivery points. So pickup(1) is the pickup point of delivery(1).
    *
    * @param pickups the pickup points array
    * @param deliveries the delivery points array
    */
  def addPickupDeliveryCouples(pickups:Array[Int], deliveries:Array[Int]): Unit ={
    assert(pickups.length == deliveries.length,
      "The pickup array and the delivery array must have the same length.")
    assert(!pickups.exists(_ >= n) || !deliveries.exists(_ >= n),
      "The pickup and the delivery array may only contain values between 0 and the number of nodes")
    assert(pickups.intersect(deliveries).length == 0,
      "One node can't be a pickup node and a delivery node at the same time")

    var precedenceList = List.tabulate((n-v)/2)(c => (pickups(c),deliveries(c)))
    precedenceObj = new IntVarObjective(Precedence(routes,precedenceList))
    for(i <- pickups.indices){
      addPickupDeliveryCouple(pickups(i),deliveries(i),i)
    }
  }

  /**
    * This method check if the given node is a pickup one. (if his load value is > 0 it's a pickup node)
    *
    * @param index the index of the node
    * @return
    */
  def isPickup(index:Int): Boolean = pickupDeliveryNodes(index)._1 > 0

  /**
    * This method check if the given node is a delivery one. (if his load value is < 0 it's a delivery node)
    *
    * @param index the index of the node
    * @return
    */
  def isDelivery(index:Int):Boolean = pickupDeliveryNodes(index)._1 < 0

  def getRelatedPickup(d:Int): Int ={
    require(pickupDeliveryNodes(d)._1 < 0,"This node isn't a delivery node")
    pickupDeliveryNodes(d)._2
  }

  def getRelatedDelivery(p:Int): Int ={
    require(pickupDeliveryNodes(p)._1 > 0,"This node isn't a pickup node")
    pickupDeliveryNodes(p)._2
  }

  /**
    * @param n the index of a node
    * @return the load value of the node
    */
  def deltaAtNode(n:Int): Int ={
    pickupDeliveryNodes(n)._1
  }

  def getPickups: Iterable[Int] = pickupNodes

  def getDeliverys: Iterable[Int] = deliveryNodes

  def getRoutedPickups: Iterable[Int] = pickupNodes.filter(isRouted(_))

  def getRoutedDeliverys: Iterable[Int] = deliveryNodes.filter(isRouted(_))

  def getRoutedPickupsPredecessors: Iterable[Int] = getRoutedPickups.map(prev(_).value)

  def getRoutedDeliverysPredecessors: Iterable[Int] = getRoutedDeliverys.map(prev(_).value)

  def getUnroutedPickups: Iterable[Int] = pickupNodes.filter(!isRouted(_))

  def getUnroutedDeliverys: Iterable[Int] = deliveryNodes.filter(!isRouted(_))

  def getNodesBeforeRelatedDelivery()(node:Int):Iterable[Int] = {
    assert(isPickup(node), "The referenced node must be a pickup one !")
    getNodesBeforePosition()(getRelatedDelivery(node))
  }

  def getNodesAfterRelatedPickup()(node: Int): Iterable[Int] = {
    assert(isDelivery(node), "The referenced node must be a delivery one !")
    getNodesAfterPosition()(getRelatedPickup(node))
  }

  /**
    * Still Usefull ???
    *
    * This method search all the complete segments contained in a specified route.
    * A segment is considered as complete when you can move it to another branch
    * without breaking the precedence constraint.
    * It runs through the specified route and try to create the smallest complete segments possible
    * After that it try to combine adjacent segment
    *
    * @param routeNumber the number of the route
    * @return the list of all the complete segment present in the route
    */
  def getCompleteSegments(routeNumber:Int): List[(Int,Int)] ={
    val route = getRouteOfVehicle(routeNumber)
    /**
      * Each value of segmentsArray represent a possible complete segment.
      * The Int value represents the amount of pickup nodes whose related delivery node isn't currently in the segment
      * The List[Int] value represents the segment
      */
    val segmentsArray:Array[(Int,List[Int])] = new Array[(Int, List[Int])](route.size)

    for(i <- route.indices){
      if(isPickup(route(i))){
        //If the node is a pickup one, we add the node to all the active segment and we create a new one
        for(j <- segmentsArray.indices)
          if(segmentsArray(j) != null)
            if (segmentsArray(j)._1 != 0)
              segmentsArray(j) = (segmentsArray(j)._1 + 1, segmentsArray(j)._2 :+ route(i))
        segmentsArray(i) = (1,route(i)::Nil)
      }
      else{
        for(j <- segmentsArray.indices)
          if(segmentsArray(j) != null)
            if (segmentsArray(j)._1 != 0) {
              /**
                * If the segment doesn't contain the related pickup node it means that the related pickup node is before
                * the beginning of the segment and thus this is not possible to create a complete segment beginning
                * at this position.
                */
              if (!segmentsArray(j)._2.contains(getRelatedPickup(route(i))))
                segmentsArray(j) = null
              /**
                * Else we decrement the number of single pickup
                */
              else
                segmentsArray(j) = (segmentsArray(j)._1 - 1, segmentsArray(j)._2 :+ route(i))
            }
      }
    }

    var completeSegments: List[(Int, Int)] = Nil

    /**
      * We loop only on the segment that are not null and whose the number of single pickup is equals to 0
      */
    for(i <- segmentsArray.indices)if(segmentsArray(i) != null && segmentsArray(i)._1 == 0){
      val currentSegment = segmentsArray(i)._2
      completeSegments = (currentSegment.head, currentSegment.last) :: completeSegments
      var j = i-1
      var currentPreds = prev.element(route(i)).value
      while(j != -1){
        if(segmentsArray(j) != null && currentPreds == segmentsArray(j)._2.last){
          completeSegments = (segmentsArray(j)._2.head, currentSegment.last) :: completeSegments
          currentPreds = prev.element(route(j)).value
        }
        j -= 1
      }
    }
    completeSegments
  }


  def getUnCompleteSegments(routeNumber:Int): List[List[Int]] ={
    var unCompleteSegmentsList:List[List[Int]] = Nil
    var currentList:List[Int] = Nil
    val route = getRouteOfVehicle(routeNumber)
    for(node <- route){
      if(isPickup(node))
        currentList = node :: currentList
      else if(isDelivery(node)){
        if(!currentList.contains(getRelatedPickup(node)))
          currentList = node :: currentList
        else{
          val pos = currentList.indexOf(getRelatedPickup(node))
          unCompleteSegmentsList = currentList.reverse :: unCompleteSegmentsList
          currentList = node :: currentList.dropRight(currentList.size-pos)
        }
      }
    }
    currentList.reverse :: unCompleteSegmentsList
  }


  //---- Vehicle capacity ----//

  val defaultArrivalLoadValue = 0

  //We create a array of size v+1 so when we initiate constraints the max capacity for unrouted node is null.
  //Maybe not the best solution but it works.
  val vehicleMaxCapacity:Array[Int] = Array.tabulate(v+1) ((vehicle:Int) => 0)

  val arrivalLoadValue = Array.tabulate(n){ (i:Int) => CBLSIntVar(m, 0, FullRange, "Arrival load at node " + i)}

  var leaveLoadValue:Array[IntValue] = null

  def setArrivalLeaveLoadValue(): Unit ={
    leaveLoadValue = Array.tabulate(n+1) {
      (i :Int) =>
        if(i == n || i < v)
          defaultArrivalLoadValue
        else
          arrivalLoadValue(i) + deltaAtNode(i)
    }
    for(i <- 0 until n){
      arrivalLoadValue(i) <== leaveLoadValue.element(prev(i))
    }
  }

  def setVehiclesMaxCargo(max:Int): Unit ={
    for(i <- 0 until v)
      vehicleMaxCapacity(i) = max
  }

  def setVehiclesCapacityStrongConstraint(): Unit ={
    for(i <- arrivalLoadValue.indices)
      slowConstraints.post(LE(arrivalLoadValue(i), vehicleMaxCapacity.element(vehicleOfNodes(i))))
  }

  //---- Time Windows ----//

  var defaultArrivalTime:CBLSIntConst = null

  var arrivalTime:Array[CBLSIntVar] = null

  var leaveTime:Array[CBLSIntVar] = null

  var travelOutDuration:Array[CBLSIntVar] = null

  var arrivalTimeToNext:Array[IntValue] = null

  var travelDurationMatrix: TravelTimeFunction = null

  def initiateTimeWindowInvariants(): Unit ={
    defaultArrivalTime = new CBLSIntConst(0)

    arrivalTime = Array.tabulate(n) {
      (i: Int) => CBLSIntVar(m, 0, 0 to Int.MaxValue / n, "arrivalTimeAtNode" + i)
    }
    leaveTime = Array.tabulate(n) {
      (i: Int) => CBLSIntVar(m, 0, 0 to Int.MaxValue / n, "leaveTimeAtNode" + i)
    }
    travelOutDuration = Array.tabulate(n) {
      (i: Int) => CBLSIntVar(m, 0, 0 to Int.MaxValue / n, "travelDurationToLeave" + i)
    }
    arrivalTimeToNext = Array.tabulate(n + 1) {
      (i: Int) =>
        if (i == n) defaultArrivalTime
        else travelOutDuration(i) + leaveTime(i)
    }

    for (i <- 0 until n) {
      arrivalTime(i) <== arrivalTimeToNext.element(prev(i))
    }
  }

  def addTimeWidowStringInfo() {
    addToStringInfo(() => "arrivalTime:      " + arrivalTime.toList.mkString(","))
    addToStringInfo(() => "leaveTime:        " + leaveTime.toList.mkString(","))
    addToStringInfo(() => "travelOutDuration:" + travelOutDuration.toList.mkString(","))
    addToStringInfo(() => "arrivalTimeToNext:" + arrivalTimeToNext.toList.mkString(","))
  }

  def setEndWindow(node: Int, endWindow: Int) {
    require(node >= v, "only for specifying time windows on nodes, not on vehicles")
    slowConstraints.post(LE(IntITE(next(node), 0, leaveTime(node), n - 1), endWindow).nameConstraint("end of time window on node " + node))
  }

  def setVehicleEnd(vehicle: Int, endTime: Int) {
    require(vehicle < v, "only for specifying end time of vehicles")
    slowConstraints.post(LE(arrivalTime(vehicle), endTime).nameConstraint("end of time for vehicle " + vehicle))
  }


  def setNodeDuration(node: Int, duration: IntValue) {
    assert(node >= v)
    leaveTime(node) <== arrivalTime(node) + duration
  }

  def setNodeDuration(node: Int, durationWithoutWait: IntValue, startWindow: Int) {
    leaveTime(node) <== Max2(arrivalTime(node), startWindow) + durationWithoutWait
  }

  def setNodeDuration(node: Int, durationWithoutWait: IntValue, startWindow: Int, maxWaiting: Int) {
    setNodeDuration(node, durationWithoutWait, startWindow)
    slowConstraints.post(GE(arrivalTime(node), startWindow - maxWaiting).nameConstraint("end of time window on node (with duration)" + node))
  }

  def setTravelTimeFunctions(travelCosts: TravelTimeFunction) {
    travelDurationMatrix = travelCosts
    for (i <- 0 until n) {
      travelOutDuration(i) <== new IntInt2Int(leaveTime(i), next(i),
        (leaveTime, successor) =>
          if (successor == n) 0
          else travelCosts.getTravelDuration(i, leaveTime, successor))
    }
  }

  def setTimeWindows(timeWindows: Array[(Int,(Int,Int))]): Unit ={
    initiateTimeWindowInvariants()
    addTimeWidowStringInfo()

    for(i <- timeWindows.indices if i >= v) {
      setEndWindow(i,timeWindows(i)._1)
      setNodeDuration(i,timeWindows(i)._2._1, timeWindows(i)._2._2)
    }
  }

  def setMaxTravelDistancePDConstraint(){
    for(p <- getPickups) {
      val d = getRelatedDelivery(p)
      slowConstraints.post(LE(arrivalTime(d) - leaveTime(p), 2*travelDurationMatrix.getTravelDuration(p, leaveTime(p).value, d)))
    }
  }
}
