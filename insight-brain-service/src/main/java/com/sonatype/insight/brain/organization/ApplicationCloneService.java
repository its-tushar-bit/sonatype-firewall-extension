/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.organization;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.insight.brain.api.v2.ApiApplicationAdapter;
import com.sonatype.insight.brain.api.v2.dto.ApiApplicationDTO;
import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.configuration.ProprietaryConfigDAO;
import com.sonatype.insight.brain.dataaccess.label.ComponentLabelDAO;
import com.sonatype.insight.brain.dataaccess.label.LabelDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseOverrideDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupLicenseDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyMonitoringDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO;
import com.sonatype.insight.brain.dataaccess.security.MembershipMappingDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlDAO;
import com.sonatype.insight.brain.dataaccess.tag.ApplicationTagDAO;
import com.sonatype.insight.brain.dataaccess.tag.PolicyTagDAO;
import com.sonatype.insight.brain.dataaccess.vulnerability.SecurityVulnerabilityOverrideDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.configuration.ProprietaryConfig;
import com.sonatype.insight.brain.model.label.ComponentLabel;
import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.brain.model.license.LicenseOverride;
import com.sonatype.insight.brain.model.license.LicenseThreatGroup;
import com.sonatype.insight.brain.model.license.LicenseThreatGroupLicense;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyMonitoring;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.insight.brain.model.tag.ApplicationTag;
import com.sonatype.insight.brain.model.tag.PolicyTag;
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

  private final ApplicationTagDAO appTagDAO;

  private final LabelDAO labelDAO;

  private final LicenseOverrideDAO licenseOverrideDAO;

  private final LicenseThreatGroupDAO licenseThreatGroupDAO;

  private final LicenseThreatGroupLicenseDAO licenseThreatGroupLicenseDAO;

  private final ComponentLabelDAO componentLabelDAO;

  private final MembershipMappingDAO membershipMappingDAO;

  private final PolicyDAO policyDAO;

  private final PolicyMonitoringDAO policyMonitoringDAO;

  private final PolicyTagDAO policyTagDAO;

  private final PolicyWaiverDAO policyWaiverDAO;

  private final ProprietaryConfigDAO proprietaryConfigDAO;

  private final SecurityVulnerabilityOverrideDAO securityVulnerabilityOverrideDAO;

  private final SourceControlDAO sourceControlDAO;

  private final ApiApplicationAdapter apiAppAdapter;

  @Inject
  public ApplicationCloneService(
      OrganizationDAO orgDAO,
      ApplicationDAO appDAO,
      ApplicationTagDAO appTagDAO,
      LabelDAO labelDAO,
      LicenseOverrideDAO licenseOverrideDAO,
      LicenseThreatGroupDAO licenseThreatGroupDAO,
      LicenseThreatGroupLicenseDAO licenseThreatGroupLicenseDAO,
      ComponentLabelDAO componentLabelDAO,
      MembershipMappingDAO membershipMappingDAO,
      PolicyDAO policyDAO,
      PolicyMonitoringDAO policyMonitoringDAO,
      PolicyTagDAO policyTagDAO,
      PolicyWaiverDAO policyWaiverDAO,
      ProprietaryConfigDAO proprietaryConfigDAO,
      SecurityVulnerabilityOverrideDAO securityVulnerabilityOverrideDAO,
      SourceControlDAO sourceControlDAO,
      ApiApplicationAdapter apiAppAdapter)
  {
    this.orgDAO = orgDAO;
    this.appDAO = appDAO;
    this.appTagDAO = appTagDAO;
    this.labelDAO = labelDAO;
    this.licenseOverrideDAO = licenseOverrideDAO;
    this.licenseThreatGroupDAO = licenseThreatGroupDAO;
    this.licenseThreatGroupLicenseDAO = licenseThreatGroupLicenseDAO;
    this.componentLabelDAO = componentLabelDAO;
    this.membershipMappingDAO = membershipMappingDAO;
    this.policyDAO = policyDAO;
    this.policyMonitoringDAO = policyMonitoringDAO;
    this.policyTagDAO = policyTagDAO;
    this.policyWaiverDAO = policyWaiverDAO;
    this.proprietaryConfigDAO = proprietaryConfigDAO;
    this.securityVulnerabilityOverrideDAO = securityVulnerabilityOverrideDAO;
    this.sourceControlDAO = sourceControlDAO;
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
    cloneApplicationTags(tx, sourceApp, clonedApp);
    cloneProprietaryConfig(tx, sourceApp, clonedApp);
    cloneSourceControl(tx, sourceApp, clonedApp);
    Map<String, String> mappedPolicyIds = clonePolicies(tx, sourceApp, clonedApp);
    clonePolicyTags(tx, mappedPolicyIds);
    clonePolicyWaivers(tx, sourceApp, clonedApp, mappedPolicyIds);

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

  private void cloneApplicationTags(TransactionContext tx, Application sourceApp, Application clonedApp) {
    List<ApplicationTag> appTags = appTagDAO.getByApplicationId(tx, sourceApp.getId());
    for (ApplicationTag appTag : appTags) {
      String sourceAppTagId = appTag.getId();

      detachEntity(appTag);
      appTag.setApplicationId(clonedApp.getId());
      appTagDAO.insert(tx, appTag);

      log.info("Cloned application tag {} to application tag {}.", sourceAppTagId, appTag.getId());
    }
  }

  private void cloneProprietaryConfig(TransactionContext tx, Application sourceApp, Application clonedApp) {
    ProprietaryConfig proprietaryConfig = proprietaryConfigDAO.getByOwnerId(tx, sourceApp.getId());
    if (proprietaryConfig == null) {
      return;
    }

    String sourceProprietaryConfigId = proprietaryConfig.getId();

    detachEntity(proprietaryConfig);
    proprietaryConfig.setOwnerId(clonedApp.getId());
    proprietaryConfigDAO.insert(tx, proprietaryConfig);

    log.info("Cloned proprietary config {} to proprietary config {}.", //
        sourceProprietaryConfigId, proprietaryConfig.getId());
  }

  private void cloneSourceControl(TransactionContext tx, Application sourceApp, Application clonedApp) {
    SourceControl sourceControl = sourceControlDAO.getByOwnerId(tx, sourceApp.getId());
    if (sourceControl == null) {
      return;
    }

    String sourceSourceControlId = sourceControl.getId();

    detachEntity(sourceControl);
    sourceControl.setOwnerId(clonedApp.getId());
    sourceControl.setEnablePullRequests(false);
    sourceControlDAO.insert(tx, sourceControl);

    log.info("Cloned source control {} to source control {}.", sourceSourceControlId, sourceControl.getId());
  }

  private Map<String, String> clonePolicies(TransactionContext tx, Application sourceApp, Application clonedApp) {
    Map<String, String> mappedPolicyIds = new HashMap<>();

    List<Policy> policies = policyDAO.getByOwnerId(tx, sourceApp.getId());
    for (Policy policy : policies) {
      String sourcePolicyId = policy.getId();

      policy.setId(null);
      policy.setOwnerId(clonedApp.getId());
      // TODO: Replace all related IDs in the policy constraints
      policyDAO.insert(tx, policy);

      mappedPolicyIds.put(sourcePolicyId, policy.getId());

      log.info( //
          "Cloned policy {} (name: {}) to policy {}.", //
          sourcePolicyId, policy.getName(), //
          policy.getId());
    }

    return mappedPolicyIds;
  }

  private void clonePolicyTags(TransactionContext tx, Map<String, String> mappedPolicyIds) {
    for (String sourcePolicyId : mappedPolicyIds.keySet()) {
      List<PolicyTag> policyTags = policyTagDAO.getByPolicyId(tx, sourcePolicyId);

      for (PolicyTag policyTag : policyTags) {
        String sourcePolicyTagId = policyTag.getId();

        detachEntity(policyTag);
        policyTag.setPolicyId(mappedPolicyIds.get(sourcePolicyId));
        policyTagDAO.insert(tx, policyTag);

        log.info("Cloned policy tag {} to policy tag {}.", sourcePolicyTagId, policyTag.getId());
      }
    }
  }

  private void clonePolicyWaivers(
      TransactionContext tx,
      Application sourceApp,
      Application clonedApp,
      Map<String, String> mappedPolicyIds)
  {
    List<PolicyWaiver> policyWaivers = policyWaiverDAO.getByOwnerId(tx, sourceApp.getId());
    for (PolicyWaiver policyWaiver : policyWaivers) {
      String sourcePolicyWaiverId = policyWaiver.getId();

      detachEntity(policyWaiver);
      policyWaiver.setOwnerId(clonedApp.getId());
      policyWaiver.setPolicyId(mappedPolicyIds.get(policyWaiver.getPolicyId()));
      // TODO: Replace all related IDs in the policy constraint facts
      policyWaiverDAO.insert(tx, policyWaiver);

      log.info("Cloned policy waiver {} to policy waiver {}.", sourcePolicyWaiverId, policyWaiver.getId());
    }
  }

  private <E extends HasStringId> void detachEntity(E entity) {
    PersistenceCapable pc = (PersistenceCapable) entity;
    pc.pcSetDetachedState(null);
    pc.pcReplaceStateManager(null);
    entity.setId(null);
  }
}
