/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.client;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;

import com.sonatype.clm.dto.model.ScanReceipt;
import com.sonatype.clm.dto.model.component.ProprietaryComponentNames;
import com.sonatype.clm.dto.model.component.RepositoryComponentEvaluationDataList;
import com.sonatype.clm.dto.model.component.RepositoryComponentEvaluationDataRequestList;
import com.sonatype.clm.dto.model.component.RepositoryComponentPathnames;
import com.sonatype.clm.dto.model.component.UnquarantinedComponentList;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationPollingResult;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationStatus;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationSummary;
import com.sonatype.clm.dto.model.policy.RepositoryPolicyEvaluationSummary;
import com.sonatype.clm.dto.model.repository.QuarantinedComponentReport;
import com.sonatype.clm.dto.model.repository.container.image.FirewallContainerImageEvaluationResponse;
import com.sonatype.clm.dto.model.repository.container.image.FirewallContainerImageEvaluationWithPollingResponse;
import com.sonatype.insight.client.utils.HttpClientUtils.Configuration;
import com.sonatype.insight.client.utils.Result;
import com.sonatype.insight.json.store.JsonUtils;

import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FirewallClient
    extends AbstractRequestClient
{
  private static final Logger log = LoggerFactory.getLogger(FirewallClient.class);

  public static final String NEXUS_RESOURCE_PATH = "rest/integration/repositories";

  public static final String ARTIFACTORY_RESOURCE_PATH =  "rest/integration/artifactory/repositories";

  private static final String EVALUATE_PATH = "evaluate/audit";

  private static final String EVALUATE_COMPONENTS_ADHOC_PATH = "evaluate/adhoc";

  private static final String SUMMARY_PATH = "summary";

  private static final String REPOSITORY_RESULTS_URL = "repositoryResultsUrl";

  private static final String ENABLE_PATH = "enable";

  private static final String QUARANTINE_PATH = "quarantine";

  private static final String COMPONENTS_PATH = "components";

  private static final String UNQUARANTINED_COMPONENTS_PATH = "components/unquarantined";

  private static final String EVALUATE_COMPONENT_WITH_QUARANTINE_PATH = "evaluate/quarantine";

  private static final String EVALUATE_COMPONENT_METADATA_PATH = "evaluate/componentMetadata";

  private static final String PROPRIETARY_NAMES_PATH = "proprietary/names";

  static final String QUARANTINED_COMPONENT_REPORT_URL_PATH =  "quarantinedComponentReportUrl";

  private static final String REMOVE_EXTRA_COMPONENTS_PATH = "removeExtraComponents";

  private static final String EVALUATE_CONTAINER_IMAGE_PATH = "evaluate/containerImage";

  private final String repositoryManagerInstanceId;

  private final String repositoryPublicId;

  private final String resourcePath;

  public FirewallClient(final Configuration config,
                        final String repositoryManagerInstanceId,
                        final String repositoryPublicId,
                        final String resourcePath)
  {
    super(config);

    this.repositoryManagerInstanceId = repositoryManagerInstanceId;
    this.repositoryPublicId = repositoryPublicId;
    this.resourcePath = resourcePath;
  }

  public void setEnabled(boolean enabled) throws IOException {
    Result result =
        path(resourcePath, repositoryManagerInstanceId, repositoryPublicId, ENABLE_PATH,
            Boolean.toString(enabled)).post(null);
    verifyStatusCode(result);
  }

  public void setQuarantine(final boolean enabled) throws IOException {
    Result result =
        path(resourcePath, repositoryManagerInstanceId, repositoryPublicId, QUARANTINE_PATH,
            Boolean.toString(enabled)).post(null);
    verifyStatusCode(result);
  }

  public void removeComponent(String pathname) throws IOException {
    Result result =
        path(resourcePath, repositoryManagerInstanceId, repositoryPublicId, COMPONENTS_PATH, pathname).delete();
    verifyStatusCode(result);
  }

  public void evaluateComponents(final RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList)
      throws IOException
  {
    final ByteArrayEntity entity = new ByteArrayEntity(JsonUtils.generate(componentEvaluationDataRequestList),
        ContentType.APPLICATION_JSON);

    final Result result =
        path(resourcePath, repositoryManagerInstanceId, repositoryPublicId, EVALUATE_PATH).post(entity);
    verifyStatusCode(result);
  }

  public RepositoryComponentEvaluationDataList evaluateComponentsAdhoc(
      RepositoryComponentEvaluationDataRequestList repositoryComponentEvaluationDataRequestList)
      throws IOException
  {
    ByteArrayEntity entity = new ByteArrayEntity(
        JsonUtils.generate(repositoryComponentEvaluationDataRequestList),
        ContentType.APPLICATION_JSON);

    Result result = path(
        resourcePath, repositoryManagerInstanceId, repositoryPublicId, EVALUATE_COMPONENTS_ADHOC_PATH)
        .post(entity);
    verifyStatusCode(result);

    return parseResult(result, RepositoryComponentEvaluationDataList.class);
  }

  public RepositoryComponentEvaluationDataList evaluateComponentWithQuarantine(
      RepositoryComponentEvaluationDataRequestList repositoryComponentEvaluationDataRequestList) throws IOException
  {
    ByteArrayEntity entity = new ByteArrayEntity(JsonUtils.generate(repositoryComponentEvaluationDataRequestList),
        ContentType.APPLICATION_JSON);

    Result result =
        path(resourcePath, repositoryManagerInstanceId, repositoryPublicId, EVALUATE_COMPONENT_WITH_QUARANTINE_PATH)
            .post(entity);
    return parseResult(result, RepositoryComponentEvaluationDataList.class);
  }

  /**
   * Evaluates policies on versions of the same component.
   * The specified componentEvaluationDataRequestList must contain only versions of the same component
   * Only the npm format is supported.
   * 
   * @since 1.133
   */
  public RepositoryComponentEvaluationDataList evaluateComponentMetadata(
      RepositoryComponentEvaluationDataRequestList repositoryComponentEvaluationDataRequestList) throws IOException
  {
    ByteArrayEntity entity = new ByteArrayEntity(JsonUtils.generate(repositoryComponentEvaluationDataRequestList),
        ContentType.APPLICATION_JSON);

    Result result =
        path(resourcePath, repositoryManagerInstanceId, repositoryPublicId, EVALUATE_COMPONENT_METADATA_PATH)
            .post(entity);
    return parseResult(result, RepositoryComponentEvaluationDataList.class);
  }

  /**
   * Removes all components from the given repository that have paths not in the given pathname list and with timestamp
   * before or equal to the given timestamp.
   * 
   * @param repositoryComponentPathnames the pathname list and timestamp used to filter the components to be deleted.
   * 
   * @since 1.142
   */
  public void removeExtraComponents(RepositoryComponentPathnames repositoryComponentPathnames) throws IOException {
    ByteArrayEntity entity =
        new ByteArrayEntity(JsonUtils.generate(repositoryComponentPathnames), ContentType.APPLICATION_JSON);

    Result result =
        path(resourcePath, repositoryManagerInstanceId, repositoryPublicId, REMOVE_EXTRA_COMPONENTS_PATH).post(entity);
    verifyStatusCode(result);
  }

  public RepositoryPolicyEvaluationSummary getPolicyEvaluationSummary() throws IOException {
    Result result = path(resourcePath, repositoryManagerInstanceId, repositoryPublicId, SUMMARY_PATH).get();
    return parseResult(result, RepositoryPolicyEvaluationSummary.class);
  }

  public String getRepositoryResultsUrl() throws IOException {
    Result result = path(resourcePath, repositoryManagerInstanceId, repositoryPublicId, REPOSITORY_RESULTS_URL).get();
    verifyStatusCode(result);
    return result.text();
  }

  public UnquarantinedComponentList getUnquarantinedComponents(final long sinceUtcTimestamp) throws IOException {
    Result result = path(resourcePath, repositoryManagerInstanceId, repositoryPublicId, UNQUARANTINED_COMPONENTS_PATH)
        .query("sinceUtcTimestamp", Long.toString(sinceUtcTimestamp)).get();
    return parseResult(result, UnquarantinedComponentList.class);
  }

  public void addProprietaryComponentNames(ProprietaryComponentNames proprietaryComponentNames) throws IOException {
    ByteArrayEntity entity =
        new ByteArrayEntity(JsonUtils.generate(proprietaryComponentNames), ContentType.APPLICATION_JSON);

    Result result =
        path(resourcePath, repositoryManagerInstanceId, repositoryPublicId, PROPRIETARY_NAMES_PATH).post(entity);
    verifyStatusCode(result);
  }

  public QuarantinedComponentReport getQuarantinedComponentReport(String pathname) throws IOException {
    Result result = path(resourcePath, repositoryManagerInstanceId, repositoryPublicId, COMPONENTS_PATH, pathname,
        QUARANTINED_COMPONENT_REPORT_URL_PATH).get();
    return parseResult(result, QuarantinedComponentReport.class);
  }

  public boolean isContainerImageQuarantined(String containerImagePublicId) throws IOException {
    Result result = path(
        resourcePath,
        repositoryManagerInstanceId,
        repositoryPublicId,
        "containerImage",
        containerImagePublicId,
        "isQuarantined")
            .get();
    return parseResult(result, Boolean.class);
  }

  public FirewallContainerImageEvaluationWithPollingResponse evaluateContainerImageWithPolling(String bomJson)
      throws IOException
  {
    long start = System.currentTimeMillis();

    ByteArrayEntity entity =
        new ByteArrayEntity(bomJson.getBytes(StandardCharsets.UTF_8), ContentType.APPLICATION_JSON);
    Result result = path(resourcePath, repositoryManagerInstanceId, repositoryPublicId, EVALUATE_CONTAINER_IMAGE_PATH)
        .post(entity);

    FirewallContainerImageEvaluationResponse evaluationResponse =
        parseResult(result, FirewallContainerImageEvaluationResponse.class);
    String containerImagePublicId = evaluationResponse.getContainerImagePublicId();
    String statusId = evaluationResponse.getStatusId();
    String statusUrl = evaluationResponse.getStatusUrl();

    log.debug("Assigned evaluation status ID {} for container image {}", statusId, containerImagePublicId);
    log.info("Waiting for container image evaluation to complete...");

    PolicyEvaluationPollingResult policyEvaluationPollingResult = pollContainerImageEvaluationResult(statusUrl);

    log.info("Container image evaluation completed in {} seconds.", (System.currentTimeMillis() - start) / 1000);

    FirewallContainerImageEvaluationWithPollingResponse response =
        new FirewallContainerImageEvaluationWithPollingResponse();
    response.setContainerImageId(evaluationResponse.getContainerImageId());
    response.setContainerImagePublicId(evaluationResponse.getContainerImagePublicId());
    response.setPollingResult(policyEvaluationPollingResult);

    return response;
  }

  public PolicyEvaluationSummary getContainerImageReportUrl(String containerImageIdOrPublicId) throws IOException {
    Result result = path(resourcePath, repositoryManagerInstanceId, repositoryPublicId, "containerImage",
        containerImageIdOrPublicId, "report").get();
    return parseResult(result, PolicyEvaluationSummary.class);
  }

  private PolicyEvaluationPollingResult pollContainerImageEvaluationResult(String statusUrl) throws IOException {
    PolicyEvaluationPollingResult pollingStatus;
    ScanReceipt scanReceipt = null;

    do {
      log.debug("Checking container image evaluation status at {}", new Date());
      pollingStatus = parseResult(path(statusUrl).get(), PolicyEvaluationPollingResult.class);

      if (scanReceipt == null && pollingStatus.getScanReceipt() != null) {
        scanReceipt = pollingStatus.getScanReceipt();
        log.info("Assigned scan ID {} for container image evaluation", scanReceipt.getScanId());
      }

      if (pollingStatus.getStatus().equals(PolicyEvaluationStatus.PENDING)) {
        sleepForPolling(pollingStatus);
      }
    }
    while (PolicyEvaluationStatus.PENDING.equals(pollingStatus.getStatus()));

    if (pollingStatus.getStatus().equals(PolicyEvaluationStatus.FAILED)) {
      throw new IOException("Container image evaluation could not be completed: " + pollingStatus.getReason());
    }

    return pollingStatus;
  }

  private static void sleepForPolling(PolicyEvaluationPollingResult pollingStatus) throws IOException {
    try {
      Thread.sleep(Duration.ofSeconds(pollingStatus.getNextPollingIntervalInSeconds()).toMillis());
    }
    catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IOException("Container image evaluation interrupted.", e);
    }
  }
}
