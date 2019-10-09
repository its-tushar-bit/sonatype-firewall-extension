/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.inject.Inject;

import com.sonatype.insight.brain.api.v2.ApiReportDataResourceV2;
import com.sonatype.insight.brain.api.v2.dto.ApiApplicationReportDTOV2;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.landing.UserInterfaceLinksResource;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.StageType;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.telemetry.ReportsTelemetry;

public class ApiReportServiceV2
{
  private final PolicyEvaluationDAO policyEvaluationDAO;

  private final ApiApplicationService applicationService;

  private final ApplicationDAO applicationDAO;

  private final ReportsTelemetry reportsTelemetry;

  @Inject
  public ApiReportServiceV2(
      PolicyEvaluationDAO policyEvaluationDAO,
      ApiApplicationService applicationService,
      ApplicationDAO applicationDAO,
      ReportsTelemetry reportsTelemetry)
  {
    this.applicationDAO = applicationDAO;
    this.applicationService = applicationService;
    this.policyEvaluationDAO = policyEvaluationDAO;
    this.reportsTelemetry = reportsTelemetry;
  }

  @Authorize(permission = Permission.READ)
  public List<ApiApplicationReportDTOV2> getByApplicationId(
      @AuthzContext(AuthzContext.Key.APPLICATION_ID) String applicationId)
  {
    Application application = applicationDAO.getById(applicationId);

    List<ApiApplicationReportDTOV2> reports = new LinkedList<>();
    addReports(reports, application);

    reportsTelemetry.sendSingleApplicationTelemetry();

    return reports;
  }

  public List<ApiApplicationReportDTOV2> getAll() {
    List<ApiApplicationReportDTOV2> reports = new LinkedList<>();

    for (Application application : applicationService.getApplications(Collections.emptySet())) {
      addReports(reports, application);
    }

    reportsTelemetry.sendAllApplicationsTelemetry();

    return reports;
  }

  private void addReports(List<ApiApplicationReportDTOV2> reports, Application application) {
    for (StageType stageType : StageTypes.getAll()) {
      PolicyEvaluation eval = policyEvaluationDAO.getLastByApplicationIdAndStageId(application.getId(),
          stageType.getId());
      if (eval != null) {
        reports.add(getReportDTO(application, eval));
      }
    }
  }

  private ApiApplicationReportDTOV2 getReportDTO(Application app, PolicyEvaluation eval) {
    ApiApplicationReportDTOV2 report = new ApiApplicationReportDTOV2();

    report.applicationId = app.getId();
    report.evaluationDate = eval.getTime();
    report.stage = eval.getStageTypeId();

    report.reportPdfUrl = UserInterfaceLinksResource.getPdfUrl(app.getPublicId(), eval.getScanId());
    report.reportHtmlUrl = UserInterfaceLinksResource.getReportUrl(app.getPublicId(), eval.getScanId());
    report.embeddableReportHtmlUrl = UserInterfaceLinksResource.getEmbeddableReportUrl(app.getPublicId(),
        eval.getScanId());
    report.reportDataUrl = ApiReportDataResourceV2.getDataUrl(app.getPublicId(), eval.getScanId());

    return report;
  }
}
