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
/*******************************************************************************
  * Contributors:
  *     This code has been initially developed by CETIC www.cetic.be
  *         by Renaud De Landtsheer
  ******************************************************************************/

package oscar.cbls.invariants.core.computation

import collection.immutable.{SortedSet, SortedMap}
import oscar.cbls.invariants.core.propagation._
import language.implicitConversions

/**This class contains the invariants and variables
  * They are all modelled as propagation Elements, which are handled by the inherited
  * [[oscar.cbls.invariants.core.propagation.PropagationStructure]] class.
  *
  * @param verbose requires that the propagation structure prints a trace of what it is doing.
  *                All prints are preceded by ''PropagationStruture''
  * @param checker specifies that once propagation is finished,
  *                it must call the checkInternals method on all propagation elements.
  * @param noCycle is to be set to true only if the static dependency graph between propagation elements has no cycles.
  *                If unsure, set to false, the engine will discover it by itself.
  *                See also method isAcyclic to query a propagation structure.
  * @param topologicalSort set to true if you want to use topological sort, to false for layered sort (layered is faster)
  * @param propagateOnToString set to true if a toString triggers a propagation, to false otherwise.
  *                            Set to false only for deep debugging
  * @param sortScc true if SCC should be sorted, false otherwise. Set to true, unless you know what your are doing.
  *                Setting to false might provide a speedup (eg in routing problems),
  *                but propagation will not be single pass on SCC anymore
  */
case class Store(override val verbose:Boolean = false,
                 override val checker:Option[Checker] = None,
                 override val noCycle:Boolean = true,
                 override val topologicalSort:Boolean = false,
                 val propagateOnToString:Boolean = true,
                 override val sortScc:Boolean = true)
  extends PropagationStructure(verbose,checker,noCycle,topologicalSort, sortScc)
  with Bulker with StorageUtilityManager{

  assert({println("You are using a CBLS store with asserts activated. It makes the engine slower. Recompile it with -Xdisable-assertions"); true})

  private var variables:List[AbstractVariable] = List.empty
  private var propagationElements:List[PropagationElement] = List.empty
  private var closed:Boolean=false

  def isClosed = closed

  private var privateDecisionVariables:List[Variable] = null;

  def decisionVariables():List[Variable] = {
    if(privateDecisionVariables == null){
      privateDecisionVariables  = List.empty
      for (v:AbstractVariable <- variables if v.isDecisionVariable){
        privateDecisionVariables = v.asInstanceOf[Variable] :: privateDecisionVariables
      }
    }
    privateDecisionVariables
  }
  /**To save the current value of the variables registered in the model
    * @param inputOnly if set to true (as by default) the solution will only contain the variables that are not derived through an invariant
    */
  def solution(inputOnly:Boolean = true):Solution = {
    var assignationInt:SortedMap[ChangingIntValue,Int] = SortedMap.empty
    var assignationIntSet:SortedMap[ChangingSetValue,SortedSet[Int]] = SortedMap.empty

    val VariablesToSave = if(inputOnly) {
      decisionVariables()
    }else variables

    for (v:AbstractVariable <- VariablesToSave){
      v match {
        case i:ChangingIntValue =>
          assignationInt += ((i, i.value))
        case s:ChangingSetValue =>
          assignationIntSet += ((s, s.value))
      }
    }
    Solution(assignationInt,assignationIntSet,this)
  }

  /**this is to be used as a backtracking point in a search engine
    * you can only save variables that are not controlled*/
  def saveValues(vars:Variable*):Snapshot = {
    var assignationInt:List[(CBLSIntVar,Int)] = List.empty
    var assignationIntSet:List[(CBLSSetVar,SortedSet[Int])] = List.empty
    for (v:Variable <- vars if v.isDecisionVariable){
      if(v.isInstanceOf[CBLSIntVar]){
        assignationInt = ((v.asInstanceOf[CBLSIntVar], v.asInstanceOf[CBLSIntVar].getValue(true))) :: assignationInt
      }else if(v.isInstanceOf[CBLSSetVar]){
        assignationIntSet = ((v.asInstanceOf[CBLSSetVar], v.asInstanceOf[CBLSSetVar].getValue(true))) :: assignationIntSet
      }
    }
    Snapshot(assignationInt,assignationIntSet,this)
  }

  /**To restore a saved solution
    * notice that only the variables that are not derived will be restored; others will be derived lazily at the next propagation wave.
    * This enables invariants to rebuild their internal data structure if needed.
    * Only solutions saved from the same model can be restored in the model.
    */
  def restoreSolution(s:Solution){
    assert(s.model==this)
    for((intvar,int) <- s.assignationInt if intvar.isDecisionVariable){
      intvar.asInstanceOf[CBLSIntVar] := int
    }
    for((intsetvar,intset) <- s.assignationIntSet if intsetvar.isDecisionVariable){
      intsetvar.asInstanceOf[CBLSSetVar]:= intset
    }
  }

  /**To restore a saved snapshot
    * notice that only the variables that are not derived will be restored; others will be derived lazily at the next propagation wave.
    * This enables invariants to rebuild their internal data structure if needed.
    * Only solutions saved from the same model can be restored in the model.
    */
  def restoreSnapshot(s:Snapshot){
    assert(s.model==this)
    for((intsetvar,intset) <- s.assignationIntSet if intsetvar.isDecisionVariable){
      intsetvar := intset
    }
    for((intvar,int) <- s.assignationInt if intvar.isDecisionVariable){
      intvar := int
    }
  }

  /**Called by each variable to register themselves to the model
    * @param v the variable
    * @return a unique identifier that will be used to distinguish variables. basically, variables use this value to set up an arbitrary ordering for use in dictionnaries.
    */
  def registerVariable(v:AbstractVariable):Int = {
    assert(!closed,"model is closed, cannot add variables")
    //ici on utilise des listes parce-que on ne peut pas utiliser des dictionnaires
    // vu que les variables n'ont pas encore recu leur unique ID.
    variables = v :: variables
    propagationElements =  v :: propagationElements
    GetNextID()
  }

  /**Called by each invariants to register themselves to the model
    * @param i the invariant
    * @return a unique identifier that will be used to distinguish invariants. basically, invariants use this value to set up an arbitrary ordering for use in dictionnaries.
    */
  def registerInvariant(i:Invariant):Int = {
    assert(!closed,"model is closed, cannot add invariant")
    propagationElements = i :: propagationElements
    GetNextID()
  }

  override def getPropagationElements:Iterable[PropagationElement] = {
    propagationElements
  }

  var toCallBeforeClose:List[(()=>Unit)] = List.empty

  def addToCallBeforeClose(toCallBeforeCloseProc : (()=>Unit)){
    toCallBeforeClose = (toCallBeforeCloseProc) :: toCallBeforeClose
  }


  protected def performCallsBeforeClose() {
    for (p <- toCallBeforeClose) p()
    toCallBeforeClose = List.empty
  }

  /**calls this when you have declared all your invariants and variables.
    * This must be called before any query and update can be made on the model,
    * and after all the invariants and variables have been declared.
   * @param DropStaticGraph true if you want to drop the static propagation graph to free memory. It takes little time
   */
  def close(DropStaticGraph: Boolean = true){
    assert(!closed, "cannot close a model twice")
    performCallsBeforeClose()
    setupPropagationStructure(DropStaticGraph)
    killBulker() //we won't create any new model artifacts, thus we can kill the bulker and free its memory
    closed=true
  }

  /**this checks that invariant i is one that is supposed to do something now
    * used to check that invariants have declared all their controling links to the model
    * */
  def checkExecutingInvariantOK(i:Invariant):Boolean = {
    if(i != null){
      if (NotifiedInvariant != null && NotifiedInvariant != i){
        return false
      }
      if (NotifiedInvariant == null && getPropagatingElement != null &&  getPropagatingElement != i){
        return false
      }
    }else{
      if (NotifiedInvariant != null || getPropagatingElement != null){
        return false
      }
    }
    true
  }

  var NotifiedInvariant:Invariant=null

  override def toString:String = variables.toString()
  def toStringInputOnly = variables.filter(v => v.isDecisionVariable).toString()

  //returns the set of source variable that define this one.
  // This exploration procedure explores passed dynamic invariants,
  // but it over-estimates the set of source variables over dynamic invariants, as it uses the static graph.
  def getSourceVariables(v:Variable):SortedSet[Variable] = {
    var ToExplore: List[PropagationElement] = List(v)
    var SourceVariables:SortedSet[Variable] = SortedSet.empty
    while(!ToExplore.isEmpty){
      val head = ToExplore.head
      ToExplore = ToExplore.tail
      //TODO: we can have both var and invar in the same class now.
      if(head.isInstanceOf[Variable]){
        val v:Variable = head.asInstanceOf[Variable]
        if(!SourceVariables.contains(v)){
          SourceVariables += v
          for(listened <- v.getStaticallyListenedElements)
            ToExplore = listened :: ToExplore
        }
        //TODO: keep a set of the explored invariants, to speed up this thing?
      }else if(head.isInstanceOf[Invariant]){
        val i:Invariant = head.asInstanceOf[Invariant]
        for (listened <- i.getStaticallyListenedElements){
          if (listened.propagationStructure != null && (!listened.isInstanceOf[Variable] || !SourceVariables.contains(listened.asInstanceOf[Variable]))){
            ToExplore = listened :: ToExplore
          }
        }
      }else{assert(false,"propagation element that is not a variable, and not an invariant??")}
    }
    SourceVariables
  }

  /** returns some info on the Store
    * call this after closing
    * @return
    */
  override def stats:String = {
    require(isClosed, "store must be closed to get some stats")
    super.stats + "\n" +
      "Store(" + "\n" +
      "  variableCount:" + variables.size + "\n" +
      "  inputVariableCount" + decisionVariables.size + "\n" +
      ")"
  }
}

case class Snapshot(assignationInt:List[(CBLSIntVar, Int)],
                    assignationIntSet:List[(CBLSSetVar,SortedSet[Int])],
                    model:Store)

/**This class contains a solution. It can be generated by a model, to store the state of the search, and restored.
  * it remains linked to the model, as it maintains references to the variables declared in the model.
  * you cannot pass it over a network connection for instance.
  * see methods getSolution and restoreSolution in [[oscar.cbls.invariants.core.computation.Store]]
  */
case class Solution(assignationInt:SortedMap[ChangingIntValue,Int],
                    assignationIntSet:SortedMap[ChangingSetValue,SortedSet[Int]],
                    model:Store){

  /**to get the value of an IntVar in the saved solution*/
  def getSnapshot(v:ChangingIntValue):Int = assignationInt(v)

  /**to get the value of an IntSetVar in the saved solution*/
  def getSnapshot(v:ChangingSetValue):SortedSet[Int] = assignationIntSet(v)

  /**converts the solution to a human-readable string*/
  override def toString:String = {
    var acc:String = ""
    var started = false
    acc = "IntVars("
    for(v <-assignationInt){
      if(started) acc += ","
      started = true
      acc += v._1.name + ":=" + v._2
    }
    acc +=")  IntSetVars("
    started = false
    for(v <-assignationIntSet){
      if(started) acc += ","
      started = true
      acc += v._1.name + ":=" + (if (v._2.isEmpty){"null"}else{"{" + v._2.foldLeft("")((acc,intval) => if(acc.equalsIgnoreCase("")) ""+intval else acc+","+intval) + "}"})
    }
    acc + ")"
  }
}

object Invariant{
  implicit val Ord:Ordering[Invariant] = new Ordering[Invariant]{
    def compare(o1: Invariant, o2: Invariant) = o1.compare(o2)
  }
}

trait VaryingDependenciesInvariant extends VaryingDependencies{
  /**register to determining element. It must be in the static dependency graph*/
  def registerDeterminingDependency(v:Value,i:Any = -1){
    registerDeterminingElement(v,i)
  }
}

/**This is be base class for all invariants.
  * Invariants also register to the model, but they identify the model they are associated to by querying the variables they are monitoring.
  *
  */
trait Invariant extends PropagationElement{

  def model:Store = schedulingHandler.asInstanceOf[Store]

  def hasModel:Boolean = schedulingHandler != null

  final def preFinishInitialization(model:Store = null):Store = {
    if (schedulingHandler == null){
      if (model == null){
        schedulingHandler = InvariantHelper.findModel(getStaticallyListenedElements)
        this.model
      }else{
        schedulingHandler = model //assert(model == InvariantHelper.FindModel(getStaticallyListenedElements()))
        model
      }
    }else this.model
  }

  /**Must be called by all invariant after they complete their initialization
    * that is: before they get their output variable.
    * This performs some registration to the model, which is discovered by exploring the variables that are statically registered to the model
    * no more variable can be registered statically after this method has been called.
    * @param model; if specified, it only checks that the model is coherent, and registers to it for the ordering
    */
  final def finishInitialization(model:Store = null){
    val m:Store = preFinishInitialization(model)
    if (m != null){
      uniqueID = m.registerInvariant(this)
    }else{
      uniqueID = -1
    }
  }

  /**Call this from within the invariant to notify that you will statically listen to this variable.
    * You CANNOT register a variable twice. It is undetected, but will lead to unexpected behavior.
    * @param v the variable that you want to listen to (and be notified about change)
    */
  def registerStaticDependency(v:Value){
    registerStaticallyListenedElement(v)
  }

  def registerStaticDependencies(v:Value*){
    for (vv <- v)registerStaticDependency(vv)
  }

  /**this method is an alternative to the two call to the registration methods
    * it performs the registration to the static and dynamic graphs at once.
    * just to spare one call
    * @param v the variable that we want to register to
    * @param i the integer value that will be passed to the invariant to notify some changes in the value of this variable
    */
  def registerStaticAndDynamicDependency(v:Value,i:Any = -1){
    registerStaticDependency(v)
    registerDynamicDependency(v,i)
  }

  def registerStaticAndDynamicDependencies(v:((Value,Any))*){
    for (varint <- v){
      registerStaticDependency(varint._1)
      registerDynamicDependency(varint._1,varint._2)
    }
  }

  /**
   * registers static and dynamic dependency to all items in the array
   * let be i, a position in the array, the index used for dynamic dependency registration is i+offset
   * @param v the variable that are registered
   * @param offset and offset applied to the position in the array when registering the dynamic dependency
   */
  def registerStaticAndDynamicDependencyArrayIndex[T <: Value](v:Array[T],offset:Int = 0):Array[KeyForElementRemoval] = {
    Array.tabulate(v.size)((i:Int) => {
      registerStaticDependency(v(i))
      registerDynamicDependency(v(i),i + offset)
    })
  }

  def registerStaticAndDynamicDependenciesNoID(v:Value*){
    for (varint <- v){
      registerStaticDependency(varint)
      registerDynamicDependency(varint)
    }
  }

  def registerStaticAndDynamicDependencyAllNoID(v:Iterable[Value]){
    for (varint <- v){
      registerStaticDependency(varint)
      registerDynamicDependency(varint)
    }
  }

  /**Call this from within the invariant to notify that you will listen to this variable.
    * The variable must be registered in the static propagation graph.
    * You CANNOT register a variable twice. It is undetected, but will lead to unexpected behavior.
    * @param v the variable that you want to listen to (and be notified about change)
    * @param i: an integer value that will be passed when updates on this variable are notified to the invariant
    * @return a handle that is required to remove the listened var from the dynamically listened ones
    */
  def registerDynamicDependency(v:Value,i:Any = -1):KeyForElementRemoval = {
    registerDynamicallyListenedElement(v,i)
  }

  /**Call this from within the invariant to notify that you stop listen to this variable.
    * The variable must have been be registered in the static propagation graph.
    * @param k the handle that was returned when the variable added to the dynamic graph
    */
  def unregisterDynamicDependency(k:KeyForElementRemoval){
    unregisterDynamicallyListenedElement(k)
  }

  //we are only notified for the variable we really want to listen (cfr. mGetReallyListenedElements, registerDynamicDependency, unregisterDynamicDependency)
  def notifyIntChangedAny(v:ChangingIntValue,i:Any,OldVal:Int,NewVal:Int){notifyIntChanged(v,i.asInstanceOf[Int], OldVal,NewVal)}

  def notifyIntChanged(v:ChangingIntValue,i:Int,OldVal:Int,NewVal:Int){notifyIntChanged(v,OldVal,NewVal)}

  def notifyIntChanged(v:ChangingIntValue,OldVal:Int,NewVal:Int){}

  def notifyInsertOnAny(v:ChangingSetValue,i:Any,value:Int){notifyInsertOn(v,i.asInstanceOf[Int],value)}

  def notifyInsertOn(v:ChangingSetValue,i:Int,value:Int){notifyInsertOn(v,value)}

  def notifyInsertOn(v:ChangingSetValue,value:Int){}

  def notifyDeleteOnAny(v:ChangingSetValue,i:Any,value:Int){notifyDeleteOn(v,i.asInstanceOf[Int],value)}

  def notifyDeleteOn(v:ChangingSetValue,i:Int,value:Int){notifyDeleteOn(v,value)}

  def notifyDeleteOn(v:ChangingSetValue,value:Int){}

  /**To override whenever possible to spot errors in invariants.
    * this will be called for each invariant after propagation is performed.
    * It requires that the Model is instantiated with the variable debug set to true.
    */
  override def checkInternals(c:Checker){c.check(false, Some("DEFAULT EMPTY CHECK " + this.toString() + ".checkInternals"))}

  def getDotNode = "[label = \"" + this.getClass.getSimpleName + "\" shape = box]"

  /** this is the propagation method that should be overridden by propagation elements.
    * notice that it is only called in a propagation wave if:
    * 1: it has been registered for propagation since the last time it was propagated
    * 2: it is included in the propagation wave: partial propagation wave do not propagate all propagation elements;
    * it only propagates the ones that come in the predecessors of the targeted propagation element
    * overriding this method is optional, so an empty body is provided by default */
  override final def performPropagation(){performInvariantPropagation()}

  def performInvariantPropagation(){}
}

object InvariantHelper{
  /**this is useful to find the model out of a set of propagation elements.
    *
    * @param i some propagation elements, typically, variables listened by some invariants
    * @return the model that the invariant belongs to
    */
  def findModel(i:Iterable[PropagationElement]):Store={
    i.foreach(e => {
      if (e.isInstanceOf[Variable]){
        val m = e.asInstanceOf[Variable].model
        if (m != null){
          return m
        }
      }
    })
    null
  }

  def findModel(i:PropagationElement*):Store={
    i.foreach(e => {
      if (e.isInstanceOf[Variable]){
        val m = e.asInstanceOf[Variable].model
        if (m != null){
          return m
        }
      }
    })
    null
  }

  def getMinMaxBounds(variables:Iterable[IntValue]):Range = {
    var MyMax = Int.MinValue
    var MyMin = Int.MaxValue
    for (v <- variables) {
      if (MyMax < v.max) MyMax = v.max
      if (MyMin > v.min) MyMin = v.min
    }
    MyMin to MyMax
  }
  def getMinMaxBoundsIntSetVar(variables:Iterable[SetValue]):Range = {
    var MyMax = Int.MinValue
    var MyMin = Int.MaxValue
    for (v <- variables) {
      if (MyMax < v.max) MyMax = v.max
      if (MyMin > v.min) MyMin = v.min
    }
    MyMin to MyMax
  }

  def arrayToString[T<:Variable](a:Array[T]):String =
    "[" + a.toList.mkString(",")+"]"
}

trait Value extends DummyPropagationElement

trait Variable extends AbstractVariable{
  protected var definingInvariant:Invariant = null
  def setDefiningInvariant(i:Invariant){
    assert(i.model == model || i.model == null,"i.model == null:" + (i.model == null) + " i.model == model:" + (i.model == model) + " model == null:" + (model == null))
    if(definingInvariant == null){
      definingInvariant = i
      registerStaticallyListenedElement(i)
      registerDynamicallyListenedElement(i,0)
    }else{
      throw new Exception("variable [" + name + "] cannot have more than one controlling invariant, already has " + definingInvariant)
    }
  }
}

object Variable{
  implicit val ord:Ordering[Variable] = new Ordering[Variable]{
    def compare(o1: Variable, o2: Variable) = o1.compare(o2)
  }
}

/**This is the base class for variable. A variable is a propagation element that holds some value.
  * Variables have an associated model, to which they register as soon as they are created. Variables also have a name,
  * which is used solely for printing models.
  */
trait AbstractVariable
  extends PropagationElement with DistributedStorageUtility with Value{

  final def model_=(s:Store): Unit ={
    schedulingHandler = s
    uniqueID = if (s == null) -1 else s.registerVariable(this)
  }
  final def model = propagationStructure.asInstanceOf[Store]

  def name:String

  def defaultName = s"Var_$uniqueID"

  def definingInvariant:Invariant

  def isControlledVariable:Boolean = definingInvariant != null
  def isDecisionVariable:Boolean = definingInvariant == null

  /**this method s to be called by any method that internally modifies the value of the variable
    * it schedules the variable for propagation, and performs a basic check of the identify of the executing invariant*/
  def notifyChanged(){
    //modifier le test.
    if (this.model == null ||(!this.model.isClosed && this.getDynamicallyListeningElements.isEmpty)){
      performPropagation()
    }else{
      assert(model.checkExecutingInvariantOK(definingInvariant),"variable [" + this + "] affected by non-controlling invariant")
      scheduleForPropagation()
    }
  }

  /** this method is a toString that does not trigger a propagation.
    * use this to debug your software.
    * you should specify to your IDE to render variable objects using this method isntead of the toString method
    * @return a string similar to the toString method
    */
  def toStringNoPropagate:String

  def getDotColor:String = {
    if (getStaticallyListeningElements.isEmpty){
      "blue"
    }else if (getStaticallyListenedElements.isEmpty){
      "green"
    }else{
      "black"
    }
  }
}

object AbstractVariable{
  implicit val ord:Ordering[AbstractVariable] = new Ordering[AbstractVariable]{
    def compare(o1: AbstractVariable, o2: AbstractVariable) = o1.compare(o2)
  }
}

/**add this to your invariant when you instantiate them if ylou want the variable to have a custom name*/
trait CustomName extends AbstractVariable{
  override def name = customName
  def customName:String
}