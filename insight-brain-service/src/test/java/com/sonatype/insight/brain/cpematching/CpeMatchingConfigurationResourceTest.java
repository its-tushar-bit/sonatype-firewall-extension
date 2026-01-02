/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.cpematching;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.configuration.CpeMatchingConfigurationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.configuration.CpeMatchingConfiguration;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.license.model.LicensedFeature;

import org.junit.Before;
import org.junit.Test;

import static com.sonatype.insight.brain.api.PublicApiPaths.CPE_MATCHING_CONFIGURATION_RESOURCE_PATH;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.experimental.categories.Category;

public class CpeMatchingConfigurationResourceTest
    extends AbstractResourceTest
{
  public static final String CPE_MATCHING_OVERRIDING_DISABLED_ERROR_TEMPLATE =
      "Updating cpe matching configuration for ownerId %s is disabled by parent organization %s";

  private OrganizationDAO organizationDAO;

  private CpeMatchingConfigurationDAO cpeMatchingConfigurationDAO;

  @Before
  public void setUp() {
    organizationDAO = lookup(OrganizationDAO.class);
    cpeMatchingConfigurationDAO = lookup(CpeMatchingConfigurationDAO.class);
    getTestProductLicenseManager().setFeatures(LicensedFeature.CPE_MATCHING);
  }

  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(CPE_MATCHING_CONFIGURATION_RESOURCE_PATH);
  }

  @Test
  public void testUpdateCpeMatchingConfiguration_toApplication() throws Exception {
    Application app1 = tempEntity.newApplicationWithParent();
    CpeMatchingConfigurationRequest requestDTO = new CpeMatchingConfigurationRequest();
    requestDTO.enabled = true;
    HttpResponse response = restRequest().parameter("application", app1.getId())
        .body(requestDTO).put();
    assertResponseStatus(200, response);
    CpeMatchingConfigurationDTO actualRestResponse = response.getBody(CpeMatchingConfigurationDTO.class);
    assertThat(actualRestResponse).isNotNull();
    assertThat(actualRestResponse.enabled).isTrue();
    assertThat(actualRestResponse.allowOverride).isFalse();
  }

  @Test
  public void testUpdateCpeMatchingConfiguration_toOrganization() throws Exception {
    Application app1 = tempEntity.newApplicationWithParent();
    Organization org1 = organizationDAO.getById(app1.getParentOwnerId());
    CpeMatchingConfigurationRequest requestDTO = new CpeMatchingConfigurationRequest();
    requestDTO.enabled = true;
    requestDTO.allowOverride = true;
    HttpResponse response = restRequest().parameter("organization", org1.getId())
        .body(requestDTO).put();
    assertResponseStatus(200, response);
    CpeMatchingConfigurationDTO actualRestResponse = response.getBody(CpeMatchingConfigurationDTO.class);
    assertThat(actualRestResponse.enabled).isTrue();
    assertThat(actualRestResponse.allowOverride).isTrue();
  }

  @Test
  public void testUpdateCpeMatchingConfiguration_toApplication_errorWhenParentDisallowsOverriding() throws Exception {
    Application app1 = tempEntity.newApplicationWithParent();
    Organization org1 = organizationDAO.getById(app1.getParentOwnerId());
    cpeMatchingConfigurationDAO.insert(new CpeMatchingConfiguration(org1.getId(), false, false));
    CpeMatchingConfigurationRequest requestDTO = new CpeMatchingConfigurationRequest();
    requestDTO.enabled = true;
    requestDTO.allowOverride = true;
    HttpResponse response = restRequest().parameter("application", app1.getId())
        .body(requestDTO).put();
    assertResponseStatus(403, response);
    assertThat(response.getBodyText()).isEqualTo(
        String.format(CPE_MATCHING_OVERRIDING_DISABLED_ERROR_TEMPLATE, app1.getId(),
            org1.getName()));
  }

  @Test
  public void testUpdateCpeMatchingConfiguration_toOrganization_errorWhenParentDisallowsOverriding() throws Exception {
    Organization org1 = tempEntity.newOrganization();
    Organization org2 = tempEntity.newOrganization(org1);
    cpeMatchingConfigurationDAO.insert(new CpeMatchingConfiguration(org1.getId(), false, false));
    CpeMatchingConfigurationRequest requestDTO = new CpeMatchingConfigurationRequest();
    requestDTO.enabled = true;
    requestDTO.allowOverride = true;
    HttpResponse response = restRequest().parameter("organization", org2.getId())
        .body(requestDTO).put();
    assertResponseStatus(403, response);
    assertThat(response.getBodyText()).isEqualTo(
        String.format(CPE_MATCHING_OVERRIDING_DISABLED_ERROR_TEMPLATE, org2.getId(),
            org1.getName()));
  }

  @Test
  public void testUpdateCpeMatchingConfiguration_noRequestObjectError() throws Exception {
    Application app1 = tempEntity.newApplicationWithParent();
    HttpResponse response = restRequest().parameter("application", app1.getId()).put();
    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).isEqualTo("CPE matching configuration cannot be null");
  }

  @Test
  public void testUpdateCpeMatchingConfiguration_notFoundError() throws Exception {
    HttpResponse response = restRequest().parameter("application", "fakeApp")
        .body(new CpeMatchingConfigurationRequest()).put();
    assertResponseStatus(404, response);
    assertThat(response.getBodyText()).isEqualTo("Application with ID fakeApp does not exist.");
  }

  @Test
  public void testUpdateCpeMatchingConfiguration_licenseFeatureNotEnabledError() throws Exception {
    getTestProductLicenseManager().getFeatures().remove(LicensedFeature.CPE_MATCHING);
    Application app1 = tempEntity.newApplicationWithParent();
    CpeMatchingConfigurationRequest requestDTO = new CpeMatchingConfigurationRequest();
    requestDTO.enabled = true;
    HttpResponse response = restRequest().parameter("application", app1.getId())
        .body(requestDTO).put();
    assertResponseStatus(402, response);
    assertThat(response.getBodyText()).isEqualTo("Your IQ Server license does not enable this feature.");
  }

  @Test
  public void testGetCpeMatchingConfiguration_application() throws Exception {
    Application app1 = tempEntity.newApplicationWithParent();
    CpeMatchingConfiguration savedCpeMatchingConfig = new CpeMatchingConfiguration(app1.getId(), true,
        true);
    cpeMatchingConfigurationDAO.insert(savedCpeMatchingConfig);

    ApplicationDAO aDao = lookup(ApplicationDAO.class);
    aDao.getById(app1.getId());

    HttpResponse response = restRequest().parameter("application", app1.getId()).get();
    assertResponseStatus(200, response);
    CpeMatchingConfigurationDTO actualRestResponse = response.getBody(CpeMatchingConfigurationDTO.class);
    assertThat(actualRestResponse).isNotNull();
    assertThat(actualRestResponse.enabled).isTrue();
    assertThat(actualRestResponse.allowOverride).isFalse();
    assertThat(actualRestResponse.inheritedFromOrganizationName).isNull();
    assertThat(actualRestResponse.enabledInParent).isNull();
  }

  @Test
  public void testGetCpeMatchingConfiguration_organization() throws Exception {
    Organization org1 = tempEntity.newOrganization();
    CpeMatchingConfiguration savedCpeMatchingConfig = new CpeMatchingConfiguration(org1.getId(), true,
        false);
    cpeMatchingConfigurationDAO.insert(savedCpeMatchingConfig);

    HttpResponse response = restRequest().parameter("organization", org1.getId()).get();
    assertResponseStatus(200, response);
    CpeMatchingConfigurationDTO actualRestResponse = response.getBody(CpeMatchingConfigurationDTO.class);
    assertThat(actualRestResponse).isNotNull();
    assertThat(actualRestResponse.enabled).isTrue();
    assertThat(actualRestResponse.allowOverride).isFalse();
    assertThat(actualRestResponse.inheritedFromOrganizationName).isNull();
    assertThat(actualRestResponse.enabledInParent).isNull();
  }

  @Test
  public void testGetCpeMatchingConfiguration_getInheritedConfig_noOverrides() throws Exception {
    Application app1 = tempEntity.newApplicationWithParent();
    Organization org1 = organizationDAO.getById(app1.getParentOwnerId());
    Organization root = organizationDAO.getById(Organization.ROOT_ORGANIZATION_ID);
    CpeMatchingConfiguration rootSavedCpeMatchingConfig = new CpeMatchingConfiguration(root.getId(), true, false);
    cpeMatchingConfigurationDAO.insert(rootSavedCpeMatchingConfig);
    CpeMatchingConfiguration orgSavedCpeMatchingConfig = new CpeMatchingConfiguration(org1.getId(), true,
        true);
    cpeMatchingConfigurationDAO.insert(orgSavedCpeMatchingConfig);

    HttpResponse response = restRequest().parameter("application", app1.getId()).get();
    assertResponseStatus(200, response);
    CpeMatchingConfigurationDTO actualRestResponse = response.getBody(CpeMatchingConfigurationDTO.class);
    assertThat(actualRestResponse).isNotNull();
    assertThat(actualRestResponse.enabled).isTrue();
    assertThat(actualRestResponse.allowOverride).isTrue();
    assertThat(actualRestResponse.inheritedFromOrganizationName).isEqualTo(org1.getName());
    assertThat(actualRestResponse.enabledInParent).isTrue();
    assertThat(actualRestResponse.inheritedFromOrganizationAllowOverride).isTrue();

    response = restRequest().parameter("organization", org1.getId()).get();
    assertResponseStatus(200, response);
    actualRestResponse = response.getBody(CpeMatchingConfigurationDTO.class);
    assertThat(actualRestResponse).isNotNull();
    assertThat(actualRestResponse.enabled).isTrue();
    assertThat(actualRestResponse.allowOverride).isTrue();
    assertThat(actualRestResponse.inheritedFromOrganizationName).isNull();
    assertThat(actualRestResponse.enabledInParent).isTrue();
    assertThat(actualRestResponse.inheritedFromOrganizationAllowOverride).isNull();

    response = restRequest().parameter("organization", root.getId()).get();
    assertResponseStatus(200, response);
    actualRestResponse = response.getBody(CpeMatchingConfigurationDTO.class);
    assertThat(actualRestResponse).isNotNull();
    assertThat(actualRestResponse.enabled).isTrue();
    assertThat(actualRestResponse.allowOverride).isFalse();
    assertThat(actualRestResponse.inheritedFromOrganizationName).isNull();
    assertThat(actualRestResponse.enabledInParent).isNull();
    assertThat(actualRestResponse.inheritedFromOrganizationAllowOverride).isNull();
  }

  @Test
  public void testGetCpeMatchingConfiguration_licenseFeatureNotEnabledError() throws Exception {
    getTestProductLicenseManager().getFeatures().remove(LicensedFeature.CPE_MATCHING);
    Application app1 = tempEntity.newApplicationWithParent();
    HttpResponse response = restRequest().parameter("application", app1.getId()).get();
    assertResponseStatus(402, response);
    assertThat(response.getBodyText()).isEqualTo("Your IQ Server license does not enable this feature.");
  }

  @Test
  public void testGetCpeMatchingConfiguration_notFoundError() throws Exception {
    HttpResponse response = restRequest().parameter("application", "fakeApp").get();
    assertResponseStatus(404, response);
    assertThat(response.getBodyText()).isEqualTo("Application with ID fakeApp does not exist.");
  }

  @Test
  public void testGetCpeMatchingConfiguration_emptyConfigReturned() throws Exception {
    Application app1 = tempEntity.newApplicationWithParent();
    HttpResponse response = restRequest().parameter("application", app1.getId()).get();
    assertResponseStatus(200, response);
    CpeMatchingConfigurationDTO actualRestResponse = response.getBody(CpeMatchingConfigurationDTO.class);
    assertThat(actualRestResponse).isNotNull();
    assertThat(actualRestResponse.enabled).isNull();
    assertThat(actualRestResponse.allowOverride).isNull();
    assertThat(actualRestResponse.inheritedFromOrganizationName).isNull();
    assertThat(actualRestResponse.enabledInParent).isNull();
    assertThat(actualRestResponse.inheritedFromOrganizationAllowOverride).isNull();
  }
}
