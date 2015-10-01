/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.license;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.model.NameHelper;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.license.LicenseThreatGroup;
import com.sonatype.insight.brain.model.license.LicenseThreatGroupLicense;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.json.store.JsonUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LicenseThreatGroupDAO
    extends AbstractOperationalSqlDAO<LicenseThreatGroup>
{
  public static final int DEFAULT_LICENSE_THREAT_GROUP_COUNT = 6;

  private static final Logger log = LoggerFactory.getLogger(LicenseThreatGroupDAO.class);

  private static final OwnerDAO ownerDAO = new OwnerDAO();

  private static final OrganizationDAO orgDAO = new OrganizationDAO();

  public List<LicenseThreatGroup> getByOwnerId(TransactionContext tx, String ownerId) {
    String sQuery = "SELECT entity FROM LicenseThreatGroup entity" + //
        " WHERE entity.ownerId=?1" + //
        " ORDER BY entity.name";
    return getList(tx, sQuery, ownerId);
  }

  public List<LicenseThreatGroup> getByOwnerId(String ownerId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByOwnerId(tx, ownerId);
    }
  }

  public List<LicenseThreatGroup> getByOwnerIdAndLicenseId(String ownerId, String licenseId) {
    String sQuery = "SELECT licenseThreatGroup" + //
        " FROM LicenseThreatGroup licenseThreatGroup, LicenseThreatGroupLicense licenseThreatGroupLicense" + //
        " WHERE licenseThreatGroup.id=licenseThreatGroupLicense.licenseThreatGroupId" + //
        " AND licenseThreatGroup.ownerId=?1 AND licenseThreatGroupLicense.licenseId=?2";
    return getList(sQuery, ownerId, licenseId);
  }

  @Override
  protected LicenseThreatGroup getById(TransactionContext tx, String id) {
    String sQuery = "SELECT entity FROM LicenseThreatGroup entity" + //
        " WHERE entity.id=?1";
    return get(tx, sQuery, id);
  }

  LicenseThreatGroup getByIdNotNull(TransactionContext tx, String id) {
    LicenseThreatGroup licenseThreatGroup = getById(tx, id);
    if (licenseThreatGroup == null) {
      throw new NotFoundException("Cannot find a license threat group with ID " + id + ".");
    }
    return licenseThreatGroup;
  }

  public LicenseThreatGroup getByIdNotNull(String id) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByIdNotNull(tx, id);
    }
  }

  public LicenseThreatGroup getByOwnerIdAndName(String ownerId, String name) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByOwnerIdAndName(tx, ownerId, name);
    }
  }

  public LicenseThreatGroup getByOwnerIdAndName(TransactionContext tx, String ownerId, String name) {
    name = NameHelper.normalize(name);
    String sQuery = "SELECT entity FROM LicenseThreatGroup entity" + //
        " WHERE entity.ownerId=?1 AND entity.nameLowercaseNoWhitespace=?2";
    return get(tx, sQuery, ownerId, name);
  }

  @Override
  public void insert(TransactionContext tx, LicenseThreatGroup licenseThreatGroup) {
    validateThreatLevel(licenseThreatGroup.getThreatLevel());

    validateName(tx, licenseThreatGroup);
    if (getByOwnerIdAndName(tx, licenseThreatGroup.getOwnerId(), licenseThreatGroup.getName()) != null) {
      throw new InvalidLicenseThreatGroupException("A license threat group with the same name already exists.");
    }

    super.insert(tx, licenseThreatGroup);
  }

  public LicenseThreatGroup getInheritedByName(final TransactionContext tx,
      final LicenseThreatGroup licenseThreatGroup)
  {
    String name = licenseThreatGroup.getName();
    Owner owner = ownerDAO.getById(tx, licenseThreatGroup.getOwnerId());
    return getInheritedByName(tx, owner.getParentOwnerId(), name);
  }

  private LicenseThreatGroup getInheritedByName(final TransactionContext tx, final String parentId, final String name)
  {
    if (parentId == null) {
      return null; // no parent, we're done
    }

    Organization parentOrganization = orgDAO.getByIdNotNull(tx, parentId);
    LicenseThreatGroup ltg = getByOwnerIdAndName(tx, parentOrganization.getId(), name);
    if (ltg != null) {
      return ltg;
    }
    return getInheritedByName(tx, parentOrganization.getParentOrganizationId(), name);
  }

  private void validateName(TransactionContext tx, LicenseThreatGroup licenseThreatGroup) {
    NameHelper.validate(licenseThreatGroup.getName());

    Owner owner = ownerDAO.getById(tx, licenseThreatGroup.getOwnerId());
    validateNameWithinHierarchyUp(tx, owner.getParentOwnerId(), licenseThreatGroup.getName());
    validateNameWithinHierarchyDown(tx, owner, licenseThreatGroup.getName());
  }

  private void validateNameWithinHierarchyUp(final TransactionContext tx, final String parentId, final String name)
  {
    if (parentId == null) {
      return; // no parent, we're done
    }

    Organization parentOrganization = orgDAO.getByIdNotNull(tx, parentId);
    if (getByOwnerIdAndName(tx, parentOrganization.getId(), name) != null) {
      throw new InvalidLicenseThreatGroupException(
          "A license threat group with the same name already exists for the organization '"
              + parentOrganization.getName() + "'.");
    }
    validateNameWithinHierarchyUp(tx, parentOrganization.getParentOrganizationId(), name);
  }

  private void validateNameWithinHierarchyDown(TransactionContext tx, Owner owner, String name)
  {
    if (!owner.canHaveChildren()) {
      return;
    }
    List<Owner> children = ownerDAO.getChildOwners(tx, owner);
    for (Owner child : children) {
      if (getByOwnerIdAndName(tx, child.getId(), name) != null) {
        throw new InvalidLicenseThreatGroupException(
            "A license threat group with the same name already exists for the " + child.getType()
                + " '" + child.getName() + "'.");
      }
      validateNameWithinHierarchyDown(tx, child, name);
    }
  }

  @Override
  public void update(TransactionContext tx, LicenseThreatGroup licenseThreatGroup) {
    validateThreatLevel(licenseThreatGroup.getThreatLevel());

    validateName(tx, licenseThreatGroup);
    LicenseThreatGroup otherLicenseThreatGroup = getByOwnerIdAndName(tx, licenseThreatGroup.getOwnerId(),
        licenseThreatGroup.getName());
    if (otherLicenseThreatGroup != null && !otherLicenseThreatGroup.getId().equals(licenseThreatGroup.getId())) {
      throw new InvalidLicenseThreatGroupException("A license threat group with the same name already exists.");
    }

    super.update(tx, licenseThreatGroup);
  }

  private void validateThreatLevel(int threatLevel) {
    if (threatLevel < 0 || threatLevel > 10) {
      throw new InvalidLicenseThreatGroupException("The threat level must be a number between 0 and 10.");
    }
  }

  @Override
  public void delete(TransactionContext tx, LicenseThreatGroup licenseThreatGroup) {
    LicenseThreatGroupLicenseDAO licenseThreatGroupLicenseDAO = new LicenseThreatGroupLicenseDAO();
    List<LicenseThreatGroupLicense> licenseThreatGroupLicenses = licenseThreatGroupLicenseDAO
        .getByLicenseThreatGroupId(tx, licenseThreatGroup.getId());
    for (LicenseThreatGroupLicense licenseThreatGroupLicense : licenseThreatGroupLicenses) {
      licenseThreatGroupLicenseDAO.delete(tx, licenseThreatGroupLicense);
    }
    super.delete(tx, licenseThreatGroup);
  }

  public void createDefaultLicenseThreatGroups() {
    List<Organization> organizations = orgDAO.getAll();
    List<LicenseThreatGroup> ltgs = getByOwnerId(Organization.ROOT_ORGANIZATION_ID);
    // If the only org is root org and it has no LTGs then create default LTGs
    if (organizations.size() == 1 && Organization.ROOT_ORGANIZATION_ID.equals(organizations.get(0).getId())
        && ltgs.isEmpty()) {
      // Add the LTGs
      createDefaultGroups(Organization.ROOT_ORGANIZATION_ID);
    }
  }

  public void deleteDefaultLicenseThreatGroups() {
    try (TransactionContext tx = createTransactionContext()) {
      tx.begin();
      List<LicenseThreatGroup> licenseThreatGroups = getByOwnerId(tx, Organization.ROOT_ORGANIZATION_ID);
      for (LicenseThreatGroup licenseThreatGroup : licenseThreatGroups) {
        delete(tx, licenseThreatGroup);
      }
      tx.commit();
    }
  }

  public void createDefaultGroups(final String ownerId) {
    try (TransactionContext tx = createTransactionContext()) {
      tx.begin();
      createDefaultGroups(tx, ownerId);
      tx.commit();
    }
  }

  private void createDefaultGroups(TransactionContext tx, String ownerId) {
    long start = System.currentTimeMillis();

    LicenseThreatGroupDefaults defaults = LicenseThreatGroupDefaults.load();

    Map<String, String> newLtgIdsByOldId = new HashMap<>();
    for (LicenseThreatGroup licenseThreatGroup : defaults.licenseThreatGroups) {
      LicenseThreatGroup ltg = new LicenseThreatGroup(ownerId, licenseThreatGroup.getName(),
          licenseThreatGroup.getThreatLevel());
      insert(tx, ltg);
      newLtgIdsByOldId.put(licenseThreatGroup.getId(), ltg.getId());
    }

    LicenseThreatGroupLicenseDAO licenseThreatGroupLicenseDAO = new LicenseThreatGroupLicenseDAO();
    for (LicenseThreatGroupLicense licenseThreatGroupLicense : defaults.licenseThreatGroupLicenses) {
      String licenseThreatGroupId = newLtgIdsByOldId.get(licenseThreatGroupLicense.getLicenseThreatGroupId());
      LicenseThreatGroupLicense ltgl = new LicenseThreatGroupLicense(ownerId, licenseThreatGroupId,
          licenseThreatGroupLicense.getLicenseId());
      licenseThreatGroupLicenseDAO.insert(tx, ltgl);
    }

    log.debug("Created default license threat groups for owner id {} in {} ms.", ownerId, System.currentTimeMillis()
        - start);
  }

  private static class LicenseThreatGroupDefaults
  {
    public List<LicenseThreatGroup> licenseThreatGroups;

    public List<LicenseThreatGroupLicense> licenseThreatGroupLicenses;

    static LicenseThreatGroupDefaults load() {
      Class<LicenseThreatGroupDefaults> type = LicenseThreatGroupDefaults.class;
      try {
        return JsonUtils.parse(type.getResourceAsStream("/LicenseThreatGroupDefaults.json"), type);
      }
      catch (IOException e) {
        throw new IllegalStateException("Invalid LTG defaults", e);
      }
    }
  }
}
