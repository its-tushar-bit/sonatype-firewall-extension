/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;

import org.apache.commons.collections4.CollectionUtils;

import com.sonatype.insight.brain.dataaccess.configuration.CallFlowAnalysisConfigDAO;
import com.sonatype.insight.brain.dataaccess.configuration.DataRetentionPolicyDAO;
import com.sonatype.insight.brain.dataaccess.legal.ComponentCopyrightDAO;
import com.sonatype.insight.brain.dataaccess.legal.ComponentLegalFileDAO;
import com.sonatype.insight.brain.dataaccess.legal.ComponentObligationAttributionDAO;
import com.sonatype.insight.brain.dataaccess.legal.ComponentObligationDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseOverrideDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyMonitoringDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverRequestDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryManagerDAO;
import com.sonatype.insight.brain.dataaccess.search.SearchIndexManager;
import com.sonatype.insight.brain.dataaccess.vulnerability.SecurityVulnerabilityOverrideDAO;
import com.sonatype.insight.brain.dataaccess.vulnerability.VulnerabilityCustomCvssSeverityDAO;
import com.sonatype.insight.brain.dataaccess.vulnerability.VulnerabilityCustomCvssVectorDAO;
import com.sonatype.insight.brain.dataaccess.vulnerability.VulnerabilityCustomCweDAO;
import com.sonatype.insight.brain.dataaccess.vulnerability.VulnerabilityCustomRemediationDAO;
import com.sonatype.insight.brain.dataaccess.vulnerability.VulnerabilityGroupDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerImpl;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.configuration.CallFlowAnalysisConfig;
import com.sonatype.insight.brain.model.configuration.DataRetentionPolicy;
import com.sonatype.insight.brain.model.legal.ComponentCopyright;
import com.sonatype.insight.brain.model.legal.ComponentLegalFile;
import com.sonatype.insight.brain.model.legal.ComponentObligation;
import com.sonatype.insight.brain.model.legal.ComponentObligationAttribution;
import com.sonatype.insight.brain.model.license.LicenseOverride;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyMonitoring;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.PolicyWaiverRequest;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.model.security.MemberType;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.vulnerability.SecurityVulnerabilityOverride;
import com.sonatype.insight.brain.model.vulnerability.VulnerabilityCustomCvssSeverity;
import com.sonatype.insight.brain.model.vulnerability.VulnerabilityCustomCvssVector;
import com.sonatype.insight.brain.model.vulnerability.VulnerabilityCustomCwe;
import com.sonatype.insight.brain.model.vulnerability.VulnerabilityCustomRemediation;
import com.sonatype.insight.brain.model.vulnerability.VulnerabilityGroup;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.NotFoundException;

import org.jooq.CommonTableExpression;
import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Record1;
import org.jooq.Table;
import org.jooq.impl.DSL;

import static com.sonatype.insight.brain.jooq.generated.ods.tables.Application.APPLICATION;
import static com.sonatype.insight.brain.jooq.generated.ods.tables.ApplicationTag.APPLICATION_TAG;
import static com.sonatype.insight.brain.jooq.generated.ods.tables.ApplicationAncestor.APPLICATION_ANCESTOR;
import static com.sonatype.insight.brain.jooq.generated.ods.tables.MembershipMapping.MEMBERSHIP_MAPPING;
import static com.sonatype.insight.brain.jooq.generated.ods.tables.RolePermission.ROLE_PERMISSION;
import static com.sonatype.insight.brain.jooq.generated.ods.tables.Organization.ORGANIZATION;
import static com.sonatype.insight.brain.jooq.generated.ods.tables.OrganizationAncestor.ORGANIZATION_ANCESTOR;
import static com.sonatype.insight.brain.jooq.generated.ods.tables.OwnerAncestor.OWNER_ANCESTOR;
import static com.sonatype.insight.brain.jooq.generated.ods.tables.Repository.REPOSITORY;
import static com.sonatype.insight.brain.jooq.generated.ods.tables.RepositoryAncestor.REPOSITORY_ANCESTOR;
import static com.sonatype.insight.brain.jooq.generated.ods.tables.RepositoryContainerAncestor.REPOSITORY_CONTAINER_ANCESTOR;
import static com.sonatype.insight.brain.jooq.generated.ods.tables.RepositoryManagerAncestor.REPOSITORY_MANAGER_ANCESTOR;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;

@Named
@Singleton
public class OwnerDAO
    extends AbstractOperationalSqlDAO<Owner>
{
  private final ApplicationDAO appDAO;

  private final OrganizationDAO orgDAO;

  private final RepositoryDAO repoDAO;

  private final RepositoryManagerDAO repoManagerDAO;

  private final Provider<PolicyWaiverDAO> policyWaiverDAOProvider;

  private final Provider<PolicyWaiverRequestDAO> policyWaiverRequestDAOProvider;

  private final Provider<LicenseOverrideDAO> licenseOverrideDAOProvider;

  private final SecurityVulnerabilityOverrideDAO securityVulnerabilityOverrideDAO;

  private final Provider<PolicyDAO> policyDAOProvider;

  private final DataRetentionPolicyDAO dataRetentionPolicyDAO;

  private final PolicyMonitoringDAO policyMonitoringDAO;

  private final Provider<ComponentCopyrightDAO> componentCopyrightDAOProvider;

  private final Provider<ComponentLegalFileDAO> componentLegalFileDAOProvider;

  private final Provider<ComponentObligationDAO> componentObligationDAOProvider;

  private final Provider<ComponentObligationAttributionDAO> componentObligationAttributionDAOProvider;

  private final Provider<VulnerabilityGroupDAO> vulnerabilityGroupDAOProvider;

  private final Provider<VulnerabilityCustomRemediationDAO> vulnerabilityCustomRemediationDAOProvider;

  private final Provider<VulnerabilityCustomCweDAO> vulnerabilityCustomCweDAOProvider;

  private final Provider<CallFlowAnalysisConfigDAO> callFlowAnalysisConfigDAOProvider;

  private final Provider<VulnerabilityCustomCvssVectorDAO> vulnerabilityCustomCvssVectorDAOProvider;

  private final Provider<VulnerabilityCustomCvssSeverityDAO> vulnerabilityCustomCvssSeverityDAOProvider;

  @Inject
  public OwnerDAO(
      final OperationalDataStore operationalDataStore,
      final SearchIndexManager searchIndexManager,
      final ApplicationDAO appDAO,
      final OrganizationDAO orgDAO,
      final RepositoryDAO repoDAO,
      final RepositoryManagerDAO repoManagerDAO,
      final Provider<PolicyWaiverDAO> policyWaiverDAOProvider,
      final Provider<PolicyWaiverRequestDAO> policyWaiverRequestDAOProvider,
      final Provider<LicenseOverrideDAO> licenseOverrideDAOProvider,
      final SecurityVulnerabilityOverrideDAO securityVulnerabilityOverrideDAO,
      final Provider<PolicyDAO> policyDAOProvider,
      final DataRetentionPolicyDAO dataRetentionPolicyDAO,
      final PolicyMonitoringDAO policyMonitoringDAO,
      final Provider<ComponentCopyrightDAO> componentCopyrightDAOProvider,
      final Provider<ComponentLegalFileDAO> componentLegalFileDAOProvider,
      final Provider<ComponentObligationDAO> componentObligationDAOProvider,
      final Provider<ComponentObligationAttributionDAO> componentObligationAttributionDAOProvider,
      final Provider<VulnerabilityGroupDAO> vulnerabilityGroupDAOProvider,
      final Provider<VulnerabilityCustomRemediationDAO> vulnerabilityCustomRemediationDAOProvider,
      final Provider<VulnerabilityCustomCweDAO> vulnerabilityCustomCweDAOProvider,
      final Provider<VulnerabilityCustomCvssVectorDAO> vulnerabilityCustomCvssVectorDAOProvider,
      final Provider<VulnerabilityCustomCvssSeverityDAO> vulnerabilityCustomCvssSeverityDAOProvider,
      final Provider<CallFlowAnalysisConfigDAO> callFlowAnalysisConfigDAOProvider)
  {
    super(operationalDataStore, searchIndexManager);
    this.appDAO = appDAO;
    this.orgDAO = orgDAO;
    this.repoDAO = repoDAO;
    this.repoManagerDAO = repoManagerDAO;
    this.policyWaiverDAOProvider = policyWaiverDAOProvider;
    this.policyWaiverRequestDAOProvider = policyWaiverRequestDAOProvider;
    this.licenseOverrideDAOProvider = licenseOverrideDAOProvider;
    this.securityVulnerabilityOverrideDAO = securityVulnerabilityOverrideDAO;
    this.policyDAOProvider = policyDAOProvider;
    this.dataRetentionPolicyDAO = dataRetentionPolicyDAO;
    this.policyMonitoringDAO = policyMonitoringDAO;
    this.componentCopyrightDAOProvider = componentCopyrightDAOProvider;
    this.componentLegalFileDAOProvider = componentLegalFileDAOProvider;
    this.componentObligationDAOProvider = componentObligationDAOProvider;
    this.componentObligationAttributionDAOProvider = componentObligationAttributionDAOProvider;
    this.vulnerabilityGroupDAOProvider = vulnerabilityGroupDAOProvider;
    this.vulnerabilityCustomRemediationDAOProvider = vulnerabilityCustomRemediationDAOProvider;
    this.vulnerabilityCustomCweDAOProvider = vulnerabilityCustomCweDAOProvider;
    this.vulnerabilityCustomCvssVectorDAOProvider = vulnerabilityCustomCvssVectorDAOProvider;
    this.vulnerabilityCustomCvssSeverityDAOProvider = vulnerabilityCustomCvssSeverityDAOProvider;
    this.callFlowAnalysisConfigDAOProvider = callFlowAnalysisConfigDAOProvider;
  }

  @Override
  public Owner getById(TransactionContext tx, String id) {
    if (RepositoryContainer.REPOSITORY_CONTAINER_ID.equals(id)) {
      return RepositoryContainer.SINGLETON;
    }

    // Since on any path in the hierarchy there are more orgs than apps or repos, query for org first.
    Organization org = orgDAO.getById(tx, id);
    if (org != null) {
      return org;
    }

    Application app = appDAO.getById(tx, id);
    if (app != null) {
      return app;
    }

    Repository repo = repoDAO.getById(tx, id);
    if (repo != null) {
      return repo;
    }

    return repoManagerDAO.getById(tx, id);
  }

  @Override
  public Owner getById(String id) {
    try (TransactionContext tx = appDAO.createTransactionContext()) {
      return getById(tx, id);
    }
  }

  @Override
  public List<Owner> getByIds(Collection<String> ids) {
    throw new UnsupportedOperationException("OwnerDAO is a composite DAO and does not support batch operations");
  }

  @Override
  public void insertBatch(TransactionContext tx, List<Owner> entities, boolean ignoreDuplicateKey) {
    throw new UnsupportedOperationException("OwnerDAO is a composite DAO and does not support batch operations");
  }

  @Override
  public void updateBatch(TransactionContext tx, List<Owner> entities) {
    throw new UnsupportedOperationException("OwnerDAO is a composite DAO and does not support batch operations");
  }

  @Override
  public Owner getByIdNotNull(String id) {
    Owner owner = getById(id);
    if (owner == null) {
      throw new NotFoundException("Owner with ID " + id + " does not exist.");
    }
    return owner;
  }

  public List<Owner> getChildOwners(final Owner owner) {
    try (TransactionContext tx = appDAO.createTransactionContext()) {
      return getChildOwners(tx, owner);
    }
  }

  public List<Owner> getChildOwners(TransactionContext tx, Owner owner) {
    List<Owner> result = new ArrayList<>();
    if (!owner.canHaveChildren()) {
      return result;
    }

    switch (owner.getType()) {
      case ORGANIZATION:
        List<Application> apps = appDAO.getByOrganizationId(tx, owner.getId());
        result.addAll(apps);
        List<Organization> orgs = orgDAO.getByParentOrganizationId(tx, owner.getId());
        result.addAll(orgs);
        if (Organization.ROOT_ORGANIZATION_ID.equals(owner.getId())) {
          result.add(RepositoryContainer.SINGLETON);
        }
        break;
      case REPOSITORY_CONTAINER:
        result.addAll(repoManagerDAO.getAll(tx));
        break;
      case REPOSITORY_MANAGER:
        result.addAll(repoDAO.getByRepositoryManagerId(tx, owner.getId()));
        break;
      default:
        throw new IllegalStateException("Unhandled owner type: " + owner.getType());
    }

    return result;
  }

  public Set<Application> getDescendantOrSelfApplications(Owner owner) {
    Set<Application> applications = new HashSet<>();
    addDescendantOrSelfApplications(applications, owner);
    return applications;
  }

  private void addDescendantOrSelfApplications(Set<Application> applications, Owner owner) {
    if (OwnerType.APPLICATION.equals(owner.getType())) {
      applications.add((Application) owner);
    }
    else {
      getChildOwners(owner).forEach(childOwner -> addDescendantOrSelfApplications(applications, childOwner));
    }
  }

  public Set<String> getDescendantOrSelfApplicationIds(Owner owner) {
    return getDescendantOrSelfApplications(owner).stream().map(Owner::getId).collect(Collectors.toSet());
  }

  public Owner getParentOwner(Owner owner) {
    return getById(owner.getParentOwnerId());
  }

  /**
   * Batch query using the appropriate ancestor view to get only owner IDs that the user has permission to access.
   * This method uses a single SQL query with a CTE that joins membership_mapping with role_permission to compute
   * user contexts, then filters owner IDs based on whether they have any ancestor in the user's context IDs
   * (including global short-circuit).
   *
   * @param ownerIds the IDs of the owners to check
   * @param ownerType the type of owner to select the appropriate ancestor view (null for generic view)
   * @param permission the permission to check
   * @param username the username to check for user membership
   * @param groupNames the group names to check for group membership
   * @return a Set of owner IDs that the user has permission to access
   */
  public Set<String> getPermittedOwnerIds(
      final List<String> ownerIds,
      final OwnerType ownerType,
      final Permission permission,
      final String username,
      final Set<String> groupNames)
  {
    if (ownerIds == null || ownerIds.isEmpty()) {
      return Collections.emptySet();
    }
    try (TransactionContext tx = createTransactionContext()) {
      if (ownerType == null) {
        return getPermittedOwnerIdsSpecific(tx, ownerIds, OWNER_ANCESTOR,
            OWNER_ANCESTOR.OWNER_ID, OWNER_ANCESTOR.ANCESTOR_ID, permission, username, groupNames);
      }
      else {
        switch (ownerType) {
          case APPLICATION:
            return getPermittedOwnerIdsSpecific(tx, ownerIds, APPLICATION_ANCESTOR,
                APPLICATION_ANCESTOR.APPLICATION_ID, APPLICATION_ANCESTOR.ANCESTOR_ID,
                permission, username, groupNames);
          case ORGANIZATION:
            return getPermittedOwnerIdsSpecific(tx, ownerIds, ORGANIZATION_ANCESTOR,
                ORGANIZATION_ANCESTOR.ORGANIZATION_ID, ORGANIZATION_ANCESTOR.ANCESTOR_ID,
                permission, username, groupNames);
          case REPOSITORY:
            return getPermittedOwnerIdsSpecific(tx, ownerIds, REPOSITORY_ANCESTOR,
                REPOSITORY_ANCESTOR.REPOSITORY_ID, REPOSITORY_ANCESTOR.ANCESTOR_ID,
                permission, username, groupNames);
          case REPOSITORY_MANAGER:
            return getPermittedOwnerIdsSpecific(tx, ownerIds, REPOSITORY_MANAGER_ANCESTOR,
                REPOSITORY_MANAGER_ANCESTOR.REPOSITORY_MANAGER_ID, REPOSITORY_MANAGER_ANCESTOR.ANCESTOR_ID,
                permission, username, groupNames);
          case REPOSITORY_CONTAINER:
            return getPermittedOwnerIdsSpecific(tx, ownerIds, REPOSITORY_CONTAINER_ANCESTOR,
                REPOSITORY_CONTAINER_ANCESTOR.REPOSITORY_CONTAINER_ID, REPOSITORY_CONTAINER_ANCESTOR.ANCESTOR_ID,
                permission, username, groupNames);
          default:
            return getPermittedOwnerIdsSpecific(tx, ownerIds, OWNER_ANCESTOR,
                OWNER_ANCESTOR.OWNER_ID, OWNER_ANCESTOR.ANCESTOR_ID, permission, username, groupNames);
        }
      }
    }
  }

  /**
   * Returns the IDs of all proxy repositories the user has READ permission on, in a single query.
   * Joins REPOSITORY (filtered by repository_type='proxy') with REPOSITORY_ANCESTOR and the
   * user-contexts CTE — avoids a separate pre-fetch of all proxy repo IDs.
   */
  public Set<String> getPermittedProxyRepositoryIds(
      final Permission permission,
      final String username,
      final Set<String> groupNames)
  {
    Set<String> effectiveGroupNames = groupNames != null ? groupNames : Collections.emptySet();

    var userCondition = MEMBERSHIP_MAPPING.MEMBER_TYPE.eq(MemberType.USER.name())
        .and(MEMBERSHIP_MAPPING.MEMBER_NAME.eq(username)
            .or(DSL.lower(MEMBERSHIP_MAPPING.MEMBER_NAME).eq(username.toLowerCase()))
            .or(DSL.upper(MEMBERSHIP_MAPPING.MEMBER_NAME).eq(username.toUpperCase())));
    var groupCondition = MEMBERSHIP_MAPPING.MEMBER_TYPE.eq(MemberType.GROUP.name())
        .and(MEMBERSHIP_MAPPING.MEMBER_NAME.in(effectiveGroupNames));

    try (TransactionContext tx = createTransactionContext()) {
      CommonTableExpression<Record1<String>> cte = DSL.name("user_contexts")
          .fields("context_id")
          .as(tx.dsl()
              .selectDistinct(MEMBERSHIP_MAPPING.CONTEXT_ID)
              .from(MEMBERSHIP_MAPPING)
              .join(ROLE_PERMISSION)
              .on(ROLE_PERMISSION.ROLE_ID.eq(MEMBERSHIP_MAPPING.ROLE_ID))
              .where(ROLE_PERMISSION.PERMISSION.eq(permission.name()))
              .and(userCondition.or(groupCondition)));

      Field<String> cteCtxId = cte.field("context_id", String.class);

      return tx.dsl()
          .with(cte)
          .selectDistinct(REPOSITORY_ANCESTOR.REPOSITORY_ID)
          .from(REPOSITORY_ANCESTOR)
          .join(REPOSITORY)
          .on(REPOSITORY.REPOSITORY_ID.eq(REPOSITORY_ANCESTOR.REPOSITORY_ID))
          .where(REPOSITORY.REPOSITORY_TYPE.eq("proxy"))
          .and(
              (permission.isGlobal()
                  ? DSL.exists(DSL.selectOne().from(cte))
                  : DSL.noCondition())
                      .or(DSL.exists(
                          DSL.selectOne()
                              .from(cte)
                              .where(cteCtxId.eq(MembershipMapping.GLOBAL_CONTEXT_ID))))
                      .or(REPOSITORY_ANCESTOR.ANCESTOR_ID.in(
                          DSL.select(cteCtxId).from(cte))))
          .fetchSet(REPOSITORY_ANCESTOR.REPOSITORY_ID);
    }
  }

  /**
   * Batch query using a specific ancestor view with CTE-based permission resolution. The CTE joins
   * membership_mapping with role_permission to find contexts where the user has the required permission,
   * then the main query filters owner IDs by ancestor intersection with those contexts.
   */
  private <R extends Record> Set<String> getPermittedOwnerIdsSpecific(
      TransactionContext tx,
      List<String> ownerIds,
      Table<R> ancestorTable,
      Field<String> ownerIdColumn,
      Field<String> ancestorIdColumn,
      Permission permission,
      String username,
      Set<String> groupNames)
  {
    Set<String> effectiveGroupNames = groupNames != null ? groupNames : Collections.emptySet();

    // Build membership conditions (same logic as
    // MembershipMappingDAO.getContextIdsByUserCaseInsensitiveAndGroupsAndRoles)
    var userCondition = MEMBERSHIP_MAPPING.MEMBER_TYPE.eq(MemberType.USER.name())
        .and(MEMBERSHIP_MAPPING.MEMBER_NAME.eq(username)
            .or(DSL.lower(MEMBERSHIP_MAPPING.MEMBER_NAME).eq(username.toLowerCase()))
            .or(DSL.upper(MEMBERSHIP_MAPPING.MEMBER_NAME).eq(username.toUpperCase())));
    var groupCondition = MEMBERSHIP_MAPPING.MEMBER_TYPE.eq(MemberType.GROUP.name())
        .and(MEMBERSHIP_MAPPING.MEMBER_NAME.in(effectiveGroupNames));

    // CTE: compute user's context IDs once by joining membership_mapping with role_permission
    CommonTableExpression<Record1<String>> userContextsCte = DSL.name("user_contexts")
        .fields("context_id")
        .as(tx.dsl()
            .selectDistinct(MEMBERSHIP_MAPPING.CONTEXT_ID)
            .from(MEMBERSHIP_MAPPING)
            .join(ROLE_PERMISSION)
            .on(ROLE_PERMISSION.ROLE_ID.eq(MEMBERSHIP_MAPPING.ROLE_ID))
            .where(ROLE_PERMISSION.PERMISSION.eq(permission.name()))
            .and(userCondition.or(groupCondition)));

    Field<String> ctxId = userContextsCte.field("context_id", String.class);

    // Main query: return entity IDs that pass authz, using getStreamWithSqlInClause for IN clause partitioning
    // When permission is global, any user who has the permission in ANY context can access all entities
    // (the CTE will be non-empty if they have it anywhere, and we check for GLOBAL_CONTEXT existence)
    // OR check if the ancestor is in the user's contexts
    try (var stream = getStreamWithSqlInClause(ownerIds, partition -> tx.dsl()
        .with(userContextsCte)
        .selectDistinct(ownerIdColumn)
        .from(ancestorTable)
        .where(ownerIdColumn.in(partition))
        .and(
            (permission.isGlobal()
                ? DSL.exists(DSL.selectOne().from(userContextsCte))
                : DSL.noCondition())
                    .or(DSL.exists(
                        DSL.selectOne()
                            .from(userContextsCte)
                            .where(ctxId.eq(MembershipMapping.GLOBAL_CONTEXT_ID))))
                    .or(ancestorIdColumn.in(
                        DSL.select(ctxId).from(userContextsCte))))
        .fetchStream()))
    {
      return stream.map(r -> r.get(ownerIdColumn)).collect(Collectors.toSet());
    }
  }

  /**
   * Result of a combined entity-existence + permission check.
   *
   * @param entityExists whether the entity exists in the ancestor view
   * @param permitted whether the user has the permission (via global context, isGlobal, or ancestor match)
   */
  public record PermissionCheckResult(boolean entityExists, boolean permitted)
  {
  }

  /**
   * Single-entity permission check with entity existence detection. Uses the same CTE + ancestor view approach as
   * {@link #getPermittedOwnerIds} but returns a {@link PermissionCheckResult} from a single query that checks both
   * entity existence and permission, avoiding extra round trips.
   */
  public PermissionCheckResult checkPermissionForOwner(
      final String ownerId,
      final OwnerType ownerType,
      final Permission permission,
      final String username,
      final Set<String> groupNames)
  {
    try (TransactionContext tx = createTransactionContext()) {
      if (ownerType == null) {
        return checkPermissionForOwnerSpecific(tx, ownerId, OWNER_ANCESTOR,
            OWNER_ANCESTOR.OWNER_ID, OWNER_ANCESTOR.ANCESTOR_ID, permission, username, groupNames);
      }
      switch (ownerType) {
        case APPLICATION:
          return checkPermissionForOwnerSpecific(tx, ownerId, APPLICATION_ANCESTOR,
              APPLICATION_ANCESTOR.APPLICATION_ID, APPLICATION_ANCESTOR.ANCESTOR_ID,
              permission, username, groupNames);
        case ORGANIZATION:
          return checkPermissionForOwnerSpecific(tx, ownerId, ORGANIZATION_ANCESTOR,
              ORGANIZATION_ANCESTOR.ORGANIZATION_ID, ORGANIZATION_ANCESTOR.ANCESTOR_ID,
              permission, username, groupNames);
        case REPOSITORY:
          return checkPermissionForOwnerSpecific(tx, ownerId, REPOSITORY_ANCESTOR,
              REPOSITORY_ANCESTOR.REPOSITORY_ID, REPOSITORY_ANCESTOR.ANCESTOR_ID,
              permission, username, groupNames);
        case REPOSITORY_MANAGER:
          return checkPermissionForOwnerSpecific(tx, ownerId, REPOSITORY_MANAGER_ANCESTOR,
              REPOSITORY_MANAGER_ANCESTOR.REPOSITORY_MANAGER_ID, REPOSITORY_MANAGER_ANCESTOR.ANCESTOR_ID,
              permission, username, groupNames);
        case REPOSITORY_CONTAINER:
          return checkPermissionForOwnerSpecific(tx, ownerId, REPOSITORY_CONTAINER_ANCESTOR,
              REPOSITORY_CONTAINER_ANCESTOR.REPOSITORY_CONTAINER_ID, REPOSITORY_CONTAINER_ANCESTOR.ANCESTOR_ID,
              permission, username, groupNames);
        default:
          return checkPermissionForOwnerSpecific(tx, ownerId, OWNER_ANCESTOR,
              OWNER_ANCESTOR.OWNER_ID, OWNER_ANCESTOR.ANCESTOR_ID, permission, username, groupNames);
      }
    }
  }

  private <R extends Record> PermissionCheckResult checkPermissionForOwnerSpecific(
      TransactionContext tx,
      String ownerId,
      Table<R> ancestorTable,
      Field<String> ownerIdColumn,
      Field<String> ancestorIdColumn,
      Permission permission,
      String username,
      Set<String> groupNames)
  {
    Set<String> effectiveGroupNames = groupNames != null ? groupNames : Collections.emptySet();

    var userCondition = MEMBERSHIP_MAPPING.MEMBER_TYPE.eq(MemberType.USER.name())
        .and(MEMBERSHIP_MAPPING.MEMBER_NAME.eq(username)
            .or(DSL.lower(MEMBERSHIP_MAPPING.MEMBER_NAME).eq(username.toLowerCase()))
            .or(DSL.upper(MEMBERSHIP_MAPPING.MEMBER_NAME).eq(username.toUpperCase())));
    var groupCondition = MEMBERSHIP_MAPPING.MEMBER_TYPE.eq(MemberType.GROUP.name())
        .and(MEMBERSHIP_MAPPING.MEMBER_NAME.in(effectiveGroupNames));

    CommonTableExpression<Record1<String>> userContextsCte = DSL.name("user_contexts")
        .fields("context_id")
        .as(tx.dsl()
            .selectDistinct(MEMBERSHIP_MAPPING.CONTEXT_ID)
            .from(MEMBERSHIP_MAPPING)
            .join(ROLE_PERMISSION)
            .on(ROLE_PERMISSION.ROLE_ID.eq(MEMBERSHIP_MAPPING.ROLE_ID))
            .where(ROLE_PERMISSION.PERMISSION.eq(permission.name()))
            .and(userCondition.or(groupCondition)));

    Field<String> ctxId = userContextsCte.field("context_id", String.class);

    // Single query that returns both entity existence and permission status.
    // entity_exists: whether the ownerId appears in the type-specific ancestor view.
    // permitted: whether the user has the permission via global context, isGlobal, or ancestor match.
    Field<Boolean> entityExists = DSL.field(
        DSL.exists(DSL.selectOne().from(ancestorTable).where(ownerIdColumn.eq(ownerId))));
    Field<Boolean> permitted = DSL.field(
        DSL.exists(
            tx.dsl()
                .with(userContextsCte)
                .selectOne()
                .where(
                    (permission.isGlobal()
                        ? DSL.exists(DSL.selectOne().from(userContextsCte))
                        : DSL.noCondition())
                            .or(DSL.exists(
                                DSL.selectOne()
                                    .from(userContextsCte)
                                    .where(ctxId.eq(MembershipMapping.GLOBAL_CONTEXT_ID))))
                            .or(DSL.exists(
                                DSL.selectOne()
                                    .from(ancestorTable)
                                    .where(ownerIdColumn.eq(ownerId))
                                    .and(ancestorIdColumn.in(
                                        DSL.select(ctxId).from(userContextsCte))))))));

    Record result = tx.dsl()
        .select(entityExists.as("entity_exists"), permitted.as("permitted"))
        .fetchSingle();

    boolean exists = Boolean.TRUE.equals(result.get("entity_exists"));
    boolean isPermitted = Boolean.TRUE.equals(result.get("permitted"));

    return new PermissionCheckResult(exists, isPermitted);
  }

  /**
   * NOTE: if your goal is ultimately to obtain instances of some other owner-related entity, such as policies,
   * this method is probably not the most effective choice. Instead, consider joining against the OwnerAncestor view
   * when querying the database in order to get entities related to a given owner and all of its ancestors at the
   * same time.
   */
  public Iterable<Owner> walkHierarchy(final Owner owner) {
    if (owner == null) {
      return Collections.emptyList();
    }
    else if (owner.getParentOwnerId() == null) {
      return Collections.singletonList(owner);
    }
    else {
      // Return an iterable that will initially return the passed-in owner and then call walkHierarchy on that
      // owner's parent lazily, only if needed
      return () -> {
        return new Iterator<>()
        {
          private boolean returnedImmediateOwner = false;

          private Iterator<Owner> parentIterator;

          @Override
          public boolean hasNext() {
            return !returnedImmediateOwner || getParentIterator().hasNext();
          }

          @Override
          public Owner next() {
            if (!returnedImmediateOwner) {
              returnedImmediateOwner = true;
              return owner;
            }
            else {
              return getParentIterator().next();
            }
          }

          private Iterator<Owner> getParentIterator() {
            if (parentIterator == null) {
              parentIterator = walkHierarchy(owner.getParentOwnerId(), owner.getType().getParentType()).iterator();
            }

            return parentIterator;
          }
        };
      };
    }
  }

  public Iterable<Owner> walkHierarchy(final String ownerId) {
    return walkHierarchy(ownerId, null);
  }

  public Iterable<Owner> walkHierarchy(final String ownerId, final OwnerType type) {
    return walkHierarchy(null, ownerId, type);
  }

  public Iterable<Owner> walkHierarchy(final TransactionContext tx, final String ownerId) {
    return walkHierarchy(tx, ownerId, null);
  }

  /**
   * @param type the type of the owner whose ancestors are being queried. Can be left null if unknown, or specified
   *          if known in order to optimize the number of queries performed
   */
  public Iterable<Owner> walkHierarchy(final TransactionContext tx, final String ownerId, final OwnerType type) {
    // We use Stream for its laziness, but Stream itself cannot be iterated multiple times, so we must wrap its
    // construction in this Iterable that, if its iterator() method were called multiple times, would repeat this
    // logic
    return () -> {
      boolean fetchApp = type == null || type == OwnerType.APPLICATION;
      boolean fetchRepo = type == null || type == OwnerType.REPOSITORY;
      boolean fetchRepoManager = type == null || type == OwnerType.REPOSITORY || type == OwnerType.REPOSITORY_MANAGER;

      // also will be included in the result if a repo manager is fetched
      boolean fetchRepoContainerSeparately = RepositoryContainer.REPOSITORY_CONTAINER_ID.equals(ownerId);

      // Because the returned list will contain objects of multiple different types, we have to fetch them in multiple
      // JPA queries. Note that due to the limitations of TransactionContext (it is not thread-safe) we cannot do these
      // queries concurrently
      Stream.Builder<Stream<Owner>> hierarchyBuilder = Stream.builder();
      if (fetchApp) {
        hierarchyBuilder.accept(lazy(() -> tx == null ? appDAO.getById(ownerId) : appDAO.getById(tx, ownerId)));
      }
      if (fetchRepo) {
        hierarchyBuilder.accept(lazy(() -> tx == null ? repoDAO.getById(ownerId) : repoDAO.getById(tx, ownerId)));
      }
      if (fetchRepoManager) {
        Supplier<RepositoryManager> repoManagerSupplier = () -> tx == null
            ? repoManagerDAO.getByIdOrRepositoryId(ownerId)
            : repoManagerDAO.getByIdOrRepositoryId(tx, ownerId);

        hierarchyBuilder.accept(
            lazy(repoManagerSupplier)
                .filter(Objects::nonNull)
                // NOTE: we avoid doing a separate database lookup for the RepositoryContainer - we know that if
                // a repo manager is an ancestor then a RepositoryContainer is as well.
                .flatMap(repoManager -> Stream.of(repoManager, RepositoryContainer.SINGLETON)));
      }
      if (fetchRepoContainerSeparately) {
        hierarchyBuilder.accept(Stream.of(RepositoryContainer.SINGLETON));
      }

      Supplier<Stream<Organization>> parentOrgSupplier = () -> tx == null
          ? orgDAO.getAllParentOrganizations(ownerId, type).stream()
          : orgDAO.getAllParentOrganizations(tx, ownerId, type).stream();

      hierarchyBuilder.accept(lazy(parentOrgSupplier).flatMap(Function.identity()).map(Owner.class::cast));

      return hierarchyBuilder.build()
          .flatMap(Function.identity())
          .filter(Objects::nonNull)
          .iterator();
    };
  }

  /**
   * Walk the children of a given owner
   * THIS METHOD DOES NOT GUARANTEE ANY SPECIFIC ORDER
   *
   * @param owner Organization | Application | Repository | Repository Manager | Repository Container
   * @return List of owners
   */
  public List<Owner> walkChildren(final TransactionContext tx, final Owner owner) {
    OwnerType type = owner.getType();
    String ownerId = owner.getId();
    boolean isRootOrg = type == OwnerType.ORGANIZATION && Organization.ROOT_ORGANIZATION_ID.equals(ownerId);
    boolean fetchApp = type == OwnerType.ORGANIZATION;
    boolean fetchRepo = type == OwnerType.ORGANIZATION || type == OwnerType.REPOSITORY_CONTAINER ||
        type == OwnerType.REPOSITORY_MANAGER;
    boolean fetchRepoManager = isRootOrg || type == OwnerType.REPOSITORY_CONTAINER;
    boolean fetchRepoContainer = isRootOrg;

    // Because the returned list will contain objects of multiple different types, we have to fetch them in multiple
    // JPA queries
    List<Owner> children = orgDAO.getAllChildOrganizations(tx, ownerId)
        .stream()
        // the first org is the owner that was queried, so skip it
        .skip(1)
        .collect(Collectors.toCollection(ArrayList::new));

    if (fetchRepoContainer && isRootOrg) {
      children.add(RepositoryContainer.SINGLETON);
    }
    if (fetchRepoManager) {
      children.addAll(repoManagerDAO.getAll(tx));
    }
    if (fetchRepo) {
      children.addAll(repoDAO.getByAncestorId(tx, ownerId));
    }
    if (fetchApp) {
      children.addAll(appDAO.getByAncestorId(tx, ownerId));
    }

    return children;
  }

  public List<Owner> walkChildren(final Owner owner) {
    try (TransactionContext tx = appDAO.createTransactionContext()) {
      return walkChildren(tx, owner);
    }
  }

  /**
   * Deletes all entities associated with the given owner.
   * <p>
   * <b>Transaction Boundary Semantics:</b> All delete operations in this method participate in the
   * provided transaction context. This ensures atomic rollback behavior - if any cascade operation
   * fails, all previous deletions will also be rolled back when the transaction is rolled back.
   * </p>
   * <p>
   * <b>What gets deleted:</b>
   * </p>
   * <ul>
   * <li>Policy waivers owned by this owner</li>
   * <li>License overrides owned by this owner</li>
   * <li>Security vulnerability overrides owned by this owner</li>
   * <li>Policy overrides referencing this owner (updates policies to remove references)</li>
   * <li>Policy waiver requests owned by this owner</li>
   * <li>Data retention policies owned by this owner</li>
   * <li>Policy monitoring configurations owned by this owner</li>
   * <li>Component copyrights owned by this owner</li>
   * <li>Component legal files owned by this owner</li>
   * <li>Component obligations owned by this owner</li>
   * <li>Component obligation attributions owned by this owner</li>
   * <li>Vulnerability groups owned by this owner</li>
   * <li>Custom vulnerability remediations owned by this owner</li>
   * <li>Custom vulnerability CWE data owned by this owner</li>
   * <li>Custom vulnerability CVSS vector data owned by this owner</li>
   * <li>Custom vulnerability CVSS severity data owned by this owner</li>
   * <li>Call flow analysis configuration owned by this owner</li>
   * </ul>
   * <p>
   * <b>Note:</b> This method is called from cascade delete operations in {@code ApplicationDAO},
   * {@code OrganizationDAO}, {@code RepositoryDAO}, and {@code RepositoryManagerDAO}. All these
   * callers pass their transaction context to ensure the entire cascade operation is atomic.
   * </p>
   *
   * @param tx the transaction context that all operations will participate in
   * @param owner the owner whose associated entities should be deleted
   */
  public void cascadeDelete(TransactionContext tx, Owner owner) {
    // Cascade to policy waivers
    PolicyWaiverDAO policyWaiverDAO = policyWaiverDAOProvider.get();
    List<PolicyWaiver> policyWaivers = policyWaiverDAO.getByOwnerId(tx, owner.getId());
    for (PolicyWaiver policyWaiver : policyWaivers) {
      policyWaiverDAO.delete(tx, policyWaiver);
    }

    // Cascade to license overrides
    LicenseOverrideDAO licenseOverrideDAO = licenseOverrideDAOProvider.get();
    List<LicenseOverride> licenseOverrides = licenseOverrideDAO.getByOwnerId(tx, owner.getId());
    for (LicenseOverride licenseOverride : licenseOverrides) {
      licenseOverrideDAO.delete(tx, licenseOverride);
    }

    // Cascade to security vulnerability overrides
    List<SecurityVulnerabilityOverride> securityVulnerabilityOverrides = securityVulnerabilityOverrideDAO.getByOwnerId(
        tx, owner.getId());
    for (SecurityVulnerabilityOverride securityVulnerabilityOverride : securityVulnerabilityOverrides) {
      securityVulnerabilityOverrideDAO.delete(tx, securityVulnerabilityOverride);
    }

    // Cascade to policy overrides
    PolicyDAO policyDAO = policyDAOProvider.get();
    for (Policy policy : policyDAO.getAll(tx)) {
      boolean updated = false;
      if (policy.getPolicyActionsOverrides() != null && policy.getPolicyActionsOverrides().containsKey(owner.getId())) {
        policy.getPolicyActionsOverrides().remove(owner.getId());
        updated = true;
      }
      if (policy.getPolicyNotificationsOverrides() != null &&
          policy.getPolicyNotificationsOverrides().containsKey(owner.getId()))
      {
        policy.getPolicyNotificationsOverrides().remove(owner.getId());
        updated = true;
      }
      if (updated) {
        policyDAO.update(tx, policy);
      }
    }

    // Cascade to policy waiver requests
    PolicyWaiverRequestDAO policyWaiverRequestDAO = policyWaiverRequestDAOProvider.get();
    List<PolicyWaiverRequest> policyWaiverRequests = policyWaiverRequestDAO.getByOwnerId(tx, owner.getId());
    for (PolicyWaiverRequest policyWaiverRequest : policyWaiverRequests) {
      policyWaiverRequestDAO.delete(tx, policyWaiverRequest);
    }

    // Cascade to data retention policies
    for (DataRetentionPolicy dataRetentionPolicy : dataRetentionPolicyDAO.getByOwnerId(tx, owner.getId()).values()) {
      dataRetentionPolicyDAO.delete(tx, dataRetentionPolicy);
    }

    // Cascade to policy monitoring
    List<PolicyMonitoring> policyMonitorings = policyMonitoringDAO.getByOwnerId(tx, owner.getId());
    if (isNotEmpty(policyMonitorings)) {
      for (PolicyMonitoring policyMonitoring : policyMonitorings) {
        policyMonitoringDAO.delete(tx, policyMonitoring);
      }
    }

    // Cascade to component copyrights
    ComponentCopyrightDAO componentCopyrightDAO = componentCopyrightDAOProvider.get();
    for (ComponentCopyright componentCopyright : componentCopyrightDAO.getByOwnerId(tx, owner.getId())) {
      componentCopyrightDAO.delete(tx, componentCopyright);
    }

    // Cascade to component legal files
    ComponentLegalFileDAO componentLegalFileDAO = componentLegalFileDAOProvider.get();
    for (ComponentLegalFile componentLegalFile : componentLegalFileDAO.getByOwnerId(tx, owner.getId())) {
      componentLegalFileDAO.delete(tx, componentLegalFile);
    }

    // Cascade to component obligations
    ComponentObligationDAO componentObligationDAO = componentObligationDAOProvider.get();
    for (ComponentObligation componentObligation : componentObligationDAO.getByOwnerId(tx, owner.getId())) {
      componentObligationDAO.delete(tx, componentObligation);
    }

    // Cascade to component obligation attributions
    ComponentObligationAttributionDAO componentObligationAttributionDAO =
        componentObligationAttributionDAOProvider.get();
    for (ComponentObligationAttribution componentObligationAttribution : componentObligationAttributionDAO
        .getByOwnerId(tx, owner.getId()))
    {
      componentObligationAttributionDAO.delete(tx, componentObligationAttribution);
    }

    // Cascade to vulnerability groups
    VulnerabilityGroupDAO vulnerabilityGroupDAO = vulnerabilityGroupDAOProvider.get();
    for (VulnerabilityGroup vulnerabilityGroup : vulnerabilityGroupDAO.getByOwnerId(tx, owner.getId())) {
      vulnerabilityGroupDAO.delete(tx, vulnerabilityGroup);
    }

    // Cascade to vulnerability custom remediation
    VulnerabilityCustomRemediationDAO vulnerabilityCustomRemediationDAO =
        vulnerabilityCustomRemediationDAOProvider.get();
    for (VulnerabilityCustomRemediation vulnerabilityCustomRemediation : vulnerabilityCustomRemediationDAO
        .getByOwnerId(tx, owner.getId()))
    {
      vulnerabilityCustomRemediationDAO.delete(tx, vulnerabilityCustomRemediation);
    }

    // Cascade to vulnerability custom CWE
    VulnerabilityCustomCweDAO vulnerabilityCustomCweDAO = vulnerabilityCustomCweDAOProvider.get();
    for (VulnerabilityCustomCwe vulnerabilityCustomCwe : vulnerabilityCustomCweDAO.getByOwnerId(tx, owner.getId())) {
      vulnerabilityCustomCweDAO.delete(tx, vulnerabilityCustomCwe);
    }

    // Cascade to vulnerability custom CVSS vector data
    VulnerabilityCustomCvssVectorDAO vulnerabilityCustomCvssDAO = vulnerabilityCustomCvssVectorDAOProvider.get();
    for (VulnerabilityCustomCvssVector vulnerabilityCustomCvss : vulnerabilityCustomCvssDAO.getByOwnerId(tx,
        owner.getId()))
    {
      vulnerabilityCustomCvssDAO.delete(tx, vulnerabilityCustomCvss);
    }

    // Cascade to vulnerability custom CVSS severity data
    VulnerabilityCustomCvssSeverityDAO vulnerabilityCustomCvssSeverityDAO =
        vulnerabilityCustomCvssSeverityDAOProvider.get();
    for (VulnerabilityCustomCvssSeverity vulnerabilityCustomCvssSeverity : vulnerabilityCustomCvssSeverityDAO
        .getByOwnerId(tx,
            owner.getId()))
    {
      vulnerabilityCustomCvssSeverityDAO.delete(tx, vulnerabilityCustomCvssSeverity);
    }

    // Cascade to call flow config
    CallFlowAnalysisConfigDAO callFlowAnalysisConfigDAO = callFlowAnalysisConfigDAOProvider.get();
    CallFlowAnalysisConfig callFlowAnalysisConfig = callFlowAnalysisConfigDAO.getByOwnerId(tx, owner.getId());
    if (callFlowAnalysisConfig != null) {
      callFlowAnalysisConfigDAO.delete(tx, callFlowAnalysisConfig);
    }
  }

  /**
   * Returns the ids of all the owners up in the hierarchy for the specified owner, starting with the id of the input
   * owner.
   */
  public List<String> getOwnerIds(Owner owner) {
    List<String> ownerIds = new ArrayList<>();
    walkHierarchy(owner).forEach(anOwner -> ownerIds.add(anOwner.getId()));
    return ownerIds;
  }

  public List<String> getOwnerIds(String ownerId) {
    Owner owner = getById(ownerId);
    return getOwnerIds(owner);
  }

  /**
   * Batch-fetches all ancestor IDs for the given application IDs using the APPLICATION_ANCESTOR view.
   * This includes both the application IDs themselves and all their ancestor organization IDs.
   *
   * @return all unique ancestor IDs (application + organization) for the given applications
   */
  public Set<String> getAncestorIdsByApplicationIds(Set<String> applicationIds) {
    if (CollectionUtils.isEmpty(applicationIds)) {
      return Collections.emptySet();
    }
    try (TransactionContext tx = createTransactionContext()) {
      return new HashSet<>(getListWithSqlInClause(applicationIds,
          ids -> tx.dsl()
              .selectDistinct(APPLICATION_ANCESTOR.ANCESTOR_ID)
              .from(APPLICATION_ANCESTOR)
              .where(APPLICATION_ANCESTOR.APPLICATION_ID.in(ids))
              .fetchInto(String.class)));
    }
  }

  public List<Owner> getAllAppsAndOrgs() {
    try (TransactionContext tx = createTransactionContext()) {
      // Application query
      var appQuery = tx.dsl()
          .select(
              APPLICATION.PUBLIC_ID.as("public_id"),
              APPLICATION.NAME.as("name"),
              APPLICATION.ORGANIZATION_ID.as("parent_owner_id"),
              APPLICATION.APPLICATION_ID.as("id"),
              DSL.inline(false).as("have_children"),
              DSL.inline("APPLICATION").as("type"))
          .from(APPLICATION);

      // Organization query
      var orgQuery = tx.dsl()
          .select(
              ORGANIZATION.ORGANIZATION_ID.as("public_id"),
              ORGANIZATION.NAME.as("name"),
              ORGANIZATION.PARENT_ORGANIZATION_ID.as("parent_owner_id"),
              ORGANIZATION.ORGANIZATION_ID.as("id"),
              DSL.inline(true).as("have_children"),
              DSL.inline("ORGANIZATION").as("type"))
          .from(ORGANIZATION);

      return appQuery.union(orgQuery)
          .limit(MAX_ALLOWED_DB_RESULTS)
          .fetchStream()
          .map(record -> new OwnerImpl(
              record.get("public_id", String.class),
              record.get("name", String.class),
              record.get("parent_owner_id", String.class),
              record.get("have_children", Boolean.class),
              OwnerType.fromString(record.get("type", String.class)),
              record.get("id", String.class)))
          .collect(Collectors.toList());
    }
  }

  public List<Owner> getOwnersByAppTagsAndOrgs(
      final Set<String> applicationIds,
      final Set<String> tagIds,
      final Set<String> organizationsIds)
  {
    if (isEmpty(applicationIds) && isEmpty(tagIds) && isEmpty(organizationsIds)) {
      return Collections.emptyList();
    }

    try (TransactionContext tx = createTransactionContext()) {
      Set<Owner> results = new LinkedHashSet<>();

      if (isNotEmpty(applicationIds) || isNotEmpty(tagIds)) {
        results.addAll(fetchApplicationOwners(tx, applicationIds, tagIds));
      }

      if (isNotEmpty(organizationsIds)) {
        results.addAll(fetchOrganizationOwners(tx, organizationsIds));
      }

      if (results.size() > MAX_ALLOWED_DB_RESULTS) {
        return new ArrayList<>(results).subList(0, MAX_ALLOWED_DB_RESULTS);
      }
      return new ArrayList<>(results);
    }
  }

  private List<Owner> fetchApplicationOwners(
      TransactionContext tx,
      final Set<String> applicationIds,
      final Set<String> tagIds)
  {
    Condition whereCondition = null;

    if (isNotEmpty(applicationIds) && isNotEmpty(tagIds)) {
      var tagCondition = buildTagCondition(tagIds);
      whereCondition = APPLICATION.APPLICATION_ID.in(applicationIds).and(tagCondition);
    }
    else if (isNotEmpty(tagIds)) {
      whereCondition = buildTagCondition(tagIds);
    }
    else if (isNotEmpty(applicationIds)) {
      whereCondition = APPLICATION.APPLICATION_ID.in(applicationIds);
    }

    var baseQuery = tx.dsl()
        .selectDistinct(
            APPLICATION.PUBLIC_ID.as("public_id"),
            APPLICATION.NAME.as("name"),
            APPLICATION.ORGANIZATION_ID.as("parent_owner_id"),
            APPLICATION.APPLICATION_ID.as("id"),
            DSL.inline(false).as("have_children"),
            DSL.inline("APPLICATION").as("type"))
        .from(APPLICATION)
        .leftOuterJoin(APPLICATION_TAG)
        .on(APPLICATION_TAG.APPLICATION_ID.eq(APPLICATION.APPLICATION_ID));

    var query = whereCondition != null ? baseQuery.where(whereCondition) : baseQuery;

    return query.fetchStream()
        .map(record -> (Owner) new OwnerImpl(
            record.get("public_id", String.class),
            record.get("name", String.class),
            record.get("parent_owner_id", String.class),
            record.get("have_children", Boolean.class),
            OwnerType.fromString(record.get("type", String.class)),
            record.get("id", String.class)))
        .collect(Collectors.toList());
  }

  private List<Owner> fetchOrganizationOwners(
      TransactionContext tx,
      final Set<String> organizationsIds)
  {
    var baseQuery = tx.dsl()
        .select(
            ORGANIZATION.ORGANIZATION_ID.as("public_id"),
            ORGANIZATION.NAME.as("name"),
            ORGANIZATION.PARENT_ORGANIZATION_ID.as("parent_owner_id"),
            ORGANIZATION.ORGANIZATION_ID.as("id"),
            DSL.inline(true).as("have_children"),
            DSL.inline("ORGANIZATION").as("type"))
        .from(ORGANIZATION);

    var query = isNotEmpty(organizationsIds)
        ? baseQuery.where(ORGANIZATION.ORGANIZATION_ID.in(organizationsIds))
        : baseQuery;

    return query.fetchStream()
        .map(record -> (Owner) new OwnerImpl(
            record.get("public_id", String.class),
            record.get("name", String.class),
            record.get("parent_owner_id", String.class),
            record.get("have_children", Boolean.class),
            OwnerType.fromString(record.get("type", String.class)),
            record.get("id", String.class)))
        .collect(Collectors.toList());
  }

  private Condition buildTagCondition(final Set<String> tagIds) {
    final boolean includeAppsWithNoTags = tagIds.contains(null);

    if (includeAppsWithNoTags) {
      return APPLICATION_TAG.TAG_ID.in(tagIds).or(APPLICATION_TAG.TAG_ID.isNull());
    }
    else {
      return APPLICATION_TAG.TAG_ID.in(tagIds);
    }
  }

  /**
   * Converts a Supplier into a Stream which will only invoke the underlying logic if it is needed
   */
  private static <T> Stream<T> lazy(Supplier<T> supplier) {
    return Stream.of(supplier).map(Supplier::get);
  }

  @Override
  public Table<?> getJooqTable() {
    // OwnerDAO is a composite DAO that queries multiple tables (applications, organizations, repositories, etc.)
    // There is no single jOOQ table for Owner entities
    return null;
  }

  @Override
  public List<Owner> getAll(TransactionContext tx) {
    // OwnerDAO is a composite DAO - to get all owners, we combine results from multiple DAOs
    List<Owner> allOwners = new ArrayList<>();
    allOwners.addAll(orgDAO.getAll(tx));
    allOwners.addAll(appDAO.getAll(tx));
    allOwners.addAll(repoDAO.getAll(tx));
    allOwners.addAll(repoManagerDAO.getAll(tx));
    allOwners.add(RepositoryContainer.SINGLETON);
    return allOwners;
  }

  @Override
  public void insert(TransactionContext tx, Owner entity) {
    // OwnerDAO is a composite DAO - delegate to the appropriate DAO based on owner type
    switch (entity.getType()) {
      case ORGANIZATION:
        orgDAO.insert(tx, (Organization) entity);
        break;
      case APPLICATION:
        appDAO.insert(tx, (Application) entity);
        break;
      case REPOSITORY:
        repoDAO.insert(tx, (Repository) entity);
        break;
      case REPOSITORY_MANAGER:
        repoManagerDAO.insert(tx, (RepositoryManager) entity);
        break;
      case REPOSITORY_CONTAINER:
        // RepositoryContainer.SINGLETON is immutable and cannot be inserted
        throw new UnsupportedOperationException("RepositoryContainer cannot be inserted");
      default:
        throw new IllegalStateException("Unhandled owner type: " + entity.getType());
    }
  }

  @Override
  public void update(TransactionContext tx, Owner entity) {
    // OwnerDAO is a composite DAO - delegate to the appropriate DAO based on owner type
    switch (entity.getType()) {
      case ORGANIZATION:
        orgDAO.update(tx, (Organization) entity);
        break;
      case APPLICATION:
        appDAO.update(tx, (Application) entity);
        break;
      case REPOSITORY:
        repoDAO.update(tx, (Repository) entity);
        break;
      case REPOSITORY_MANAGER:
        repoManagerDAO.update(tx, (RepositoryManager) entity);
        break;
      case REPOSITORY_CONTAINER:
        // RepositoryContainer.SINGLETON is immutable and cannot be updated
        throw new UnsupportedOperationException("RepositoryContainer cannot be updated");
      default:
        throw new IllegalStateException("Unhandled owner type: " + entity.getType());
    }
  }

  @Override
  public void delete(TransactionContext tx, Owner entity) {
    // OwnerDAO is a composite DAO - delegate to the appropriate DAO based on owner type
    switch (entity.getType()) {
      case ORGANIZATION:
        orgDAO.delete(tx, (Organization) entity);
        break;
      case APPLICATION:
        appDAO.delete(tx, (Application) entity);
        break;
      case REPOSITORY:
        repoDAO.delete(tx, (Repository) entity);
        break;
      case REPOSITORY_MANAGER:
        repoManagerDAO.delete(tx, (RepositoryManager) entity);
        break;
      case REPOSITORY_CONTAINER:
        // RepositoryContainer.SINGLETON is immutable and cannot be deleted
        throw new UnsupportedOperationException("RepositoryContainer cannot be deleted");
      default:
        throw new IllegalStateException("Unhandled owner type: " + entity.getType());
    }
  }

  @Override
  public Class<Owner> getEntityClass() {
    return Owner.class;
  }
}
