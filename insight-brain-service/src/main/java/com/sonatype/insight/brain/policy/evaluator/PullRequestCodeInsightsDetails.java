/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.sonatype.insight.brain.component.ComponentDisplayNameUtil;
import com.sonatype.insight.brain.landing.UserInterfaceLinksResource;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.report.ReportEntry;
import com.sonatype.insight.brain.utils.ThreatLevel;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.nexus.scm.api.model.CodeInsightAnnotation;
import com.sonatype.nexus.scm.bitbucket.BitbucketApiClientUtils;
import com.sonatype.nexus.scm.bitbucket.BitbucketCodeInsightAnnotationRequestBuilder;
import com.sonatype.nexus.scm.bitbucket.BitbucketCodeInsightAnnotationType;
import com.sonatype.nexus.scm.bitbucket.BitbucketCodeInsightReportOutcome;
import com.sonatype.nexus.scm.bitbucket.BitbucketCodeInsightSeverity;
import com.sonatype.nexus.scm.bitbucket.BitbucketLinkDataParameter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
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

  private final String repositoryUrl;

  private final Application application;

  private final ReportEntry bomReportEntry;

  private final PolicyEvaluation featureBranchEvaluation;

  private final PolicyViolationDiff<PolicyViolation> policyViolationDiff;

  private final String baseUrl;

  private final Map<String, String> componentDisplayNamesMap;

  private final List<PolicyViolation> newPolicyViolations;

  private final List<PolicyViolation> clearedPolicyViolations;

  public PullRequestCodeInsightsDetails(
      final String repositoryUrl,
      final Application application,
      final ReportEntry bomReportEntry,
      final PolicyEvaluation featureBranchEvaluation,
      final PolicyViolationDiff<PolicyViolation> policyViolationDiff,
      final String baseUrl)
  {
    this.repositoryUrl = checkNotNull(repositoryUrl, "repositoryUrl is required and cannot be null");
    this.application = checkNotNull(application, "app is required and cannot be null");
    this.bomReportEntry = checkNotNull(bomReportEntry, "bomReportEntry is required and cannot be null");
    this.featureBranchEvaluation = checkNotNull(featureBranchEvaluation,
        "featureBranchEvaluation is required and cannot be null");
    this.policyViolationDiff = checkNotNull(policyViolationDiff, "policyViolationDiff is required and cannot be null");
    checkNotNull(policyViolationDiff.getAppeared(), "new violations data is required, and cannot be null");
    this.baseUrl = checkNotNull(baseUrl, "baseUrl is required and cannot be null");

    componentDisplayNamesMap = createDisplayNamesMap();

    newPolicyViolations = getComponentPolicyViolationsMap(policyViolationDiff.getAppeared(),
        componentDisplayNamesMap);

    clearedPolicyViolations = getComponentPolicyViolationsMap(
        policyViolationDiff.getCleared(),
        componentDisplayNamesMap);
  }

  /**
   * Get the value for the 'details' field of the report. This is the main description field.
   */
  public String getReportDetails() {
    // Process new violations
    long policiesViolatedCount = newPolicyViolations.size();
    int componentCountForPolicyViolated = newPolicyViolations.stream()
        .collect(Collectors.groupingBy(policyViolation -> componentDisplayNamesMap.get(policyViolation.getHash())))
        .size();

    // Process cleared violations
    int fixedPolicyViolationsCount = clearedPolicyViolations.size();
    int componentCountForFixedPolicyViolations = clearedPolicyViolations.stream()
        .filter(policyViolation -> componentDisplayNamesMap.containsKey(policyViolation.getHash()))
        .collect(Collectors.groupingBy(policyViolation -> componentDisplayNamesMap.get(policyViolation.getHash())))
        .size();

    String timestamp = DATE_TIME_FORMATTER.format(ZonedDateTime.ofInstant(Instant.ofEpochMilli(bomReportEntry.time),
        ZoneId.systemDefault()));
    StringBuilder stringBuilder = new StringBuilder();
    if (policiesViolatedCount > 0) {
      stringBuilder.append(String
          .format("On %s, Nexus IQ found %d new policy %s affecting %d %s.", timestamp, policiesViolatedCount,
              violationsSuffix(policiesViolatedCount), componentCountForPolicyViolated,
              componentsSuffix(componentCountForPolicyViolated)));
    }
    else {
      stringBuilder.append(String.format("Nexus IQ found no new policy violations on %s.", timestamp));
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
    return policyViolationDiff.getAppeared()
        .isEmpty() ? BitbucketCodeInsightReportOutcome.PASS : BitbucketCodeInsightReportOutcome.FAIL;
  }

  public URI getReportUri() {
    logBaseUriIfLocalhost();
    return URI.create(baseUrl +
        UserInterfaceLinksResource.getReportUrl(application.getPublicId(), featureBranchEvaluation.getScanId()));
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

    newPolicyViolations.forEach(policyViolation -> {
      String componentDisplayName = componentDisplayNamesMap.get(policyViolation.getHash());
      AnnotationContent annotationContent = new AnnotationContent(policyViolation, componentDisplayName);
      BitbucketCodeInsightSeverity severity = getSeverity(policyViolation.getThreatLevel());

      // TODO: Path and LOC to be completed once line-level commenting is available
      String path = null;
      Integer lineOfCode = null;

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

    return builder.build();
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


  /**
   * Gets the display names for all components in the BOM and components in the cleared policy violations section
   * (some of them may not be included in the BOM).
   *
   * @return Returns a map with the component hash as the key and the component display name as the value
   */
  Map<String, String> createDisplayNamesMap() {
    final Map<String, String> componentDisplayNamesMap = new HashMap<>();
    JsonNode bomJson = loadJson();
    if (bomJson != null) {
      bomJson = bomJson.get("aaData");
      if (bomJson != null) {
        final ArrayNode bomJsonArray = (ArrayNode) bomJson;
        bomJsonArray.forEach(jsonNode -> {
          final String hash = JsonUtils.getNullableString(jsonNode.get("hash"));
          componentDisplayNamesMap.put(hash, ComponentDisplayNameUtil.fromJsonNode((ObjectNode) jsonNode).toString());
        });
      }
    }
    if (policyViolationDiff.hasCleared()) {
      // add mappings for the components from the cleared violations section; some may not be included in the bom file
      List<PolicyViolation> cleared = policyViolationDiff.getCleared();
      for (PolicyViolation violation : cleared) {
        String hash = violation.getHash();
        if (violation.getComponentIdentifier() != null && !componentDisplayNamesMap.containsKey(hash)) {
          componentDisplayNamesMap.put(hash,
              ComponentDisplayNameUtil.fromIdentifier(violation.getComponentIdentifier()).toString());
        }
      }
    }
    return componentDisplayNamesMap;
  }

  private JsonNode loadJson() {
    checkNotNull(bomReportEntry.buf, "bom data is required, and cannot be null");
    try {
      return JsonUtils.parse(bomReportEntry.buf);
    }
    catch (final IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private List<PolicyViolation> getComponentPolicyViolationsMap(
      final List<PolicyViolation> violations,
      final Map<String, String> componentDisplayNamesMap)
  {
    return violations.stream()
        .filter(policyViolation -> componentDisplayNamesMap.containsKey(policyViolation.getHash()))
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
          ImmutableList.of(policyViolation), baseUrl, false);

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
