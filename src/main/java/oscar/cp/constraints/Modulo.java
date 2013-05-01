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
package oscar.cp.constraints;

import java.util.Arrays;

import javax.management.RuntimeErrorException;

import oscar.cp.core.CPOutcome;
import oscar.cp.core.CPPropagStrength;
import oscar.cp.core.CPVarInt;
import oscar.cp.core.Constraint;
import oscar.reversible.ReversibleSetIndexedArray;


/** 
 * Modulo Constraint: x % v = y
 * @author pschaus@gmail.com
 */
public class Modulo extends Constraint{

	private CPVarInt x;
	private CPVarInt y;
	private int v;

	// for each value from 0 to v-1 how many values in D(x) support it ? When it fall to 0 the value can be removed from y	
	// for each value in D(y) what are the values in x supporting it (= set), when a value in D(y) is removed, all values from the set are removed as well from D(x)

	private ReversibleSetIndexedArray [] supportSet; // supportSet[i] is the set of values val in D(x) such that val%v == i

	/**
	 * Creates a modulo constraint x % v = y
	 * @param x
	 * @param v a value > 0
	 * @param y
	 */
	public Modulo(CPVarInt x, int v, CPVarInt y) {
		super(x.s());
		assert( v > 0);
		if (v <= 0) throw new RuntimeException("v must be > 0");
		this.x = x;
		this.v = v;
		this.y = y;
	}

	@Override
	protected CPOutcome setup(CPPropagStrength l) {
		if (y.updateMin(-v+1) == CPOutcome.Failure) {
			return CPOutcome.Failure;
		}
		if (y.updateMax(v-1) == CPOutcome.Failure) {
			return CPOutcome.Failure;
		}

		supportSet = new ReversibleSetIndexedArray[2*v-1]; // for negative and positive numbers if v = 3, we must consider -2,-1,0,1,2
		for (int i = -v+1; i < v; i++) {		
			for (int val = x.min(); val <= x.max(); val++) {
				if (x.hasValue(val)) {
					if ((val % v) == i) {
						if (supportSet[i+v-1] == null) {
							supportSet[i+v-1] = new ReversibleSetIndexedArray(s, x.getMin(), x.getMax(),true); // contains no values initially
						}
						supportSet[i+v-1].insert(val);
					}
				}
			}
			if (supportSet[i+v-1] == null || supportSet[i+v-1].isEmpty()) {
				if (y.removeValue(i) == CPOutcome.Failure) {
					return CPOutcome.Failure;
				}
			}
			//else {
			//	System.out.println("support for "+i+" :" +Arrays.toString(supportSet[i].getValues()));
			//}
		}
		for (int i = y.getMin(); i <= y.getMax(); i++) {
			if (!y.hasValue(i)) {
				if (valRemovedFromY(i) == CPOutcome.Failure) {
					return CPOutcome.Failure;
				}
			}
		}
		

		if (!x.isBound()) x.callValRemoveWhenValueIsRemoved(this);
		if (!y.isBound()) y.callValRemoveWhenValueIsRemoved(this);

		return CPOutcome.Suspend;
	}

	private CPOutcome valRemovedFromY(int val) {
		assert(val > -v && val < v);
		if (!supportSet[val+v-1].isEmpty()) {
			for (int j: supportSet[val+v-1]) {
				if (x.removeValue(j) == CPOutcome.Failure) {
					return CPOutcome.Failure;
				}
			}

		}
		return CPOutcome.Suspend;
	}

	@Override
	protected CPOutcome valRemove(CPVarInt var, int val) {
		if (var == x) {
			int i = val % v;
			supportSet[i+v-1].removeValue(val);
			if (supportSet[i+v-1].isEmpty()) {
				if (y.removeValue(i) == CPOutcome.Failure) {
					return CPOutcome.Failure;
				}
			}
			return CPOutcome.Suspend;
		} else { // var == y
			assert(val > -v && val < v);
			return valRemovedFromY(val);
		}

	}

}
