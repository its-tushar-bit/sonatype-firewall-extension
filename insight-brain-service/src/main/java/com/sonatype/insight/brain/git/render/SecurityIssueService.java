/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git.render;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.api.v2.service.VulnerabilityDetailsService;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.git.render.model.MDImages;
import com.sonatype.insight.brain.git.render.model.SecurityIssue;
import com.sonatype.insight.brain.git.render.model.SeverityInfo;
import com.sonatype.insight.brain.model.HasComponentId;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.vulnerability.model.SecurityVulnerabilityData;
import com.sonatype.nexus.scm.SourceControlProvider;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Throwables.getRootCause;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.sonatype.insight.brain.git.render.ReferenceIdParser.parseReferenceIds;
import static com.sonatype.insight.brain.git.render.UTMSourceUtil.maybeAppendUTMSourceParam;
import static com.sonatype.insight.brain.git.render.model.MDImages.SONATYPE_DEEP_DIVE_TAG;
import static com.sonatype.insight.brain.git.render.model.MDImages.SONATYPE_FAST_TRACK_TAG;
import static com.sonatype.insight.brain.landing.UserInterfaceLinksHelper.getPolicyViolationReportPath;
import static com.sonatype.insight.brain.model.OwnerType.APPLICATION;
import static java.util.Objects.nonNull;

@Named
@Singleton
public class SecurityIssueService
{
  private static final Logger log = LoggerFactory.getLogger(SecurityIssueService.class);

  private static final String NO_REF_IDS_SENTINEL_KEY = "";

  private final VulnerabilityDetailsService vulnerabilityDetailsService;

  private final PolicyViolationDAO policyViolationDAO;

  @Inject
  public SecurityIssueService(
      final VulnerabilityDetailsService vulnerabilityDetailsService,
      final PolicyViolationDAO policyViolationDAO)
  {
    this.vulnerabilityDetailsService = vulnerabilityDetailsService;
    this.policyViolationDAO = policyViolationDAO;
  }

  public List<SecurityIssue> getSecurityIssuesFromViolations(
      final String iqBaseUrl,
      final List<PolicyViolation> policyViolations,
      final SourceControlProvider provider)
  {
    policyViolationDAO.loadConstraintFacts(policyViolations);
    // Since a vulnerability can be associated with multiple PVs,
    // we need to aggregate to ensure only 1 call to the vulnerabilityDetailsService is made per vulnerability
    final Map<String, Collection<PolicyViolation>> agg = aggregateViolationsByRefId(policyViolations);

    final ComponentIdentifier componentIdentifier = resolveComponentIdentifier(policyViolations).orElse(null);
    return agg.entrySet()
        .stream()
        .flatMap(e -> streamSecurityIssues(iqBaseUrl, componentIdentifier, e.getKey(), e.getValue(), provider))
        .sorted(SecurityIssueComparator.ASC)
        .collect(toImmutableList());
  }

  private Map<String, Collection<PolicyViolation>> aggregateViolationsByRefId(
      final List<PolicyViolation> policyViolations)
  {
    final MultiValuedMap<String, PolicyViolation> agg = new ArrayListValuedHashMap<>();
    policyViolations.forEach(policyViolation -> collectRefIdsForViolation(agg, policyViolation));
    return ImmutableMap.copyOf(agg.asMap());
  }

  private void collectRefIdsForViolation(
      final MultiValuedMap<String, PolicyViolation> agg,
      final PolicyViolation policyViolation)
  {
    final Set<String> refIds = parseReferenceIds(policyViolation);
    if (refIds.isEmpty()) {
      agg.put(NO_REF_IDS_SENTINEL_KEY, policyViolation);
    }
    else {
      refIds.forEach(refId -> agg.put(refId, policyViolation));
    }
  }

  private Stream<SecurityIssue> streamSecurityIssues(
      final String iqBaseUrl,
      final ComponentIdentifier componentIdentifier,
      final String refId,
      final Collection<PolicyViolation> policyViolationsForRefId,
      final SourceControlProvider provider)
  {
    final String applicationId = policyViolationsForRefId.stream()
        .findFirst()
        .map(PolicyViolation::getOwnerId)
        .orElse(null);
    final boolean policyViolationsHasVulnerabilities = !NO_REF_IDS_SENTINEL_KEY.equals(refId);
    final SecurityVulnerabilityData securityVulnerabilityData = policyViolationsHasVulnerabilities ?
            findSecurityVulnerabilityData(refId, componentIdentifier, applicationId ) : null;
    return policyViolationsForRefId.stream().map(pv ->
        buildSecurityIssue(iqBaseUrl, pv, securityVulnerabilityData, provider));
  }

  private SecurityVulnerabilityData findSecurityVulnerabilityData(final String refId,
                                                                  final ComponentIdentifier componentIdentifier,
                                                                  final String applicationId )
  {
    try {
      return vulnerabilityDetailsService.getSecurityVulnerabilityDetails(
          refId,
          componentIdentifier,
          null,
          null,
          nonNull(applicationId) ? APPLICATION : null,
          applicationId,
          true);
    }
    catch (final NotFoundException e) {
      log.debug("Could not find vulnerability details for referenceId '{}'", refId);
    }
    catch (final Throwable t) {
      log.error("Error getting security vulnerability details for referenceId '{}': {}",
          refId, getRootCause(t).getMessage());
    }
    return null;
  }

  private static String trimTrailingSlash(final String s) {
    if (s.endsWith("/")) {
      return s.substring(0, s.length() - 1);
    }
    return s;
  }

  private static SecurityIssue buildSecurityIssue(
      final String iqBaseUrl,
      final PolicyViolation policyViolation,
      final SecurityVulnerabilityData securityVulnerabilityDetails,
      final SourceControlProvider provider)
  {
    final String policyViolationDetailsLink = buildPolicyViolationDetailsLink(
        iqBaseUrl, policyViolation.getId(), provider);
    final boolean isSecurityVulnerabilityDataDefined = nonNull(securityVulnerabilityDetails);
    return new SecurityIssue(
        policyViolation.getThreatLevel(),
        isSecurityVulnerabilityDataDefined ? buildSeverityInfo(securityVulnerabilityDetails) : null,
        isSecurityVulnerabilityDataDefined ? securityVulnerabilityDetails.description : null,
        policyViolationDetailsLink);
  }

  private static String buildPolicyViolationDetailsLink(
      final String iqBaseUrl,
      final String policyViolationId,
      final SourceControlProvider provider)
  {
    final String reportPath = getPolicyViolationReportPath(policyViolationId);
    final String url  = trimTrailingSlash(iqBaseUrl) + "/" + trimTrailingSlash(reportPath);
    return maybeAppendUTMSourceParam(url, provider);
  }

  private static SeverityInfo buildSeverityInfo(final SecurityVulnerabilityData data) {
    return new SeverityInfo(data.identifier, resolveCvssScore(data).orElse(null), resolveVerificationImage(data));
  }

  private static Optional<Float> resolveCvssScore(final SecurityVulnerabilityData securityVulnerabilityData) {
    final Optional<Float> customSeverityResult = resolveCustomSeverityScore(securityVulnerabilityData);
    if (customSeverityResult.isPresent()) {
      return customSeverityResult;
    }
    return resolveMainSeverityScore(securityVulnerabilityData);
  }

  private static Optional<Float> resolveMainSeverityScore(final SecurityVulnerabilityData securityVulnerabilityData) {
    return nonNull(securityVulnerabilityData.mainSeverity) && securityVulnerabilityData.mainSeverity.score > 0
        ? Optional.of(securityVulnerabilityData.mainSeverity.score) : Optional.empty();
  }

  private static Optional<Float> resolveCustomSeverityScore(final SecurityVulnerabilityData securityVulnerabilityData) {
    return nonNull(securityVulnerabilityData.customData)
        && nonNull(securityVulnerabilityData.customData.cvssSeverity)
        && securityVulnerabilityData.customData.cvssSeverity > 0
        ? Optional.of(securityVulnerabilityData.customData.cvssSeverity) : Optional.empty();
  }

  private static MDImages resolveVerificationImage(final SecurityVulnerabilityData securityVulnerabilityData) {
    if (nonNull(securityVulnerabilityData) && nonNull(securityVulnerabilityData.researchType)) {
      switch (securityVulnerabilityData.researchType) {
        case DEEP_DIVE:
          return SONATYPE_DEEP_DIVE_TAG;
        case FAST_TRACK:
          return SONATYPE_FAST_TRACK_TAG;
        default:
          return null;
      }
    }
    return null;
  }

  private static Optional<ComponentIdentifier> resolveComponentIdentifier(
      List<PolicyViolation> policyViolationsOfAComponent)
  {
    return policyViolationsOfAComponent.stream().findFirst().map(HasComponentId::getComponentIdentifier);
  }
}
