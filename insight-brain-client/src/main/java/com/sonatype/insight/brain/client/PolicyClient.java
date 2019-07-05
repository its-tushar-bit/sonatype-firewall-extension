/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.client;

import java.io.File;
import java.io.IOException;
import java.util.Date;

import com.sonatype.clm.dto.model.ScanReceipt;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationPollingResult;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationReceipt;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationStatus;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationSummary;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.client.utils.HttpClientUtils.Configuration;
import com.sonatype.insight.client.utils.Result;
import com.sonatype.insight.client.utils.UrlUtils;
import com.sonatype.insight.scan.model.ClientScanType;

import org.apache.http.entity.ContentType;
import org.apache.http.entity.FileEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PolicyClient
    extends AbstractRequestClient
{
  private static final Logger log = LoggerFactory.getLogger(PolicyClient.class);

  private static final ContentType GZIP_CONTENT_TYPE = ContentType.create("application/x-gzip");

  private final String serverUrl;

  private final String appId;

  public PolicyClient(final Configuration config, final String appId) {
    super(config);

    this.serverUrl = config.getServerUrl();
    this.appId = UrlUtils.encodeUrlComponent(appId);
  }

  /**
   * @since 1.69
   */
  public PolicyEvaluationPollingResult evaluateCLI(final File scanFile,
                                                   final ClientScanType clientScanType,
                                                   final Stage stage) throws IOException
  {
    return evaluate("cli", scanFile, clientScanType, stage);
  }

  /**
   * @since 1.69
   */
  public PolicyEvaluationPollingResult evaluateCI(final File scanFile,
                                                  final Stage stage) throws IOException
  {
    return evaluate("ci", scanFile, ClientScanType.SONATYPE, stage);
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
    final FileEntity entity = new FileEntity(scanFile, GZIP_CONTENT_TYPE);
    long start = System.currentTimeMillis();
    Result evaluateResult = path("rest/integration/applications/", appId, "/evaluations/", integrationPath, "/stages/",
        stage.getStageTypeId()).query("scanType", clientScanType.name()).post(entity);
    PolicyEvaluationReceipt receipt = parseResult(evaluateResult, PolicyEvaluationReceipt.class);
    log.debug("Assigned status ID {}", receipt.getStatusId());
    PolicyEvaluationPollingResult result = pollEvaluationResult(receipt.getStatusId());
    log.debug("Policy evaluation completed in {} seconds.", (System.currentTimeMillis() - start) / 1000);
    return result;
  }

  private PolicyEvaluationPollingResult pollEvaluationResult(final String statusId) throws IOException {
    log.info("Waiting for policy evaluation to complete...");
    PolicyEvaluationPollingResult pollingStatus;
    ScanReceipt scanReceipt = null;
    do {
      log.debug("Checking evaluation status at {}", new Date());
      Result pollingResult = path("rest/integration/applications/", appId, "/evaluations/status/", statusId).get();
      pollingStatus = parseResult(pollingResult, PolicyEvaluationPollingResult.class);
      if (scanReceipt == null && pollingStatus.getScanReceipt() != null) {
        scanReceipt = pollingStatus.getScanReceipt();
        log.info("Assigned scan ID " + scanReceipt.getScanId());
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
