/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiAutoPolicyWaiverRevocationDTO;
import com.sonatype.insight.brain.dataaccess.policy.AutoPolicyWaiverRevocationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.policy.AutoPolicyWaiver;
import com.sonatype.insight.brain.model.policy.AutoPolicyWaiverRevocation;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.license.model.LicensedFeature;

import org.eclipse.jetty.http.HttpStatus;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static com.sonatype.insight.brain.api.v2.ApiAutoPolicyWaiverRevocationResource.OWNERS_PATH;
import static com.sonatype.insight.brain.api.v2.ApiAutoPolicyWaiverRevocationResource.BY_AUTO_POLICY_WAIVER_REVOCATION_ID_PATH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

public class ApiAutoPolicyWaiverRevocationResourceTest
    extends AbstractResourceTest
{
  private AutoPolicyWaiverRevocationDAO autoPolicyWaiverRevocationDAO;

  @Before
  public void setUp() {
    autoPolicyWaiverRevocationDAO = lookup(AutoPolicyWaiverRevocationDAO.class);
    when(mockDeveloperEnablementService.shouldEnableDeveloperProduct()).thenReturn(true);
    licenseManager.setFeatures(LicensedFeature.DEVELOPER_DASHBOARD);
    SystemConfigurationPropertyFeature.AUTO_WAIVERS.setEnabled(true);
  }

  @After
  public void cleanup() {
    licenseManager.reset();
  }

  @Test
  public void testDeleteAutoPolicyWaiverRevocation() throws Exception {
    Application application = tempEntity.newApplicationWithParent();
    AutoPolicyWaiver autoPolicyWaiver = tempEntity.newAutoPolicyWaiver(application.getId());
    AutoPolicyWaiverRevocation revocation =
        tempEntity.newAutoPolicyWaiverRevocation(application.getId(), autoPolicyWaiver.getId());

    HttpResponse response = restRequest()
        .path(PublicApiPaths.AUTO_POLICY_WAIVER_REVOCATION_PATH + "/" + BY_AUTO_POLICY_WAIVER_REVOCATION_ID_PATH)
        .parameter(OwnerType.APPLICATION, application.getId(), revocation.getId())
        .delete();

    assertResponseStatus(204, response);
    assertThat(autoPolicyWaiverRevocationDAO.getById(autoPolicyWaiver.getId())).isNull();
  }

  @Test
  public void testDeleteAutoPolicyWaiverRevocation_FeatureFlag() throws Exception {
    //when feature flag is disabled
    SystemConfigurationPropertyFeature.AUTO_WAIVERS.setEnabled(false);
    Application application = tempEntity.newApplicationWithParent();
    AutoPolicyWaiver autoPolicyWaiver = tempEntity.newAutoPolicyWaiver(application.getId());
    AutoPolicyWaiverRevocation revocation =
        tempEntity.newAutoPolicyWaiverRevocation(application.getId(), autoPolicyWaiver.getId());

    HttpResponse response = restRequest()
        .path(PublicApiPaths.AUTO_POLICY_WAIVER_REVOCATION_PATH + "/" + BY_AUTO_POLICY_WAIVER_REVOCATION_ID_PATH)
        .parameter(OwnerType.APPLICATION, application.getId(), revocation.getId())
        .delete();

    assertResponseStatus(400, response);

    //when feature flag is enabled
    SystemConfigurationPropertyFeature.AUTO_WAIVERS.setEnabled(true);

    response = restRequest()
        .path(PublicApiPaths.AUTO_POLICY_WAIVER_REVOCATION_PATH + "/" + BY_AUTO_POLICY_WAIVER_REVOCATION_ID_PATH)
        .parameter(OwnerType.APPLICATION, application.getId(), revocation.getId())
        .delete();

    assertResponseStatus(204, response);
    assertThat(autoPolicyWaiverRevocationDAO.getById(autoPolicyWaiver.getId())).isNull();
  }

  @Test
  public void testDeleteAutoPolicyWaiverRevocation_MissingDeveloperDashboardFeature() throws Exception {
    when(mockDeveloperEnablementService.shouldEnableDeveloperProduct()).thenReturn(false);
    setMissingFeature(LicensedFeature.DEVELOPER_DASHBOARD);

    Application application = tempEntity.newApplicationWithParent();
    AutoPolicyWaiver autoPolicyWaiver = tempEntity.newAutoPolicyWaiver(application.getId());
    AutoPolicyWaiverRevocation revocation =
        tempEntity.newAutoPolicyWaiverRevocation(application.getId(), autoPolicyWaiver.getId());

    HttpResponse response = restRequest()
        .path(PublicApiPaths.AUTO_POLICY_WAIVER_REVOCATION_PATH + "/" + BY_AUTO_POLICY_WAIVER_REVOCATION_ID_PATH)
        .parameter(OwnerType.APPLICATION, application.getId(), revocation.getId())
        .delete();

    assertResponseStatus(HttpStatus.PAYMENT_REQUIRED_402, response);
  }

  @Test
  public void testAddAutoPolicyWaiverRevocation_Application() throws Exception {
    Application application = tempEntity.newApplicationWithParent();
    AutoPolicyWaiver autoPolicyWaiver = tempEntity.newAutoPolicyWaiver(application.getId());

    ApiAutoPolicyWaiverRevocationDTO revocation = new ApiAutoPolicyWaiverRevocationDTO();
    revocation.ownerId = application.getId();
    revocation.autoPolicyWaiverId = autoPolicyWaiver.getId();
    revocation.hash = "hash";
    revocation.scanId = "scanId";

    HttpResponse response = restRequest()
        .path(PublicApiPaths.AUTO_POLICY_WAIVER_REVOCATION_PATH + "/" + OWNERS_PATH)
        .parameter(OwnerType.APPLICATION, application.getId())
        .body(revocation)
        .post();

    assertResponseStatus(200, response);
    AutoPolicyWaiverRevocation resultingRevocation =
        autoPolicyWaiverRevocationDAO.getByOwnerIdAndAutoPolicyWaiverIdAndHash(application.getId(),
            autoPolicyWaiver.getId(), "hash");
    assertThat(resultingRevocation).isNotNull();
  }

  @Test
  public void testAddAutoPolicyWaiverRevocation_Organization() throws Exception {
    Organization org = tempEntity.newOrganization();
    AutoPolicyWaiver autoPolicyWaiver = tempEntity.newAutoPolicyWaiver(org.getId());

    ApiAutoPolicyWaiverRevocationDTO revocation = new ApiAutoPolicyWaiverRevocationDTO();
    revocation.ownerId = org.getId();
    revocation.autoPolicyWaiverId = autoPolicyWaiver.getId();
    revocation.hash = "hash";
    revocation.scanId = "scanId";

    HttpResponse response = restRequest()
        .path(PublicApiPaths.AUTO_POLICY_WAIVER_REVOCATION_PATH + "/" + OWNERS_PATH)
        .parameter(OwnerType.ORGANIZATION, org.getId())
        .body(revocation)
        .post();

    assertResponseStatus(200, response);
    AutoPolicyWaiverRevocation resultingRevocation =
        autoPolicyWaiverRevocationDAO.getByOwnerIdAndAutoPolicyWaiverIdAndHash(org.getId(),
            autoPolicyWaiver.getId(), "hash");
    assertThat(resultingRevocation).isNotNull();
  }

  @Test
  public void testAddAutoPolicyWaiverRevocation_FeatureFlag() throws Exception {
    //when feature flag is disabled
    SystemConfigurationPropertyFeature.AUTO_WAIVERS.setEnabled(false);
    Application application = tempEntity.newApplicationWithParent();
    AutoPolicyWaiver autoPolicyWaiver = tempEntity.newAutoPolicyWaiver(application.getId());

    ApiAutoPolicyWaiverRevocationDTO revocation = new ApiAutoPolicyWaiverRevocationDTO();
    revocation.ownerId = application.getId();
    revocation.autoPolicyWaiverId = autoPolicyWaiver.getId();
    revocation.hash = "hash";
    revocation.scanId = "scanId";

    HttpResponse response = restRequest()
        .path(PublicApiPaths.AUTO_POLICY_WAIVER_REVOCATION_PATH + "/" + OWNERS_PATH)
        .parameter(OwnerType.APPLICATION, application.getId())
        .body(revocation)
        .post();

    assertResponseStatus(400, response);

    //when feature flag is enabled
    SystemConfigurationPropertyFeature.AUTO_WAIVERS.setEnabled(true);

    response = restRequest()
        .path(PublicApiPaths.AUTO_POLICY_WAIVER_REVOCATION_PATH + "/" + OWNERS_PATH)
        .parameter(OwnerType.APPLICATION, application.getId())
        .body(revocation)
        .post();

    assertResponseStatus(200, response);
    AutoPolicyWaiverRevocation resultingRevocation =
        autoPolicyWaiverRevocationDAO.getByOwnerIdAndAutoPolicyWaiverIdAndHash(application.getId(),
            autoPolicyWaiver.getId(), "hash");
    assertThat(resultingRevocation).isNotNull();
  }

  @Test
  public void testAddAutoPolicyWaiverRevocation_MissingDeveloperDashboardFeature() throws Exception {
    when(mockDeveloperEnablementService.shouldEnableDeveloperProduct()).thenReturn(false);
    setMissingFeature(LicensedFeature.DEVELOPER_DASHBOARD);

    Application application = tempEntity.newApplicationWithParent();
    AutoPolicyWaiver autoPolicyWaiver = tempEntity.newAutoPolicyWaiver(application.getId());
    ApiAutoPolicyWaiverRevocationDTO revocation = new ApiAutoPolicyWaiverRevocationDTO();
    revocation.ownerId = application.getId();
    revocation.autoPolicyWaiverId = autoPolicyWaiver.getId();
    revocation.hash = "hash";

    HttpResponse response = restRequest()
        .path(PublicApiPaths.AUTO_POLICY_WAIVER_REVOCATION_PATH + "/" + OWNERS_PATH)
        .parameter(OwnerType.APPLICATION, application.getId())
        .body(revocation)
        .post();

    assertResponseStatus(HttpStatus.PAYMENT_REQUIRED_402, response);
  }

  @Test
  public void testAddAutoPolicyWaiverRevocation_IncompleteDto() throws Exception {
    Application application = tempEntity.newApplicationWithParent();
    AutoPolicyWaiver autoPolicyWaiver = tempEntity.newAutoPolicyWaiver(application.getId());
    ApiAutoPolicyWaiverRevocationDTO revocation = new ApiAutoPolicyWaiverRevocationDTO();
    revocation.ownerId = application.getId();
    revocation.autoPolicyWaiverId = autoPolicyWaiver.getId();

    HttpResponse response = restRequest()
        .path(PublicApiPaths.AUTO_POLICY_WAIVER_REVOCATION_PATH + "/" + OWNERS_PATH)
        .parameter(OwnerType.APPLICATION, application.getId())
        .body(revocation)
        .post();

    assertResponseStatus(400, response);
  }
}
