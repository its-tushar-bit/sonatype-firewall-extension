/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.license;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;

import org.apache.commons.lang3.StringUtils;
import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.dataaccess.component.ComponentIdentifierAdapter;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.license.LicenseOverrideInternal;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.jooq.Table;

import static com.sonatype.insight.brain.jooq.generated.ods.tables.LicenseOverride.LICENSE_OVERRIDE;

/**
 * @since 1.13
 */
@Named
@Singleton
public class LicenseOverrideInternalDAO
    extends AbstractOperationalSqlDAO<LicenseOverrideInternal>
{
  @Inject
  public LicenseOverrideInternalDAO(OperationalDataStore operationalDataStore) {
    super(operationalDataStore);
  }

  static SortedMap<String, String> normalizeCoordinates(Map<String, String> coordinates) {
    return coordinates.entrySet()
        .stream()
        .filter(e -> StringUtils.isNotBlank(e.getValue()))
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, TreeMap::new));
  }

  /**
   * Returns the candidate coordinate JSON strings to try when looking up license overrides:
   * exact match, normalized (empty values stripped), and Maven GAV-only legacy fallback.
   * Uses a LinkedHashSet to preserve order and deduplicate.
   */
  static List<String> getCandidateCoordinateJsons(ComponentIdentifier componentIdentifier) {
    if (componentIdentifier == null) {
      return List.of();
    }
    Set<String> candidates = new LinkedHashSet<>();
    candidates.add(ComponentIdentifierAdapter.toJson(componentIdentifier.getCoordinates()));
    candidates.add(ComponentIdentifierAdapter.toJson(normalizeCoordinates(componentIdentifier.getCoordinates())));
    if (componentIdentifier.isMaven()) {
      candidates.add(ComponentIdentifierAdapter.toJson(
          ComponentIdentifierAdapter.toGavOnlyCoordinates(componentIdentifier.getCoordinates())));
    }
    return new ArrayList<>(candidates);
  }

  public LicenseOverrideInternal getByOwnerIdAndComponentIdentifier(
      TransactionContext tx,
      String ownerId,
      ComponentIdentifier componentIdentifier)
  {
    if (componentIdentifier == null) {
      return null;
    }
    List<String> candidates = getCandidateCoordinateJsons(componentIdentifier);
    return this.toEntity(tx.dsl()
        .selectFrom(LICENSE_OVERRIDE)
        .where(LICENSE_OVERRIDE.OWNER_ID.eq(ownerId))
        .and(LICENSE_OVERRIDE.COMPONENT_ID_FORMAT.eq(componentIdentifier.getFormat()))
        .and(LICENSE_OVERRIDE.COMPONENT_ID_COORDINATES_JSON.in(candidates))
        .orderBy(LICENSE_OVERRIDE.COMPONENT_ID_COORDINATES_JSON.sortAsc(candidates))
        .limit(1)
        .fetchOne());
  }

  public List<LicenseOverrideInternal> getByComponentIdentifier(
      final TransactionContext tx,
      final ComponentIdentifier componentIdentifier)
  {
    if (componentIdentifier == null) {
      return List.of();
    }
    // Use exact + normalized candidates only (no GAV-only fallback) since this spans all owners
    Set<String> candidates = new LinkedHashSet<>();
    candidates.add(ComponentIdentifierAdapter.toJson(componentIdentifier.getCoordinates()));
    candidates.add(ComponentIdentifierAdapter.toJson(normalizeCoordinates(componentIdentifier.getCoordinates())));
    List<String> candidateList = new ArrayList<>(candidates);
    List<LicenseOverrideInternal> results = tx.dsl()
        .selectFrom(LICENSE_OVERRIDE)
        .where(LICENSE_OVERRIDE.COMPONENT_ID_FORMAT.eq(componentIdentifier.getFormat()))
        .and(LICENSE_OVERRIDE.COMPONENT_ID_COORDINATES_JSON.in(candidateList))
        .orderBy(LICENSE_OVERRIDE.OWNER_ID, LICENSE_OVERRIDE.COMPONENT_ID_COORDINATES_JSON.sortAsc(candidateList))
        .fetch(this::toEntity);
    Set<String> seenOwners = new LinkedHashSet<>();
    return results.stream()
        .filter(r -> seenOwners.add(r.getOwnerId()))
        .collect(Collectors.toList());
  }

  public List<LicenseOverrideInternal> getByOwnerId(TransactionContext tx, String ownerId) {
    return tx.dsl()
        .selectFrom(LICENSE_OVERRIDE)
        .where(LICENSE_OVERRIDE.OWNER_ID.eq(ownerId))
        .fetch(this::toEntity);
  }

  public List<LicenseOverrideInternal> getByOwnerId(String ownerId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByOwnerId(tx, ownerId);
    }
  }

  public int getCountByOwnerId(TransactionContext tx, String ownerId) {
    return tx.dsl().fetchCount(LICENSE_OVERRIDE, LICENSE_OVERRIDE.OWNER_ID.eq(ownerId));
  }

  @Override
  public void insert(TransactionContext tx, LicenseOverrideInternal entity) {
    if (getByOwnerIdAndComponentIdentifier(tx, entity.getOwnerId(), entity.getComponentIdentifier()) != null) {
      throw new BadRequestException("LicenseOverride already exists for this ownerId and component");
    }
    super.insert(tx, entity);
  }

  @Override
  public Table<?> getJooqTable() {
    return LICENSE_OVERRIDE;
  }

  @Override
  public Class<LicenseOverrideInternal> getEntityClass() {
    return LicenseOverrideInternal.class;
  }
}
