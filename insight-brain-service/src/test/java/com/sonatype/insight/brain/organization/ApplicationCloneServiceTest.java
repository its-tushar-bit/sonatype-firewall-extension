/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.organization;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.api.v2.dto.ApiApplicationDTO;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
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
import com.sonatype.insight.brain.model.license.LicenseOverrideStatus;
import com.sonatype.insight.brain.model.license.LicenseThreatGroup;
import com.sonatype.insight.brain.model.license.LicenseThreatGroupLicense;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyMonitoring;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.insight.brain.model.tag.ApplicationTag;
import com.sonatype.insight.brain.model.tag.PolicyTag;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.brain.model.vulnerability.SecurityVulnerabilityOverride;
import com.sonatype.insight.brain.model.vulnerability.SecurityVulnerabilityOverrideStatus;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.nexus.scm.SourceControlProvider;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class ApplicationCloneServiceTest
    extends AbstractComponentTest
{
  @Inject
  private ApplicationCloneService appCloneService;

  @Test
  public void testCloneApplication_SourceApplicationDoesNotExist() {
    assertThatExceptionOfType(NotFoundException.class).isThrownBy(() -> {
      appCloneService.cloneApplication("AppDoesNotExistId", "clonedAppName", "clonedAppPublicId");
    }).withMessage("Could not find an application with ID AppDoesNotExistId.");
  }

  @Test
  public void testCloneApplication_Application() {
    String clonedAppName = "clonedAppName";
    String clonedAppPublicId = "clonedAppPublicId";
    String contactUsername = "testuser";
    Application sourceApp = tempEntity.newApplicationWithParent();
    sourceApp.setContactInternalName(contactUsername);
    // The application cloning is supposed to disable grandfathering for the cloned app.
    // So we set it to true in the source application in order to verify
    // that is not copied to the cloned application.
    sourceApp.setPolicyViolationGrandfatheringEnabled(true);
    new ApplicationDAO().update(sourceApp);

    ApiApplicationDTO clonedAppDTO =
        appCloneService.cloneApplication(sourceApp.getId(), clonedAppName, clonedAppPublicId);

    // Assert the returned app DTO.
    assertThat(clonedAppDTO.organizationId).isEqualTo(sourceApp.getOrganizationId());
    assertThat(clonedAppDTO.name).isEqualTo(clonedAppName);
    assertThat(clonedAppDTO.publicId).isEqualTo(clonedAppPublicId);
    assertThat(clonedAppDTO.contactUserName).isEqualTo(contactUsername);
    assertThat(clonedAppDTO.applicationTags).isEmpty();
    
    // Assert the app stored in the db.
    Application clonedApp = new ApplicationDAO().getByIdNotNull(clonedAppDTO.id);
    assertThat(clonedApp.getOrganizationId()).isEqualTo(sourceApp.getOrganizationId());
    assertThat(clonedApp.getName()).isEqualTo(clonedAppName);
    assertThat(clonedApp.getPublicId()).isEqualTo(clonedAppPublicId);
    assertThat(clonedApp.getContactInternalName()).isEqualTo(contactUsername);
    assertThat(clonedApp.isPolicyViolationGrandfatheringEnabled()).isFalse();
  }

  @Test
  public void testCloneApplication_DuplicateApplicationName() {
    String clonedAppName = "clonedAppName";
    Application sourceApp = tempEntity.newApplicationWithParent();
    tempEntity.newApplicationWithParent("appPublicId", clonedAppName);

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> {
      appCloneService.cloneApplication(sourceApp.getId(), clonedAppName, "clonedAppPublicId");
    }).withMessage("An application with name '" + clonedAppName + "' already exists.");
  }

  @Test
  public void testCloneApplication_DuplicateApplicationPublicId() {
    String clonedAppPublicId = "clonedAppPublicId";
    Application sourceApp = tempEntity.newApplicationWithParent();
    tempEntity.newApplicationWithParent(clonedAppPublicId, "aAppName");

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> {
      appCloneService.cloneApplication(sourceApp.getId(), "clonedAppName", clonedAppPublicId);
    }).withMessage("An application with public ID '" + clonedAppPublicId + "' already exists.");
  }

  @Test
  public void testCloneApplication_Labels() {
    Application sourceApp = tempEntity.newApplicationWithParent();
    Label sourceLabel = tempEntity.newLabel(sourceApp.getId());
    ComponentLabel sourceComponentLabel =
        tempEntity.newComponentLabel(sourceApp.getId(), sourceLabel.getId(), "testhash");

    ApiApplicationDTO clonedAppDTO =
        appCloneService.cloneApplication(sourceApp.getId(), "clonedAppName", "clonedAppPublicId");

    List<Label> clonedLabels = new LabelDAO().getByOwnerId(clonedAppDTO.id);
    assertThat(clonedLabels).hasSize(1);
    Label clonedLabel = clonedLabels.get(0);
    assertThat(clonedLabel.getId()).isNotEqualTo(sourceLabel.getId());
    assertThat(clonedLabel.getLabel()).isEqualTo(sourceLabel.getLabel());
    assertThat(clonedLabel.getDescription()).isEqualTo(sourceLabel.getDescription());
    assertThat(clonedLabel.getColor()).isEqualTo(sourceLabel.getColor());

    List<ComponentLabel> clonedComponentLabels = new ComponentLabelDAO().getByOwnerId(clonedAppDTO.id);
    assertThat(clonedComponentLabels).hasSize(1);
    ComponentLabel clonedComponentLabel = clonedComponentLabels.get(0);
    assertThat(clonedComponentLabel.getId()).isNotEqualTo(sourceComponentLabel.getId());
    assertThat(clonedComponentLabel.getLabelId()).isEqualTo(clonedLabel.getId());
    assertThat(clonedComponentLabel.getHash()).isEqualTo(sourceComponentLabel.getHash());

    // Assert the source objects were cloned, not moved.
    assertThat(new LabelDAO().getById(sourceLabel.getId())).isNotNull();
    assertThat(new ComponentLabelDAO().getById(sourceComponentLabel.getId())).isNotNull();
  }

  @Test
  public void testCloneApplication_LicenseThreatGroups() {
    Application sourceApp = tempEntity.newApplicationWithParent();
    LicenseThreatGroup sourceLicenseThreatGroup = tempEntity.newLicenseThreatGroup(sourceApp.getId());
    LicenseThreatGroupLicense sourceLicenseThreatGroupLicense =
        tempEntity.newLicenseThreatGroupLicense(sourceApp.getId(), sourceLicenseThreatGroup.getId());

    ApiApplicationDTO clonedAppDTO =
        appCloneService.cloneApplication(sourceApp.getId(), "clonedAppName", "clonedAppPublicId");

    List<LicenseThreatGroup> clonedLicenseThreatGroups = new LicenseThreatGroupDAO().getByOwnerId(clonedAppDTO.id);
    assertThat(clonedLicenseThreatGroups).hasSize(1);
    LicenseThreatGroup clonedLicenseThreatGroup = clonedLicenseThreatGroups.get(0);
    assertThat(clonedLicenseThreatGroup.getId()).isNotEqualTo(sourceLicenseThreatGroup.getId());
    assertThat(clonedLicenseThreatGroup.getName()).isEqualTo(sourceLicenseThreatGroup.getName());
    assertThat(clonedLicenseThreatGroup.getThreatLevel()).isEqualTo(sourceLicenseThreatGroup.getThreatLevel());

    List<LicenseThreatGroupLicense> clonedLicenseThreatGroupLicenses =
        new LicenseThreatGroupLicenseDAO().getByOwnerId(clonedAppDTO.id);
    assertThat(clonedLicenseThreatGroupLicenses).hasSize(1);
    LicenseThreatGroupLicense clonedLicenseThreatGroupLicense = clonedLicenseThreatGroupLicenses.get(0);
    assertThat(clonedLicenseThreatGroupLicense.getId()).isNotEqualTo(sourceLicenseThreatGroupLicense.getId());
    assertThat(clonedLicenseThreatGroupLicense.getLicenseThreatGroupId()).isEqualTo(clonedLicenseThreatGroup.getId());
    assertThat(clonedLicenseThreatGroupLicense.getLicenseId())
        .isEqualTo(sourceLicenseThreatGroupLicense.getLicenseId());

    // Assert the source objects were cloned, not moved.
    assertThat(new LicenseThreatGroupDAO().getById(sourceLicenseThreatGroup.getId())).isNotNull();
    assertThat(new LicenseThreatGroupLicenseDAO().getById(sourceLicenseThreatGroupLicense.getId())).isNotNull();
  }

  @Test
  public void testCloneApplication_LicenseOverrides() {
    Application sourceApp = tempEntity.newApplicationWithParent();
    LicenseOverride sourceLicenseOverride = tempEntity.newLicenseOverride(sourceApp.getId(),
        ComponentIdentifier.createNpmCoordinates("packageId", "version"), LicenseOverrideStatus.OVERRIDDEN,
        "Apache-2.0");

    ApiApplicationDTO clonedAppDTO =
        appCloneService.cloneApplication(sourceApp.getId(), "clonedAppName", "clonedAppPublicId");

    List<LicenseOverride> clonedLicenseOverrides = new LicenseOverrideDAO().getByOwnerId(clonedAppDTO.id);
    assertThat(clonedLicenseOverrides).hasSize(1);
    LicenseOverride clonedLicenseOverride = clonedLicenseOverrides.get(0);
    assertThat(clonedLicenseOverride.getId()).isNotEqualTo(sourceLicenseOverride.getId());
    assertThat(clonedLicenseOverride.getStatus()).isEqualTo(sourceLicenseOverride.getStatus());
    assertThat(clonedLicenseOverride.getComponentIdentifier())
        .isEqualTo(sourceLicenseOverride.getComponentIdentifier());
    assertThat(clonedLicenseOverride.getLicenseIds()).isEqualTo(sourceLicenseOverride.getLicenseIds());
    assertThat(clonedLicenseOverride.getComment()).isEqualTo(sourceLicenseOverride.getComment());

    // Assert the source objects were cloned, not moved.
    assertThat(new LicenseOverrideDAO().getById(sourceLicenseOverride.getId())).isNotNull();
  }

  @Test
  public void testCloneApplication_SecurityVulnerabilityOverrides() {
    Application sourceApp = tempEntity.newApplicationWithParent();
    SecurityVulnerabilityOverride sourceSecurityVulnerabilityOverride =
        tempEntity.newSecurityVulnerabilityOverride(sourceApp.getId(), "hash", "source", "refrenceId",
            SecurityVulnerabilityOverrideStatus.NOT_APPLICABLE, "comment");

    ApiApplicationDTO clonedAppDTO =
        appCloneService.cloneApplication(sourceApp.getId(), "clonedAppName", "clonedAppPublicId");

    List<SecurityVulnerabilityOverride> clonedSecurityVulnerabilityOverrides =
        new SecurityVulnerabilityOverrideDAO().getByOwnerId(clonedAppDTO.id);
    assertThat(clonedSecurityVulnerabilityOverrides).hasSize(1);
    SecurityVulnerabilityOverride clonedSecurityVulnerabilityOverride = clonedSecurityVulnerabilityOverrides.get(0);
    assertThat(clonedSecurityVulnerabilityOverride.getId()).isNotEqualTo(sourceSecurityVulnerabilityOverride.getId());
    assertThat(clonedSecurityVulnerabilityOverride.getStatus())
        .isEqualTo(sourceSecurityVulnerabilityOverride.getStatus());
    assertThat(clonedSecurityVulnerabilityOverride.getHash()).isEqualTo(sourceSecurityVulnerabilityOverride.getHash());
    assertThat(clonedSecurityVulnerabilityOverride.getSource())
        .isEqualTo(sourceSecurityVulnerabilityOverride.getSource());
    assertThat(clonedSecurityVulnerabilityOverride.getReferenceId())
        .isEqualTo(sourceSecurityVulnerabilityOverride.getReferenceId());
    assertThat(clonedSecurityVulnerabilityOverride.getComment())
        .isEqualTo(sourceSecurityVulnerabilityOverride.getComment());

    // Assert the source objects were cloned, not moved.
    assertThat(new SecurityVulnerabilityOverrideDAO().getById(sourceSecurityVulnerabilityOverride.getId())).isNotNull();
  }

  @Test
  public void testCloneApplication_MembershipMappings() {
    Application sourceApp = tempEntity.newApplicationWithParent();
    MembershipMapping sourceMembershipMapping =
        tempEntity.newMembershipMapping(sourceApp.getId(), Role.DEVELOPER_ROLE_ID, "username");

    ApiApplicationDTO clonedAppDTO =
        appCloneService.cloneApplication(sourceApp.getId(), "clonedAppName", "clonedAppPublicId");

    List<MembershipMapping> clonedMembershipMappings = new MembershipMappingDAO().getByContextId(clonedAppDTO.id);
    assertThat(clonedMembershipMappings).hasSize(1);
    MembershipMapping clonedMembershipMapping = clonedMembershipMappings.get(0);
    assertThat(clonedMembershipMapping.getId()).isNotEqualTo(sourceMembershipMapping.getId());
    assertThat(clonedMembershipMapping.getRoleId()).isEqualTo(sourceMembershipMapping.getRoleId());
    assertThat(clonedMembershipMapping.getMemberName()).isEqualTo(sourceMembershipMapping.getMemberName());
    assertThat(clonedMembershipMapping.getMemberType()).isEqualTo(sourceMembershipMapping.getMemberType());

    // Assert the source objects were cloned, not moved.
    assertThat(new MembershipMappingDAO().getById(sourceMembershipMapping.getId())).isNotNull();
  }

  @Test
  public void testCloneApplication_PolicyMonitoring() {
    Application sourceApp = tempEntity.newApplicationWithParent();
    PolicyMonitoring sourcePolicyMonitoring =
        tempEntity.newPolicyMonitoring(sourceApp.getId(), StageTypes.BUILD.getId());

    ApiApplicationDTO clonedAppDTO =
        appCloneService.cloneApplication(sourceApp.getId(), "clonedAppName", "clonedAppPublicId");

    PolicyMonitoring clonedPolicyMonitoring = new PolicyMonitoringDAO().getByOwnerId(clonedAppDTO.id);
    assertThat(clonedPolicyMonitoring.getId()).isNotEqualTo(sourcePolicyMonitoring.getId());
    assertThat(clonedPolicyMonitoring.getStageTypeId()).isEqualTo(sourcePolicyMonitoring.getStageTypeId());

    // Assert the source objects were cloned, not moved.
    assertThat(new PolicyMonitoringDAO().getById(sourcePolicyMonitoring.getId())).isNotNull();
  }

  @Test
  public void testCloneApplication_ApplicationTags() {
    Application sourceApp = tempEntity.newApplicationWithParent();
    Tag tag = tempEntity.newTag(sourceApp.getOrganizationId());
    ApplicationTag sourceAppTag = tempEntity.newApplicationTag(sourceApp.getId(), tag.getId());

    ApiApplicationDTO clonedAppDTO =
        appCloneService.cloneApplication(sourceApp.getId(), "clonedAppName", "clonedAppPublicId");

    List<ApplicationTag> clonedAppTags = new ApplicationTagDAO().getByApplicationId(clonedAppDTO.id);
    assertThat(clonedAppTags).hasSize(1);
    ApplicationTag clonedAppTag = clonedAppTags.get(0);
    assertThat(clonedAppTag.getId()).isNotEqualTo(sourceAppTag.getId());
    assertThat(clonedAppTag.getTagId()).isEqualTo(sourceAppTag.getTagId());

    // Assert the source objects were cloned, not moved.
    assertThat(new ApplicationTagDAO().getById(sourceAppTag.getId())).isNotNull();
  }

  @Test
  public void testCloneApplication_ProprietaryConfig() {
    Application sourceApp = tempEntity.newApplicationWithParent();
    ProprietaryConfig sourceProprietaryConfig = tempEntity.newProprietaryConfig(sourceApp.getId(),
        Collections.singletonList("proprietarypackage"), Collections.singletonList("proprietaryregex"));

    ApiApplicationDTO clonedAppDTO =
        appCloneService.cloneApplication(sourceApp.getId(), "clonedAppName", "clonedAppPublicId");

    ProprietaryConfig clonedProprietaryConfig = new ProprietaryConfigDAO().getByOwnerId(clonedAppDTO.id);
    assertThat(clonedProprietaryConfig.getId()).isNotEqualTo(sourceProprietaryConfig.getId());
    assertThat(clonedProprietaryConfig.getPackages()).isEqualTo(sourceProprietaryConfig.getPackages());
    assertThat(clonedProprietaryConfig.getRegexes()).isEqualTo(sourceProprietaryConfig.getRegexes());

    // Assert the source objects were cloned, not moved.
    assertThat(new ProprietaryConfigDAO().getById(sourceProprietaryConfig.getId())).isNotNull();
  }

  @Test
  public void testCloneApplication_SourceControl() {
    Application sourceApp = tempEntity.newApplicationWithParent();
    // The application cloning is supposed to disable pull requests for the cloned app.
    // So we set it to true in the source application in order to verify
    // that is not copied to the cloned application.
    SourceControl sourceSourceControl =
        tempEntity.newSourceControl(sourceApp.getId(), "https://example.com/organization/project", "token",
            SourceControlProvider.GITHUB, true /* enablePullRequests */, true /* enableStatusChecks */, "baseBranch");

    ApiApplicationDTO clonedAppDTO =
        appCloneService.cloneApplication(sourceApp.getId(), "clonedAppName", "clonedAppPublicId");

    SourceControl clonedSourceControl = new SourceControlDAO().getByOwnerId(clonedAppDTO.id);
    assertThat(clonedSourceControl.getId()).isNotEqualTo(sourceSourceControl.getId());
    assertThat(clonedSourceControl.getRepositoryUrl()).isEqualTo(sourceSourceControl.getRepositoryUrl());
    assertThat(clonedSourceControl.getToken()).isEqualTo(sourceSourceControl.getToken());
    assertThat(clonedSourceControl.getProvider()).isEqualTo(sourceSourceControl.getProvider());
    assertThat(clonedSourceControl.getBaseBranch()).isEqualTo(sourceSourceControl.getBaseBranch());
    assertThat(clonedSourceControl.getEnablePullRequests()).isFalse();
    assertThat(clonedSourceControl.getEnableStatusChecks()).isEqualTo(sourceSourceControl.getEnableStatusChecks());

    // Assert the source objects were cloned, not moved.
    assertThat(new SourceControlDAO().getById(sourceSourceControl.getId())).isNotNull();
  }

  @Test
  public void testCloneApplication_Policies() {
    Application sourceApp = tempEntity.newApplicationWithParent();
    Policy sourcePolicy = tempEntity.newPolicy(sourceApp.getId());

    ApiApplicationDTO clonedAppDTO =
        appCloneService.cloneApplication(sourceApp.getId(), "clonedAppName", "clonedAppPublicId");

    List<Policy> clonedPolicies = new PolicyDAO().getByOwnerId(clonedAppDTO.id);
    assertThat(clonedPolicies).hasSize(1);
    Policy clonedPolicy = clonedPolicies.get(0);
    // TODO: Assert the policy constraints
    assertThat(clonedPolicy.getId()).isNotEqualTo(sourcePolicy.getId());
    assertThat(clonedPolicy.getName()).isEqualTo(sourcePolicy.getName());
    assertThat(clonedPolicy.getThreatLevel()).isEqualTo(sourcePolicy.getThreatLevel());
    assertThat(clonedPolicy.isPolicyViolationGrandfatheringAllowed())
        .isEqualTo(sourcePolicy.isPolicyViolationGrandfatheringAllowed());
    assertThat(clonedPolicy.getActions()).isEqualTo(sourcePolicy.getActions());
    assertThat(clonedPolicy.getNotifications()).isEqualTo(sourcePolicy.getNotifications());

    // Assert the source objects were cloned, not moved.
    assertThat(new PolicyDAO().getById(sourcePolicy.getId())).isNotNull();
  }

  @Test
  public void testCloneApplication_PolicyTags() {
    Application sourceApp = tempEntity.newApplicationWithParent();
    Tag tag = tempEntity.newTag(sourceApp.getOrganizationId());
    Policy sourcePolicy = tempEntity.newPolicy(sourceApp.getId());
    PolicyTag sourcePolicyTag = tempEntity.newPolicyTag(sourcePolicy.getId(), tag.getId());

    ApiApplicationDTO clonedAppDTO =
        appCloneService.cloneApplication(sourceApp.getId(), "clonedAppName", "clonedAppPublicId");

    Policy clonedPolicy = new PolicyDAO().getByOwnerIdAndName(clonedAppDTO.id, sourcePolicy.getName());
    List<PolicyTag> clonedPolicyTags = new PolicyTagDAO().getByPolicyId(clonedPolicy.getId());
    assertThat(clonedPolicyTags).hasSize(1);
    PolicyTag clonedPolicyTag = clonedPolicyTags.get(0);
    assertThat(clonedPolicyTag.getId()).isNotEqualTo(sourcePolicyTag.getId());
    assertThat(clonedPolicyTag.getTagId()).isEqualTo(sourcePolicyTag.getTagId());

    // Assert the source objects were cloned, not moved.
    assertThat(new PolicyTagDAO().getById(sourcePolicyTag.getId())).isNotNull();
  }

  @Test
  public void testCloneApplication_PolicyWaivers() {
    Application sourceApp = tempEntity.newApplicationWithParent();
    Policy sourcePolicy = tempEntity.newPolicy(sourceApp.getId());
    PolicyWaiver sourcePolicyWaiver = tempEntity.newWaiver("hash", sourcePolicy.getId(), sourceApp.getId(), "comment");

    ApiApplicationDTO clonedAppDTO =
        appCloneService.cloneApplication(sourceApp.getId(), "clonedAppName", "clonedAppPublicId");

    Policy clonedPolicy = new PolicyDAO().getByOwnerIdAndName(clonedAppDTO.id, sourcePolicy.getName());
    List<PolicyWaiver> clonedPolicyWaivers = new PolicyWaiverDAO().getByOwnerId(clonedAppDTO.id);
    assertThat(clonedPolicyWaivers).hasSize(1);
    PolicyWaiver clonedPolicyWaiver = clonedPolicyWaivers.get(0);
    // TODO: Assert the policy constraint facts
    assertThat(clonedPolicyWaiver.getId()).isNotEqualTo(sourcePolicyWaiver.getId());
    assertThat(clonedPolicyWaiver.getPolicyId()).isEqualTo(clonedPolicy.getId());
    assertThat(clonedPolicyWaiver.getHash()).isEqualTo(sourcePolicyWaiver.getHash());
    assertThat(clonedPolicyWaiver.getComment()).isEqualTo(sourcePolicyWaiver.getComment());
    assertThat(clonedPolicyWaiver.getCreateTime()).isEqualTo(sourcePolicyWaiver.getCreateTime());

    // Assert the source objects were cloned, not moved.
    assertThat(new PolicyWaiverDAO().getById(sourcePolicyWaiver.getId())).isNotNull();
  }
}
