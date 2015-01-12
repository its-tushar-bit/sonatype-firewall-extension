/*
 * Copyright (c) 2011-2015 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.inject.Inject;

import com.sonatype.insight.brain.api.v1.service.ApiApplicationService;
import com.sonatype.insight.brain.api.v2.ApiReportDataResourceV2;
import com.sonatype.insight.brain.api.v2.dto.ApiApplicationReportDTO;
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

public class ApiReportServiceV2
{

  private final PolicyEvaluationDAO policyEvaluationDAO;

  private final ApiApplicationService applicationService;

  private final ApplicationDAO applicationDAO;

  @Inject
  public ApiReportServiceV2(PolicyEvaluationDAO policyEvaluationDAO, ApiApplicationService applicationService,
      ApplicationDAO applicationDAO)
  {
    this.applicationDAO = applicationDAO;
    this.applicationService = applicationService;
    this.policyEvaluationDAO = policyEvaluationDAO;
  }

  @Authorize(permission = Permission.READ)
  public List<ApiApplicationReportDTO> getByApplicationId(
      @AuthzContext(AuthzContext.Key.APPLICATION_ID) String applicationId)
  {
    Application application = applicationDAO.getById(applicationId);

    List<ApiApplicationReportDTO> reports = new LinkedList<>();
    addReports(reports, application);
    return reports;
  }

  public List<ApiApplicationReportDTO> getAll() {
    List<ApiApplicationReportDTO> reports = new LinkedList<>();

    for (Application application : applicationService.getApplications(Collections.<String> emptySet())) {
      addReports(reports, application);
    }

    return reports;
  }

  private void addReports(List<ApiApplicationReportDTO> reports, Application application) {
    for (StageType stageType : StageTypes.getAll()) {
      PolicyEvaluation eval = policyEvaluationDAO.getLastByApplicationIdAndStageId(application.getId(),
          stageType.getId());
      if (eval != null) {
        reports.add(getReportDTO(application, eval));
      }
    }
  }

  private ApiApplicationReportDTO getReportDTO(Application app, PolicyEvaluation eval) {
    ApiApplicationReportDTO report = new ApiApplicationReportDTO();

    report.applicationId = app.getId();
    report.evaluationDate = eval.getTime();
    report.stage = eval.getStageTypeId();

    report.reportPdfUrl = UserInterfaceLinksResource.getPdfUrl(app.getPublicId(), eval.getScanId());
    report.reportHtmlUrl = UserInterfaceLinksResource.getReportUrl(app.getPublicId(), eval.getScanId());
    report.reportDataUrl = ApiReportDataResourceV2.getDataUrl(app.getPublicId(), eval.getScanId());

    return report;
  }
}
