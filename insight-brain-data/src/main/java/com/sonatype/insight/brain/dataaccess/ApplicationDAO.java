/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

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
import com.sonatype.insight.brain.model.ApplicationComponent;
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
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.NotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ApplicationDAO
    extends AbstractOperationalSqlDAO<Application>
{
  private static final Logger log = LoggerFactory.getLogger(ApplicationDAO.class);

  private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s");

  @Override
  public Application getById(TransactionContext tx, String id) {
    String sQuery = "SELECT entity FROM Application entity" + //
        " WHERE entity.id=?1";
    return get(tx, sQuery, id);
  }

  public Application getByIdNotNull(String id) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByIdNotNull(tx, id);
    }
  }

  public Application getByIdNotNull(TransactionContext tx, String id) {
    Application application = getById(tx, id);
    if (application == null) {
      throw new NotFoundException("Could not find an application with ID " + id + ".");
    }
    return application;
  }

  public Application getByPublicId(TransactionContext tx, String publicId) {
    if (publicId == null || publicId.trim().isEmpty()) {
      throw new DataAccessException("The application public ID cannot be null or empty.");
    }

    publicId = publicId.trim().toLowerCase(Locale.ENGLISH);
    String sQuery = "SELECT entity FROM Application entity" + //
        " WHERE entity.publicIdLowercase=?1";
    return get(tx, sQuery, publicId);
  }

  public Application getByPublicId(String publicId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByPublicId(tx, publicId);
    }
  }

  public Application getByPublicIdNotNull(String publicId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByPublicIdNotNull(tx, publicId);
    }
  }

  public Application getByPublicIdNotNull(TransactionContext tx, String publicId) {
    Application application = getByPublicId(tx, publicId);
    if (application == null) {
      throw new NotFoundException("Could not find an application with public ID " + publicId + ".");
    }
    return application;
  }

  public Application getByName(TransactionContext tx, String name) {
    if (name == null || name.trim().isEmpty()) {
      throw new DataAccessException("The application name cannot be null or empty.");
    }
    // Application Name is whitespace and case insensitive
    name = NameHelper.normalize(name);
    String sQuery = "SELECT entity FROM Application entity WHERE entity.nameLowercaseNoWhitespace=?1";
    return get(tx, sQuery, name);
  }

  public Application getByName(String name) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByName(tx, name);
    }
  }

  public List<Application> getByContactInternalName(String contactInternalName) {
    String sQuery = "SELECT entity FROM Application entity WHERE entity.contactInternalName=?1";
    return getList(sQuery, contactInternalName);
  }

  public List<Application> getAll(TransactionContext tx) {
    String sQuery = "SELECT entity FROM Application entity" + //
        " ORDER BY entity.publicIdLowercase";
    return getList(tx, sQuery);
  }

  public List<Application> getAll() {
    try (TransactionContext tx = createTransactionContext()) {
      return getAll(tx);
    }
  }

  public List<Application> getByOrganizationId(TransactionContext tx, String organizationId) {
    String sQuery = "SELECT entity FROM Application entity" + //
        " WHERE entity.organizationId=?1" + //
        " ORDER BY entity.publicIdLowercase";
    return getList(tx, sQuery, organizationId);
  }

  public List<Application> getByOrganizationId(String organizationId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByOrganizationId(tx, organizationId);
    }
  }

  public List<Application> getByIdsAndTagIds(Set<String> applicationIds, Set<String> tagIds) {
    String sQuery = "SELECT DISTINCT application FROM Application application, ApplicationTag applicationTag" + //
        " WHERE application.id = applicationTag.applicationId" + //
        " AND application.id IN (?1)" +
        " AND applicationTag.tagId IN (?2)";
    return getList(sQuery, applicationIds, tagIds);
  }

  public List<Application> getByTagIds(Set<String> tagIds) {
    String sQuery = "SELECT application FROM Application application, ApplicationTag applicationTag" + //
        " WHERE application.id = applicationTag.applicationId" + //
        " AND applicationTag.tagId IN (?1)";
    return getList(sQuery, tagIds);
  }

  public List<Application> getByPublicIds(Set<String> applicationPublicIds) {
    String sQuery = "SELECT entity FROM Application entity" + //
        " WHERE entity.publicId IN (?1)";
    return getList(sQuery, applicationPublicIds);
  }

  public List<Application> getByIds(Set<String> applicationIds) {
    String sQuery = "SELECT entity FROM Application entity" + //
        " WHERE entity.id IN (?1)";
    return getList(sQuery, applicationIds);
  }

  @Override
  public void insert(TransactionContext tx, Application application) {
    validate(application);
    validatePublicId(application.getPublicId());

    if (getByName(tx, application.getName()) != null) {
      throw new InvalidNameException(application.getName() + " is already used as a name.");
    }
    if (getByPublicId(tx, application.getPublicId()) != null) {
      throw new InvalidApplicationException(application.getPublicId() + " is already used as an ID.");
    }

    super.insert(tx, application);
  }

  @Override
  public void update(TransactionContext tx, Application application) {
    validate(application);

    Application existingApplication = getById(tx, application.getId());
    if (existingApplication == null) {
      throw new InvalidApplicationException("Attempting to edit an application that doesn't exist. ID : "
          + application.getPublicId() + ".");
    }
    if (!existingApplication.getPublicId().equals(application.getPublicId())) {
      throw new InvalidApplicationException("Cannot change public ID of existing application.");
    }
    if (!existingApplication.getOrganizationId().equals(application.getOrganizationId())) {
      throw new InvalidApplicationException("Cannot change the parent organization of an application.");
    }
    Organization organization = new OrganizationDAO().getByIdNotNull(application.getOrganizationId());
    checkConflictingLicenseThreatGroups(tx, application, organization);
    checkConflictingLabels(tx, existingApplication, organization);
    existingApplication = getByName(tx, application.getName());
    if (existingApplication != null && !existingApplication.getId().equals(application.getId())) {
      throw new InvalidNameException(application.getName() + " is already used as a name.");
    }
    existingApplication = getByPublicId(tx, application.getPublicId());
    if (existingApplication != null && !existingApplication.getId().equals(application.getId())) {
      throw new InvalidApplicationException(application.getPublicId() + " is already used as an ID.");
    }

    super.update(tx, application);
  }

  private void checkConflictingLicenseThreatGroups(TransactionContext tx, Application application, Organization organization)
  {
    LicenseThreatGroupDAO licenseThreatGroupDAO = new LicenseThreatGroupDAO();
    List<LicenseThreatGroup> appLicenseThreatGroups = licenseThreatGroupDAO.getByOwnerId(tx, application.getId());
    for (LicenseThreatGroup appLicenseThreatGroup : appLicenseThreatGroups) {
      if (licenseThreatGroupDAO.getByOwnerIdAndName(tx, organization.getId(), appLicenseThreatGroup.getName()) != null) {
        throw new InvalidApplicationException(
            "Both the application and the organization have a license threat group with the same name '"
                + appLicenseThreatGroup.getName() + "'.");
      }
    }
  }

  private void checkConflictingLabels(TransactionContext tx, Application application, Organization organization) {
    final List<Label> conflicts = new ArrayList<>();
    final LabelDAO labelDAO = new LabelDAO();
    for (Label appLabel : labelDAO.getByOwnerId(tx, application.getId())) {
      if (labelDAO.getByOwnerIdAndLabelLowercase(tx, organization.getId(), appLabel.getLabelLowercase()) != null) {
        conflicts.add(appLabel);
      }
    }
    if (!conflicts.isEmpty()) {
      final StringBuilder msg = new StringBuilder(
          "Both the application and the organization have labels with the same names. Conflicting label names :");
      for (Label conflict : conflicts) {
        msg.append(" '").append(conflict.getLabelLowercase()).append('\'');
      }
      msg.append(".");
      throw new InvalidApplicationException(msg.toString());
    }
  }

  public void deleteWithIcon(Application application, File iconDirectory) {
    try (TransactionContext tx = createTransactionContext()) {
      tx.begin();
      deleteWithIcon(tx, application, iconDirectory);
      tx.commit();
    }
  }

  public void deleteWithIcon(TransactionContext tx, Application application, File iconDirectory) {
    File applicationIconDirectory = new File(iconDirectory, application.getId());
    try {
      new FileCleaner().delete(applicationIconDirectory);
    }
    catch (IOException e) {
      log.error("Could not delete application icons: {}" + applicationIconDirectory, e);
    }

    delete(tx, application);
  }

  @Override
  public void delete(TransactionContext tx, Application application) {
    // Cascade to license threat groups
    LicenseThreatGroupDAO licenseThreatGroupDAO = new LicenseThreatGroupDAO();
    List<LicenseThreatGroup> licenseThreatGroups = licenseThreatGroupDAO.getByOwnerId(tx, application.getId());
    for (LicenseThreatGroup licenseThreatGroup : licenseThreatGroups) {
      licenseThreatGroupDAO.delete(tx, licenseThreatGroup);
    }

    // Cascade to labels
    LabelDAO labelDAO = new LabelDAO();
    List<Label> labels = labelDAO.getByOwnerId(tx, application.getId());
    for (Label label : labels) {
      labelDAO.delete(tx, label);
    }

    // Cascade to policy evaluations
    PolicyEvaluationDAO policyEvaluationDAO = new PolicyEvaluationDAO();
    for (PolicyEvaluation policyEvaluation : policyEvaluationDAO.getByApplicationId(tx, application.getId())) {
      policyEvaluationDAO.delete(tx, policyEvaluation);
    }

    // Cascade to policies
    new PolicyDAO().deleteByOwnerId(tx, application.getId());

    // Cascade to policy waivers
    PolicyWaiverDAO policyWaiverDAO = new PolicyWaiverDAO();
    List<PolicyWaiver> policyWaivers = policyWaiverDAO.getByOwnerId(tx, application.getId());
    for (PolicyWaiver policyWaiver : policyWaivers) {
      policyWaiverDAO.delete(tx, policyWaiver);
    }

    // Cascade to license overrides
    LicenseOverrideDAO licenseOverrideDAO = new LicenseOverrideDAO();
    List<LicenseOverride> licenseOverrides = licenseOverrideDAO.getByOwnerId(tx, application.getId());
    for (LicenseOverride licenseOverride : licenseOverrides) {
      licenseOverrideDAO.delete(tx, licenseOverride);
    }

    // Cascade to membership mappings
    MembershipMappingDAO membershipMappingDAO = new MembershipMappingDAO();
    for (MembershipMapping membershipMapping : membershipMappingDAO.getByContextId(tx, application.getId())) {
      membershipMappingDAO.delete(tx, membershipMapping);
    }

    // Cascade to policy monitoring
    PolicyMonitoringDAO policyMonitoringDAO = new PolicyMonitoringDAO();
    PolicyMonitoring policyMonitoring = policyMonitoringDAO.getByOwnerId(tx, application.getId());
    if (policyMonitoring != null) {
      policyMonitoringDAO.delete(tx, policyMonitoring);
    }

    // Cascade to applied tags
    ApplicationTagDAO applicationTagDAO = new ApplicationTagDAO();
    List<ApplicationTag> appTags = applicationTagDAO.getByApplicationId(tx, application.getId());
    for (ApplicationTag appTag : appTags) {
      applicationTagDAO.delete(tx, appTag);
    }

    // Cascade to components
    ApplicationComponentDAO applicationComponentDAO = new ApplicationComponentDAO();
    List<ApplicationComponent> appComponents = applicationComponentDAO.getByApplicationId(tx, application.getId());
    for (ApplicationComponent appComponent : appComponents) {
      applicationComponentDAO.delete(tx, appComponent);
    }

    super.delete(tx, application);
  }

  private void validate(Application application) {
    NameHelper.validate(application.getName());
  }

  private void validatePublicId(String publicId) {
    NameHelper.validate("Public ID", publicId);
    if (WHITESPACE_PATTERN.matcher(publicId).find()) {
      throw new InvalidApplicationException("Public ID cannot contain whitespaces.");
    }
  }

  public List<Application> getByOrganizationIdAndLabelLowercase(TransactionContext tx, String organizationId,
      String labelLowercase)
  {
    final String oQuery = "SELECT app FROM Label label, Application app" + //
        " WHERE label.ownerId=app.id AND app.organizationId=?1" + //
        "    AND label.labelLowercase=?2";
    return getList(tx, oQuery, organizationId, labelLowercase);
  }
}
