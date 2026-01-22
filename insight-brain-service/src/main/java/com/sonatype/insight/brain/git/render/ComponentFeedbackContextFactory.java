/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git.render;

import java.util.List;
import java.util.Optional;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.api.v2.dto.remediation.options.ApiVersionChangeOptionType;
import com.sonatype.insight.brain.git.RemediationVersionDTO;
import com.sonatype.insight.brain.git.render.model.ComponentFeedbackContext;
import com.sonatype.insight.brain.git.render.model.SecurityIssue;
import com.sonatype.insight.brain.model.policy.AbstractPolicyViolation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.nexus.scm.SourceControlProvider;

import static com.sonatype.insight.brain.git.render.UTMSourceUtil.maybeAppendUTMSourceParam;
import static com.sonatype.insight.brain.git.render.model.MDImages.DIRECT_DEP_LOGO;
import static com.sonatype.insight.brain.landing.UserInterfaceLinksHelper.getReportUrl;
import static com.sonatype.insight.brain.policy.evaluator.PullRequestDetailsBase.getHighestThreatLevel;
import static java.lang.String.format;

/**
 * Constructs a ComponentFeedbackContext object
 */
@Named
@Singleton
public class ComponentFeedbackContextFactory
{
  private final SecurityIssueService securityIssueService;

  @Inject
  public ComponentFeedbackContextFactory(final SecurityIssueService securityIssueService) {
    this.securityIssueService = securityIssueService;
  }

  public ComponentFeedbackContext build(
      final SourceControlProvider provider,
      final List<PolicyViolation> violations,
      final String displayName,
      final RemediationVersionDTO remediationVersionDTO,
      final String applicationPublicId,
      final String featureBranchScanId,
      final String iqBaseUrl,
      final Optional<String> codeSuggestion,
      final boolean hasReducedSecurityData)
  {
    final int threatLevelValue = getHighestThreatLevel(violations);
    final String componentDetailsLink =
        findComponentReportUrl(iqBaseUrl, violations, applicationPublicId, featureBranchScanId, provider)
            .orElse(null);
    final List<SecurityIssue> securityIssues =
            securityIssueService.getSecurityIssuesFromViolations(iqBaseUrl, violations, provider);
    boolean hasSecurityIssues = hasSecurityIssuesWithSeverityInfo(securityIssues);

    return new ComponentFeedbackContext(
            true, // Only HTML supported SCM providers are supported
            ThreatLevelDisplay.fromValue(threatLevelValue),
            componentDetailsLink,
            displayName,
            provider,
            countBreakingChanges(remediationVersionDTO),
            resolveSuggestedVersion(remediationVersionDTO),
            resolveSuggestedVersionType(remediationVersionDTO),
            hasRemediationForDependencies(remediationVersionDTO),
            securityIssues,
            //For now, we can only process Direct dependencies since
            // there is no line number in the PR diff for the transitive dependency
            DIRECT_DEP_LOGO,
            codeSuggestion.orElse(null),
        hasSecurityIssues,
        hasReducedSecurityData);
  }

  private boolean hasSecurityIssuesWithSeverityInfo(final List<SecurityIssue> securityIssues) {
    return securityIssues.stream()
        .anyMatch(securityIssue -> securityIssue.getSeverityInfo() != null);
  }

  private static String resolveSuggestedVersion(final RemediationVersionDTO remediationVersionDTO) {
    return remediationVersionDTO == null ? "" : remediationVersionDTO.getVersion();
  }
  
  private static String resolveSuggestedVersionType(final RemediationVersionDTO remediationVersionDTO) {
    if (remediationVersionDTO == null || remediationVersionDTO.getRemediationType() == null) {
      return "";
    }
    return remediationVersionDTO.getRemediationType().getDisplayName();
  }

  private static int countBreakingChanges(final RemediationVersionDTO remediationVersionDTO) {
    if (remediationVersionDTO != null && remediationVersionDTO.getBreakingChangesCount() != null) {
      return remediationVersionDTO.getBreakingChangesCount();
    }
    return -1;
  }

  private static boolean hasRemediationForDependencies(final RemediationVersionDTO remediationVersionDTO) {
    if (remediationVersionDTO != null && remediationVersionDTO.getRemediationType() != null) {
      return ApiVersionChangeOptionType.NEXT_NO_VIOLATIONS_WITH_DEPENDENCIES
              .equals(remediationVersionDTO.getRemediationType());
    }
    return false;
  }

  private static Optional<String> findComponentReportUrl(
          final String baseUrl,
          final List<PolicyViolation> violations,
          final String applicationPublicId,
          final String featureBranchScanId,
          final SourceControlProvider provider)
  {
    final String reportPath = getReportUrl(applicationPublicId, featureBranchScanId);
    return extractComponentHash(violations)
        .map(componentHash -> format("/componentDetails/%s?source=pr-line-commenting&tab=violations", componentHash))
        .map(url -> maybeAppendUTMSourceParam(url, provider))
        .map(componentDetailsPath -> baseUrl + reportPath + componentDetailsPath);
  }

  private static Optional<String> extractComponentHash(final List<PolicyViolation> violations) {
    return violations.stream().map(AbstractPolicyViolation::getHash).findFirst();
  }
}
