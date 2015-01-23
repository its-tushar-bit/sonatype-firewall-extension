/*
 * Copyright (c) 2011-2015 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.license;

import java.util.List;
import java.util.SortedMap;

import javax.persistence.EntityManager;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.dataaccess.component.ComponentIdentifierAdapter;
import com.sonatype.insight.brain.model.license.LicenseOverrideInternal;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;

/**
 * @since 1.13
 */
public class LicenseOverrideInternalDAO
    extends AbstractOperationalSqlDAO<LicenseOverrideInternal>
{
  @Override
  protected LicenseOverrideInternal getById(EntityManager em, String id) {
    String sQuery = "SELECT entity FROM LicenseOverrideInternal entity" + //
        " WHERE entity.id=?1";

    return get(em, sQuery, id);
  }

  public LicenseOverrideInternal getByOwnerIdAndComponentIdentifier(String ownerId,
      ComponentIdentifier componentIdentifier)
  {
    EntityManager em = createEntityManager();
    try {
      return getByOwnerIdAndComponentIdentifier(em, ownerId, componentIdentifier);
    }
    finally {
      close(em);
    }
  }

  public LicenseOverrideInternal getByOwnerIdAndComponentIdentifier(EntityManager em,
      String ownerId, ComponentIdentifier componentIdentifier)
  {
    String sQuery = "SELECT entity from LicenseOverrideInternal entity " +
        "WHERE entity.ownerId=?1 and entity.componentIdFormat=?2 and entity.componentIdCoordinatesJson=?3";
    LicenseOverrideInternal licenseOverride = get(em, sQuery, ownerId, componentIdentifier.getFormat(),
        ComponentIdentifierAdapter.toJson(componentIdentifier.getCoordinates()));
    if (licenseOverride == null && componentIdentifier.isMaven()) {
      // Legacy license overrides for maven components have only G, A and V coordinates and those overrides must be used
      // even if the passed in component identifier has complete maven coordinates (i.e. includes extension and
      // classifier).
      SortedMap<String, String> gavCoordinates = ComponentIdentifierAdapter
          .toGavOnlyCoordinates(componentIdentifier.getCoordinates());
      if (!gavCoordinates.equals(componentIdentifier.getCoordinates())) {
        licenseOverride = get(em, sQuery, ownerId, componentIdentifier.getFormat(),
            ComponentIdentifierAdapter.toJson(gavCoordinates));
      }
    }

    return licenseOverride;
  }

  public List<LicenseOverrideInternal> getByOwnerId(EntityManager em, String ownerId) {
    String sQuery = "SELECT entity FROM LicenseOverrideInternal entity WHERE entity.ownerId=?1";
    return getList(em, sQuery, ownerId);
  }

  public List<LicenseOverrideInternal> getByOwnerId(String ownerId) {
    EntityManager em = createEntityManager();
    try {
      return getByOwnerId(em, ownerId);
    }
    finally {
      close(em);
    }
  }

  @Override
  public void insert(EntityManager em, LicenseOverrideInternal entity) {
    if (getByOwnerIdAndComponentIdentifier(em, entity.getOwnerId(), entity.getComponentIdentifier()) != null) {
      throw new BadRequestException("LicenseOverride already exists for this ownerId and component");
    }
    super.insert(em, entity);
  }

  private LicenseOverrideInternal getByIdNotNull(EntityManager em, String id) {
    LicenseOverrideInternal licenseOverride = getById(em, id);
    if (licenseOverride == null) {
      throw new NotFoundException("Cannot find a license override with ID " + id + ".");
    }
    return licenseOverride;
  }

  public LicenseOverrideInternal getByIdNotNull(String id) {
    EntityManager em = createEntityManager();
    try {
      return getByIdNotNull(em, id);
    }
    finally {
      close(em);
    }
  }
}
