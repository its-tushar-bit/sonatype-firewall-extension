/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.cpematching;

import java.util.stream.Stream;
import jakarta.inject.Inject;

import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.configuration.CpeMatchingConfigurationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.configuration.CpeMatchingConfiguration;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.product.license.TestProductLicense;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.license.model.ProductLicenseDetails;
import com.sonatype.insight.test.LogOutput;

import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class CpeMatchingConfigurationServiceTest
    extends AbstractComponentTest
{
  @Inject
  private CpeMatchingConfigurationService cpeMatchingConfigurationService;

  @Inject
  private CpeMatchingConfigurationDAO cpeMatchingConfigurationDAO;

  @Inject
  private OrganizationDAO organizationDAO;

  @Inject
  private TestProductLicense productLicense;

  @Rule
  public LogOutput logOutput = new LogOutput(CpeMatchingConfigurationService.class);

  @Before
  public void before() {
    productLicense.setFeatures(LicensedFeature.CPE_MATCHING);
  }

  @Test
  public void getCpeMatchingConfiguration_root_cpeCfgRecordFound() {
    cpeMatchingConfigurationDAO.insert(new CpeMatchingConfiguration(Organization.ROOT_ORGANIZATION_ID, true, true));

    CpeMatchingConfigurationDTO cfg = cpeMatchingConfigurationService.getCpeMatchingConfigurationNoAuthz(
        OwnerType.ORGANIZATION, Organization.ROOT_ORGANIZATION_ID);
    assertThat(cfg.enabled).isTrue();
    assertThat(cfg.allowOverride).isTrue();
    assertThat(cfg.enabledInParent).isNull();
    assertThat(cfg.inheritedFromOrganizationName).isNull();
    assertThat(cfg.inheritedFromOrganizationAllowOverride).isNull();
  }

  @Test
  public void getCpeMatchingConfiguration_root_cpeCfgRecordFound_false() {
    cpeMatchingConfigurationDAO.insert(new CpeMatchingConfiguration(Organization.ROOT_ORGANIZATION_ID, false, false));

    CpeMatchingConfigurationDTO cfg = cpeMatchingConfigurationService.getCpeMatchingConfigurationNoAuthz(
        OwnerType.ORGANIZATION, Organization.ROOT_ORGANIZATION_ID);
    assertThat(cfg.enabled).isFalse();
    assertThat(cfg.allowOverride).isFalse();
    assertThat(cfg.enabledInParent).isNull();
    assertThat(cfg.inheritedFromOrganizationName).isNull();
    assertThat(cfg.inheritedFromOrganizationAllowOverride).isNull();
  }

  @Test
  public void getCpeMatchingConfiguration_root_cpeCfgRecordNotFound() {
    CpeMatchingConfigurationDTO cfg = cpeMatchingConfigurationService.getCpeMatchingConfigurationNoAuthz(
        OwnerType.ORGANIZATION, Organization.ROOT_ORGANIZATION_ID);
    assertThat(cfg.enabled).isNull();
    assertThat(cfg.allowOverride).isNull();
    assertThat(cfg.enabledInParent).isNull();
    assertThat(cfg.inheritedFromOrganizationName).isNull();
    assertThat(cfg.inheritedFromOrganizationAllowOverride).isNull();
  }

  @Test
  public void testGetCpeMatchingConfiguration_ForApp_NoParentEnabled_NoOverrides() {
    Application app1 = tempEntity.newApplicationWithParent();
    CpeMatchingConfiguration appCpeConfig = new CpeMatchingConfiguration(
        app1.getId(), true, true);

    cpeMatchingConfigurationDAO.insert(appCpeConfig);

    CpeMatchingConfigurationDTO actualCpeMatchingConfiguration = cpeMatchingConfigurationService
        .getCpeMatchingConfiguration(app1.getType(), app1.getId());
    assertThat(actualCpeMatchingConfiguration).isNotNull();
    assertThat(actualCpeMatchingConfiguration.enabled).isTrue();
    assertThat(actualCpeMatchingConfiguration.enabledInParent).isNull(); // Because Root org does not have a CPE config
    assertThat(actualCpeMatchingConfiguration.allowOverride).isFalse();
    assertThat(actualCpeMatchingConfiguration.inheritedFromOrganizationName).isNull();
    assertThat(actualCpeMatchingConfiguration.inheritedFromOrganizationAllowOverride).isNull();
  }

  @Test
  public void testGetCpeMatchingConfiguration_ForOrg_NoParentEnabled_NoOverrides() {
    Organization org1 = tempEntity.newOrganization();
    Organization parentOrg = organizationDAO.getById(org1.getParentOwnerId());
    CpeMatchingConfiguration orgCpeConfig = new CpeMatchingConfiguration(
        parentOrg.getId(), true, false);
    cpeMatchingConfigurationDAO.insert(orgCpeConfig);

    CpeMatchingConfigurationDTO actualCpeMatchingConfiguration = cpeMatchingConfigurationService
        .getCpeMatchingConfiguration(parentOrg.getType(), parentOrg.getId());
    assertThat(actualCpeMatchingConfiguration).isNotNull();
    assertThat(actualCpeMatchingConfiguration.enabled).isTrue();
    assertThat(actualCpeMatchingConfiguration.enabledInParent).isNull(); // Because Root org does not have a CPE config
    assertThat(actualCpeMatchingConfiguration.allowOverride).isFalse();
    assertThat(actualCpeMatchingConfiguration.inheritedFromOrganizationName).isNull();
    assertThat(actualCpeMatchingConfiguration.inheritedFromOrganizationAllowOverride).isNull();
  }

  @Test
  public void testGetCpeMatchingConfiguration_RootParentEnabled_NoOverrides() {
    // ROOT -> Dummy Org -> App1
    Application app1 = tempEntity.newApplicationWithParent();
    Organization rootOrg = organizationDAO.getById("ROOT_ORGANIZATION_ID");
    Organization dummyOrg = organizationDAO.getById(app1.getParentOwnerId());

    CpeMatchingConfiguration rootCpeConfig = new CpeMatchingConfiguration(
        rootOrg.getId(), true, false);

    cpeMatchingConfigurationDAO.insert(rootCpeConfig);

    CpeMatchingConfigurationDTO rootCpeMatchingConfigurationDtoAppTest = cpeMatchingConfigurationService
        .getCpeMatchingConfiguration(rootOrg.getType(), rootOrg.getId());

    assertThat(rootCpeMatchingConfigurationDtoAppTest).isNotNull();
    assertThat(rootCpeMatchingConfigurationDtoAppTest.enabled).isTrue();
    assertThat(rootCpeMatchingConfigurationDtoAppTest.enabledInParent).isNull(); // Root org does not have a parent
    assertThat(rootCpeMatchingConfigurationDtoAppTest.allowOverride).isFalse();
    assertThat(rootCpeMatchingConfigurationDtoAppTest.inheritedFromOrganizationName).isNull();
    assertThat(rootCpeMatchingConfigurationDtoAppTest.inheritedFromOrganizationAllowOverride).isNull();

    // Check cpe matching configuration status for dummy Org
    CpeMatchingConfigurationDTO dummyOrgCpeMatchingConfigurationDtoAppTest =
        cpeMatchingConfigurationService.getCpeMatchingConfiguration(
            dummyOrg.getType(), dummyOrg.getId());

    assertThat(dummyOrgCpeMatchingConfigurationDtoAppTest).isNotNull();
    assertThat(dummyOrgCpeMatchingConfigurationDtoAppTest.enabled).isTrue();
    assertThat(dummyOrgCpeMatchingConfigurationDtoAppTest.enabledInParent).isTrue();
    assertThat(dummyOrgCpeMatchingConfigurationDtoAppTest.allowOverride).isFalse();
    assertThat(dummyOrgCpeMatchingConfigurationDtoAppTest.inheritedFromOrganizationName)
        .isEqualTo("Root Organization");

    // Check cpe matching configuration status for app1
    CpeMatchingConfigurationDTO app1CpeMatchingConfigurationDtoAppTest =
        cpeMatchingConfigurationService.getCpeMatchingConfiguration(
            app1.getType(), app1.getId());

    assertThat(app1CpeMatchingConfigurationDtoAppTest).isNotNull();
    assertThat(app1CpeMatchingConfigurationDtoAppTest.enabled).isTrue();
    assertThat(app1CpeMatchingConfigurationDtoAppTest.enabledInParent).isTrue();
    assertThat(app1CpeMatchingConfigurationDtoAppTest.allowOverride).isFalse();
    assertThat(app1CpeMatchingConfigurationDtoAppTest.inheritedFromOrganizationName)
        .isEqualTo("Root Organization");
  }

  @Test
  public void testGetCpeMatchingConfiguration_RootParentEnabled_DummyOrgOverrides() {
    // ROOT -> Dummy Org -> App1
    Application app1 = tempEntity.newApplicationWithParent();
    Organization rootOrg = organizationDAO.getById("ROOT_ORGANIZATION_ID");
    Organization dummyOrg = organizationDAO.getById(app1.getParentOwnerId());

    CpeMatchingConfiguration rootCpeConfig = new CpeMatchingConfiguration(
        rootOrg.getId(), true, true);

    CpeMatchingConfiguration dummyOrgOverrideCpeConfig = new CpeMatchingConfiguration(
        dummyOrg.getId(), false, true);

    CpeMatchingConfiguration app1OverrideCpeConfig = new CpeMatchingConfiguration(
        app1.getId(), true, true);

    cpeMatchingConfigurationDAO.insert(rootCpeConfig);
    cpeMatchingConfigurationDAO.insert(dummyOrgOverrideCpeConfig);
    cpeMatchingConfigurationDAO.insert(app1OverrideCpeConfig);

    CpeMatchingConfigurationDTO rootCpeMatchingConfigurationDtoAppTest =
        cpeMatchingConfigurationService.getCpeMatchingConfiguration(
            rootOrg.getType(), rootOrg.getId());

    assertThat(rootCpeMatchingConfigurationDtoAppTest).isNotNull();
    assertThat(rootCpeMatchingConfigurationDtoAppTest.enabled).isTrue();
    assertThat(rootCpeMatchingConfigurationDtoAppTest.enabledInParent).isNull();
    // Allow children to override the inherited value
    assertThat(rootCpeMatchingConfigurationDtoAppTest.allowOverride).isTrue();
    assertThat(rootCpeMatchingConfigurationDtoAppTest.inheritedFromOrganizationName).isNull();

    // Check cpe matching configuration status for dummy Org
    CpeMatchingConfigurationDTO dummyOrgCpeMatchingConfigurationDtoAppTest =
        cpeMatchingConfigurationService.getCpeMatchingConfiguration(
            dummyOrg.getType(), dummyOrg.getId());

    assertThat(dummyOrgCpeMatchingConfigurationDtoAppTest).isNotNull();
    assertThat(dummyOrgCpeMatchingConfigurationDtoAppTest.enabledInParent).isTrue();
    // Overridden values at this level
    assertThat(dummyOrgCpeMatchingConfigurationDtoAppTest.enabled).isFalse();
    assertThat(dummyOrgCpeMatchingConfigurationDtoAppTest.allowOverride).isTrue();
    assertThat(dummyOrgCpeMatchingConfigurationDtoAppTest.inheritedFromOrganizationName).isNull();

    // Check cpe matching configuration status for app1
    CpeMatchingConfigurationDTO app1CpeMatchingConfigurationDtoAppTest =
        cpeMatchingConfigurationService.getCpeMatchingConfiguration(
            app1.getType(), app1.getId());

    assertThat(app1CpeMatchingConfigurationDtoAppTest).isNotNull();
    assertThat(app1CpeMatchingConfigurationDtoAppTest.enabledInParent).isFalse();
    // Override is always false at app level (because it does not have children elements)
    assertThat(app1CpeMatchingConfigurationDtoAppTest.allowOverride).isFalse();
    // Overridden values
    assertThat(app1CpeMatchingConfigurationDtoAppTest.inheritedFromOrganizationName).isNull();
    assertThat(app1CpeMatchingConfigurationDtoAppTest.enabled).isTrue();
  }

  @Test
  public void testUpdateCpeMatchingConfiguration_nonExistingApp() {
    String appId = "nonExistentId";
    CpeMatchingConfigurationRequest mockDto = new CpeMatchingConfigurationRequest();
    mockDto.enabled = true;
    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(
            () -> cpeMatchingConfigurationService.updateCpeMatchingConfiguration(OwnerType.APPLICATION, appId, mockDto))
        .withMessage("Owner with ID " + appId + " does not exist.");
  }

  @Test
  public void testUpdateCpeMatchingConfiguration_nonExistingOrg() {
    String orgId = "nonExistentId";
    CpeMatchingConfigurationRequest mockDto = new CpeMatchingConfigurationRequest();
    mockDto.enabled = true;
    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> cpeMatchingConfigurationService.updateCpeMatchingConfiguration(OwnerType.ORGANIZATION, orgId,
            mockDto))
        .withMessage("Owner with ID " + orgId + " does not exist.");
  }

  @Test
  public void testUpdateCpeMatchingConfiguration_NullConfig() {
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(
            () -> cpeMatchingConfigurationService.updateCpeMatchingConfiguration(OwnerType.APPLICATION, "appId", null))
        .withMessage("CPE matching configuration cannot be null");
  }

  @Test
  public void testUpdateCpeMatchingConfiguration_InvalidConfig_organization() {
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> cpeMatchingConfigurationService.updateCpeMatchingConfiguration(
            OwnerType.ORGANIZATION, "ROOT_ORGANIZATION_ID",
            new CpeMatchingConfigurationRequest()))
        .withMessage("CPE matching configuration enabled cannot be null for Root Organization");
  }

  @Test
  public void testUpdateCpeMatchingConfiguration_NullEnabledValue_Organization() {
    Organization org = tempEntity.newOrganization();
    CpeMatchingConfigurationRequest configRequest = new CpeMatchingConfigurationRequest();
    configRequest.enabled = null; // This should be allowed
    configRequest.allowOverride = true;
    CpeMatchingConfigurationDTO cpeConfig = cpeMatchingConfigurationService
        .updateCpeMatchingConfiguration(OwnerType.ORGANIZATION, org.getId(), configRequest);
    assertThat(cpeConfig).isNotNull();
  }

  @Test
  public void testUpdateCpeMatchingConfiguration_Application() {
    Application app = tempEntity.newApplicationWithParent();
    CpeMatchingConfigurationRequest configRequest = new CpeMatchingConfigurationRequest();
    configRequest.enabled = true;

    // enable
    cpeMatchingConfigurationService.updateCpeMatchingConfiguration(OwnerType.APPLICATION, app.getId(), configRequest);
    CpeMatchingConfiguration stored = cpeMatchingConfigurationDAO.getByOwnerId(app.getId());
    assertThat(stored).isNotNull()
        .hasFieldOrPropertyWithValue("cpeEnabled", true)
        .hasFieldOrPropertyWithValue("allowOverride", false);

    // disable
    configRequest.enabled = false;
    cpeMatchingConfigurationService.updateCpeMatchingConfiguration(OwnerType.APPLICATION, app.getId(), configRequest);
    stored = cpeMatchingConfigurationDAO.getByOwnerId(app.getId());
    assertThat(stored).isNotNull()
        .hasFieldOrPropertyWithValue("cpeEnabled", false)
        .hasFieldOrPropertyWithValue("allowOverride", false);
  }

  @Test
  public void testUpdateCpeMatchingConfiguration_Org_NoAncestorsPreviousConfig_NoChildren() {
    Organization org = tempEntity.newOrganization();
    // enable
    CpeMatchingConfigurationRequest configRequest = new CpeMatchingConfigurationRequest();
    configRequest.enabled = true;
    configRequest.allowOverride = true;

    cpeMatchingConfigurationService.updateCpeMatchingConfiguration(OwnerType.ORGANIZATION, org.getId(), configRequest);

    CpeMatchingConfiguration stored = cpeMatchingConfigurationDAO.getByOwnerId(org.getId());
    assertThat(stored).isNotNull();
    assertThat(stored.isCpeEnabled()).isTrue();
    assertThat(stored.isAllowOverride()).isTrue();

    // disable
    configRequest.enabled = false;
    configRequest.allowOverride = false;
    cpeMatchingConfigurationService.updateCpeMatchingConfiguration(OwnerType.ORGANIZATION, org.getId(), configRequest);
    stored = cpeMatchingConfigurationDAO.getByOwnerId(org.getId());
    assertThat(stored).isNotNull()
        .hasFieldOrPropertyWithValue("cpeEnabled", false)
        .hasFieldOrPropertyWithValue("allowOverride", false);
  }

  @Test
  public void testUpdateCpeMatchingConfiguration_OrgWithChildren_Enable_DisallowOverride() {
    Organization level0Org = tempEntity.newOrganization();
    Organization level1Org = tempEntity.newOrganization(level0Org);
    cpeMatchingConfigurationDAO.insert(new CpeMatchingConfiguration(level1Org.getId(), true, true));
    Application level1App = tempEntity.newApplicationWithParent(level0Org);
    cpeMatchingConfigurationDAO.insert(new CpeMatchingConfiguration(level1App.getId(), true, true));
    Application level2App = tempEntity.newApplicationWithParent(level1Org);
    cpeMatchingConfigurationDAO.insert(new CpeMatchingConfiguration(level2App.getId(), true, false));

    CpeMatchingConfigurationRequest configRequest = new CpeMatchingConfigurationRequest();
    configRequest.enabled = true;
    configRequest.allowOverride = false;

    cpeMatchingConfigurationService.updateCpeMatchingConfiguration(OwnerType.ORGANIZATION, level0Org.getId(),
        configRequest);

    // this change should cause all children to inherit the parent's value, thus removing all children configs
    CpeMatchingConfiguration level0OrgConfig = cpeMatchingConfigurationDAO.getByOwnerId(level0Org.getId());
    assertThat(level0OrgConfig).isNotNull()
        .hasFieldOrPropertyWithValue("cpeEnabled", true)
        .hasFieldOrPropertyWithValue("allowOverride", false);
    CpeMatchingConfiguration level1AppConfig = cpeMatchingConfigurationDAO.getByOwnerId(level1App.getId());
    assertThat(level1AppConfig).isNull();
    CpeMatchingConfiguration level1OrgConfig = cpeMatchingConfigurationDAO.getByOwnerId(level1Org.getId());
    assertThat(level1OrgConfig).isNull();
    CpeMatchingConfiguration level2AppConfig = cpeMatchingConfigurationDAO.getByOwnerId(level2App.getId());
    assertThat(level2AppConfig).isNull();
  }

  @Test
  public void updateCpeMatchingConfiguration_allowOverrideCheckValidation() {
    /*
     * Test data hierarchy
     * level0Org
     * |__level1Org
     * |__level1App
     * |__
     * level2Org
     * |__level2App
     * |__level3Org
     * |__level3App
     *
     */
    final String expectedExceptionMessageTemplate = "Updating cpe matching configuration for ownerId %s is disabled " +
        "by parent organization %s";
    Organization level0Org = tempEntity.newOrganization();
    Organization level1Org = tempEntity.newOrganization(level0Org);
    Organization level2Org = tempEntity.newOrganization(level1Org);
    Organization level3Org = tempEntity.newOrganization(level2Org);
    cpeMatchingConfigurationDAO.insert(new CpeMatchingConfiguration(level1Org.getId(), true, true));
    cpeMatchingConfigurationDAO.insert(new CpeMatchingConfiguration(level2Org.getId(), false, false));
    Application level1App = tempEntity.newApplicationWithParent(level1Org);
    cpeMatchingConfigurationDAO.insert(new CpeMatchingConfiguration(level1App.getId(), false, false));
    Application level2App = tempEntity.newApplicationWithParent(level2Org);
    Application level3App = tempEntity.newApplicationWithParent(level3Org);
    cpeMatchingConfigurationDAO.insert(new CpeMatchingConfiguration(level3App.getId(), true, false));

    CpeMatchingConfigurationRequest configRequest = new CpeMatchingConfigurationRequest();
    configRequest.enabled = true;
    configRequest.allowOverride = true;

    // level2App is not allowed to override because of ancestor level2Org allowOverride setting is false
    assertThatExceptionOfType(UnauthorizedException.class)
        .isThrownBy(() -> cpeMatchingConfigurationService.updateCpeMatchingConfiguration(OwnerType.APPLICATION,
            level2App.getId(), configRequest))
        .withMessageContaining(String.format(expectedExceptionMessageTemplate, level2App.getId(), level2Org.getName()));

    // level3Org not allowed to override because of ancestor level2Org allowOverride setting is false
    assertThatExceptionOfType(UnauthorizedException.class)
        .isThrownBy(() -> cpeMatchingConfigurationService.updateCpeMatchingConfiguration(OwnerType.ORGANIZATION,
            level3Org.getId(), configRequest))
        .withMessageContaining(String.format(expectedExceptionMessageTemplate, level3Org.getId(), level2Org.getName()));

    // level1Org is allowed to override because parent level0Org does not contain any cpe config configuration in db
    cpeMatchingConfigurationService.updateCpeMatchingConfiguration(OwnerType.ORGANIZATION,
        level1Org.getId(), configRequest);

    // level1App is allowed to override because parent level1Org has explicit record that allows it and ancestor
    // level0Org does not contain any cpe config configuration in db
    cpeMatchingConfigurationService.updateCpeMatchingConfiguration(OwnerType.APPLICATION,
        level1App.getId(), configRequest);
  }

  @Test
  public void testUpdateCpeMatchingConfiguration_OrgWithChildren_Disable_AllowOverride() {
    Organization level0Org = tempEntity.newOrganization();
    Organization level1Org = tempEntity.newOrganization(level0Org);
    cpeMatchingConfigurationDAO.insert(new CpeMatchingConfiguration(level1Org.getId(), true, true));
    Application level1App = tempEntity.newApplicationWithParent(level0Org);
    cpeMatchingConfigurationDAO.insert(new CpeMatchingConfiguration(level1App.getId(), true, false));
    Application level2App = tempEntity.newApplicationWithParent(level1Org);
    cpeMatchingConfigurationDAO.insert(new CpeMatchingConfiguration(level2App.getId(), true, false));

    CpeMatchingConfigurationRequest configRequest = new CpeMatchingConfigurationRequest();
    configRequest.enabled = false;
    configRequest.allowOverride = true;

    cpeMatchingConfigurationService.updateCpeMatchingConfiguration(OwnerType.ORGANIZATION,
        level0Org.getId(), configRequest);

    CpeMatchingConfiguration level0OrgConfig = cpeMatchingConfigurationDAO.getByOwnerId(level0Org.getId());
    assertThat(level0OrgConfig).isNotNull();
    assertThat(level0OrgConfig.isCpeEnabled()).isFalse();
    assertThat(level0OrgConfig.isAllowOverride()).isTrue();

    // children config should remain unchanged
    CpeMatchingConfiguration level1AppConfig = cpeMatchingConfigurationDAO.getByOwnerId(level1App.getId());
    assertThat(level1AppConfig).isNotNull()
        .hasFieldOrPropertyWithValue("cpeEnabled", true)
        .hasFieldOrPropertyWithValue("allowOverride", false);
    CpeMatchingConfiguration level1OrgConfig = cpeMatchingConfigurationDAO.getByOwnerId(level1Org.getId());
    assertThat(level1OrgConfig).isNotNull()
        .hasFieldOrPropertyWithValue("cpeEnabled", true)
        .hasFieldOrPropertyWithValue("allowOverride", true);
    CpeMatchingConfiguration level2AppConfig = cpeMatchingConfigurationDAO.getByOwnerId(level2App.getId());
    assertThat(level2AppConfig).isNotNull()
        .hasFieldOrPropertyWithValue("cpeEnabled", true)
        .hasFieldOrPropertyWithValue("allowOverride", false);
  }

  @Test
  public void testDisableCpeMatchingConfiguration_nonExistentApp() {
    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> cpeMatchingConfigurationService.disableCpeMatchingConfiguration(OwnerType.APPLICATION,
            "fakeId", null))
        .withMessageContaining("Owner with ID fakeId does not exist");
  }

  @Test
  public void testDisableCpeMatchingConfiguration_nonExistentOrg() {
    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> cpeMatchingConfigurationService.disableCpeMatchingConfiguration(OwnerType.ORGANIZATION,
            "fakeId", null))
        .withMessageContaining("Owner with ID fakeId does not exist");
  }

  @Test
  public void testDisableCpeMatchingConfiguration_App() {
    // non-existent config
    Application app = tempEntity.newApplicationWithParent();
    cpeMatchingConfigurationService.disableCpeMatchingConfiguration(OwnerType.APPLICATION, app.getId(), null);

    CpeMatchingConfiguration stored = cpeMatchingConfigurationDAO.getByOwnerId(app.getId());
    assertThat(stored).isNull();

    // existing config
    cpeMatchingConfigurationDAO.insert(new CpeMatchingConfiguration(app.getId(), true, false));
    cpeMatchingConfigurationService.disableCpeMatchingConfiguration(OwnerType.APPLICATION, app.getId(), null);
    stored = cpeMatchingConfigurationDAO.getByOwnerId(app.getId());
    assertThat(stored).isNull();
  }

  @Test
  public void testDisableCpeMatchingConfiguration_OrgWithNoChildren() {
    // disable a nonexistent config with null allowOverride
    Organization org = tempEntity.newOrganization();
    cpeMatchingConfigurationService.disableCpeMatchingConfiguration(OwnerType.ORGANIZATION, org.getId(), null);

    CpeMatchingConfiguration stored = cpeMatchingConfigurationDAO.getByOwnerId(org.getId());
    assertThat(stored).isNull(); // no effect. the overall result should be inherited from parent

    // disable a nonexistent config with allowOverride
    cpeMatchingConfigurationService.disableCpeMatchingConfiguration(OwnerType.ORGANIZATION, org.getId(), true);
    stored = cpeMatchingConfigurationDAO.getByOwnerId(org.getId());
    assertThat(stored).isNotNull()
        .hasFieldOrPropertyWithValue("cpeEnabled", false)
        .hasFieldOrPropertyWithValue("allowOverride", true);

    // disable an existing config with allowOverride
    cpeMatchingConfigurationService.disableCpeMatchingConfiguration(OwnerType.ORGANIZATION, org.getId(), false);
    stored = cpeMatchingConfigurationDAO.getByOwnerId(org.getId());
    assertThat(stored).isNotNull()
        .hasFieldOrPropertyWithValue("cpeEnabled", false)
        .hasFieldOrPropertyWithValue("allowOverride", false);
  }

  @Test
  public void testDisableCpeMatchingConfiguration_OrgWithChildren_DisallowOverride() {
    Organization level0Org = tempEntity.newOrganization();
    Organization level1Org = tempEntity.newOrganization(level0Org);
    cpeMatchingConfigurationDAO.insert(new CpeMatchingConfiguration(level1Org.getId(), true, true));
    Application level1App = tempEntity.newApplicationWithParent(level0Org);
    cpeMatchingConfigurationDAO.insert(new CpeMatchingConfiguration(level1App.getId(), true, true));
    Application level2App = tempEntity.newApplicationWithParent(level1Org);
    cpeMatchingConfigurationDAO.insert(new CpeMatchingConfiguration(level2App.getId(), true, false));

    cpeMatchingConfigurationService.disableCpeMatchingConfiguration(OwnerType.ORGANIZATION, level0Org.getId(), false);

    // this change should cause all children to inherit the parent's value, thus removing all children configs
    CpeMatchingConfiguration level0OrgConfig = cpeMatchingConfigurationDAO.getByOwnerId(level0Org.getId());
    assertThat(level0OrgConfig).isNotNull()
        .hasFieldOrPropertyWithValue("cpeEnabled", false)
        .hasFieldOrPropertyWithValue("allowOverride", false);

    CpeMatchingConfiguration level1AppConfig = cpeMatchingConfigurationDAO.getByOwnerId(level1App.getId());
    assertThat(level1AppConfig).isNull();
    CpeMatchingConfiguration level1OrgConfig = cpeMatchingConfigurationDAO.getByOwnerId(level1Org.getId());
    assertThat(level1OrgConfig).isNull();
    CpeMatchingConfiguration level2AppConfig = cpeMatchingConfigurationDAO.getByOwnerId(level2App.getId());
    assertThat(level2AppConfig).isNull();
  }

  @Test
  public void testDisableCpeMatchingConfiguration_OrgWithChildren_AllowOverride() {
    Organization level0Org = tempEntity.newOrganization();
    Organization level1Org = tempEntity.newOrganization(level0Org);
    cpeMatchingConfigurationDAO.insert(new CpeMatchingConfiguration(level1Org.getId(), true, true));
    Application level1App = tempEntity.newApplicationWithParent(level0Org);
    cpeMatchingConfigurationDAO.insert(new CpeMatchingConfiguration(level1App.getId(), true, false));
    Application level2App = tempEntity.newApplicationWithParent(level1Org);
    cpeMatchingConfigurationDAO.insert(new CpeMatchingConfiguration(level2App.getId(), true, false));

    cpeMatchingConfigurationService.disableCpeMatchingConfiguration(OwnerType.ORGANIZATION, level0Org.getId(), true);

    // children config should remain unchanged
    CpeMatchingConfiguration level0OrgConfig = cpeMatchingConfigurationDAO.getByOwnerId(level0Org.getId());
    assertThat(level0OrgConfig).isNotNull()
        .hasFieldOrPropertyWithValue("cpeEnabled", false)
        .hasFieldOrPropertyWithValue("allowOverride", true);
    CpeMatchingConfiguration level1AppConfig = cpeMatchingConfigurationDAO.getByOwnerId(level1App.getId());
    assertThat(level1AppConfig).isNotNull()
        .hasFieldOrPropertyWithValue("cpeEnabled", true)
        .hasFieldOrPropertyWithValue("allowOverride", false);
    CpeMatchingConfiguration level1OrgConfig = cpeMatchingConfigurationDAO.getByOwnerId(level1Org.getId());
    assertThat(level1OrgConfig).isNotNull()
        .hasFieldOrPropertyWithValue("cpeEnabled", true)
        .hasFieldOrPropertyWithValue("allowOverride", true);
    CpeMatchingConfiguration level2AppConfig = cpeMatchingConfigurationDAO.getByOwnerId(level2App.getId());
    assertThat(level2AppConfig).isNotNull()
        .hasFieldOrPropertyWithValue("cpeEnabled", true)
        .hasFieldOrPropertyWithValue("allowOverride", false);
  }

  @Test
  public void testIsCpeDataMatchingEnabled_LicenseDoesNotHaveCpeMatchingFeature() {
    productLicense.setMissingFeatures(LicensedFeature.CPE_MATCHING);
    Application app = tempEntity.newApplicationWithParent();

    boolean isCpeDataMatchingEnabled = cpeMatchingConfigurationService.isCpeDataMatchingEnabled(app.getId());

    assertThat(isCpeDataMatchingEnabled).isFalse();
  }

  @Test
  public void testIsCpeDataMatchingEnabled_ConfigurationDisabled_CpeMatchingMustBeEnabledForSbomManagerOnlyLicenses() {

    Stream.of(ProductLicenseDetails.PRODUCT_SBOM_MANAGER, ProductLicenseDetails.PRODUCT_SBOM_MANAGER_SAAS)
        .forEach(product -> {
          productLicense.setProducts(product);
          Application app = tempEntity.newApplicationWithParent();
          CpeMatchingConfiguration appCpeConfig = new CpeMatchingConfiguration(app.getId(), false, false);
          cpeMatchingConfigurationDAO.insert(appCpeConfig);

          boolean isCpeDataMatchingEnabled = cpeMatchingConfigurationService.isCpeDataMatchingEnabled(app.getId());

          assertThat(isCpeDataMatchingEnabled).isTrue();
        });
  }

  @Test
  public void testIsCpeDataMatchingEnabled_LicenseContainsOnlySbomManagerProducts_MultipleProducts() {
    productLicense.setProducts(ProductLicenseDetails.PRODUCT_SBOM_MANAGER,
        ProductLicenseDetails.PRODUCT_ADVANCED_LEGAL_PACK);

    Application app = tempEntity.newApplicationWithParent();
    CpeMatchingConfiguration appCpeConfig = new CpeMatchingConfiguration(app.getId(), false, false);
    cpeMatchingConfigurationDAO.insert(appCpeConfig);

    boolean isCpeDataMatchingEnabled = cpeMatchingConfigurationService.isCpeDataMatchingEnabled(app.getId());

    assertThat(isCpeDataMatchingEnabled).isTrue();
  }

  @Test
  public void testIsCpeDataMatchingEnabled_ConfigurationDisabled_CpeMatchingMustBeDisabledForLifecycleOnlyLicenses() {
    Stream.of(ProductLicenseDetails.PRODUCT_RISK_AND_REMEDIATION,
        ProductLicenseDetails.PRODUCT_LIFECYCLE_SAAS,
        ProductLicenseDetails.PRODUCT_LIFECYCLE_CLOUD,
        ProductLicenseDetails.PRODUCT_TEAMS_EDITION)
        .forEach(product -> {
          productLicense.setProducts(product);
          Application app = tempEntity.newApplicationWithParent();
          CpeMatchingConfiguration appCpeConfig = new CpeMatchingConfiguration(app.getId(), false, false);
          cpeMatchingConfigurationDAO.insert(appCpeConfig);

          boolean isCpeDataMatchingEnabled = cpeMatchingConfigurationService.isCpeDataMatchingEnabled(app.getId());

          assertThat(isCpeDataMatchingEnabled).isFalse();
        });
  }

  @Test
  public void testIsCpeDataMatchingEnabled_ConfigurationEnabled_CpeMatchingMustBeEnabledForLifecycleOnlyLicenses() {
    productLicense.setProducts(ProductLicenseDetails.PRODUCT_LIFECYCLE_SAAS);

    Application app = tempEntity.newApplicationWithParent();
    CpeMatchingConfiguration appCpeConfig = new CpeMatchingConfiguration(app.getId(), true, false);
    cpeMatchingConfigurationDAO.insert(appCpeConfig);

    boolean isCpeDataMatchingEnabled = cpeMatchingConfigurationService.isCpeDataMatchingEnabled(app.getId());

    assertThat(isCpeDataMatchingEnabled).isTrue();
  }

  @Test
  public void testIsCpeDataMatchingEnabled_ConfigurationDisabled_CpeMatchingMustBeDisabledForMixedLicensesWithSM() {
    productLicense.setProducts(ProductLicenseDetails.PRODUCT_LIFECYCLE_SAAS,
        ProductLicenseDetails.PRODUCT_SBOM_MANAGER);

    Application app = tempEntity.newApplicationWithParent();
    CpeMatchingConfiguration appCpeConfig = new CpeMatchingConfiguration(app.getId(), false, false);
    cpeMatchingConfigurationDAO.insert(appCpeConfig);

    boolean isCpeDataMatchingEnabled = cpeMatchingConfigurationService.isCpeDataMatchingEnabled(app.getId());

    assertThat(isCpeDataMatchingEnabled).isFalse();
  }

  @Test
  public void testIsCpeDataMatchingEnabled_ConfigurationEnabled_CpeMatchingMustBeEnabledForMixedLicensesWithSM() {
    productLicense.setProducts(ProductLicenseDetails.PRODUCT_LIFECYCLE_SAAS,
        ProductLicenseDetails.PRODUCT_SBOM_MANAGER);

    Application app = tempEntity.newApplicationWithParent();
    CpeMatchingConfiguration appCpeConfig = new CpeMatchingConfiguration(app.getId(), true, false);
    cpeMatchingConfigurationDAO.insert(appCpeConfig);

    boolean isCpeDataMatchingEnabled = cpeMatchingConfigurationService.isCpeDataMatchingEnabled(app.getId());

    assertThat(isCpeDataMatchingEnabled).isTrue();
  }

  @Test
  public void testIsCpeDataMatchingEnabled_ConfigurationDisabled_CpeMatchingMustBeDisabledForMixedLicensesWithoutSM() {
    productLicense.setProducts(ProductLicenseDetails.PRODUCT_NEXUS, ProductLicenseDetails.PRODUCT_LIFECYCLE_SAAS);

    Application app = tempEntity.newApplicationWithParent();
    CpeMatchingConfiguration appCpeConfig = new CpeMatchingConfiguration(app.getId(), false, false);
    cpeMatchingConfigurationDAO.insert(appCpeConfig);

    boolean isCpeDataMatchingEnabled = cpeMatchingConfigurationService.isCpeDataMatchingEnabled(app.getId());

    assertThat(isCpeDataMatchingEnabled).isFalse();
  }

  @Test
  public void testUpdateCpeMatchingConfiguration_ForApplication_RootOrgAllowOverride_SubOrgDoNotAllowOverride() {
    Organization subOrg = tempEntity.newOrganization();
    CpeMatchingConfiguration subOrgCpeConfig = new CpeMatchingConfiguration(
        subOrg.getId(), true, false);
    cpeMatchingConfigurationDAO.insert(subOrgCpeConfig);

    Application app = tempEntity.newApplicationWithParent(subOrg);

    CpeMatchingConfigurationRequest request = new CpeMatchingConfigurationRequest();
    request.enabled = false;
    request.allowOverride = false;
    assertThatExceptionOfType(UnauthorizedException.class)
        .isThrownBy(() -> cpeMatchingConfigurationService.updateCpeMatchingConfiguration(
            OwnerType.APPLICATION, app.getId(), request))
        .withMessageContaining("Updating cpe matching configuration for ownerId " + app.getId()
            + " is disabled by parent organization " + subOrg.getName());
  }

  @Test
  public void testGetCpeMatchingConfiguration_ForApplication_ParentHasNullEnableValue_RootOrgHasExplicitEnableValue() {
    // ROOT -> Dummy Org -> App1
    cpeMatchingConfigurationDAO.insert(new CpeMatchingConfiguration(Organization.ROOT_ORGANIZATION_ID, true, true));
    Organization subOrg = tempEntity.newOrganization();
    Application app1 = tempEntity.newApplicationWithParent(subOrg);
    CpeMatchingConfiguration subOrgCpeConfig = new CpeMatchingConfiguration(
        subOrg.getId(), null, false);
    cpeMatchingConfigurationDAO.insert(subOrgCpeConfig);
    CpeMatchingConfigurationDTO appCpeConfig = cpeMatchingConfigurationService
        .getCpeMatchingConfiguration(app1.getType(), app1.getId());
    assertThat(appCpeConfig).isNotNull();
    assertThat(appCpeConfig.enabled).isTrue(); // Taken from Root Org
    assertThat(appCpeConfig.enabledInParent).isTrue(); // Taken from Root Org
    assertThat(appCpeConfig.allowOverride).isFalse(); // Taken from Parent Org
    assertThat(appCpeConfig.inheritedFromOrganizationName).isEqualTo("Root Organization");
    assertThat(appCpeConfig.inheritedFromOrganizationAllowOverride).isFalse(); // Taken from Parent Org
  }

  @Test
  public void isCpeDataMatchingEnabled_application_twoArgOverload_matchesShim() {
    Application app = tempEntity.newApplicationWithParent();
    cpeMatchingConfigurationDAO.insert(new CpeMatchingConfiguration(app.getId(), true, false));

    boolean singleArg = cpeMatchingConfigurationService.isCpeDataMatchingEnabled(app.getId());
    boolean twoArg = cpeMatchingConfigurationService.isCpeDataMatchingEnabled(OwnerType.APPLICATION, app.getId());

    assertThat(twoArg).isEqualTo(singleArg);
    assertThat(twoArg).isTrue();
  }

  @Test
  public void isCpeDataMatchingEnabled_repository_walksAncestorChainInheritedFromRootOrg() {
    // Root Organization has CPE enabled; a Repository under the default RepositoryContainer inherits it.
    // Ancestor chain: Repository -> RepositoryManager -> REPOSITORY_CONTAINER_ID -> ROOT_ORGANIZATION_ID
    cpeMatchingConfigurationDAO.insert(new CpeMatchingConfiguration(Organization.ROOT_ORGANIZATION_ID, true, true));
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    Repository repo = tempEntity.newRepository(repositoryManager);

    // Repository has no own cpe_matching_configuration row -> must inherit from Root Organization ancestor.
    boolean result =
        cpeMatchingConfigurationService.isCpeDataMatchingEnabled(OwnerType.REPOSITORY, repo.getId());

    assertThat(result).isTrue();
  }

  @Test
  public void isCpeDataMatchingEnabled_sbomManagerOnly_returnsTrueForAllOwnerTypes() {
    productLicense.setProducts(ProductLicenseDetails.PRODUCT_SBOM_MANAGER);

    Application app = tempEntity.newApplicationWithParent();
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    Repository repo = tempEntity.newRepository(repositoryManager);

    assertThat(cpeMatchingConfigurationService.isCpeDataMatchingEnabled(OwnerType.APPLICATION, app.getId()))
        .isTrue();
    assertThat(cpeMatchingConfigurationService.isCpeDataMatchingEnabled(OwnerType.REPOSITORY, repo.getId()))
        .isTrue();
  }

  @Test
  public void getCpeMatchingConfigurationNoAuthz_unknownOwnerId_throwsNotFoundException() {
    // Use an ID that resolves to no owner subtype in OwnerDAO.
    String bogusId = "00000000-0000-0000-0000-000000000000";

    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> cpeMatchingConfigurationService.getCpeMatchingConfigurationNoAuthz(
            OwnerType.APPLICATION, bogusId));
  }

  @Test
  public void isCpeDataMatchingEnabled_ownerOverload_matchesTwoArgVariantForApplication() {
    Application app = tempEntity.newApplicationWithParent();
    cpeMatchingConfigurationDAO.insert(new CpeMatchingConfiguration(app.getId(), true, false));

    boolean twoArg = cpeMatchingConfigurationService.isCpeDataMatchingEnabled(OwnerType.APPLICATION, app.getId());
    boolean ownerArg = cpeMatchingConfigurationService.isCpeDataMatchingEnabled(app);

    assertThat(ownerArg).isEqualTo(twoArg);
    assertThat(ownerArg).isTrue();
  }

  @Test
  public void isCpeDataMatchingEnabled_ownerOverload_matchesTwoArgVariantForRepository() {
    // Root Organization has CPE enabled; a Repository inherits via the ancestor chain.
    cpeMatchingConfigurationDAO.insert(new CpeMatchingConfiguration(Organization.ROOT_ORGANIZATION_ID, true, true));
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    Repository repo = tempEntity.newRepository(repositoryManager);

    boolean twoArg = cpeMatchingConfigurationService.isCpeDataMatchingEnabled(OwnerType.REPOSITORY, repo.getId());
    boolean ownerArg = cpeMatchingConfigurationService.isCpeDataMatchingEnabled(repo);

    assertThat(ownerArg).isEqualTo(twoArg);
    assertThat(ownerArg).isTrue();
  }

  @Test
  public void getCpeMatchingConfigurationNoAuthz_ownerOverload_matchesTwoArgVariant() {
    Application app = tempEntity.newApplicationWithParent();
    cpeMatchingConfigurationDAO.insert(new CpeMatchingConfiguration(app.getId(), true, false));

    CpeMatchingConfigurationDTO twoArg =
        cpeMatchingConfigurationService.getCpeMatchingConfigurationNoAuthz(OwnerType.APPLICATION, app.getId());
    CpeMatchingConfigurationDTO ownerArg =
        cpeMatchingConfigurationService.getCpeMatchingConfigurationNoAuthz(app);

    assertThat(ownerArg.enabled).isEqualTo(twoArg.enabled);
    assertThat(ownerArg.allowOverride).isEqualTo(twoArg.allowOverride);
    assertThat(ownerArg.enabledInParent).isEqualTo(twoArg.enabledInParent);
  }
}
