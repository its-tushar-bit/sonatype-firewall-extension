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

import com.sonatype.insight.brain.api.v2.dto.remediation.options.ApiVersionChangeOptionType;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.git.RemediationVersionDTO;
import com.sonatype.insight.brain.landing.UserInterfaceLinksHelper;
import com.sonatype.insight.brain.model.policy.AbstractPolicyViolation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.utils.TemplateUtils;
import com.sonatype.nexus.scm.SourceControlProvider;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import freemarker.template.Template;
import io.dropwizard.logback.shaded.guava.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;

/**
 * Constructs a pull request line comment from the given values
 */
public class PullRequestLineFeedback
    extends PullRequestDetailsBase
{
  private static Logger log = LoggerFactory.getLogger(PullRequestLineFeedback.class);

  private final List<PolicyViolation> violations;

  private final String displayName;

  private final RemediationVersionDTO remediationVersionDTO;

  private final String applicationPublicId;

  private final String featureBranchScanId;

  private final String iqBaseUrl;

  private final String scmBaseUrl;

  private final Optional<String> codeSuggestion;

  private final boolean scmImprovementsEnabled;

  public PullRequestLineFeedback(
      final List<PolicyViolation> violations,
      final String displayName,
      final String iqBaseUrl,
      final RemediationVersionDTO remediationVersionDTO,
      final String scmBaseUrl,
      final String applicationPublicId,
      final String featureBranchScanId,
      final Optional<String> codeSuggestion,
      final boolean scmImprovementsEnabled,
      final OrganizationDAO organizationDAO,
      final boolean reducedSecurityData)
  {
    super(organizationDAO, reducedSecurityData);
    this.violations = checkNotNull(violations, "violations is required and cannot be null");
    this.displayName = checkNotNull(displayName, "displayName is required and cannot be null");
    this.remediationVersionDTO = remediationVersionDTO;
    this.iqBaseUrl = iqBaseUrl;
    this.scmBaseUrl = scmBaseUrl;
    this.applicationPublicId = checkNotNull(applicationPublicId, "applicationPublicId is required and cannot be null");
    this.featureBranchScanId = checkNotNull(featureBranchScanId, "featureBranchScanId is required and cannot be null");
    this.codeSuggestion = codeSuggestion;
    this.scmImprovementsEnabled = scmImprovementsEnabled;
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
   *         empty if no new violations or no components available
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
  private String constructContents(final SourceControlProvider provider) throws IOException {
    Preconditions.checkState(!violations.isEmpty(), "violations cannot be empty");

    // Get a map containing the values to be populated in the template for the component
    final Map<String, Object> componentFeedbackList =
        getComponentFeedbackList(
            displayName,
            violations,
            iqBaseUrl,
            remediationVersionDTO,
            codeSuggestion,
            provider,
            applicationPublicId,
            featureBranchScanId,
            scmImprovementsEnabled,
            reducedSecurityData);
    return TemplateUtils
        .render(getLineFeedbackTemplate(provider.supportsEmbeddedHtmlInMarkdown(scmBaseUrl)),
            componentFeedbackList);
  }

  /**
   * Gets a list of feedback items for each of the components
   *
   * @param displayName The display name for the given component
   * @param violations The list of violations for the given component
   * @param baseUrl The baseUrl of the IQ server
   * @param remediationVersionDTO Recommended version to upgrade to
   * @return A map containing the feedback for a specific component
   */
  @VisibleForTesting
  static Map<String, Object> getComponentFeedbackList(
      final String displayName,
      final List<PolicyViolation> violations,
      final String baseUrl,
      final RemediationVersionDTO remediationVersionDTO,
      final Optional<String> codeSuggestion,
      final SourceControlProvider provider,
      final String applicationPublicId,
      final String featureBranchScanId,
      final boolean scmImprovementsEnabled,
      final boolean reducedSecurityData)
  {
    int threatLevel = getHighestThreatLevel(violations);
    String threatImage = PullRequestFeedbackDetails.getImageForThreatLevel(threatLevel);
    String suggestedVersion = remediationVersionDTO == null ? "" : remediationVersionDTO.getVersion();
    String suggestedVersionType =
        remediationVersionDTO == null ? "" : remediationVersionDTO.getRemediationType().getDisplayName();
    int breakingChangesCount = -1;
    boolean remediationForDependencies = false;
    if (remediationVersionDTO != null) {
      if (remediationVersionDTO.getBreakingChangesCount() != null) {
        breakingChangesCount = remediationVersionDTO.getBreakingChangesCount();
      }
      if (remediationVersionDTO.getRemediationType() != null) {
        remediationForDependencies = ApiVersionChangeOptionType.NEXT_NO_VIOLATIONS_WITH_DEPENDENCIES
            .equals(remediationVersionDTO.getRemediationType());
      }
    }

    Builder<String, Object> modelMapBuilder = ImmutableMap.<String, Object>builder()
        .put("componentNameAndVersion", displayName)
        .put("threatLevel", threatLevel)
        .put("threatImage", threatImage)
        .put("policiesViolated",
            getPoliciesViolatedMap(violations, baseUrl, true, reducedSecurityData, applicationPublicId,
                featureBranchScanId))
        .put("suggestedVersion", suggestedVersion)
        .put("suggestedVersionType", suggestedVersionType)
        .put("remediationForDependencies", remediationForDependencies)
        .put("codeSuggestion", codeSuggestion)
        .put("breakingChangesCount", breakingChangesCount)
        .put("policiesViolatedCount", violations.size())
        .put("date", new SimpleDateFormat("MMM dd, yyyy").format(new Date()))
        .put("scmChangesEnabled", scmImprovementsEnabled)
        .put("provider", provider);

    findComponentReportUrl(baseUrl, violations, applicationPublicId, featureBranchScanId)
        .ifPresent(url -> modelMapBuilder.put("componentDetailsReportUrl", url));

    return modelMapBuilder.build();
  }

  private static Optional<String> findComponentReportUrl(
      String baseUrl,
      List<PolicyViolation> violations,
      String applicationPublicId,
      String featureBranchScanId)
  {
    String reportPath = UserInterfaceLinksHelper.getReportUrl(applicationPublicId, featureBranchScanId);
    return extractComponentHash(violations)
        .map(componentHash -> format("/componentDetails/%s?source=pr-line-commenting", componentHash))
        .map(componentDetailsPath -> baseUrl + reportPath + componentDetailsPath);
  }

  private static Optional<String> extractComponentHash(List<PolicyViolation> violations) {
    return violations.stream().map(AbstractPolicyViolation::getHash).findFirst();
  }
}
