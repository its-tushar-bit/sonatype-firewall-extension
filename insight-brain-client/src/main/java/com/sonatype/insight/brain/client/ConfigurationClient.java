/*
 * Copyright (c) 2011-2015 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.client;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import com.sonatype.clm.dto.model.ProprietaryConfig;
import com.sonatype.clm.dto.model.application.ApplicationSummaryList;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.client.utils.HttpClientUtils.Configuration;
import com.sonatype.insight.client.utils.Result;
import com.sonatype.insight.client.utils.UrlUtils;
import com.sonatype.insight.json.store.JsonUtils;

import org.apache.http.client.HttpResponseException;

public class ConfigurationClient
    extends AbstractRequestClient
{
  public enum Context
  {
    ALL, CI, CLI, QA, RM, MAVEN
  }

  public ConfigurationClient(final Configuration config) {
    super(config);
  }

  private Result get(RequestBuilder builder) throws IOException {
    final Result result = getRequest(builder);
    final int status = result.status();
    if (status >= 300) {
      String msg = result.message();
      throw new HttpResponseException(status, msg);
    }
    return result;
  }

  private Result getAnon(RequestBuilder builder) throws IOException {
    try {
      return get(builder);
    }
    catch (HttpResponseException e) {
      if (e.getStatusCode() == 401) {
        /*
         * For clients making anonymous calls, a misconfigured base URL will make the client encounter authentication
         * errors from protected resources, so tweak the user facing error message to better highlight the proper
         * remediation.
         */
        throw new HttpResponseException(e.getStatusCode(), "Resource not found, please check your request URL.");
      }
      throw e;
    }
  }

  /**
   * @deprecated as of version 1.11.0 use {@link #getApplications()} instead. This method does not require
   *             authentication (while {@link #getApplications()} does).
   */
  @Deprecated
  @SuppressWarnings("unchecked")
  public Map<String, String> getApplicationIdNameMap() throws IOException {
    Result result = getAnon(path("rest/application/services/names"));
    Map<String, String> applicationsById = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);
    applicationsById.putAll(JsonUtils.parse(result.text(), Map.class));
    return applicationsById;
  }

  /**
   * @since 1.13
   */
  public List<Stage> getLicensedStages(final Context context) throws IOException {
    RequestBuilder requestBuilder = path("rest/policy/stages");
    if (context == null) {
      throw new IllegalArgumentException("Context can not be null");
    }
    requestBuilder = requestBuilder.query("context", context.name().toLowerCase(Locale.ENGLISH));
    Result result = get(requestBuilder);
    final String jsonResult = result.text();
    if (jsonResult == null) {
      return Collections.emptyList();
    }
    Stage[] stageArray = JsonUtils.parse(jsonResult, Stage[].class);
    return Arrays.asList(stageArray);
  }

  /**
   * The list of application summaries from the CLM server. This method requires authentication and it returns only the
   * applications the user is authorized to see.
   *
   * @since 1.11.0
   */
  public ApplicationSummaryList getApplications() throws IOException {
    Result result = get(path("rest/integration/applications"));
    return JsonUtils.parse(result.text(), ApplicationSummaryList.class);
  }

  public void validateConfiguration() throws IOException {
    final Result result = getAnon(path("rest/version"));
    final String text = result.text();
    // at this point, the network connection appears fine, now let's just check we actually talked to a CLM server
    try {
      final Map<?, ?> versionInfo = JsonUtils.parse(text, Map.class);
      if (versionInfo.get("version") == null && versionInfo.get("name") == null) {
        throw new Exception("No CLM version information present");
      }
    }
    catch (Exception e) {
      throw new IOException("Server is not compatible with this Sonatype CLM integration", e);
    }
  }

  public void validateApplicationId(final String appId) throws IOException {
    final Result result = getAnon(path("rest/application/validate", UrlUtils.encodeUrlComponent(appId)));
    final String text = result.text();
    if (!"OK".equals(text)) {
      throw new IOException(text);
    }
  }

  public ProprietaryConfig getProprietaryConfiguration() throws IOException {
    Result result = getAnon(path("rest/config/proprietary"));
    return JsonUtils.parse(result.text(), ProprietaryConfig.class);
  }

  public void validateAuthentication() throws IOException {
    final Result result = path("rest/user/session").post(null);
    final int status = result.status();
    if (status >= 300) {
      throw new HttpResponseException(status, result.message());
    }
  }
}
