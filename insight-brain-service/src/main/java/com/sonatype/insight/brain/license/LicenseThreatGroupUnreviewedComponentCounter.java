/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.license;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.api.v2.dto.legal.LicenseObligationReviewStatus;
import com.sonatype.insight.brain.api.v2.service.legal.LegalDashboardsService;
import com.sonatype.insight.brain.dataaccess.legal.ComponentObligationDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupDAO;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.legal.ComponentObligation;
import com.sonatype.insight.brain.model.license.LicenseThreatGroup;
import com.sonatype.insight.brain.model.license.LicenseThreatGroupComponentCandidate;
import com.sonatype.insight.brain.model.license.LicenseThreatGroupCount;
import com.sonatype.insight.dataaccess.TransactionContext;

/**
 * Aggregates per-license-threat-group counts of components whose obligation review is {@code UNREVIEWED},
 * using the same review semantics as the classic Legal dashboard.
 *
 * @since 1.204
 */
@Named
@Singleton
public class LicenseThreatGroupUnreviewedComponentCounter
{
  private final LicenseThreatGroupDAO licenseThreatGroupDAO;

  private final LegalDashboardsService legalDashboardsService;

  @Inject
  public LicenseThreatGroupUnreviewedComponentCounter(
      final LicenseThreatGroupDAO licenseThreatGroupDAO,
      final LegalDashboardsService legalDashboardsService)
  {
    this.licenseThreatGroupDAO = licenseThreatGroupDAO;
    this.legalDashboardsService = legalDashboardsService;
  }

  /**
   * Counts unreviewed components per LTG for the given owner, participating in the caller-provided
   * {@code tx}. The caller owns the transaction boundary: this aggregation must not open a connection of
   * its own, otherwise a caller that already holds a transaction (and therefore a pooled connection) would
   * force this method to acquire a second connection — a deadlock risk under connection-pool exhaustion.
   */
  public List<LicenseThreatGroupCount> countByOwner(
      final TransactionContext tx,
      final OwnerType ownerType,
      final String ownerId)
  {
    // Single LEFT JOIN: getCandidatesWithObligationsByOwner folds the obligation lookup into the candidate join graph
    // (one query, one round trip), eliminating the row-value (format, coords) IN-list that overflowed the PostgreSQL
    // parser at large customers (CLM-41470). The DAO de-dupes the LEFT JOIN fanout before returning.
    LicenseThreatGroupDAO.CandidateComponentObligations candidateObligations =
        licenseThreatGroupDAO.getCandidatesWithObligationsByOwner(tx, ownerType, ownerId);
    List<LicenseThreatGroup> visibleLtgs =
        licenseThreatGroupDAO.listVisibleLicenseThreatGroupsForOwner(tx, ownerId);
    return aggregate(candidateObligations.candidates(), visibleLtgs, true,
        candidateObligations.obligationsByComponent());
  }

  /**
   * Counts unreviewed components per LTG across the supplied applications, participating in the
   * caller-provided {@code tx}. See {@link #countByOwner} for why the transaction is supplied by the caller
   * rather than opened here.
   */
  public List<LicenseThreatGroupCount> countByApplicationIds(
      final TransactionContext tx,
      final Collection<String> applicationIds)
  {
    if (applicationIds == null || applicationIds.isEmpty()) {
      return Collections.emptyList();
    }
    LicenseThreatGroupDAO.CandidateComponentObligations candidateObligations =
        licenseThreatGroupDAO.getCandidatesWithObligationsByApplicationIds(tx, applicationIds);
    return aggregate(candidateObligations.candidates(), List.of(), false,
        candidateObligations.obligationsByComponent());
  }

  private List<LicenseThreatGroupCount> aggregate(
      final List<LicenseThreatGroupComponentCandidate> candidates,
      final List<LicenseThreatGroup> zeroFillLtgs,
      final boolean includeZeroCountVisibleLtgs,
      final Map<ComponentIdentifier, List<ComponentObligation>> obligationsByComponent)
  {
    Map<String, String> ltgNameById = new HashMap<>();
    Map<String, Integer> ltgThreatLevelById = new HashMap<>();
    Map<ComponentKey, Set<String>> licenseIdsByComponent = new HashMap<>();
    Map<ComponentKey, Set<String>> ltgIdsByComponent = new HashMap<>();
    Map<ComponentKey, ComponentIdentifier> componentIdentifierByKey = new HashMap<>();

    for (LicenseThreatGroupComponentCandidate candidate : candidates) {
      ltgNameById.put(candidate.getLicenseThreatGroupId(), candidate.getLicenseThreatGroupName());
      ltgThreatLevelById.put(candidate.getLicenseThreatGroupId(), candidate.getThreatLevel());

      ComponentKey key = ComponentKey.from(candidate);
      componentIdentifierByKey.put(key, candidate.getComponentIdentifier());
      licenseIdsByComponent.computeIfAbsent(key, ignored -> new HashSet<>())
          .add(candidate.getEffectiveLicenseId());
      ltgIdsByComponent.computeIfAbsent(key, ignored -> new HashSet<>())
          .add(candidate.getLicenseThreatGroupId());
    }

    for (LicenseThreatGroup ltg : zeroFillLtgs) {
      ltgNameById.putIfAbsent(ltg.getId(), ltg.getName());
      ltgThreatLevelById.putIfAbsent(ltg.getId(), ltg.getThreatLevel());
    }

    Set<String> allLicenseIds = licenseIdsByComponent.values()
        .stream()
        .flatMap(Set::stream)
        .collect(Collectors.toSet());
    Map<String, Set<String>> obligationNamesByLicenseId =
        legalDashboardsService.getLicenseObligationsFromHds(allLicenseIds);

    // Root-org obligation scope matches ApiLicenseLegalService.fillReviewProgress and classic Legal tile semantics.
    // The single-element owner list is what resolveObligationsForOwnerOrder below precedence-resolves against; the
    // obligations themselves were already fetched at ROOT by the candidate/obligation LEFT JOIN (see
    // LicenseThreatGroupDAO.getCandidatesWithObligationsBy*), so no component identifiers are shipped back to the
    // database here (CLM-41470).
    List<String> obligationOwnerIds = List.of(Organization.ROOT_ORGANIZATION_ID);

    Map<String, Long> unreviewedCountByLtgId = new HashMap<>();
    for (Map.Entry<ComponentKey, Set<String>> entry : licenseIdsByComponent.entrySet()) {
      ComponentKey key = entry.getKey();
      Set<String> licenseIds = entry.getValue();
      ComponentIdentifier componentIdentifier = componentIdentifierByKey.get(key);

      Set<String> allObligationNames = licenseIds.stream()
          .filter(obligationNamesByLicenseId::containsKey)
          .flatMap(licenseId -> obligationNamesByLicenseId.get(licenseId).stream())
          .collect(Collectors.toSet());

      List<ComponentObligation> componentObligations = ComponentObligationDAO.resolveObligationsForOwnerOrder(
          obligationOwnerIds,
          obligationsByComponent.getOrDefault(componentIdentifier, List.of()),
          allObligationNames);

      Map<String, Integer> obligationCounts =
          legalDashboardsService.countObligations(componentObligations, allObligationNames);
      LicenseObligationReviewStatus reviewStatus = legalDashboardsService.getReviewStatus(
          obligationCounts.get(LegalDashboardsService.FLAGGEDCOUNT),
          obligationCounts.get(LegalDashboardsService.OPENCOUNT),
          obligationCounts.get(LegalDashboardsService.ADDRESSEDCOUNT),
          allObligationNames,
          licenseIds);

      if (reviewStatus != LicenseObligationReviewStatus.UNREVIEWED) {
        continue;
      }

      for (String ltgId : ltgIdsByComponent.getOrDefault(key, Set.of())) {
        unreviewedCountByLtgId.merge(ltgId, 1L, Long::sum);
      }
    }

    Set<String> ltgIdsToReturn = new HashSet<>(ltgNameById.keySet());
    if (!includeZeroCountVisibleLtgs) {
      ltgIdsToReturn.retainAll(unreviewedCountByLtgId.keySet());
    }

    List<LicenseThreatGroupCount> result = new ArrayList<>(ltgIdsToReturn.size());
    for (String ltgId : ltgIdsToReturn) {
      result.add(new LicenseThreatGroupCount(
          ltgId,
          ltgNameById.get(ltgId),
          ltgThreatLevelById.getOrDefault(ltgId, 0),
          unreviewedCountByLtgId.getOrDefault(ltgId, 0L)));
    }
    sortCounts(result);
    return result;
  }

  private static void sortCounts(final List<LicenseThreatGroupCount> result) {
    result.sort(Comparator.comparingInt(LicenseThreatGroupCount::getThreatLevel)
        .reversed()
        .thenComparing(Comparator.comparingLong(LicenseThreatGroupCount::getUnreviewedComponentCount).reversed())
        .thenComparing(LicenseThreatGroupCount::getLicenseThreatGroupName,
            Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));
  }

  private record ComponentKey(String applicationId, String hash, String format, String coordinatesJson)
  {
    private static ComponentKey from(final LicenseThreatGroupComponentCandidate candidate) {
      return new ComponentKey(
          candidate.getApplicationId(),
          candidate.getHash(),
          candidate.getComponentIdFormat(),
          candidate.getComponentIdCoordinatesJson());
    }
  }
}
