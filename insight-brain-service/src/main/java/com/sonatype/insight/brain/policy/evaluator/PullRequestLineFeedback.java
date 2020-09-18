/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.utils.TemplateUtils;
import com.sonatype.nexus.scm.SourceControlProvider;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import freemarker.template.Template;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Constructs a pull request line comment from the given values
 */
public class PullRequestLineFeedback
    extends PullRequestDetailsBase
{
  private static Logger log = LoggerFactory.getLogger(PullRequestLineFeedback.class);
  
  private final List<PolicyViolation> violations;

  private final String displayName;

  private final String suggestedVersion;

  private final String baseUrl;

  public PullRequestLineFeedback(
      final List<PolicyViolation> violations,
      final String displayName,
      final String baseUrl,
      final String suggestedVersion)
  {
    Preconditions.checkNotNull(violations, "violations is required and cannot be null");
    this.violations = violations;
    Preconditions.checkNotNull(displayName, "displayName is required and cannot be null");
    this.displayName = displayName;
    this.suggestedVersion = suggestedVersion;
    this.baseUrl = baseUrl;
  }

  private synchronized Template getLineFeedbackTemplate(final boolean includeEmbeddedHtml) throws IOException {
    Template lineFeedbackTemplate;

    if (includeEmbeddedHtml) {
      lineFeedbackTemplate =
          TemplateUtils.createFreemarkerConfig().getTemplate("pullrequest-line-feedback.ftl");
    }
    else {
      lineFeedbackTemplate =
          TemplateUtils.createFreemarkerConfig().getTemplate("pullrequest-minimal-markdown-line-feedback.ftl");
    }
    return lineFeedbackTemplate;
  }

  /**
   * Renders the template and returns the content
   *
   * @return An optional variable containing the Markdown-formatted contents of the Pull Request Line Comment, will be
   * empty if no new violations or no components available
   */
  public Optional<String> renderTemplateAndGetContents(
      final SourceControlProvider provider)
  {
    String contents = null;
    try {
      contents = constructContents(provider);
    }
    catch (IOException e) {
      log.debug("Cannot create PR line comment content", e);
    }
    return Optional.ofNullable(contents);
  }

  /**
   * Constructs the contents for the PR Line feedback
   *
   * @return An optional variable containing the PR line feedback contents
   * @throws IOException
   */
  private String constructContents(final SourceControlProvider provider)
      throws IOException
  {
    Preconditions.checkState(!violations.isEmpty(), "violations cannot be empty");

    //Get a map containing the values to be populated in the template for the component
    final Map<String, Object> componentFeedbackList =
        getComponentFeedbackList(displayName, violations, baseUrl, suggestedVersion, provider);
    return TemplateUtils
        .render(getLineFeedbackTemplate(provider.supportsEmbeddedHtmlInMarkdown()), componentFeedbackList);
  }

  /**
   * Gets a list of feedback items for each of the components
   *
   * @param displayName      The display name for the given component
   * @param violations       The list of violations for the given component
   * @param baseUrl          The baseUrl of the IQ server
   * @param suggestedVersion Recommended version to upgrade to
   * @return A map containing the feedback for a specific component
   */
  @VisibleForTesting
  static Map<String, Object> getComponentFeedbackList(
      final String displayName,
      final List<PolicyViolation> violations,
      final String baseUrl,
      final String suggestedVersion,
      final SourceControlProvider provider)
  {
    int threatLevel = getHighestThreatLevel(violations);
    String threatImage = PullRequestFeedbackDetails.getImageForThreatLevel(threatLevel);
    return ImmutableMap.<String, Object>builder()
        .put("componentNameAndVersion", displayName)
        .put("threatLevel", threatLevel)
        .put("threatImage", threatImage)
        .put("policiesViolated", getPoliciesViolatedMap(violations, baseUrl, true))
        .put("suggestedVersion", suggestedVersion == null ? "" : suggestedVersion)
        .put("policiesViolatedCount", violations.size())
        .put("date", new SimpleDateFormat("MMM dd, yyyy").format(new Date()))
        .put("provider", provider)
        .build();
  }
}
