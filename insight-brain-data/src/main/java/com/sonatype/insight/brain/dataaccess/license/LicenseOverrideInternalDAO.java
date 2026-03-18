/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.license;

import java.util.List;
import java.util.SortedMap;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.dataaccess.component.ComponentIdentifierAdapter;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.license.LicenseOverrideInternal;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;

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
    String sQuery = "SELECT entity from LicenseOverrideInternal entity "
        + "WHERE entity.ownerId=?1 and entity.componentIdFormat=?2 and entity.componentIdCoordinatesJson=?3";
    LicenseOverrideInternal licenseOverride = get(tx, sQuery, ownerId, componentIdentifier.getFormat(),
        ComponentIdentifierAdapter.toJson(componentIdentifier.getCoordinates()));
    if (licenseOverride == null && componentIdentifier.isMaven()) {
      // Legacy license overrides for maven components have only G, A and V coordinates and those overrides must be used
      // even if the passed in component identifier has complete maven coordinates (i.e. includes extension and
      // classifier).
      SortedMap<String, String> gavCoordinates = ComponentIdentifierAdapter.toGavOnlyCoordinates(componentIdentifier
          .getCoordinates());
      if (!gavCoordinates.equals(componentIdentifier.getCoordinates())) {
        licenseOverride = get(tx, sQuery, ownerId, componentIdentifier.getFormat(),
            ComponentIdentifierAdapter.toJson(gavCoordinates));
      }
    }

    return licenseOverride;
  }

  public List<LicenseOverrideInternal> getByComponentIdentifier(
      final TransactionContext tx,
      final ComponentIdentifier componentIdentifier)
  {
    String sQuery = "SELECT entity FROM LicenseOverrideInternal entity "
        + "WHERE entity.componentIdFormat=?1 and entity.componentIdCoordinatesJson=?2";

    return getList(tx, sQuery, componentIdentifier.getFormat(),
        ComponentIdentifierAdapter.toJson(componentIdentifier.getCoordinates()));
  }

  public List<LicenseOverrideInternal> getByOwnerId(TransactionContext tx, String ownerId) {
    String sQuery = "SELECT entity FROM LicenseOverrideInternal entity WHERE entity.ownerId=?1";
    return getList(tx, sQuery, ownerId);
  }

  public List<LicenseOverrideInternal> getByOwnerId(String ownerId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByOwnerId(tx, ownerId);
    }
  }

  public int getCountByOwnerId(TransactionContext tx, String ownerId) {
    String sQuery = "SELECT COUNT(entity.id) FROM LicenseOverrideInternal entity WHERE entity.ownerId=?1";
    return getSingle(tx, Number.class, sQuery, ownerId).intValue();
  }

  @Override
  public void insert(TransactionContext tx, LicenseOverrideInternal entity) {
    if (getByOwnerIdAndComponentIdentifier(tx, entity.getOwnerId(), entity.getComponentIdentifier()) != null) {
      throw new BadRequestException("LicenseOverride already exists for this ownerId and component");
    }
    super.insert(tx, entity);
  }
}
