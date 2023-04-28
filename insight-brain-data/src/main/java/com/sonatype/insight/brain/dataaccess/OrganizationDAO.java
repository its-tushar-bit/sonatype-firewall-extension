/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.dataaccess.configuration.AutomaticApplicationsConfigurationDAO;
import com.sonatype.insight.brain.dataaccess.configuration.ProprietaryConfigDAO;
import com.sonatype.insight.brain.dataaccess.label.LabelDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryConnectionDAO;
import com.sonatype.insight.brain.dataaccess.security.MembershipMappingDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlDAO;
import com.sonatype.insight.brain.dataaccess.tag.TagDAO;
import com.sonatype.insight.brain.model.InvalidNameException;
import com.sonatype.insight.brain.model.NameHelper;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.SearchIndexChange;
import com.sonatype.insight.brain.model.SearchIndexChange.ChangeType;
import com.sonatype.insight.brain.model.configuration.ProprietaryConfig;
import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.brain.model.license.LicenseThreatGroup;
import com.sonatype.insight.brain.model.policy.InvalidStageException;
import com.sonatype.insight.brain.model.repository.RepositoryConnection;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OrganizationDAO
    extends AbstractOperationalSqlDAO<Organization>
{
  private static final Logger log = LoggerFactory.getLogger(OrganizationDAO.class);

  @Override
  public Organization getById(TransactionContext tx, String id) {
    String sQuery = "SELECT entity FROM Organization entity" + //
        " WHERE entity.id=?1";
    return get(tx, sQuery, id);
  }

  public Organization getByIdNotNull(String id) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByIdNotNull(tx, id);
    }
  }

  public Organization getByIdNotNull(TransactionContext tx, String id) {
    Organization organization = getById(tx, id);
    if (organization == null) {
      throw new NotFoundException("Cannot find organization with ID " + id + ".");
    }
    return organization;
  }

  private Organization getByName(TransactionContext tx, String name) {
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

  public Organization getByName(String name) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByName(tx, name);
    }
  }

  public List<Organization> getAll() {
    String sQuery = "SELECT entity FROM Organization entity" + //
        " ORDER BY entity.name";
    return getList(sQuery);
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

    /*
     * Check that the stage id is valid or null
     */
    if (organization.getWaivedComponentUpgradeStageTypeId() != null
        && !Stage.isValidStageTypeId(organization.getWaivedComponentUpgradeStageTypeId())) {
      throw new InvalidStageException(organization.getWaivedComponentUpgradeStageTypeId());
    }

    super.insert(tx, organization);
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
    Organization existingOrganization = getByName(tx, organization.getName());
    if (existingOrganization != null && !existingOrganization.getId().equals(organization.getId())) {
      throw new InvalidNameException(organization.getName() + " is already used as a name.");
    }

    /*
     * Check that the stage id is valid or null
     */
    if (organization.getWaivedComponentUpgradeStageTypeId() != null
        && !Stage.isValidStageTypeId(organization.getWaivedComponentUpgradeStageTypeId())) {
      throw new InvalidStageException(organization.getWaivedComponentUpgradeStageTypeId());
    }

    super.update(tx, organization);
  }

  @Override
  public void delete(TransactionContext tx, Organization organization) {
    long start = System.currentTimeMillis();

    if (Organization.ROOT_ORGANIZATION_ID.equals(organization.getId())) {
      // Do not allow the deletion of the root organization
      throw new BadRequestException("Cannot delete the root organization: " + organization.getName());
    }

    AutomaticApplicationsConfigurationDAO automaticApplicationsConfigurationDAO =
        new AutomaticApplicationsConfigurationDAO();
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
    LicenseThreatGroupDAO licenseThreatGroupDAO = new LicenseThreatGroupDAO();
    List<LicenseThreatGroup> licenseThreatGroups = licenseThreatGroupDAO.getByOwnerId(tx, organization.getId());
    for (LicenseThreatGroup licenseThreatGroup : licenseThreatGroups) {
      licenseThreatGroupDAO.delete(tx, licenseThreatGroup);
    }

    // Cascade to labels
    LabelDAO labelDAO = new LabelDAO();
    List<Label> labels = labelDAO.getByOwnerId(tx, organization.getId());
    for (Label label : labels) {
      labelDAO.delete(tx, label);
    }

    // Cascade to policies
    new PolicyDAO().deleteByOwnerId(tx, organization.getId());

    // Cascade to membership mappings
    MembershipMappingDAO membershipMappingDAO = new MembershipMappingDAO();
    for (MembershipMapping membershipMapping : membershipMappingDAO.getByContextId(tx, organization.getId())) {
      membershipMappingDAO.delete(tx, membershipMapping);
    }

    // Cascade to owned entities
    new OwnerDAO().cascadeDelete(tx, organization);

    // Cascade to tags
    TagDAO tagDAO = new TagDAO();
    List<Tag> tags = tagDAO.getByOrganizationId(tx, organization.getId());
    for (Tag tag : tags) {
      tagDAO.delete(tx, tag);
    }

    // Cascade to proprietary config
    ProprietaryConfigDAO proprietaryConfigDAO = new ProprietaryConfigDAO();
    ProprietaryConfig proprietaryConfig = proprietaryConfigDAO.getByOwnerId(tx, organization.getId());
    if (proprietaryConfig != null) {
      proprietaryConfigDAO.delete(tx, proprietaryConfig);
    }

    // Cascade to SourceControl config
    new SourceControlDAO().deleteByOwnerId(tx, organization.getId());

    // Cascade to locks
    ClusterLock.deleteForAuditJsonFileStore(tx, organization.getId());

    // Cascade to repository connections
    RepositoryConnectionDAO repositoryConnectionDAO = new RepositoryConnectionDAO();
    for (RepositoryConnection repositoryConnection : repositoryConnectionDAO.getByOwnerId(tx, organization.getId())) {
      repositoryConnectionDAO.delete(tx, repositoryConnection);
    }

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
    return new SearchIndexChange(ChangeType.ORGANIZATION, entity.getId());
  }
}
