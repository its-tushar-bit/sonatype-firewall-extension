/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.persistence.EntityManager;

import com.sonatype.insight.brain.common.io.FileCleaner;
import com.sonatype.insight.brain.dataaccess.label.LabelDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseOverrideDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyMonitoringDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO;
import com.sonatype.insight.brain.dataaccess.security.MembershipMappingDAO;
import com.sonatype.insight.brain.dataaccess.tag.ApplicationTagDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.InvalidNameException;
import com.sonatype.insight.brain.model.NameHelper;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.brain.model.license.LicenseOverride;
import com.sonatype.insight.brain.model.license.LicenseThreatGroup;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyMonitoring;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.model.tag.ApplicationTag;
import com.sonatype.insight.error.exception.NotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ApplicationDAO
    extends AbstractOperationalSqlDAO<Application>
{
  private static final Logger log = LoggerFactory.getLogger(ApplicationDAO.class);

  @Override
  public Application getById(EntityManager em, String id) {
    String sQuery = "SELECT entity FROM Application entity" + //
        " WHERE entity.id=?1";
    return get(em, sQuery, id);
  }

  public Application getByIdNotNull(String id) {
    Application application = getById(id);
    if (application == null) {
      throw new NotFoundException("Could not find an application with id " + id + ".");
    }
    return application;
  }

  public Application getByPublicId(EntityManager em, String publicId) {
    if (publicId == null || publicId.trim().isEmpty()) {
      throw new DataAccessException("The application public ID cannot be null or empty.");
    }

    publicId = publicId.trim().toLowerCase(Locale.ENGLISH);
    String sQuery = "SELECT entity FROM Application entity" + //
        " WHERE entity.publicIdLowercase=?1";
    return get(em, sQuery, publicId);
  }

  public Application getByPublicId(String publicId) {
    EntityManager em = createEntityManager();
    try {
      return getByPublicId(em, publicId);
    }
    finally {
      close(em);
    }
  }

  public Application getByPublicIdNotNull(String publicId) {
    EntityManager em = createEntityManager();
    try {
      return getByPublicIdNotNull(em, publicId);
    }
    finally {
      close(em);
    }
  }

  public Application getByPublicIdNotNull(EntityManager em, String publicId) {
    Application application = getByPublicId(em, publicId);
    if (application == null) {
      throw new NotFoundException("Could not find an application with public id " + publicId + ".");
    }
    return application;
  }

  public Application getByName(EntityManager em, String name) {
    if (name == null || name.trim().isEmpty()) {
      throw new DataAccessException("The application name cannot be null or empty.");
    }
    // Application Name is whitespace and case insensitive
    name = NameHelper.normalize(name);
    String sQuery = "SELECT entity FROM Application entity WHERE entity.nameLowercaseNoWhitespace=?1";
    return get(em, sQuery, name);
  }

  public Application getByName(String name) {
    EntityManager em = createEntityManager();
    try {
      return getByName(em, name);
    }
    finally {
      close(em);
    }
  }

  public List<Application> getByContactInternalName(String contactInternalName) {
    String sQuery = "SELECT entity FROM Application entity WHERE entity.contactInternalName=?1";
    return getList(sQuery, contactInternalName);
  }

  public List<Application> getAll(EntityManager em) {
    String sQuery = "SELECT entity FROM Application entity" + //
        " ORDER BY entity.publicIdLowercase";
    return getList(em, sQuery);
  }

  public List<Application> getAll() {
    EntityManager em = createEntityManager();
    try {
      return getAll(em);
    }
    finally {
      close(em);
    }
  }

  public List<Application> getByOrganizationId(EntityManager em, String organizationId) {
    String sQuery = "SELECT entity FROM Application entity" + //
        " WHERE entity.organizationId=?1" + //
        " ORDER BY entity.publicIdLowercase";
    return getList(em, sQuery, organizationId);
  }

  public List<Application> getByOrganizationId(String organizationId) {
    EntityManager em = createEntityManager();
    try {
      return getByOrganizationId(em, organizationId);
    }
    finally {
      close(em);
    }
  }

  @Override
  public void insert(EntityManager em, Application application) {
    validate(application);

    if (getByName(em, application.getName()) != null) {
      throw new InvalidNameException(application.getName() + " is already used as a name.");
    }
    if (getByPublicId(em, application.getPublicId()) != null) {
      throw new InvalidApplicationException(application.getPublicId() + " is already used as an ID.");
    }

    super.insert(em, application);
  }

  @Override
  public void update(EntityManager em, Application application) {
    validate(application);

    Application existingApplication = getById(em, application.getId());
    if (existingApplication == null) {
      throw new InvalidApplicationException("Attempting to edit an application that doesn't exist. ID "
          + application.getPublicId());
    }
    if (!existingApplication.getPublicId().equals(application.getPublicId())) {
      throw new InvalidApplicationException("Cannot change Public ID of existing application.");
    }
    if (!existingApplication.getOrganizationId().equals(application.getOrganizationId())) {
      throw new InvalidApplicationException("Cannot change the parent organization of an application.");
    }
    Organization organization = new OrganizationDAO().getByIdNotNull(application.getOrganizationId());
    checkConflictingLicenseThreatGroups(em, application, organization);
    checkConflictingLabels(em, existingApplication, organization);
    existingApplication = getByName(em, application.getName());
    if (existingApplication != null && !existingApplication.getId().equals(application.getId())) {
      throw new InvalidNameException(application.getName() + " is already used as a name.");
    }
    existingApplication = getByPublicId(em, application.getPublicId());
    if (existingApplication != null && !existingApplication.getId().equals(application.getId())) {
      throw new InvalidApplicationException(application.getPublicId() + " is already used as an ID.");
    }

    super.update(em, application);
  }

  private void checkConflictingLicenseThreatGroups(EntityManager em, Application application, Organization organization)
  {
    LicenseThreatGroupDAO licenseThreatGroupDAO = new LicenseThreatGroupDAO();
    List<LicenseThreatGroup> appLicenseThreatGroups = licenseThreatGroupDAO.getByOwnerId(em, application.getId());
    for (LicenseThreatGroup appLicenseThreatGroup : appLicenseThreatGroups) {
      if (licenseThreatGroupDAO.getByOwnerIdAndName(em, organization.getId(), appLicenseThreatGroup.getName()) != null) {
        throw new InvalidApplicationException(
            "Both the application and the organization have a license threat group with the same name '"
                + appLicenseThreatGroup.getName() + "'");
      }
    }
  }

  private void checkConflictingLabels(EntityManager em, Application application, Organization organization) {
    final List<Label> conflicts = new ArrayList<Label>();
    final LabelDAO labelDAO = new LabelDAO();
    for (Label appLabel : labelDAO.getByOwnerId(em, application.getId())) {
      if (labelDAO.getByOwnerIdAndLabelLowercase(em, organization.getId(), appLabel.getLabelLowercase()) != null) {
        conflicts.add(appLabel);
      }
    }
    if (!conflicts.isEmpty()) {
      final StringBuilder msg = new StringBuilder(
          "Both the application and the organization have labels with the same names." + " Conflicting label names :");
      for (Label conflict : conflicts) {
        msg.append(" '").append(conflict.getLabelLowercase()).append('\'');
      }
      throw new InvalidApplicationException(msg.toString());
    }
  }

  public void deleteWithIcon(Application application, File iconDirectory) {
    EntityManager em = createEntityManager();
    try {
      em.getTransaction().begin();
      deleteWithIcon(em, application, iconDirectory);
      em.getTransaction().commit();
    }
    finally {
      close(em);
    }
  }

  public void deleteWithIcon(EntityManager em, Application application, File iconDirectory) {
    File applicationIconDirectory = new File(iconDirectory, application.getId());
    try {
      new FileCleaner().delete(applicationIconDirectory);
    }
    catch (IOException e) {
      log.error("Could not delete application icons: {}" + applicationIconDirectory, e);
    }

    delete(em, application);
  }

  @Override
  public void delete(EntityManager em, Application application) {
    // Cascade to license threat groups
    LicenseThreatGroupDAO licenseThreatGroupDAO = new LicenseThreatGroupDAO();
    List<LicenseThreatGroup> licenseThreatGroups = licenseThreatGroupDAO.getByOwnerId(em, application.getId());
    for (LicenseThreatGroup licenseThreatGroup : licenseThreatGroups) {
      licenseThreatGroupDAO.delete(em, licenseThreatGroup);
    }

    // Cascade to labels
    LabelDAO labelDAO = new LabelDAO();
    List<Label> labels = labelDAO.getByOwnerId(em, application.getId());
    for (Label label : labels) {
      labelDAO.delete(em, label);
    }

    // Cascade to policy evaluations
    PolicyEvaluationDAO policyEvaluationDAO = new PolicyEvaluationDAO();
    for (PolicyEvaluation policyEvaluation : policyEvaluationDAO.getByApplicationId(em, application.getId())) {
      policyEvaluationDAO.delete(em, policyEvaluation);
    }

    // Cascade to policies
    new PolicyDAO().deleteByOwnerId(em, application.getId());

    // Cascade to policy waivers
    PolicyWaiverDAO policyWaiverDAO = new PolicyWaiverDAO();
    List<PolicyWaiver> policyWaivers = policyWaiverDAO.getByOwnerId(em, application.getId());
    for (PolicyWaiver policyWaiver : policyWaivers) {
      policyWaiverDAO.delete(em, policyWaiver);
    }

    // Cascade to license overrides
    LicenseOverrideDAO licenseOverrideDAO = new LicenseOverrideDAO();
    List<LicenseOverride> licenseOverrides = licenseOverrideDAO.getByOwnerId(em, application.getId());
    for (LicenseOverride licenseOverride : licenseOverrides) {
      licenseOverrideDAO.delete(em, licenseOverride);
    }

    // Cascade to membership mappings
    MembershipMappingDAO membershipMappingDAO = new MembershipMappingDAO();
    for (MembershipMapping membershipMapping : membershipMappingDAO.getByContextId(em, application.getId())) {
      membershipMappingDAO.delete(em, membershipMapping);
    }

    // Cascade to policy monitoring
    PolicyMonitoringDAO policyMonitoringDAO = new PolicyMonitoringDAO();
    PolicyMonitoring policyMonitoring = policyMonitoringDAO.getByOwnerId(em, application.getId());
    if (policyMonitoring != null) {
      policyMonitoringDAO.delete(em, policyMonitoring);
    }

    // Cascade to applied tags
    ApplicationTagDAO applicationTagDAO = new ApplicationTagDAO();
    List<ApplicationTag> appTags = applicationTagDAO.getByApplicationId(em, application.getId());
    for (ApplicationTag appTag : appTags) {
      applicationTagDAO.delete(em, appTag);
    }

    super.delete(em, application);
  }

  private void validate(Application application) {
    NameHelper.validate(application.getName());

    final String applicationPublicId = application.getPublicId();
    if (applicationPublicId == null || applicationPublicId.trim().isEmpty()) {
      throw new InvalidApplicationException("ID is required.");
    }
  }

  public List<Application> getByOrganizationIdAndLabelLowercase(EntityManager em, String organizationId,
      String labelLowercase)
  {
    final String oQuery = "SELECT app FROM Label label, Application app" + //
        " WHERE label.ownerId=app.id AND app.organizationId=?1" + //
        "    AND label.labelLowercase=?2";
    return getList(em, oQuery, organizationId, labelLowercase);
  }
}
