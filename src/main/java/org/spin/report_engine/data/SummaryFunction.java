/************************************************************************************
 * Copyright (C) 2012-2018 E.R.P. Consultores y Asociados, C.A.                     *
 * Contributor(s): Yamel Senih ysenih@erpya.com                                     *
 * This program is free software: you can redistribute it and/or modify             *
 * it under the terms of the GNU General Public License as published by             *
 * the Free Software Foundation, either version 2 of the License, or                *
 * (at your option) any later version.                                              *
 * This program is distributed in the hope that it will be useful,                  *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of                   *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the                     *
 * GNU General Public License for more details.                                     *
 * You should have received a copy of the GNU General Public License                *
 * along with this program. If not, see <https://www.gnu.org/licenses/>.            *
 ************************************************************************************/
package org.spin.report_engine.data;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

import org.compiere.util.DisplayType;
import org.compiere.util.Env;

/**
 * This class have all need for manage row summary and groups
 * @author Yamel Senih, ysenih@erpya.com, ERPCyA http://www.erpya.com
 */
public class SummaryFunction {
	/** The Sum				*/
	private BigDecimal sum;
	/** The Count			*/
	private int count;
	/** Minimum				*/
	private BigDecimal minimum;
	/** Maximum				*/
	private BigDecimal maximum;
	/** Sum of Squares		*/
	private BigDecimal sumSquare;
	/**	Display values	*/
	private Map<String, String> displayValues;
	
	/** Sum			*/
	static public final char		F_SUM = 'S';
	/** Mean		*/
	static public final char		F_MEAN = 'A';		//	Average mu
	/** Count		*/
	static public final char		F_COUNT = 'C';
	/** Min			*/
	static public final char		F_MIN = 'm';
	/** Max			*/
	static public final char		F_MAX = 'M';
	/** Variance	*/
	static public final char		F_VARIANCE = 'V';	//	sigma square
	/** Deviation	*/
	static public final char		F_DEVIATION = 'D';	//	sigma
	/** Function Keys							*/
	static private final char[]		FUNCTIONS = new char[]
		{F_SUM,     F_MEAN,    F_COUNT,   F_MIN,     F_MAX,     F_VARIANCE, F_DEVIATION};
	/** Symbols									*/
	static private final String[]	FUNCTION_SYMBOLS = new String[]
		{" \u03A3", " \u03BC", " \u2116", " \u2193", " \u2191", " \u03C3\u00B2", " \u03C3"};
	/**	AD_Message Names of Functions			*/
	static private final String[]	FUNCTION_NAMES = new String[]
		{"Sum",     "Mean",    "Count",   "Min",     "Max",     "Variance", "Deviation"};
	
	
	private SummaryFunction() {
		sum = Env.ZERO;
		sumSquare = Env.ZERO;
		count = 0;
		displayValues = new HashMap<String, String>();
	}
	
	public static SummaryFunction newInstance() {
		return new SummaryFunction();
	}

	
	/**
	 * 	Add Value to Counter
	 * 	@param value data
	 */
	public SummaryFunction addValue (BigDecimal value) {
		if (value != null) {
			//	Sum
			sum = sum.add(value);
			//	Count
			count++;
			//	Min
			if (minimum == null) {
				minimum = value;
			}
			minimum = minimum.min(value);
			//	Max
			if (maximum == null) {
				maximum = value;
			}
			maximum = maximum.max(value);
			//	Sum of Squares
			sumSquare = sumSquare.add (value.multiply(value));
		}
		return this;
	}
	
	/**
	 * 	Get Function Value
	 *  @param function function
	 *  @return function value
	 */
	public BigDecimal getValue(char function) {
		//	Sum
		if (function == F_SUM) {
			return sum;
		}
		//	Min/Max
		if (function == F_MIN) {
			return minimum;
		}
		if (function == F_MAX) {
			return maximum;
		}
		//	Count
		BigDecimal counter = new BigDecimal(count);
		if (function == F_COUNT) {
			return counter;
		}

		//	All other functions require count > 0
		if (count == 0) {
			return Env.ZERO;
		}

		//	Mean = sum/count - round to 4 digits
		if (function == F_MEAN) {
			BigDecimal mean = sum.divide(counter, 4, RoundingMode.HALF_UP);
			if (mean.scale() > 4) {
				mean = mean.setScale(4, RoundingMode.HALF_UP);
			}
			return mean;
		}
		//	Variance = sum of squares - (square of sum / count)
		BigDecimal ss = sum.multiply(sum);
		ss = ss.divide(counter, 4, RoundingMode.HALF_UP);
		BigDecimal variance = sumSquare.subtract(ss);
		if (function == F_VARIANCE) {
			if (variance.scale() > 4) {
				variance = variance.setScale(4, RoundingMode.HALF_UP);
			}
			return variance;
		}
		//	Standard Deviation
		BigDecimal deviation = new BigDecimal(Math.sqrt(variance.doubleValue()));
		if (deviation.scale() > 4) {
			deviation = deviation.setScale(4, RoundingMode.HALF_UP);
		}
		return deviation;
	}	//	getValue
	
	/**
	 * Get display value of function
	 * @param function
	 * @return
	 */
	public String getDisplayValue(char function) {
		return displayValues.get(String.valueOf(function));
	}
	
	/**
	 * Set display value for function
	 * @param function
	 * @param displayValue
	 */
	public void setDisplayValue(char function, String displayValue) {
		displayValues.put(String.valueOf(function), displayValue);
	}
	
	/*************************************************************************/

	/**
	 * 	Get Function Symbol of function
	 * 	@param function function
	 * 	@return function symbol
	 */
	static public String getFunctionSymbol (char function) {
		for (int i = 0; i < FUNCTIONS.length; i++) {
			if (FUNCTIONS[i] == function)
				return FUNCTION_SYMBOLS[i];
		}
		return "UnknownFunction=" + function;
	}	//	getFunctionSymbol

	/**
	 * 	Get Function Name of function
	 * 	@param function function
	 * 	@return function name
	 */
	static public String getFunctionName (char function) {
		for (int i = 0; i < FUNCTIONS.length; i++) {
			if (FUNCTIONS[i] == function)
				return FUNCTION_NAMES[i];
		}
		return "UnknownFunction=" + function;
	}	//	getFunctionName

	/**
	 * Get DisplayType of function
	 * @param function function
	 * @param displayType columns display type
	 * @return function name
	 */
	static public int getFunctionDisplayType (char function, int displayType) {
		if (function == F_SUM || function == F_MIN || function == F_MAX)
			return displayType;
		if (function == F_COUNT)
			return DisplayType.Integer;
		//	Mean, Variance, Std. Deviation 
		return DisplayType.Number;
	}
	
	@Override
	public String toString() {
		return "SummaryFunction [sum=" + sum + ", count=" + count + ", minimum=" + minimum + ", maximum=" + maximum
				+ ", sumSquare=" + sumSquare + "]";
	}
}
