/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.organization;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.insight.brain.api.v2.ApiApplicationAdapter;
import com.sonatype.insight.brain.api.v2.dto.ApiApplicationDTO;
import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.label.ComponentLabelDAO;
import com.sonatype.insight.brain.dataaccess.label.LabelDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseOverrideDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupLicenseDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyMonitoringDAO;
import com.sonatype.insight.brain.dataaccess.security.MembershipMappingDAO;
import com.sonatype.insight.brain.dataaccess.vulnerability.SecurityVulnerabilityOverrideDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.label.ComponentLabel;
import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.brain.model.license.LicenseOverride;
import com.sonatype.insight.brain.model.license.LicenseThreatGroup;
import com.sonatype.insight.brain.model.license.LicenseThreatGroupLicense;
import com.sonatype.insight.brain.model.policy.PolicyMonitoring;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.vulnerability.SecurityVulnerabilityOverride;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.model.HasStringId;

import org.apache.openjpa.enhance.PersistenceCapable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
public class ApplicationCloneService
{
  private static final Logger log = LoggerFactory.getLogger(ApplicationCloneService.class);

  private final OrganizationDAO orgDAO;

  private final ApplicationDAO appDAO;

  private final LabelDAO labelDAO;

  private final LicenseOverrideDAO licenseOverrideDAO;

  private final LicenseThreatGroupDAO licenseThreatGroupDAO;

  private final LicenseThreatGroupLicenseDAO licenseThreatGroupLicenseDAO;

  private final ComponentLabelDAO componentLabelDAO;

  private final MembershipMappingDAO membershipMappingDAO;

  private final PolicyMonitoringDAO policyMonitoringDAO;

  private final SecurityVulnerabilityOverrideDAO securityVulnerabilityOverrideDAO;

  private final ApiApplicationAdapter apiAppAdapter;

  @Inject
  public ApplicationCloneService(
      OrganizationDAO orgDAO,
      ApplicationDAO appDAO,
      LabelDAO labelDAO,
      LicenseOverrideDAO licenseOverrideDAO,
      LicenseThreatGroupDAO licenseThreatGroupDAO,
      LicenseThreatGroupLicenseDAO licenseThreatGroupLicenseDAO,
      ComponentLabelDAO componentLabelDAO,
      MembershipMappingDAO membershipMappingDAO,
      PolicyMonitoringDAO policyMonitoringDAO,
      SecurityVulnerabilityOverrideDAO securityVulnerabilityOverrideDAO,
      ApiApplicationAdapter apiAppAdapter)
  {
    this.orgDAO = orgDAO;
    this.appDAO = appDAO;
    this.labelDAO = labelDAO;
    this.licenseOverrideDAO = licenseOverrideDAO;
    this.licenseThreatGroupDAO = licenseThreatGroupDAO;
    this.licenseThreatGroupLicenseDAO = licenseThreatGroupLicenseDAO;
    this.componentLabelDAO = componentLabelDAO;
    this.membershipMappingDAO = membershipMappingDAO;
    this.policyMonitoringDAO = policyMonitoringDAO;
    this.securityVulnerabilityOverrideDAO = securityVulnerabilityOverrideDAO;
    this.apiAppAdapter = apiAppAdapter;
  }

  public ApiApplicationDTO cloneApplication(String sourceAppId, String clonedAppName, String clonedAppPublicId) {
    long start = System.currentTimeMillis();

    AuditData.get().setData("sourceApplicationId", sourceAppId);

    try (TransactionContext tx = appDAO.createTransactionContext()) {
      tx.begin();

      Application sourceApp = appDAO.getByIdNotNull(tx, sourceAppId);
      log.info("Cloning application {} (name: {})...", sourceApp.getId(), sourceApp.getName());

      AuditData.get() //
          .setData("sourceApplicationPublicId", sourceApp.getPublicId()) //
          .setData("sourceApplicationName", sourceApp.getName()) //
          .setParentOrganization(orgDAO.getById(sourceApp.getOrganizationId()));

      checkAddApplicationPermission(sourceApp.getOrganizationId());

      ApiApplicationDTO clonedApp = cloneApplication(tx, sourceApp, clonedAppName, clonedAppPublicId);

      tx.commit();

      log.info("Cloned application {} (name: {}) to application {} (name: {}) in {} ms.", //
          sourceApp.getId(), sourceApp.getName(), //
          clonedApp.id, clonedApp.name, //
          System.currentTimeMillis() - start);
      return clonedApp;
    }
  }

  @Authorize(permission = Permission.ADD_APPLICATION)
  void checkAddApplicationPermission(
      @SuppressWarnings("unused") @AuthzContext(AuthzContext.Key.ORGANIZATION_ID) String organizationId)
  {
  }

  private ApiApplicationDTO cloneApplication(
      TransactionContext tx,
      Application sourceApp,
      String clonedAppName,
      String clonedAppPublicId)
  {
    if (appDAO.getByName(tx, clonedAppName) != null) {
      throw new BadRequestException("An application with name '" + clonedAppName + "' already exists.");
    }
    if (appDAO.getByPublicId(tx, clonedAppPublicId) != null) {
      throw new BadRequestException("An application with public ID '" + clonedAppPublicId + "' already exists.");
    }

    Application clonedApp = createClonedApplication(tx, sourceApp, clonedAppName, clonedAppPublicId);
    cloneLabels(tx, sourceApp, clonedApp);
    cloneLicenseThreatGroups(tx, sourceApp, clonedApp);
    cloneLicenseOverrides(tx, sourceApp, clonedApp);
    cloneSecurityVulnerabilityOverrides(tx, sourceApp, clonedApp);
    cloneMembershipMappings(tx, sourceApp, clonedApp);
    clonePolicyMonitoring(tx, sourceApp, clonedApp);

    return apiAppAdapter.convertToDTO(clonedApp);
  }

  private Application createClonedApplication(
      TransactionContext tx,
      Application sourceApp,
      String clonedAppName,
      String clonedAppPublicId)
  {
    Application clonedApp = new Application(clonedAppPublicId, clonedAppName, sourceApp.getOrganizationId());
    clonedApp.setContactInternalName(sourceApp.getContactInternalName());
    // Disable policy violation grandfathering in the cloned application.
    // If grandfathering is enabled, then all policy violations will be grandfathered when the first policy evaluation
    // happens.
    clonedApp.setPolicyViolationGrandfatheringEnabled(false);
    appDAO.insert(tx, clonedApp);

    AuditData.get().setApplicationWithDetails(clonedApp);

    return clonedApp;
  }

  private void cloneLabels(TransactionContext tx, Application sourceApp, Application clonedApp) {
    List<Label> labels = labelDAO.getByOwnerId(tx, sourceApp.getId());
    for (Label label : labels) {
      String sourceLabelId = label.getId();
      
      detachEntity(label);
      label.setOwnerId(clonedApp.getId());
      labelDAO.insert(tx, label);
      
      List<ComponentLabel> componentLabels = componentLabelDAO.getByLabelId(tx, sourceLabelId);
      for (ComponentLabel componentLabel : componentLabels) {
        detachEntity(componentLabel);
        componentLabel.setLabelId(label.getId());
        componentLabel.setOwnerId(clonedApp.getId());
        componentLabelDAO.insert(tx, componentLabel);
      }

      log.info("Cloned label {} (label: {}) to label {}.", //
          sourceLabelId, label.getLabel(), //
          label.getId());
    }
  }

  private void cloneLicenseThreatGroups(TransactionContext tx, Application sourceApp, Application clonedApp) {
    List<LicenseThreatGroup> licenseThreatGroups = licenseThreatGroupDAO.getByOwnerId(tx, sourceApp.getId());
    for (LicenseThreatGroup licenseThreatGroup : licenseThreatGroups) {
      String sourceLicenseThreatGroupId = licenseThreatGroup.getId();

      detachEntity(licenseThreatGroup);
      licenseThreatGroup.setOwnerId(clonedApp.getId());
      licenseThreatGroupDAO.insert(tx, licenseThreatGroup);

      List<LicenseThreatGroupLicense> licenseThreatGroupLicenses =
          licenseThreatGroupLicenseDAO.getByLicenseThreatGroupId(tx, sourceLicenseThreatGroupId);
      for (LicenseThreatGroupLicense licenseThreatGroupLicense : licenseThreatGroupLicenses) {
        detachEntity(licenseThreatGroupLicense);
        licenseThreatGroupLicense.setLicenseThreatGroupId(licenseThreatGroup.getId());
        licenseThreatGroupLicense.setOwnerId(clonedApp.getId());
        licenseThreatGroupLicenseDAO.insert(tx, licenseThreatGroupLicense);
      }

      log.info("Cloned license threat group {} (name: {}) to license threat group {}.", //
          sourceLicenseThreatGroupId, licenseThreatGroup.getName(), //
          licenseThreatGroup.getId());
    }
  }

  private void cloneLicenseOverrides(TransactionContext tx, Application sourceApp, Application clonedApp) {
    List<LicenseOverride> licenseOverrides = licenseOverrideDAO.getByOwnerId(tx, sourceApp.getId());
    for (LicenseOverride licenseOverride : licenseOverrides) {
      String sourceLicenseOverrideId = licenseOverride.getId();

      licenseOverride.setId(null);
      licenseOverride.setOwnerId(clonedApp.getId());
      licenseOverrideDAO.insert(tx, licenseOverride);

      log.info("Cloned license override {} (component ID: {}) to license override {}.", //
          sourceLicenseOverrideId, licenseOverride.getComponentIdentifier(), //
          licenseOverride.getId());
    }
  }

  private void cloneSecurityVulnerabilityOverrides(
      TransactionContext tx,
      Application sourceApp,
      Application clonedApp)
  {
    List<SecurityVulnerabilityOverride> securityVulnerabilityOverrides =
        securityVulnerabilityOverrideDAO.getByOwnerId(tx, sourceApp.getId());
    for (SecurityVulnerabilityOverride securityVulnerabilityOverride : securityVulnerabilityOverrides) {
      String sourceSecurityVulnerabilityOverrideId = securityVulnerabilityOverride.getId();

      detachEntity(securityVulnerabilityOverride);
      securityVulnerabilityOverride.setOwnerId(clonedApp.getId());
      securityVulnerabilityOverrideDAO.insert(tx, securityVulnerabilityOverride);

      log.info( //
          "Cloned security vulnerability override {} (component hash: {}) to security vulnerability override {}.", //
          sourceSecurityVulnerabilityOverrideId, securityVulnerabilityOverride.getHash(), //
          securityVulnerabilityOverride.getId());
    }
  }

  private void cloneMembershipMappings(TransactionContext tx, Application sourceApp, Application clonedApp) {
    List<MembershipMapping> membershipMappings = membershipMappingDAO.getByContextId(tx, sourceApp.getId());
    for (MembershipMapping membershipMapping : membershipMappings) {
      String sourceMembershipMappingId = membershipMapping.getId();

      detachEntity(membershipMapping);
      membershipMapping.setContextId(clonedApp.getId());
      membershipMappingDAO.insert(tx, membershipMapping);

      log.info("Cloned membership mapping {} (member: {}) to membership mapping {}.", //
          sourceMembershipMappingId, membershipMapping.getMemberName(), //
          membershipMapping.getId());
    }
  }

  private void clonePolicyMonitoring(TransactionContext tx, Application sourceApp, Application clonedApp) {
    PolicyMonitoring policyMonitoring = policyMonitoringDAO.getByOwnerId(tx, sourceApp.getId());
    if (policyMonitoring == null) {
      return;
    }

    String sourcePolicyMonitoringId = policyMonitoring.getId();

    detachEntity(policyMonitoring);
    policyMonitoring.setOwnerId(clonedApp.getId());
    policyMonitoringDAO.insert(tx, policyMonitoring);

    log.info("Cloned policy monitoring {} (stage: {}) to policy monitoring {}.", //
        sourcePolicyMonitoringId, policyMonitoring.getStageTypeId(), //
        policyMonitoring.getId());
  }

  private <E extends HasStringId> void detachEntity(E entity) {
    PersistenceCapable pc = (PersistenceCapable) entity;
    pc.pcSetDetachedState(null);
    pc.pcReplaceStateManager(null);
    entity.setId(null);
  }
}
