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
package org.spin.report_engine.controller;

import org.compiere.util.CLogger;
import org.spin.backend.grpc.report_engine.ReportEngineGrpc.ReportEngineImplBase;
import org.spin.report_engine.service.Service;
import org.spin.backend.grpc.report_engine.GetReportRequest;
import org.spin.backend.grpc.report_engine.GetSystemInfoRequest;
import org.spin.backend.grpc.report_engine.Report;
import org.spin.backend.grpc.report_engine.RunExportRequest;
import org.spin.backend.grpc.report_engine.RunExportResponse;
import org.spin.backend.grpc.report_engine.SystemInfo;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;

public class ReportService extends ReportEngineImplBase {

	/**	Logger			*/
	private CLogger log = CLogger.getCLogger(ReportService.class);


	@Override
	public void getSystemInfo(GetSystemInfoRequest request, StreamObserver<SystemInfo> responseObserver) {
		try {
			SystemInfo.Builder builder = Service.getSystemInfo();
			responseObserver.onNext(builder.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.warning(e.getLocalizedMessage());
			e.printStackTrace();
			responseObserver.onError(
				Status.INTERNAL
				.withDescription(e.getLocalizedMessage())
				.withCause(e)
				.asRuntimeException()
			);
		}
	}

	@Override
	public void getReport(GetReportRequest request, StreamObserver<Report> responseObserver) {
		try {
			Report.Builder reportBuilder = Service.getReport(request);
			responseObserver.onNext(reportBuilder.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.warning(e.getLocalizedMessage());
			e.printStackTrace();
			responseObserver.onError(
				Status.INTERNAL
					.withDescription(e.getLocalizedMessage())
					.withCause(e)
					.asRuntimeException()
			);
		}
	}

	@Override
	public void getView(GetReportRequest request, StreamObserver<Report> responseObserver) {
		try {
			Report.Builder reportBuilder = Service.getView(request);
			responseObserver.onNext(reportBuilder.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.warning(e.getLocalizedMessage());
			e.printStackTrace();
			responseObserver.onError(
				Status.INTERNAL
					.withDescription(e.getLocalizedMessage())
					.withCause(e)
					.asRuntimeException()
			);
		}
	}

	@Override
	public void runExport(RunExportRequest request, StreamObserver<RunExportResponse> responseObserver) {
		try {
			RunExportResponse.Builder reportBuilder = Service.getExportReport(request);
			responseObserver.onNext(reportBuilder.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.warning(e.getLocalizedMessage());
			e.printStackTrace();
			responseObserver.onError(
				Status.INTERNAL
					.withDescription(e.getLocalizedMessage())
					.withCause(e)
					.asRuntimeException()
			);
		}
	}

}
