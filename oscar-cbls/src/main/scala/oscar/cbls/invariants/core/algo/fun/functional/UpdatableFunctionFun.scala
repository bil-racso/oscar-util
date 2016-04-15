package oscar.cbls.invariants.core.algo.fun.functional

import oscar.cbls.invariants.core.algo.fun.mutable.LinearPositionTransform
import oscar.cbls.invariants.core.algo.rb.RedBlackTree

object PiecewiseLinearFun{
  def identity = new PiecewiseLinearFun()
}

class PiecewiseLinearFun(transformation: RedBlackTree[Pivot] = RedBlackTree.empty) {

  def firstPivot:Option[(Int,Pivot)] = transformation.getSmallestBiggerOrEqual(Int.MinValue)

  override def toString: String = {
    "PiecewiseLinearFun(nbSegments:" + transformation.size + ", " + (if(transformation.isEmpty) "identity" else ("segments:" + transformation.values.mkString(",")))+")"
  }

  def apply(value:Int):Int = {
    transformation.getBiggestLowerOrEqual(value) match {
      case None => value
      case Some((_,pivot)) => pivot.f(value)
    }
  }

  def update(fromIncluded: Int, toIncluded: Int, additionalF: LinearPositionTransform): PiecewiseLinearFun = {
    new PiecewiseLinearFun(updatePivots(fromIncluded, toIncluded, additionalF))
  }

  private def updatePivots(fromIncluded: Int, toIncluded: Int, additionalF: LinearPositionTransform): RedBlackTree[Pivot] = {
    println("updatePivots(from:" + fromIncluded + ", to:" + toIncluded + ", fct:" + additionalF + ")")
    transformation.getBiggestLowerOrEqual(fromIncluded) match {
      case Some((_,pivot)) if (pivot.fromValue == fromIncluded) =>
        updateFromPivot(pivot, toIncluded, additionalF, transformation)
      case Some((_,pivot)) =>
        //there is a pivot below the point
        //need to add an intermediary pivot, with same transform as previous one
        val newPivot = new Pivot(fromIncluded, pivot.f)
        updateFromPivot(newPivot, toIncluded, additionalF, transformation.insert(fromIncluded, newPivot))
      case None =>
        transformation.getSmallestBiggerOrEqual(fromIncluded) match{
          case None =>
            //need to add a first pivot from this point
            val newPivot = new Pivot(fromIncluded, LinearPositionTransform.identity)
            updateFromPivot(newPivot, toIncluded, additionalF, transformation.insert(fromIncluded, newPivot))
          case Some((_,next)) =>
            val newPivot = new Pivot(fromIncluded, LinearPositionTransform.identity)
            updateFromPivot(newPivot, toIncluded, additionalF,transformation.insert(fromIncluded, newPivot))
        }
    }
  }

  private def updateFromPivot(pivot: Pivot, toIncluded: Int, additionalF: LinearPositionTransform, transformation: RedBlackTree[Pivot]):RedBlackTree[Pivot] = {
    if (pivot.fromValue == toIncluded+1) return transformation //finished the correction

    val previousCorrection = pivot.f
    val newCorrection = additionalF(previousCorrection) //TODO: vérifier que c'est pas l'inverse

    val transformWithNewPivot = transformation.insert(pivot.fromValue,new Pivot(pivot.fromValue,newCorrection))

    val prevPivot = transformWithNewPivot.getBiggestLowerOrEqual(pivot.fromValue-1)

    val removeCurrentPivot = prevPivot match{
      case None => newCorrection.isIdentity
      case Some((fromValue,pivot)) =>
        pivot.f.equals(newCorrection)
    }

    val(newPrev,newTransform) = if(removeCurrentPivot){
      (prevPivot,transformWithNewPivot.remove(pivot.fromValue))
    }else{
      (Some(pivot.fromValue,pivot),transformWithNewPivot)
    }

    newTransform.getSmallestBiggerOrEqual(pivot.fromValue+1) match{
      case None =>
        if (newPrev == null) newTransform
        //We have an open correction, and need to close it with the previous value previousCorrection
        else newTransform.insert(toIncluded+1, new Pivot(toIncluded+1, previousCorrection))
      case Some((nextFromValue,nextPivot)) =>
        if (nextFromValue > toIncluded + 1) {
          //need to add a new intermediary pivot
          println("coucou")
          newPrev match{
            case None =>
              println("no new prev")
              newTransform
            case Some((newPrevFromValue,newPrevPivot)) =>
              println("some new prev:" + newPrevPivot)
              if (newPrevPivot.f.equals(previousCorrection)) {
                println("case 1")
                newTransform
              }else {
                println("case2")
                newTransform.insert(toIncluded + 1, new Pivot(toIncluded + 1, previousCorrection))
              }
          }
        } else if (nextFromValue < toIncluded+1){
          //there is a next such that next.value is <= correctedTo
          //so recurse to it
          updateFromPivot(nextPivot, toIncluded, additionalF,newTransform)
        }else {
          //check that next pivot should not be removed, actually
          newPrev match {
            case None if nextPivot.f.isIdentity => newTransform.remove(nextFromValue)
            case Some((newPrevFromValue, newPrevPivot)) if nextPivot.f.equals(newPrevPivot.f) => newTransform.remove(nextFromValue)
            case _ => newTransform
          }
        }
    }
  }
}

class Pivot(val fromValue:Int, val f: LinearPositionTransform){
  override def toString = "Pivot(from:" + fromValue + " f:" + f + ")"
}
