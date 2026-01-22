/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;

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
import com.sonatype.insight.brain.model.vulnerability.SecurityVulnerabilityOverride;
import com.sonatype.insight.brain.model.vulnerability.VulnerabilityCustomCvssSeverity;
import com.sonatype.insight.brain.model.vulnerability.VulnerabilityCustomCvssVector;
import com.sonatype.insight.brain.model.vulnerability.VulnerabilityCustomCwe;
import com.sonatype.insight.brain.model.vulnerability.VulnerabilityCustomRemediation;
import com.sonatype.insight.brain.model.vulnerability.VulnerabilityGroup;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.NotFoundException;

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
        return new Iterator<>() {
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
   * if known in order to optimize the number of queries performed
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
        Supplier<RepositoryManager> repoManagerSupplier = () -> tx == null ?
            repoManagerDAO.getByIdOrRepositoryId(ownerId) :
            repoManagerDAO.getByIdOrRepositoryId(tx, ownerId);

        hierarchyBuilder.accept(
            lazy(repoManagerSupplier)
                .filter(Objects::nonNull)
                // NOTE: we avoid doing a separate database lookup for the RepositoryContainer - we know that if
                // a repo manager is an ancestor then a RepositoryContainer is as well.
                .flatMap(repoManager -> Stream.of(repoManager, RepositoryContainer.SINGLETON))
        );
      }
      if (fetchRepoContainerSeparately) {
        hierarchyBuilder.accept(Stream.of(RepositoryContainer.SINGLETON));
      }

      Supplier<Stream<Organization>> parentOrgSupplier = () -> tx == null ?
          orgDAO.getAllParentOrganizations(ownerId, type).stream() :
          orgDAO.getAllParentOrganizations(tx, ownerId, type).stream();

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
    List<Owner> children = orgDAO.getAllChildOrganizations(tx, ownerId).stream()
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
          policy.getPolicyNotificationsOverrides().containsKey(owner.getId())) {
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
        .getByOwnerId(tx, owner.getId())) {
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
        .getByOwnerId(tx, owner.getId())) {
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
        owner.getId())) {
      vulnerabilityCustomCvssDAO.delete(tx, vulnerabilityCustomCvss);
    }

    // Cascade to vulnerability custom CVSS severity data
    VulnerabilityCustomCvssSeverityDAO vulnerabilityCustomCvssSeverityDAO =
        vulnerabilityCustomCvssSeverityDAOProvider.get();
    for (VulnerabilityCustomCvssSeverity vulnerabilityCustomCvssSeverity :
        vulnerabilityCustomCvssSeverityDAO.getByOwnerId(tx,
            owner.getId())) {
      vulnerabilityCustomCvssSeverityDAO.delete(tx, vulnerabilityCustomCvssSeverity);
    }

    // Cascade to call flow config
    CallFlowAnalysisConfigDAO callFlowAnalysisConfigDAO = callFlowAnalysisConfigDAOProvider.get();
    CallFlowAnalysisConfig callFlowAnalysisConfig = callFlowAnalysisConfigDAO.getByOwnerId(owner.getId());
    callFlowAnalysisConfigDAO.delete(tx, callFlowAnalysisConfig);
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

  public List<Owner> getAllAppsAndOrgs() {
    final String sQuery = """
        SELECT
          application.public_id as public_id,
          application.name as name,
          application.organization_id as parent_owner_id,
          application.application_id as id,
          false as have_children,
          'APPLICATION' as type
          FROM %s.application application
        UNION
        SELECT
            org.organization_id as public_id,
            org.name as name,
            org.parent_organization_id as parent_owner_id,
            org.organization_id as id,
            true as have_children,
            'ORGANIZATION' as type
          FROM %s.organization org;
        """.formatted(getDatabaseSchema(), getDatabaseSchema());

    try (TransactionContext tx = createTransactionContext()) {
      final jakarta.persistence.Query query = tx.createNativeQuery(sQuery);
      query.setMaxResults(MAX_ALLOWED_DB_RESULTS);

      try (Stream<Object[]> resultStream = query.getResultStream()) {
        return resultStream.map(values -> {
          return new OwnerImpl(
              (String) values[0], // public id
              (String) values[1], // name
              (String) values[2], // parent owner id
              (Boolean) values[4], // can have children
              OwnerType.fromString((String) values[5]), // type
              (String) values[3]); // id
        }).collect(Collectors.toList());
      }
    }
  }

  public List<Owner> getOwnersByAppTagsAndOrgs(
      final Set<String> applicationIds,
      final Set<String> tagIds,
      final Set<String> organizationsIds
  )
  {
    if (isEmpty(applicationIds) && isEmpty(tagIds) && isEmpty(organizationsIds)) {
      return Collections.emptyList();
    }

    final StringJoiner queryUnionizer = new StringJoiner("\nUNION\n");

    if (isNotEmpty(applicationIds) || isNotEmpty(tagIds)) {
      queryUnionizer.add(getApplicationsForOwnersByAppAndTagIdsQuery(applicationIds, tagIds));
    }

    if (isNotEmpty(organizationsIds)) {
      queryUnionizer.add(getOrganizationsQuery(applicationIds, tagIds, organizationsIds));
    }

    final String sQuery = queryUnionizer.toString();

    try (TransactionContext tx = createTransactionContext()) {
      final jakarta.persistence.Query query = tx.createNativeQuery(sQuery);
      query.setMaxResults(MAX_ALLOWED_DB_RESULTS);

      return performQueryForOwnersByAppAndTagIds(query, applicationIds, tagIds, organizationsIds);
    }
  }

  private String getApplicationsForOwnersByAppAndTagIdsQuery(
      final Set<String> applicationIds,
      final Set<String> tagIds
  )
  {
    final String getApplicationsTemplate = """
          SELECT
            DISTINCT application.public_id,
            application.name,
            application.organization_id as parent_owner_id,
            application.application_id as id,
            false as have_children,
            ''APPLICATION'' as type
          FROM {0}.application application
          LEFT OUTER JOIN {0}.application_tag application_tag
            ON application_tag.application_id = application.application_id
          {1}
          """;

    return MessageFormat.format(
        getApplicationsTemplate,
        getDatabaseSchema(),
        getWhereClauseForOwnersByAppAndTagIds(applicationIds, tagIds)
    );
  }

  private String getOrganizationsQuery(
      final Set<String> applicationIds,
      final Set<String> tagIds,
      final Set<String> organizationsIds
  )
  {
    final int startForOrgIds = getOrgIdsOffset(applicationIds, tagIds);

    final String sQuery = """
              SELECT org.organization_id as public_id,
              org.name as name,
              org.parent_organization_id as parent_owner_id,
              org.organization_id as id,
              true as have_children,
              'ORGANIZATION' as type
            FROM %s.organization org
        """.formatted(getDatabaseSchema());

    if (isNotEmpty(organizationsIds)) {
      return sQuery +
          " WHERE organization_id IN %s".formatted(buildPositionalParameters(organizationsIds, startForOrgIds));
    }
    else {
      return sQuery;
    }
  }

  private String getWhereClauseForOwnersByAppAndTagIds(
      final Set<String> applicationIds,
      final Set<String> tagIds
  )
  {
    final boolean hasTags = isNotEmpty(tagIds);
    final boolean hasApplicationIds = isNotEmpty(applicationIds);
    final int startForTagIds = getTagIdOffset(applicationIds);

    if (!hasTags && !hasApplicationIds) {
      return "";
    }

    if (hasApplicationIds && hasTags) {
      // application ids and tags
      return """
          WHERE application.application_id IN %s AND (
            %s
          )
          """.formatted(
              buildPositionalParameters(applicationIds, 1),
              getTagIdConditionalExpressionForOwnersByAppAndTagIds(tagIds, startForTagIds));
    }
    else if (!hasApplicationIds) {
      // only tags
      return "WHERE " + getTagIdConditionalExpressionForOwnersByAppAndTagIds(tagIds, startForTagIds);
    }
    else {
      // only application id
      return "WHERE application.application_id IN " +
          buildPositionalParameters(applicationIds, 1);
    }
  }

  private String getTagIdConditionalExpressionForOwnersByAppAndTagIds(
      final Set<String> tagIds,
      final int startForTagIds
  )
  {
    final boolean includeAppsWithNoTags = tagIds.contains(null);

    // includes all apps that have one of the supplied tags associated or that have no tags associated
    if (includeAppsWithNoTags) {
      return "application_tag.tag_id IN " + buildPositionalParameters(tagIds, startForTagIds) +
          " OR application_tag.tag_id IS NULL";
    }
    else {
      return "application_tag.tag_id IN " + buildPositionalParameters(tagIds, startForTagIds);
    }
  }

  private List<Owner> performQueryForOwnersByAppAndTagIds(
      final jakarta.persistence.Query query,
      final Set<String> applicationIds,
      final Set<String> tagIds,
      final Set<String> organizationIds
  )
  {
    if (isNotEmpty(applicationIds)) {
      addPositionalParameters(query, applicationIds, 1);
    }

    if (isNotEmpty(tagIds)) {
      final int startForTagIds = isNotEmpty(applicationIds) ? applicationIds.size() + 1 : 1;
      addPositionalParameters(query, tagIds, startForTagIds);
    }

    if (isNotEmpty(organizationIds)) {
      addPositionalParameters(query, organizationIds, getOrgIdsOffset(applicationIds, tagIds));
    }

    try (Stream<Object[]> resultStream = query.getResultStream()) {
      return resultStream.map(values -> {
        return new OwnerImpl(
          (String) values[0], // public id
          (String) values[1], // name
          (String) values[2], // parent owner id
          (Boolean) values[4], // can have children
          OwnerType.fromString((String) values[5]), // type
          (String) values[3]); // id
      }).collect(Collectors.toList());
    }
  }

  private int getTagIdOffset(Set<String> applicationIds) {
    return isNotEmpty(applicationIds) ? applicationIds.size() + 1 : 1;
  }

  private int getOrgIdsOffset(Set<String> applicationIds, Set<String> tagIds) {
    if (isEmpty(applicationIds) && isEmpty(tagIds)) {
      return 1;
    }
    else if (isEmpty(tagIds)) {
      return getTagIdOffset(applicationIds);
    }
    else {
      return getTagIdOffset(applicationIds) + tagIds.size() + 1;
    }
  }

  /**
   * Converts a Supplier into a Stream which will only invoke the underlying logic if it is needed
   */
  private static <T> Stream<T> lazy(Supplier<T> supplier) {
    return Stream.of(supplier).map(Supplier::get);
  }
}
