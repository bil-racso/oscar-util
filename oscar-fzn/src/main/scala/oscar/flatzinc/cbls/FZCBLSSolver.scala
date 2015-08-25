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
/**
 * @author Gustav Björdal
 * @author Jean-Noël Monette
 */
package oscar.flatzinc.cbls

import scala.util.Random
import scala.collection.mutable.{ Map => MMap, Set => MSet }
import oscar.cbls.search._
import oscar.cbls.objective.{ Objective => CBLSObjective }
import oscar.cbls.constraints.core._
import oscar.cbls.constraints.lib.basic._
import oscar.cbls.constraints.lib.global._
import oscar.cbls.invariants.lib.logic._
import oscar.cbls.invariants.lib.minmax._
import oscar.cbls.invariants.core.computation._
import oscar.cbls.invariants.lib.numeric._
import oscar.cbls.invariants.core.computation.IntInvariant.toIntVar
//import oscar.flatzinc.parser.NewParser
import oscar.flatzinc.parser.Options
import oscar.flatzinc.model._
import oscar.flatzinc.model.Constraint
import oscar.flatzinc.model.Variable
import oscar.flatzinc.cbls.support._
import oscar.cbls.invariants.lib.numeric.Sum2
import oscar.flatzinc.transfo.FZModelTransfo
import java.io.PrintWriter
import oscar.flatzinc.parser.FZParser


//TODO: Move this class somewhere else...
//TODO: Add several levels of logging
//TODO: Add the possibility to print to file or something else
class Log(opts:Options){
  def apply(s:String) = {
    if (opts.verbose) Console.err.println("% "+s)
  }
}


class FZCBLSSolver extends SearchEngine with FZCBLSConstraints with StopWatch {
  
  def solve(opts: Options): List[(Long, Int, Int, String)] = {
    startWatch()
    val log = new Log(opts);
    log("start")
    
    implicit val model = FZParser.readFlatZincModelFromFile(opts.fileName).problem;
    log("Parsing took "+getWatch+" ms")
    /*object parser extends NewParser {
//      try {
        myParseAll(opts)
//      } catch {
//        case e: Throwable => {
//          println(e)
//          println(e.printStackTrace())
//          throw new Exception("adding the constraint failed")
//        }
//      }
    }
    
    val model2 = parser.model;
    
    //println(model.constraints)
    log("Parsing 2 took "+getWatch+" ms")
    println("% "+ model.map.size+"\t"+model2.map.size)
    println("% "+model.constraints.size+"\t"+model2.constraints.size)*/
//    if(model2.constraints != model.constraints){
//      println(model2.constraints.mkString("[\n","\n","\n]"))
//      println(model.constraints.mkString("[\n","\n","\n]"))
//    // System.exit(0);
//    }
    //model.fixDomains();
    //Turn all free variables into defined variables where possible
    log("Parsed")
    FZModelTransfo.propagateDomainBounds(model);
    //FZModelTransfo.propagateDomainBounds(model2);
    log("Reduced Domains")
    if(!opts.opts.contains("no-find-inv")){
      FZModelTransfo.findInvariants(model);
      log("Found Invariants")
    }else{
      log("Did not search for new invariants")
    }
    
    //m = new Store(false, None, false)//This is already created in FZBCLSConstraint...
    implicit val cblsIntMap: MMap[String, CBLSIntVarDom] = MMap.empty[String, CBLSIntVarDom]
    //cblsIntMap = MMap.empty[String, CBLSIntVar]

    // Model

    var searchVariables: List[CBLSIntVarDom] = createVariables()
    // constraint system
    implicit val c = ConstraintSystem(m)

    //Objective
    val objectiveWeight = CBLSIntVar(m, 0 to 10000, 1, "objective_weight")
    val violationWeight = CBLSIntVar(m, 0 to 10000, 1, "violation_weight")
    val objective: CBLSObjective = new CBLSObjective(
      model.search.obj match {
        case Objective.SATISFY => c.violation
        case Objective.MAXIMIZE => Minus(Prod2(c.violation, violationWeight), Prod2(model.search.variable.get, objectiveWeight))
        case Objective.MINIMIZE => Sum2(Prod2(c.violation, violationWeight), Prod2(model.search.variable.get, objectiveWeight))
      })
    log("Created Objective")
    //TODO: Most of those should be List instead of Array
    var constraints = model.constraints.toArray[Constraint];
    var remainingConstraints = Array.empty[Constraint];
    var implicitConstraints = Array.empty[Neighbourhood]
    var softConstraints = Array.empty[Constraint];
    
    var implicitConstants = searchVariables.filter(_.domainSize==1);
    searchVariables = searchVariables.filterNot(_.domainSize==1);
    //println(searchVariables)
    //Replaced the following piece of code by a simplification of the FZModel above
    /*def tryMap(v: ConcreteVariable, c: ConcreteConstant): Boolean = {
      if (!v.isDefined && searchVariables.contains(getVar(v))) {
        cblsIntMap(v.id) <== CBLSIntConst(c.value, m);
        implicitConstants :+= cblsIntMap(v.id);
        searchVariables = searchVariables.filterNot(_.name == v.id);
        return true
      }
      return false
    }
    constraints = constraints.filterNot((constraint: Constraint) =>
      constraint match {
        case int_eq(x: ConcreteConstant, y: ConcreteVariable, ann) => tryMap(y, x)
        case int_eq(x: ConcreteVariable, y: ConcreteConstant, ann) => tryMap(x, y)
        case bool_eq(x: ConcreteConstant, y: ConcreteVariable, ann) => tryMap(y, x)
        case bool_eq(x: ConcreteVariable, y: ConcreteConstant, ann) => tryMap(x, y)
        case _ => false
      })
    log("Removed Constant Variables from Search")
    *
    */
    def tryAllDiff(xs: Array[Variable]):Boolean = {
      if (!xs.foldLeft(false)((acc: Boolean, x: Variable) => acc || x.isDefined || x.isIntroduced || (!searchVariables.exists((v: CBLSIntVar) => v.name == x.id) && !implicitConstants.contains(getCBLSVar(x))))){
        val nonConstants = xs.filterNot(x => implicitConstants.contains(getCBLSVar(x)))
        val domMin = nonConstants(0).min;
        val domMax = nonConstants(0).max;
        if (nonConstants.foldLeft(true)((acc: Boolean, x: Variable) => acc && (x.min == domMin && x.max == domMax))) {
          implicitConstraints :+= new AllDifferentEqDom(xs.map(getCBLSVar(_)), implicitConstants, objective, c)
          for (x <- xs)
            searchVariables = searchVariables.filterNot(_.name == x.id);
          true
        } else {
          false
        }
      }else{ 
        false
      }
    }
    def tryCircuit(xs: Array[Variable]):Boolean = {
      if (xs.foldLeft(true)((acc: Boolean, x: Variable) => 
          acc && !x.isDefined && 
          //TODO: What is the purpose of this guard? Avoid constants?
          (searchVariables.exists((v: CBLSIntVar) => v.name == x.id)  || implicitConstants.contains(getCBLSVar(x))))){
        //TODO: remove some of the defined if it is better to use the Circuit implicit constraint
        //TODO: We assume that the offset is 1. Is it always the case?
        for(i <- 0 until xs.length){
          EnsureDomain(xs(i))
        }
        implicitConstraints :+= new ThreeOpt(xs.map(getCBLSVar(_)),objective, c,1)
        for (x <- xs)
            searchVariables = searchVariables.filterNot(_.name == x.id);
        true
      }else{
        false
      }
    }
    def trySubCircuit(xs: Array[Variable]):Boolean = {
      if (xs.foldLeft(true)((acc: Boolean, x: Variable) => 
          acc && !x.isDefined && 
          //TODO: What is the purpose of this guard? Avoid constants?
          (searchVariables.exists((v: CBLSIntVar) => v.name == x.id)  || implicitConstants.contains(getCBLSVar(x))))){
        //TODO: We assume that the offset is 1. Is it always the case?
        //TODO: remove some of the defined if it is better to use the Circuit implicit constraint
        for(i <- 0 until xs.length){
          //println(xs(i).dom)
          EnsureDomain(xs(i))
        }
        implicitConstraints :+= new ThreeOptSub(xs.map(getCBLSVar(_)),objective, c,1)
        for (x <- xs)
            searchVariables = searchVariables.filterNot(_.name == x.id);
        true
      }else{
        false
      }
    }
    
    def tryGCC(xs: Array[Variable],vals: Array[Variable], cnts: Array[Variable],closed: Boolean):Boolean ={
      if (xs.forall(x => ! x.isDefined && 
          //TODO: What is the purpose of this guard? Avoid constants?
          (searchVariables.exists((v: CBLSIntVar) => v.name == x.id)  || implicitConstants.contains(getCBLSVar(x))))
          && cnts.forall(c => c.min==c.max)){//Only for fixed count variables for now
        implicitConstraints :+= new GCCNeighborhood(xs.map(getCBLSVar(_)),vals.map(_.min),cnts.map(_.min),cnts.map(_.max),closed,objective,c)
        for (x <- xs)
            searchVariables = searchVariables.filterNot(_.name == x.id);
        true
      }else{
        false
      }
    }
    def tryGCClu(xs: Array[Variable],vals: Array[Variable], low: Array[Variable],up: Array[Variable],closed: Boolean):Boolean ={
      if (xs.forall(x => ! x.isDefined && 
          //TODO: What is the purpose of this guard? Avoid constants?
          (searchVariables.exists((v: CBLSIntVar) => v.name == x.id)  || implicitConstants.contains(getCBLSVar(x))))){
        implicitConstraints :+= new GCCNeighborhood(xs.map(getCBLSVar(_)),vals.map(_.min),low.map(_.min),up.map(_.max),closed,objective,c)
        for (x <- xs)
            searchVariables = searchVariables.filterNot(_.name == x.id);
        true
      }else{
        false
      }
    }
    //println(implicitConstants.mkString(", "))
    if(!opts.opts.contains("no-impl-cstr")){
      //TODO: DO not like the filtering here.
      //TODO: Why is constraints an Array. Could be a List?
      constraints = constraints.filterNot((constraint: Constraint) =>
        constraint match {
          //TODO: this line of simplification should come somewhere else, actually
          case all_different_int(xs, ann) if xs.length == 1 => true //flattening can generate alldiffs with only one variable... lets ignore those.
          case all_different_int(xs, ann) => tryAllDiff(xs)
          case circuit(xs, ann) => tryCircuit(xs)
          case subcircuit(xs, ann) => trySubCircuit(xs)
          case global_cardinality_closed(xs,vals,cnts,ann) => tryGCC(xs,vals,cnts,true)
          //TODO: detect when a GCC is closed even if not declared as such (should come much earlier)
          case global_cardinality(xs,vals,cnts,ann) => tryGCC(xs,vals,cnts,false)
          case global_cardinality_low_up_closed(xs,vals,low,up,ann) => tryGCClu(xs,vals,low,up,true)
          case global_cardinality_low_up(xs,vals,low,up,ann) => tryGCClu(xs,vals,low,up,false)
          
          case _ => false;
        })
        
     
      log("Found "+implicitConstraints.length+" Implicit Constraints")
    }else{
      log("Did not try to find implicit constraints")
    }
    var invariants = getSortedInvariants(constraints.filter(_.definedVar.isDefined))
    log("Sorted "+invariants.length+" Invariants")
    softConstraints = constraints.filterNot(_.definedVar.isDefined);
    //println(searchVariables)
    // println(softConstraints.length)
    for (invariant <- invariants){
      //log("Posting as Invariant "+invariant)
      add_constraint(invariant);//TODO Separate the two methods
    }
    log("Posted "+invariants.length+" Invariants")
    for (constraint <- softConstraints) {
      //log("Posting "+constraint)
      add_constraint(constraint);
    }
    log("Posted "+softConstraints.length+" Soft Constraints")
    //println(implicitConstraints.length + " implicit constraints");
    val boolVars = searchVariables.filter((v: CBLSIntVar) => v.minVal == 0 && v.maxVal == 1)
    val boolSwapNeighbourhood = if (boolVars.length > 0) {
      Array(new MaxViolatingSwap(boolVars.toArray, objective, c))
    } else { Array.empty[Neighbourhood] }

    val searchNeighbourhood = implicitConstraints ++ boolSwapNeighbourhood ++ (if (searchVariables.length > 0) {
      Array(new MaxViolating(searchVariables.toArray, objective, c))
    } else { Array.empty[Neighbourhood] })
    log("Using "+searchVariables.length+" Search Variables")
    log("Created all Neighborhoods")
    //println(searchNeighbourhood.length + " searchNeighbourhood");
    val timeout = (if(opts.timeOut>0) {println("% time(s) "+opts.timeOut); opts.timeOut} else 5 * 60) * 1000
    log("Starting Search at "+getWatchString)
    for(v<-searchVariables){
      log("search var: "+v.name+" dom size:"+v.domain.length+" ")
    }
    val search =  new Chain(
        new SimpleLocalSearch(c,searchVariables,CBLSObjective(c.violation)/* objective*/,m,handleSolution,timeout,() => getWatch,log),
        //new FakeSearch(),
        model.search.obj match {
          case Objective.SATISFY => new NeighbourhoodSearchSAT(searchNeighbourhood, c, objective, violationWeight, objectiveWeight, timeout, m, getSolution, handleSolution, () => getWatch,log);
          case Objective.MAXIMIZE => new NeighbourhoodSearchOPT(searchNeighbourhood, c, objective,model.search.variable.get, - model.search.variable.get.max, violationWeight, objectiveWeight, timeout, m, getSolution, handleSolution, () => getWatch,log);
          case Objective.MINIMIZE => new NeighbourhoodSearchOPT(searchNeighbourhood, c, objective,model.search.variable.get, model.search.variable.get.min, violationWeight, objectiveWeight, timeout, m, getSolution, handleSolution, () => getWatch,log);
        });
    
    
    m.close();
    /*
    val tmp = m.dumpToDot(true, true);
    println(tmp);
    val pw = new PrintWriter("graph.dot");
    pw.print(tmp);
    pw.close();
    */
    log("Model closed");
    val solutions = search.run();
    log("Done at "+getWatchString)
    return solutions;
  }

  def createVariables()(implicit model: FZProblem, cblsIntMap: MMap[String, CBLSIntVarDom]) = {
    var searchVariables: List[CBLSIntVarDom] = List.empty[CBLSIntVarDom];
    //println("Mapping " + model.map.size + " variables")
    for ((varName, parsedVariable) <- model.map) {
      parsedVariable match {
        case ConcreteVariable(id, dom, annotation) =>
          val initialValue = (dom match {
            case DomainRange(min, max) =>
              val range = (min to max);
//              println(range.length+ "\t"+range+"\t"+varName+ "\t"+ parsedVariable)
              range(Random.nextInt(range.length))
            case DomainSet(values) =>
              val v = values.toArray;
              v(Random.nextInt(v.length))
          });
          val cblsVariable = CBLSIntVarDom(m, dom, initialValue, id);
          cblsIntMap += varName -> cblsVariable;
          if (/*parsedVariable.max != parsedVariable.min &&*/ !parsedVariable.isDefined) {
            searchVariables = cblsVariable :: searchVariables;
          }
        case _ => ()
      }
    }
    searchVariables;
  }
  def getSortedInvariants(inv: Array[Constraint]): Array[Constraint] = {
    var invariants = inv.toArray;
    var sorted = List.empty[Constraint];
    var mapping = MMap.empty[Constraint, Int];
    var heads = List.empty[Constraint]
    for (i <- invariants) {
      mapping += i -> i.getVariables.filter((v) => v.isDefined && (v != i.definedVar.get)).length;
      if(mapping(i)==0)heads = i :: heads;
    }
    while (!heads.isEmpty) {
      val k = heads.head
      heads = heads.tail
      sorted = k::sorted
      
      for(j <- k.definedVar.get.cstrs){
        if(mapping.contains(j) ){
          mapping(j) = mapping(j)-1
          if(mapping(j)==0)heads = j :: heads;
        }
      }
    }
    return sorted.reverse.toArray;
  }
  

  def handleSolution()(implicit model: FZProblem, cblsIntMap: MMap[String, CBLSIntVarDom]) = {
    model.solution.handleSolution(
      (s: String) => cblsIntMap.get(s) match {
        case Some(intVar) =>
          intVar.value + "";
        case _ => throw new Exception("Unhappy")
      });
  }
  def getSolution()(implicit model: FZProblem, cblsIntMap: MMap[String, CBLSIntVarDom]):String = {
    model.solution.getSolution(
      (s: String) => cblsIntMap.get(s) match {
        case Some(intVar) =>
          intVar.value + "";
        case _ => throw new Exception("Unhappy")
      });
  }
}