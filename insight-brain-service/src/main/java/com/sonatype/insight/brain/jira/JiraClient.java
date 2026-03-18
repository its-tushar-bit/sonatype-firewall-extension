/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.jira;

import java.io.IOException;

import com.sonatype.insight.brain.jira.JiraIssueCreateRequest.JiraIssueCreateResponse;
import com.sonatype.insight.client.utils.AbstractClient;
import com.sonatype.insight.client.utils.HttpClientUtils.Configuration;
import com.sonatype.insight.client.utils.Result;
import com.sonatype.insight.error.exception.BadGatewayException;
import com.sonatype.insight.json.store.JsonUtils;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Minimal JIRA REST client needed to support notification integration.
 *
 * @since 1.21.0
 */
public class JiraClient
    extends AbstractClient
{
  private static final Logger log = LoggerFactory.getLogger(JiraClient.class);

  public JiraClient(final Configuration configuration) {
    super(configuration);
  }

  /**
   * Get details about creating issues for all projects.
   *
   * https://docs.atlassian.com/jira/REST/latest/#api/2/issue-getCreateIssueMeta
   *
   * It is important to expand issuetype.fields so we can inspect required fields.
   */
  public JiraIssueCreateMeta getIssueCreateMeta() throws IOException {
    Result result = path("/rest/api/2/issue/createmeta").query("expand", "projects.issuetypes.fields").get();
    if (result.status() >= 300) {
      handleError(result, "fetch issue metadata.");
    }

    return JsonUtils.parse(result.text(), JiraIssueCreateMeta.class);
  }

  /**
   * Get details about deployment type: cloud vs server
   * https://docs.atlassian.com/jira/REST/latest/#api/2/serverInfo
   */
  public boolean isCloudDeployment() throws IOException {
    Result result = path("/rest/api/2/serverInfo").get();
    if (result.status() >= 300) {
      handleError(result, "fetch server info.");
    }
    ServerInfoResponse serverInfoResponse = JsonUtils.parse(result.text(), ServerInfoResponse.class);
    return serverInfoResponse != null && serverInfoResponse.isCloudDeployment();
  }

  /**
   * Create a new issue
   * for Jira Cloud: https://developer.atlassian.com/cloud/jira/platform/rest/v3/#api-rest-api-3-issue-post
   * , or
   * for Jira Server: https://docs.atlassian.com/jira/REST/latest/#api/2/issue-createIssue
   */
  public JiraIssueCreateResponse createIssue(
      final JiraIssueCreateRequest request,
      final boolean isForCloud) throws IOException
  {
    String jsonRequest = JsonUtils.format(request);
    HttpEntity entity = new StringEntity(jsonRequest, ContentType.APPLICATION_JSON);
    String path = isForCloud ? "/rest/api/3/issue" : "/rest/api/2/issue";
    Result result = path(path).post(entity);
    if (result.status() >= 300) {
      return handleError(result, "create issue.");
    }
    return JsonUtils.parse(result.text(), JiraIssueCreateResponse.class);
  }

  private JiraIssueCreateResponse handleError(final Result result, final String context) throws IOException {
    String contentType = result.header(HttpHeaders.CONTENT_TYPE);

    if (result.status() == HttpStatus.SC_BAD_REQUEST && contentType != null
        && contentType.contains(ContentType.APPLICATION_JSON.getMimeType()))
    {
      log.error("Unexpected response from Jira when trying to {} Status Code: {} Message: {}", context, result.status(),
          result.text());
    }
    else {
      log.error("Unexpected response from Jira when trying to {} Status Code: {} Message: {}", context, result.status(),
          result.message());
    }

    throw new BadGatewayException("Unexpected error from Jira when trying to " + context);
  }

  static class ServerInfoResponse
  {
    private String deploymentType;

    public void setDeploymentType(final String deploymentType) {
      this.deploymentType = deploymentType;
    }

    public boolean isCloudDeployment() {
      return "Cloud".equalsIgnoreCase(deploymentType);
    }
  }
}
