/******************************************************************************
 * Product: Adempiere ERP & CRM Smart Business Solution                       *
 * Copyright (C) 1999-2006 ComPiere, Inc. All Rights Reserved.                *
 * This program is free software; you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program; if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 * For the text or an alternative of this public license, you may reach us    *
 * ComPiere, Inc., 2620 Augustine Dr. #245, Santa Clara, CA 95054, USA        *
 * or via info@compiere.org or http://www.compiere.org/license.html           *
 *****************************************************************************/
package org.spin.report_engine.service;

import java.util.logging.Level;

import org.adempiere.core.domains.models.I_AD_Process;
import org.adempiere.util.ProcessUtil;
import org.compiere.model.MProcess;
import org.compiere.model.MRule;
import org.compiere.process.ProcessInfo;
import org.compiere.process.ProcessInfoUtil;
import org.compiere.util.CLogger;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.compiere.util.Trx;
import org.compiere.wf.MWFProcess;

public class ReportProcessor {
	
	/**	Static Logger	*/
	private static CLogger	log	= CLogger.getCLogger (ReportProcessor.class);
	/** Process Info */
	private ProcessInfo processInfo;
	private Trx transaction;

	public static ReportProcessor newInstance() {
		return new ReportProcessor();
	}
	
	public ProcessInfo getProcessInfo() {
		return processInfo;
	}

	public ReportProcessor withProcessInfo(ProcessInfo processInfo) {
		this.processInfo = processInfo;
		return this;
	}

	public Trx getTransaction() {
		return transaction;
	}

	public ReportProcessor withTransactionName(String transactionName) {
		this.transaction = Trx.get(transactionName, false);
		return this;
	}

	/**
	 *	Execute Process Instance and Lock UI.
	 *  Calls lockUI and unlockUI if parent is a ASyncProcess
	 *  <pre>
	 *		- Get Process Information
	 *      - Call Class
	 *		- Submit SQL Procedure
	 *		- Run SQL Procedure
	 *	</pre>
	 */
	public void run () {
		log.fine("AD_PInstance_ID=" + processInfo.getAD_PInstance_ID()
			+ ", Record_ID=" + processInfo.getRecord_ID());

		//	Get Process Information: Name, Procedure Name, ClassName, IsReport, IsDirectPrint
		String procedureName = "";
		//	Get Process data
		MProcess process = null;
		//	
		if(processInfo.getAD_Process_ID() != 0) {
			process = MProcess.get(Env.getCtx(), processInfo.getAD_Process_ID());
		} else {
			process = MProcess.getFromInstance(Env.getCtx(), processInfo.getAD_PInstance_ID());
			processInfo.setAD_Process_ID(process.getAD_Process_ID());
		}
		//	
		if(process.getAD_Process_ID() <= 0) {
			processInfo.setSummary (Msg.parseTranslation(Env.getCtx(), "@AD_Process_ID@ @NotFound@"), true);
			log.log(Level.SEVERE, "run", "AD_Process_ID=" + processInfo.getAD_Process_ID() + " Not Found");
			return;
		}
		//	Set values from process
		processInfo.setTitle (process.get_Translation(I_AD_Process.COLUMNNAME_Name));
		procedureName = process.getProcedureName();
		processInfo.setClassName(process.getClassname());
		//
		int estimate = process.getEstimatedSeconds();
		if (estimate != 0) {
			processInfo.setEstSeconds (estimate + 1);     //  admin overhead
		}
		//  No PL/SQL Procedure
		if (procedureName == null)
			procedureName = "";
		
		/**********************************************************************
		 *	Workflow
		 */
		if (process.isWorkflow())	
		{
			startWorkflow (process.getAD_Workflow_ID());
			return;
		}

		// Clear Jasper Report class if default - to be executed later
		if (process.isJasper()) {
			if (ProcessUtil.JASPER_STARTER_CLASS.equals(processInfo.getClassName())) {
				processInfo.setClassName(null);
			}
		}
		/**********************************************************************
		 *	Start Optional Class
		 */
		if (processInfo.getClassName() != null) {
			if (process.isJasper()) {
				processInfo.setReportingProcess(true);
			}
			//	Run Class
			if (!startProcess()) {
				return;
			}

			//  No Optional SQL procedure ... done
			if (!process.isReport() && procedureName.length() == 0) {
				return;
			}
			//  No Optional Report ... done
			if (process.isReport() && process.getAD_ReportView_ID() == 0 && ! process.isJasper()) {
				return;
			}
		}

		/**********************************************************************
		 *	Report submission
		 */
		//	Optional Pre-Report Process
		if (process.isReport() && procedureName.length() > 0) {	
			processInfo.setReportingProcess(true);
			if (!startDBProcess(procedureName)) {
				return;
			}
		}	//	Pre-Report

		if (process.isJasper()) {
			processInfo.setReportingProcess(true);
			processInfo.setClassName(ProcessUtil.JASPER_STARTER_CLASS);
			startProcess();
			return;
		}
		
		if (process.isReport()) {
			processInfo.setReportingProcess(true);
			processInfo.setSummary("Report", true);
		}
		/**********************************************************************
		 * 	Process submission
		 */
		else {
			if (procedureName.length() > 0 
					&& !startDBProcess (procedureName)) {
				return;
			}
			//	Success - getResult
			ProcessInfoUtil.setSummaryFromDB(processInfo);
		}
	}   //  run
	
	/**************************************************************************
	 *  Start Workflow.
	 *
	 *  @param workflowId workflow
	 *  @return     true if started
	 */
	private boolean startWorkflow (int workflowId) {
		log.fine(workflowId + " - " + processInfo);
		if (transaction != null) {
			processInfo.setTransactionName(transaction.getTrxName());
		}
		MWFProcess wfProcess = ProcessUtil.startWorkFlow(Env.getCtx(), processInfo, workflowId);
		return wfProcess != null;
	}   //  startWorkflow

	/**************************************************************************
	 *  Start Java Process Class.
	 *      instanciate the class implementing the interface ProcessCall.
	 *  The class can be a Server/Client class (when in Package
	 *  org adempiere.process or org.compiere.model) or a client only class
	 *  (e.g. in org.compiere.report)
	 *
	 *  @return     true if success
	 */
	private boolean startProcess() {
		log.fine(processInfo.toString());
		//	Run locally
		if (processInfo.getClassName().toLowerCase().startsWith(MRule.SCRIPT_PREFIX)) {
			return ProcessUtil.startScriptProcess(Env.getCtx(), processInfo, transaction);
		} else {
			if(processInfo.isManagedTransaction()) {
				return ProcessUtil.startJavaProcess(Env.getCtx(), processInfo, transaction);
			} else {
				return ProcessUtil.startJavaProcess(Env.getCtx(), processInfo, transaction, processInfo.isManagedTransaction());
			}
		}
	}   //  startProcess


	/**************************************************************************
	 *  Start Database Process
	 *  @param ProcedureName PL/SQL procedure name
	 *  @return true if success
	 */
	private boolean startDBProcess (String ProcedureName) {
		//  execute on this thread/connection
		log.fine(ProcedureName + "(" + processInfo.getAD_PInstance_ID() + ")");
		if (processInfo.isManagedTransaction()) {
			return ProcessUtil.startDatabaseProcedure(processInfo, ProcedureName, transaction);
		} else {
			return ProcessUtil.startDatabaseProcedure(processInfo, ProcedureName, transaction, processInfo.isManagedTransaction());
		}
	}   //  startDBProcess
}
