/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor.AbortPolicy;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

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
import com.sonatype.insight.brain.report.ApplicationReport;
import com.sonatype.insight.brain.report.ReportDataStore;
import com.sonatype.insight.brain.report.ReportEntry;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.shutdown.ShutdownHandler;
import com.sonatype.insight.brain.tenancy.TenantReference;
import com.sonatype.insight.brain.tenancy.TenantThreadPoolExecutor;
import com.sonatype.insight.brain.utils.DefaultExecutorThreadPools;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.json.store.JsonUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.insight.brain.report.ApplicationReport.ReportFile.POLICY_THREATS;
import static com.sonatype.insight.brain.report.ApplicationReport.ReportFile.SUMMARY_JSON;

@Named
@Singleton
public class ApiReportServiceV2
{
  private static final Logger log = LoggerFactory.getLogger(ApiReportServiceV2.class);

  private static final int MAX_POLICY_EVALUATIONS_TO_RETURN = 100;

  private static final int REPORT_HISTORY_THREADS_MIN = 1;

  private static final int REPORT_HISTORY_THREADS_MAX = Integer.MAX_VALUE;

  private static final int REPORT_HISTORY_THREADS_DEFAULT = 10;

  private static final String REPORT_HISTORY_THREADS = "reportHistoryThreads";

  private final PolicyEvaluationDAO policyEvaluationDAO;

  private final ApiApplicationService applicationService;

  private final ApplicationDAO applicationDAO;

  private final ScanPolicyEvaluator scanPolicyEvaluator;

  private final ReportDataStore reportDataStore;

  private final TenantReference<TenantThreadPoolExecutor> reportHistoryExecutors;

  @Inject
  public ApiReportServiceV2(
      PolicyEvaluationDAO policyEvaluationDAO,
      ApiApplicationService applicationService,
      ApplicationDAO applicationDAO,
      ScanPolicyEvaluator scanPolicyEvaluator,
      ReportDataStore reportDataStore,
      ShutdownHandler shutdownHandler)
  {
    this.applicationDAO = applicationDAO;
    this.applicationService = applicationService;
    this.policyEvaluationDAO = policyEvaluationDAO;
    this.scanPolicyEvaluator = scanPolicyEvaluator;
    this.reportDataStore = reportDataStore;
    reportHistoryExecutors = new TenantReference<>(() -> {
      int reportHistoryThreadCount = DefaultExecutorThreadPools.getThreadCount(
          REPORT_HISTORY_THREADS_MIN,
          REPORT_HISTORY_THREADS_MAX,
          REPORT_HISTORY_THREADS_DEFAULT,
          REPORT_HISTORY_THREADS
      );
      TenantThreadPoolExecutor tenantThreadPoolExecutor = new TenantThreadPoolExecutor(
          reportHistoryThreadCount,
          reportHistoryThreadCount,
          5L,
          TimeUnit.SECONDS,
          new LinkedBlockingQueue<>(),
          new ThreadFactoryBuilder().setNameFormat("ReportHistory-%d").build(),
          new AbortPolicy(),
          "report_history",
          getClass().getSimpleName()
      );
      tenantThreadPoolExecutor.allowCoreThreadTimeOut(true);
      shutdownHandler.add(tenantThreadPoolExecutor);
      return tenantThreadPoolExecutor;
    });
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
    apiReportHistoryDTO.reports = new CopyOnWriteArrayList<>();
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

    List<PolicyEvaluation> uniquePolicyEvaluations = policyEvaluations.stream()
        .collect(Collectors.toMap(
            PolicyEvaluation::getScanId,
            Function.identity(),
            (existing, replacement) -> existing))
        .values()
        .stream()
        .toList();

    List<CompletableFuture<Void>> futures = uniquePolicyEvaluations.stream()
        .map(policyEvaluation -> CompletableFuture.runAsync(
            () -> addPolicyEvaluationResult(apiReportHistoryDTO, policyEvaluation, application),
            reportHistoryExecutors.get()))
        .toList();

    CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();

    apiReportHistoryDTO.reports.sort(Comparator.comparing((ApiReportResultsDTO r) -> r.evaluationDate).reversed());
  }

  private void addPolicyEvaluationResult(
      ApiReportHistoryDTO apiReportHistoryDTO,
      PolicyEvaluation policyEvaluation,
      Application application)
  {
    try {
      ApplicationReport applicationReport =
          reportDataStore.getApplicationReport(application, policyEvaluation.getScanId());
      PolicyThreats policyThreats = JsonUtils.parse(
          Objects.requireNonNull(applicationReport.getEntry(POLICY_THREATS.getName())).buf,
          PolicyThreats.class);
      List<PolicyViolation> policyViolations =
          PolicyAlertUtil.getDummyPolicyViolationsFromPolicyThreatsForCounts(policyThreats);

      ReportEntry summaryReportEntry = applicationReport.getEntry(SUMMARY_JSON.getName());
      ApiReportResultsDTO apiReportResultsDTO = new ApiReportResultsDTO(
          policyEvaluation,
          scanPolicyEvaluator.createPolicyEvaluationResult(policyEvaluation, policyViolations, false,
              summaryReportEntry),
          getScannerVersion(summaryReportEntry)
      );
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

  private String getScannerVersion(final ReportEntry summaryReportEntry) throws IOException {
    if (summaryReportEntry == null || summaryReportEntry.buf == null) {
      return null;
    }
    JsonNode summary = JsonUtils.parse(summaryReportEntry.buf);
    return summary.path("scannerVersion").asText();
  }
}
