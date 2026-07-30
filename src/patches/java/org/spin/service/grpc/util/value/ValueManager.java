/*************************************************************************************
 * Product: Adempiere ERP & CRM Smart Business Solution                              *
 * This program is free software; you can redistribute it and/or modify it           *
 * under the terms version 2 or later of the GNU General Public License as published *
 * by the Free Software Foundation. This program is distributed in the hope          *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied        *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.                  *
 * See the GNU General Public License for more details.                              *
 * You should have received a copy of the GNU General Public License along           *
 * with this program; if not, write to the Free Software Foundation, Inc.,           *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                            *
 * For the text or an alternative of this public license, you may reach us           *
 * Copyright (C) 2012-2023 E.R.P. Consultores y Asociados, S.A. All Rights Reserved. *
 * Contributor(s): Yamel Senih www.erpya.com                                         *
 *************************************************************************************/
package org.spin.service.grpc.util.value;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.adempiere.core.domains.models.I_C_Order;
import org.compiere.model.MLookup;
import org.compiere.model.MLookupFactory;
import org.compiere.model.MLookupInfo;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.compiere.util.Language;
import org.compiere.util.NamePair;
import org.compiere.util.TimeUtil;

import com.google.protobuf.Value;

/**
 * Class for handle Values from and to client
 * @author Yamel Senih, ysenih@erpya.com , http://www.erpya.com
 * @author Edwin Betancourt, EdwinBetanc0urt@outlook.com, https://github.com/EdwinBetanc0urt
 */
public class ValueManager {

	public static final String TYPE_KEY = "type";
	public static final String VALUE_KEY = "value";
	//	Types
	public static final String TYPE_DATE = "date";
	public static final String TYPE_DATE_TIME = "date_time";
	public static final String TYPE_DECIMAL = "decimal";


	/**
	 * Get Value
	 * @deprecated Use {@link ValueManager#getProtoValueFromObject(Object)} instead.
	 * @param value
	 * @return
	 */
	@Deprecated
	public static Value.Builder getValueFromObject(Object value) {
		return getProtoValueFromObject(value);
	}
	/**
	 * Get Proto Value
	 * @param value
	 * @return
	 */
	public static Value.Builder getProtoValueFromObject(Object value) {
		Value.Builder builder = Value.newBuilder();
		if(value == null) {
			return getProtoValueFromNull();
		}
		// if (value instanceof Value) {
		// 	builder.mergeFrom((Value) value);
		// 	return builder;
		// } else if (value instanceof Value.Builder) {
		// 	return (Value.Builder) value;
		// }
		//	Validate value
		if(value instanceof BigDecimal) {
			// TODO: Add support to `Float` and `Double`
			return NumberManager.getProtoValueFromBigDecimal((BigDecimal) value);
		} else if (value instanceof Integer) {
			return NumberManager.getProtoValueFromInteger((Integer)value);
		} else if (value instanceof String) {
			return TextManager.getProtoValueFromString((String) value);
		} else if (value instanceof Boolean) {
			return BooleanManager.getProtoValueFromBoolean((Boolean) value);
		} else if(value instanceof Timestamp) {
			// TODO: Add support to `Long`
			return TimeManager.getProtoValueFromTimestamp((Timestamp) value);
		} else if (value instanceof Map) {
			return CollectionManager.getProtoValueFromMap(
				(Map<?, ?>) value
			);
		} else if (value instanceof List) {
			// TODO: Add support to `Enum`
			return CollectionManager.getProtoValueFromList(
				(List<?>) value
			);
		}
		//	
		return builder;
	}

	/**
	 * @deprecated Use {@link ValueManager#getObjectFromProtoValue(Object)} instead. With recursive support
	 * @param value
	 * @return Object
	 */
	@Deprecated
	public static Object getObjectFromValue(Value value) {
		return ValueManager.getObjectFromProtoValue(value);
	}
	/**
	 * Convert Object to a Proto Value
	 * @param value
	 * @return
	 */
	public static Object getObjectFromProtoValue(Value value) {
		return ValueManager.getObjectFromProtoValue(value, false);
	}
	/**
	 * @deprecated Use {@link ValueManager#getObjectFromProtoValue(Object, boolean)} instead. With recursive support
	 * @param value
	 * @return Object
	 */
	@Deprecated
	public static Object getObjectFromValue(Value value, boolean uppercase) {
		return ValueManager.getObjectFromProtoValue(value, uppercase);
	}
	/**
	 * Convert Object to a Proto Value
	 * @param value
	 * @return
	 */
	public static Object getObjectFromProtoValue(Value value, boolean uppercase) {
		if(value == null || value.hasNullValue()) {
			return null;
		}
		if(value.hasStringValue()) {
			return TextManager.getStringFromProtoValue(value, uppercase);
		}
		if(value.hasNumberValue()) {
			return (int) value.getNumberValue();
		}
		if(value.hasBoolValue()) {
			return value.getBoolValue();
		}
		if(value.hasStructValue()) {
			if(NumberManager.isDecimalProtoValue(value)) {
				return NumberManager.getBigDecimalFromProtoValue(value);
			} else if(TimeManager.isDateProtoValue(value)) {
				return TimeManager.getTimestampFromProtoValue(value);
			}
			return CollectionManager.getMapFromProtoValue(value);
		}
		if(value.hasListValue()) {
			return CollectionManager.getListFromProtoValue(
				value
			);
		}
		return null;
	}

	/**
	 * @deprecated Use {@link ValueManager#getObjectFromProtoValue(Object, int)} instead. With recursive support
	 * @param value
	 * @return Object
	 */
	@Deprecated
	public static Object getObjectFromReference(Value value, int referenceId) {
		return getObjectFromProtoValue(value, referenceId);
	}
	/**
	 * Get Object from value based on reference
	 * @param value
	 * @param referenceId
	 * @return
	 */
	public static Object getObjectFromProtoValue(Value value, int displayTypeId) {
		if(value == null) {
			return null;
		}
		if (displayTypeId <= 0) {
			Object objectValue = ValueManager.getObjectFromProtoValue(value);
			return objectValue;
		}
		//	Validate values
		if(DisplayType.isID(displayTypeId) || DisplayType.Integer == displayTypeId) {
			if (DisplayType.Search == displayTypeId || DisplayType.Table == displayTypeId) {
				Object lookupValue = ValueManager.getObjectFromProtoValue(value);
				try {
					// casteable for integer, except `AD_Language`, `EntityType`
					lookupValue = Integer.valueOf(
						lookupValue.toString()
					);
				} catch (Exception e) {
				}
				return lookupValue;
			}
			int integerValue = NumberManager.getIntegerFromProtoValue(value);
			return integerValue;
		} else if(DisplayType.isNumeric(displayTypeId)) {
			BigDecimal bigDecimalValue = NumberManager.getBigDecimalFromProtoValue(value);
			return bigDecimalValue;
		} else if(DisplayType.YesNo == displayTypeId) {
			boolean booleanValue = BooleanManager.getBooleanFromProtoValue(value);
			return booleanValue;
		} else if(DisplayType.isDate(displayTypeId)) {
			Timestamp timestampValue = TimeManager.getTimestampFromProtoValue(value);
			return timestampValue;
		} else if(DisplayType.isText(displayTypeId) || DisplayType.List == displayTypeId) {
			String stringValue = TextManager.getStringFromProtoValue(value);
			return stringValue;
		} else if (DisplayType.Button == displayTypeId) {
			Object objectValue = ValueManager.getObjectFromProtoValue(value);
			return objectValue;
		}
		//	
		Object objectValue = ValueManager.getObjectFromProtoValue(value);
		return objectValue;
	}



	/**
	 * @deprecated Use {@link ValueManager#getProtoValueFromMap(Object)} instead. With recursive support
	 * @param values
	 * @return
	 */
	@Deprecated
	public static Value.Builder convertObjectMapToStruct(Map<String, Object> values) {
		return CollectionManager.getProtoValueFromMap(values);
	}



	/**
	 * Get NULL Value object from NULL
	 * @deprecated Use {@link ValueManager#getProtoValueFromNull(Object)} instead. With recursive support
	 * @param nullValue
	 * @return Value.Builder
	 */
	@Deprecated
	public static Value.Builder getValueFromNull(Object nullValue) {
		return getProtoValueFromNull();
	}
	/**
	 * Get NULL Value object from NULL
	 * @param nullValue
	 * @return
	 */
	public static Value.Builder getProtoValueFromNull(Object nullValue) {
		return getProtoValueFromNull();
	}
	/**
	 * Get NULL Value object from NULL
	 * @deprecated Use {@link ValueManager#getValueFromNull()} instead. With recursive support
	 * @return Value.Builder
	 */
	@Deprecated
	public static Value.Builder getValueFromNull() {
		return getProtoValueFromNull();
	}
	/**
	 * Get NULL Value object from NULL
	 * @return Value.Builder
	 */
	public static Value.Builder getProtoValueFromNull() {
		return Value.newBuilder().setNullValue(
			com.google.protobuf.NullValue.NULL_VALUE
		);
	}


	/**
	 * Get Empty Value object from DisplayType
	 * @deprecated Use {@link ValueManager#getEmptyProtoValueByReference()} instead. With recursive support
	 * @return Value.Builder
	 */
	@Deprecated
	public static Value.Builder getEmptyValueByReference(int displayTypeId) {
		return ValueManager.getEmptyProtoValueByReference(displayTypeId);
	}
	/**
	 * Get default empty value
	 * @param displayTypeId
	 * @return
	 */
	public static Value.Builder getEmptyProtoValueByReference(int displayTypeId) {
		if (displayTypeId <= 0) {
			return getProtoValueFromNull();
		}
		if (DisplayType.isID(displayTypeId) || DisplayType.Integer == displayTypeId) {
			int emptyId = 0;
			return NumberManager.getProtoValueFromInteger(emptyId);
		} else if (DisplayType.isNumeric(displayTypeId)) {
			return NumberManager.getProtoValueFromBigDecimal(null);
		} else if (DisplayType.isDate(displayTypeId)) {
			return TimeManager.getProtoValueFromTimestamp(null);
		} else if (DisplayType.isText(displayTypeId) || DisplayType.List == displayTypeId) {
			;
		} else if (DisplayType.YesNo == displayTypeId) {
			return BooleanManager.getProtoValueFromBoolean(false);
		}
		return getProtoValueFromNull();
	}



	/**
	 * @deprecated Use {@link ValueManager#getProtoValueFromInteger(Integer)} instead.
	 * @param value
	 * @return Value.Builder
	 */
	@Deprecated
	public static Value.Builder getValueFromInteger(Integer value) {
		return NumberManager.getProtoValueFromInteger(value);
	}
	/**
	 * @deprecated Use {@link ValueManager#getProtoValueFromInt(Integer)} instead.
	 * @param value
	 * @return Value.Builder
	 */
	@Deprecated
	public static Value.Builder getValueFromInt(int value) {
		return NumberManager.getProtoValueFromInt(value);
	}

	/**
	 * @deprecated Use {@link ValueManager#getIntegerFromProtoValue(Value)} instead.
	 * @param value
	 * @return int
	 */
	@Deprecated
	public static int getIntegerFromValue(Value value) {
		return NumberManager.getIntegerFromProtoValue(value);
	}



	/**
	 * @deprecated Use {@link ValueManager#getProtoValueFromString(String)} instead.
	 * @param value
	 * @return Value.Builder
	 */
	@Deprecated
	public static Value.Builder getValueFromString(String value) {
		return TextManager.getProtoValueFromString(value);
	}

	/**
	 * @deprecated Use {@link ValueManager#getStringFromProtoValue(Value)} instead.
	 * @param value
	 * @return
	 */
	@Deprecated
	public static String getStringFromValue(Value value) {
		return TextManager.getStringFromProtoValue(value, false);
	}
	/**
	 * @deprecated Use {@link ValueManager#getStringFromProtoValue(Value, boolean)} instead.
	 * @param value
	 * @param uppercase
	 * @return
	 */
	@Deprecated
	public static String getStringFromValue(Value value, boolean uppercase) {
		return TextManager.getStringFromProtoValue(value, uppercase);
	}



	/**
	 * @deprecated Use {@link ValueManager#getProtoValueFromBoolean(boolean)} instead. With recursive support
	 * @param value
	 * @return Value.Builder
	 */
	@Deprecated
	public static Value.Builder getValueFromBoolean(boolean value) {
		return BooleanManager.getProtoValueFromBoolean(value);
	}
	/**
	 * @deprecated Use {@link ValueManager#getProtoValueFromBoolean(Boolean)} instead. With recursive support
	 * @param value
	 * @return Value.Builder
	 */
	@Deprecated
	public static Value.Builder getValueFromBoolean(Boolean value) {
		return BooleanManager.getProtoValueFromBoolean(value);
	}
	/**
	 * @deprecated Use {@link ValueManager#getProtoValueFromBoolean(String)} instead. With recursive support
	 * @param value
	 * @return Value.Builder
	 */
	@Deprecated
	public static Value.Builder getValueFromStringBoolean(String value) {
		return BooleanManager.getProtoValueFromBoolean(value);
	}

	/**
	 * @deprecated Use {@link ValueManager#getBooleanFromProtoValue(String)} instead. With recursive support
	 * @param value
	 * @return Value.Builder
	 */
	@Deprecated
	public static boolean getBooleanFromValue(Value value) {
		return BooleanManager.getBooleanFromProtoValue(value);
	}



	/**
	 * @deprecated Use {@link NumberManager#getProtoValueFromBigDecimal(String)} instead.
	 * @param value
	 * @return Value.Builder
	 */
	@Deprecated
	public static Value.Builder getValueFromBigDecimal(BigDecimal value) {
		return NumberManager.getProtoValueFromBigDecimal(value);
	}
	/**
	 * @deprecated Use {@link NumberManager#getBigDecimalFromProtoValue(Value)} instead.
	 * @param value
	 * @return BigDecimal
	 */
	@Deprecated
	public static BigDecimal getBigDecimalFromValue(Value decimalValue) {
		return NumberManager.getBigDecimalFromProtoValue(decimalValue);
	}



	/**
	 * @deprecated Use {@link TimeManager#getTimestampFromProtoValue(Value)} instead.
	 * @param value
	 * @return Timestamp
	 */
	@Deprecated
	public static Timestamp getTimestampFromValue(Value dateValue) {
		return TimeManager.getTimestampFromProtoValue(dateValue);
	}

	/**
	 * @deprecated Use {@link TimeManager#getTimestampFromProtoValue(Value)} instead.
	 * @param value
	 * @return Value.Builder
	 */
	@Deprecated
	public static Value.Builder getValueFromTimestamp(Timestamp value) {
		return TimeManager.getProtoValueFromTimestamp(value);
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
	 * @deprecated Use {@link TimeManager#getProtoTimestampFromTimestamp(Value)} instead.
	 * @param dateValue
	 * @return com.google.protobuf.Timestamp
	 */
	@Deprecated
	public static com.google.protobuf.Timestamp getProtoTimestampFromTimestamp(Timestamp dateValue) {
		return TimeManager.getProtoTimestampFromTimestamp(dateValue);
	}
	/**
	 * @deprecated Use {@link TimeManager#getTimestampFromProtoValue(Value)} instead.
	 * @param value
	 * @return Timestamp
	 */
	@Deprecated
	public static Timestamp getTimestampFromProtoTimestamp(com.google.protobuf.Timestamp dateValue) {
		return TimeManager.getTimestampFromProtoTimestamp(dateValue);
	}
	/**
	 * Get google.protobuf.Timestamp from Timestamp
	 * @deprecated Use {@link TimeManager#getProtoTimestampFromTimestamp(Timestamp)} instead.
	 * @param dateValue
	 * @return com.google.protobuf.Timestamp
	 */
	@Deprecated
	public static com.google.protobuf.Timestamp getTimestampFromDate(Timestamp dateValue) {
		return TimeManager.getProtoTimestampFromTimestamp(dateValue);
	}


	/**
	 * @deprecated Use {@link ValueManager#getProtoValueFromObject(Object, int)} instead.
	 * @param value
	 * @param displayTypeId reference of value
	 * @return Value.Builder
	 */
	@Deprecated
	public static Value.Builder getValueFromReference(Object value, int displayTypeId) {
		return ValueManager.getProtoValueFromObject(value, displayTypeId);
	}
	/**
	 * Get Value from reference
	 * @param value
	 * @param displayTypeId reference of value
	 * @return
	 */
	public static Value.Builder getProtoValueFromObject(Object value, int displayTypeId) {
		Value.Builder builderValue = Value.newBuilder();
		if(value == null) {
			// getEmptyValueByReference(displayTypeId);
			return getProtoValueFromNull();
		}
		// if (value instanceof Value) {
		// 	builderValue.mergeFrom((Value) value);
		// 	return builderValue;
		// } else if (value instanceof Value.Builder) {
		// 	return (Value.Builder) value;
		// }
		if (displayTypeId <= 0) {
			return getProtoValueFromObject(value);
		}
		//	Validate values
		if (DisplayType.isID(displayTypeId) || DisplayType.Integer == displayTypeId) {
			Integer integerValue = NumberManager.getIntegerFromObject(
				value
			);
			if (integerValue == null && (DisplayType.Search == displayTypeId || DisplayType.Table == displayTypeId)) {
				// no casteable for integer, as `AD_Language`, `EntityType`
				return getProtoValueFromObject(value);
			}
			return NumberManager.getProtoValueFromInteger(integerValue);
		} else if(DisplayType.isNumeric(displayTypeId)) {
			BigDecimal bigDecimalValue = NumberManager.getBigDecimalFromObject(
				value
			);
			return NumberManager.getProtoValueFromBigDecimal(bigDecimalValue);
		} else if(DisplayType.YesNo == displayTypeId) {
			if (value instanceof String) {
				String stringValue = TextManager.getStringFromObject(
					value
				);
				return BooleanManager.getProtoValueFromBoolean(stringValue);
			}
			return BooleanManager.getProtoValueFromBoolean((Boolean) value);
		} else if(DisplayType.isDate(displayTypeId)) {
			Timestamp dateValue = TimeManager.getTimestampFromObject(
				value
			);
			return TimeManager.getProtoValueFromTimestamp(dateValue);
		} else if(DisplayType.isText(displayTypeId) || DisplayType.List == displayTypeId) {
			String stringValue = TextManager.getStringFromObject(
				value
			);
			return TextManager.getProtoValueFromString(
				stringValue
			);
		} else if (DisplayType.Button == displayTypeId) {
			if (value instanceof Integer) {
				return NumberManager.getProtoValueFromInteger((Integer) value);
			} else if (value instanceof Long) {
				Integer integerValue = NumberManager.getIntegerFromLong(
					(Long) value
				);
				return NumberManager.getProtoValueFromInt(integerValue);
			} else if(value instanceof BigDecimal) {
				Integer bigDecimalValue = NumberManager.getIntegerFromBigDecimal(
					(BigDecimal) value
				);
				return NumberManager.getProtoValueFromInteger(bigDecimalValue);
			} else if (value instanceof String) {
				String stringValue = TextManager.getStringFromObject(
					value
				);
				return TextManager.getProtoValueFromString(
					stringValue
				);
			}
		}
		builderValue = getProtoValueFromObject(value);
		//
		return builderValue;
	}


	/**
	 * Get Display Value from reference
	 * @param value
	 * @param columnName data base column name
	 * @param displayTypeId display type of field
	 * @param referenceValueId reference of list or table
	 * @return
	 */
	public static String getDisplayedValueFromReference(Object value, String columnName, int displayTypeId, int referenceValueId) {
		return getDisplayedValueFromReference(
			Env.getCtx(),
			value,
			columnName,
			displayTypeId,
			referenceValueId
		);
	}
	public static String getDisplayedValueFromReference(Properties context, Object value, String columnName, int displayTypeId, int referenceValueId) {
		String displayedValue = null;
		if (value == null) {
			return displayedValue;
		}
		if (displayTypeId <= 0) {
			return displayedValue;
		}
		if (context == null) {
			context = Env.getCtx();
		}
		if (value instanceof Value) {
			value = getObjectFromProtoValue(
				(Value) value,
				displayTypeId
			);
		}
		if (DisplayType.isText(displayTypeId)) {
			;
		} else if (displayTypeId == DisplayType.YesNo) {
			String language = Env.getAD_Language(context);
			displayedValue = BooleanManager.getDisplayValue(
				value.toString(),
				language
			);
		} else if (displayTypeId == DisplayType.Integer) {
			displayedValue = NumberManager.getDisplayValue(
				value,
				displayTypeId
			);
		} else if (DisplayType.isNumeric(displayTypeId)) {
			if (I_C_Order.COLUMNNAME_ProcessedOn.equals(columnName)) {
				Timestamp timeValue = TimeManager.getTimestampFromObject(value);
				displayedValue = TimeUtil.formatElapsed(
					timeValue
				);
			} else {
				displayedValue = NumberManager.getDisplayValue(
					value,
					displayTypeId
				);
			}
		} else if (DisplayType.isDate(displayTypeId)) {
			Language language = Env.getLanguage(context);
			displayedValue = TimeManager.getDisplayValue(
				value,
				displayTypeId,
				language
			);
		} else if (DisplayType.isLookup(displayTypeId) && displayTypeId != DisplayType.List) {
			Language language = Env.getLanguage(context);
			MLookupInfo lookupInfo = MLookupFactory.getLookupInfo(
				context, 0,
				0, displayTypeId, language, columnName,
				referenceValueId, false,
				null, false
			);
			MLookup lookup = new MLookup(lookupInfo, 0);
			NamePair pp = lookup.get(value);
			if (pp != null) {
				displayedValue = pp.getName();
			}
		} else if(displayTypeId == DisplayType.Button || displayTypeId == DisplayType.List) {
			if (referenceValueId > 0) {
				Language language = Env.getLanguage(context);
				MLookupInfo lookupInfo = MLookupFactory.getLookup_List(
					language,
					referenceValueId
				);
				MLookup lookup = new MLookup(lookupInfo, 0);
				if (value != null) {
					Object key = value;
					NamePair pp = lookup.get(key);
					if (pp != null) {
						displayedValue = pp.getName();
					}
				}
			} else if (BooleanManager.isBoolean(value)) {
				Language language = Env.getLanguage(context);
				displayedValue = BooleanManager.getDisplayValue(
					value.toString(),
					language
				);
			}
		} else if (DisplayType.isLOB(displayTypeId)) {
			;
		}
		return displayedValue;
	}



	/**
	 * @deprecated Use {@link CollectionManager#getMapFromJsomString(String)} instead.
	 * @param jsonValues
	 * @return Map<String, Object>
	 */
	@Deprecated
	public static Map<String, Object> convertJsonStringToMap(String jsonValues) {
		return CollectionManager.getMapFromJsomString(jsonValues);
	}



	/**
	 * @deprecated Use {@link CollectionManager#getMapObjectFromMapProtoValue(Map<String, Value>)} instead.
	 * @param values
	 * @return
	 */
	@Deprecated
	public static Map<String, Object> convertValuesMapToObjects(Map<String, Value> values) {
		return CollectionManager.getMapObjectFromMapProtoValue(values);
	}

	/**
	 * @deprecated Use {@link CollectionManager#getMapObjectFromMapProtoValue(Map<String, Value>, Map<String, Integer>)} instead.
	 * @param values
	 * @param displayTypeColumns Map(ColumnName, DisplayType)
	 * @return
	 */
	@Deprecated
	public static Map<String, Object> convertValuesMapToObjects(Map<String, Value> values, Map<String, Integer> displayTypeColumns) {
		return CollectionManager.getMapObjectFromMapProtoValue(values, displayTypeColumns);
	}



	/**
	 * Is lookup include location
	 * @param displayType
	 * @return
	 */
	public static boolean isLookup(int displayType) {
		return DisplayType.isLookup(displayType)
			|| DisplayType.Account == displayType
			|| DisplayType.Location == displayType
			|| DisplayType.Locator == displayType
			|| DisplayType.PAttribute == displayType
		;
	}

}
