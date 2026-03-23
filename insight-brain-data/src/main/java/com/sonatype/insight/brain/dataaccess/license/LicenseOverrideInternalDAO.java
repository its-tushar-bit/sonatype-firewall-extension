/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.license;

import java.util.List;
import java.util.SortedMap;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
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

  public LicenseOverrideInternal getByOwnerIdAndComponentIdentifier(
      TransactionContext tx,
      String ownerId,
      ComponentIdentifier componentIdentifier)
  {
    LicenseOverrideInternal licenseOverride = this.toEntity(tx.dsl()
        .selectFrom(LICENSE_OVERRIDE)
        .where(LICENSE_OVERRIDE.OWNER_ID.eq(ownerId))
        .and(LICENSE_OVERRIDE.COMPONENT_ID_FORMAT.eq(componentIdentifier.getFormat()))
        .and(LICENSE_OVERRIDE.COMPONENT_ID_COORDINATES_JSON.eq(
            ComponentIdentifierAdapter.toJson(componentIdentifier.getCoordinates())))
        .fetchOne());

    if (licenseOverride == null && componentIdentifier.isMaven()) {
      // Legacy license overrides for maven components have only G, A and V coordinates and those overrides must be used
      // even if the passed in component identifier has complete maven coordinates (i.e. includes extension and
      // classifier).
      SortedMap<String, String> gavCoordinates = ComponentIdentifierAdapter.toGavOnlyCoordinates(componentIdentifier
          .getCoordinates());
      if (!gavCoordinates.equals(componentIdentifier.getCoordinates())) {
        licenseOverride = this.toEntity(tx.dsl()
            .selectFrom(LICENSE_OVERRIDE)
            .where(LICENSE_OVERRIDE.OWNER_ID.eq(ownerId))
            .and(LICENSE_OVERRIDE.COMPONENT_ID_FORMAT.eq(componentIdentifier.getFormat()))
            .and(LICENSE_OVERRIDE.COMPONENT_ID_COORDINATES_JSON.eq(
                ComponentIdentifierAdapter.toJson(gavCoordinates)))
            .fetchOne());
      }
    }

    return licenseOverride;
  }

  public List<LicenseOverrideInternal> getByComponentIdentifier(
      final TransactionContext tx,
      final ComponentIdentifier componentIdentifier)
  {
    return tx.dsl()
        .selectFrom(LICENSE_OVERRIDE)
        .where(LICENSE_OVERRIDE.COMPONENT_ID_FORMAT.eq(componentIdentifier.getFormat()))
        .and(LICENSE_OVERRIDE.COMPONENT_ID_COORDINATES_JSON.eq(
            ComponentIdentifierAdapter.toJson(componentIdentifier.getCoordinates())))
        .fetch(this::toEntity);
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
