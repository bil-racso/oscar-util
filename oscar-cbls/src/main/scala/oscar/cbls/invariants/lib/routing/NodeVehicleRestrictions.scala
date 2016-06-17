package oscar.cbls.invariants.lib.routing

/*******************************************************************************
  * OscaR is free software: you can redistribute it and/or modify
  * it under the terms of the GNU Lesser General Public License as published by
  * the Free Software Foundation, either version 2.1 of the License, or
  * (at your option) any later version.
  *
  * OscaR is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU Lesser General Public License  for more details.
  *
  * You should have received a copy of the GNU Lesser General Public License along with OscaR.
  * If not, see http://www.gnu.org/licenses/lgpl-3.0.en.html
  ******************************************************************************/

import oscar.cbls.invariants.core.algo.quick.QList
import oscar.cbls.invariants.core.algo.seq.functional.IntSequence
import oscar.cbls.invariants.core.computation._
import oscar.cbls.invariants.core.propagation.Checker
import oscar.cbls.invariants.lib.logic.Filter
import oscar.cbls.invariants.lib.set.BelongsTo

import scala.collection.immutable.SortedSet

object NodeVehicleRestrictions{
  def apply(routes:ChangingSeqValue,v:Int, nodeVehicleRestrictions:Iterable[(Int,Int)]):Array[CBLSIntVar] = {
    val violationPerVehicle =  Array.tabulate(v)(vehicle => CBLSIntVar(routes.model,name="violation_of_nodeRoute restriction_vehicle:" + vehicle))

    new NodeVehicleRestrictions(routes, v, nodeVehicleRestrictions, violationPerVehicle)

    violationPerVehicle
  }

  def violatedNodes(routes:ChangingSeqValue,v:Int,nodeVehicleRestrictions:Iterable[(Int,Int)]):SetValue = {
    violatedNodes(VehicleOfNodes(routes,v).asInstanceOf[Array[IntValue]],v,nodeVehicleRestrictions)
  }

  def violatedNodes(vehicleOfNode:Array[IntValue],v:Int,nodeVehicleRestrictions:Iterable[(Int,Int)]):SetValue = {

    val n = vehicleOfNode.length

    val noForbidden = SortedSet.empty[Int]
    val forbiddenVehicleForNodes: Array[SortedSet[Int]] = Array.fill(n)(null)

    for ((node, vehicle) <- nodeVehicleRestrictions) {
      if(forbiddenVehicleForNodes(node) == null){
        forbiddenVehicleForNodes(node) = noForbidden + vehicle
      }else{
        forbiddenVehicleForNodes(node) = forbiddenVehicleForNodes(node) + vehicle
      }
    }

    Filter(Array.tabulate(n)(node => {
      val forbiddenVehicleForNode = forbiddenVehicleForNodes(node)
      if(forbiddenVehicleForNode == null) 0
      else BelongsTo(vehicleOfNode(node),forbiddenVehicleForNode)}
    ))
  }
}

/**
 * this invariant maintains a degree of violation for route restriction constraints.
 * there is a set of restrictions node<->vehicle,
 * the invariant maintains, for each vehicle, the number of node
 * that it reaches although it should not, according to the mentioned restrictions.
 * @param routes
 * @param v
 * @param nodeVehicleRestrictions the restrictions that we are monitoring
 * @param violationPerVehicle an array of v non-controlled var that the invariant will control
 *                            and maintain to the degree of violation of each vehicle
 */
class NodeVehicleRestrictions(routes:ChangingSeqValue,
                              v:Int,
                              nodeVehicleRestrictions:Iterable[(Int,Int)],
                              violationPerVehicle:Array[CBLSIntVar])
  extends Invariant() with SeqNotificationTarget {

  val n = routes.maxValue+1
  val vehicles = 0 until v

  registerStaticAndDynamicDependency(routes)
  finishInitialization()
  for(v <- violationPerVehicle) v.setDefiningInvariant(this)

  private val nodeToVehiclesRestriction : Array[(Array[Boolean],QList[Int])] = Array.fill(n)(null)

  for ((node, vehicle) <- nodeVehicleRestrictions) {
    nodeToVehiclesRestriction(node) match{
      case null =>
        val restrictionArray = Array.fill(v)(false)
        restrictionArray(vehicle) = true
        nodeToVehiclesRestriction(node) = (restrictionArray,QList(vehicle))
      case (restrictionArray,restrictionList) =>
        if(!restrictionArray(vehicle)){
          restrictionArray(vehicle) = true
          nodeToVehiclesRestriction(node) = (restrictionArray,QList(vehicle,restrictionList))
        }
    }
  }

  def anyRestrictionForNode(node:Int):Boolean = {
    nodeToVehiclesRestriction(node) != null
  }

  def isAllowed(node : Int, vehicle : Int) = {
    val nodeRestrictions = nodeToVehiclesRestriction(node)
    nodeRestrictions == null || !nodeRestrictions._1(vehicle)
  }

  def isForbidden(node : Int, vehicle : Int) = {
    val nodeRestrictions =nodeToVehiclesRestriction(node)
    nodeRestrictions != null && nodeRestrictions._1(vehicle)
  }

  def forbiddenVehicles(node:Int):Iterable[Int] = {
    val nodeRestrictions =nodeToVehiclesRestriction(node)
    if(nodeRestrictions == null) None
    else nodeRestrictions._2
  }

  //il faut mettre à jour les pré-calculs par véhicules, et uniquement lorsqu'on fait au moins une requête sur un véhicule à partir du checkpoint pour un mouvement qui a besoin de cette struture (3-opt)
  var checkpoint : IntSequence = null
  var violationAtCheckpoint:Array[Int] = Array.fill(v)(-1)

  //TODO: improve complexity of the pre-computation!
  //node n => vehicle v => number of node from start of vehicle reaching n that cannot be reached by vehicle v
  val precomputationAtCheckpoint:Array[Array[Int]] = Array.tabulate(n)(_=>Array.fill(v)(0))
  val vehicleChangedSinceCheckpoint:Array[Boolean] = Array.fill(v)(true)
  var changedVehicleSinceCheckpoint:QList[Int] = vehicles.foldLeft[QList[Int]](null)((acc,v) => QList(v,acc))

  def setVehicleChangedSinceCheckpoint(vehicle:Int) {
    if(!vehicleChangedSinceCheckpoint(vehicle)){
      vehicleChangedSinceCheckpoint(vehicle) = true
      changedVehicleSinceCheckpoint = QList(vehicle,changedVehicleSinceCheckpoint)
    }
  }

  def setAllVehicleChangedSinceCheckpoint(){
    for (vehicle <- vehicles) {
      setVehicleChangedSinceCheckpoint(vehicle)
    }
  }

  def updateAllInvalidPrecomputationsToCheckpointAndSaveCheckpoint(){
    while(changedVehicleSinceCheckpoint != null){
      val vehicle = changedVehicleSinceCheckpoint.head
      changedVehicleSinceCheckpoint = changedVehicleSinceCheckpoint.tail
      violationAtCheckpoint(vehicle) = doUpdatePrecomputationToCheckpointAndSaveCheckpoint(vehicle)
    }
  }

  def doUpdateAllPrecomputationsToCheckpointAndSaveCheckpoint(){
    for (vehicle <- vehicles) {
      violationAtCheckpoint(vehicle) = doUpdatePrecomputationToCheckpointAndSaveCheckpoint(vehicle)
    }
    changedVehicleSinceCheckpoint = null
  }

  def doUpdatePrecomputationToCheckpointAndSaveCheckpoint(vehicle:Int):Int = {
    vehicleChangedSinceCheckpoint(vehicle) = false
    val explorerAtVehicleStart = checkpoint.explorerAtAnyOccurrence(vehicle).head
    var restrictionsForPrev = precomputationAtCheckpoint(explorerAtVehicleStart.value)
    var explorerAtCurrentNode = explorerAtVehicleStart.next
    while(explorerAtCurrentNode match{
      case None => return restrictionsForPrev(vehicle); false
      case Some(position) =>
        val node = position.value
        if(node < v){
          //we are at a vehicle
          return restrictionsForPrev(vehicle)
          false
        }else {
          val restrictionsForCurrent = precomputationAtCheckpoint(node)
          for (v <- vehicles) {
            restrictionsForCurrent(v) = restrictionsForPrev(v)
          }

          for (forbiddenVehicle <- forbiddenVehicles(node)) {
            restrictionsForCurrent(forbiddenVehicle) += 1
          }
          restrictionsForPrev = restrictionsForCurrent
          explorerAtCurrentNode = position.next
          true
        }
    }){}
    throw new Error("unexpected end")
  }

  def violationOnSegmentFromPrecomputation(fromValue:Int,toValue:Int,vehicle:Int):Int = {
    require(!vehicleChangedSinceCheckpoint(vehicle))
    val toReturn = if(fromValue == toValue){
      if(isForbidden(toValue,vehicle)) 1 else 0
    }else{
      val nbRejectionFrom = precomputationAtCheckpoint(fromValue)(vehicle)
      val nbRejectionTo = precomputationAtCheckpoint(toValue)(vehicle)
      nbRejectionTo - nbRejectionFrom + (if(isForbidden(fromValue,vehicle)) 1 else 0)
    }
    toReturn
  }

  def violationOnNodesFromScratch(nodes:Iterable[Int],vehicle:Int):Int = {
    var toReturn = 0
    val it = nodes.iterator
    while(it.hasNext){
      val node = it.next()
      if(isForbidden(node,vehicle)) toReturn += 1
    }
    toReturn
  }

  def violationOnSegment(seqWithSegment:IntSequence, fromValue:Int, toValue:Int, vehicle:Int):Int = {
    if(vehicleChangedSinceCheckpoint(vehicle)){
      violationOnNodesFromScratch(
        seqWithSegment.valuesBetweenPositions(
          seqWithSegment.positionOfAnyOccurrence(fromValue).head,
          seqWithSegment.positionOfAnyOccurrence(toValue).head),vehicle)
    }else{
      val toReturn = violationOnSegmentFromPrecomputation(fromValue,toValue,vehicle)
      assert(toReturn == violationOnNodesFromScratch(
        seqWithSegment.valuesBetweenPositions(
          seqWithSegment.positionOfAnyOccurrence(fromValue).head,
          seqWithSegment.positionOfAnyOccurrence(toValue).head),vehicle))
      toReturn
    }
  }

  def violationOnVehicle(vehicle:Int,seq:IntSequence):Int = {
    if(vehicleChangedSinceCheckpoint(vehicle)){
      violationOnNodesFromScratch(
        seq.valuesBetweenPositions(
          seq.positionOfAnyOccurrence(vehicle).head,
          if(vehicle == v-1) seq.size-1 else seq.positionOfAnyOccurrence(vehicle+1).head),
        vehicle)
    }else{
      val toReturn = violationOnSegmentFromPrecomputation(vehicle,if(vehicle == v-1) seq.last else seq.predecessorPos2Val(seq.positionOfAnyOccurrence(vehicle+1).head).head,vehicle)
      assert(toReturn == violationOnNodesFromScratch(
        seq.valuesBetweenPositions(
          seq.positionOfAnyOccurrence(vehicle).head,
          if(vehicle == v-1) seq.size-1 else seq.positionOfAnyOccurrence(vehicle+1).head),
        vehicle))
      toReturn
    }
  }

  override def notifySeqChanges(v : ChangingSeqValue, d : Int, changes : SeqUpdate) {
    if (!digestUpdates(changes, false)) {
      setAllVehicleChangedSinceCheckpoint()
      for (vehicle <- vehicles) {
        violationPerVehicle(vehicle) := violationOnVehicle(vehicle:Int,changes.newValue)
      }
    }
  }

  private def digestUpdates(changes : SeqUpdate, skipNewCheckpoints : Boolean) : Boolean = {
    changes match {
      case SeqUpdateDefineCheckpoint(prev : SeqUpdate, isActive : Boolean) =>
        if(skipNewCheckpoints) {
          digestUpdates(prev, true)
        }else{
          if (!digestUpdates(prev, true)) {
            this.checkpoint = prev.newValue
            doUpdateAllPrecomputationsToCheckpointAndSaveCheckpoint()
            for(vehicle <- vehicles) violationPerVehicle(vehicle) := violationOnVehicle(vehicle,checkpoint)
          }else {
            this.checkpoint = prev.newValue
            updateAllInvalidPrecomputationsToCheckpointAndSaveCheckpoint()
          }
          true
        }
      case SeqUpdateRollBackToCheckpoint(checkpoint : IntSequence) =>
        require(checkpoint quickEquals this.checkpoint)
        while(changedVehicleSinceCheckpoint!=null){
          val vehicle = changedVehicleSinceCheckpoint.head
          changedVehicleSinceCheckpoint = changedVehicleSinceCheckpoint.tail
          violationPerVehicle(vehicle) := violationAtCheckpoint(vehicle)
          vehicleChangedSinceCheckpoint(vehicle) = false
        }

        true
      case SeqUpdateInsert(value : Int, pos : Int, prev : SeqUpdate) =>
        //on which vehicle did we insert?
        if (!digestUpdates(prev, skipNewCheckpoints)) return false

        val vehicleOfInsert = RoutingConventionMethods.searchVehicleReachingPosition(pos, prev.newValue, v)
        if (isForbidden(value, vehicleOfInsert)) {
          violationPerVehicle(vehicleOfInsert) :+= 1
        }
        setVehicleChangedSinceCheckpoint(vehicleOfInsert)

        true
      case x@SeqUpdateMove(fromIncluded : Int, toIncluded : Int, after : Int, flip : Boolean, prev : SeqUpdate) =>
        //on which vehicle did we move?
        //also from --> to cannot include a vehicle start.
        if (!digestUpdates(prev, skipNewCheckpoints)) false
        else if (x.isNop) true
        else if (x.isSimpleFlip) {
          //this is a simple flip, no change on vehicle violation, since obeys routing rule!
          val vehicleOfMovedSegment = RoutingConventionMethods.searchVehicleReachingPosition(fromIncluded, prev.newValue, v)
          setVehicleChangedSinceCheckpoint(vehicleOfMovedSegment)
          true
        } else if(fromIncluded == toIncluded) {
          //per vehicle, there might be some node cost to consider
          val vehicleOfMovedSegment = RoutingConventionMethods.searchVehicleReachingPosition(fromIncluded, prev.newValue, v)
          val targetVehicleOfMove = RoutingConventionMethods.searchVehicleReachingPosition(after, prev.newValue, v)

          if (vehicleOfMovedSegment == targetVehicleOfMove) {
            //moving within the vehicle; same as flip, actually
            setVehicleChangedSinceCheckpoint(vehicleOfMovedSegment)
          }else {
            //moving accross vehicles

            val value = x.fromValue

            if(isForbidden(value,vehicleOfMovedSegment)){
              violationPerVehicle(vehicleOfMovedSegment) :-= 1
            }
            if(isForbidden(value,targetVehicleOfMove)){
              violationPerVehicle(targetVehicleOfMove) :+= 1
            }

            if(anyRestrictionForNode(value)) {
              setVehicleChangedSinceCheckpoint(vehicleOfMovedSegment)
            }
            //TODO: we can actually update this in O(1) if there is no restriction on the moved node instead of invalidating it
            setVehicleChangedSinceCheckpoint(targetVehicleOfMove)
          }
          true
        }else {
          //per vehicle, there might be some node cost to consider
          val vehicleOfMovedSegment = RoutingConventionMethods.searchVehicleReachingPosition(fromIncluded, prev.newValue, v)
          val targetVehicleOfMove = RoutingConventionMethods.searchVehicleReachingPosition(after, prev.newValue, v)
          assert(vehicleOfMovedSegment == RoutingConventionMethods.searchVehicleReachingPosition(toIncluded, prev.newValue, v))

          if (vehicleOfMovedSegment == targetVehicleOfMove) {
            //moving within the vehicle; same as flip, actually
            setVehicleChangedSinceCheckpoint(vehicleOfMovedSegment)
          }else {
            //moving accross vehicles

            val fromValue = x.fromValue
            val toValue = x.toValue

            if(violationPerVehicle(vehicleOfMovedSegment).newValue != 0){
              //check if we decrease the violation on that vehicle
              violationPerVehicle(vehicleOfMovedSegment) :-= violationOnSegment(prev.newValue, fromValue, toValue, vehicleOfMovedSegment)
            }//else we do not introduce a violation on that vehicle by removing some nodes, right?

            violationPerVehicle(targetVehicleOfMove) :+= violationOnSegment(prev.newValue, fromValue, toValue, targetVehicleOfMove)

            setVehicleChangedSinceCheckpoint(vehicleOfMovedSegment)
            setVehicleChangedSinceCheckpoint(targetVehicleOfMove)
          }
          true
        }

      case x@SeqUpdateRemove(position : Int, prev : SeqUpdate) =>
        //on which vehicle did we remove?
        if (!digestUpdates(prev, skipNewCheckpoints)) return false
        val removedNode = x.removedValue
        val vehicleOfMovedSegment = RoutingConventionMethods.searchVehicleReachingPosition(position, prev.newValue, v)
        if(isForbidden(removedNode,vehicleOfMovedSegment)){
          violationPerVehicle(vehicleOfMovedSegment) :-= 1
        }
        if(anyRestrictionForNode(removedNode)) {
          setVehicleChangedSinceCheckpoint(vehicleOfMovedSegment)
        }
        true

      case SeqUpdateLastNotified(value : IntSequence) =>
        true //we are starting from the previous value
      case SeqUpdateSet(value : IntSequence) =>
        false //impossible to go incremental
    }
  }

  override def checkInternals(c : Checker) : Unit = {

    val seq = routes.value
    for(vehicle <- vehicles){
      c.check(violationPerVehicle(vehicle).value == violationOnVehicle(vehicle,seq),
        Some("violationPerVehicle(vehicle).value=" + violationPerVehicle(vehicle).value
          + " should == violationOnVehicle(vehicle,seq)="+  violationOnVehicle(vehicle,seq) + " vehicle:" + vehicle + " route:" + seq))

      val oldUpToDate = vehicleChangedSinceCheckpoint(vehicle)
      vehicleChangedSinceCheckpoint(vehicle) = true
      c.check(violationPerVehicle(vehicle).value == violationOnVehicle(vehicle,seq))
      vehicleChangedSinceCheckpoint(vehicle) = oldUpToDate
    }

    if(checkpoint != null){
      for(vehicle <- vehicles){
        val oldUpToDate = vehicleChangedSinceCheckpoint(vehicle)
        vehicleChangedSinceCheckpoint(vehicle) = true
        c.check(violationAtCheckpoint(vehicle) == violationOnVehicle(vehicle,checkpoint))
        vehicleChangedSinceCheckpoint(vehicle) = oldUpToDate
      }
    }
  }
}

