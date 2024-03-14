/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.inject.Inject;

import com.sonatype.insight.brain.api.v2.ApiReportDataResourceV2;
import com.sonatype.insight.brain.api.v2.dto.ApiApplicationReportDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiReportHistoryDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiReportResultsDTO;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.landing.UserInterfaceLinksHelper;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.policy.evaluator.PolicyAlertUtil;
import com.sonatype.insight.brain.policy.evaluator.PolicyThreats;
import com.sonatype.insight.brain.policy.evaluator.ScanPolicyEvaluator;
import com.sonatype.insight.brain.report.Report;
import com.sonatype.insight.brain.report.ReportService;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.json.store.JsonUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ApiReportServiceV2
{
  private static final Logger log = LoggerFactory.getLogger(ApiReportServiceV2.class);

  private final PolicyEvaluationDAO policyEvaluationDAO;

  private final ApiApplicationService applicationService;

  private final ApplicationDAO applicationDAO;

  private final ScanPolicyEvaluator scanPolicyEvaluator;

  private final ReportService reportService;

  private static final int MAX_POLICY_EVALUATIONS_TO_RETURN = 100;

  @Inject
  public ApiReportServiceV2(
          PolicyEvaluationDAO policyEvaluationDAO,
          ApiApplicationService applicationService,
          ApplicationDAO applicationDAO,
          ScanPolicyEvaluator scanPolicyEvaluator,
          ReportService reportService)
  {
    this.applicationDAO = applicationDAO;
    this.applicationService = applicationService;
    this.policyEvaluationDAO = policyEvaluationDAO;
    this.scanPolicyEvaluator = scanPolicyEvaluator;
    this.reportService = reportService;
  }

  @Authorize(permission = Permission.READ)
  public List<ApiApplicationReportDTOV2> getByApplicationId(
      @AuthzContext(AuthzContext.Key.APPLICATION_ID) String applicationId)
  {
    Application application = applicationDAO.getById(applicationId);

    return getReports(Collections.singletonList(application));
  }

  public List<ApiApplicationReportDTOV2> getAll() {
    List<Application> apps = applicationService.getApplicationsWithReadPermission(Collections.emptySet());

    return getReports(apps);
  }

  @Authorize(permission = Permission.READ)
  public ApiReportHistoryDTO getReportHistoryForApplication(
      @AuthzContext(AuthzContext.Key.APPLICATION_ID) final String applicationId, String stage, Integer limit)
  {
    Application application = applicationDAO.getByIdNotNull(applicationId);

    if (stage != null && StageTypes.getById(stage) == null) {
      throw new BadRequestException("Invalid stage: " + stage + ".");
    }

    if (limit != null && limit < 1) {
      throw new BadRequestException("Limit must be positive integer.");
    }

    final ApiReportHistoryDTO
        apiReportHistoryDTO = new ApiReportHistoryDTO();
    apiReportHistoryDTO.applicationId = applicationId;
    apiReportHistoryDTO.reports = new ArrayList<>();
    loadReportHistory(apiReportHistoryDTO, application, stage, limit);
    return apiReportHistoryDTO;
  }

  private List<ApiApplicationReportDTOV2> getReports(List<Application> apps) {
    List<ApiApplicationReportDTOV2> reports = new ArrayList<>();

    Map<String, Application> appsById =
        apps.stream().collect(Collectors.toMap(Application::getId, Function.identity()));
    List<PolicyEvaluation> policyEvaluations = policyEvaluationDAO.getLastByApplicationIds(appsById.keySet());
    for (PolicyEvaluation policyEvaluation : policyEvaluations) {
      ApiApplicationReportDTOV2 apiApplicationReportDTOV2 = new ApiApplicationReportDTOV2();
      populateReportDTO(apiApplicationReportDTOV2, appsById.get(policyEvaluation.getApplicationId()), policyEvaluation);
      reports.add(apiApplicationReportDTOV2);
    }

    return reports;
  }

  private void populateReportDTO(ApiApplicationReportDTOV2 report, Application app, PolicyEvaluation eval) {
    report.applicationId = app.getId();
    report.evaluationDate = eval.getTime();
    report.stage = eval.getStageTypeId();

    report.latestReportHtmlUrl = UserInterfaceLinksHelper.getLatestReportUrl(app.getPublicId(), report.stage);

    report.reportPdfUrl = UserInterfaceLinksHelper.getPdfUrl(app.getPublicId(), eval.getScanId());
    report.reportHtmlUrl = UserInterfaceLinksHelper.getReportUrl(app.getPublicId(), eval.getScanId());
    report.embeddableReportHtmlUrl = UserInterfaceLinksHelper.getEmbeddableReportUrl(app.getPublicId(),
        eval.getScanId());
    report.reportDataUrl = ApiReportDataResourceV2.getDataUrl(app.getPublicId(), eval.getScanId());
  }

  private void loadReportHistory(
      ApiReportHistoryDTO apiReportHistoryDTO,
      Application application,
      String stage,
      Integer limit)
  {
    int maxResultsToReturn = limit != null ? limit : MAX_POLICY_EVALUATIONS_TO_RETURN;
    List<PolicyEvaluation> policyEvaluations =
        policyEvaluationDAO.getLimitedAmountByApplicationId(apiReportHistoryDTO.applicationId, maxResultsToReturn,
            stage);
    Set<String> processedScans = new HashSet<>();
    policyEvaluations.forEach(policyEvaluation -> {
      if (!processedScans.contains(policyEvaluation.getScanId())) {
        addPolicyEvaluationResult(apiReportHistoryDTO, policyEvaluation, application);
        processedScans.add(policyEvaluation.getScanId());
      }
    });
  }

  private void addPolicyEvaluationResult(
      ApiReportHistoryDTO apiReportHistoryDTO,
      PolicyEvaluation policyEvaluation,
      Application application)
  {
    try {
      File reportFile = reportService.getReport(policyEvaluation.getApplicationId(), policyEvaluation.getScanId());
      PolicyThreats policyThreats = JsonUtils.parse(Objects.requireNonNull(Report.getEntry(reportFile,
              ScanPolicyEvaluator.POLICY_THREATS_FILENAME)).buf, PolicyThreats.class);
      List<PolicyViolation> policyViolations =
              PolicyAlertUtil.getDummyPolicyViolationsFromPolicyThreatsForCounts(policyThreats);

      ApiReportResultsDTO apiReportResultsDTO = new ApiReportResultsDTO(policyEvaluation,
              scanPolicyEvaluator.createPolicyEvaluationResult(policyEvaluation, policyViolations, false));
      populateReportDTO(apiReportResultsDTO, application, policyEvaluation);
      apiReportHistoryDTO.reports.add(apiReportResultsDTO);
    }
    catch (IOException | NullPointerException e) {
      log.debug("Could not load violations from report file for application {} scan {}, report is not available",
          policyEvaluation.getApplicationId(), policyEvaluation.getScanId(), e);
    }
    catch (NotFoundException e) {
      log.debug("Could not load violations from report file for application {} scan {}, report is not available.",
          policyEvaluation.getApplicationId(), policyEvaluation.getScanId());
    }
  }
}
