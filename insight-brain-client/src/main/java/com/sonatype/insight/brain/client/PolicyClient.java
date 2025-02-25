/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.client;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.Optional;

import com.sonatype.clm.dto.model.ScanReceipt;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationPollingResult;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationReceipt;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationRequestDTO;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationResult;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationStatus;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationSummary;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.clm.dto.model.signature.VulnerabilitySignatureAnalysisDTO;
import com.sonatype.insight.client.utils.HttpClientUtils.Configuration;
import com.sonatype.insight.client.utils.Result;
import com.sonatype.insight.client.utils.UrlUtils;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.scan.model.ClientScanResult;
import com.sonatype.insight.scan.model.ClientScanType;
import com.sonatype.nexus.git.utils.Environment.GitLabCI;
import com.sonatype.nexus.git.utils.repository.RepositoryUrlFinderBuilder;

import org.apache.http.HttpEntity;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.FileEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.clm.dto.model.policy.PolicyEvaluationStatus.FAILED;
import static com.sonatype.clm.dto.model.policy.PolicyEvaluationSubStatus.COMPONENT_ANALYSIS_PENDING;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

public class PolicyClient
    extends AbstractRequestClient
{
  private static final ContentType GZIP_CONTENT_TYPE = ContentType.create("application/x-gzip");

  private static final String CLI_INTEGRATION_PATH = "cli";

  private static final String CI_INTEGRATION_PATH = "ci";

  private static final String REPOSITORY_MANAGER_INTEGRATION_PATH = "rm";

  private static final String BASE_PATH = "rest/integration/applications/";

  private static final String POLLING_PATH = "/evaluations/status/";

  private static final String EVALUATION_SUB_PATH = "/evaluations/";

  private static final String STAGES_SUB_PATH = "/stages/";

  private static final String POLICY_EVALUATION_SUB_PATH = "/policy-evaluation";

  private static final String COMPONENT_ANALYSIS_SUB_PATH = "/component-analysis";

  private static final String SCAN_TYPE = "scanType";

  private static final String STATUS_ID = "statusId";

  private final Logger log;

  private final String serverUrl;

  protected final String appId;

  private final SourceControlClient sourceControlClient;

  public PolicyClient(final Configuration config, final String appId) {
    this(config, appId, null);
  }

  public PolicyClient(final Configuration config, final String appId, final Logger log) {
    super(config);
    this.log = log != null ? log : LoggerFactory.getLogger(PolicyClient.class);
    this.serverUrl = config.getServerUrl();
    this.appId = UrlUtils.encodeUrlComponent(appId);
    this.sourceControlClient = new SourceControlClient(config);
  }

  /**
   * @since 1.69
   */
  public PolicyEvaluationPollingResult evaluateCLI(final ClientScanResult clientScanResult,
                                                   final ClientScanType clientScanType,
                                                   final Stage stage) throws IOException
  {
    return evaluate(CLI_INTEGRATION_PATH, clientScanResult.getScanFile(),
        getClientScanType(clientScanResult, clientScanType), stage);
  }

  /**
   * @since 1.69
   */
  public PolicyEvaluationPollingResult evaluateCI(final ClientScanResult clientScanResult,
                                                  final Stage stage) throws IOException
  {
    return evaluate(CI_INTEGRATION_PATH, clientScanResult.getScanFile(), clientScanResult.getClientScanType(), stage);
  }

  private ClientScanType getClientScanType(ClientScanResult clientScanResult, ClientScanType clientScanType) {
    if (ClientScanType.SONATYPE_THIRD_PARTY.equals(clientScanResult.getClientScanType())) {
      return ClientScanType.SONATYPE_THIRD_PARTY;
    }
    return clientScanType;
  }

  /**
   * @since 1.69
   */
  public PolicyEvaluationPollingResult evaluateRepoMan(final File scanFile,
                                                       final Stage stage) throws IOException
  {
    return evaluate(REPOSITORY_MANAGER_INTEGRATION_PATH, scanFile, ClientScanType.SONATYPE, stage);
  }

  /**
   * @since 1.188
   */
  public PolicyEvaluationPollingResult runPolicyEvaluationForCLI(
      final ClientScanResult clientScanResult,
      final ClientScanType clientScanType,
      final Stage stage,
      final String statusId,
      final VulnerabilitySignatureAnalysisDTO analysisDTO) throws IOException
  {
    return evaluate(
        CLI_INTEGRATION_PATH,
        clientScanResult.getScanFile(),
        getClientScanType(clientScanResult, clientScanType),
        stage,
        statusId,
        analysisDTO
    );
  }

  /**
   * @since 1.188
   */
  public PolicyEvaluationPollingResult runPolicyEvaluationForCI(
      final ClientScanResult clientScanResult,
      final Stage stage,
      final String statusId,
      final VulnerabilitySignatureAnalysisDTO analysisDTO) throws IOException
  {
    return evaluate(
        CI_INTEGRATION_PATH,
        clientScanResult.getScanFile(),
        clientScanResult.getClientScanType(),
        stage,
        statusId,
        analysisDTO
    );
  }

  /**
   * @since 1.188
   */
  public PolicyEvaluationPollingResult runComponentAnalysisForCLI(
      final ClientScanResult clientScanResult,
      final ClientScanType clientScanType,
      final Stage stage) throws IOException
  {
    return runComponentAnalysis(CLI_INTEGRATION_PATH, clientScanResult.getScanFile(),
        getClientScanType(clientScanResult, clientScanType), stage);
  }

  /**
   * @since 1.188
   */
  public PolicyEvaluationPollingResult runComponentAnalysisForCI(
      final ClientScanResult clientScanResult,
      final Stage stage) throws IOException
  {
    return runComponentAnalysis(CI_INTEGRATION_PATH, clientScanResult.getScanFile(),
        clientScanResult.getClientScanType(), stage);
  }

  // visible for testing
  PolicyEvaluationPollingResult evaluate(final String integrationPath,
                                         final File scanFile,
                                         final ClientScanType clientScanType,
                                         final Stage stage) throws IOException
  {
    return evaluate(integrationPath, scanFile, clientScanType, stage, null, null);
  }

  // visible for testing
  PolicyEvaluationPollingResult evaluate(final String integrationPath,
                                         final File scanFile,
                                         final ClientScanType clientScanType,
                                         final Stage stage,
                                         final String statusId,
                                         final VulnerabilitySignatureAnalysisDTO analysisDTO) throws IOException
  {
    if (CLI_INTEGRATION_PATH.equals(integrationPath)) {
      addOrUpdateSourceControl();
    }
    long start = System.currentTimeMillis();
    Result evaluateResult;

    if (isNotBlank(statusId)) {
      final PolicyEvaluationRequestDTO requestDTO = new PolicyEvaluationRequestDTO();
      requestDTO.setAnalysisDTO(analysisDTO);
      evaluateResult = getPolicyEvaluationResult(integrationPath, clientScanType, stage, statusId, requestDTO);
    }
    else {
      final FileEntity entity = new FileEntity(scanFile, GZIP_CONTENT_TYPE);
      evaluateResult = evaluateResult(integrationPath, clientScanType, stage, entity);
    }

    PolicyEvaluationReceipt receipt = parseResult(evaluateResult, PolicyEvaluationReceipt.class);

    // allow handling of receipt before polling
    beforePolling(receipt, integrationPath);

    log.debug("Assigned status ID {} for evaluation", receipt.getStatusId());
    log.info("Waiting for policy evaluation to complete...");
    PolicyEvaluationPollingResult result = pollEvaluationResult(receipt.getStatusId());
    log.info("Policy evaluation completed in {} seconds.", (System.currentTimeMillis() - start) / 1000);
    return result;
  }

  /**
   * Retrieve the {@link Result} for post to {@link #getEvaluationRequestPathBuilder(String, ClientScanType, Stage)}.
   *
   * @param integrationPath - CI, CLI or RM path
   * @param clientScanType  - {@link ClientScanType}
   * @param stage           - {@link Stage}
   * @param entity          - {@link HttpEntity}
   * @return Result to post of {@link #getEvaluationRequestPathBuilder(String, ClientScanType, Stage)}
   * @since 1.101
   */
  protected Result evaluateResult(final String integrationPath,
                                  final ClientScanType clientScanType,
                                  final Stage stage,
                                  final HttpEntity entity) throws IOException
  {
    return getEvaluationRequestPathBuilder(integrationPath, clientScanType, stage).post(entity);
  }

  /**
   * Retrieve the {@link Result} for post to
   * {@link #getPolicyEvaluationRequestPathBuilder(String, ClientScanType, Stage, String)}.
   *
   * @param integrationPath            - CI, CLI path
   * @param clientScanType             - {@link ClientScanType}
   * @param stage                      - {@link Stage}
   * @param statusId                   - {@link String} of the previously-run component analysis step
   * @param policyEvaluationRequestDTO - {@link PolicyEvaluationRequestDTO}
   * @return Result to post of {@link #getPolicyEvaluationRequestPathBuilder(String, ClientScanType, Stage, String)}
   * @since 1.188
   */
  protected Result getPolicyEvaluationResult(
      final String integrationPath,
      final ClientScanType clientScanType,
      final Stage stage,
      final String statusId,
      final PolicyEvaluationRequestDTO policyEvaluationRequestDTO) throws IOException
  {
    final ByteArrayEntity entity = new ByteArrayEntity(
        JsonUtils.generate(policyEvaluationRequestDTO), ContentType.APPLICATION_JSON
    );

    return getPolicyEvaluationRequestPathBuilder(integrationPath, clientScanType, stage, statusId)
        .post(entity);
  }

  /**
   * Retrieve the {@link Result} for a POST request to
   * {@link #getComponentAnalysisRequestPathBuilder(String, ClientScanType, Stage)}.
   *
   * @param integrationPath - CI, CLI
   * @param clientScanType  - {@link ClientScanType}
   * @param stage           - {@link Stage}
   * @param entity          - {@link HttpEntity}
   * @return Result of the POST request to
   * {@link #getComponentAnalysisRequestPathBuilder(String, ClientScanType, Stage)}
   * @since 1.187
   */
  protected Result getComponentAnalysisResult(final String integrationPath,
                                  final ClientScanType clientScanType,
                                  final Stage stage,
                                  final HttpEntity entity) throws IOException
  {
    return getComponentAnalysisRequestPathBuilder(integrationPath, clientScanType, stage).post(entity);
  }

  /**
   * Construct a {@link RequestBuilder} for the evaluation request.
   *
   * @param integrationPath - CI, CLI or RM path
   * @param clientScanType  - {@link ClientScanType}
   * @param stage           - {@link Stage}
   * @return RequestBuilder to allow continuing of build the request.
   * @since 1.101
   */
  protected RequestBuilder getEvaluationRequestPathBuilder(final String integrationPath,
                                                           final ClientScanType clientScanType,
                                                           final Stage stage)
  {
    return path(BASE_PATH, appId, EVALUATION_SUB_PATH,
        integrationPath, STAGES_SUB_PATH, stage.getStageTypeId())
        .query(SCAN_TYPE, clientScanType.name());
  }

  /**
   * Construct a {@link RequestBuilder} for the policy evaluation request.
   *
   * @param integrationPath - CI, CLI
   * @param clientScanType  - {@link ClientScanType}
   * @param stage           - {@link Stage}
   * @param statusId        - {@link String} of the previously-run component analysis step
   * @return RequestBuilder for use with further building of the request
   * @since 1.188
   */
  protected RequestBuilder getPolicyEvaluationRequestPathBuilder(
      final String integrationPath,
      final ClientScanType clientScanType,
      final Stage stage,
      final String statusId)
  {
    return path(
        BASE_PATH, appId, EVALUATION_SUB_PATH, integrationPath,
        STAGES_SUB_PATH, stage.getStageTypeId(), POLICY_EVALUATION_SUB_PATH
    ).query(SCAN_TYPE, clientScanType.name(), STATUS_ID, statusId);
  }

  /**
   * Construct a {@link RequestBuilder} for the component analysis request.
   *
   * @param integrationPath - CI, CLI
   * @param clientScanType  - {@link ClientScanType}
   * @param stage           - {@link Stage}
   * @return RequestBuilder for use with further building of the request
   * @since 1.101
   */
  protected RequestBuilder getComponentAnalysisRequestPathBuilder(
      final String integrationPath,
      final ClientScanType clientScanType,
      final Stage stage)
  {
    return path(BASE_PATH, appId, EVALUATION_SUB_PATH,
        integrationPath, STAGES_SUB_PATH, stage.getStageTypeId(), COMPONENT_ANALYSIS_SUB_PATH)
        .query(SCAN_TYPE, clientScanType.name());
  }

  // visible for testing
  PolicyEvaluationPollingResult runComponentAnalysis(
      final String integrationPath,
      final File scanFile,
      final ClientScanType clientScanType,
      final Stage stage) throws IOException
  {
    if (CLI_INTEGRATION_PATH.equals(integrationPath)) {
      addOrUpdateSourceControl();
    }
    final FileEntity entity = new FileEntity(scanFile, GZIP_CONTENT_TYPE);
    final long start = System.currentTimeMillis();
    final Result componentAnalysisResult = getComponentAnalysisResult(integrationPath, clientScanType, stage, entity);
    final PolicyEvaluationReceipt receipt = parseResult(componentAnalysisResult, PolicyEvaluationReceipt.class);

    // Allow handling of receipt before polling
    beforePolling(receipt, integrationPath);

    log.debug("Assigned status ID {} for component analysis", receipt.getStatusId());
    log.info("Waiting for component analysis to complete...");
    final PolicyEvaluationPollingResult result = pollComponentAnalysisResult(receipt.getStatusId());
    log.info("Component analysis completed in {} seconds.", (System.currentTimeMillis() - start) / 1000);
    return result;
  }

  /**
   * Allow implementers to handle any actions on the {@link PolicyEvaluationReceipt} and <code>integrationPath</code>
   * before we execute to wait on the {@link #pollEvaluationResult(String)}. Implementation in {@link PolicyClient} is
   * doing no operations itself.
   *
   * @param receipt         - {@link PolicyEvaluationReceipt}
   * @param integrationPath - {@link String}
   * @throws IOException on io issues handling the {@link PolicyEvaluationReceipt} before polling.
   * @since 1.101
   */
  protected void beforePolling(final PolicyEvaluationReceipt receipt, final String integrationPath) throws IOException {
    // no-op
  }

  private void addOrUpdateSourceControl() {
    try {
      Optional<String> optional = new RepositoryUrlFinderBuilder()
          .withEnvironmentVariableDefault()
          .withEnvironmentVariableNamed(GitLabCI.REPOSITORY_URL_ENV_VARIABLE)
          .withGitRepo()
          .build()
          .tryGetRepositoryUrl();
      if (optional.isPresent()) {
        String repositoryUrl = optional.get();
        log.debug(
            "Amending source control record for application with id: {} with discovered repository URL", appId);
        sourceControlClient.addOrUpdateSourceControlRecord(appId, repositoryUrl);
      }
      else {
        log.debug("Repository URL for application with id: {} could not be found.", appId);
      }
    }
    catch (Exception e) {
      log.debug("Failed to add or update the source control record due to:", e);
    }
  }

  private PolicyEvaluationPollingResult pollEvaluationResult(final String statusId) throws IOException {
    PolicyEvaluationPollingResult pollingStatus;
    ScanReceipt scanReceipt = null;
    do {
      log.debug("Checking evaluation status at {}", new Date());
      pollingStatus = getPollingStatus(statusId);
      if (isScanReceiptReady(scanReceipt, pollingStatus)) {
        scanReceipt = pollingStatus.getScanReceipt();
        log.info("Assigned scan ID {} for evaluation", scanReceipt.getScanId());
      }
      if (pollingStatus.getStatus().equals(PolicyEvaluationStatus.PENDING)) {
        try {
          sleep(pollingStatus.getNextPollingIntervalInSeconds());
        }
        catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          throw new IOException("Policy evaluation interrupted.", e);
        }
      }
    }
    while (PolicyEvaluationStatus.PENDING.equals(pollingStatus.getStatus()));

    if (pollingStatus.getStatus().equals(PolicyEvaluationStatus.FAILED)) {
      throw new IOException("Policy evaluation could not be completed: " + pollingStatus.getReason());
    }
    return pollingStatus;
  }

  public String linkToManagement() {
    return UrlUtils.appendUrlPaths(serverUrl, "ui/links/application", appId, "management");
  }

  /**
   * Get the latest (most recent) policy evaluation summary for the given application and stage
   *
   * @param stage must be one of the valid stages, see {@link Stage#Stage(String)}
   * @return the latest policy evaluation summary, or null if not found
   * @throws IOException if the returned json is invalid and cannot be parsed
   * @since 1.11.0
   */
  public PolicyEvaluationSummary getPolicyEvaluationSummary(final Stage stage) throws IOException {
    Result result = path("rest/quality/evaluations/", appId, "/", stage.getStageTypeId()).get();
    return parseResult(result, PolicyEvaluationSummary.class);
  }

  /**
   * Sends the vulnerability signatures analysis to be processed and components with vulnerable functions/method
   * signatures are labeled as reachable.
   * 
   * @param scanId report with the components analyzed.
   * @param analysisDTO result of the analysis.
   * @return result of the re-evaluation after the analysis is imported.
   * @throws IOException if the returned json is invalid and cannot be parsed
   */
  public PolicyEvaluationResult importReachabilityAnalysis(
      String scanId,
      VulnerabilitySignatureAnalysisDTO analysisDTO) throws IOException
  {
    ByteArrayEntity entity = new ByteArrayEntity(JsonUtils.generate(analysisDTO), ContentType.APPLICATION_JSON);

    Result result =
        path("api/experimental/signatures/vulnerability/application/publicId/", appId, "/report", scanId, "/reachable")
            .post(entity);
    return parseResult(result, PolicyEvaluationResult.class);
  }

  private PolicyEvaluationPollingResult pollComponentAnalysisResult(final String statusId) throws IOException {
    PolicyEvaluationPollingResult pollingStatus;
    ScanReceipt scanReceipt = null;
    do {
      log.debug("Checking component analysis status at {}", new Date());
      pollingStatus = getPollingStatus(statusId);
      if (isScanReceiptReady(scanReceipt, pollingStatus)) {
        scanReceipt = pollingStatus.getScanReceipt();
        log.info("Assigned scan ID {} for component analysis", scanReceipt.getScanId());
      }
      if (isComponentAnalysisPendingAndNotFailed(pollingStatus)) {
        try {
          sleep(pollingStatus.getNextPollingIntervalInSeconds());
        }
        catch (final InterruptedException e) {
          Thread.currentThread().interrupt();
          throw new IOException("Component analysis interrupted.", e);
        }
      }
    }
    while (isComponentAnalysisPendingAndNotFailed(pollingStatus));

    if (pollingStatus.getStatus().equals(PolicyEvaluationStatus.FAILED)) {
      throw new IOException("Component analysis could not be completed: " + pollingStatus.getReason());
    }
    return pollingStatus;
  }

  private PolicyEvaluationPollingResult getPollingStatus(final String statusId) throws IOException {
    final Result pollingResult = path(BASE_PATH, appId, POLLING_PATH, statusId).get();

    PolicyEvaluationPollingResult result = parseResult(pollingResult, PolicyEvaluationPollingResult.class);
    result.setStatusId(statusId);
    return result;
  }

  private static boolean isScanReceiptReady(
      final ScanReceipt scanReceipt,
      final PolicyEvaluationPollingResult pollingStatus)
  {
    return scanReceipt == null && pollingStatus.getScanReceipt() != null;
  }

  private static boolean isComponentAnalysisPendingAndNotFailed(final PolicyEvaluationPollingResult pollingStatus) {
    return COMPONENT_ANALYSIS_PENDING.equals(pollingStatus.getSubStatus()) &&
        !FAILED.equals(pollingStatus.getStatus());
  }

  private static void sleep(final int pollingIntervalSeconds) throws InterruptedException {
    Thread.sleep(pollingIntervalSeconds * 1000L);
  }
}
