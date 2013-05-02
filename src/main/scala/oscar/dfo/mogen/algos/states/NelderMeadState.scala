package oscar.dfo.mogen.algos.states

import oscar.util.mo.MOOComparator
import oscar.util.mo.MOOPoint
import oscar.util.mo.MOEvaluator
import oscar.util.mo.RandomGenerator
import oscar.util.mo.FeasibleRegion
import oscar.dfo.mogen.algos.utils.Simplex

class NelderMeadState[E <% Ordered[E]](simplexInit: Array[MOOPoint[E]]) extends ComparativeAlgorithmState[E] with Simplex[E] {
  val simplex = simplexInit
  var bestPoint = simplex(0)

  var deltaR = 1
  var deltaE = 2
  var deltaOC = 0.5
  var deltaIC = -0.5
  var gammaS = 0.5
  
  def getNewState(newBestPoint: MOOPoint[E], comparator: MOOComparator[E]): ComparativeAlgorithmState[E] = {
    val newState = NelderMeadState(simplex)
    newState.deltaR = this.deltaR
    newState.deltaE = this.deltaE
    newState.deltaOC = this.deltaOC
    newState.deltaIC = this.deltaIC
    newState.gammaS = this.gammaS
    newState.bestPoint = newBestPoint
    orderSimplex(comparator)
    newState
  }
  
  def getReflection(evaluator: MOEvaluator[E], feasibleReg: FeasibleRegion, centroid: Array[Double] = getCentroid): MOOPoint[E] = getSinglePointTransformation(centroid, deltaR, evaluator, feasibleReg)
  
  def getExpansion(evaluator: MOEvaluator[E], feasibleReg: FeasibleRegion, centroid: Array[Double] = getCentroid): MOOPoint[E] = getSinglePointTransformation(centroid, deltaE, evaluator, feasibleReg)
  
  def getInsideContraction(evaluator: MOEvaluator[E], feasibleReg: FeasibleRegion, centroid: Array[Double] = getCentroid): MOOPoint[E] = getSinglePointTransformation(centroid, deltaIC, evaluator, feasibleReg)
  
  def getOutsideContraction(evaluator: MOEvaluator[E], feasibleReg: FeasibleRegion, centroid: Array[Double] = getCentroid): MOOPoint[E] = getSinglePointTransformation(centroid, deltaOC, evaluator, feasibleReg)
  
  def getSinglePointTransformation(centroid: Array[Double], factor: Double, evaluator: MOEvaluator[E], feasibleReg: FeasibleRegion): MOOPoint[E] = {
    val newCoordinates = arraySum(centroid, arrayProd(arrayDiff(centroid, worstPoint.coordinates), factor))
    evaluator.eval(newCoordinates, feasibleReg)
  }
  
  def applySinglePointTransformation(newPoint: MOOPoint[E], comparator: MOOComparator[E]) = {
    simplex(simplexSize - 1) = newPoint
    orderSimplex(comparator)
  }
  
  def getCentroid: Array[Double] = {
    val allButWorstCoordinates = simplex.map(mooP => mooP.coordinates).take(simplexSize - 1)
    arrayProd(allButWorstCoordinates.drop(1).foldLeft(allButWorstCoordinates(0))((acc, newCoords) => arraySum(acc, newCoords)), 1.0 / (simplexSize - 1))
  }
  
  def applyShrink(comparator: MOOComparator[E], evaluator: MOEvaluator[E], feasibleReg: FeasibleRegion) = {
    val simplexCoordinates = simplex.map(mooP => mooP.coordinates).drop(1)
    for (i <- 1 until simplexSize - 1) {
      simplex(i) = evaluator.eval(arrayProd(simplexCoordinates(i), gammaS), feasibleReg)
    }
    orderSimplex(comparator)
  }
  
  def printSimplex = {
    println("=" * 80)
    for (i <- 0 until simplexSize)
      println(i + ": " + simplex(i).toString)
  }
  
}

object NelderMeadState {
  def apply[E <% Ordered[E]](simplex: Array[MOOPoint[E]]) = new NelderMeadState(simplex)
  
  def apply[E <% Ordered[E]](coordinates: Array[Double], startIntervals: Array[(Double, Double)], evaluator: MOEvaluator[E], feasReg: FeasibleRegion, comparator: MOOComparator[E]): ComparativeAlgorithmState[E] = {
    val simplex = Array.tabulate(coordinates.length + 1){ index =>
      if (index == 0) coordinates
      else {
        val randPerturbation = startIntervals.map(e => (0.5 - RandomGenerator.nextDouble) * math.abs(e._2 - e._1) * 0.5)
        Array.tabulate(coordinates.length)(i => coordinates(i) + randPerturbation(i))
      }
    }
    NelderMeadState(simplex.map(coord => evaluator.eval(coord, feasReg)))
  }
}