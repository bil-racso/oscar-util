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

package oscar.examples.cbls.queens

import oscar.cbls.modeling.CBLSSolver
import oscar.cbls.invariants.core.computation.{SetVar, IntVar}


/**
 * Created by rdl on 13/01/14.
 */
object NQueensEasy2 extends CBLSSolver with App{

  val N = 20

  println("NQueens(" + N + ")")

  val rand = new scala.util.Random()

  startWatch()

  // initial solution
  val init = rand.shuffle((0 to N-1).toList).toArray

  val queens = Array.tabulate(N)(q => intVar(0 to N-1,init(q),"queen_" + q))

  //alldiff on rows in enforced because we swap queens initially different
  c.add(allDifferent(Array.tabulate(N)(q => (queens(q) + q).toIntVar)))
  c.add(allDifferent(Array.tabulate(N)(q => (q - queens(q)).toIntVar)))

  val violationArray:Array[IntVar] = Array.tabulate(N)(q => c.violation(queens(q))).toArray

  val tabu:Array[IntVar] = Array.tabulate(N)(q => intVar(0 to Int.MaxValue, 0, "tabu_queen" + q))
  val it = intVar(0 to Int.MaxValue,1,"it")
  val nonTabuQueens:SetVar = selectLESetQueue(tabu, it)
  val nonTabuMaxViolQueens:SetVar = argMax(violationArray, nonTabuQueens)

  close()

  val tabulength = 3

  while((c.Violation.value > 0) && (it.value < N)){

    val oldviolation:Int = c.Violation.value

    // to ensure that the set of tabu queens is no too restrictive
    // (but you'd better tune the tabu better)
    while(nonTabuMaxViolQueens.value.isEmpty){
      it ++;
      println("Warning: Tabu it too big compared to queens count")
    }

    val q1 = selectFirst(nonTabuMaxViolQueens.value)
    val q2 = selectFirst(nonTabuQueens.value, (q:Int) => {
      q!=q1 && c.swapVal(queens(q1),queens(q)) < oldviolation
    })

    queens(q1) :=: queens(q2)
    tabu(q1) := it.value + tabulength
    tabu(q2) := it.value + tabulength

    //println("" + it + " swapping " + q1 + " and " + q2)

    it ++
  }

  println(getWatchString)
  println(it)
  println(queens.mkString(","))
}
