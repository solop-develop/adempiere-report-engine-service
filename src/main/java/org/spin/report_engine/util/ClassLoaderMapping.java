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
package org.spin.report_engine.util;

import java.lang.reflect.Constructor;

import org.compiere.util.Util;
import org.spin.report_engine.mapper.IColumnMapping;

/**
 * This class is for load a dynamic class
 * @author Yamel Senih, ysenih@erpya.com, ERPCyA http://www.erpya.com
 */
public class ClassLoaderMapping {
	public static Class<?> getHandlerClass(String className) {
        //	Validate null values
        if(Util.isEmpty(className)) {
            return null;
        }
        try {
            Class<?> clazz = Class.forName(className);
            if(IColumnMapping.class.isAssignableFrom(clazz)) {
                return clazz;
            }
            //	Make sure that it is a PO class
            Class<?> superClazz = clazz.getSuperclass();
            //	Validate super class
            while (superClazz != null) {
                if (superClazz == IColumnMapping.class) {
                    return clazz;
                }
                //	Get Super Class
                superClazz = superClazz.getSuperclass();
            }
        } catch (Exception e) {
        	e.printStackTrace();
        }
        return null;
    }	//	getHandlerClass

	public static IColumnMapping loadClass(String className) {
		IColumnMapping mapping = null;
		try {
			Class<?> clazz = getHandlerClass(className);
	        if (clazz != null) {
	        	Constructor<?> constructor = clazz.getDeclaredConstructor();
	        	mapping = (IColumnMapping) constructor.newInstance();
	        }
		} catch (Exception e) {
			e.printStackTrace();
		}
        return mapping;
    }
}
