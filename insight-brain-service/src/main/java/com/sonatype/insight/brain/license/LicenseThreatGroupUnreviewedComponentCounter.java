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
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupDAO;
import com.sonatype.insight.brain.dataaccess.legal.ComponentObligationDAO;
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

  private final ComponentObligationDAO componentObligationDAO;

  private final LegalDashboardsService legalDashboardsService;

  @Inject
  public LicenseThreatGroupUnreviewedComponentCounter(
      final LicenseThreatGroupDAO licenseThreatGroupDAO,
      final ComponentObligationDAO componentObligationDAO,
      final LegalDashboardsService legalDashboardsService)
  {
    this.licenseThreatGroupDAO = licenseThreatGroupDAO;
    this.componentObligationDAO = componentObligationDAO;
    this.legalDashboardsService = legalDashboardsService;
  }

  public List<LicenseThreatGroupCount> countByOwner(final OwnerType ownerType, final String ownerId) {
    try (TransactionContext tx = licenseThreatGroupDAO.createTransactionContext()) {
      List<LicenseThreatGroupComponentCandidate> candidates =
          licenseThreatGroupDAO.listComponentCandidatesByOwner(tx, ownerType, ownerId);
      List<LicenseThreatGroup> visibleLtgs =
          licenseThreatGroupDAO.listVisibleLicenseThreatGroupsForOwner(tx, ownerId);
      return aggregate(tx, candidates, visibleLtgs, true);
    }
  }

  public List<LicenseThreatGroupCount> countByApplicationIds(final Collection<String> applicationIds) {
    if (applicationIds == null || applicationIds.isEmpty()) {
      return Collections.emptyList();
    }
    try (TransactionContext tx = licenseThreatGroupDAO.createTransactionContext()) {
      List<LicenseThreatGroupComponentCandidate> candidates =
          licenseThreatGroupDAO.listComponentCandidatesByApplicationIds(tx, applicationIds);
      return aggregate(tx, candidates, List.of(), false);
    }
  }

  private List<LicenseThreatGroupCount> aggregate(
      final TransactionContext tx,
      final List<LicenseThreatGroupComponentCandidate> candidates,
      final List<LicenseThreatGroup> zeroFillLtgs,
      final boolean includeZeroCountVisibleLtgs)
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

    Map<String, Long> unreviewedCountByLtgId = new HashMap<>();
    for (Map.Entry<ComponentKey, Set<String>> entry : licenseIdsByComponent.entrySet()) {
      ComponentKey key = entry.getKey();
      Set<String> licenseIds = entry.getValue();
      ComponentIdentifier componentIdentifier = componentIdentifierByKey.get(key);

      Set<String> allObligationNames = licenseIds.stream()
          .filter(obligationNamesByLicenseId::containsKey)
          .flatMap(licenseId -> obligationNamesByLicenseId.get(licenseId).stream())
          .collect(Collectors.toSet());

      List<ComponentObligation> componentObligations =
          componentObligationDAO.getByOwnerIdsAndComponentIdentifierAndObligationNames(tx,
              List.of(Organization.ROOT_ORGANIZATION_ID), componentIdentifier, allObligationNames);

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
