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

import com.sonatype.clm.dto.model.ProprietaryConfig;
import com.sonatype.clm.dto.model.application.ApplicationSummaryList;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.client.utils.HttpClientUtils.Configuration;
import com.sonatype.insight.client.utils.Result;
import com.sonatype.insight.client.utils.UrlUtils;
import com.sonatype.insight.json.store.JsonUtils;

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
    verifyStatusCode(result);
    return result;
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
   * The list of application summaries from the CLM server for which the user can submit an application scan for
   * evaluation.
   *
   * @since 1.14.0
   */
  public ApplicationSummaryList getApplicationsForApplicationEvaluation() throws IOException {
    Result result = get(path("rest/integration/applications?goal=EVALUATE_APPLICATION"));
    return JsonUtils.parse(result.text(), ApplicationSummaryList.class);
  }

  /**
   * Gets the list of application summaries from the CLM server for which the user can retrieve a summary of a recent
   * policy evaluation (see {@link PolicyClient#getPolicyEvaluationSummary(Stage)}).
   * 
   * @since 1.14.0
   */
  public ApplicationSummaryList getApplicationsForEvaluationSummary() throws IOException {
    Result result = get(path("rest/integration/applications?goal=SUMMARIZE_EVALUATION"));
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
      throw new IOException("Server is not compatible with this Nexus IQ integration", e);
    }
  }

  public void validateApplicationId(final String appId) throws IOException {
    final Result result = get(path("rest/application/validate", UrlUtils.encodeUrlComponent(appId)));
    final String text = result.text();
    if (!"OK".equals(text)) {
      throw new IOException(text);
    }
  }

  /**
   * Get the proprietary configuration used for an application evaluation.
   * 
   * @since 1.22.0
   */
  public ProprietaryConfig getProprietaryConfigForApplicationEvaluation(String applicationPublicId)
      throws IOException
  {
    Result result = get(path("rest/config/proprietary").query("goal", "EVALUATE_APPLICATION", "applicationPublicId",
        applicationPublicId));
    return JsonUtils.parse(result.text(), ProprietaryConfig.class);
  }

  /**
   * Get the proprietary configuration used for a component evaluation.
   * 
   * @since 1.22.0
   */
  public ProprietaryConfig getProprietaryConfigForComponentEvaluation(String applicationPublicId)
      throws IOException
  {
    Result result = get(path("rest/config/proprietary").query("goal", "EVALUATE_COMPONENT", "applicationPublicId",
        applicationPublicId));
    return JsonUtils.parse(result.text(), ProprietaryConfig.class);
  }

  public void validateAuthentication() throws IOException {
    final Result result = postRequest(path("rest/user/session"), null);
    verifyStatusCode(result);
  }
}
