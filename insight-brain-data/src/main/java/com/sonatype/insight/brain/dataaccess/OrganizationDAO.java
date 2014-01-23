/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess;

import java.util.List;

import javax.persistence.EntityManager;

import com.sonatype.insight.brain.dataaccess.label.LabelDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseOverrideDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyMonitoringDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO;
import com.sonatype.insight.brain.dataaccess.security.MembershipMappingDAO;
import com.sonatype.insight.brain.dataaccess.tag.TagDAO;
import com.sonatype.insight.brain.model.InvalidNameException;
import com.sonatype.insight.brain.model.NameHelper;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.brain.model.license.LicenseOverride;
import com.sonatype.insight.brain.model.license.LicenseThreatGroup;
import com.sonatype.insight.brain.model.policy.PolicyMonitoring;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.error.exception.NotFoundException;

public class OrganizationDAO
    extends AbstractOperationalSqlDAO<Organization>
{
  @Override
  public Organization getById(EntityManager em, String id) {
    String sQuery = "SELECT entity FROM Organization entity" + //
        " WHERE entity.id=?1";
    return get(em, sQuery, id);
  }

  public Organization getByIdNotNull(String id) {
    EntityManager em = createEntityManager();
    try {
      return getByIdNotNull(em, id);
    }
    finally {
      close(em);
    }
  }

  public Organization getByIdNotNull(EntityManager em, String id) {
    Organization organization = getById(em, id);
    if (organization == null) {
      throw new NotFoundException("Cannot find organization with id " + id + ".");
    }
    return organization;
  }

  private Organization getByName(EntityManager em, String name) {
    if (name == null || name.trim().isEmpty()) {
      throw new DataAccessException("The organization name cannot be null or empty.");
    }
    // Organization Name is whitespace and case insensitive
    name = NameHelper.normalize(name);
    String sQuery = "SELECT entity FROM Organization entity WHERE entity.nameLowercaseNoWhitespace=?1";
    return get(em, sQuery, name);
  }

  public Organization getByName(String name) {
    EntityManager em = createEntityManager();
    try {
      return getByName(em, name);
    }
    finally {
      close(em);
    }
  }

  public List<Organization> getAll() {
    String sQuery = "SELECT entity FROM Organization entity" + //
        " ORDER BY entity.name";
    return getList(sQuery);
  }

  @Override
  public void insert(EntityManager em, Organization organization) {
    insert(em, organization, true /* createLicenseThreatGroups */);
  }

  private void insert(EntityManager em, Organization organization, boolean createLicenseThreatGroups) {
    NameHelper.validate(organization.getName());

    if (getByName(em, organization.getName()) != null) {
      throw new InvalidNameException(organization.getName() + " is already used as a name.");
    }

    super.insert(em, organization);

    if (createLicenseThreatGroups) {
      new LicenseThreatGroupDAO().createDefaultGroups(em, organization.getId());
    }
  }

  @Override
  public void insert(Organization entity) {
    insert(entity, true /* createLicenseThreatGroups */);
  }

  public void insert(Organization entity, boolean createLicenseThreatGroups) {
    EntityManager em = createEntityManager();
    try {
      em.getTransaction().begin();
      insert(em, entity, createLicenseThreatGroups);
      em.getTransaction().commit();
    }
    finally {
      close(em);
    }
  }

  @Override
  public void update(EntityManager em, Organization organization) {
    NameHelper.validate(organization.getName());

    Organization existingOrganization = getByName(em, organization.getName());
    if (existingOrganization != null && !existingOrganization.getId().equals(organization.getId())) {
      throw new InvalidNameException(organization.getName() + " is already used as a name.");
    }

    super.update(em, organization);
  }

  @Override
  public void delete(EntityManager em, Organization organization) {
    // Cascade to license threat groups
    LicenseThreatGroupDAO licenseThreatGroupDAO = new LicenseThreatGroupDAO();
    List<LicenseThreatGroup> licenseThreatGroups = licenseThreatGroupDAO.getByOwnerId(em, organization.getId());
    for (LicenseThreatGroup licenseThreatGroup : licenseThreatGroups) {
      licenseThreatGroupDAO.delete(em, licenseThreatGroup);
    }

    // Cascade to labels
    LabelDAO labelDAO = new LabelDAO();
    List<Label> labels = labelDAO.getByOwnerId(em, organization.getId());
    for (Label label : labels) {
      labelDAO.delete(em, label);
    }

    // Cascade to policy waivers
    PolicyWaiverDAO policyWaiverDAO = new PolicyWaiverDAO();
    List<PolicyWaiver> policyWaivers = policyWaiverDAO.getByOwnerId(em, organization.getId());
    for (PolicyWaiver policyWaiver : policyWaivers) {
      policyWaiverDAO.delete(em, policyWaiver);
    }

    // Cascade to license overrides
    LicenseOverrideDAO licenseOverrideDAO = new LicenseOverrideDAO();
    List<LicenseOverride> licenseOverrides = licenseOverrideDAO.getByOwnerId(em, organization.getId());
    for (LicenseOverride licenseOverride : licenseOverrides) {
      licenseOverrideDAO.delete(em, licenseOverride);
    }

    // Cascade to membership mappings
    MembershipMappingDAO membershipMappingDAO = new MembershipMappingDAO();
    for (MembershipMapping membershipMapping : membershipMappingDAO.getByContextId(em, organization.getId())) {
      membershipMappingDAO.delete(em, membershipMapping);
    }

    // Cascade to policy monitoring
    PolicyMonitoringDAO policyMonitoringDAO = new PolicyMonitoringDAO();
    PolicyMonitoring policyMonitoring = policyMonitoringDAO.getByOwnerId(em, organization.getId());
    if (policyMonitoring != null) {
      policyMonitoringDAO.delete(em, policyMonitoring);
    }

    // Cascade to tags
    TagDAO tagDAO = new TagDAO();
    List<Tag> tags = tagDAO.getByOrganizationId(em, organization.getId());
    for (Tag tag : tags) {
      tagDAO.delete(em, tag);
    }

    super.delete(em, organization);
  }
}
