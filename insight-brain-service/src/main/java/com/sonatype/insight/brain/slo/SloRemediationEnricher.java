/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.slo;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.api.v2.dto.ApiDependencyDataDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiReportComponentDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiReportRawDataDTOV2;
import com.sonatype.insight.brain.api.v2.dto.PrioritizedComponent;
import com.sonatype.insight.brain.dataaccess.development.prioritization.DevelopmentPrioritizationComponentInfoDAO;
import com.sonatype.insight.brain.development.prioritization.DevelopmentPrioritiesReportService;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.prioritization.DevelopmentPrioritizationComponentInfo;
import com.sonatype.insight.error.exception.NotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Loads the report-derived dependency type and the bulk-table recommended remediation version for the components
 * referenced by a page of {@link PolicyViolation}s, keyed by synthetic component hash.
 *
 * <p>
 * This is a focused collaborator for {@link SloViolationEnricher}: it owns the "prioritization" concern (report +
 * recommendation lookups) so the base enricher stays small. Both the report and the bulk recommendation table are
 * loaded exactly once per page — never per violation — to avoid N+1 queries.
 * </p>
 */
@Named
public class SloRemediationEnricher
{
  private static final Logger log = LoggerFactory.getLogger(SloRemediationEnricher.class);

  private final DevelopmentPrioritiesReportService developmentPrioritiesReportService;

  private final DevelopmentPrioritizationComponentInfoDAO prioritizationComponentInfoDAO;

  @Inject
  SloRemediationEnricher(
      final DevelopmentPrioritiesReportService developmentPrioritiesReportService,
      final DevelopmentPrioritizationComponentInfoDAO prioritizationComponentInfoDAO)
  {
    this.developmentPrioritiesReportService = developmentPrioritiesReportService;
    this.prioritizationComponentInfoDAO = prioritizationComponentInfoDAO;
  }

  /**
   * @return a map from synthetic component hash to remediation info for every violation component that could be
   *         resolved. Components absent from the map should be treated as {@code Unknown} dependency type with no
   *         recommended version by the caller.
   */
  public Map<String, SloRemediationInfo> loadByComponentHash(
      final Application application,
      // Reserved for the deferred live ApiComponentRemediationService remediation fallback, which needs the stage to
      // compute a suggested version when the bulk table has no entry. Intentionally unused by the primary path.
      final String stageId,
      final String scanId,
      final Collection<PolicyViolation> violations)
  {
    final Set<String> syntheticHashes = violations.stream()
        .map(PolicyViolation::getComponentIdentifier)
        .filter(Objects::nonNull)
        .map(ComponentIdentifier::toSyntheticHash)
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());

    if (syntheticHashes.isEmpty()) {
      return Map.of();
    }

    final Map<String, String> dependencyTypeByHash = loadDependencyTypes(application, scanId, syntheticHashes);
    // development_prioritization_component_info is keyed by the identifier-derived synthetic hash:
    // DevelopmentPrioritizationRemediationService writes componentIdentifier.toSyntheticHash() into
    // component_hash, and DevelopmentPrioritiesService reads it back the same way. We must therefore look it up by
    // toSyntheticHash() (computed above); the violation's raw component hash is a different key that would never
    // match a row in that table.
    final Map<String, DevelopmentPrioritizationComponentInfo> componentInfoByHash =
        prioritizationComponentInfoDAO.getByScanIdAndComponentHashes(scanId, syntheticHashes);

    final Map<String, SloRemediationInfo> result = new HashMap<>();
    for (String hash : syntheticHashes) {
      final String dependencyType =
          dependencyTypeByHash.getOrDefault(hash, PrioritizedComponent.DEPENDENCY_TYPE_UNKNOWN);
      final DevelopmentPrioritizationComponentInfo info = componentInfoByHash.get(hash);
      final String recommendedVersion = info != null ? info.getRemediationVersion() : null;
      result.put(hash, new SloRemediationInfo(dependencyType, recommendedVersion));
    }
    return result;
  }

  private Map<String, String> loadDependencyTypes(
      final Application application,
      final String scanId,
      final Set<String> syntheticHashes)
  {
    final ApiReportRawDataDTOV2 report;
    try {
      report = developmentPrioritiesReportService.getDependencyInformation(application.getPublicId(), scanId);
    }
    catch (final NotFoundException e) {
      // A missing/unreadable report is the one EXPECTED failure here: getDependencyInformation surfaces it as
      // NotFoundException (ReportService when the report is absent, and on IOException). Dependency type is
      // best-effort enrichment, so degrade gracefully to Unknown. All other exceptions propagate so genuine
      // infra/programming errors surface as errors rather than being silently masked.
      log.debug("Could not load dependency information for application {} scan {}: {}",
          application.getPublicId(), scanId, e.getMessage());
      return Map.of();
    }

    final Map<String, String> dependencyTypeByHash = new HashMap<>();
    for (ApiReportComponentDTOV2 component : report.components) {
      if (component.componentIdentifier == null) {
        continue;
      }
      final String hash = component.componentIdentifier.toComponentIdentifier().toSyntheticHash();
      if (hash == null || !syntheticHashes.contains(hash)) {
        continue;
      }
      dependencyTypeByHash.put(hash, ApiDependencyDataDTO.dependencyType(component.dependencyData));
    }
    return dependencyTypeByHash;
  }

  /**
   * Report-derived dependency type and bulk-table recommended remediation version for a single component.
   *
   * @param dependencyType never null; defaults to {@code Unknown} when the component is absent from the report
   * @param recommendedVersion nullable; null when the bulk recommendation table has no entry for the component
   */
  public record SloRemediationInfo(String dependencyType, String recommendedVersion)
  {
  }
}
