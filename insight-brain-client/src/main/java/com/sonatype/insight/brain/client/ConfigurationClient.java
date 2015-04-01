/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
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

  /**
   * @deprecated as of version 1.11.0 use {@link #getApplications()} instead. This method does not require
   *             authentication (while {@link #getApplications()} does).
   */
  @Deprecated
  @SuppressWarnings("unchecked")
  public Map<String, String> getApplicationIdNameMap() throws IOException {
    Result result = get(path("rest/application/services/names"));
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
   * applications for which the user has READ permission.
   *
   * @deprecated To be removed once we update all clients that use this method.
   *
   * @since 1.11.0
   */
  @Deprecated
  public ApplicationSummaryList getApplications() throws IOException {
    Result result = get(path("rest/integration/applications"));
    return JsonUtils.parse(result.text(), ApplicationSummaryList.class);
  }

  /**
   * The list of application summaries from the CLM server for which the user can submit an application scan for
   * evaluation.
   *
   * @since 1.14.0
   */
  public ApplicationSummaryList getApplicationsForApplicationEvaluation() throws IOException {
    Result result = get(path("rest/integration/applications?goal=EVALUATE_APPLICATION"));
    return JsonUtils.parse(result.text(), ApplicationSummaryList.class);
  }

  public void validateConfiguration() throws IOException {
    final Result result = get(path("rest/config/proprietary"));
    final String text = result.text();
    // at this point, the network connection appears fine, now let's just check we actually talked to a CLM server
    try {
      // this smoke checks we actually got JSON back and not say some HTML from a misconfigured proxy
      JsonUtils.parse(text, Map.class);
    }
    catch (Exception e) {
      throw new IOException("Server is not compatible with this Sonatype CLM integration", e);
    }
  }

  public void validateApplicationId(final String appId) throws IOException {
    final Result result = get(path("rest/application/validate", UrlUtils.encodeUrlComponent(appId)));
    final String text = result.text();
    if (!"OK".equals(text)) {
      throw new IOException(text);
    }
  }

  public ProprietaryConfig getProprietaryConfiguration() throws IOException {
    Result result = get(path("rest/config/proprietary"));
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
