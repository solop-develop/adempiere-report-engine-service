/************************************************************************************
 * Copyright (C) 2018-present E.R.P. Consultores y Asociados, C.A.                  *
 * Contributor(s): Edwin Betancourt, EdwinBetanc0urt@outlook.com                    *
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

package org.spin.service.grpc.util.value;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Map;
import java.util.TimeZone;

import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.compiere.util.TimeUtil;
import org.compiere.util.Util;

import com.google.protobuf.Struct;
import com.google.protobuf.Value;

/**
 * Class for handle Time (TimesTamp, Date, Long, Instant) values
 * @author Edwin Betancourt, EdwinBetanc0urt@outlook.com, https://github.com/EdwinBetanc0urt
 */
public class TimeManager {

	/**	Date format	*/
	private static final String DATE_FORMAT = "yyyy-MM-dd";
	private static final String TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
	private static final String TIMEZONE_FORMAT = "yyyy-MM-ddTHH:mm:ss.000Z";


	public static SimpleDateFormat FORMAT = DisplayType.getDateFormat(
		DisplayType.DateTime,
		Env.getLanguage(Env.getCtx())
	);


	/**
	 * Validate Date
	 * @param value
	 * @return
	 */
	public static boolean isDate(String value) {
		return getTimestampFromString(value) != null;
	}


	/**
	 * Get Date
	 * @return
	 */
	public static Timestamp getDate() {
		return TimeUtil.getDay(System.currentTimeMillis());
	}


	/**
	 * 	Return DateTime + last time
	 * 	@param dateTime Date and Time
	 * 	@param offset minute offset
	 * 	@return dateTime + offset in minutes
	 */
	static public Timestamp getDayLastTime(Timestamp dateTime) {
		if (dateTime == null) {
			dateTime = new Timestamp(System.currentTimeMillis());
		}
		//
		GregorianCalendar cal = new GregorianCalendar();
		cal.setTime(dateTime);
		cal.set(Calendar.HOUR_OF_DAY, 23);
		cal.set(Calendar.MINUTE, 59);
		cal.set(Calendar.SECOND, 59);
		cal.set(Calendar.MILLISECOND, 999);
		return new Timestamp (cal.getTimeInMillis());
	}	//	addMinutes



	public static Timestamp getTimestampFromObject(Object value) {
		Timestamp dateValue = null;
		if (value == null) {
			return dateValue;
		}
		if (value instanceof Timestamp) {
			dateValue = (Timestamp) value;
		} else if (value instanceof Date) {
			dateValue = getTimestampFromDate(
				(Date) value
			);
		} else if (value instanceof Long) {
			dateValue = TimeManager.getTimestampFromLong(
				(Long) value
			);
		} else if (value instanceof Instant) {
			dateValue = TimeManager.getTimestampFromInstant(
				(Instant) value
			);
		} else if (value instanceof LocalDateTime) {
			dateValue = TimeManager.getTimestampFromLocalDateTime(
				(LocalDateTime) value
			);
		}else if (value instanceof Integer) {
			dateValue = TimeManager.getTimestampFromInteger(
				(Integer) value
			);
		} else if (value instanceof Double) {
			dateValue = TimeManager.getTimestampFromDouble(
				(Double) value
			);
		} else if (value instanceof Float) {
			dateValue = TimeManager.getTimestampFromFloat(
				(Float) value
			);
		} else if (value instanceof String) {
			final String stringValue = (String) value;
			final Integer integerValue = NumberManager.getIntegerFromString(stringValue);
			final BigDecimal bigDecimalValue = NumberManager.getBigDecimalFromString(stringValue);
			if (integerValue != null) {
				dateValue = TimeManager.getTimestampFromInteger(integerValue);
			} else if (bigDecimalValue != null) {
				dateValue = TimeManager.getTimestampFromBigDecimal(bigDecimalValue);
			} else {
				dateValue = TimeManager.getTimestampFromString(
					stringValue
				);
			}
		} else if (value instanceof BigDecimal) {
			dateValue = TimeManager.getTimestampFromBigDecimal(
				(BigDecimal) value
			);
		} else if (value instanceof Number) {
			dateValue = TimeManager.getTimestampFromNumber(
				(Number) value
			);
		}
		return dateValue;
	}


	/**
	 * Convert string to dates
	 * @param stringValue
	 * @return
	 */
	public static Timestamp getTimestampFromString(String stringValue) {
		if (Util.isEmpty(stringValue, true)) {
			return null;
		}

		String format = DATE_FORMAT;
		if(stringValue.length() == TIME_FORMAT.length()) {
			format = TIME_FORMAT;
		} else if (stringValue.length() == TIMEZONE_FORMAT.length()){
			try{
				return getTimestampFromInstant(stringValue);
			} catch (Exception e) {
				//
			}
		} else if(stringValue.length() != DATE_FORMAT.length() && stringValue.length() != TIMEZONE_FORMAT.length() && stringValue.length() != TIMEZONE_FORMAT.length()) {
			// throw new AdempiereException(
			// 	"Invalid date format, please use some like this: \"" + DATE_FORMAT + "\" or \"" + TIME_FORMAT + "\"" or \"" + TIMEZONE_FORMAT + "\""
			// );
		}

		Date validDate = null;
		try {
			SimpleDateFormat dateConverter = new SimpleDateFormat(format);
			validDate = dateConverter.parse(stringValue);
		} catch (ParseException e) {
			// throw new AdempiereException(e);
		}

		//	Convert
		if(validDate != null) {
			return getTimestampFromLong(
				validDate.getTime()
			);
		}
		return null;
	}

	/**
	 * Convert Timestamp to String
	 * @param date
	 * @return
	 */
	public static String getTimestampToString(Timestamp date) {
		if(date == null) {
			return null;
		}
		return getTimestampToString(date, TIME_FORMAT);
	}
	/**
	 * Convert Timestamp to String
	 * @param date
	 * @param pattern default `yyyy-MM-dd hh:mm:ss`
	 * @return
	 */
	public static String getTimestampToString(Timestamp date, String pattern) {
		if(date == null) {
			return null;
		}
		if (Util.isEmpty(pattern, true)) {
			pattern = TIME_FORMAT;
		}
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
		simpleDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
		String timeString = simpleDateFormat.format(date);
		return timeString;
	}

	/**
	 * Convert Timestamp to String
	 * @param value
	 * @return
	 */
	public static Timestamp getTimestampFromDate(Date value) {
		if(value == null) {
			return null;
		}
		return new Timestamp(
			value.getTime()
		);
	}

	public static Timestamp getTimestampFromLong(long value) {
		if (value > 0) {
			return new Timestamp(value);
		}
		return null;
	}

	public static Timestamp getTimestampFromLocalDateTime(LocalDateTime value) {
		if (value == null) {
			return null;
		}
		return Timestamp.valueOf(value);
	}

	public static Timestamp getTimestampFromInstant(Instant value) {
		if (value == null) {
			return null;
		}
		return new Timestamp(value.toEpochMilli());
	}
	public static Timestamp getTimestampFromInstant(String value) {
		Instant instant = getInstantFromString(value);
		if (instant == null) {
			return null;
		}
		return getTimestampFromInstant(instant);
	}

	public static Timestamp getTimestampFromNumber(Number value) {
		Timestamp dateValue = null;
		if (value == null) {
			return dateValue;
		}
		if (value instanceof Long) {
			dateValue = TimeManager.getTimestampFromLong(
				(Long) value
			);
		} else if (value instanceof Integer) {
			return getTimestampFromInteger(
				(Integer) value
			);
		} else if (value instanceof Double) {
			dateValue = TimeManager.getTimestampFromDouble(
				(Double) value
			);
		} else if (value instanceof Float) {
			return getTimestampFromFloat(
				(Float) value
			);
		} else if (value instanceof BigDecimal) {
			dateValue = TimeManager.getTimestampFromBigDecimal(
				(BigDecimal) value
			);
		}
		return dateValue;
	}

	public static Timestamp getTimestampFromDouble(double value) {
		if (value > 0.0) {
			return new Timestamp(
				(int) value
			);
		}
		return null;
	}

	public static Timestamp getTimestampFromFloat(Float value) {
		if (value == null || value < 0.0f) {
			return null;
		}
		return new Timestamp(
			value.longValue()
		);
	}

	public static Timestamp getTimestampFromBigDecimal(BigDecimal value) {
		if (value == null || value.compareTo(BigDecimal.ZERO) < 0) {
			return null;
		}
		// final int decimalPointIndex = value.toString().indexOf(".");
		// if (decimalPointIndex > 0) {
		// 	value = value.toString().substring(0, decimalPointIndex);
		// }
		// long longValue = new BigDecimal(
		// 	value.toString()
		// ).longValue();
		final long longValue = value
			.setScale(0, RoundingMode.HALF_UP)
			.longValueExact()
		;
		return new Timestamp(longValue);
	}

	public static Timestamp getTimestampFromInteger(int value) {
		if (value > 0) {
			return new Timestamp(value);
		}
		return null;
	}


	/**
	 * Get long from Timestamp
	 * @param value
	 * @return
	 */
	public static long getLongFromTimestamp(Timestamp value) {
		if (value == null) {
			return 0L;
		}
		return value.getTime();
	}



	public static Instant getInstantFromString(String value) {
		if (!Util.isEmpty(value, true)) {
			return Instant.parse(value);
		}
		return null;
	}


	/**
	 * Validate if a value is date
	 * @param value
	 * @return
	 */
	public static boolean isDateProtoValue(Value value) {
		if (value == null) {
			return false;
		}
		Map<String, Value> values = value.getStructValue().getFieldsMap();
		if(values == null) {
			return false;
		}
		Value type = values.get(ValueManager.TYPE_KEY);
		if(type == null) {
			return false;
		}
		String validType = TextManager.getValidString(
			type.getStringValue()
		);
		return validType.equals(ValueManager.TYPE_DATE) || validType.equals(ValueManager.TYPE_DATE_TIME);
	}

	/**
	 * @deprecated Use {@link TimeManager#getProtoTimestampFromTimestamp(Timestamp)} instead.
	 * @param value
	 * @return com.google.protobuf.Timestamp
	 */
	@Deprecated
	public static com.google.protobuf.Timestamp convertDateToValue(Timestamp value) {
		return TimeManager.getProtoTimestampFromTimestamp(value);
	}
	/**
	 * Get google.protobuf.Timestamp from Timestamp
	 * @param dateValue
	 * @return com.google.protobuf.Timestamp
	 */
	public static com.google.protobuf.Timestamp getProtoTimestampFromTimestamp(Timestamp dateValue) {
		Timestamp minDate = TimeManager.getTimestampFromProtoTimestamp(com.google.protobuf.util.Timestamps.MIN_VALUE);
		if (dateValue == null || minDate.equals(dateValue)) {
			// return com.google.protobuf.Timestamp.newBuilder().build(); // 1970-01-01T00:00:00Z
			// return com.google.protobuf.Timestamp.getDefaultInstance(); // 1970-01-01T00:00:00Z
			// return com.google.protobuf.util.Timestamps.EPOCH; // 1970-01-01T00:00:00Z
			return com.google.protobuf.util.Timestamps.MIN_VALUE; // 0001-01-01T00:00:00Z
		}
		return com.google.protobuf.util.Timestamps.fromMillis(
			dateValue.getTime()
		);
	}

	/**
	 * Get Date from value
	 * @param dateValue
	 * @return com.google.protobuf.Timestamp
	 */
	public static Timestamp getTimestampFromProtoTimestamp(com.google.protobuf.Timestamp dateValue) {
		if(dateValue == null || (dateValue.getSeconds() == 0 && dateValue.getNanos() == 0)) {
			return null;
		}
		LocalDateTime dateTime = LocalDateTime.ofEpochSecond(
			dateValue.getSeconds(),
			dateValue.getNanos(),
			ZoneOffset.UTC
		);
		return Timestamp.valueOf(dateTime);
	}

	/**
	 * Get Date from a value
	 * @param dateValue
	 * @return
	 */
	public static Timestamp getTimestampFromProtoValue(Value dateValue) {
		if(dateValue == null
				|| dateValue.hasNullValue()
				|| !(dateValue.hasStringValue() || dateValue.hasNumberValue() || dateValue.hasStructValue())) {
			return null;
		}

		if (dateValue.hasStructValue()) {
			Map<String, Value> values = dateValue.getStructValue().getFieldsMap();
			if(values == null) {
				return null;
			}
			Value type = values.get(ValueManager.TYPE_KEY);
			Value value = values.get(ValueManager.VALUE_KEY);
			if(type == null || value == null) {
				return null;
			}
			String validType = TextManager.getValidString(
				type.getStringValue()
			);
			String validValue = TextManager.getValidString(
				value.getStringValue()
			);
			if((!validType.equals(ValueManager.TYPE_DATE)
					&& !validType.equals(ValueManager.TYPE_DATE_TIME))
					|| validValue.length() == 0) {
				return null;
			}
			return TimeManager.getTimestampFromString(
				validValue
			);
		}
		if (dateValue.hasStringValue()) {
			return TimeManager.getTimestampFromString(
				dateValue.getStringValue()
			);
		}
		if (dateValue.hasNumberValue()) {
			return TimeManager.getTimestampFromDouble(
				dateValue.getNumberValue()
			);
		}
		return null;
	}

	/**
	 * Get value from a date
	 * @param value
	 * @return Value.Builder
	 */
	public static Value.Builder getProtoValueFromTimestamp(Timestamp value) {
		Struct.Builder date = Struct.newBuilder();
		date.putFields(
			ValueManager.TYPE_KEY,
			Value.newBuilder().setStringValue(
				ValueManager.TYPE_DATE
			).build()
		);

		Value.Builder valueBuilder = Value.newBuilder();
		if (value == null) {
			valueBuilder = ValueManager.getProtoValueFromNull();
		} else {
			String valueString = TimeManager.getTimestampToString(
				value
			);
			valueBuilder.setStringValue(
				valueString
			);
		}

		date.putFields(
			ValueManager.VALUE_KEY,
			valueBuilder.build()
		);
		return Value.newBuilder().setStructValue(date);
	}

}
