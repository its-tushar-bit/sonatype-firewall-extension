/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.client;

import java.io.IOException;

import com.sonatype.clm.dto.model.policy.PolicyEvaluationSummary;
import com.sonatype.insight.client.utils.HttpClientUtils.Configuration;
import com.sonatype.insight.client.utils.Result;
import com.sonatype.insight.json.store.JsonUtils;

import org.apache.http.client.HttpResponseException;

public class FirewallClient
    extends AbstractRequestClient
{
  private static final String SERVICE_PATH = "rest/integration/repositories";

  private static final String SUMMARY_PATH = "summary";

  private final String repositoryManagerInstanceId;

  private final String repositoryPublicId;


  public FirewallClient(final Configuration config, final String repositoryManagerInstanceId,
      final String repositoryPublicId)
  {
    super(config);

    this.repositoryManagerInstanceId = repositoryManagerInstanceId;
    this.repositoryPublicId = repositoryPublicId;
  }

  public void enableRepository() throws IOException {
    Result result = postRequest(path(SERVICE_PATH, repositoryManagerInstanceId, repositoryPublicId),
        null);
    int status = result.status();
    if (status >= 300) {
      String msg = result.message();
      throw new HttpResponseException(status, msg);
    }
  }

  public PolicyEvaluationSummary getPolicyEvaluationSummary() throws IOException {
    Result result = getRequest(path(SERVICE_PATH, repositoryManagerInstanceId, repositoryPublicId, SUMMARY_PATH));
    int status = result.status();
    if (status >= 300) {
      String msg = result.message();
      throw new HttpResponseException(status, msg);
    }

    final String jsonResult = result.text();
    try {
      return JsonUtils.parse(jsonResult, PolicyEvaluationSummary.class);
    }
    catch (final IOException e) {
      throw new IOException("Could not parse: " + jsonResult, e);
    }
  }
}
