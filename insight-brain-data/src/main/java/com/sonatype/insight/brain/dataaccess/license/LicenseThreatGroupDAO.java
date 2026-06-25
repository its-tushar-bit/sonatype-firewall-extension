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
import java.util.HashSet;
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
import com.sonatype.insight.brain.model.legal.ComponentObligation;
import com.sonatype.insight.brain.model.license.LicenseThreatGroup;
import com.sonatype.insight.brain.model.license.LicenseThreatGroupComponentCandidate;
import com.sonatype.insight.brain.model.license.LicenseThreatGroupLicense;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.dataaccess.TransactionContext;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import org.apache.commons.collections4.CollectionUtils;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.SelectFieldOrAsterisk;
import org.jooq.Table;

import static com.sonatype.insight.brain.jooq.generated.ods.tables.ApplicationAncestor.APPLICATION_ANCESTOR;
import static com.sonatype.insight.brain.jooq.generated.ods.tables.ApplicationComponent.APPLICATION_COMPONENT;
import static com.sonatype.insight.brain.jooq.generated.ods.tables.ApplicationComponentLicense.APPLICATION_COMPONENT_LICENSE;
import static com.sonatype.insight.brain.jooq.generated.ods.tables.ComponentObligation.COMPONENT_OBLIGATION;
import static com.sonatype.insight.brain.jooq.generated.ods.tables.LicenseThreatGroup.LICENSE_THREAT_GROUP;
import static com.sonatype.insight.brain.jooq.generated.ods.tables.LicenseThreatGroupLicense.LICENSE_THREAT_GROUP_LICENSE;
import static com.sonatype.insight.brain.jooq.generated.ods.tables.OwnerAncestor.OWNER_ANCESTOR;

@Named
@Singleton
public class LicenseThreatGroupDAO
    extends AbstractOperationalSqlDAO<LicenseThreatGroup>
{
  /**
   * Projection for the single-query candidate + obligation join (see
   * {@link #getCandidatesWithObligationsByOwner}): the candidate columns the counter aggregates on, plus every
   * {@code component_obligation} column so a {@link ComponentObligation} can be rebuilt from the {@code LEFT JOIN} row
   * via jOOQ's own {@code into(...)} auto-mapping (the same path {@code ComponentObligationDAO} uses), which keeps the
   * mapping in lock-step with the entity if a column is later added to the table. The obligation columns are nullable
   * — a {@code LEFT JOIN} miss (an unreviewed component) leaves them {@code null}.
   */
  private static final List<Field<?>> CANDIDATE_OBLIGATION_FIELDS = candidateObligationFields();

  private static List<Field<?>> candidateObligationFields() {
    List<Field<?>> fields = new ArrayList<>(List.of(
        LICENSE_THREAT_GROUP.LICENSE_THREAT_GROUP_ID,
        LICENSE_THREAT_GROUP.NAME,
        LICENSE_THREAT_GROUP.THREAT_LEVEL,
        APPLICATION_COMPONENT.APPLICATION_ID,
        APPLICATION_COMPONENT.HASH,
        APPLICATION_COMPONENT.COMPONENT_ID_FORMAT,
        APPLICATION_COMPONENT.COMPONENT_ID_COORDINATES_JSON,
        APPLICATION_COMPONENT_LICENSE.EFFECTIVE_LICENSE_ID));
    Collections.addAll(fields, COMPONENT_OBLIGATION.fields());
    return List.copyOf(fields);
  }

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
   * Single-query data source for {@code LicenseThreatGroupUnreviewedComponentCounter}: returns every license-threat-
   * group candidate component in the given owner's scope together with the component obligations recorded at the
   * {@linkplain Organization#ROOT_ORGANIZATION_ID root organization} (the only scope obligations are stored under), in
   * one round trip.
   * <p>
   * The obligation lookup is folded into the candidate join graph as a {@code LEFT JOIN component_obligation} keyed on
   * {@code (component_id_format, component_id_coordinates_json)} with {@code owner_id} pinned in the {@code ON} clause.
   * This replaces the previous design where the candidate identifiers were materialized in Java and shipped back to
   * the database in a row-value {@code (format, coords) IN ((?,?), ...)} list, which overflowed PostgreSQL's parser
   * recursion stack at large customers (CLM-41470). No component identifiers are sent back to the database, and no
   * IN-list is built, so the parser-depth limit is never approached at any scale.
   * <p>
   * The join is {@code LEFT} (not inner) and the {@code owner_id} predicate lives in {@code ON} (not {@code WHERE}) so
   * that candidate components with no recorded obligation — the {@code UNREVIEWED} case the dashboard tile counts —
   * are still returned. Because a component can map to several {@code (LTG, license)} pairs and carry several
   * obligations the raw result fans out; {@link #accumulateCandidateObligations} de-dupes candidates and obligation
   * rows back to the distinct sets the caller expects.
   * <p>
   * Obligation names are intentionally not filtered in SQL: the caller re-filters per component in
   * {@link com.sonatype.insight.brain.dataaccess.legal.ComponentObligationDAO#resolveObligationsForOwnerOrder}, so
   * fetching all root-organization obligations for the in-scope components yields an identical result while keeping
   * this query free of any per-name bind-parameter list.
   *
   * @since 1.205
   */
  public CandidateComponentObligations getCandidatesWithObligationsByOwner(
      final TransactionContext tx,
      final OwnerType ownerType,
      final String ownerId)
  {
    var hierarchyOwnerIdsSubquery = tx.dsl()
        .select(OWNER_ANCESTOR.ANCESTOR_ID)
        .from(OWNER_ANCESTOR)
        .where(OWNER_ANCESTOR.OWNER_ID.eq(ownerId));

    List<Record> rows = tx.dsl()
        .select(CANDIDATE_OBLIGATION_FIELDS)
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
        .leftJoin(COMPONENT_OBLIGATION)
        .on(COMPONENT_OBLIGATION.COMPONENT_ID_FORMAT.eq(APPLICATION_COMPONENT.COMPONENT_ID_FORMAT)
            .and(COMPONENT_OBLIGATION.COMPONENT_ID_COORDINATES_JSON
                .eq(APPLICATION_COMPONENT.COMPONENT_ID_COORDINATES_JSON))
            .and(COMPONENT_OBLIGATION.OWNER_ID.eq(Organization.ROOT_ORGANIZATION_ID)))
        .where(LICENSE_THREAT_GROUP.OWNER_ID.in(hierarchyOwnerIdsSubquery))
        .and(APPLICATION_COMPONENT.HASH.isNotNull())
        .fetch();

    return accumulateCandidateObligations(rows);
  }

  /**
   * @see #getCandidatesWithObligationsByOwner(TransactionContext, OwnerType, String)
   *
   * @since 1.205
   */
  public CandidateComponentObligations getCandidatesWithObligationsByOwner(
      final OwnerType ownerType,
      final String ownerId)
  {
    try (TransactionContext tx = createTransactionContext()) {
      return getCandidatesWithObligationsByOwner(tx, ownerType, ownerId);
    }
  }

  /**
   * Application-scoped counterpart to
   * {@link #getCandidatesWithObligationsByOwner(TransactionContext, OwnerType, String)}. Chunks
   * {@code applicationIds} into single-column {@code IN} lists (never subject to the row-value parser-depth limit) and
   * accumulates the per-chunk rows; {@link #accumulateCandidateObligations} de-dupes across chunk boundaries.
   * <p>
   * Unlike the owner-scoped method this applies no {@code LICENSE_THREAT_GROUP.OWNER_ID} hierarchy filter: callers
   * pass an already-authorized scoped-application set and no owner id is available here. Do not "align" the two
   * methods on this point — their scope semantics differ by design. An empty {@code applicationIds} short-circuits.
   *
   * @since 1.205
   */
  public CandidateComponentObligations getCandidatesWithObligationsByApplicationIds(
      final TransactionContext tx,
      final Collection<String> applicationIds)
  {
    if (CollectionUtils.isEmpty(applicationIds)) {
      return new CandidateComponentObligations(Collections.emptyList(), Collections.emptyMap());
    }

    List<Record> rows = getListWithSqlInClause(
        new ArrayList<>(applicationIds),
        chunk -> tx.dsl()
            .select(CANDIDATE_OBLIGATION_FIELDS)
            .from(LICENSE_THREAT_GROUP)
            .join(LICENSE_THREAT_GROUP_LICENSE)
            .on(LICENSE_THREAT_GROUP.LICENSE_THREAT_GROUP_ID
                .eq(LICENSE_THREAT_GROUP_LICENSE.LICENSE_THREAT_GROUP_ID))
            .join(APPLICATION_COMPONENT_LICENSE)
            .on(APPLICATION_COMPONENT_LICENSE.EFFECTIVE_LICENSE_ID.eq(LICENSE_THREAT_GROUP_LICENSE.LICENSE_ID))
            .join(APPLICATION_COMPONENT)
            .on(APPLICATION_COMPONENT.APPLICATION_COMPONENT_ID
                .eq(APPLICATION_COMPONENT_LICENSE.APPLICATION_COMPONENT_ID)
                .and(APPLICATION_COMPONENT.APPLICATION_ID.in(chunk)))
            .leftJoin(COMPONENT_OBLIGATION)
            .on(COMPONENT_OBLIGATION.COMPONENT_ID_FORMAT.eq(APPLICATION_COMPONENT.COMPONENT_ID_FORMAT)
                .and(COMPONENT_OBLIGATION.COMPONENT_ID_COORDINATES_JSON
                    .eq(APPLICATION_COMPONENT.COMPONENT_ID_COORDINATES_JSON))
                .and(COMPONENT_OBLIGATION.OWNER_ID.eq(Organization.ROOT_ORGANIZATION_ID)))
            .where(APPLICATION_COMPONENT.HASH.isNotNull())
            .fetch());

    return accumulateCandidateObligations(rows);
  }

  /**
   * @see #getCandidatesWithObligationsByApplicationIds(TransactionContext, Collection)
   *
   * @since 1.205
   */
  public CandidateComponentObligations getCandidatesWithObligationsByApplicationIds(
      final Collection<String> applicationIds)
  {
    try (TransactionContext tx = createTransactionContext()) {
      return getCandidatesWithObligationsByApplicationIds(tx, applicationIds);
    }
  }

  /**
   * De-dupes the fanned-out {@code LEFT JOIN} result of the candidate/obligation query into the distinct candidate
   * list and per-component obligation map the counter expects. Candidates are de-duped on their full natural key and
   * obligations on their primary key, so both cross-{@code (LTG, license)} fanout and cross-chunk repetition collapse.
   */
  private CandidateComponentObligations accumulateCandidateObligations(final List<Record> rows) {
    List<LicenseThreatGroupComponentCandidate> candidates = new ArrayList<>();
    Set<CandidateKey> seenCandidates = new HashSet<>();
    Map<ComponentIdentifier, List<ComponentObligation>> obligationsByComponent = new HashMap<>();
    Set<String> seenObligationIds = new HashSet<>();

    for (Record row : rows) {
      CandidateKey candidateKey = new CandidateKey(
          row.get(LICENSE_THREAT_GROUP.LICENSE_THREAT_GROUP_ID),
          row.get(APPLICATION_COMPONENT.APPLICATION_ID),
          row.get(APPLICATION_COMPONENT.HASH),
          row.get(APPLICATION_COMPONENT.COMPONENT_ID_FORMAT),
          row.get(APPLICATION_COMPONENT.COMPONENT_ID_COORDINATES_JSON),
          row.get(APPLICATION_COMPONENT_LICENSE.EFFECTIVE_LICENSE_ID));
      if (seenCandidates.add(candidateKey)) {
        candidates.add(toComponentCandidate(row));
      }

      String obligationId = row.get(COMPONENT_OBLIGATION.COMPONENT_OBLIGATION_ID);
      if (obligationId != null && seenObligationIds.add(obligationId)) {
        ComponentObligation obligation = toComponentObligation(row);
        ComponentIdentifier identifier = obligation.getComponentIdentifier();
        if (identifier != null) {
          obligationsByComponent.computeIfAbsent(identifier, ignored -> new ArrayList<>()).add(obligation);
        }
      }
    }

    return new CandidateComponentObligations(candidates, obligationsByComponent);
  }

  /**
   * Rebuild the {@link ComponentObligation} from its {@code component_obligation} columns in the joined row. We narrow
   * the joined record to the {@code component_obligation} table first ({@code into(COMPONENT_OBLIGATION)}) — which
   * resolves the {@code component_id_format}/{@code component_id_coordinates_json} column-name collision with
   * {@code application_component} — then defer to jOOQ's auto-mapping, the same path {@code ComponentObligationDAO}
   * uses, so the two stay in sync as the table evolves.
   */
  private ComponentObligation toComponentObligation(final Record row) {
    return row.into(COMPONENT_OBLIGATION).into(ComponentObligation.class);
  }

  /**
   * The candidate components in scope plus the component obligations recorded for them, as returned by a single
   * {@code LEFT JOIN} query. Obligation rows are keyed by {@link ComponentIdentifier}; a candidate with no recorded
   * obligation simply has no entry in {@code obligationsByComponent}.
   *
   * @since 1.205
   */
  public record CandidateComponentObligations(
      List<LicenseThreatGroupComponentCandidate> candidates,
      Map<ComponentIdentifier, List<ComponentObligation>> obligationsByComponent)
  {
  }

  private record CandidateKey(
      String licenseThreatGroupId,
      String applicationId,
      String hash,
      String componentIdFormat,
      String componentIdCoordinatesJson,
      String effectiveLicenseId)
  {
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
