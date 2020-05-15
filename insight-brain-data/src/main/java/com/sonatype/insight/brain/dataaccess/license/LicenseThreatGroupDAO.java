/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.license;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.NameHelper;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.license.LicenseThreatGroup;
import com.sonatype.insight.brain.model.license.LicenseThreatGroupLicense;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.NotFoundException;

public class LicenseThreatGroupDAO
    extends AbstractOperationalSqlDAO<LicenseThreatGroup>
{
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

  public List<LicenseThreatGroup> getByName(String name) {
    name = NameHelper.normalize(name);
    String sQuery = "SELECT entity FROM LicenseThreatGroup entity" + //
        " WHERE entity.nameLowercaseNoWhitespace=?1";
    return getList(sQuery, name);
  }

  public List<LicenseThreatGroup> getByOwnerIdAndLicenseId(String ownerId, String licenseId) {
    String sQuery = "SELECT licenseThreatGroup" + //
        " FROM LicenseThreatGroup licenseThreatGroup, LicenseThreatGroupLicense licenseThreatGroupLicense" + //
        " WHERE licenseThreatGroup.id=licenseThreatGroupLicense.licenseThreatGroupId" + //
        " AND licenseThreatGroup.ownerId=?1 AND licenseThreatGroupLicense.licenseId=?2";
    return getList(sQuery, ownerId, licenseId);
  }

  public List<LicenseThreatGroup> getByIds(Set<String> licenseThreatGroupIds) {
    String sQuery = "SELECT licenseThreatGroup" + //
        " FROM LicenseThreatGroup licenseThreatGroup" + //
        " WHERE licenseThreatGroup.id IN (?1)";
    return getList(sQuery, licenseThreatGroupIds);
  }

  @Override
  public LicenseThreatGroup getById(TransactionContext tx, String id) {
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

  private LicenseThreatGroup getInheritedByName(final TransactionContext tx, final String parentId, final String name) {
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

  private void validateNameWithinHierarchyUp(final TransactionContext tx, final String parentId, final String name) {
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

  private void validateNameWithinHierarchyDown(TransactionContext tx, Owner owner, String name) {
    if (!owner.canHaveChildren()) {
      return;
    }
    List<Owner> children = ownerDAO.getChildOwners(tx, owner);
    for (Owner child : children) {
      if (getByOwnerIdAndName(tx, child.getId(), name) != null) {
        throw new InvalidLicenseThreatGroupException(
            "A license threat group with the same name already exists for the " + child.getType() + " '"
                + child.getName() + "'.");
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

  /**
   * @since 1.35
   */
  public List<LicenseThreatGroup> getAll() {
    String sQuery = "SELECT entity FROM LicenseThreatGroup entity";
    return getList(sQuery);
  }

  /**
   * Returns a map of threat levels by (simple) license id for the specified application.
   * The threat levels are determined from the License Threat Groups in the app/org hierarchy.
   * 
   * @since 1.91
   */
  public Map<String, Integer> getLicenseThreatLevelsByApplication(Application application) {
    Collection<String> ownerIds = ownerDAO.getOwnerIds(application);

    Map<String, Integer> threatLevelsByLicenseThreatGroupId = getByOwnerIds(ownerIds).stream()
        .collect(Collectors.toMap(LicenseThreatGroup::getId, LicenseThreatGroup::getThreatLevel));

    Map<String, Integer> threatLevelsByLicenseId = new HashMap<>();
    LicenseThreatGroupLicenseDAO licenseThreatGroupLicenseDAO = new LicenseThreatGroupLicenseDAO();
    for (LicenseThreatGroupLicense licenseThreatGroupLicense : licenseThreatGroupLicenseDAO
        .getByLicenseThreatGroupIds(threatLevelsByLicenseThreatGroupId.keySet())) {
      String licenseId = licenseThreatGroupLicense.getLicenseId();
      threatLevelsByLicenseId.merge(licenseId,
          threatLevelsByLicenseThreatGroupId.get(licenseThreatGroupLicense.getLicenseThreatGroupId()), Math::max);
    }

    return threatLevelsByLicenseId;
  }

  public List<LicenseThreatGroup> getByOwnerIds(Collection<String> ownerIds) {
    String sQuery = "SELECT entity FROM LicenseThreatGroup entity" + //
        " WHERE entity.ownerId IN (?1)";
    return getList(sQuery, ownerIds);
  }
}
