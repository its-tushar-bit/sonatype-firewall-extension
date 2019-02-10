/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.license;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.license.LicenseOverride;
import com.sonatype.insight.brain.model.license.LicenseOverrideInternal;
import com.sonatype.insight.brain.model.license.LicenseOverrideLicenseInternal;
import com.sonatype.insight.brain.model.license.LicenseOverrideStatus;
import com.sonatype.insight.dataaccess.TransactionContext;
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

  private final OwnerDAO ownerDAO = new OwnerDAO();

  public LicenseOverride getById(String id) {
    LicenseOverrideInternal licenseOverride = licenseOverrideInternalDAO.getById(id);

    if (licenseOverride == null) {
      return null;
    }

    return new LicenseOverride(licenseOverrideInternalDAO.getById(id), getLicenseIds(id));
  }

  /**
   * Walks the owner hierarchy to find the applied license override if applicable
   *
   * @since 1.17
   */
  public LicenseOverride getAppliedByOwnerIdAndComponentIdentifier(String ownerId,
                                                                   ComponentIdentifier componentIdentifier)
  {
    for (Owner owner : ownerDAO.walkHierarchy(ownerId)) {
      LicenseOverride override = getByOwnerIdAndComponentIdentifier(owner.getId(), componentIdentifier);
      if (override != null) {
        return override;
      }
    }

    return null;
  }

  public LicenseOverride getByOwnerIdAndComponentIdentifier(String ownerId, ComponentIdentifier componentIdentifier) {
    try (TransactionContext tx = licenseOverrideInternalDAO.createTransactionContext()) {
      return getByOwnerIdAndComponentIdentifier(tx, ownerId, componentIdentifier);
    }
  }

  public LicenseOverride getByOwnerIdAndComponentIdentifier(TransactionContext tx,
                                                            String ownerId,
                                                            ComponentIdentifier componentIdentifier)
  {
    LicenseOverrideInternal licenseOverride = licenseOverrideInternalDAO.getByOwnerIdAndComponentIdentifier(tx,
        ownerId, componentIdentifier);

    if (licenseOverride == null) {
      return null;
    }

    return new LicenseOverride(licenseOverride, getLicenseIds(tx, licenseOverride.getId()));
  }

  public List<LicenseOverride> getByComponentIdentifier(final TransactionContext tx,
                                                        final ComponentIdentifier componentIdentifier)
  {
    List<LicenseOverrideInternal> licenseOverrideInternalList = licenseOverrideInternalDAO.getByComponentIdentifier(tx,
        componentIdentifier);
    List<LicenseOverride> licenseOverrideList = new ArrayList<>(licenseOverrideInternalList.size());
    for (LicenseOverrideInternal licenseOverrideInternal : licenseOverrideInternalList) {
      licenseOverrideList.add(new LicenseOverride(licenseOverrideInternal, getLicenseIds(tx,
          licenseOverrideInternal.getId())));
    }

    return licenseOverrideList;
  }

  public List<LicenseOverride> getByOwnerId(TransactionContext tx, String ownerId) {
    List<LicenseOverrideInternal> internalOverrides = licenseOverrideInternalDAO.getByOwnerId(tx, ownerId);
    List<LicenseOverride> overrides = new ArrayList<>();

    for (LicenseOverrideInternal internalOverride : internalOverrides) {
      overrides.add(new LicenseOverride(internalOverride, getLicenseIds(tx, internalOverride.getId())));
    }

    return overrides;
  }

  public List<LicenseOverride> getByOwnerId(String ownerId) {
    try (TransactionContext tx = licenseOverrideInternalDAO.createTransactionContext()) {
      return getByOwnerId(tx, ownerId);
    }
  }

  public void insert(TransactionContext tx, LicenseOverride entity) {
    validate(entity);

    LicenseOverrideInternal internal = toInternal(entity);

    licenseOverrideInternalDAO.insert(tx, internal);

    entity.setId(internal.getId());

    addLicenseOverrideLicenseInternals(tx, entity);
  }

  public void insert(LicenseOverride entity) {
    try (TransactionContext tx = licenseOverrideInternalDAO.createTransactionContext()) {
      tx.begin();
      insert(tx, entity);
      tx.commit();
    }
  }

  public void update(TransactionContext tx, LicenseOverride entity) {
    validate(entity);
    licenseOverrideInternalDAO.update(tx, toInternal(entity));

    // clear existing licenses
    clearLicenseOverrideLicenseInternals(tx, entity.getId());

    // add new ones
    addLicenseOverrideLicenseInternals(tx, entity);
  }

  public void update(LicenseOverride entity) {
    try (TransactionContext tx = licenseOverrideInternalDAO.createTransactionContext()) {
      tx.begin();
      update(tx, entity);
      tx.commit();
    }
  }

  public void delete(TransactionContext tx, LicenseOverride entity) {
    clearLicenseOverrideLicenseInternals(tx, entity.getId());
    licenseOverrideInternalDAO.delete(tx, toInternal(entity));
  }

  public void delete(LicenseOverride entity) {
    try (TransactionContext tx = licenseOverrideInternalDAO.createTransactionContext()) {
      tx.begin();
      delete(tx, entity);
      tx.commit();
    }
  }

  private void validate(LicenseOverride entity) {
    if (entity.getStatus() == LicenseOverrideStatus.OVERRIDDEN
        || entity.getStatus() == LicenseOverrideStatus.SELECTED) {
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

  private void clearLicenseOverrideLicenseInternals(TransactionContext tx, String licenseOverrideId) {
    for (LicenseOverrideLicenseInternal license : licenseOverrideLicenseInternalDAO.getByLicenseOverrideId(tx,
        licenseOverrideId)) {
      licenseOverrideLicenseInternalDAO.delete(tx, license);
    }
  }

  private void addLicenseOverrideLicenseInternals(TransactionContext tx, LicenseOverride entity) {
    for (String licenseId : entity.getLicenseIds()) {
      LicenseOverrideLicenseInternal license = new LicenseOverrideLicenseInternal();
      license.setLicenseOverrideId(entity.getId());
      license.setLicenseId(licenseId);
      licenseOverrideLicenseInternalDAO.insert(tx, license);
    }
  }

  private Set<String> getLicenseIds(TransactionContext tx, String id) {
    List<LicenseOverrideLicenseInternal> licenses = licenseOverrideLicenseInternalDAO.getByLicenseOverrideId(tx, id);

    Set<String> licenseIds = new LinkedHashSet<>();

    for (LicenseOverrideLicenseInternal license : licenses) {
      licenseIds.add(license.getLicenseId());
    }

    return licenseIds;
  }

  private Set<String> getLicenseIds(String id) {
    try (TransactionContext tx = licenseOverrideInternalDAO.createTransactionContext()) {
      return getLicenseIds(tx, id);
    }
  }

  private LicenseOverrideInternal toInternal(LicenseOverride override) {
    LicenseOverrideInternal internal = new LicenseOverrideInternal(override.getOwnerId(),
        override.getComponentIdentifier(), override.getStatus(), override.getComment());
    internal.setId(override.getId());

    return internal;
  }
}
