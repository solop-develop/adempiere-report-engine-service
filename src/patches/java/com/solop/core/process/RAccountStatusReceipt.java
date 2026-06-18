/**
 * 
 */
package com.solop.core.process;

import org.compiere.model.MBPartner;
import org.compiere.model.MTable;
import org.compiere.model.M_Element;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.TimeUtil;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;

public class RAccountStatusReceipt {

	private ReportAccountStatus acctStatusFilters;
	private static final String MOLD_TABLE = "T_C0024_AccountStatus";

	private String condition = "";
	
	/**
	 * Constructor. Receives selected filters for this report.
	 * @param acctStatusFilters
	 */
	public RAccountStatusReceipt(ReportAccountStatus acctStatusFilters) {
		this.acctStatusFilters = acctStatusFilters;
		if(M_Element.get(Env.getCtx(), "SP029C_DeferredDocument") != null) {
			this.condition = " AND doc.SP029C_DeferredDocument = 'N'";
		}
	}

	/**
	 * Loads data for the report.
	 */
	public void execute() throws Exception {

		// Delete previous report instances for this user to clean up the mold table
		this.deletePreviousReportInstances();
		this.getData();
		this.updateData();
	}

	/**
	 * Deletes previous runs of this report for this user.
	 * @throws Exception
	 */
	private void deletePreviousReportInstances() throws Exception{
		
		String sql = "";
		try{
			sql = " DELETE FROM " + MOLD_TABLE +
				  " WHERE AD_User_ID =" + this.acctStatusFilters.adUserID;
			DB.executeUpdateEx(sql,null);
		}
		catch (Exception e)
		{
			throw e;
		}
	}

	/**
	 * Loads data into mold table considering filters.
	 * @throws Exception
	 */
	private void getData() throws Exception{
		
		String insert ="", sql = "", whereFilters = "";
		MBPartner partner = null;

		try
		{
			// Build filter where clause
			if (this.acctStatusFilters.customerID > 0)
				partner = new MBPartner(Env.getCtx(),this.acctStatusFilters.customerID,null);//instantiate customer

			// Resolve AD_Table_ID for each Record_ID source so the report rows can zoom to the origin document
			int invoiceTableID = MTable.getTable_ID("C_Invoice");
			int paymentTableID = MTable.getTable_ID("C_Payment");
			int payReceiptTableID = MTable.getTable_ID("UY_PayReceipt");
			int crTableID = MTable.getTable_ID("SP028C_CR");
			int allocationTableID = MTable.getTable_ID("C_AllocationHdr");

			whereFilters += " AND st.C_Currency_ID = " + this.acctStatusFilters.cCurrencyID;
			insert = "INSERT INTO " + MOLD_TABLE + " (T_C0024_AccountStatus_ID, Created, CreatedBy, Updated, UpdatedBy, AD_User_ID, AD_Client_ID, AD_Org_ID," +
					" C0024_IsInvoice, C_BPartner_ID, BPValue, BPName, IsReceipt, Record_ID, DocumentNo, C_Currency_ID, CurrencyName, C_DocType_ID," +
					" DocTypeName, StartDate, DateTrx, DueDate, DocumentAmt, C0024_OpeningBalance, C0024_Debit, C0024_Credit, C0024_CumulativeBalance, "
					+ "POReference, IsSOTrx, AD_Corporation_ID, AD_Table_ID)";

			StringBuilder strb = new StringBuilder("");
			Timestamp dateFrom = TimeUtil.trunc(this.acctStatusFilters.dateFrom, TimeUtil.TRUNC_DAY);
			Timestamp dateTo = TimeUtil.trunc(this.acctStatusFilters.dateTo, TimeUtil.TRUNC_DAY);
			
			String whereOrg = "";
			if (this.acctStatusFilters.adOrgID > 0) {
				whereOrg = " AND st.AD_Org_ID =" + this.acctStatusFilters.adOrgID;
			} else if(this.acctStatusFilters.adCorporationID > 0){
				whereOrg = " AND st.AD_Org_ID IN (SELECT AD_Org_ID FROM AD_OrgInfo WHERE AD_Corporation_ID = " + this.acctStatusFilters.adCorporationID + ")";
			}

			//	Without Payment Schedule
			strb.append(" SELECT " + "nextval('" + MOLD_TABLE + "_seq')"  + ", NOW(), 100, NOW(), 100," + this.acctStatusFilters.adUserID + "," +
						this.acctStatusFilters.adClientID + ",st.AD_Org_ID," +
						" CASE WHEN doc.DocBaseType ='ARI' OR doc.DocBaseType = 'API' THEN 'Y' ELSE 'N' END AS C0024_IsInvoice, st.C_BPartner_ID," +
						" bp.Value AS BPValue, (bp.Name || ' - ' ||COALESCE(bp.Name2,'')) AS BPName, st.IsSOTrx, " +
						" st.C_Invoice_ID, st.DocumentNo, st.C_Currency_ID, cur.Description AS CurName, " +
						" st.C_DocType_ID, doc.PrintName, st.DateInvoiced AS StartDate, st.DateInvoiced AS DateDoc, st.DateInvoiced AS DueDate," +
					    " st.GrandTotal, 0, 0, 0, 0, COALESCE(st.POReference, st.Description) AS POReference, st.IsSOTrx, " + this.acctStatusFilters.adCorporationID + "," + invoiceTableID +
						" FROM C_Invoice st " +
						" INNER JOIN C_DocType doc ON st.C_DocType_ID = doc.C_DocType_ID" +
					    " INNER JOIN C_Currency cur ON st.C_Currency_ID = cur.C_Currency_ID " +
					    " INNER JOIN C_BPartner bp ON st.C_BPartner_ID = bp.C_BPartner_ID " +
					    " WHERE st.AD_Client_ID =" + this.acctStatusFilters.adClientID +
					    whereOrg +
					    " AND st.DateInvoiced BETWEEN '" + dateFrom + "' AND '" + dateTo + "' " +
						condition +
					    " AND st.DocStatus = 'CO' " + whereFilters);

			//if it is a parent customer
			if(isParent(partner)){
				strb.append(" AND st.C_BPartner_ID IN (SELECT C_BPartner_ID FROM C_BPartner WHERE BPartner_Parent_ID = " + this.acctStatusFilters.customerID + " OR C_BPartner_ID = " + this.acctStatusFilters.customerID + ")");
			} else strb.append(" AND st.C_BPartner_ID = " + this.acctStatusFilters.customerID);

			if(this.acctStatusFilters.IsSOTrx){
				strb.append(" AND st.IsSOTrx='Y' ");
			} else {
				strb.append(" AND st.IsSOTrx='N' ");
			}

			// Payments
			strb.append(" UNION SELECT " + "nextval('" + MOLD_TABLE + "_seq')" + ", NOW(), 100, NOW(), 100," + this.acctStatusFilters.adUserID + "," +
					this.acctStatusFilters.adClientID + ",st.AD_Org_ID," +
					" 'N' AS C0024_IsInvoice,st.C_BPartner_ID," +
					" bp.Value AS BPValue, (bp.Name || ' - ' ||COALESCE(bp.Name2,'')) AS BPName, st.IsReceipt, " +
					" st.C_Payment_ID, st.DocumentNo, st.C_Currency_ID, cur.Description AS CurName, " +
					" st.C_DocType_ID, doc.PrintName, st.DateTrx AS StartDate, st.DateTrx AS DateDoc, st.DateTrx AS DueDate, st.PayAmt,"
					+ " 0,0,0,0, COALESCE(st.Description,'') AS POReference, st.IsReceipt, " + this.acctStatusFilters.adCorporationID + "," + paymentTableID +
					" FROM C_Payment st " +
					" INNER JOIN C_DocType doc ON st.C_DocType_ID = doc.C_DocType_ID" +
					" INNER JOIN C_Currency cur ON st.C_Currency_ID = cur.C_Currency_ID " +
					" INNER JOIN C_BPartner bp ON st.C_BPartner_ID = bp.C_BPartner_ID " +
					" WHERE st.AD_Client_ID =" + this.acctStatusFilters.adClientID +
					whereOrg +
					" AND st.DateTrx BETWEEN '" + dateFrom + "' AND '" + dateTo + "' " +
					" AND st.UY_PayReceipt_ID IS NULL" +
					" AND st.SP028C_CR_ID IS NULL" +
					" AND st.C_Charge_ID IS NULL" +
					" AND st.DocStatus IN ('CO','CL') " + condition + whereFilters);

			//if it is a parent customer
			if(isParent(partner)){
				strb.append(" AND st.C_BPartner_ID IN (SELECT C_BPartner_ID FROM C_BPartner WHERE BPartner_Parent_ID = " + this.acctStatusFilters.customerID + " OR C_BPartner_ID = " + this.acctStatusFilters.customerID + ")");
			} else strb.append(" AND st.C_BPartner_ID = " + this.acctStatusFilters.customerID);

			if(this.acctStatusFilters.IsSOTrx){
				strb.append(" AND st.IsReceipt='Y' ");
			} else {
				strb.append(" AND st.IsReceipt='N' ");
			}
			
			// Receipts
			strb.append(" UNION SELECT " + "nextval('" + MOLD_TABLE + "_seq')" + ", NOW(), 100, NOW(), 100," + this.acctStatusFilters.adUserID + "," +
						this.acctStatusFilters.adClientID + ",st.AD_Org_ID," +
						" 'N' AS C0024_IsInvoice,st.C_BPartner_ID," +
						" bp.Value AS BPValue, (bp.Name || ' - ' ||COALESCE(bp.Name2,'')) AS BPName, st.IsSOTrx, " +
						" st.UY_PayReceipt_ID, st.DocumentNo, st.C_Currency_ID, cur.Description AS CurName, " +
						" st.C_DocType_ID, doc.PrintName, st.DateDoc AS StartDate, st.DateDoc AS DateDoc, st.DateDoc AS DueDate, st.PayAmt,"
						+ " 0,0,0,0, COALESCE(st.Description,'') AS POReference, st.IsSOTrx, " + this.acctStatusFilters.adCorporationID + "," + payReceiptTableID +
						" FROM UY_PayReceipt st " +
						" INNER JOIN C_DocType doc ON st.C_DocType_ID = doc.C_DocType_ID" +
					    " INNER JOIN C_Currency cur ON st.C_Currency_ID = cur.C_Currency_ID " +
					    " INNER JOIN C_BPartner bp ON st.C_BPartner_ID = bp.C_BPartner_ID " +
					    " WHERE st.AD_Client_ID =" + this.acctStatusFilters.adClientID +
					    whereOrg +
					    " AND st.DateDoc BETWEEN '" + dateFrom + "' AND '" + dateTo + "' " +
						" AND st.IsMultiCurrency = 'N' " +
						" AND st.DocStatus = 'CO' " + whereFilters);

			//if it is a parent customer
			if(isParent(partner)){
				strb.append(" AND st.C_BPartner_ID IN (SELECT C_BPartner_ID FROM C_BPartner WHERE BPartner_Parent_ID = " + this.acctStatusFilters.customerID + " OR C_BPartner_ID = " + this.acctStatusFilters.customerID + ")");
			} else strb.append(" AND st.C_BPartner_ID = " + this.acctStatusFilters.customerID);

			if(this.acctStatusFilters.IsSOTrx){
				strb.append(" AND st.IsSOTrx='Y' ");
			} else {
				strb.append(" AND st.IsSOTrx='N' ");
			}

			// Receipts v2
			strb.append(" UNION SELECT " + "nextval('" + MOLD_TABLE + "_seq')" + ", NOW(), 100, NOW(), 100," + this.acctStatusFilters.adUserID + "," +
					this.acctStatusFilters.adClientID + ",st.AD_Org_ID," +
					" 'N' AS C0024_IsInvoice,st.C_BPartner_ID," +
					" bp.Value AS BPValue, (bp.Name || ' - ' ||COALESCE(bp.Name2,'')) AS BPName, st.IsSOTrx, " +
					" st.SP028C_CR_ID, st.DocumentNo, st.C_Currency_ID, cur.Description AS CurName, " +
					" st.C_DocType_ID, doc.PrintName, st.DateDoc AS StartDate, st.DateDoc AS DateDoc, st.DateDoc AS DueDate, st.PayAmt,"
					+ " 0,0,0,0, COALESCE(st.Description,'') AS POReference, st.IsSOTrx, " + this.acctStatusFilters.adCorporationID + "," + crTableID +
					" FROM SP028C_CR st " +
					" INNER JOIN C_DocType doc ON st.C_DocType_ID = doc.C_DocType_ID" +
					" INNER JOIN C_Currency cur ON st.C_Currency_ID = cur.C_Currency_ID " +
					" INNER JOIN C_BPartner bp ON st.C_BPartner_ID = bp.C_BPartner_ID " +
					" WHERE st.AD_Client_ID =" + this.acctStatusFilters.adClientID +
					whereOrg +
					" AND st.DateDoc BETWEEN '" + dateFrom + "' AND '" + dateTo + "' " +
					" AND st.SP028C_IsMultiCurrency = 'N' " +
					" AND st.DocStatus = 'CO' " + whereFilters);

			//if it is a parent customer
			if(isParent(partner)){
				strb.append(" AND st.C_BPartner_ID IN (SELECT C_BPartner_ID FROM C_BPartner WHERE BPartner_Parent_ID = " + this.acctStatusFilters.customerID + " OR C_BPartner_ID = " + this.acctStatusFilters.customerID + ")");
			} else strb.append(" AND st.C_BPartner_ID = " + this.acctStatusFilters.customerID);

			if(this.acctStatusFilters.IsSOTrx){
				strb.append(" AND st.IsSOTrx='Y' ");
			} else {
				strb.append(" AND st.IsSOTrx='N' ");
			}

			// Receipts (second currency)
			strb.append(" UNION SELECT " + "nextval('" + MOLD_TABLE + "_seq')" + ", NOW(), 100, NOW(), 100," + this.acctStatusFilters.adUserID + "," +
					this.acctStatusFilters.adClientID + ",st.AD_Org_ID," +
					" 'N' AS C0024_IsInvoice,st.C_BPartner_ID," +
					" bp.Value AS BPValue, (bp.Name || ' - ' ||COALESCE(bp.Name2,'')) AS BPName, st.IsSOTrx, " +
					" st.UY_PayReceipt_ID, st.DocumentNo, st.C_Currency_ID_To, cur.Description AS CurName, " +
					" st.C_DocType_ID, doc.PrintName, st.DateDoc AS StartDate, st.DateDoc AS DateDoc, st.DateDoc AS DueDate, " +
					" currencyconvert_receipt(st.PayAmt, st.CurrencyRate, st.C_Currency_ID, st.C_Currency_ID_To, " +
					" st.AD_Client_ID, st.AD_Org_ID) AS PayAmt, " +
					" 0,0,0,0, COALESCE(st.Description,'') AS POReference, st.IsSOTrx, " + this.acctStatusFilters.adCorporationID + "," + payReceiptTableID +
					" FROM UY_PayReceipt st " +
					" INNER JOIN C_DocType doc ON st.C_DocType_ID = doc.C_DocType_ID" +
					" INNER JOIN C_Currency cur ON st.C_Currency_ID = cur.C_Currency_ID " +
					" INNER JOIN C_BPartner bp ON st.C_BPartner_ID = bp.C_BPartner_ID " +
					" WHERE st.AD_Client_ID =" + this.acctStatusFilters.adClientID +
					whereOrg +
					" AND st.DateDoc BETWEEN '" + dateFrom + "' AND '" + dateTo + "' " +
					" AND st.DocStatus = 'CO' " +
					" AND st.IsMultiCurrency = 'Y' " +
					" AND st.C_Currency_ID_To = " + this.acctStatusFilters.cCurrencyID);

			//if it is a parent customer
			if(isParent(partner)){
				strb.append(" AND st.C_BPartner_ID IN (SELECT C_BPartner_ID FROM C_BPartner WHERE BPartner_Parent_ID = " + this.acctStatusFilters.customerID + " OR C_BPartner_ID = " + this.acctStatusFilters.customerID + ")");
			} else strb.append(" AND st.C_BPartner_ID = " + this.acctStatusFilters.customerID);

			if(this.acctStatusFilters.IsSOTrx){
				strb.append(" AND st.IsSOTrx='Y' ");
			} else {
				strb.append(" AND st.IsSOTrx='N' ");
			}

			// Receipts v2 (second currency)
			strb.append(" UNION SELECT " + "nextval('" + MOLD_TABLE + "_seq')" + ", NOW(), 100, NOW(), 100," + this.acctStatusFilters.adUserID + "," +
					this.acctStatusFilters.adClientID + ",st.AD_Org_ID," +
					" 'N' AS C0024_IsInvoice,st.C_BPartner_ID," +
					" bp.Value AS BPValue, (bp.Name || ' - ' ||COALESCE(bp.Name2,'')) AS BPName, st.IsSOTrx, " +
					" st.SP028C_CR_ID, st.DocumentNo, st.C_Currency_ID_To, cur.Description AS CurName, " +
					" st.C_DocType_ID, doc.PrintName, st.DateDoc AS StartDate, st.DateDoc AS DateDoc, st.DateDoc AS DueDate, " +
					" currencyconvert(st.PayAmt, st.C_Currency_ID, st.C_Currency_ID_To, st.DateDoc, st.C_ConversionType_ID," +
					" st.AD_Client_ID, st.AD_Org_ID) AS PayAmt, " +
					//" currencyconvert_receipt(st.PayAmt, st.CurrencyRate, st.C_Currency_ID, st.C_Currency_ID_To, " +
					//" st.AD_Client_ID, st.AD_Org_ID) AS PayAmt, " +
					" 0,0,0,0, COALESCE(st.Description,'') AS POReference, st.IsSOTrx, " + this.acctStatusFilters.adCorporationID + "," + crTableID +
					" FROM SP028C_CR st " +
					" INNER JOIN C_DocType doc ON st.C_DocType_ID = doc.C_DocType_ID" +
					" INNER JOIN C_Currency cur ON st.C_Currency_ID = cur.C_Currency_ID " +
					" INNER JOIN C_BPartner bp ON st.C_BPartner_ID = bp.C_BPartner_ID " +
					" WHERE st.AD_Client_ID =" + this.acctStatusFilters.adClientID +
					whereOrg +
					" AND st.DateDoc BETWEEN '" + dateFrom + "' AND '" + dateTo + "' " +
					" AND st.DocStatus = 'CO' " +
					" AND st.SP028C_IsMultiCurrency = 'Y' " +
					" AND st.C_Currency_ID_To = " + this.acctStatusFilters.cCurrencyID);

			//if it is a parent customer
			if(isParent(partner)){
				strb.append(" AND st.C_BPartner_ID IN (SELECT C_BPartner_ID FROM C_BPartner WHERE BPartner_Parent_ID = " + this.acctStatusFilters.customerID + " OR C_BPartner_ID = " + this.acctStatusFilters.customerID + ")");
			} else strb.append(" AND st.C_BPartner_ID = " + this.acctStatusFilters.customerID);

			if(this.acctStatusFilters.IsSOTrx){
				strb.append(" AND st.IsSOTrx='Y' ");
			} else {
				strb.append(" AND st.IsSOTrx='N' ");
			}

			BigDecimal multiplier = Env.ONE;
			if(!this.acctStatusFilters.IsSOTrx)
				multiplier = multiplier.negate();

			//Allocations with Charge
			strb.append(" UNION SELECT " + "nextval('" + MOLD_TABLE + "_seq')" + ", NOW(), 100, NOW(), 100," + this.acctStatusFilters.adUserID + "," +
					this.acctStatusFilters.adClientID + ",st.AD_Org_ID," +
					" 'N' AS C0024_IsInvoice,al.C_BPartner_ID," +
					" bp.Value AS BPValue, (bp.Name || ' - ' ||COALESCE(bp.Name2,'')) AS BPName, '" + this.acctStatusFilters.isSoTrx + "'," +
					" st.C_AllocationHdr_ID, st.DocumentNo, st.C_Currency_ID, cur.Description AS CurName, " +
					" st.C_DocType_ID, 'Asignación con Cargo' AS PrintName, st.DateTrx AS StartDate, st.DateTrx AS DateDoc, st.DateTrx AS DueDate, al.Amount * " + multiplier + ","
					+ " 0,0,0,0, st.Description AS POReference, '" + this.acctStatusFilters.isSoTrx + "'," + this.acctStatusFilters.adCorporationID + "," + allocationTableID +
					" FROM C_AllocationHdr st " +
					" INNER JOIN C_AllocationLine al ON st.C_AllocationHdr_ID = al.C_AllocationHdr_ID " +
					" INNER JOIN C_DocType doc ON st.C_DocType_ID = doc.C_DocType_ID" +
					" INNER JOIN C_Currency cur ON st.C_Currency_ID = cur.C_Currency_ID " +
					" INNER JOIN C_BPartner bp ON al.C_BPartner_ID = bp.C_BPartner_ID " +
					" WHERE st.AD_Client_ID =" + this.acctStatusFilters.adClientID +
					whereOrg +
					" AND al.C_Charge_ID > 0 AND st.DateTrx BETWEEN '" + dateFrom + "' AND '" + dateTo + "' " +
					" AND st.DocStatus IN ('CO','CL') " + whereFilters +
					" AND NOT EXISTS (SELECT UY_PayReceipt_ID FROM UY_PayReceipt WHERE C_AllocationHdr_ID = st.C_AllocationHdr_ID)" +
					" AND NOT EXISTS (SELECT SP028C_CR_ID FROM SP028C_CR WHERE C_AllocationHdr_ID = st.C_AllocationHdr_ID)" +
					" AND ((SELECT COUNT(l.C_AllocationLine_ID) FROM C_AllocationLine l" +
					" INNER JOIN C_Invoice i ON l.C_Invoice_ID = i.C_Invoice_ID" +
					" WHERE l.C_AllocationHdr_ID = st.C_AllocationHdr_ID" +
					" AND i.IsSOTrx = '" + this.acctStatusFilters.isSoTrx +"') > 0 OR (SELECT COUNT(l.C_AllocationLine_ID) FROM C_AllocationLine l" +
					" INNER JOIN C_Payment p ON l.C_Payment_ID = p.C_Payment_ID" +
					" WHERE l.C_AllocationHdr_ID = st.C_AllocationHdr_ID" +
					" AND p.IsReceipt = '" + this.acctStatusFilters.isSoTrx + "') > 0)");

			//if it is a parent customer
			if(isParent(partner)){
				strb.append(" AND al.C_BPartner_ID IN (SELECT C_BPartner_ID FROM C_BPartner WHERE BPartner_Parent_ID = " + this.acctStatusFilters.customerID + " OR C_BPartner_ID = " + this.acctStatusFilters.customerID + ")");
			} else strb.append(" AND al.C_BPartner_ID = " + this.acctStatusFilters.customerID);

			//Allocations with WriteOff/Discount
			strb.append(" UNION SELECT " + "nextval('" + MOLD_TABLE + "_seq')" + ", NOW(), 100, NOW(), 100," + this.acctStatusFilters.adUserID + "," +
					this.acctStatusFilters.adClientID + ",st.AD_Org_ID," +
					" 'N' AS C0024_IsInvoice,al.C_BPartner_ID," +
					" bp.Value AS BPValue, (bp.Name || ' - ' ||COALESCE(bp.Name2,'')) AS BPName, '" + this.acctStatusFilters.isSoTrx + "'," +
					" st.C_AllocationHdr_ID, st.DocumentNo, st.C_Currency_ID, cur.Description AS CurName, " +
					" st.C_DocType_ID, 'Ajuste por Redondeo' AS PrintName, st.DateTrx AS StartDate, st.DateTrx AS DateDoc, st.DateTrx AS DueDate, al.WriteOffAmt * " + multiplier + ","
					+ " 0,0,0,0, st.Description AS POReference, '" + this.acctStatusFilters.isSoTrx + "'," + this.acctStatusFilters.adCorporationID + "," + allocationTableID +
					" FROM C_AllocationHdr st " +
					" INNER JOIN C_AllocationLine al ON st.C_AllocationHdr_ID = al.C_AllocationHdr_ID " +
					" INNER JOIN C_DocType doc ON st.C_DocType_ID = doc.C_DocType_ID" +
					" INNER JOIN C_Currency cur ON st.C_Currency_ID = cur.C_Currency_ID " +
					" INNER JOIN C_BPartner bp ON al.C_BPartner_ID = bp.C_BPartner_ID " +
					" WHERE st.AD_Client_ID =" + this.acctStatusFilters.adClientID +
					whereOrg +
					" AND al.WriteOffAmt <> 0 AND st.DateTrx BETWEEN '" + dateFrom + "' AND '" + dateTo + "' " +
					" AND st.DocStatus IN ('CO','CL') " + whereFilters +
					" AND NOT EXISTS (SELECT UY_PayReceipt_ID FROM UY_PayReceipt WHERE C_AllocationHdr_ID = st.C_AllocationHdr_ID)" +
					" AND NOT EXISTS (SELECT SP028C_CR_ID FROM SP028C_CR WHERE C_AllocationHdr_ID = st.C_AllocationHdr_ID)" +
					" AND ((SELECT COUNT(l.C_AllocationLine_ID) FROM C_AllocationLine l" +
					" INNER JOIN C_Invoice i ON l.C_Invoice_ID = i.C_Invoice_ID" +
					" WHERE l.C_AllocationHdr_ID = st.C_AllocationHdr_ID" +
					" AND i.IsSOTrx = '" + this.acctStatusFilters.isSoTrx +"') > 0 OR (SELECT COUNT(l.C_AllocationLine_ID) FROM C_AllocationLine l" +
					" INNER JOIN C_Payment p ON l.C_Payment_ID = p.C_Payment_ID" +
					" WHERE l.C_AllocationHdr_ID = st.C_AllocationHdr_ID" +
					" AND p.IsReceipt = '" + this.acctStatusFilters.isSoTrx + "') > 0)");

			//if it is a parent customer
			if(isParent(partner)){
				strb.append(" AND al.C_BPartner_ID IN (SELECT C_BPartner_ID FROM C_BPartner WHERE BPartner_Parent_ID = " + this.acctStatusFilters.customerID + " OR C_BPartner_ID = " + this.acctStatusFilters.customerID + ")");
			} else strb.append(" AND al.C_BPartner_ID = " + this.acctStatusFilters.customerID);

			strb.append(" ORDER BY DateDoc");
			sql = strb.toString();
			DB.executeUpdateEx(insert + sql, null);
 		}
		catch (Exception e)
		{
			throw e;
		}
	}

	/**
	 * Calculates balance fields.
	 * @throws Exception
	 */
	private void updateData() throws Exception{

		String sql = "";
		ResultSet rs = null;
		PreparedStatement pstmt = null;
		
		try{
			
			sql = " SELECT * " +
				  " FROM " + MOLD_TABLE +
				  " WHERE AD_User_ID=?" +
				  " ORDER BY DateTrx, T_C0024_AccountStatus_ID ASC";

			pstmt = DB.prepareStatement(sql, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY, null);
			pstmt.setInt(1, this.acctStatusFilters.adUserID);
			
			rs = pstmt.executeQuery ();
			
			rs.last();
			int totalRowCount = rs.getRow(), rowCount = 0;
			rs.beforeFirst();

			if (totalRowCount <= 0){
				this.updateWithOpeningBalanceOnly();
				return;
			}
			
			String action = ""; 
			BigDecimal cumulativeBalance = Env.ZERO, openingBalance = Env.ZERO, documentTotal = Env.ZERO;
			int cBPartnerID = 0, cBPartnerIDAux = 0, cDocTypeID = 0, recordID = 0, moldeID = 0;
			boolean isDebit = false;
			
			// Control break by business partner
			while (rs.next()){
				
				cDocTypeID = rs.getInt("C_DocType_ID");
				recordID = rs.getInt("Record_ID");
				cBPartnerIDAux = rs.getInt("C_BPartner_ID");
				moldeID = rs.getInt("T_C0024_AccountStatus_ID");

				// If business partner changed
				if (cBPartnerIDAux != cBPartnerID){
					cBPartnerID = cBPartnerIDAux;
					openingBalance = getOpeningBalance(cBPartnerID);
					cumulativeBalance = openingBalance;
				}				
				
				documentTotal = rs.getBigDecimal("DocumentAmt");
				
				// If date is before range, consider amount as zero
				if (rs.getTimestamp("DateTrx").compareTo(this.acctStatusFilters.dateFrom) < 0){
					documentTotal = Env.ZERO;
				}

				if(this.acctStatusFilters.IsSOTrx){

					if (rs.getString("C0024_IsInvoice").equalsIgnoreCase("Y")){
						cumulativeBalance = cumulativeBalance.add(documentTotal);
						isDebit = true;
					}
					else{
						cumulativeBalance = cumulativeBalance.subtract(documentTotal);
						isDebit = false;
					}


				} else {

					if (rs.getString("C0024_IsInvoice").equalsIgnoreCase("Y")){
						cumulativeBalance = cumulativeBalance.subtract(documentTotal);
						isDebit = false;
					}
					else{
						cumulativeBalance = cumulativeBalance.add(documentTotal);
						isDebit = true;
					}

				}
				
				action = " UPDATE " + MOLD_TABLE +
		 		 		 " SET C0024_CumulativeBalance =" + cumulativeBalance + "," +
  	 		 		     " C0024_OpeningBalance =" + openingBalance + "," +
		 		 		 ((isDebit) ? " C0024_Debit =" : " C0024_Credit =") + documentTotal +
		 		 		 " WHERE AD_User_ID =" + this.acctStatusFilters.adUserID +
		 		 		 " AND C_BPartner_ID =" + cBPartnerID +
		 		 		 " AND C_DocType_ID =" + cDocTypeID +
		 		 		 " AND Record_ID =" + recordID +
						 " AND T_C0024_AccountStatus_ID = " + moldeID;
				DB.executeUpdateEx(action, null);
			}
			
		}
		catch (Exception e)
		{
			throw e;
		}
		finally
		{
			DB.close(rs, pstmt);
			rs = null; pstmt = null;
		}
	}
	
	/**
	 * Inserts only opening balances when no data was retrieved
	 * @throws Exception
	 */
	private void updateWithOpeningBalanceOnly() throws Exception{
		
		String sql = "", where = "", insert = "", select = "", name = "";
		int partnerID = 0;
		BigDecimal openingBalance = Env.ZERO;
		ResultSet rs = null;
		PreparedStatement pstmt = null;

		try{

			if (this.acctStatusFilters.customerID > 0)
				where += " AND bp.C_BPartner_ID = " + this.acctStatusFilters.customerID;

			sql = "SELECT C_BPartner_ID, Value, Name FROM C_BPartner bp WHERE bp.IsActive='Y'" + where + " ORDER BY bp.Value";

			pstmt = DB.prepareStatement(sql, null);
			rs = pstmt.executeQuery();

			while (rs.next()) {

				partnerID = rs.getInt("C_BPartner_ID");
				name = rs.getString("Name").replace("'", "").trim();
				openingBalance = getOpeningBalance(partnerID);

				if (openingBalance.compareTo(Env.ZERO) != 0) {

					insert = "INSERT INTO " + MOLD_TABLE + " (T_C0024_AccountStatus_ID, Created, CreatedBy, Updated, UpdatedBy, AD_User_ID, AD_Client_ID, AD_Org_ID," +
							" C0024_IsInvoice, C_BPartner_ID, BPValue, BPName, IsReceipt, Record_ID, DocumentNo, C_Currency_ID, CurrencyName, C_DocType_ID," +
							" DocTypeName, StartDate, DateTrx, DueDate, DocumentAmt, C0024_OpeningBalance, C0024_Debit, C0024_Credit, C0024_CumulativeBalance, IsSOTrx, AD_Corporation_ID)";

					select = " SELECT " +
							"nextval('" + MOLD_TABLE + "_seq')" +
							", NOW(), 100, NOW(), 100," + this.acctStatusFilters.adUserID + "," +
							this.acctStatusFilters.adClientID + "," + this.acctStatusFilters.adOrgID + ",'N'," + partnerID + ",'" + rs.getString("Value") + "','" +
							name + "','N',null,null," + this.acctStatusFilters.cCurrencyID + ",null,null,null,'" + this.acctStatusFilters.dateFrom + "','" +
							this.acctStatusFilters.dateFrom + "',null,null," + openingBalance + ",null,null," + openingBalance + ",'" + this.acctStatusFilters.isSoTrx + "'," + this.acctStatusFilters.adCorporationID;

					DB.executeUpdateEx(insert + select, null);

				}

			}

		} catch (Exception e) {
			throw e;
		} finally {
			DB.close(rs, pstmt);
			rs = null;
			pstmt = null;
		}

	}

	/**
	 * Gets and returns the opening balance for a customer.
	 * @param cBPartnerID
	 * @return
	 * @throws Exception 
	 */
	private BigDecimal getOpeningBalance(int cBPartnerID) throws Exception {
		return (this.getOpeningDebitBalance(cBPartnerID).subtract(this.getOpeningCreditBalance(cBPartnerID))).subtract(this.getOpeningAllocations(cBPartnerID));
	}

	/**
	 * OpenUp.	
	 * Description: Gets opening DEBIT balance for a given business partner (customer or vendor).
	 * @param cBPartnerID int : Business partner ID to consider.
	 * @return
	 * @throws Exception
	 */
	private BigDecimal getOpeningDebitBalance(int cBPartnerID) throws Exception {
		
		BigDecimal value;

		if(this.acctStatusFilters.IsSOTrx){
			value = this.getOpeningDebitCustomerInvoice(cBPartnerID);
		} else {
			value = this.getOpeningDebitVendorInvoice(cBPartnerID).add(this.getOpeningDebitVendorPayment(cBPartnerID));
		}
		return value;
	}
	
	/**
	 * OpenUp.	
	 * Description: Gets opening CREDIT balance for a given business partner (customer or vendor).
	 * @param cBPartnerID int : Business partner ID to consider.
	 * @return
	 * @throws Exception
	 */
	private BigDecimal getOpeningCreditBalance(int cBPartnerID) throws Exception {

		BigDecimal value;

		if(this.acctStatusFilters.IsSOTrx){
			value = this.getOpeningCreditCustomerInvoice(cBPartnerID).add(this.getOpeningCreditCustomerPayment(cBPartnerID));
		} else {
			value = this.getOpeningCreditVendorInvoice(cBPartnerID);
		}
		return value;
	}

	/**
	 * OpenUp.	
	 * Description: Gets opening balance from sales invoices for a given customer.
	 * @param cBPartnerID int : Customer ID to consider.
	 * @return
	 * @throws Exception
	 */
	private BigDecimal getOpeningDebitCustomerInvoice(int cBPartnerID) throws Exception{
		
		String sql = "";
		ResultSet rs = null;
		PreparedStatement pstmt = null;
		BigDecimal value = new BigDecimal(0);
		
		try{
			String whereCurrency = "";
			// Currency condition
			if (this.acctStatusFilters.cCurrencyID > 0) whereCurrency = " AND a.C_Currency_ID = " + this.acctStatusFilters.cCurrencyID;

			String whereOrg = "";
			if (this.acctStatusFilters.adOrgID > 0) {
				whereOrg = " AND a.AD_Org_ID =" + this.acctStatusFilters.adOrgID;
			} else if(this.acctStatusFilters.adCorporationID > 0){
				whereOrg = " AND a.AD_Org_ID IN (SELECT AD_Org_ID FROM AD_OrgInfo WHERE AD_Corporation_ID = " + this.acctStatusFilters.adCorporationID + ")";
			}

			sql = "SELECT COALESCE(SUM(a.GrandTotal),0) AS Balance " +
				  " FROM C_Invoice a INNER JOIN C_DocType b ON a.C_DocType_ID = b.C_DocType_ID " +
				  " WHERE a.AD_Client_ID =" + this.acctStatusFilters.adClientID +
				  whereOrg +
				  " AND a.DateInvoiced <? " + 
				  " AND a.DocStatus = 'CO' " +
				  " AND a.C_BPartner_ID =" + cBPartnerID + whereCurrency +
				  " AND b.IsSOTrx='Y' " +
                  " AND b.DocBaseType ='ARI'" +
				  " AND b.SP029C_DeferredDocument = 'N'";

			pstmt = DB.prepareStatement(sql, null);
			pstmt.setTimestamp(1, this.acctStatusFilters.dateFrom);			
			rs = pstmt.executeQuery();
		
			if (rs.next()){
				value = rs.getBigDecimal("Balance");
			}
		}
		catch (Exception e)
		{
			throw e;
		}
		finally
		{
			DB.close(rs, pstmt);
			rs = null; pstmt = null;
		}
		return value;		
	}
	
	
	/**
	 * OpenUp.	
	 * Description: Gets opening balance from sales credit notes for a given customer.
	 * @param cBPartnerID int : Customer ID to consider.
	 * @return
	 * @throws Exception
	 */
	private BigDecimal getOpeningCreditCustomerInvoice(int cBPartnerID) throws Exception{
		
		String sql = "";
		ResultSet rs = null;
		PreparedStatement pstmt = null;
		BigDecimal value = new BigDecimal(0);
		
		try{
			String whereCurrency = "";
			// Currency condition
			if (this.acctStatusFilters.cCurrencyID > 0) whereCurrency = " AND a.C_Currency_ID = " + this.acctStatusFilters.cCurrencyID;

			String whereOrg = "";
			if (this.acctStatusFilters.adOrgID > 0){
				whereOrg = " AND a.AD_Org_ID =" + this.acctStatusFilters.adOrgID;
			} else if(this.acctStatusFilters.adCorporationID > 0){
				whereOrg = " AND a.AD_Org_ID IN (SELECT AD_Org_ID FROM AD_OrgInfo WHERE AD_Corporation_ID = " + this.acctStatusFilters.adCorporationID + ")";
			}

			sql = "SELECT COALESCE(SUM(a.GrandTotal),0) AS Balance " +
				  " FROM C_Invoice a INNER JOIN C_DocType b ON a.C_DocType_ID = b.C_DocType_ID " +
				  " WHERE a.AD_Client_ID =" + this.acctStatusFilters.adClientID +
				  whereOrg +
				  " AND a.DateInvoiced <? " + 
				  " AND a.DocStatus = 'CO' " +
				  " AND a.C_BPartner_ID =" + cBPartnerID + whereCurrency +
				  " AND b.IsSOTrx='Y'" +
				  " AND b.DocBaseType = 'ARC'" +
				  " AND b.SP029C_DeferredDocument = 'N'";

			pstmt = DB.prepareStatement(sql, null);
			pstmt.setTimestamp(1, this.acctStatusFilters.dateFrom);			
			rs = pstmt.executeQuery();
		
			if (rs.next()){
				value = rs.getBigDecimal("Balance");
			}
		}
		catch (Exception e)
		{
			throw e;
		}
		finally
		{
			DB.close(rs, pstmt);
			rs = null; pstmt = null;
		}
		return value;		
	}
	
	
	/**
	 * OpenUp.	
	 * Description: Gets opening balance from sales collections for a given customer.
	 * @param cBPartnerID int : Customer ID to consider.
	 * @return
	 * @throws Exception
	 */
	private BigDecimal getOpeningCreditCustomerPayment(int cBPartnerID) throws Exception{
		
		String sql = "";
		ResultSet rs = null;
		PreparedStatement pstmt = null;
		ResultSet rs2 = null;
		PreparedStatement pstmt2 = null;
		ResultSet rs3 = null;
		PreparedStatement pstmt3 = null;
		ResultSet rs4 = null;
		PreparedStatement pstmt4 = null;
		ResultSet rs5 = null;
		PreparedStatement pstmt5 = null;
		BigDecimal value;
		BigDecimal amtPay = new BigDecimal(0);
		BigDecimal amtReceipt = new BigDecimal(0);
		BigDecimal amtReceiptCurr2 = new BigDecimal(0);
		
		try{
		
			String whereCurrency = "";
			// Currency condition
			if (this.acctStatusFilters.cCurrencyID > 0) whereCurrency = " AND a.C_Currency_ID = " + this.acctStatusFilters.cCurrencyID;

			String whereOrg = "";
			if (this.acctStatusFilters.adOrgID > 0){
				whereOrg = " AND a.AD_Org_ID =" + this.acctStatusFilters.adOrgID;
			} else if(this.acctStatusFilters.adCorporationID > 0){
				whereOrg = " AND a.AD_Org_ID IN (SELECT AD_Org_ID FROM AD_OrgInfo WHERE AD_Corporation_ID = " + this.acctStatusFilters.adCorporationID + ")";
			}

			//Collections
			sql = "SELECT COALESCE(SUM(a.PayAmt),0) AS Balance " +
				  " FROM C_Payment a INNER JOIN C_DocType b ON a.C_DocType_ID = b.C_DocType_ID " +
				  " WHERE a.AD_Client_ID =" + this.acctStatusFilters.adClientID +
				  whereOrg +
				  " AND a.DateTrx <? " + 
				  " AND a.DocStatus IN ('CO','CL')" +
				  " AND a.C_BPartner_ID =" + cBPartnerID + whereCurrency +
				  " AND b.IsSOTrx='Y'" +
				  " AND b.DocBaseType='ARR'" +
				  " AND b.SP029C_DeferredDocument='N'" +
				  " AND a.UY_PayReceipt_ID IS NULL" +
				  " AND a.SP028C_CR_ID IS NULL" +
				  " AND a.C_Charge_ID IS NULL";

			pstmt = DB.prepareStatement(sql, null);
			pstmt.setTimestamp(1, this.acctStatusFilters.dateFrom);			
			rs = pstmt.executeQuery();
		
			if (rs.next()){
				amtPay = rs.getBigDecimal("Balance");
			}

			//Collection receipts
			sql = "SELECT COALESCE(SUM(a.PayAmt),0) AS Balance" +
					" FROM UY_PayReceipt a" +
					" INNER JOIN C_DocType b ON a.C_DocType_ID = b.C_DocType_ID" +
					" WHERE a.AD_Client_ID = " + this.acctStatusFilters.adClientID + whereOrg +
					" AND a.DateDoc <? " +
					" AND a.DocStatus = 'CO'" +
					" AND a.IsMultiCurrency = 'N'" +
					" AND b.DocBaseType = 'BRR'" +
					" AND a.IsSOTrx = 'Y'" +
					" AND a.C_BPartner_ID = " + cBPartnerID + whereCurrency;

			pstmt2 = DB.prepareStatement(sql, null);
			pstmt2.setTimestamp(1, this.acctStatusFilters.dateFrom);
			rs2 = pstmt2.executeQuery();

			if (rs2.next()){
				amtReceipt = rs2.getBigDecimal("Balance");
			}

			//Collection receipts v2
			sql = "SELECT COALESCE(SUM(a.PayAmt),0) AS Balance" +
					" FROM SP028C_CR a" +
					" INNER JOIN C_DocType b ON a.C_DocType_ID = b.C_DocType_ID" +
					" WHERE a.AD_Client_ID = " + this.acctStatusFilters.adClientID + whereOrg +
					" AND a.DateDoc <? " +
					" AND a.DocStatus = 'CO'" +
					" AND a.SP028C_IsMultiCurrency = 'N'" +
					" AND b.DocBaseType = 'BRR'" +
					" AND a.IsSOTrx = 'Y'" +
					" AND a.C_BPartner_ID = " + cBPartnerID + whereCurrency;

			pstmt3 = DB.prepareStatement(sql, null);
			pstmt3.setTimestamp(1, this.acctStatusFilters.dateFrom);
			rs3 = pstmt3.executeQuery();

			if (rs3.next()){
				amtReceipt = amtReceipt.add(rs3.getBigDecimal("Balance"));
			}

			//Collection receipts second currency
			sql = "SELECT COALESCE(SUM(currencyconvert_receipt(a.PayAmt, a.CurrencyRate, a.C_Currency_ID, a.C_Currency_ID_To," +
					" a.AD_Client_ID, a.AD_Org_ID)),0) AS Balance" +
					" FROM UY_PayReceipt a" +
					" INNER JOIN C_DocType b ON a.C_DocType_ID = b.C_DocType_ID" +
					" WHERE a.AD_Client_ID = " + this.acctStatusFilters.adClientID + whereOrg +
					" AND a.DateDoc <? " +
					" AND a.DocStatus = 'CO'" +
					" AND a.IsMultiCurrency = 'Y'" +
					" AND a.C_Currency_ID_To = " + this.acctStatusFilters.cCurrencyID +
					" AND b.DocBaseType = 'BRR'" +
					" AND a.IsSOTrx = 'Y'" +
					" AND a.C_BPartner_ID = " + cBPartnerID;

			pstmt4 = DB.prepareStatement(sql, null);
			pstmt4.setTimestamp(1, this.acctStatusFilters.dateFrom);
			rs4 = pstmt4.executeQuery();

			if (rs4.next()){
				amtReceiptCurr2 = rs4.getBigDecimal("Balance");
			}

			//Collection receipts v2 second currency
			sql = "SELECT COALESCE(SUM(currencyconvert(a.PayAmt, a.C_Currency_ID, a.C_Currency_ID_To, a.DateDoc, a.C_ConversionType_ID," +
					" a.AD_Client_ID, a.AD_Org_ID)),0) AS Balance" +
					" FROM SP028C_CR a" +
					" INNER JOIN C_DocType b ON a.C_DocType_ID = b.C_DocType_ID" +
					" WHERE a.AD_Client_ID = " + this.acctStatusFilters.adClientID + whereOrg +
					" AND a.DateDoc <? " +
					" AND a.DocStatus = 'CO'" +
					" AND a.SP028C_IsMultiCurrency = 'Y'" +
					" AND a.C_Currency_ID_To = " + this.acctStatusFilters.cCurrencyID +
					" AND b.DocBaseType = 'BRR'" +
					" AND a.IsSOTrx = 'Y'" +
					" AND a.C_BPartner_ID = " + cBPartnerID;

			pstmt5 = DB.prepareStatement(sql, null);
			pstmt5.setTimestamp(1, this.acctStatusFilters.dateFrom);
			rs5 = pstmt5.executeQuery();

			if (rs5.next()){
				amtReceiptCurr2 = amtReceiptCurr2.add(rs5.getBigDecimal("Balance"));
			}

			value = amtPay.add(amtReceipt).add(amtReceiptCurr2);

		}
		catch (Exception e)
		{
			throw e;
		}
		finally
		{
			DB.close(rs, pstmt);
			DB.close(rs2, pstmt2);
			DB.close(rs3, pstmt3);
			DB.close(rs4, pstmt4);
			DB.close(rs5, pstmt5);
		}
		return value;		
	}

	
	/**
	 * OpenUp.	
	 * Description: Gets opening balance from purchase credit notes for a given vendor.
	 * @param cBPartnerID int : Vendor ID to consider.
	 * @return
	 * @throws Exception
	 */
	private BigDecimal getOpeningDebitVendorInvoice(int cBPartnerID) throws Exception{
		
		String sql = "";
		ResultSet rs = null;
		PreparedStatement pstmt = null;
		BigDecimal value = new BigDecimal(0);
		
		try{
		
			String whereCurrency = "";
			// Currency condition
			if (this.acctStatusFilters.cCurrencyID > 0) whereCurrency = " AND a.C_Currency_ID = " + this.acctStatusFilters.cCurrencyID;

			String whereOrg = "";
			if (this.acctStatusFilters.adOrgID > 0){
				whereOrg = " AND a.AD_Org_ID =" + this.acctStatusFilters.adOrgID;
			} else if(this.acctStatusFilters.adCorporationID > 0){
				whereOrg = " AND a.AD_Org_ID IN (SELECT AD_Org_ID FROM AD_OrgInfo WHERE AD_Corporation_ID = " + this.acctStatusFilters.adCorporationID + ")";
			}

			sql = "SELECT COALESCE(SUM(a.GrandTotal),0) AS Balance " +
				  " FROM C_Invoice a INNER JOIN C_DocType b ON a.C_DocType_ID = b.C_DocType_ID " +
				  " WHERE a.AD_Client_ID =" + this.acctStatusFilters.adClientID +
				  whereOrg +
				  " AND a.DateInvoiced <? " + 
				  " AND a.DocStatus = 'CO' " +
				  " AND a.C_BPartner_ID =" + cBPartnerID + whereCurrency +
				  " AND b.IsSOTrx='N'" +
				  " AND b.DocBaseType = 'APC'" +
				  " AND b.SP029C_DeferredDocument = 'N'";

			pstmt = DB.prepareStatement(sql, null);
			pstmt.setTimestamp(1, this.acctStatusFilters.dateFrom);			
			rs = pstmt.executeQuery();
		
			if (rs.next()){
				value = rs.getBigDecimal("Balance");
			}
		}
		catch (Exception e)
		{
			throw e;
		}
		finally
		{
			DB.close(rs, pstmt);
			rs = null; pstmt = null;
		}
		return value;		
	}

	
	/**
	 * OpenUp.	
	 * Description: Gets opening balance from purchase payments for a given vendor.
	 * @param cBPartnerID int : Vendor ID to consider.
	 * @return
	 * @throws Exception
	 */
	private BigDecimal getOpeningDebitVendorPayment(int cBPartnerID) throws Exception{
		
		String sql = "";
		ResultSet rs = null;
		PreparedStatement pstmt = null;
		ResultSet rs2 = null;
		PreparedStatement pstmt2 = null;
		ResultSet rs3 = null;
		PreparedStatement pstmt3 = null;
		ResultSet rs4 = null;
		PreparedStatement pstmt4 = null;
		ResultSet rs5 = null;
		PreparedStatement pstmt5 = null;
		BigDecimal value;
		BigDecimal amtPay = new BigDecimal(0);
		BigDecimal amtReceipt = new BigDecimal(0);
		BigDecimal amtReceiptCurr2 = new BigDecimal(0);
		
		try{
		
			String whereCurrency = "";
			// Currency condition
			if (this.acctStatusFilters.cCurrencyID > 0) whereCurrency = " AND a.C_Currency_ID = " + this.acctStatusFilters.cCurrencyID;

			String whereOrg = "";
			if (this.acctStatusFilters.adOrgID > 0){
				whereOrg = " AND a.AD_Org_ID =" + this.acctStatusFilters.adOrgID;
			} else if(this.acctStatusFilters.adCorporationID > 0){
				whereOrg = " AND a.AD_Org_ID IN (SELECT AD_Org_ID FROM AD_OrgInfo WHERE AD_Corporation_ID = " + this.acctStatusFilters.adCorporationID + ")";
			}
			
			sql = "SELECT COALESCE(SUM(a.PayAmt),0) AS Balance " +
				  " FROM C_Payment a INNER JOIN C_DocType b ON a.C_DocType_ID = b.C_DocType_ID " +
				  " WHERE a.AD_Client_ID =" + this.acctStatusFilters.adClientID +
				  whereOrg +
				  " AND a.DateTrx <? " + 
				  " AND a.DocStatus = 'CO' " +
				  " AND a.C_BPartner_ID =" + cBPartnerID + whereCurrency +
				  " AND b.IsSOTrx='N'" +
				  " AND b.DocBaseType='APP'" +
				  " AND b.SP029C_DeferredDocument='N'" +
				  " AND a.UY_PayReceipt_ID IS NULL" +
				  " AND a.SP028C_CR_ID IS NULL";

			pstmt = DB.prepareStatement(sql, null);
			pstmt.setTimestamp(1, this.acctStatusFilters.dateFrom);			
			rs = pstmt.executeQuery();
		
			if (rs.next()){
				amtPay = rs.getBigDecimal("Balance");
			}

			//Payment receipts
			sql = "SELECT COALESCE(SUM(a.PayAmt),0) AS Balance" +
					" FROM UY_PayReceipt a" +
					" INNER JOIN C_DocType b ON a.C_DocType_ID = b.C_DocType_ID" +
					" WHERE a.AD_Client_ID = " + this.acctStatusFilters.adClientID + whereOrg +
					" AND a.DateDoc <? " +
					" AND a.DocStatus = 'CO'" +
					" AND a.IsMultiCurrency = 'N'" +
					" AND b.DocBaseType = 'BPP'" +
					" AND a.IsSOTrx = 'N'" +
					" AND a.C_BPartner_ID = " + cBPartnerID + whereCurrency;

			pstmt2 = DB.prepareStatement(sql, null);
			pstmt2.setTimestamp(1, this.acctStatusFilters.dateFrom);
			rs2 = pstmt2.executeQuery();

			if (rs2.next()){
				amtReceipt = rs2.getBigDecimal("Balance");
			}

			//Payment receipts v2
			sql = "SELECT COALESCE(SUM(a.PayAmt),0) AS Balance" +
					" FROM SP028C_CR a" +
					" INNER JOIN C_DocType b ON a.C_DocType_ID = b.C_DocType_ID" +
					" WHERE a.AD_Client_ID = " + this.acctStatusFilters.adClientID + whereOrg +
					" AND a.DateDoc <? " +
					" AND a.DocStatus = 'CO'" +
					" AND a.SP028C_IsMultiCurrency = 'N'" +
					" AND b.DocBaseType = 'BPP'" +
					" AND a.IsSOTrx = 'N'" +
					" AND a.C_BPartner_ID = " + cBPartnerID + whereCurrency;

			pstmt3 = DB.prepareStatement(sql, null);
			pstmt3.setTimestamp(1, this.acctStatusFilters.dateFrom);
			rs3 = pstmt3.executeQuery();

			if (rs3.next()){
				amtReceipt = amtReceipt.add(rs3.getBigDecimal("Balance"));
			}

			//Payment receipts second currency
			sql = "SELECT COALESCE(SUM(currencyconvert_receipt(a.PayAmt, a.CurrencyRate, a.C_Currency_ID, a.C_Currency_ID_To," +
					" a.AD_Client_ID, a.AD_Org_ID)),0) AS Balance" +
					" FROM UY_PayReceipt a" +
					" INNER JOIN C_DocType b ON a.C_DocType_ID = b.C_DocType_ID" +
					" WHERE a.AD_Client_ID = " + this.acctStatusFilters.adClientID + whereOrg +
					" AND a.DateDoc <? " +
					" AND a.DocStatus = 'CO'" +
					" AND a.IsMultiCurrency = 'Y'" +
					" AND a.C_Currency_ID_To = " + this.acctStatusFilters.cCurrencyID +
					" AND b.DocBaseType = 'BPP'" +
					" AND a.IsSOTrx = 'N'" +
					" AND a.C_BPartner_ID = " + cBPartnerID;

			pstmt4 = DB.prepareStatement(sql, null);
			pstmt4.setTimestamp(1, this.acctStatusFilters.dateFrom);
			rs4 = pstmt4.executeQuery();

			if (rs4.next()){
				amtReceiptCurr2 = rs4.getBigDecimal("Balance");
			}

			//Payment receipts v2 second currency
			sql = "SELECT COALESCE(SUM(currencyconvert(a.PayAmt, a.C_Currency_ID, a.C_Currency_ID_To, a.DateDoc, a.C_ConversionType_ID," +
					" a.AD_Client_ID, a.AD_Org_ID)),0) AS Balance" +
					" FROM SP028C_CR a" +
					" INNER JOIN C_DocType b ON a.C_Doctype_ID = b.C_Doctype_ID" +
					" WHERE a.AD_Client_ID = " + this.acctStatusFilters.adClientID + whereOrg +
					" AND a.DateDoc <? " +
					" AND a.DocStatus = 'CO'" +
					" AND a.SP028C_IsMultiCurrency = 'Y'" +
					" AND a.C_Currency_ID_To = " + this.acctStatusFilters.cCurrencyID +
					" AND b.DocBaseType = 'BPP'" +
					" AND a.IsSOTrx = 'N'" +
					" AND a.C_BPartner_ID = " + cBPartnerID;

			pstmt5 = DB.prepareStatement(sql, null);
			pstmt5.setTimestamp(1, this.acctStatusFilters.dateFrom);
			rs5 = pstmt5.executeQuery();

			if (rs5.next()){
				amtReceiptCurr2 = amtReceiptCurr2.add(rs5.getBigDecimal("Balance"));
			}

			value = amtPay.add(amtReceipt).add(amtReceiptCurr2);
		}
		catch (Exception e)
		{
			throw e;
		}
		finally
		{
			DB.close(rs, pstmt);
			DB.close(rs2, pstmt2);
			DB.close(rs3, pstmt3);
			DB.close(rs4, pstmt4);
			DB.close(rs5, pstmt5);
		}
		return value;
	}

	
	/**
	 * OpenUp.	
	 * Description: Gets opening balance from purchase invoices for a given vendor.
	 * @param cBPartnerID int : Vendor ID to consider.
	 * @return
	 * @throws Exception
	 */
	private BigDecimal getOpeningCreditVendorInvoice(int cBPartnerID) throws Exception{
		
		String sql = "";
		ResultSet rs = null;
		PreparedStatement pstmt = null;

		BigDecimal value = new BigDecimal(0);
		
		try{
		
			String whereCurrency = "";
			// Currency condition
			if (this.acctStatusFilters.cCurrencyID > 0) whereCurrency = " AND a.C_Currency_ID = " + this.acctStatusFilters.cCurrencyID;

			String whereOrg = "";
			if (this.acctStatusFilters.adOrgID > 0){
				whereOrg = " AND a.AD_Org_ID =" + this.acctStatusFilters.adOrgID;
			} else if(this.acctStatusFilters.adCorporationID > 0){
				whereOrg = " AND a.AD_Org_ID IN (SELECT AD_Org_ID FROM AD_OrgInfo WHERE AD_Corporation_ID = " + this.acctStatusFilters.adCorporationID + ")";
			}
			
			sql = "SELECT COALESCE(SUM(a.GrandTotal),0) AS Balance " +
				  " FROM C_Invoice a INNER JOIN C_DocType b ON a.C_DocType_ID = b.C_DocType_ID " +
				  " WHERE a.AD_Client_ID =" + this.acctStatusFilters.adClientID +
				  whereOrg +
				  " AND a.DateInvoiced <? " + 
				  " AND a.DocStatus = 'CO' " +
				  //" AND a.ispaid = 'N' " +
				  " AND a.C_BPartner_ID =" + cBPartnerID + whereCurrency +
				  " AND b.IsSOTrx='N'" +
				  " AND b.SP029C_DeferredDocument='N'" +
				  " AND b.DocBaseType = 'API'";

			pstmt = DB.prepareStatement(sql, null);
			pstmt.setTimestamp(1, this.acctStatusFilters.dateFrom);			
			rs = pstmt.executeQuery();
		
			if (rs.next()){
				value = rs.getBigDecimal("Balance");
			}
		}
		catch (Exception e)
		{
			throw e;
		}
		finally
		{
			DB.close(rs, pstmt);
		}
		return value;		
	}

	/**
	 * Solop.
	 * Description: Gets opening balance from allocations for a given customer.
	 * @param cBPartnerID int : Customer ID to consider.
	 * @return
	 * @throws Exception
	 */
	private BigDecimal getOpeningAllocations(int cBPartnerID) throws Exception{

		String sql = "";
		ResultSet rs = null;
		PreparedStatement pstmt = null;

		BigDecimal value = new BigDecimal(0);

		try{

			String whereCurrency = "";
			BigDecimal multiplier = Env.ONE;

			if(!this.acctStatusFilters.IsSOTrx)
				multiplier = multiplier.negate();

			// Currency condition
			if (this.acctStatusFilters.cCurrencyID > 0) whereCurrency = " AND a.C_Currency_ID = " + this.acctStatusFilters.cCurrencyID;

			String whereOrg = "";
			if (this.acctStatusFilters.adOrgID > 0){
				whereOrg = " AND a.AD_Org_ID =" + this.acctStatusFilters.adOrgID;
			} else if(this.acctStatusFilters.adCorporationID > 0){
				whereOrg = " AND a.AD_Org_ID IN (SELECT AD_Org_ID FROM AD_OrgInfo WHERE AD_Corporation_ID = " + this.acctStatusFilters.adCorporationID + ")";
			}

			//Allocations with Charge
			sql = "SELECT COALESCE(SUM(al.Amount),0) AS Balance " +
					" FROM C_AllocationHdr a " +
					" INNER JOIN C_AllocationLine al ON a.C_AllocationHdr_ID = al.C_AllocationHdr_ID " +
					" INNER JOIN C_DocType doc ON a.C_DocType_ID = doc.C_DocType_ID" +
					" INNER JOIN C_Currency cur ON a.C_Currency_ID = cur.C_Currency_ID " +
					" INNER JOIN C_BPartner bp ON al.C_BPartner_ID = bp.C_BPartner_ID " +
					" WHERE a.AD_Client_ID =" + this.acctStatusFilters.adClientID +
					" AND al.C_BPartner_ID = " + cBPartnerID +
					whereOrg + whereCurrency +
					" AND al.C_Charge_ID > 0 AND a.DateTrx < ? " +
					" AND a.DocStatus IN ('CO','CL') " +
					" AND NOT EXISTS (SELECT UY_PayReceipt_ID FROM UY_PayReceipt WHERE C_AllocationHdr_ID = a.C_AllocationHdr_ID)" +
					" AND NOT EXISTS (SELECT SP028C_CR_ID FROM SP028C_CR WHERE C_AllocationHdr_ID = a.C_AllocationHdr_ID)" +
					" AND ((SELECT COUNT(l.C_AllocationLine_ID) FROM C_AllocationLine l" +
					" INNER JOIN C_Invoice i ON l.C_Invoice_ID = i.C_Invoice_ID" +
					" WHERE l.C_AllocationHdr_ID = a.C_AllocationHdr_ID" +
					" AND i.IsSOTrx = '" + this.acctStatusFilters.isSoTrx +"') > 0 OR (SELECT COUNT(l.C_AllocationLine_ID) FROM C_AllocationLine l" +
					" INNER JOIN C_Payment p ON l.C_Payment_ID = p.C_Payment_ID" +
					" WHERE l.C_AllocationHdr_ID = a.C_AllocationHdr_ID" +
					" AND p.IsReceipt = '" + this.acctStatusFilters.isSoTrx + "') > 0)";

			pstmt = DB.prepareStatement(sql, null);
			pstmt.setTimestamp(1, this.acctStatusFilters.dateFrom);
			rs = pstmt.executeQuery();

			if (rs.next()){
				value = rs.getBigDecimal("Balance");
			}

			//Allocations with WriteOff/Discount
			sql = "SELECT COALESCE(SUM(al.WriteOffAmt),0) AS Balance " +
					" FROM C_AllocationHdr a " +
					" INNER JOIN C_AllocationLine al ON a.C_AllocationHdr_ID = al.C_AllocationHdr_ID " +
					" INNER JOIN C_DocType doc ON a.C_DocType_ID = doc.C_DocType_ID" +
					" INNER JOIN C_Currency cur ON a.C_Currency_ID = cur.C_Currency_ID " +
					" INNER JOIN C_BPartner bp ON al.C_BPartner_ID = bp.C_BPartner_ID " +
					" WHERE a.AD_Client_ID =" + this.acctStatusFilters.adClientID +
					" AND al.C_BPartner_ID = " + cBPartnerID +
					whereOrg + whereCurrency +
					" AND al.WriteOffAmt <> 0 AND a.DateTrx < ? " +
					" AND a.DocStatus IN ('CO','CL') " +
					" AND NOT EXISTS (SELECT UY_PayReceipt_ID FROM UY_PayReceipt WHERE C_AllocationHdr_ID = a.C_AllocationHdr_ID)" +
					" AND NOT EXISTS (SELECT SP028C_CR_ID FROM SP028C_CR WHERE C_AllocationHdr_ID = a.C_AllocationHdr_ID)" +
					" AND ((SELECT COUNT(l.C_AllocationLine_ID) FROM C_AllocationLine l" +
					" INNER JOIN C_Invoice i ON l.C_Invoice_ID = i.C_Invoice_ID" +
					" WHERE l.C_AllocationHdr_ID = a.C_AllocationHdr_ID" +
					" AND i.IsSOTrx = '" + this.acctStatusFilters.isSoTrx +"') > 0 OR (SELECT COUNT(l.C_AllocationLine_ID) FROM C_AllocationLine l" +
					" INNER JOIN C_Payment p ON l.C_Payment_ID = p.C_Payment_ID" +
					" WHERE l.C_AllocationHdr_ID = a.C_AllocationHdr_ID" +
					" AND p.IsReceipt = '" + this.acctStatusFilters.isSoTrx + "') > 0)";

			pstmt = DB.prepareStatement(sql, null);
			pstmt.setTimestamp(1, this.acctStatusFilters.dateFrom);
			rs = pstmt.executeQuery();

			if (rs.next()){
				value = value.add(rs.getBigDecimal("Balance"));
			}
		}
		catch (Exception e)
		{
			throw e;
		}
		finally
		{
			DB.close(rs, pstmt);
		}
		return value;
	}

	/***
	 * Returns true if it is a parent customer
	 * @see
	 * @return
	 */
	public boolean isParent(MBPartner mbPartner){

		boolean parent = false;

		String sql = "SELECT C_BPartner_ID FROM C_BPartner WHERE BPartner_Parent_ID = " + mbPartner.get_ID();
		int value = DB.getSQLValueEx(null, sql);

		if(value > 0) parent = true;

		return parent;
	}

	public static class ReportAccountStatus {
		public Timestamp dateFrom;
		public Timestamp dateTo;
		public int cCurrencyID = 0;
		public int customerID = 0;
		public int adUserID = 0;
		public int adClientID = 0;
		public int adOrgID = 0;
		public int adCorporationID = 0;
		public Timestamp today;
		public String isSoTrx = "";
		public boolean IsSOTrx = true;
	}

}
