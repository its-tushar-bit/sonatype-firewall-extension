/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.license;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.NameHelper;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.license.LicenseThreatGroup;
import com.sonatype.insight.brain.model.license.LicenseThreatGroupComponentCandidate;
import com.sonatype.insight.brain.model.license.LicenseThreatGroupLicense;
import com.sonatype.insight.dataaccess.TransactionContext;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import org.apache.commons.collections4.CollectionUtils;
import org.jooq.Record;
import org.jooq.SelectFieldOrAsterisk;
import org.jooq.Table;

import static com.sonatype.insight.brain.jooq.generated.ods.tables.ApplicationAncestor.APPLICATION_ANCESTOR;
import static com.sonatype.insight.brain.jooq.generated.ods.tables.ApplicationComponent.APPLICATION_COMPONENT;
import static com.sonatype.insight.brain.jooq.generated.ods.tables.ApplicationComponentLicense.APPLICATION_COMPONENT_LICENSE;
import static com.sonatype.insight.brain.jooq.generated.ods.tables.LicenseThreatGroup.LICENSE_THREAT_GROUP;
import static com.sonatype.insight.brain.jooq.generated.ods.tables.LicenseThreatGroupLicense.LICENSE_THREAT_GROUP_LICENSE;
import static com.sonatype.insight.brain.jooq.generated.ods.tables.OwnerAncestor.OWNER_ANCESTOR;

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
    return tx.dsl()
        .selectFrom(LICENSE_THREAT_GROUP)
        .where(LICENSE_THREAT_GROUP.OWNER_ID.eq(ownerId))
        .orderBy(LICENSE_THREAT_GROUP.NAME)
        .fetch(this::toEntity);
  }

  public List<LicenseThreatGroup> getByOwnerId(String ownerId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByOwnerId(tx, ownerId);
    }
  }

  public List<LicenseThreatGroup> getByName(String name) {
    name = NameHelper.normalize(name);
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .selectFrom(LICENSE_THREAT_GROUP)
          .where(LICENSE_THREAT_GROUP.NAME_LOWERCASE_NO_WHITESPACE.eq(name))
          .fetch(this::toEntity);
    }
  }

  public List<LicenseThreatGroup> getByOwnerIdAndLicenseId(String ownerId, String licenseId) {
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .select(LICENSE_THREAT_GROUP.fields())
          .from(LICENSE_THREAT_GROUP)
          .join(LICENSE_THREAT_GROUP_LICENSE)
          .on(LICENSE_THREAT_GROUP.LICENSE_THREAT_GROUP_ID.eq(LICENSE_THREAT_GROUP_LICENSE.LICENSE_THREAT_GROUP_ID))
          .where(LICENSE_THREAT_GROUP.OWNER_ID.eq(ownerId))
          .and(LICENSE_THREAT_GROUP_LICENSE.LICENSE_ID.eq(licenseId))
          .fetch(r -> toEntity(r.into(LICENSE_THREAT_GROUP)));
    }
  }

  /**
   * Queries license threat groups for the given license IDs across the full owner hierarchy.
   * Uses OWNER_ANCESTOR to resolve the hierarchy in a single query.
   */
  public Map<String, List<LicenseThreatGroup>> getLicenseIdThreatGroupsByLicenseIdsWithHierarchy(
      String ownerId,
      Set<String> licenseIds)
  {
    if (CollectionUtils.isEmpty(licenseIds)) {
      return Collections.emptyMap();
    }

    List<Record> rows = getListWithSqlInClause(new ArrayList<>(licenseIds), chunk -> {
      try (TransactionContext tx = createTransactionContext()) {
        List<SelectFieldOrAsterisk> selectFields = new ArrayList<>();
        selectFields.add(LICENSE_THREAT_GROUP_LICENSE.LICENSE_ID);
        selectFields.addAll(Arrays.asList(LICENSE_THREAT_GROUP.fields()));

        return new ArrayList<Record>(tx.dsl()
            .select(selectFields)
            .from(LICENSE_THREAT_GROUP)
            .join(LICENSE_THREAT_GROUP_LICENSE)
            .on(LICENSE_THREAT_GROUP.LICENSE_THREAT_GROUP_ID.eq(LICENSE_THREAT_GROUP_LICENSE.LICENSE_THREAT_GROUP_ID))
            .join(OWNER_ANCESTOR)
            .on(LICENSE_THREAT_GROUP.OWNER_ID.eq(OWNER_ANCESTOR.ANCESTOR_ID))
            .where(OWNER_ANCESTOR.OWNER_ID.eq(ownerId))
            .and(LICENSE_THREAT_GROUP_LICENSE.LICENSE_ID.in(chunk))
            .fetch());
      }
    }, 1, 1); // 1 param per licenseId, 1 extra param for ownerId

    Map<String, List<LicenseThreatGroup>> licenseIdAndThreatGroups = new HashMap<>();
    for (var record : rows) {
      String licenseId = record.get(LICENSE_THREAT_GROUP_LICENSE.LICENSE_ID);
      LicenseThreatGroup ltg = record.into(LICENSE_THREAT_GROUP).into(LicenseThreatGroup.class);
      licenseIdAndThreatGroups.computeIfAbsent(licenseId, k -> new ArrayList<>()).add(ltg);
    }

    return licenseIdAndThreatGroups;
  }

  public Map<String, List<LicenseThreatGroup>> getLicenseIdThreatGroupsByOwnerIdsAndLicenseIds(
      TransactionContext tx,
      List<String> ownerIds,
      Set<String> licenseIds)
  {
    List<SelectFieldOrAsterisk> selectFields = new ArrayList<>();
    selectFields.add(LICENSE_THREAT_GROUP_LICENSE.LICENSE_ID);
    selectFields.addAll(Arrays.asList(LICENSE_THREAT_GROUP.fields()));

    var result = tx.dsl()
        .select(selectFields)
        .from(LICENSE_THREAT_GROUP)
        .join(LICENSE_THREAT_GROUP_LICENSE)
        .on(LICENSE_THREAT_GROUP.LICENSE_THREAT_GROUP_ID.eq(LICENSE_THREAT_GROUP_LICENSE.LICENSE_THREAT_GROUP_ID))
        .where(LICENSE_THREAT_GROUP.OWNER_ID.in(ownerIds))
        .and(LICENSE_THREAT_GROUP_LICENSE.LICENSE_ID.in(licenseIds))
        .fetch();

    Map<String, List<LicenseThreatGroup>> licenseIdAndThreatGroups = new HashMap<>();
    for (var record : result) {
      String licenseId = record.get(LICENSE_THREAT_GROUP_LICENSE.LICENSE_ID);
      LicenseThreatGroup ltg = record.into(LICENSE_THREAT_GROUP).into(LicenseThreatGroup.class);
      licenseIdAndThreatGroups.computeIfAbsent(licenseId, k -> new ArrayList<>()).add(ltg);
    }

    return licenseIdAndThreatGroups;
  }

  public List<LicenseThreatGroup> getByIds(Set<String> licenseThreatGroupIds) {
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .selectFrom(LICENSE_THREAT_GROUP)
          .where(LICENSE_THREAT_GROUP.LICENSE_THREAT_GROUP_ID.in(licenseThreatGroupIds))
          .fetch(this::toEntity);
    }
  }

  public LicenseThreatGroup getByOwnerIdAndName(String ownerId, String name) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByOwnerIdAndName(tx, ownerId, name);
    }
  }

  public LicenseThreatGroup getByOwnerIdAndName(TransactionContext tx, String ownerId, String name) {
    name = NameHelper.normalize(name);
    return toEntity(tx.dsl()
        .selectFrom(LICENSE_THREAT_GROUP)
        .where(LICENSE_THREAT_GROUP.OWNER_ID.eq(ownerId))
        .and(LICENSE_THREAT_GROUP.NAME_LOWERCASE_NO_WHITESPACE.eq(name))
        .fetchOne());
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

  public LicenseThreatGroup getInheritedByName(
      final TransactionContext tx,
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
    String normalizedName = NameHelper.normalize(licenseThreatGroup.getName());
    NameHelper.validate(licenseThreatGroup.getName());

    String ownerId = licenseThreatGroup.getOwnerId();

    LicenseThreatGroup ancestorConflict = findInAncestors(tx, ownerId, normalizedName);
    if (ancestorConflict != null) {
      Organization parentOrg = orgDAO.getByIdNotNull(tx, ancestorConflict.getOwnerId());
      throw new InvalidLicenseThreatGroupException(
          "A license threat group with the same name already exists for the organization '"
              + parentOrg.getName() + "'.");
    }

    LicenseThreatGroup descendantConflict = findInDescendants(tx, ownerId, normalizedName);
    if (descendantConflict != null) {
      Owner childOwner = ownerDAO.getById(tx, descendantConflict.getOwnerId());
      if (childOwner == null) {
        throw new InvalidLicenseThreatGroupException(
            "A license threat group with the same name already exists.");
      }
      throw new InvalidLicenseThreatGroupException(
          "A license threat group with the same name already exists for the " + childOwner.getType() + " '"
              + childOwner.getName() + "'.");
    }
  }

  private LicenseThreatGroup findInAncestors(TransactionContext tx, String ownerId, String normalizedName) {
    return toEntity(tx.dsl()
        .select(LICENSE_THREAT_GROUP.fields())
        .from(LICENSE_THREAT_GROUP)
        .join(OWNER_ANCESTOR)
        .on(OWNER_ANCESTOR.ANCESTOR_ID.eq(LICENSE_THREAT_GROUP.OWNER_ID))
        .where(OWNER_ANCESTOR.OWNER_ID.eq(ownerId))
        .and(OWNER_ANCESTOR.ANCESTOR_DISTANCE.gt(0))
        .and(OWNER_ANCESTOR.ANCESTOR_TYPE.eq(OwnerType.ORGANIZATION.name()))
        .and(LICENSE_THREAT_GROUP.NAME_LOWERCASE_NO_WHITESPACE.eq(normalizedName))
        .limit(1)
        .fetchOne());
  }

  private LicenseThreatGroup findInDescendants(TransactionContext tx, String ownerId, String normalizedName) {
    return toEntity(tx.dsl()
        .select(LICENSE_THREAT_GROUP.fields())
        .from(LICENSE_THREAT_GROUP)
        .join(OWNER_ANCESTOR)
        .on(OWNER_ANCESTOR.OWNER_ID.eq(LICENSE_THREAT_GROUP.OWNER_ID))
        .where(OWNER_ANCESTOR.ANCESTOR_ID.eq(ownerId))
        .and(OWNER_ANCESTOR.ANCESTOR_DISTANCE.gt(0))
        .and(OWNER_ANCESTOR.OWNER_TYPE.in(OwnerType.ORGANIZATION.name(), OwnerType.APPLICATION.name()))
        .and(LICENSE_THREAT_GROUP.NAME_LOWERCASE_NO_WHITESPACE.eq(normalizedName))
        .limit(1)
        .fetchOne());
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
        .max(Comparator.comparingInt(LicenseThreatGroup::getThreatLevel))
        .orElse(null);
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
   * Returns a map of threat levels by (simple) license id for the specified application. The threat levels are
   * determined from the License Threat Groups in the app/org hierarchy.
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
        .getByLicenseThreatGroupIds(threatLevelsByLicenseThreatGroupId.keySet()))
    {
      String licenseId = licenseThreatGroupLicense.getLicenseId();
      threatLevelsByLicenseId.merge(licenseId,
          threatLevelsByLicenseThreatGroupId.get(licenseThreatGroupLicense.getLicenseThreatGroupId()), Math::max);
    }

    return threatLevelsByLicenseId;
  }

  public List<LicenseThreatGroup> getByOwnerIds(Collection<String> ownerIds) {
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .selectFrom(LICENSE_THREAT_GROUP)
          .where(LICENSE_THREAT_GROUP.OWNER_ID.in(ownerIds))
          .fetch(this::toEntity);
    }
  }

  public List<LicenseThreatGroup> getByOwnerIdAndLicenseIdsWithHierarchy(
      TransactionContext tx,
      String ownerId,
      Set<String> licenseIds)
  {
    List<LicenseThreatGroup> result = new ArrayList<>();

    for (Owner currentOwner : ownerDAO.walkHierarchy(tx, ownerId)) {
      var groups = tx.dsl()
          .select(LICENSE_THREAT_GROUP.fields())
          .from(LICENSE_THREAT_GROUP)
          .join(LICENSE_THREAT_GROUP_LICENSE)
          .on(LICENSE_THREAT_GROUP.LICENSE_THREAT_GROUP_ID.eq(LICENSE_THREAT_GROUP_LICENSE.LICENSE_THREAT_GROUP_ID))
          .where(LICENSE_THREAT_GROUP.OWNER_ID.eq(currentOwner.getId()))
          .and(LICENSE_THREAT_GROUP_LICENSE.LICENSE_ID.in(licenseIds))
          .fetch(r -> toEntity(r.into(LICENSE_THREAT_GROUP)));
      result.addAll(groups);
    }

    return result;
  }

  /**
   * Lists license threat groups visible to the given owner via the {@code owner_ancestor} view. Used to surface
   * inherited LTGs with zero matching components on the owner-scoped REST counts path.
   *
   * @since 1.204
   */
  public List<LicenseThreatGroup> listVisibleLicenseThreatGroupsForOwner(
      final TransactionContext tx,
      final String ownerId)
  {
    var hierarchyOwnerIdsSubquery = tx.dsl()
        .select(OWNER_ANCESTOR.ANCESTOR_ID)
        .from(OWNER_ANCESTOR)
        .where(OWNER_ANCESTOR.OWNER_ID.eq(ownerId));

    return tx.dsl()
        .selectFrom(LICENSE_THREAT_GROUP)
        .where(LICENSE_THREAT_GROUP.OWNER_ID.in(hierarchyOwnerIdsSubquery))
        .fetch(this::toEntity);
  }

  /**
   * Lists scoped application components whose effective license maps to a visible license threat group for the
   * given owner. Does not apply obligation review filtering — see
   * {@code LicenseThreatGroupUnreviewedComponentCounter} in the service module.
   *
   * @since 1.204
   */
  public List<LicenseThreatGroupComponentCandidate> listComponentCandidatesByOwner(
      final TransactionContext tx,
      final OwnerType ownerType,
      final String ownerId)
  {
    var hierarchyOwnerIdsSubquery = tx.dsl()
        .select(OWNER_ANCESTOR.ANCESTOR_ID)
        .from(OWNER_ANCESTOR)
        .where(OWNER_ANCESTOR.OWNER_ID.eq(ownerId));

    return tx.dsl()
        .select(
            LICENSE_THREAT_GROUP.LICENSE_THREAT_GROUP_ID,
            LICENSE_THREAT_GROUP.NAME,
            LICENSE_THREAT_GROUP.THREAT_LEVEL,
            APPLICATION_COMPONENT.APPLICATION_ID,
            APPLICATION_COMPONENT.HASH,
            APPLICATION_COMPONENT.COMPONENT_ID_FORMAT,
            APPLICATION_COMPONENT.COMPONENT_ID_COORDINATES_JSON,
            APPLICATION_COMPONENT_LICENSE.EFFECTIVE_LICENSE_ID)
        .from(LICENSE_THREAT_GROUP)
        .join(LICENSE_THREAT_GROUP_LICENSE)
        .on(LICENSE_THREAT_GROUP.LICENSE_THREAT_GROUP_ID
            .eq(LICENSE_THREAT_GROUP_LICENSE.LICENSE_THREAT_GROUP_ID))
        .join(APPLICATION_COMPONENT_LICENSE)
        .on(APPLICATION_COMPONENT_LICENSE.EFFECTIVE_LICENSE_ID.eq(LICENSE_THREAT_GROUP_LICENSE.LICENSE_ID))
        .join(APPLICATION_COMPONENT)
        .on(APPLICATION_COMPONENT.APPLICATION_COMPONENT_ID
            .eq(APPLICATION_COMPONENT_LICENSE.APPLICATION_COMPONENT_ID)
            .and(ownerType == OwnerType.APPLICATION
                ? APPLICATION_COMPONENT.APPLICATION_ID.eq(ownerId)
                : APPLICATION_COMPONENT.APPLICATION_ID.in(tx.dsl()
                    .select(APPLICATION_ANCESTOR.APPLICATION_ID)
                    .from(APPLICATION_ANCESTOR)
                    .where(APPLICATION_ANCESTOR.ANCESTOR_ID.eq(ownerId)))))
        .where(LICENSE_THREAT_GROUP.OWNER_ID.in(hierarchyOwnerIdsSubquery))
        .and(APPLICATION_COMPONENT.HASH.isNotNull())
        .fetch(this::toComponentCandidate);
  }

  /**
   * @see #listComponentCandidatesByOwner(TransactionContext, OwnerType, String)
   *
   * @since 1.204
   */
  public List<LicenseThreatGroupComponentCandidate> listComponentCandidatesByOwner(
      final OwnerType ownerType,
      final String ownerId)
  {
    try (TransactionContext tx = createTransactionContext()) {
      return listComponentCandidatesByOwner(tx, ownerType, ownerId);
    }
  }

  /**
   * Lists application components whose effective license maps to any license threat group across the supplied
   * {@code applicationIds}. The caller passes in the user's already-authorized scoped-application set, so this
   * method does no further authz filtering. An empty {@code applicationIds} short-circuits with an empty list.
   *
   * @since 1.204
   */
  public List<LicenseThreatGroupComponentCandidate> listComponentCandidatesByApplicationIds(
      final TransactionContext tx,
      final Collection<String> applicationIds)
  {
    if (applicationIds == null || applicationIds.isEmpty()) {
      return Collections.emptyList();
    }

    return getListWithSqlInClause(
        new ArrayList<>(applicationIds),
        chunk -> listComponentCandidatesByApplicationIdsChunk(tx, chunk));
  }

  private List<LicenseThreatGroupComponentCandidate> listComponentCandidatesByApplicationIdsChunk(
      final TransactionContext tx,
      final Collection<String> applicationIds)
  {
    return tx.dsl()
        .select(
            LICENSE_THREAT_GROUP.LICENSE_THREAT_GROUP_ID,
            LICENSE_THREAT_GROUP.NAME,
            LICENSE_THREAT_GROUP.THREAT_LEVEL,
            APPLICATION_COMPONENT.APPLICATION_ID,
            APPLICATION_COMPONENT.HASH,
            APPLICATION_COMPONENT.COMPONENT_ID_FORMAT,
            APPLICATION_COMPONENT.COMPONENT_ID_COORDINATES_JSON,
            APPLICATION_COMPONENT_LICENSE.EFFECTIVE_LICENSE_ID)
        .from(LICENSE_THREAT_GROUP)
        .join(LICENSE_THREAT_GROUP_LICENSE)
        .on(LICENSE_THREAT_GROUP.LICENSE_THREAT_GROUP_ID
            .eq(LICENSE_THREAT_GROUP_LICENSE.LICENSE_THREAT_GROUP_ID))
        .join(APPLICATION_COMPONENT_LICENSE)
        .on(APPLICATION_COMPONENT_LICENSE.EFFECTIVE_LICENSE_ID.eq(LICENSE_THREAT_GROUP_LICENSE.LICENSE_ID))
        .join(APPLICATION_COMPONENT)
        .on(APPLICATION_COMPONENT.APPLICATION_COMPONENT_ID
            .eq(APPLICATION_COMPONENT_LICENSE.APPLICATION_COMPONENT_ID)
            .and(APPLICATION_COMPONENT.APPLICATION_ID.in(applicationIds)))
        .where(APPLICATION_COMPONENT.HASH.isNotNull())
        .fetch(this::toComponentCandidate);
  }

  /**
   * @see #listComponentCandidatesByApplicationIds(TransactionContext, Collection)
   *
   * @since 1.204
   */
  public List<LicenseThreatGroupComponentCandidate> listComponentCandidatesByApplicationIds(
      final Collection<String> applicationIds)
  {
    try (TransactionContext tx = createTransactionContext()) {
      return listComponentCandidatesByApplicationIds(tx, applicationIds);
    }
  }

  private LicenseThreatGroupComponentCandidate toComponentCandidate(Record row) {
    return new LicenseThreatGroupComponentCandidate(
        row.get(LICENSE_THREAT_GROUP.LICENSE_THREAT_GROUP_ID),
        row.get(LICENSE_THREAT_GROUP.NAME),
        row.get(LICENSE_THREAT_GROUP.THREAT_LEVEL),
        row.get(APPLICATION_COMPONENT.APPLICATION_ID),
        row.get(APPLICATION_COMPONENT.HASH),
        row.get(APPLICATION_COMPONENT.COMPONENT_ID_FORMAT),
        row.get(APPLICATION_COMPONENT.COMPONENT_ID_COORDINATES_JSON),
        row.get(APPLICATION_COMPONENT_LICENSE.EFFECTIVE_LICENSE_ID));
  }

  @Override
  public Table<?> getJooqTable() {
    return LICENSE_THREAT_GROUP;
  }

  @Override
  public Class<LicenseThreatGroup> getEntityClass() {
    return LicenseThreatGroup.class;
  }
}
