/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.client;

import java.io.IOException;

import com.sonatype.clm.dto.model.component.RepositoryComponentEvaluationDataList;
import com.sonatype.clm.dto.model.component.RepositoryComponentEvaluationDataRequestList;
import com.sonatype.clm.dto.model.component.UnquarantinedComponentList;
import com.sonatype.clm.dto.model.policy.RepositoryPolicyEvaluationSummary;
import com.sonatype.insight.client.utils.HttpClientUtils.Configuration;
import com.sonatype.insight.client.utils.Result;
import com.sonatype.insight.json.store.JsonUtils;

import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;

public class FirewallClient
    extends AbstractRequestClient
{
  public static final String NEXUS_RESOURCE_PATH = "rest/integration/repositories";

  public static final String ARTIFACTORY_RESOURCE_PATH =  "rest/integration/artifactory/repositories";

  private static final String EVALUATE_PATH = "evaluate/audit";

  private static final String SUMMARY_PATH = "summary";

  private static final String ENABLE_PATH = "enable";

  private static final String QUARANTINE_PATH = "quarantine";

  private static final String COMPONENTS_PATH = "components";

  private static final String UNQUARANTINED_COMPONENTS_PATH = "components/unquarantined";

  private static final String EVALUATE_COMPONENT_WITH_QUARANTINE_PATH = "evaluate/quarantine";

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
    Result result = postRequest(
        path(resourcePath, repositoryManagerInstanceId, repositoryPublicId, ENABLE_PATH,
            Boolean.toString(enabled)), null);
    verifyStatusCode(result);
  }

  public void setQuarantine(final boolean enabled) throws IOException {
    Result result = postRequest(
        path(resourcePath, repositoryManagerInstanceId, repositoryPublicId, QUARANTINE_PATH,
            Boolean.toString(enabled)), null);
    verifyStatusCode(result);
  }

  public void removeComponent(String pathname) throws IOException {
    Result result = deleteRequest(
        path(resourcePath, repositoryManagerInstanceId, repositoryPublicId, COMPONENTS_PATH, pathname));
    verifyStatusCode(result);
  }

  public void evaluateComponents(final RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList)
      throws IOException
  {
    final ByteArrayEntity entity = new ByteArrayEntity(JsonUtils.generate(componentEvaluationDataRequestList),
        ContentType.APPLICATION_JSON);

    final Result result = postRequest(
        path(resourcePath, repositoryManagerInstanceId, repositoryPublicId, EVALUATE_PATH), entity);
    verifyStatusCode(result);
  }

  public RepositoryComponentEvaluationDataList evaluateComponentWithQuarantine(
      RepositoryComponentEvaluationDataRequestList repositoryComponentEvaluationDataRequestList) throws IOException
  {
    ByteArrayEntity entity = new ByteArrayEntity(JsonUtils.generate(repositoryComponentEvaluationDataRequestList),
        ContentType.APPLICATION_JSON);

    Result result = postRequest(
        path(resourcePath, repositoryManagerInstanceId, repositoryPublicId, EVALUATE_COMPONENT_WITH_QUARANTINE_PATH),
        entity);
    return parseResult(result, RepositoryComponentEvaluationDataList.class);
  }

  public RepositoryPolicyEvaluationSummary getPolicyEvaluationSummary() throws IOException {
    Result result = getRequest(path(resourcePath, repositoryManagerInstanceId, repositoryPublicId, SUMMARY_PATH));
    return parseResult(result, RepositoryPolicyEvaluationSummary.class);
  }

  public UnquarantinedComponentList getUnquarantinedComponents(final long sinceUtcTimestamp) throws IOException {
    Result result = getRequest(path(resourcePath, repositoryManagerInstanceId, repositoryPublicId,
            UNQUARANTINED_COMPONENTS_PATH)
            .query("sinceUtcTimestamp", Long.toString(sinceUtcTimestamp)));
    return parseResult(result, UnquarantinedComponentList.class);
  }
}
