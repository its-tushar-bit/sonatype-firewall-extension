/*
 * Copyright (c) 2011-2015 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.license;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.EntityManager;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.model.license.LicenseOverride;
import com.sonatype.insight.brain.model.license.LicenseOverrideInternal;
import com.sonatype.insight.brain.model.license.LicenseOverrideLicenseInternal;
import com.sonatype.insight.brain.model.license.LicenseOverrideStatus;
import com.sonatype.insight.dataaccess.AbstractDAO;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;

/**
 * @since 1.6
 */
public class LicenseOverrideDAO
{
  public static final int MAX_COMMENT_SIZE = 1000;

  private LicenseOverrideInternalDAO licenseOverrideInternalDAO = new LicenseOverrideInternalDAO();

  private LicenseOverrideLicenseInternalDAO licenseOverrideLicenseInternalDAO = new LicenseOverrideLicenseInternalDAO();

  public LicenseOverride getById(String id) {
    LicenseOverrideInternal licenseOverride = licenseOverrideInternalDAO.getById(id);

    if (licenseOverride == null) {
      return null;
    }

    return new LicenseOverride(licenseOverrideInternalDAO.getById(id), getLicenseIds(id));
  }

  public LicenseOverride getByOwnerIdAndComponentIdentifier(String ownerId, ComponentIdentifier componentIdentifier) {
    LicenseOverrideInternal licenseOverride = licenseOverrideInternalDAO.getByOwnerIdAndComponentIdentifier(ownerId,
        componentIdentifier);

    if (licenseOverride == null) {
      return null;
    }

    return new LicenseOverride(licenseOverride, getLicenseIds(licenseOverride.getId()));
  }

  public List<LicenseOverride> getByOwnerId(EntityManager em, String ownerId) {
    List<LicenseOverrideInternal> internalOverrides = licenseOverrideInternalDAO.getByOwnerId(em, ownerId);
    List<LicenseOverride> overrides = new ArrayList<>();

    for (LicenseOverrideInternal internalOverride : internalOverrides) {
      overrides.add(new LicenseOverride(internalOverride, getLicenseIds(em, internalOverride.getId())));
    }

    return overrides;
  }

  public List<LicenseOverride> getByOwnerId(String ownerId) {
    EntityManager em = licenseOverrideInternalDAO.createEntityManager();
    try {
      return getByOwnerId(em, ownerId);
    }
    finally {
      AbstractDAO.close(em);
    }
  }

  public void insert(EntityManager em, LicenseOverride entity) {
    validate(entity);

    LicenseOverrideInternal internal = toInternal(entity);

    licenseOverrideInternalDAO.insert(em, internal);

    entity.setId(internal.getId());

    addLicenseOverrideLicenseInternals(em, entity);
  }

  public void insert(LicenseOverride entity) {
    EntityManager em = licenseOverrideInternalDAO.createEntityManager();
    try {
      em.getTransaction().begin();
      insert(em, entity);
      em.getTransaction().commit();
    }
    finally {
      AbstractDAO.close(em);
    }
  }

  public void update(EntityManager em, LicenseOverride entity) {
    validate(entity);
    licenseOverrideInternalDAO.update(em, toInternal(entity));

    //clear existing licenses
    clearLicenseOverrideLicenseInternals(em, entity.getId());

    //add new ones
    addLicenseOverrideLicenseInternals(em, entity);
  }

  public void update(LicenseOverride entity) {
    EntityManager em = licenseOverrideInternalDAO.createEntityManager();
    try {
      em.getTransaction().begin();
      update(em, entity);
      em.getTransaction().commit();
    }
    finally {
      AbstractDAO.close(em);
    }
  }

  public void delete(EntityManager em, LicenseOverride entity) {
    clearLicenseOverrideLicenseInternals(em, entity.getId());
    licenseOverrideInternalDAO.delete(em, toInternal(entity));
  }

  public void delete(LicenseOverride entity) {
    EntityManager em = licenseOverrideInternalDAO.createEntityManager();
    try {
      em.getTransaction().begin();
      delete(em, entity);
      em.getTransaction().commit();
    }
    finally {
      AbstractDAO.close(em);
    }
  }

  private void validate(LicenseOverride entity) {
    if (entity.getStatus() == LicenseOverrideStatus.OVERRIDDEN ||
        entity.getStatus() == LicenseOverrideStatus.SELECTED) {
      if (entity.getLicenseIds().isEmpty()) {
        throw new BadRequestException("Expected at least one license ID for license override.");
      }
      for (String licenseId : entity.getLicenseIds()) {
        new LicenseDAO().getByIdNotNull(licenseId);
      }
    }
    else {
      if (!entity.getLicenseIds().isEmpty()) {
        throw new BadRequestException("Expected no license IDs for license override.");
      }
    }

    if (entity.getComment() != null && entity.getComment().length() > MAX_COMMENT_SIZE) {
      throw new BadRequestException("Comment length must not exceed " + MAX_COMMENT_SIZE + " characters.");
    }
  }

  public LicenseOverride getByIdNotNull(String id) {
    LicenseOverride licenseOverride = getById(id);
    if (licenseOverride == null) {
      throw new NotFoundException("Cannot find a license override with ID " + id + ".");
    }
    return licenseOverride;
  }

  private void clearLicenseOverrideLicenseInternals(EntityManager em, String licenseOverrideId) {
    for (LicenseOverrideLicenseInternal license : licenseOverrideLicenseInternalDAO
        .getByLicenseOverrideId(em, licenseOverrideId)) {
      licenseOverrideLicenseInternalDAO.delete(em, license);
    }
  }

  private void addLicenseOverrideLicenseInternals(EntityManager em, LicenseOverride entity) {
    for (String licenseId : entity.getLicenseIds()) {
      LicenseOverrideLicenseInternal license = new LicenseOverrideLicenseInternal();
      license.setLicenseOverrideId(entity.getId());
      license.setLicenseId(licenseId);
      licenseOverrideLicenseInternalDAO.insert(em, license);
    }
  }

  private Set<String> getLicenseIds(EntityManager em, String id) {
    List<LicenseOverrideLicenseInternal> licenses = licenseOverrideLicenseInternalDAO.getByLicenseOverrideId(em, id);

    Set<String> licenseIds = new LinkedHashSet<>();

    for (LicenseOverrideLicenseInternal license : licenses) {
      licenseIds.add(license.getLicenseId());
    }

    return licenseIds;
  }

  private Set<String> getLicenseIds(String id) {
    EntityManager em = licenseOverrideInternalDAO.createEntityManager();
    try {
      return getLicenseIds(em, id);
    }
    finally {
      AbstractDAO.close(em);
    }
  }

  private LicenseOverrideInternal toInternal(LicenseOverride override) {
    LicenseOverrideInternal internal = new LicenseOverrideInternal(override.getOwnerId(), override.getComponentIdentifier(), override.getStatus(),
        override.getComment());
    internal.setId(override.getId());

    return internal;
  }
}
