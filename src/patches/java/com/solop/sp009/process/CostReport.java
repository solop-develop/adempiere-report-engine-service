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

import org.compiere.util.AdempiereUserError;
import org.compiere.util.DB;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/** Generated Process for (Cost Report)
 *  @author ADempiere (generated)
 *  @version Release 3.9.4
 */
public class CostReport extends CostReportAbstract
{
	private static final String TABLE_NAME = "T_SP009_CostReport";
	private static final String SEQUENCE_NAME = "t_sp009_costreport_seq";

	@Override
	protected void prepare()
	{
		super.prepare();
	}

	@Override
	protected String doIt() throws Exception
	{
		if (getOrgId() <= 0)
			throw new AdempiereUserError("@FillMandatory@ @AD_Org_ID@");

		int pInstanceId = getAD_PInstance_ID();
		int clientId = getAD_Client_ID();
		int userId = getAD_User_ID();
		Timestamp asOf = getDateTo() != null ? getDateTo() : new Timestamp(System.currentTimeMillis());
		Timestamp now = new Timestamp(System.currentTimeMillis());

		// Defensive cleanup in case this instance is re-run
		DB.executeUpdateEx("DELETE FROM " + TABLE_NAME + " WHERE AD_PInstance_ID=?",
				new Object[] {pInstanceId}, get_TrxName());

		StringBuilder sql = new StringBuilder();
		sql.append("WITH last_expedient AS ( ")
			.append("    SELECT DISTINCT ON (pe.m_product_id) ")
			.append("        pe.m_product_id, e.sp009_expedient_id, e.datedoc ")
			.append("    FROM sp009_productexpedient pe ")
			.append("    JOIN sp009_expedient e ON e.sp009_expedient_id = pe.sp009_expedient_id ")
			.append("    WHERE e.docstatus IN ('CO', 'CL') AND e.datedoc <= ? ")
			.append("    ORDER BY pe.m_product_id, e.datedoc DESC, e.sp009_expedient_id DESC ")
			.append("), ")
			.append("origin_cost AS ( ")
			.append("    SELECT DISTINCT ON (le.m_product_id) ")
			.append("        le.m_product_id, cd.currentcostprice AS cost ")
			.append("    FROM last_expedient le ")
			.append("    JOIN c_invoice i      ON i.sp009_expedient_id = le.sp009_expedient_id ")
			.append("    JOIN c_invoiceline il ON il.c_invoice_id = i.c_invoice_id AND il.m_product_id = le.m_product_id ")
			.append("    JOIN m_costdetail cd  ON cd.c_invoiceline_id = il.c_invoiceline_id ")
			.append("    JOIN m_costelement ce ON ce.m_costelement_id = cd.m_costelement_id AND ce.costelementtype = 'M' ")
			.append("    WHERE i.docstatus IN ('CO', 'CL') AND cd.dateacct <= ? ")
			.append("    ORDER BY le.m_product_id, cd.dateacct DESC, cd.m_costdetail_id DESC ")
			.append("), ")
			.append("landed_cost AS ( ")
			.append("    SELECT last_by_element.m_product_id, last_by_element.ad_org_id, ")
			.append("        SUM(last_by_element.currentcostprice) AS costamt ")
			.append("    FROM ( ")
			.append("        SELECT DISTINCT ON (cd.m_product_id, cd.ad_org_id, cd.m_costelement_id) ")
			.append("            cd.m_product_id, cd.ad_org_id, cd.m_costelement_id, cd.currentcostprice ")
			.append("        FROM m_costdetail cd ")
			.append("        WHERE cd.dateacct <= ? ")
			.append("        ORDER BY cd.m_product_id, cd.ad_org_id, cd.m_costelement_id, cd.dateacct DESC, cd.m_costdetail_id DESC ")
			.append("    ) last_by_element ")
			.append("    GROUP BY last_by_element.m_product_id, last_by_element.ad_org_id ")
			.append("), ")
			.append("current_price AS NOT MATERIALIZED ( ")
			.append("    SELECT DISTINCT ON (pp.m_product_id, plv.m_pricelist_id) ")
			.append("        pp.m_product_id, plv.m_pricelist_id, pp.pricelist ")
			.append("    FROM m_productprice pp ")
			.append("    JOIN m_pricelist_version plv ON plv.m_pricelist_version_id = pp.m_pricelist_version_id ")
			.append("    WHERE pp.isactive = 'Y' AND plv.isactive = 'Y' AND plv.validfrom <= ? ")
			.append("    ORDER BY pp.m_product_id, plv.m_pricelist_id, plv.validfrom DESC ")
			.append("), ")
			.append("last_snapshot_run AS ( ")
			.append("    SELECT m_storagesnapshotrun_id, datelastrun ")
			.append("    FROM m_storagesnapshotrun ")
			.append("    WHERE isactive = 'Y' AND datelastrun <= ? ")
			.append("    ORDER BY datelastrun DESC, m_storagesnapshotrun_id DESC ")
			.append("    LIMIT 1 ")
			.append("), ")
			.append("snapshot_stock AS ( ")
			.append("    SELECT ss.m_product_id, SUM(ss.qtyonhand) AS qtyonhand ")
			.append("    FROM m_storagesnapshot ss ")
			.append("    JOIN last_snapshot_run lsr ON lsr.m_storagesnapshotrun_id = ss.m_storagesnapshotrun_id ")
			.append("    WHERE ss.isactive = 'Y' ")
			.append("    GROUP BY ss.m_product_id ")
			.append("), ")
			.append("snapshot_movement AS ( ")
			.append("    SELECT mt.m_product_id, SUM(mt.movementqty) AS movementqty ")
			.append("    FROM m_transaction mt ")
			.append("    WHERE mt.movementdate > COALESCE((SELECT datelastrun FROM last_snapshot_run), '-infinity'::timestamp) ")
			.append("        AND mt.movementdate <= ? ")
			.append("    GROUP BY mt.m_product_id ")
			.append("), ")
			.append("stock AS ( ")
			.append("    SELECT p.m_product_id, ")
			.append("        COALESCE(ss.qtyonhand, 0) + COALESCE(sm.movementqty, 0) AS qtyonhand ")
			.append("    FROM m_product p ")
			.append("    LEFT JOIN snapshot_stock ss    ON ss.m_product_id = p.m_product_id ")
			.append("    LEFT JOIN snapshot_movement sm ON sm.m_product_id = p.m_product_id ")
			.append("    WHERE p.ad_client_id = ").append(clientId).append(" ")
			.append("), ")
			.append("sales AS ( ")
			.append("    SELECT il.m_product_id, ")
			.append("        SUM(il.qtyinvoiced) AS qtyinvoiced, ")
			.append("        SUM(il.qtyinvoiced) FILTER (WHERE i.dateinvoiced >= ?::timestamp - INTERVAL '30 days') AS qtyinvoiced_30, ")
			.append("        SUM(il.qtyinvoiced) FILTER (WHERE i.dateinvoiced >= ?::timestamp - INTERVAL '60 days') AS qtyinvoiced_60, ")
			.append("        MAX(i.dateinvoiced) AS dateinvoiced ")
			.append("    FROM c_invoiceline il ")
			.append("    JOIN c_invoice i ON i.c_invoice_id = il.c_invoice_id ")
			.append("    WHERE i.issotrx = 'Y' AND i.docstatus IN ('CO', 'CL') AND il.isactive = 'Y' AND i.dateinvoiced <= ? ")
			.append("    GROUP BY il.m_product_id ")
			.append(") ")
			.append("INSERT INTO ").append(TABLE_NAME).append(" (")
			.append("T_SP009_CostReport_ID, AD_Client_ID, AD_Org_ID, AD_PInstance_ID, ")
			.append("Created, CreatedBy, Updated, UpdatedBy, IsActive, ")
			.append("M_Product_ID, DefaultVendor_ID, SP009_LastImportDate, SP009_OriginCost, SP009_ActualCost, ")
			.append("SP009_DistributorPrice, SP009_WholesalePrice, SP009_RetailPrice, ")
			.append("QtyOnHand, SP009_SalesQtyTotal, SP009_SalesQty30d, SP009_SalesQty60d, SP009_LastSaleDate) ")
			.append("SELECT ")
			.append("    nextval('").append(SEQUENCE_NAME).append("'), ")
			.append("    ").append(clientId).append(", ").append(getOrgId()).append(", ").append(pInstanceId).append(", ")
			.append("    ?, ").append(userId).append(", ?, ").append(userId).append(", 'Y', ")
			.append("    p.m_product_id, p.defaultvendor_id, ")
			.append("    le.datedoc, ")
			.append("    oc.cost, ")
			.append("    lc.costamt, ")
			.append("    distributor_price.pricelist, ")
			.append("    wholesale_price.pricelist, ")
			.append("    retail_price.pricelist, ")
			.append("    COALESCE(st.qtyonhand, 0), ")
			.append("    COALESCE(s.qtyinvoiced, 0), ")
			.append("    COALESCE(s.qtyinvoiced_30, 0), ")
			.append("    COALESCE(s.qtyinvoiced_60, 0), ")
			.append("    s.dateinvoiced ")
			.append("FROM m_product p ")
			.append("JOIN ad_orginfo oi ON oi.ad_org_id = ? ")
			.append("LEFT JOIN last_expedient le ON le.m_product_id = p.m_product_id ")
			.append("LEFT JOIN origin_cost oc    ON oc.m_product_id = p.m_product_id ")
			.append("LEFT JOIN landed_cost lc    ON lc.m_product_id = p.m_product_id AND lc.ad_org_id = ? ")
			.append("LEFT JOIN current_price distributor_price ON distributor_price.m_product_id = p.m_product_id AND distributor_price.m_pricelist_id = oi.distributorpricelist_id ")
			.append("LEFT JOIN current_price wholesale_price   ON wholesale_price.m_product_id = p.m_product_id AND wholesale_price.m_pricelist_id = oi.wholesalepricelist_id ")
			.append("LEFT JOIN current_price retail_price      ON retail_price.m_product_id = p.m_product_id AND retail_price.m_pricelist_id = oi.retailpricelist_id ")
			.append("LEFT JOIN stock st ON st.m_product_id = p.m_product_id ")
			.append("LEFT JOIN sales s  ON s.m_product_id = p.m_product_id ")
			.append("WHERE p.isactive = 'Y' AND p.ad_client_id = ? ");

		List<Object> params = new ArrayList<Object>();
		params.add(asOf);          // last_expedient.datedoc <=
		params.add(asOf);          // origin_cost.dateacct <=
		params.add(asOf);          // landed_cost.dateacct <=
		params.add(asOf);          // current_price.validfrom <=
		params.add(asOf);          // last_snapshot_run.datelastrun <=
		params.add(asOf);          // snapshot_movement.movementdate <=
		params.add(asOf);          // sales 30 day window bound
		params.add(asOf);          // sales 60 day window bound
		params.add(asOf);          // sales.dateinvoiced <=
		params.add(now);           // Created
		params.add(now);           // Updated
		params.add(getOrgId());   // ad_orginfo.ad_org_id =
		params.add(getOrgId());   // landed_cost.ad_org_id =
		params.add(clientId);     // p.ad_client_id =

		if (getProductId() > 0)
		{
			sql.append("AND p.m_product_id = ? ");
			params.add(getProductId());
		}
		if (getBPartnerId() > 0)
		{
			sql.append("AND p.defaultvendor_id = ? ");
			params.add(getBPartnerId());
		}
		if (getProductCategoryId() > 0)
		{
			sql.append("AND p.m_product_category_id = ? ");
			params.add(getProductCategoryId());
		}
		if (getProductGroupId() > 0)
		{
			sql.append("AND p.m_product_group_id = ? ");
			params.add(getProductGroupId());
		}
		if (getProductClassId() > 0)
		{
			sql.append("AND p.m_product_class_id = ? ");
			params.add(getProductClassId());
		}
		if (getProductClassificationId() > 0)
		{
			sql.append("AND p.m_product_classification_id = ? ");
			params.add(getProductClassificationId());
		}

		int rows = DB.executeUpdateEx(sql.toString(), params.toArray(), get_TrxName());

		return "@Created@ = " + rows;
	}
}
