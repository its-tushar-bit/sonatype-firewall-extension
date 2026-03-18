/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.configuration.AutomaticApplicationsConfigurationDAO;
import com.sonatype.insight.brain.dataaccess.configuration.CiIntegrationsConfigDao;
import com.sonatype.insight.brain.dataaccess.configuration.CpeMatchingConfigurationDAO;
import com.sonatype.insight.brain.dataaccess.configuration.ProprietaryConfigDAO;
import com.sonatype.insight.brain.dataaccess.configuration.VersionEvaluationWindowDAO;
import com.sonatype.insight.brain.dataaccess.label.LabelDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupDAO;
import com.sonatype.insight.brain.dataaccess.policy.AutoPolicyWaiverDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryConnectionDAO;
import com.sonatype.insight.brain.dataaccess.search.SearchIndexManager;
import com.sonatype.insight.brain.dataaccess.security.MembershipMappingDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.ScmUserMappingsDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlOrganizationImportEventDAO;
import com.sonatype.insight.brain.dataaccess.tag.TagDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.InvalidNameException;
import com.sonatype.insight.brain.model.NameHelper;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OrganizationAncestor;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.SearchIndexChange;
import com.sonatype.insight.brain.model.SearchIndexChange.ChangeType;
import com.sonatype.insight.brain.model.configuration.ProprietaryConfig;
import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.brain.model.license.LicenseThreatGroup;
import com.sonatype.insight.brain.model.policy.AutoPolicyWaiver;
import com.sonatype.insight.brain.model.repository.RepositoryConnection;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlOrganizationImportEvent;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
public class OrganizationDAO
    extends AbstractOperationalSqlDAO<Organization>
{
  private static final Logger log = LoggerFactory.getLogger(OrganizationDAO.class);

  private final AutomaticApplicationsConfigurationDAO automaticApplicationsConfigurationDAO;

  private final Provider<LicenseThreatGroupDAO> licenseThreatGroupDAOProvider;

  private final Provider<LabelDAO> labelDAOProvider;

  private final Provider<PolicyDAO> policyDAOProvider;

  private final MembershipMappingDAO membershipMappingDAO;

  private final Provider<OwnerDAO> ownerDAOProvider;

  private final Provider<TagDAO> tagDAOProvider;

  private final Provider<SourceControlDAO> sourceControlDAOProvider;

  private final RepositoryConnectionDAO repositoryConnectionDAO;

  private final SourceControlOrganizationImportEventDAO scmEventDAO;

  private final ProprietaryConfigDAO proprietaryConfigDAO;

  private final OrganizationAncestorDAO organizationAncestorDAO;

  private final AutoPolicyWaiverDAO autoPolicyWaiverDAO;

  private final ScmUserMappingsDAO scmUserMappingsDAO;

  private final CpeMatchingConfigurationDAO cpeMatchingConfigurationDAO;

  private final CiIntegrationsConfigDao ciIntegrationsConfigDao;

  private final VersionEvaluationWindowDAO versionEvaluationWindowDAO;

  @Inject
  public OrganizationDAO(
      final OperationalDataStore operationalDataStore,
      final SearchIndexManager searchIndexManager,
      final AutomaticApplicationsConfigurationDAO automaticApplicationsConfigurationDAO,
      final Provider<LicenseThreatGroupDAO> licenseThreatGroupDAOProvider,
      final Provider<LabelDAO> labelDAOProvider,
      final Provider<PolicyDAO> policyDAOProvider,
      final MembershipMappingDAO membershipMappingDAO,
      final Provider<OwnerDAO> ownerDAOProvider,
      final Provider<TagDAO> tagDAO,
      final Provider<SourceControlDAO> sourceControlDAOProvider,
      final RepositoryConnectionDAO repositoryConnectionDAO,
      final SourceControlOrganizationImportEventDAO scmEventDAO,
      final ProprietaryConfigDAO proprietaryConfigDAO,
      final OrganizationAncestorDAO organizationAncestorDAO,
      final AutoPolicyWaiverDAO autoPolicyWaiverDAO,
      final ScmUserMappingsDAO scmUserMappingsDAO,
      final CpeMatchingConfigurationDAO cpeMatchingConfigurationDAO,
      final CiIntegrationsConfigDao ciIntegrationsConfigDao,
      final VersionEvaluationWindowDAO versionEvaluationWindowDAO)
  {
    super(operationalDataStore, searchIndexManager);
    this.automaticApplicationsConfigurationDAO = automaticApplicationsConfigurationDAO;
    this.licenseThreatGroupDAOProvider = licenseThreatGroupDAOProvider;
    this.labelDAOProvider = labelDAOProvider;
    this.policyDAOProvider = policyDAOProvider;
    this.membershipMappingDAO = membershipMappingDAO;
    this.ownerDAOProvider = ownerDAOProvider;
    this.tagDAOProvider = tagDAO;
    this.sourceControlDAOProvider = sourceControlDAOProvider;
    this.repositoryConnectionDAO = repositoryConnectionDAO;
    this.scmEventDAO = scmEventDAO;
    this.proprietaryConfigDAO = proprietaryConfigDAO;
    this.organizationAncestorDAO = organizationAncestorDAO;
    this.autoPolicyWaiverDAO = autoPolicyWaiverDAO;
    this.scmUserMappingsDAO = scmUserMappingsDAO;
    this.cpeMatchingConfigurationDAO = cpeMatchingConfigurationDAO;
    this.ciIntegrationsConfigDao = ciIntegrationsConfigDao;
    this.versionEvaluationWindowDAO = versionEvaluationWindowDAO;
  }

  public Organization getByName(TransactionContext tx, String name) {
    if (name == null || name.trim().isEmpty()) {
      throw new DataAccessException("The organization name cannot be null or empty.");
    }
    // Organization Name is whitespace and case insensitive
    name = NameHelper.normalize(name);
    String sQuery = "SELECT entity FROM Organization entity WHERE entity.nameLowercaseNoWhitespace=?1";
    return get(tx, sQuery, name);
  }

  public List<Organization> getByNames(Set<String> organizationNames) {
    organizationNames = organizationNames.stream().map(NameHelper::normalize).collect(Collectors.toSet());
    String sQuery = "SELECT entity FROM Organization entity" + //
        " WHERE entity.nameLowercaseNoWhitespace IN (?1)" + //
        " ORDER BY entity.name";
    return getList(sQuery, organizationNames);
  }

  public List<Organization> getByNamesAndWithoutRelatedRepositories(Set<String> organizationNames) {
    organizationNames = organizationNames.stream().map(NameHelper::normalize).collect(Collectors.toSet());
    String sQuery = "SELECT entity FROM Organization entity" + //
        " WHERE entity.nameLowercaseNoWhitespace IN (?1)" + //
        " AND entity.relatedRepositoryId IS NULL" + //
        " AND entity.relatedRepositoryManagerId IS NULL" + //
        " ORDER BY entity.name";
    return getList(sQuery, organizationNames);
  }

  public Organization getByName(String name) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByName(tx, name);
    }
  }

  @Override
  public List<Organization> getAll() {
    String sQuery = "SELECT entity FROM Organization entity" + //
        " ORDER BY entity.name";
    return getList(sQuery);
  }

  public List<Organization> getAllWithoutRelatedRepositories() {
    String sQuery = "SELECT entity FROM Organization entity" +
        " WHERE entity.relatedRepositoryId IS NULL" +
        " AND entity.relatedRepositoryManagerId IS NULL" +
        " AND entity.relatedRepositorContainerId IS NULL" +
        " ORDER BY entity.name";
    return getList(sQuery);
  }

  public List<Organization> getByRelatedRepositoryManagerId(
      TransactionContext tx,
      String relatedRepositoryManagerId)
  {
    String sQuery = "SELECT entity FROM Organization entity" + //
        " WHERE entity.relatedRepositoryManagerId=?1" + //
        " ORDER BY entity.name";
    return getList(tx, sQuery, relatedRepositoryManagerId);
  }

  public List<Organization> getByRelatedRepositoryManagerId(String relatedRepositoryManagerId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByRelatedRepositoryManagerId(tx, relatedRepositoryManagerId);
    }
  }

  public List<Organization> getByRelatedRepositoryId(TransactionContext tx, String relatedRepositoryId) {
    String sQuery = "SELECT entity FROM Organization entity" + //
        " WHERE entity.relatedRepositoryId=?1" + //
        " ORDER BY entity.name";
    return getList(tx, sQuery, relatedRepositoryId);
  }

  public List<Organization> getByRelatedRepositoryId(String relatedRepositoryId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByRelatedRepositoryId(tx, relatedRepositoryId);
    }
  }

  @Override
  public void insert(TransactionContext tx, Organization organization) {
    NameHelper.validate("Name", organization.getName(), NameHelper.MAX_NAME_LENGTH_APP_ORG);

    if (getByName(tx, organization.getName()) != null) {
      throw new InvalidNameException(organization.getName() + " is already used as a name.");
    }

    if (organization.getParentOrganizationId() != null) {
      Organization parentOrg = getById(tx, organization.getParentOrganizationId());
      if (parentOrg == null) {
        throw new BadRequestException("Invalid parent organization");
      }
    }
    else {
      // Make sure the parent org is set to the root on creation
      organization.setParentOrganizationId(Organization.ROOT_ORGANIZATION_ID);
    }

    super.insert(tx, organization);
    insertOrganizationAncestors(tx, organization);
  }

  @Override
  public void update(TransactionContext tx, Organization organization) {
    NameHelper.validate("Name", organization.getName(), NameHelper.MAX_NAME_LENGTH_APP_ORG);

    if (Organization.ROOT_ORGANIZATION_ID.equals(organization.getId())) {
      // Make sure root org updates do not set the parent org
      organization.setParentOrganizationId(null);
    }
    else {
      // Make sure the parent org is not null
      if (organization.getParentOrganizationId() == null) {
        throw new BadRequestException("Parent organization id cant be null.");
      }
    }

    Organization existingOrganizationByName = getByName(tx, organization.getName());
    if (existingOrganizationByName != null && !existingOrganizationByName.getId().equals(organization.getId())) {
      throw new InvalidNameException(organization.getName() + " is already used as a name.");
    }

    Organization existingOrganizationById = getById(tx, organization.getId());
    String oldParentId = existingOrganizationById == null ? null : existingOrganizationById.getParentOwnerId();

    super.update(tx, organization);

    if (!Objects.equals(oldParentId, organization.getParentOwnerId())) {
      updateOrganizationAncestors(tx, organization);
    }
  }

  @Override
  public void delete(TransactionContext tx, Organization organization) {
    long start = System.currentTimeMillis();

    if (Organization.ROOT_ORGANIZATION_ID.equals(organization.getId())) {
      // Do not allow the deletion of the root organization
      throw new BadRequestException("Cannot delete the root organization: " + organization.getName());
    }

    if (organization.getId().equals(automaticApplicationsConfigurationDAO.getOrganizationId(tx))) {
      if (automaticApplicationsConfigurationDAO.isEnabled(tx)) {
        // Do not allow the deletion of the parent organization for automatic application creation if enabled
        throw new BadRequestException("Cannot delete the parent organization for automatic application creation: "
            + organization.getName() + ".");
      }
      else {
        // Remove the organization ID from the system configuration properties if not enabled
        automaticApplicationsConfigurationDAO.setOrganizationId(tx, "");
      }
    }

    // Cascade to license threat groups
    LicenseThreatGroupDAO licenseThreatGroupDAO = licenseThreatGroupDAOProvider.get();
    List<LicenseThreatGroup> licenseThreatGroups = licenseThreatGroupDAO.getByOwnerId(tx, organization.getId());
    for (LicenseThreatGroup licenseThreatGroup : licenseThreatGroups) {
      licenseThreatGroupDAO.delete(tx, licenseThreatGroup);
    }

    // Cascade to labels
    LabelDAO labelDAO = labelDAOProvider.get();
    List<Label> labels = labelDAO.getByOwnerId(tx, organization.getId());
    for (Label label : labels) {
      labelDAO.delete(tx, label);
    }

    // Cascade to policies
    policyDAOProvider.get().deleteByOwnerId(tx, organization.getId());

    // Cascade to membership mappings
    for (MembershipMapping membershipMapping : membershipMappingDAO.getByContextId(tx, organization.getId())) {
      membershipMappingDAO.delete(tx, membershipMapping);
    }

    // Cascade to SCM user mappings
    scmUserMappingsDAO.deleteByOrganizationId(tx, organization.getId());

    // Cascade to owned entities
    ownerDAOProvider.get().cascadeDelete(tx, organization);

    // Cascade to tags
    TagDAO tagDAO = this.tagDAOProvider.get();
    List<Tag> tags = tagDAO.getByOrganizationId(tx, organization.getId());
    for (Tag tag : tags) {
      tagDAO.delete(tx, tag);
    }

    // Cascade to proprietary config
    ProprietaryConfig proprietaryConfig = proprietaryConfigDAO.getByOwnerId(tx, organization.getId());
    if (proprietaryConfig != null) {
      proprietaryConfigDAO.delete(tx, proprietaryConfig);
    }

    // Cascade to SourceControl config
    sourceControlDAOProvider.get().deleteByOwnerId(tx, organization.getId());

    // Cascade to repository connections
    for (RepositoryConnection repositoryConnection : repositoryConnectionDAO.getByOwnerId(tx, organization.getId())) {
      repositoryConnectionDAO.delete(tx, repositoryConnection);
    }

    // Cascade to source control on-boarding events
    for (SourceControlOrganizationImportEvent importEvent : scmEventDAO.getByOrganizationId(tx, organization.getId())) {
      scmEventDAO.delete(tx, importEvent);
    }

    // Cascade to Auto Policy Waivers
    for (AutoPolicyWaiver autoPolicyWaiver : autoPolicyWaiverDAO.getByOwnerId(tx, organization.getId())) {
      autoPolicyWaiverDAO.delete(tx, autoPolicyWaiver);
    }

    for (OrganizationAncestor orgAncestor : organizationAncestorDAO.getByOrganizationId(tx, organization.getId())) {
      organizationAncestorDAO.delete(tx, orgAncestor);
    }

    cpeMatchingConfigurationDAO.delete(tx, cpeMatchingConfigurationDAO.getByOwnerId(tx, organization.getId()));

    // Cascade to CI integrations config
    ciIntegrationsConfigDao.delete(tx, "ORGANIZATION", organization.getId());

    versionEvaluationWindowDAO.deleteByOwnerId(tx, organization.getId());

    super.delete(tx, organization);

    long duration = System.currentTimeMillis() - start;
    if (duration > 500) {
      log.debug("Deleted organization '{}' with id {} in {} ms.", organization.getName(), organization.getId(),
          duration);
    }
  }

  public List<Organization> getByParentOrganizationId(TransactionContext tx, String parentOrganizationId) {
    String sQuery = "SELECT entity FROM Organization entity" + //
        " WHERE entity.parentOrganizationId=?1";
    return getList(tx, sQuery, parentOrganizationId);
  }

  public List<Organization> getByParentOrganizationId(String parentOrganizationId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByParentOrganizationId(tx, parentOrganizationId);
    }
  }

  @Override
  protected SearchIndexChange newSearchIndexChange(Organization entity) {
    if (entity.getRelatedRepositorContainerId() != null
        || entity.getRelatedRepositoryManagerId() != null
        || entity.getRelatedRepositoryId() != null)
    {
      return null;
    }
    return new SearchIndexChange(ChangeType.ORGANIZATION, entity.getId());
  }

  /**
   * @param ownerType if known, specify the OwnerType here for improved performance.
   * @return all organization ancestors of the specified owner, in order from the bottom up. If the specified owner
   *         is itself an organization, it is included in the returned collection.
   */
  public List<Organization> getAllParentOrganizations(TransactionContext tx, String ownerId, OwnerType ownerType) {
    String sQuery = "SELECT org FROM Organization org, OwnerAncestor oa " +
        "WHERE org.id = oa.ancestorId AND oa.id = ?1 " +
        "AND oa.ancestorType = com.sonatype.insight.brain.model.OwnerType.ORGANIZATION " +
        (ownerType == null ? "" : "AND oa.ownerType = ?2 ") +
        "ORDER BY oa.ancestorDistance";

    if (ownerType == null) {
      return getList(tx, sQuery, ownerId);
    }
    else {
      return getList(tx, sQuery, ownerId, ownerType);
    }
  }

  /**
   * @param ownerType if known, specify the OwnerType here for improved performance, this assumes all IDs in your
   *          list are of the same owner type
   *
   * @return all organization ancestors of the specified owners. If the specified owner
   *         is itself an organization, it is included in the returned collection.
   */
  public List<Organization> getAllParentOrganizations(List<String> ownerIds, OwnerType ownerType) {
    String sQuery = "SELECT org FROM Organization org, OwnerAncestor oa " +
        "WHERE org.id = oa.ancestorId AND oa.id in (?1) " +
        "AND oa.ancestorType = com.sonatype.insight.brain.model.OwnerType.ORGANIZATION " +
        (ownerType == null ? "" : "AND oa.ownerType = ?2 ");

    if (ownerType == null) {
      return getListWithSqlInClause(ownerIds, c -> getList(sQuery, c));
    }
    else {
      return getListWithSqlInClause(ownerIds, c -> getList(sQuery, c, ownerType));
    }
  }

  public List<Organization> getAllParentOrganizations(String ownerId, OwnerType ownerType) {
    try (TransactionContext tx = createTransactionContext()) {
      return getAllParentOrganizations(tx, ownerId, ownerType);
    }
  }

  /**
   * @return all organization descendants of the specified owner, in order from the top down. If the specified owner
   *         is itself an organization, it is included in the returned collection. The relative order of returned
   *         organizations
   *         that are at the same level in the tree is unspecified, but all organizations at a given level will be
   *         returned
   *         before organizations from a lower level.
   */
  public List<Organization> getAllChildOrganizations(TransactionContext tx, String ownerId) {
    String sQuery = "SELECT org FROM Organization org, OrganizationAncestor oa " +
        "WHERE org.id = oa.organizationId AND oa.ancestorId = ?1 " +
        "ORDER BY oa.ancestorDistance";

    return getList(tx, sQuery, ownerId);
  }

  public List<Organization> getAllChildOrganizations(String ownerId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getAllChildOrganizations(tx, ownerId);
    }
  }

  private void insertOrganizationAncestors(TransactionContext tx, Organization org) {
    String orgId = org.getId();
    int i = 0;
    Organization current = org;

    while (current != null) {
      organizationAncestorDAO.insert(tx, new OrganizationAncestor(orgId, current.getId(), i));

      current = current.getParentOwnerId() == null ? null : getById(tx, current.getParentOwnerId());
      i++;
    }
  }

  /**
   * @param org an Organization that has had its parent id updated and which needs its (and its
   *          children's) OrganizationAncestors updated to match
   */
  private void updateOrganizationAncestors(TransactionContext tx, Organization organization) {
    for (Organization org : getAllChildOrganizations(tx, organization.getId())) {
      List<OrganizationAncestor> orgAncestors = organizationAncestorDAO.getByOrganizationId(tx, org.getId());
      for (OrganizationAncestor orgAncestor : orgAncestors) {
        organizationAncestorDAO.delete(tx, orgAncestor);
      }

      insertOrganizationAncestors(tx, org);
    }
  }
}
