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
import java.util.stream.Collectors;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

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
@Named
@Singleton
public class LicenseOverrideDAO
{
  public static final int MAX_COMMENT_SIZE = 1000;

  private final LicenseOverrideInternalDAO licenseOverrideInternalDAO;

  private final LicenseOverrideLicenseInternalDAO licenseOverrideLicenseInternalDAO;

  private final OwnerDAO ownerDAO;

  private final LicenseDAO licenseDAO;

  @Inject
  public LicenseOverrideDAO(
      final LicenseOverrideInternalDAO licenseOverrideInternalDAO,
      final LicenseOverrideLicenseInternalDAO licenseOverrideLicenseInternalDAO,
      final OwnerDAO ownerDAO,
      final LicenseDAO licenseDAO)
  {
    this.licenseOverrideInternalDAO = licenseOverrideInternalDAO;
    this.licenseOverrideLicenseInternalDAO = licenseOverrideLicenseInternalDAO;
    this.ownerDAO = ownerDAO;
    this.licenseDAO = licenseDAO;
  }

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
  public LicenseOverride getAppliedByOwnerIdAndComponentIdentifierWithHierarchy(
      Owner owner,
      ComponentIdentifier componentIdentifier)
  {
    try (TransactionContext tx = licenseOverrideInternalDAO.createTransactionContext()) {
      return getAppliedByOwnerIdAndComponentIdentifierWithHierarchy(tx, owner, componentIdentifier);
    }
  }

  public LicenseOverride getAppliedByOwnerIdAndComponentIdentifierWithHierarchy(
      TransactionContext tx,
      Owner owner,
      ComponentIdentifier componentIdentifier)
  {
    for (Owner anOwner : ownerDAO.walkHierarchy(owner)) {
      LicenseOverride override = getByOwnerIdAndComponentIdentifier(tx, anOwner.getId(), componentIdentifier);
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

  public LicenseOverride getByOwnerIdAndComponentIdentifier(
      TransactionContext tx,
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

  public List<LicenseOverride> getByComponentIdentifier(
      final TransactionContext tx,
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
    List<LicenseOverride> overrides = new ArrayList<>();

    List<LicenseOverrideInternal> internalOverrides = licenseOverrideInternalDAO.getByOwnerId(tx, ownerId);
    if (internalOverrides.isEmpty()) {
      return overrides;
    }

    List<LicenseOverrideLicenseInternal> licenseOverrideLicenses =
        licenseOverrideLicenseInternalDAO.getByOwnerId(tx, ownerId);
    Map<String, Set<String>> licenseIdsByLicenseOverrideId = licenseOverrideLicenses.stream()
        .collect(Collectors.groupingBy(LicenseOverrideLicenseInternal::getLicenseOverrideId,
            Collectors.mapping(LicenseOverrideLicenseInternal::getLicenseId, Collectors.toSet())));
    for (LicenseOverrideInternal internalOverride : internalOverrides) {
      overrides.add(new LicenseOverride(internalOverride, licenseIdsByLicenseOverrideId.get(internalOverride.getId())));
    }

    return overrides;
  }

  public List<LicenseOverride> getByOwnerId(String ownerId) {
    try (TransactionContext tx = licenseOverrideInternalDAO.createTransactionContext()) {
      return getByOwnerId(tx, ownerId);
    }
  }

  public int getCountByOwnerId(String ownerId) {
    try (TransactionContext tx = licenseOverrideInternalDAO.createTransactionContext()) {
      return licenseOverrideInternalDAO.getCountByOwnerId(tx, ownerId);
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
        || entity.getStatus() == LicenseOverrideStatus.SELECTED)
    {
      if (entity.getLicenseIds().isEmpty()) {
        throw new BadRequestException("Expected at least one license ID for license override.");
      }
      for (String licenseId : entity.getLicenseIds()) {
        licenseDAO.getByIdNotNull(licenseId);
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
        licenseOverrideId))
    {
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

  private Set<String> getLicenseIds(TransactionContext tx, String licenseOverrideId) {
    List<LicenseOverrideLicenseInternal> licenses =
        licenseOverrideLicenseInternalDAO.getByLicenseOverrideId(tx, licenseOverrideId);

    Set<String> licenseIds = new LinkedHashSet<>();

    for (LicenseOverrideLicenseInternal license : licenses) {
      licenseIds.add(license.getLicenseId());
    }

    return licenseIds;
  }

  private Set<String> getLicenseIds(String licenseOverrideId) {
    try (TransactionContext tx = licenseOverrideInternalDAO.createTransactionContext()) {
      return getLicenseIds(tx, licenseOverrideId);
    }
  }

  private LicenseOverrideInternal toInternal(LicenseOverride override) {
    LicenseOverrideInternal internal = new LicenseOverrideInternal(override.getOwnerId(),
        override.getComponentIdentifier(), override.getStatus(), override.getComment());
    internal.setId(override.getId());

    return internal;
  }

  public List<LicenseOverride> getAll() {
    List<LicenseOverrideInternal> internalOverrides = licenseOverrideInternalDAO.getAll();
    List<LicenseOverride> overrides = new ArrayList<>();

    for (LicenseOverrideInternal internalOverride : internalOverrides) {
      overrides.add(new LicenseOverride(internalOverride, getLicenseIds(internalOverride.getId())));
    }

    return overrides;
  }
}
