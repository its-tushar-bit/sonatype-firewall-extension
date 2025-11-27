/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.net.URI;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.git.SourceControlComponentDetails;
import com.sonatype.insight.brain.landing.UserInterfaceLinksHelper;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.actions.FailActionType;
import com.sonatype.insight.brain.utils.ThreatLevel;
import com.sonatype.nexus.iq.location.dto.LocationDiscoveryResult;
import com.sonatype.nexus.iq.location.dto.RankedSourceLocation;
import com.sonatype.nexus.scm.api.model.CodeInsightAnnotation;
import com.sonatype.nexus.scm.bitbucket.BitbucketApiClientUtils;
import com.sonatype.nexus.scm.bitbucket.BitbucketCodeInsightAnnotationRequestBuilder;
import com.sonatype.nexus.scm.bitbucket.BitbucketCodeInsightAnnotationType;
import com.sonatype.nexus.scm.bitbucket.BitbucketCodeInsightReportOutcome;
import com.sonatype.nexus.scm.bitbucket.BitbucketCodeInsightSeverity;
import com.sonatype.nexus.scm.bitbucket.BitbucketLinkDataParameter;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.sonatype.nexus.scm.bitbucket.BitbucketCodeInsightAnnotationType.CODE_SMELL;

/**
 * Data utility class which stores, combines and normalizes the data for Code Insights. It accepts
 * all relevant data from the policy evaluation plus all supporting configuration in order
 * to generate all of the fields and values required to show on a Code insight Report.
 */
public class PullRequestCodeInsightsDetails
    extends PullRequestDetailsBase
{
  private static final Logger log = LoggerFactory.getLogger(PullRequestCodeInsightsDetails.class);

  private static final BitbucketCodeInsightAnnotationType ANNOTATION_TYPE = CODE_SMELL;

  // We provide the IQ report link at the Code Insight report level. The annotation link is not required.
  private static final URI ANNOTATION_LINK = null;

  @SuppressWarnings("checkstyle:LineLength")
  // BitBucket Cloud and BitBucket Server have a limit of 1000 annotations per report
  // https://support.atlassian.com/bitbucket-cloud/docs/code-insights/#Annotations
  // https://developer.atlassian.com/server/bitbucket/rest/v819/api-group-builds-and-deployments/#api-insights-latest-projects-projectkey-repos-repositoryslug-commits-commitid-reports-key-annotations-post
  private static final int BITBUCKET_ANNOTATION_LIMIT = 1000;

  private final String repositoryUrl;

  private final Application application;

  private final SourceControlComponentDetails sourceControlComponentDetails;

  private final PolicyEvaluation featureBranchEvaluation;

  private final PolicyViolationDiff<PolicyViolation> policyViolationDiff;

  private final String baseUrl;

  private final List<PolicyViolation> newPolicyViolations;

  private final List<PolicyViolation> clearedPolicyViolations;

  private final LocationDiscoveryResult locationDiscoveryResult;

  private final PolicyDAO policyDAO;

  public PullRequestCodeInsightsDetails(
      final String repositoryUrl,
      final Application application,
      final SourceControlComponentDetails sourceControlComponentDetails,
      final PolicyEvaluation featureBranchEvaluation,
      final PolicyViolationDiff<PolicyViolation> policyViolationDiff,
      final String baseUrl,
      final LocationDiscoveryResult locationDiscoveryResult,
      final PolicyDAO policyDAO,
      final OrganizationDAO organizationDAO, final boolean reducedSecurityData)
  {
    super(organizationDAO, reducedSecurityData);
    this.repositoryUrl = checkNotNull(repositoryUrl, "repositoryUrl is required and cannot be null");
    this.application = checkNotNull(application, "app is required and cannot be null");
    this.sourceControlComponentDetails =
        checkNotNull(sourceControlComponentDetails, "sourceControlComponentDetails is required and cannot be null");
    this.featureBranchEvaluation = checkNotNull(featureBranchEvaluation,
        "featureBranchEvaluation is required and cannot be null");
    this.policyViolationDiff = checkNotNull(policyViolationDiff, "policyViolationDiff is required and cannot be null");
    this.policyDAO = policyDAO;
    checkNotNull(policyViolationDiff.getAppeared(), "new violations data is required, and cannot be null");
    this.baseUrl = checkNotNull(baseUrl, "baseUrl is required and cannot be null");
    this.locationDiscoveryResult = locationDiscoveryResult;

    newPolicyViolations = getComponentPolicyViolationsMap(policyViolationDiff.getAppeared());

    clearedPolicyViolations = getComponentPolicyViolationsMap(policyViolationDiff.getCleared());
  }

  /**
   * Get the value for the 'details' field of the report. This is the main description field.
   */
  public String getReportDetails() {
    // Process new violations
    long policiesViolatedCount = newPolicyViolations.size();
    int componentCountForPolicyViolated = newPolicyViolations.stream()
        .collect(Collectors
            .groupingBy(policyViolation -> sourceControlComponentDetails.getComponentInfo(policyViolation.getHash())))
        .size();

    // Process cleared violations
    int fixedPolicyViolationsCount = clearedPolicyViolations.size();
    int componentCountForFixedPolicyViolations = clearedPolicyViolations.stream()
        .filter(policyViolation -> sourceControlComponentDetails.getComponentInfo(policyViolation.getHash()) != null)
        .collect(Collectors
            .groupingBy(policyViolation -> sourceControlComponentDetails.getComponentInfo(policyViolation.getHash())))
        .size();

    String timestamp = DATE_TIME_FORMATTER.format(ZonedDateTime.ofInstant(featureBranchEvaluation.getTime().toInstant(),
        ZoneId.systemDefault()));
    StringBuilder stringBuilder = new StringBuilder();
    if (policiesViolatedCount > 0) {
      stringBuilder.append(String
          .format("On %s, Sonatype Lifecycle found %d new policy %s affecting %d %s.", timestamp, policiesViolatedCount,
              violationsSuffix(policiesViolatedCount), componentCountForPolicyViolated,
              componentsSuffix(componentCountForPolicyViolated)));
    }
    else {
      stringBuilder.append(String.format("Sonatype Lifecycle found no new policy violations on %s.", timestamp));
    }
    if (fixedPolicyViolationsCount > 0) {
      stringBuilder.append(" ");
      stringBuilder.append(String.format("%d outstanding policy %s fixed, affecting %d %s", fixedPolicyViolationsCount,
          violationsSuffix(fixedPolicyViolationsCount), componentCountForFixedPolicyViolations,
          componentsSuffix(componentCountForFixedPolicyViolations)));
    }
    return stringBuilder.toString();
  }

  private String violationsSuffix(final long count) {
    return count > 1 ? "violations" : "violation";
  }

  private String componentsSuffix(final int count) {
    return count > 1 ? "components" : "component";
  }

  public BitbucketCodeInsightReportOutcome getReportOutcome() {
    List<PolicyViolation> policyViolations = policyViolationDiff.getAppeared();
    for (PolicyViolation policyViolation : policyViolations) {
      Policy policy = policyDAO.getById(policyViolation.getPolicyId());
      List<Action> actions = policy.toActions(featureBranchEvaluation.getStageTypeId(), false, null);
      for (Action action : actions) {
        if (action.getActionTypeId().equals(FailActionType.ID)) {
          return BitbucketCodeInsightReportOutcome.FAIL;
        }
      }
    }

    return BitbucketCodeInsightReportOutcome.PASS;
  }

  public URI getReportUri() {
    logBaseUriIfLocalhost();
    return URI.create(baseUrl +
        UserInterfaceLinksHelper.getReportUrl(application.getPublicId(), featureBranchEvaluation.getScanId()));
  }

  /**
   * Bitbucket Cloud inexplicably will outright fail any request which references localhost for the 'link' field. The
   * error is also *VERY* unhelpful as it simply says 'link is not a valid URL'. This will happen to Sonatype
   * developers/employees during testing/demos and even for customers (though most would have a qualified IQ host name).
   * Give these folks a heads up that their request is about to fail so they can shake their fists at Bitbucket
   */
  private void logBaseUriIfLocalhost() {
    if (URI.create(baseUrl).getHost().equals("localhost") && BitbucketApiClientUtils.isCloudHosted(repositoryUrl)) {
      log.warn("Bitbucket Cloud disallows a value of 'localhost' for referenced links. Please use a qualified " +
          "hostname for your IQ base URL");
    }
  }

  /**
   * Get the six data fields for the report
   */
  public Map<String, Object> getReportData() {
    int critical = 0;
    int severe = 0;
    int moderate = 0;
    for (PolicyViolation policyViolation : newPolicyViolations) {
      ThreatLevel threatLevel = ThreatLevel.from(policyViolation.getThreatLevel());
      if (threatLevel == ThreatLevel.CRITICAL) {
        critical++;
      }
      else if (threatLevel == ThreatLevel.SEVERE) {
        severe++;
      }
      else if (threatLevel == ThreatLevel.MODERATE) {
        moderate++;
      }
    }

    // Order is important here. The six data fields in the report go from left to right, top to bottom.
    // So in the UI the order defined below will be the following. Note Critical/Severe/Moderate are on the left side:
    // | Critical: 0 | Organization: org |
    // | Severe: 0   | Stage: stage      |
    // | Moderate: 0 | Details: details  |
    return new ImmutableMap.Builder<String, Object>()
        .put("Critical", critical)
        .put("Organization", getOrganizationName(application))
        .put("Severe", severe)
        .put("Stage", featureBranchEvaluation.getStageTypeId())
        .put("Moderate", moderate)
        .put("Details", new BitbucketLinkDataParameter(repositoryUrl, "Application Report", getReportUri()))
        .build();
  }

  public List<CodeInsightAnnotation> getAnnotations() {
    BitbucketCodeInsightAnnotationRequestBuilder builder = new BitbucketCodeInsightAnnotationRequestBuilder(
        repositoryUrl);

    if (newPolicyViolations.size() > BITBUCKET_ANNOTATION_LIMIT) {
      builder.withAnnotation(
          "%s component(s) with violations - ".formatted(newPolicyViolations.size()),
          null,
          BitbucketCodeInsightSeverity.HIGH,
          ANNOTATION_TYPE,
          getReportUri(),
          null,
          null
      );
    }
    else {
      newPolicyViolations.forEach(policyViolation -> {
        String componentDisplayName =
            sourceControlComponentDetails.getComponentInfo(policyViolation.getHash()).getDisplayName();
        AnnotationContent annotationContent = new AnnotationContent(policyViolation, componentDisplayName);
        BitbucketCodeInsightSeverity severity = getSeverity(policyViolation.getThreatLevel());

        String path = null;
        Integer lineOfCode = null;
        RankedSourceLocation matchingLocation = findViolationLocation(locationDiscoveryResult, policyViolation);
        if (matchingLocation != null) {
          path = matchingLocation.getFilePath();
          lineOfCode = matchingLocation.getLineNumber();
        }

        builder.withAnnotation(
            annotationContent.message,
            annotationContent.details,
            severity,
            ANNOTATION_TYPE,
            ANNOTATION_LINK,
            path,
            lineOfCode
        );
      });
    }
    return builder.build();
  }

  /**
   * attempt to find a source location for a given policy violation. If nothing is found, returns null
   * @param locationDiscoveryResult list of all possibly location discovery results for violations
   * @param policyViolation violation to search for
   * @return top ranked source location if found, null otherwise
   */
  private RankedSourceLocation findViolationLocation(
      LocationDiscoveryResult locationDiscoveryResult,
      PolicyViolation policyViolation)
  {
    if (locationDiscoveryResult == null) {
      return null;
    }

    if (locationDiscoveryResult.getLocationMap().containsKey(policyViolation.getComponentIdentifier())) {
      List<RankedSourceLocation> rankedSourceLocations =
          locationDiscoveryResult.getLocationMap().get(policyViolation.getComponentIdentifier());
      if (!rankedSourceLocations.isEmpty()) {
        return rankedSourceLocations.get(0);
      }
    }
    return null;
  }

  private BitbucketCodeInsightSeverity getSeverity(final Integer threatLevel) {
    if (threatLevel >= 8) {
      return BitbucketCodeInsightSeverity.HIGH;
    }
    if (threatLevel >= 4) {
      return BitbucketCodeInsightSeverity.MEDIUM;
    }
    return BitbucketCodeInsightSeverity.LOW;
  }

  private List<PolicyViolation> getComponentPolicyViolationsMap(
      final List<PolicyViolation> violations)
  {
    return violations.stream()
        .filter(policyViolation -> sourceControlComponentDetails.getComponentInfo(policyViolation.getHash()) != null)
        .collect(Collectors.toList());
  }

  /**
   * Encapsulate the differences between V1 and V2 regarding the summary/message/details fields
   * For Bitbucket Server (V1) there is only a single 'message' field that will contain all the information
   * For Bitbucket Cloud (V2) there are two fields so we split all the information: summary and details
   */
  private class AnnotationContent
  {
    String message;

    String details;

    AnnotationContent(
        final PolicyViolation policyViolation,
        final String componentDisplayName)
    {
      // Message format is:  {threat level} - {policy name} - {component name}
      message = String.format("%d - %s - %s", policyViolation.getThreatLevel(), policyViolation.getPolicyName(),
          componentDisplayName);

      List<Map<String, Object>> constraintsForPolicyViolationsPerPolicy = getConstraintsForPolicyViolationsPerPolicy(
          ImmutableList.of(policyViolation), baseUrl, false, reducedSecurityData, application.getPublicId(),
          featureBranchEvaluation.getScanId());

      // Details format is: {constraint name}: {condition, condition,...}, ...
      details = constraintsForPolicyViolationsPerPolicy
          .stream()
          .map(map -> {
            String constraint = (String) map.get(CONSTRAINT_NAME);
            List<String> conditions = (List<String>) map.get(CONDITIONS);
            return constraint + ": " + String.join(", ", conditions);
          })
          .collect(Collectors.joining(","));

      if (!BitbucketApiClientUtils.isCloudHosted(repositoryUrl)) {
        message = message + " - " + details;
      }
    }
  }
}
