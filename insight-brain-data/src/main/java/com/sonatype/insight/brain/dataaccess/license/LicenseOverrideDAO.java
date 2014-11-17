/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
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
import com.sonatype.insight.brain.model.license.LicenseOverride;
import com.sonatype.insight.brain.model.license.LicenseOverrideStatus;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.json.store.JsonUtils;

/**
 * @since 1.6
 */
public class LicenseOverrideDAO
    extends AbstractOperationalSqlDAO<LicenseOverride>
{
  public static final int MAX_COMMENT_SIZE = 1000;

  @Override
  protected LicenseOverride getById(EntityManager em, String id) {
    String sQuery = "SELECT entity FROM LicenseOverride entity" + //
        " WHERE entity.id=?1";
    return get(em, sQuery, id);
  }

  public LicenseOverride getByOwnerIdAndComponentIdentifier(String ownerId, ComponentIdentifier componentIdentifier) {
    String sQuery = "SELECT entity from LicenseOverride entity " +
      "WHERE entity.ownerId=?1 and entity.componentIdFormat=?2 and entity.componentIdCoordinatesJson=?3";
    LicenseOverride licenseOverride = get(sQuery, ownerId, componentIdentifier.getFormat(),
        JsonUtils.writeValueAsString(componentIdentifier.getCoordinates()));
    if (licenseOverride == null && componentIdentifier.isMaven()) {
      // Legacy license overrides for maven components have only G, A and V coordinates and those overrides must be used
      // even if the passed in component identifier has complete maven coordinates (i.e. includes extension and
      // classifier).
      SortedMap<String, String> gavCoordinates = ComponentIdentifierAdapter
          .toGavOnlyCoordinates(componentIdentifier.getCoordinates());
      if (!gavCoordinates.equals(componentIdentifier.getCoordinates())) {
        licenseOverride = get(sQuery, ownerId, componentIdentifier.getFormat(),
            JsonUtils.writeValueAsString(gavCoordinates));
      }
    }
    return licenseOverride;
  }

  public List<LicenseOverride> getByOwnerId(EntityManager em, String ownerId) {
    String sQuery = "SELECT entity FROM LicenseOverride entity WHERE entity.ownerId=?1";
    return getList(em, sQuery, ownerId);
  }

  public List<LicenseOverride> getByOwnerId(String ownerId) {
    EntityManager em = createEntityManager();
    try {
      return getByOwnerId(em, ownerId);
    }
    finally {
      close(em);
    }
  }

  @Override
  public void insert(EntityManager em, LicenseOverride entity) {
    validate(entity);
    if(getByOwnerIdAndComponentIdentifier(entity.getOwnerId(), entity.getComponentIdentifier()) != null) {
      throw new BadRequestException("LicenseOverride already exists for this ownerId and component");
    }
    super.insert(em, entity);
  }

  @Override
  public void update(EntityManager em, LicenseOverride entity) {
    validate(entity);
    super.update(em, entity);
  }

  private void validate(LicenseOverride entity) {
    if (entity.getStatus() == LicenseOverrideStatus.OVERRIDDEN || entity.getStatus() == LicenseOverrideStatus.SELECTED) {
      if (entity.getLicenseId() == null) {
        throw new BadRequestException("Expected not null license ID for license override.");
      }
      new LicenseDAO().getByIdNotNull(entity.getLicenseId());
    }
    else {
      if (entity.getLicenseId() != null) {
        throw new BadRequestException("Expected null license ID for license override.");
      }
    }

    if (entity.getComment() != null && entity.getComment().length() > MAX_COMMENT_SIZE) {
      throw new BadRequestException("Comment length must not exceed " + MAX_COMMENT_SIZE + " characters.");
    }
  }

  private LicenseOverride getByIdNotNull(EntityManager em, String id) {
    LicenseOverride licenseOverride = getById(em, id);
    if (licenseOverride == null) {
      throw new NotFoundException("Cannot find a license override with ID " + id + ".");
    }
    return licenseOverride;
  }

  public LicenseOverride getByIdNotNull(String id) {
    EntityManager em = createEntityManager();
    try {
      return getByIdNotNull(em, id);
    }
    finally {
      close(em);
    }
  }
}
