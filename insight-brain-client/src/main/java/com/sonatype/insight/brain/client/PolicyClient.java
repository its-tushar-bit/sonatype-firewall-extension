/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.client;

import java.io.IOException;

import com.sonatype.clm.dto.model.policy.PolicyEvaluationResult;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationSummary;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.client.utils.HttpClientUtils.Configuration;
import com.sonatype.insight.client.utils.Result;
import com.sonatype.insight.client.utils.UrlUtils;
import com.sonatype.insight.json.store.JsonUtils;

import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;

public class PolicyClient
    extends AbstractRequestClient
{
  private final String serverUrl;

  private final String appId;

  public PolicyClient(final Configuration config, final String appId) {
    super(config);

    this.serverUrl = config.getServerUrl();
    this.appId = UrlUtils.encodeUrlComponent(appId);
  }

  public PolicyEvaluationResult evaluate(final String scanId, final Stage stage) throws IOException {
    final ByteArrayEntity entity = new ByteArrayEntity(JsonUtils.generate(stage), ContentType.APPLICATION_JSON);
    Result result = path("rest/policy", appId, "evaluate").query("scanId", scanId).post(entity);
    return parseResult(result, PolicyEvaluationResult.class);
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
