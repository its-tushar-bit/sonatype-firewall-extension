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
import com.sonatype.clm.dto.model.policy.PolicyEvaluationStatus;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationSummary;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.client.utils.HttpClientUtils.Configuration;
import com.sonatype.insight.client.utils.Result;
import com.sonatype.insight.client.utils.UrlUtils;
import com.sonatype.insight.scan.model.ClientScanResult;
import com.sonatype.insight.scan.model.ClientScanType;
import com.sonatype.nexus.git.utils.Environment.BambooCI;
import com.sonatype.nexus.git.utils.Environment.GitLabCI;
import com.sonatype.nexus.git.utils.repository.RepositoryUrlFinderBuilder;

import org.apache.http.entity.ContentType;
import org.apache.http.entity.FileEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PolicyClient
    extends AbstractRequestClient
{
  private static final ContentType GZIP_CONTENT_TYPE = ContentType.create("application/x-gzip");

  private final Logger log;

  private final String serverUrl;

  private final String appId;

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
    return evaluate("cli", clientScanResult.getScanFile(), getClientScanType(clientScanResult, clientScanType), stage);
  }

  /**
   * @since 1.69
   */
  public PolicyEvaluationPollingResult evaluateCI(final ClientScanResult clientScanResult,
                                                  final Stage stage) throws IOException
  {
    return evaluate("ci", clientScanResult.getScanFile(), getClientScanType(clientScanResult, ClientScanType.SONATYPE),
        stage);
  }

  private ClientScanType getClientScanType(ClientScanResult clientScanResult, ClientScanType clientScanType) {
    if (clientScanResult.hasThirdPartyScanContent()) {
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
    return evaluate("rm", scanFile, ClientScanType.SONATYPE, stage);
  }

  // visible for testing
  PolicyEvaluationPollingResult evaluate(final String integrationPath,
                                         final File scanFile,
                                         final ClientScanType clientScanType,
                                         final Stage stage) throws IOException
  {
    addOrUpdateSourceControl();
    final FileEntity entity = new FileEntity(scanFile, GZIP_CONTENT_TYPE);
    long start = System.currentTimeMillis();
    Result evaluateResult = path("rest/integration/applications/", appId, "/evaluations/", integrationPath, "/stages/",
        stage.getStageTypeId()).query("scanType", clientScanType.name()).post(entity);
    PolicyEvaluationReceipt receipt = parseResult(evaluateResult, PolicyEvaluationReceipt.class);
    log.debug("Assigned status ID {}", receipt.getStatusId());
    log.info("Waiting for policy evaluation to complete...");
    PolicyEvaluationPollingResult result = pollEvaluationResult(receipt.getStatusId());
    log.info("Policy evaluation completed in {} seconds.", (System.currentTimeMillis() - start) / 1000);
    return result;
  }

  private void addOrUpdateSourceControl() {
    try {
      Optional<String> optional = new RepositoryUrlFinderBuilder()
          .withEnvironmentVariableDefault()
          .withEnvironmentVariableNamed(GitLabCI.REPOSITORY_URL_ENV_VARIABLE)
          .withEnvironmentVariableNamed(BambooCI.REPOSITORY_URL_ENV_VARIABLE)
          .withGitRepo()
          .build()
          .tryGetRepositoryUrl();
      if (optional.isPresent()) {
        String repositoryUrl = optional.get();
        log.debug(
            "Amending source control record for application with id: {} with discovered Repository URL: {}",
            appId, repositoryUrl);
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
      Result pollingResult = path("rest/integration/applications/", appId, "/evaluations/status/", statusId).get();
      pollingStatus = parseResult(pollingResult, PolicyEvaluationPollingResult.class);
      if (scanReceipt == null && pollingStatus.getScanReceipt() != null) {
        scanReceipt = pollingStatus.getScanReceipt();
        log.info("Assigned scan ID {}", scanReceipt.getScanId());
      }
      if (pollingStatus.getStatus().equals(PolicyEvaluationStatus.PENDING)) {
        try {
          Thread.sleep(pollingStatus.getNextPollingIntervalInSeconds() * 1000);
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
}
