/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.cpematching;

import javax.inject.Inject;

import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.configuration.CpeMatchingConfigurationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.configuration.CpeMatchingConfiguration;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.test.LogOutput;

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

  @Rule
  public LogOutput logOutput = new LogOutput(CpeMatchingConfigurationService.class);

  @Test
  public void testGetCpeMatchingStatus_ForApp_NoParentEnabled_NoOverrides() {
    Application app1 = tempEntity.newApplicationWithParent();
    CpeMatchingConfiguration appCpeConfig = new CpeMatchingConfiguration(
        app1.getPublicId(), true, true
    );

    cpeMatchingConfigurationDAO.insert(appCpeConfig);

    CpeMatchingConfigurationDTO actualCpeMatchingConfiguration = cpeMatchingConfigurationService.getCpeMatchingStatus(
        app1.getType(), app1.getPublicId());
    assertThat(actualCpeMatchingConfiguration).isNotNull();
    assertThat(actualCpeMatchingConfiguration.enabled).isTrue();
    assertThat(actualCpeMatchingConfiguration.enabledInParent).isFalse();
    assertThat(actualCpeMatchingConfiguration.allowOverride).isFalse();
    assertThat(actualCpeMatchingConfiguration.inheritedFromOrganizationName).isNull();
  }

  @Test
  public void testGetCpeMatchingStatus_ForOrg_NoParentEnabled_NoOverrides() {
    Application app1 = tempEntity.newApplicationWithParent();
    Organization parentOrg = organizationDAO.getById(app1.getParentOwnerId());
    CpeMatchingConfiguration orgCpeConfig = new CpeMatchingConfiguration(
        parentOrg.getId(), true, false
    );
    cpeMatchingConfigurationDAO.insert(orgCpeConfig);

    CpeMatchingConfigurationDTO actualCpeMatchingConfiguration = cpeMatchingConfigurationService.getCpeMatchingStatus(
        parentOrg.getType(), parentOrg.getId());
    assertThat(actualCpeMatchingConfiguration).isNotNull();
    assertThat(actualCpeMatchingConfiguration.enabled).isTrue();
    assertThat(actualCpeMatchingConfiguration.enabledInParent).isFalse();
    assertThat(actualCpeMatchingConfiguration.allowOverride).isFalse();
    assertThat(actualCpeMatchingConfiguration.inheritedFromOrganizationName).isNull();
  }

  @Test
  public void testGetCpeMatchingStatus_RootParentEnabled_NoOverrides() {
    // ROOT -> Dummy Org -> App1
    Application app1 = tempEntity.newApplicationWithParent();
    Organization rootOrg = organizationDAO.getById("ROOT_ORGANIZATION_ID");
    Organization dummyOrg = organizationDAO.getById(app1.getParentOwnerId());

    CpeMatchingConfiguration rootCpeConfig = new CpeMatchingConfiguration(
        rootOrg.getId(), true, false
    );

    cpeMatchingConfigurationDAO.insert(rootCpeConfig);

    CpeMatchingConfigurationDTO rootCpeMatchingConfigurationDtoAppTest = cpeMatchingConfigurationService
        .getCpeMatchingStatus(rootOrg.getType(), rootOrg.getId());

    assertThat(rootCpeMatchingConfigurationDtoAppTest).isNotNull();
    assertThat(rootCpeMatchingConfigurationDtoAppTest.enabled).isTrue();
    assertThat(rootCpeMatchingConfigurationDtoAppTest.enabledInParent).isFalse();
    assertThat(rootCpeMatchingConfigurationDtoAppTest.allowOverride).isFalse();
    assertThat(rootCpeMatchingConfigurationDtoAppTest.inheritedFromOrganizationName).isNull();

    // Check cpe matching configuration status for dummy Org
    CpeMatchingConfigurationDTO dummyOrgCpeMatchingConfigurationDtoAppTest =
        cpeMatchingConfigurationService.getCpeMatchingStatus(
            dummyOrg.getType(), dummyOrg.getId());

    assertThat(dummyOrgCpeMatchingConfigurationDtoAppTest).isNotNull();
    assertThat(dummyOrgCpeMatchingConfigurationDtoAppTest.enabled).isTrue();
    assertThat(dummyOrgCpeMatchingConfigurationDtoAppTest.enabledInParent).isTrue();
    assertThat(dummyOrgCpeMatchingConfigurationDtoAppTest.allowOverride).isFalse();
    assertThat(dummyOrgCpeMatchingConfigurationDtoAppTest.inheritedFromOrganizationName)
        .isEqualTo("Root Organization");

    // Check cpe matching configuration status for app1
    CpeMatchingConfigurationDTO app1CpeMatchingConfigurationDtoAppTest =
        cpeMatchingConfigurationService.getCpeMatchingStatus(
            app1.getType(), app1.getPublicId());

    assertThat(app1CpeMatchingConfigurationDtoAppTest).isNotNull();
    assertThat(app1CpeMatchingConfigurationDtoAppTest.enabled).isTrue();
    assertThat(app1CpeMatchingConfigurationDtoAppTest.enabledInParent).isTrue();
    assertThat(app1CpeMatchingConfigurationDtoAppTest.allowOverride).isFalse();
    assertThat(app1CpeMatchingConfigurationDtoAppTest.inheritedFromOrganizationName)
        .isEqualTo("Root Organization");
  }

  @Test
  public void testGetCpeMatchingStatus_RootParentEnabled_DummyOrgOverrides() {
    // ROOT -> Dummy Org -> App1
    Application app1 = tempEntity.newApplicationWithParent();
    Organization rootOrg = organizationDAO.getById("ROOT_ORGANIZATION_ID");
    Organization dummyOrg = organizationDAO.getById(app1.getParentOwnerId());

    CpeMatchingConfiguration rootCpeConfig = new CpeMatchingConfiguration(
        rootOrg.getId(), true, true
    );

    CpeMatchingConfiguration dummyOrgOverrideCpeConfig = new CpeMatchingConfiguration(
        dummyOrg.getId(), false, true
    );

    CpeMatchingConfiguration app1OverrideCpeConfig = new CpeMatchingConfiguration(
        app1.getPublicId(), true, true
    );

    cpeMatchingConfigurationDAO.insert(rootCpeConfig);
    cpeMatchingConfigurationDAO.insert(dummyOrgOverrideCpeConfig);
    cpeMatchingConfigurationDAO.insert(app1OverrideCpeConfig);

    CpeMatchingConfigurationDTO rootCpeMatchingConfigurationDtoAppTest =
        cpeMatchingConfigurationService.getCpeMatchingStatus(
            rootOrg.getType(), rootOrg.getId());

    assertThat(rootCpeMatchingConfigurationDtoAppTest).isNotNull();
    assertThat(rootCpeMatchingConfigurationDtoAppTest.enabled).isTrue();
    assertThat(rootCpeMatchingConfigurationDtoAppTest.enabledInParent).isFalse();
    //Allow children to override the inherited value
    assertThat(rootCpeMatchingConfigurationDtoAppTest.allowOverride).isTrue();
    assertThat(rootCpeMatchingConfigurationDtoAppTest.inheritedFromOrganizationName).isNull();

    // Check cpe matching configuration status for dummy Org
    CpeMatchingConfigurationDTO dummyOrgCpeMatchingConfigurationDtoAppTest =
        cpeMatchingConfigurationService.getCpeMatchingStatus(
            dummyOrg.getType(), dummyOrg.getId());

    assertThat(dummyOrgCpeMatchingConfigurationDtoAppTest).isNotNull();
    assertThat(dummyOrgCpeMatchingConfigurationDtoAppTest.enabledInParent).isTrue();
    // Overridden values at this level
    assertThat(dummyOrgCpeMatchingConfigurationDtoAppTest.enabled).isFalse();
    assertThat(dummyOrgCpeMatchingConfigurationDtoAppTest.allowOverride).isTrue();
    assertThat(dummyOrgCpeMatchingConfigurationDtoAppTest.inheritedFromOrganizationName).isNull();

    // Check cpe matching configuration status for app1
    CpeMatchingConfigurationDTO app1CpeMatchingConfigurationDtoAppTest =
        cpeMatchingConfigurationService.getCpeMatchingStatus(
            app1.getType(), app1.getPublicId());

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
    assertThatExceptionOfType(NotFoundException.class).isThrownBy(() ->
            cpeMatchingConfigurationService.updateCpeMatchingConfiguration(OwnerType.APPLICATION, appId, mockDto))
        .withMessage("Owner with ID " + appId + " does not exist.");
  }

  @Test
  public void testUpdateCpeMatchingConfiguration_nonExistingOrg() {
    String orgId = "nonExistentId";
    CpeMatchingConfigurationRequest mockDto = new CpeMatchingConfigurationRequest();
    mockDto.enabled = true;
    assertThatExceptionOfType(NotFoundException.class).isThrownBy(() ->
            cpeMatchingConfigurationService.updateCpeMatchingConfiguration(OwnerType.APPLICATION, orgId, mockDto))
        .withMessage("Owner with ID " + orgId + " does not exist.");
  }

  @Test
  public void testUpdateCpeMatchingConfiguration_NullConfig() {
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() ->
            cpeMatchingConfigurationService.updateCpeMatchingConfiguration(OwnerType.APPLICATION, "appId", null))
        .withMessage("CPE matching configuration cannot be null");
  }

  @Test
  public void testUpdateCpeMatchingConfiguration_InvalidConfig() {
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() ->
            cpeMatchingConfigurationService.updateCpeMatchingConfiguration(OwnerType.APPLICATION, "appId",
                new CpeMatchingConfigurationRequest()))
        .withMessage("CPE matching configuration enabled cannot be null");
  }

  @Test
  public void testUpdateCpeMatchingConfiguration_Application() {
    Application app = tempEntity.newApplicationWithParent();
    CpeMatchingConfigurationRequest configRequest = new CpeMatchingConfigurationRequest();
    configRequest.enabled = true;

    //enable
    cpeMatchingConfigurationService.updateCpeMatchingConfiguration(OwnerType.APPLICATION, app.getId(), configRequest);
    CpeMatchingConfiguration stored = cpeMatchingConfigurationDAO.getByOwnerId(app.getId());
    assertThat(stored).isNotNull()
        .hasFieldOrPropertyWithValue("cpeEnabled", true)
        .hasFieldOrPropertyWithValue("allowOverride", false);

    //disable
    configRequest.enabled = false;
    cpeMatchingConfigurationService.updateCpeMatchingConfiguration(OwnerType.APPLICATION, app.getId(), configRequest);
    stored = cpeMatchingConfigurationDAO.getByOwnerId(app.getId());
    assertThat(stored).isNotNull()
        .hasFieldOrPropertyWithValue("cpeEnabled", false)
        .hasFieldOrPropertyWithValue("allowOverride", false);
  }

  @Test
  public void testUpdateCpeMatchingConfiguration_Org_NoChildren() {
    Organization org = tempEntity.newOrganization();
    //enable
    CpeMatchingConfigurationRequest configRequest = new CpeMatchingConfigurationRequest();
    configRequest.enabled = true;
    configRequest.allowOverride = true;

    cpeMatchingConfigurationService.updateCpeMatchingConfiguration(OwnerType.ORGANIZATION, org.getId(), configRequest);
    CpeMatchingConfiguration stored = cpeMatchingConfigurationDAO.getByOwnerId(org.getId());
    assertThat(stored).isNotNull()
        .hasFieldOrPropertyWithValue("cpeEnabled", true)
        .hasFieldOrPropertyWithValue("allowOverride", true);

    //disable
    configRequest.enabled = false;
    configRequest.allowOverride = false;
    cpeMatchingConfigurationService.updateCpeMatchingConfiguration(OwnerType.APPLICATION, org.getId(), configRequest);
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

    //this change should cause all children to inherit the parent's value, thus removing all children configs
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

    cpeMatchingConfigurationService.updateCpeMatchingConfiguration(OwnerType.ORGANIZATION, level0Org.getId(),
        configRequest);
    CpeMatchingConfiguration level0OrgConfig = cpeMatchingConfigurationDAO.getByOwnerId(level0Org.getId());
    assertThat(level0OrgConfig).isNotNull()
        .hasFieldOrPropertyWithValue("cpeEnabled", false)
        .hasFieldOrPropertyWithValue("allowOverride", true);

    //children config should remain unchanged
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
        .isThrownBy(() ->
            cpeMatchingConfigurationService.disableCpeMatchingConfiguration(OwnerType.APPLICATION, "fakeId", null))
        .withMessageContaining("Owner with ID fakeId does not exist");
  }

  @Test
  public void testDisableCpeMatchingConfiguration_nonExistentOrg() {
    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() ->
            cpeMatchingConfigurationService.disableCpeMatchingConfiguration(OwnerType.ORGANIZATION, "fakeId", null))
        .withMessageContaining("Owner with ID fakeId does not exist");
  }

  @Test
  public void testDisableCpeMatchingConfiguration_App() {
    //non-existent config
    Application app = tempEntity.newApplicationWithParent();
    cpeMatchingConfigurationService.disableCpeMatchingConfiguration(OwnerType.APPLICATION, app.getId(), null);

    CpeMatchingConfiguration stored = cpeMatchingConfigurationDAO.getByOwnerId(app.getId());
    assertThat(stored).isNull();

    //existing config
    cpeMatchingConfigurationDAO.insert(new CpeMatchingConfiguration(app.getId(), true, false));
    cpeMatchingConfigurationService.disableCpeMatchingConfiguration(OwnerType.APPLICATION, app.getId(), null);
    stored = cpeMatchingConfigurationDAO.getByOwnerId(app.getId());
    assertThat(stored).isNull();
  }

  @Test
  public void testDisableCpeMatchingConfiguration_OrgWithNoChildren() {
    //disable a nonexistent config with null allowOverride
    Organization org = tempEntity.newOrganization();
    cpeMatchingConfigurationService.disableCpeMatchingConfiguration(OwnerType.ORGANIZATION, org.getId(), null);

    CpeMatchingConfiguration stored = cpeMatchingConfigurationDAO.getByOwnerId(org.getId());
    assertThat(stored).isNull(); //no effect. the overall result should inherit from parent

    //disable a nonexistent config with allowOverride
    cpeMatchingConfigurationService.disableCpeMatchingConfiguration(OwnerType.ORGANIZATION, org.getId(), true);
    stored = cpeMatchingConfigurationDAO.getByOwnerId(org.getId());
    assertThat(stored).isNotNull()
        .hasFieldOrPropertyWithValue("cpeEnabled", false)
        .hasFieldOrPropertyWithValue("allowOverride", true);

    //disable an existing config with allowOverride
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

    //this change should cause all children to inherit the parent's value, thus removing all children configs
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

    //children config should remain unchanged
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
}

