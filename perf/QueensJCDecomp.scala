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

import oscar.cp._
import oscar.algo.search.Branching

/**
 * n-queens model: place n-queens on a chess-board such that they don't attack each other.
 * @author Pierre Schaus pschaus@gmail.com
 */
object QueensJCDecomp {
  
  def main(args: Array[String]) {

    val cp = CPSolver()
    cp.silent = true
    val n = 88 //number of queens
    val Queens = 0 until n
    //variables
    val queens = for (i <- Queens) yield CPIntVar.sparse(0, n-1)(cp)

    var nbsol = 0

    val cl = Weak
    for (i <- 0 until n; j <- 0 until i) {
      cp.add(queens(i) != queens(j))
      cp.add(queens(i)+i != queens(j)+j)
      cp.add(queens(i)-i != queens(j)-j)
    }
    
    cp.search {
      binaryFirstFail(queens)
    }
    
    cp.onSolution {
      println(queens.mkString(","))
    }
    
    println(cp.start(nSols=1))
    println(cp.statistics)

  }  
 
}


