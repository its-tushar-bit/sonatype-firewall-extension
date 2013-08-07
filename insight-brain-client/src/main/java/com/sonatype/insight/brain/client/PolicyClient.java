/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.client;

import java.io.IOException;
import java.util.Map;

import com.sonatype.clm.dto.model.policy.PolicyEvaluationResult;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.client.utils.AbstractServletClient;
import com.sonatype.insight.client.utils.ClientException;
import com.sonatype.insight.client.utils.HttpClientUtils.Configuration;
import com.sonatype.insight.client.utils.Result;
import com.sonatype.insight.client.utils.ServletResult;
import com.sonatype.insight.client.utils.UrlUtils;
import com.sonatype.insight.json.store.JsonUtils;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.message.BasicHttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PolicyClient
    extends AbstractServletClient<PolicyClient>
{
  private static final Logger log = LoggerFactory.getLogger(PolicyClient.class);

  private final String appId;

  public PolicyClient(final Configuration config, final String appId) {
    super(config);

    this.appId = UrlUtils.encodeUrlComponent(appId);
  }

  @Override
  public ServletResult handle(final String path, final Map<String, String[]> query) throws IOException {
    if (path == null || path.length() == 0) {
      // implicit redirect from initial top-level request to the actual management asset
      final HttpResponse redirect = new BasicHttpResponse(HttpVersion.HTTP_1_1, 302, null);
      redirect.setHeader(HttpHeaders.LOCATION, "assets/index.html#/management/application/" + appId + "/policies");
      return result(redirect);
    }

    // workaround for DropWizard directory->index redirect bug
    if (path.contains("assets/") && path.endsWith("/")) {
      return path(path, "index.html").get();
    }

    return super.handle(path, query);
  }

  public PolicyEvaluationResult evaluate(final String scanId, final Stage stage) throws IOException {
    final ByteArrayEntity entity = new ByteArrayEntity(JsonUtils.generate(stage), ContentType.APPLICATION_JSON);

    final Result httpResult = path("rest/policy", appId, "evaluate").query("scanId", scanId).post(entity);
    if (httpResult.status() >= 400) {
      throw new ClientException(httpResult);
    }

    final String jsonResult = httpResult.text();
    try {
      return JsonUtils.parse(jsonResult, PolicyEvaluationResult.class);
    }
    catch (final IOException e) {
      log.error("Cannot parse json:" + jsonResult);
      throw new ClientException(httpResult, e);
    }
  }
}
