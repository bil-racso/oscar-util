package oscar.cbls.core.propagation.draft


import oscar.cbls.algo.dag.DAGNode
import oscar.cbls.algo.dll.{DPFDLLStorageElement, DelayedPermaFilteredDoublyLinkedList}
import oscar.cbls.algo.quick.QList
import oscar.cbls.core.propagation.{BasicPropagationElement, KeyForElementRemoval, PropagationElement, StronglyConnectedComponent}


object PropagationImpactCharacteristics extends Enumeration{
  type PropagationImpactCharacteristics = Value
  val NoPropagationNotificationReceivedNoNotificationEmitted,
  NotificationOnPropagateNoNotificationReceived,
  BulkElementNotificationBehavior,
  SCCNotificationBehavior,
  NotificationOnNotifyNoPropagate,
  NotificationOnNotifyAndPropagate,
  NotificationOnPropagateReceivesNotification
  = Value

  //Variables: NotificationOnPropagate
  //invariants classiques NotificationOnNotifyAndPropagate, si pas de propagate, alors NotificationOnNotify
  //IntInvariants NotificationOnPropagate (comme les variables en  fait) mais NotificationOnNotifyAndPropagate si ils ont plus de sorties
  //events NotificationOnNotifyAndPropagate
  //bulk BulkElement
}

import PropagationImpactCharacteristics._


abstract class PropagationElement(val notificationBehavior:PropagationImpactCharacteristics,
                                  val varyingDependencies:Boolean) extends DAGNode {

  var uniqueID = -1 //DAG node already have this kind of stuff
  var isScheduled:Boolean = false
  var schedulingHandler:SchedulingHandler = null
  var model:PropagationStructure = null

  private[this] var myScc:StronglyConnectedComponent = null
 //We have a getter because a specific setter is define herebelow
  def scc:StronglyConnectedComponent = myScc

  def couldBePropagated:Boolean = notificationBehavior match {
    case NotificationOnPropagateNoNotificationReceived | NotificationOnNotifyAndPropagate | NotificationOnPropagateReceivesNotification => true
    case BulkElementNotificationBehavior | NotificationOnNotifyNoPropagate | NoPropagationNotificationReceivedNoNotificationEmitted => false
    case SCCNotificationBehavior => true
  }

  var layer:Int = -1
  var threadID:Int = -1

  // //////////////////////////////////////////////////////////////////////
  //static propagation graph
  var staticallyListeningElements:QList[PropagationElement] = null
  var staticallyListenedElements:QList[PropagationElement] = null

  /**
    * listeningElement call this method to express that
    * they might register with dynamic dependency to the invocation target
    * @param listeningElement
    */
  protected def registerStaticallyListeningElement(listeningElement: PropagationElement) {
    staticallyListeningElements = QList(listeningElement,staticallyListeningElements)
    listeningElement.staticallyListenedElements = QList(this,listeningElement.staticallyListenedElements)
  }

  // //////////////////////////////////////////////////////////////////////
  //dynamic propagation graph

  val dynamicallyListenedElements: DelayedPermaFilteredDoublyLinkedList[PropagationElement]
  = new DelayedPermaFilteredDoublyLinkedList[PropagationElement]

  val dynamicallyListeningElements: DelayedPermaFilteredDoublyLinkedList[(PropagationElement, Int)]
  = new DelayedPermaFilteredDoublyLinkedList[(PropagationElement, Int)]

  //Hi, I listen to you!
  def registerDynamicallyListeningElement(listeningElement:PropagationElement,
                                          id:Int): KeyForDynamicDependencyRemoval = {

    new KeyForDynamicDependencyRemoval(
      this.dynamicallyListeningElements.addElem((listeningElement,id)),
      listeningElement.dynamicallyListenedElements.addElem(this))
  }

  // //////////////////////////////////////////////////////////////////////
  //DAG stuff, for SCC sort

  def compare(that: DAGNode): Int = {
    assert(this.uniqueID != -1, "cannot compare non-registered PropagationElements this: [" + this + "] that: [" + that + "]")
    assert(that.uniqueID != -1, "cannot compare non-registered PropagationElements this: [" + this + "] that: [" + that + "]")
    this.uniqueID - that.uniqueID
  }

  final var getDAGPrecedingNodes: Iterable[DAGNode] = null
  final var getDAGSucceedingNodes: Iterable[DAGNode] = null

  def scc_=(scc:StronglyConnectedComponent): Unit ={
    require(this.scc == null)
    this.scc = scc

    //we have to create the SCC injectors that will maintain the filtered Perma filter of nodes in the same SCC
    //for the listening side
    def filterForListening(listeningAndPayload: (PropagationElement, Int),
                           injector: (() => Unit),
                           isStillValid: (() => Boolean)) {
      val listening = listeningAndPayload._1
      if (scc == listening.scc) {
        scc.registerOrCompleteWaitingDependency(this, listening, injector, isStillValid)
      }
    }

    getDAGSucceedingNodes = dynamicallyListeningElements.delayedPermaFilter(filterForListening, (e) => e._1)

    getDAGPrecedingNodes = if(varyingDependencies) {
      def filterForListened(listened: PropagationElement,
                            injector: (() => Unit),
                            isStillValid: (() => Boolean)){
        if (scc == listened.scc) {
          scc.registerOrCompleteWaitingDependency(listened, this, injector, isStillValid)
        }
      }
      dynamicallyListenedElements.delayedPermaFilter(filterForListened)
    }else{
      staticallyListenedElements.filter(_.scc == scc)
    }

  }


  // ////////////////////////////////////////////////////////////////////////
  // api about scheduling and propagation

  def scheduleMyselfForPropagation(): Unit ={
    assert(!couldBePropagated)
    if(!isScheduled){
      isScheduled = true
      schedulingHandler.schedulePEForPropagation(this)
    }
  }

  def reScheduleIfScheduled(): Unit ={
    if(isScheduled){
      schedulingHandler.schedulePEForPropagation(this)
    }
  }

  def triggerPropagation(){
    model.triggerPropagation(this)
  }

  final def propagate(){
    require(!couldBePropagated)
    if(isScheduled) {
      isScheduled = false
      performPropagation()
    }
  }

  protected def performPropagation() = ???





  def finishInitialization(): Unit ={
    schedulingHandler = model
  }





}


/**
  * This is the node type to be used for bulking
  * @author renaud.delandtsheer@cetic.be
  */
trait BulkPropagationElement extends PropagationElement {

}

/**
  * This class is used in as a handle to register and unregister dynamically to variables
  * @author renaud.delandtsheer@cetic.be
  */
class KeyForDynamicDependencyRemoval(key1: DPFDLLStorageElement[(PropagationElement, Int)],
                                     key2: DPFDLLStorageElement[PropagationElement]) {
  def performRemove(): Unit = {
    if(key1 != null) key1.delete()
    if(key2 != null) key2.delete()
  }
}
