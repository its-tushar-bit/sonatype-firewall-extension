/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.client;

import java.io.IOException;

import com.sonatype.clm.dto.model.component.RepositoryComponentEvaluationDataList;
import com.sonatype.clm.dto.model.component.RepositoryComponentEvaluationDataRequestList;
import com.sonatype.clm.dto.model.policy.RepositoryPolicyEvaluationSummary;
import com.sonatype.insight.client.utils.HttpClientUtils.Configuration;
import com.sonatype.insight.client.utils.Result;
import com.sonatype.insight.json.store.JsonUtils;

import org.apache.http.client.HttpResponseException;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;

public class FirewallClient
    extends AbstractRequestClient
{
  private static final String RESOURCE_PATH = "rest/integration/repositories";

  private static final String EVALUATE_PATH = "evaluate/audit";

  private static final String SUMMARY_PATH = "summary";

  private static final String ENABLE_PATH = "enable";

  private static final String QUARANTINE_PATH = "quarantine";

  private static final String COMPONENTS_PATH = "components";

  private static final String EVALUATE_COMPONENT_WITH_QUARANTINE_PATH = "evaluate/quarantine";

  private final String repositoryManagerInstanceId;

  private final String repositoryPublicId;


  public FirewallClient(final Configuration config, final String repositoryManagerInstanceId,
      final String repositoryPublicId)
  {
    super(config);

    this.repositoryManagerInstanceId = repositoryManagerInstanceId;
    this.repositoryPublicId = repositoryPublicId;
  }

  public void setEnabled(boolean enabled) throws IOException {
    Result result = postRequest(path(RESOURCE_PATH, repositoryManagerInstanceId, repositoryPublicId, ENABLE_PATH,
        Boolean.toString(enabled)), null);
    int status = result.status();
    if (status >= 300) {
      String msg = result.message();
      throw new HttpResponseException(status, msg);
    }
  }

  public void setQuarantine(final boolean enabled) throws IOException {
    Result result = postRequest(path(RESOURCE_PATH, repositoryManagerInstanceId, repositoryPublicId, QUARANTINE_PATH,
        Boolean.toString(enabled)), null);
    int status = result.status();
    if (status >= 300) {
      String msg = result.message();
      throw new HttpResponseException(status, msg);
    }
  }

  public void removeComponent(String pathname) throws IOException {
    Result result = deleteRequest(path(RESOURCE_PATH, repositoryManagerInstanceId, repositoryPublicId,
        COMPONENTS_PATH, pathname));
    int status = result.status();
    if (status >= 300) {
      String msg = result.message();
      throw new HttpResponseException(status, msg);
    }
  }

  public void evaluateComponents(final RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList)
      throws IOException
  {
    final ByteArrayEntity entity = new ByteArrayEntity(JsonUtils.generate(componentEvaluationDataRequestList),
        ContentType.APPLICATION_JSON);

    final Result result = postRequest(
        path(RESOURCE_PATH, repositoryManagerInstanceId, repositoryPublicId, EVALUATE_PATH),
        entity);
    final int status = result.status();
    if (status >= 300) {
      String msg = result.message();
      throw new HttpResponseException(status, msg);
    }
  }

  public RepositoryComponentEvaluationDataList evaluateComponentWithQuarantine(
      final RepositoryComponentEvaluationDataRequestList repositoryComponentEvaluationDataRequestList)
      throws IOException
  {
    ByteArrayEntity entity = new ByteArrayEntity(JsonUtils.generate(repositoryComponentEvaluationDataRequestList),
        ContentType.APPLICATION_JSON);

    Result result = postRequest(
        path(RESOURCE_PATH, repositoryManagerInstanceId, repositoryPublicId, EVALUATE_COMPONENT_WITH_QUARANTINE_PATH),
        entity);
    int status = result.status();
    if (status >= 300) {
      String msg = result.message();
      throw new HttpResponseException(status, msg);
    }

    final String jsonResult = result.text();
    try {
      return JsonUtils.parse(jsonResult, RepositoryComponentEvaluationDataList.class);
    }
    catch (final IOException e) {
      throw new IOException("Could not parse: " + jsonResult, e);
    }
  }

  public RepositoryPolicyEvaluationSummary getPolicyEvaluationSummary() throws IOException {
    Result result = getRequest(path(RESOURCE_PATH, repositoryManagerInstanceId, repositoryPublicId, SUMMARY_PATH));
    int status = result.status();
    if (status >= 300) {
      String msg = result.message();
      throw new HttpResponseException(status, msg);
    }

    final String jsonResult = result.text();
    try {
      return JsonUtils.parse(jsonResult, RepositoryPolicyEvaluationSummary.class);
    }
    catch (final IOException e) {
      throw new IOException("Could not parse: " + jsonResult, e);
    }
  }
}
