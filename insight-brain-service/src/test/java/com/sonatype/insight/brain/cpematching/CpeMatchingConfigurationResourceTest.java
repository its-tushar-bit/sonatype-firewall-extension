/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.cpematching;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.license.model.LicensedFeature;

import org.junit.Before;
import org.junit.Test;

import static com.sonatype.insight.brain.api.PublicApiPaths.CPE_MATCHING_CONFIGURATION_RESOURCE_PATH;
import static org.assertj.core.api.Assertions.assertThat;

public class CpeMatchingConfigurationResourceTest
    extends AbstractResourceTest
{
  private OrganizationDAO organizationDAO;

  @Before
  public void setUp() {
    organizationDAO = lookup(OrganizationDAO.class);
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
  public void testUpdateCpeMatchingConfiguration_noRequestObjectError() throws Exception {
    Application app1 = tempEntity.newApplicationWithParent();
    HttpResponse response = restRequest().parameter("application", app1.getId()).put();
    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).isEqualTo("CPE matching configuration cannot be null");
  }

  @Test
  public void testUpdateCpeMatchingConfiguration_requestCpeMatchingObject_enabledIsNullError()
      throws Exception
  {
    Application app1 = tempEntity.newApplicationWithParent();
    HttpResponse response = restRequest().parameter("application", app1.getId())
        .body(new CpeMatchingConfigurationRequest())
        .put();
    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).isEqualTo("CPE matching configuration enabled cannot be null");
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
}
