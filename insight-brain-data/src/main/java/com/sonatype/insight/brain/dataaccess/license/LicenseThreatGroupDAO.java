/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.license;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.NameHelper;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.license.LicenseThreatGroup;
import com.sonatype.insight.brain.model.license.LicenseThreatGroupLicense;
import com.sonatype.insight.dataaccess.TransactionContext;

@Named
@Singleton
public class LicenseThreatGroupDAO
    extends AbstractOperationalSqlDAO<LicenseThreatGroup>
{
  private final OwnerDAO ownerDAO;

  private final OrganizationDAO orgDAO;

  private final Provider<LicenseThreatGroupLicenseDAO> licenseThreatGroupLicenseDAOProvider;

  @Inject
  public LicenseThreatGroupDAO(
      final OperationalDataStore operationalDataStore,
      final OwnerDAO ownerDAO,
      final OrganizationDAO orgDAO,
      final Provider<LicenseThreatGroupLicenseDAO> licenseThreatGroupLicenseDAOProvider)
  {
    super(operationalDataStore);
    this.ownerDAO = ownerDAO;
    this.orgDAO = orgDAO;
    this.licenseThreatGroupLicenseDAOProvider = licenseThreatGroupLicenseDAOProvider;
  }

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

  /**
   * Queries the {@link LicenseThreatGroup}s for a given set of license IDs for a given list of owner IDs and its
   * hierarchy.
   *
   * @param tx         Current transaction.
   * @param ownerIds   Owner IDs in which the hierarchical query should start from.
   * @param licenseIds License IDs to check.
   * @return A {@link Map} where the key is each license ID and as the value is the list of {@link LicenseThreatGroup}
   * containing that license ID.
   */
  public Map<String, List<LicenseThreatGroup>> getLicenseIdThreatGroupsByOwnerIdsAndLicenseIds(
      TransactionContext tx,
      List<String> ownerIds,
      Set<String> licenseIds)
  {
    String sQuery = "SELECT licenseThreatGroupLicense.licenseId, licenseThreatGroup" + //
        " FROM LicenseThreatGroup licenseThreatGroup, LicenseThreatGroupLicense licenseThreatGroupLicense" + //
        " WHERE licenseThreatGroup.id=licenseThreatGroupLicense.licenseThreatGroupId" + //
        " AND licenseThreatGroup.ownerId IN (?1) AND licenseThreatGroupLicense.licenseId IN (?2)";

    jakarta.persistence.Query query = tx.createQuery(sQuery);
    query.setParameter(1, ownerIds);
    query.setParameter(2, licenseIds);

    List<Object[]> resultList = query.getResultList();
    Map<String, List<LicenseThreatGroup>> licenseIdAndThreatGroups = new HashMap<>();
    for (Object[] object : resultList) {
      List<LicenseThreatGroup> listFromKey = licenseIdAndThreatGroups
          .computeIfAbsent((String) object[0], licenseId -> new ArrayList<>());
      listFromKey.add((LicenseThreatGroup) object[1]);
    }

    return licenseIdAndThreatGroups;
  }

  public List<LicenseThreatGroup> getByIds(Set<String> licenseThreatGroupIds) {
    String sQuery = "SELECT licenseThreatGroup" + //
        " FROM LicenseThreatGroup licenseThreatGroup" + //
        " WHERE licenseThreatGroup.id IN (?1)";
    return getList(sQuery, licenseThreatGroupIds);
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
    LicenseThreatGroupLicenseDAO licenseThreatGroupLicenseDAO = licenseThreatGroupLicenseDAOProvider.get();
    List<LicenseThreatGroupLicense> licenseThreatGroupLicenses = licenseThreatGroupLicenseDAO
        .getByLicenseThreatGroupId(tx, licenseThreatGroup.getId());
    for (LicenseThreatGroupLicense licenseThreatGroupLicense : licenseThreatGroupLicenses) {
      licenseThreatGroupLicenseDAO.delete(tx, licenseThreatGroupLicense);
    }
    super.delete(tx, licenseThreatGroup);
  }

  /**
   * @since 1.6
   */
  public Integer getLicenseThreatLevelByOwnerAndLicenseIdWithHierarchy(Owner owner, String licenseId) {
    Integer threatLevel = null;
    for (Owner currentOwner : ownerDAO.walkHierarchy(owner)) {
      List<LicenseThreatGroup> licenseThreatGroups = getByOwnerIdAndLicenseId(currentOwner.getId(), licenseId);
      threatLevel = max(threatLevel, licenseThreatGroups);
    }
    return threatLevel;
  }

  /**
   * @since 1.108
   */
  public LicenseThreatGroup getHighestLicenseThreatGroupWithHierarchy(
      TransactionContext tx,
      String ownerId,
      Set<String> licenseIds)
  {
    return getByOwnerIdAndLicenseIdsWithHierarchy(tx, ownerId, licenseIds).stream()
        .sorted(Comparator.comparing(LicenseThreatGroup::getNameLowercaseNoWhitespace))
        .max(Comparator.comparingInt(LicenseThreatGroup::getThreatLevel)).orElse(null);
  }

  /**
   * @since 1.108
   */
  public LicenseThreatGroup getHighestLicenseThreatGroupWithHierarchy(
      String ownerId,
      Set<String> licenseIds)
  {
    try (TransactionContext tx = createTransactionContext()) {
      return getHighestLicenseThreatGroupWithHierarchy(tx, ownerId, licenseIds);
    }
  }

  /**
   * @since 1.6
   */
  private Integer max(Integer threatLevel, List<LicenseThreatGroup> licenseThreatGroups) {
    for (LicenseThreatGroup licenseThreatGroup : licenseThreatGroups) {
      if (threatLevel == null) {
        threatLevel = licenseThreatGroup.getThreatLevel();
      }
      else {
        threatLevel = Math.max(threatLevel, licenseThreatGroup.getThreatLevel());
      }
    }
    return threatLevel;
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
    LicenseThreatGroupLicenseDAO licenseThreatGroupLicenseDAO = licenseThreatGroupLicenseDAOProvider.get();
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

  public List<LicenseThreatGroup> getByOwnerIdAndLicenseIdsWithHierarchy(
      TransactionContext tx,
      String ownerId,
      Set<String> licenseIds)
  {
    List<LicenseThreatGroup> result = new ArrayList<>();

    String sQuery = "SELECT licenseThreatGroup" + //
        " FROM LicenseThreatGroup licenseThreatGroup, LicenseThreatGroupLicense licenseThreatGroupLicense" + //
        " WHERE licenseThreatGroup.id=licenseThreatGroupLicense.licenseThreatGroupId" + //
        " AND licenseThreatGroup.ownerId=?1 AND licenseThreatGroupLicense.licenseId IN (?2)";

    for (Owner currentOwner : ownerDAO.walkHierarchy(tx, ownerId)) {
      result.addAll(getList(tx, sQuery, currentOwner.getId(), licenseIds));
    }

    return result;
  }
}
