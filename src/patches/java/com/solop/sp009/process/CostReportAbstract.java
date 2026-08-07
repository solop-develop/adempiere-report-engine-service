/******************************************************************************
 * Product: ADempiere ERP & CRM Smart Business Solution                       *
 * Copyright (C) 2006-2017 ADempiere Foundation, All Rights Reserved.         *
 * This program is free software, you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * or (at your option) any later version.                                     *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY, without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program, if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 * For the text or an alternative of this public license, you may reach us    *
 * or via info@adempiere.net                                                  *
 * or https://github.com/adempiere/adempiere/blob/develop/license.html        *
 *****************************************************************************/

package com.solop.sp009.process;

import org.compiere.process.SvrProcess;

import java.sql.Timestamp;

/** Generated Process for (Cost Report)
 *  @author ADempiere (generated) 
 *  @version Release 3.9.4
 */
public abstract class CostReportAbstract extends SvrProcess {
	/** Process Value 	*/
	private static final String VALUE_FOR_PROCESS = "SP009_CostReport";
	/** Process Name 	*/
	private static final String NAME_FOR_PROCESS = "Cost Report";
	/** Process Id 	*/
	private static final int ID_FOR_PROCESS = 54974;
	/**	Parameter Name for Organization	*/
	public static final String AD_ORG_ID = "AD_Org_ID";
	/**	Parameter Name for Product	*/
	public static final String M_PRODUCT_ID = "M_Product_ID";
	/**	Parameter Name for Business Partner 	*/
	public static final String C_BPARTNER_ID = "C_BPartner_ID";
	/**	Parameter Name for Product Category	*/
	public static final String M_PRODUCT_CATEGORY_ID = "M_Product_Category_ID";
	/**	Parameter Name for Product Group	*/
	public static final String M_PRODUCT_GROUP_ID = "M_Product_Group_ID";
	/**	Parameter Name for Product Class	*/
	public static final String M_PRODUCT_CLASS_ID = "M_Product_Class_ID";
	/**	Parameter Name for Product Classification	*/
	public static final String M_PRODUCT_CLASSIFICATION_ID = "M_Product_Classification_ID";
	/**	Parameter Name for Date To	*/
	public static final String DATETO = "DateTo";
	/**	Parameter Name for Accounting Schema	*/
	public static final String C_ACCTSCHEMA_ID = "C_AcctSchema_ID";
	/**	Parameter Name for Currency	*/
	public static final String C_CURRENCY_ID = "C_Currency_ID";
	/**	Parameter Value for Organization	*/
	private int orgId;
	/**	Parameter Value for Product	*/
	private int productId;
	/**	Parameter Value for Business Partner 	*/
	private int bPartnerId;
	/**	Parameter Value for Product Category	*/
	private int productCategoryId;
	/**	Parameter Value for Product Group	*/
	private int productGroupId;
	/**	Parameter Value for Product Class	*/
	private int productClassId;
	/**	Parameter Value for Product Classification	*/
	private int productClassificationId;
	/**	Parameter Value for Date To	*/
	private Timestamp dateTo;
	/**	Parameter Value for Accounting Schema	*/
	private int acctSchemaId;
	/**	Parameter Value for Currency	*/
	private int currencyId;

	@Override
	protected void prepare() {
		orgId = getParameterAsInt(AD_ORG_ID);
		productId = getParameterAsInt(M_PRODUCT_ID);
		bPartnerId = getParameterAsInt(C_BPARTNER_ID);
		productCategoryId = getParameterAsInt(M_PRODUCT_CATEGORY_ID);
		productGroupId = getParameterAsInt(M_PRODUCT_GROUP_ID);
		productClassId = getParameterAsInt(M_PRODUCT_CLASS_ID);
		productClassificationId = getParameterAsInt(M_PRODUCT_CLASSIFICATION_ID);
		dateTo = getParameterAsTimestamp(DATETO);
		acctSchemaId = getParameterAsInt(C_ACCTSCHEMA_ID);
		currencyId = getParameterAsInt(C_CURRENCY_ID);
	}

	/**	 Getter Parameter Value for Organization	*/
	protected int getOrgId() {
		return orgId;
	}

	/**	 Setter Parameter Value for Organization	*/
	protected void setOrgId(int orgId) {
		this.orgId = orgId;
	}

	/**	 Getter Parameter Value for Product	*/
	protected int getProductId() {
		return productId;
	}

	/**	 Setter Parameter Value for Product	*/
	protected void setProductId(int productId) {
		this.productId = productId;
	}

	/**	 Getter Parameter Value for Business Partner 	*/
	protected int getBPartnerId() {
		return bPartnerId;
	}

	/**	 Setter Parameter Value for Business Partner 	*/
	protected void setBPartnerId(int bPartnerId) {
		this.bPartnerId = bPartnerId;
	}

	/**	 Getter Parameter Value for Product Category	*/
	protected int getProductCategoryId() {
		return productCategoryId;
	}

	/**	 Setter Parameter Value for Product Category	*/
	protected void setProductCategoryId(int productCategoryId) {
		this.productCategoryId = productCategoryId;
	}

	/**	 Getter Parameter Value for Product Group	*/
	protected int getProductGroupId() {
		return productGroupId;
	}

	/**	 Setter Parameter Value for Product Group	*/
	protected void setProductGroupId(int productGroupId) {
		this.productGroupId = productGroupId;
	}

	/**	 Getter Parameter Value for Product Class	*/
	protected int getProductClassId() {
		return productClassId;
	}

	/**	 Setter Parameter Value for Product Class	*/
	protected void setProductClassId(int productClassId) {
		this.productClassId = productClassId;
	}

	/**	 Getter Parameter Value for Product Classification	*/
	protected int getProductClassificationId() {
		return productClassificationId;
	}

	/**	 Setter Parameter Value for Product Classification	*/
	protected void setProductClassificationId(int productClassificationId) {
		this.productClassificationId = productClassificationId;
	}

	/**	 Getter Parameter Value for Date To	*/
	protected Timestamp getDateTo() {
		return dateTo;
	}

	/**	 Setter Parameter Value for Date To	*/
	protected void setDateTo(Timestamp dateTo) {
		this.dateTo = dateTo;
	}

	/**	 Getter Parameter Value for Accounting Schema	*/
	protected int getAcctSchemaId() {
		return acctSchemaId;
	}

	/**	 Setter Parameter Value for Accounting Schema	*/
	protected void setAcctSchemaId(int acctSchemaId) {
		this.acctSchemaId = acctSchemaId;
	}

	/**	 Getter Parameter Value for Currency	*/
	protected int getCurrencyId() {
		return currencyId;
	}

	/**	 Setter Parameter Value for Currency	*/
	protected void setCurrencyId(int currencyId) {
		this.currencyId = currencyId;
	}

	/**	 Getter Parameter Value for Process ID	*/
	public static final int getProcessId() {
		return ID_FOR_PROCESS;
	}

	/**	 Getter Parameter Value for Process Value	*/
	public static final String getProcessValue() {
		return VALUE_FOR_PROCESS;
	}

	/**	 Getter Parameter Value for Process Name	*/
	public static final String getProcessName() {
		return NAME_FOR_PROCESS;
	}
}