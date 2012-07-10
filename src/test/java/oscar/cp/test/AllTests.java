/*******************************************************************************
 * This file is part of OscaR (Scala in OR).
 *   
 * OscaR is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2.1 of the License, or
 * (at your option) any later version.
 *  
 * OscaR is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *  
 * You should have received a copy of the GNU General Public License along with OscaR.
 * If not, see http://www.gnu.org/licenses/gpl-3.0.html
 ******************************************************************************/
package oscar.cp.test;


import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * @author Pierre Schaus pschaus@gmail.com
 */
public class AllTests {

	public static Test suite() {
		TestSuite suite = new TestSuite("Test for test");
		//$JUnit-BEGIN$
        suite.addTestSuite(TestArrayUtils.class);

		//$JUnit-END$
		return suite;
	}

}
