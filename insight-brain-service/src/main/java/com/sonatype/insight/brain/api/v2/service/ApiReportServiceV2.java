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
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.model.repository.HostedRepositoryComponent;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.policy.evaluator.PolicyAlertUtil;
import com.sonatype.insight.brain.policy.evaluator.PolicyThreats;
import com.sonatype.insight.brain.policy.evaluator.ScanPolicyEvaluator;
import com.sonatype.insight.brain.report.LifecycleReport;
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

import static com.sonatype.insight.brain.report.LifecycleReport.ReportFile.POLICY_THREATS;
import static com.sonatype.insight.brain.report.LifecycleReport.ReportFile.SUMMARY_JSON;

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
          REPORT_HISTORY_THREADS);
      TenantThreadPoolExecutor tenantThreadPoolExecutor = new TenantThreadPoolExecutor(
          reportHistoryThreadCount,
          reportHistoryThreadCount,
          5L,
          TimeUnit.SECONDS,
          new LinkedBlockingQueue<>(),
          new ThreadFactoryBuilder().setNameFormat("ReportHistory-%d").build(),
          new AbortPolicy(),
          "report_history",
          getClass().getSimpleName());
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
  public ApiReportHistoryDTO getReportHistoryForOwner(
      @AuthzContext(AuthzContext.Key.OWNER) final Owner owner,
      final String stage,
      final Integer limit)
  {
    return buildReportHistory(owner, stage, limit);
  }

  private ApiReportHistoryDTO buildReportHistory(final Owner owner, final String stage, final Integer limit) {
    if (stage != null && StageTypes.getById(stage) == null) {
      throw new BadRequestException("Invalid stage: " + stage + ".");
    }

    int effectiveLimit = resolveHistoryLimit(limit);

    final ApiReportHistoryDTO apiReportHistoryDTO = new ApiReportHistoryDTO();
    apiReportHistoryDTO.applicationId = owner.getId();
    apiReportHistoryDTO.reports = new CopyOnWriteArrayList<>();
    loadReportHistory(apiReportHistoryDTO, owner, stage, effectiveLimit);
    return apiReportHistoryDTO;
  }

  /**
   * Resolve the effective history page size. Null uses the documented default; values above the
   * maximum are clamped so a caller cannot trigger unbounded per-row report file reads against disk
   * or S3.
   */
  static int resolveHistoryLimit(Integer limit) {
    if (limit == null) {
      return MAX_POLICY_EVALUATIONS_TO_RETURN;
    }
    if (limit < 1) {
      throw new BadRequestException("Limit must be positive integer.");
    }
    return Math.min(limit, MAX_POLICY_EVALUATIONS_TO_RETURN);
  }

  private List<ApiApplicationReportDTOV2> getReports(List<Application> apps) {
    List<ApiApplicationReportDTOV2> reports = new ArrayList<>();

    Map<String, Application> appsById =
        apps.stream().collect(Collectors.toMap(Application::getId, Function.identity()));
    List<PolicyEvaluation> policyEvaluations = policyEvaluationDAO.getLastByOwnerIds(appsById.keySet());
    for (PolicyEvaluation policyEvaluation : policyEvaluations) {
      ApiApplicationReportDTOV2 apiApplicationReportDTOV2 = new ApiApplicationReportDTOV2();
      populateReportDTO(apiApplicationReportDTOV2, appsById.get(policyEvaluation.getOwnerId()), policyEvaluation);
      reports.add(apiApplicationReportDTOV2);
    }

    return reports;
  }

  private void populateReportDTO(ApiApplicationReportDTOV2 report, Owner owner, PolicyEvaluation eval) {
    report.applicationId = owner.getId();
    report.evaluationDate = eval.getTime();
    report.stage = eval.getStageTypeId();

    if (owner instanceof Application) {
      String publicId = owner.getPublicId();
      report.latestReportHtmlUrl = UserInterfaceLinksHelper.getLatestReportUrl(publicId, report.stage);
      report.reportPdfUrl = UserInterfaceLinksHelper.getPdfUrl(publicId, eval.getScanId());
      report.reportHtmlUrl = UserInterfaceLinksHelper.getReportUrl(publicId, eval.getScanId());
      report.embeddableReportHtmlUrl = UserInterfaceLinksHelper.getEmbeddableReportUrl(publicId, eval.getScanId());
      report.reportDataUrl = ApiReportDataResourceV2.getDataUrl(publicId, eval.getScanId());
    }
    else if (owner instanceof HostedRepositoryComponent) {
      report.latestReportHtmlUrl =
          UserInterfaceLinksHelper.getHostedRepositoryComponentLatestReportUrl(owner.getId(), report.stage);
      report.reportPdfUrl =
          UserInterfaceLinksHelper.getHostedRepositoryComponentPdfUrl(owner.getId(), eval.getScanId());
      report.reportHtmlUrl =
          UserInterfaceLinksHelper.getHostedRepositoryComponentReportUrl(owner.getId(), eval.getScanId());
      report.embeddableReportHtmlUrl =
          UserInterfaceLinksHelper.getHostedRepositoryComponentEmbeddableReportUrl(owner.getId(), eval.getScanId());
      report.reportDataUrl =
          ApiReportDataResourceV2.getHostedRepositoryComponentDataUrl(owner.getId(), eval.getScanId());
    }
  }

  private void loadReportHistory(
      ApiReportHistoryDTO apiReportHistoryDTO,
      Owner owner,
      String stage,
      int maxResultsToReturn)
  {
    List<PolicyEvaluation> policyEvaluations =
        policyEvaluationDAO.getLimitedAmountByOwnerId(apiReportHistoryDTO.applicationId, maxResultsToReturn,
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
            () -> addPolicyEvaluationResult(apiReportHistoryDTO, policyEvaluation, owner),
            reportHistoryExecutors.get()))
        .toList();

    CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();

    apiReportHistoryDTO.reports.sort(Comparator.comparing((ApiReportResultsDTO r) -> r.evaluationDate).reversed());
  }

  private void addPolicyEvaluationResult(
      ApiReportHistoryDTO apiReportHistoryDTO,
      PolicyEvaluation policyEvaluation,
      Owner owner)
  {
    try {
      LifecycleReport applicationReport =
          reportDataStore.getLifecycleReport(owner, policyEvaluation.getScanId());
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
          getScannerVersion(summaryReportEntry));
      populateReportDTO(apiReportResultsDTO, owner, policyEvaluation);
      apiReportHistoryDTO.reports.add(apiReportResultsDTO);
    }
    catch (IOException | NullPointerException e) {
      log.debug("Could not load violations from report file for owner {} scan {}, report is not available",
          policyEvaluation.getOwnerId(), policyEvaluation.getScanId(), e);
    }
    catch (NotFoundException e) {
      log.debug("Could not load violations from report file for owner {} scan {}, report is not available.",
          policyEvaluation.getOwnerId(), policyEvaluation.getScanId());
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
