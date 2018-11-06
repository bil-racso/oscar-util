package oscar.cbls.business.seqScheduling.invariants

import oscar.cbls._
import oscar.cbls.core._
import oscar.cbls.algo.seq.IntSequence
import oscar.cbls.business.seqScheduling.model._

object StartTimesActivities {
  def apply(priorityActivitiesList: ChangingSeqValue,
            schedulingModel: SchedulingModel): (CBLSIntVar, Array[CBLSIntVar], SetupTimes) = {
    val model = priorityActivitiesList.model
    val makeSpan = CBLSIntVar(model, 0, name="Schedule Makespan")
    val startTimes: Array[CBLSIntVar] = Array.tabulate(schedulingModel.nbActivities)(node =>  CBLSIntVar(model, 0, name=s"Start Time of Activity $node"))
    val setupTimes = new SetupTimes

    new StartTimesActivities(priorityActivitiesList, schedulingModel, makeSpan, startTimes, setupTimes)

    (makeSpan, startTimes, setupTimes)
  }
}

class StartTimesActivities(priorityActivitiesList: ChangingSeqValue,
                           schedulingModel: SchedulingModel,
                           makeSpan: CBLSIntVar,
                           startTimes: Array[CBLSIntVar],
                           setupTimes: SetupTimes
                          ) extends Invariant with SeqNotificationTarget {

  // Invariant Initialization
  registerStaticAndDynamicDependency(priorityActivitiesList)

  finishInitialization()

  for {st <- startTimes} st.setDefiningInvariant(this)
  makeSpan.setDefiningInvariant(this)

  computeAllFromScratch(priorityActivitiesList.value)

  def computeAllFromScratch(priorityList: IntSequence): Unit = {

    println(s"*** Computing all from scratch for $priorityList")

    // Initialization
    makeSpan := 0
    //startTimes.foreach { stVar => stVar := 0 }
    setupTimes.reset()
    val resourceFlowStates: Array[ResourceFlowState] = Array.tabulate(schedulingModel.nbResources) { i =>
      new ResourceFlowState(schedulingModel.resources(i).initialModeIndex,
                            schedulingModel.resources(i).capacity)
    }
    var makeSpanValue = 0
    var startTimesArray: Array[Int] = Array.tabulate(schedulingModel.nbActivities)(_ => 0)
    // Main loop
    for {indAct <- priorityList} {
      val resIndexesActI = schedulingModel.activities(indAct).resourceUsages.flatten.map(_.resourceIndex)
      val precedencesActI = schedulingModel.precedences.precArray(indAct)
      // compute maximum of start+duration for preceding activities
      val maxEndTimePrecsActI = if (precedencesActI.isEmpty) 0
        else precedencesActI.map { precInd =>
          startTimes(precInd).value + schedulingModel.activities(precInd).duration
        }.max
      // compute maximum of starting times for availability of resources
      var maxStartTimeAvailableResActI = 0
      for {resInd <- resIndexesActI} {
        // update last activity index
        resourceFlowStates(resInd).lastActivityIndex = indAct
        val lastModeRes = resourceFlowStates(resInd).lastRunningMode
        val newModeRes = schedulingModel.activities(indAct).resourceUsages(resInd).get.runningMode.index
        val lastResFlows = resourceFlowStates(resInd).forwardFlows
        val resourceQty = schedulingModel.activities(indAct).resourceUsages(resInd).get.capacity
        if (lastModeRes == newModeRes) {
          // running mode for resource in activity has not changed
          maxStartTimeAvailableResActI = math.max(maxStartTimeAvailableResActI,
            ResourceFlow.getEndTimeResourceFlows(resourceQty, lastResFlows))
        } else {
          // running mode for resource in activity changed
          resourceFlowStates(resInd).changedRunningMode = Some(lastModeRes)
          resourceFlowStates(resInd).lastRunningMode = newModeRes
          val lastEndTime = ResourceFlow.getLastEndTimeResourceFlow(lastResFlows)
          val setupTimeMode = schedulingModel.resources(resInd)
                                             .setupTimeModes(lastModeRes)(newModeRes)
                                             .get
          maxStartTimeAvailableResActI = math.max(maxStartTimeAvailableResActI,
            lastEndTime + setupTimeMode)
          // a new setup activity must be added
          setupTimes.addSetupTime(SetupTimeData(resInd, lastModeRes, newModeRes, lastEndTime, setupTimeMode))
        }
      }
      // now we can update the start time for the activity and the makespan
      val startTimeAct = math.max(maxEndTimePrecsActI, maxStartTimeAvailableResActI)
      //startTimes(indAct) := startTimeAct
      startTimesArray(indAct) = startTimeAct
      val endTimeAct = startTimeAct + schedulingModel.activities(indAct).duration
      if (endTimeAct > makeSpanValue) {
        makeSpanValue = endTimeAct
      }
      // update map of resource flows for all resources. It requires another
      // loop on the resources
      for {resInd <- resIndexesActI} {
        val lastResFlows = resourceFlowStates(resInd).forwardFlows
        val resourceQty = schedulingModel.activities(indAct).resourceUsages(resInd).get.capacity
        resourceFlowStates(resInd).forwardFlows = ResourceFlow.addResourceFlowToList(
          indAct,
          resourceQty,
          schedulingModel.activities(indAct).duration,
          startTimes,
          ResourceFlow.flowQuantityResource(
            resourceQty,
            if (resourceFlowStates(resInd).changedRunningMode.isDefined) {
              // running mode has changed
              val lastEndTime = ResourceFlow.getLastEndTimeResourceFlow(lastResFlows)
              val setupTimeMode = schedulingModel.resources(resInd)
                                                 .setupTimeModes(resourceFlowStates(resInd).changedRunningMode.get)(resourceFlowStates(resInd).lastRunningMode)
                                                 .get
              List(SetupFlow(lastEndTime,
                             setupTimeMode,
                             schedulingModel.activities(indAct).resourceUsages(resInd).get.capacity))
            } else {
              lastResFlows
            }
          )
        )
      }
    }
    // Set makespan variable
    makeSpan := makeSpanValue
    // Set start times variables
    for { i <- 0 until schedulingModel.nbActivities } {
      startTimes(i) := startTimesArray(i)
    }
  }

  //TODO Incremental computation
  override def notifySeqChanges(v: ChangingSeqValue, d: Int, changes: SeqUpdate): Unit = {
    scheduleForPropagation()
  }

  //TODO: il faut également planifier un re-calcul quand une autre dimension change: durée des tâche, attribut e resoruces, etc. J'ai vu des variables dans des resources et tâches.

  override def performInvariantPropagation(): Unit = {
    computeAllFromScratch(priorityActivitiesList.value)
  }

  /**
    * Internal class that carries the state of a resource flow
    */
  private class ResourceFlowState(initialModeInd: Int, maxCapacity: Int) {
    //TODO: je suis pas convaincu parce-que toutes les catégories de resrouces ont le mêm state du coup
    // Last activity that used this resource
    var lastActivityIndex: Int = -1
    // Forward flows after last activity that used this resource
    var forwardFlows: List[ResourceFlow] = List(SourceFlow(maxCapacity))
    // Running mode of last activity that used this resource
    var lastRunningMode: Int = initialModeInd
    // Checking whether running mode has changed
    var changedRunningMode: Option[Int] = None
  }

  /** To override whenever possible to spot errors in invariants.
    * this will be called for each invariant after propagation is performed.
    * It requires that the Model is instantiated with the variable debug set to true.
    */
  override def checkInternals(c: Checker): Unit = {
    // To be implementes...
  }
}

/**
  * This case class represents a setup time
  */
case class SetupTimeData(resourceIndex: Int, modeFromInd: Int, modeToInd: Int, startTime: Int, duration: Int)
//TODO: je pense que les SetupTimeData devraient être dans la resource concernée

/**
  * This is a container class for setup times
  */
class SetupTimes {
  var setupTimesList: List[SetupTimeData] = List()

  def reset(): Unit = {
    setupTimesList = List()
  }

  def addSetupTime(st: SetupTimeData): Unit = {
    setupTimesList :+= st
  }

  override def toString: String = {
    val stringBuilder = new StringBuilder
    stringBuilder.append("SetupTimes: \n")
    for {st <- setupTimesList} {
      stringBuilder.append(s"* $st \n")
    }
    stringBuilder.toString
  }
}
