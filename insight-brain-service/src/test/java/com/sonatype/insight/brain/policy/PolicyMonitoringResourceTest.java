/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyMonitoringDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.policy.PolicyMonitoring;
import com.sonatype.insight.brain.model.policy.stages.ReleaseStageType;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.policy.PolicyMonitoringResource.ApplicablePolicyMonitors;
import com.sonatype.insight.brain.policy.PolicyMonitoringResource.PolicyMonitoringByOwner;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.license.model.ProductLicenseDetails;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class PolicyMonitoringResourceTest
    extends AbstractResourceTest
{
  private PolicyMonitoringDAO policyMonitoringDAO;

  private OwnerDAO ownerDAO;

  @Before
  public void setUp() {
    ownerDAO = lookup(OwnerDAO.class);
    policyMonitoringDAO = lookup(PolicyMonitoringDAO.class);
  }

  private HttpRequest restRequest(OwnerType ownerType, String ownerId) {
    return restRequest().path(PolicyMonitoringResource.RESOURCE_PATH).parameter(ownerType, ownerId);
  }

  @Test
  public void testCRUD_Application() throws Exception {
    String appPublicId = "PolicyMonitoringResourceTest_AppId";
    Application application = tempEntity.newApplicationWithParent(appPublicId);

    setLicenseProducts(ProductLicenseDetails.PRODUCT_LIFECYCLE_SAAS);
    testCRUD(OwnerType.APPLICATION, appPublicId, application.getId());
    setLicenseProducts(ProductLicenseDetails.PRODUCT_SBOM_MANAGER_SAAS);
    testCRUD_ComplianceStage(OwnerType.APPLICATION, appPublicId, application.getId());
    setLicenseProducts(ProductLicenseDetails.PRODUCT_SBOM_MANAGER);
    testCRUD_ComplianceStage(OwnerType.APPLICATION, appPublicId, application.getId());
    setLicenseProducts(ProductLicenseDetails.PRODUCT_LIFECYCLE_SAAS, ProductLicenseDetails.PRODUCT_SBOM_MANAGER_SAAS);
    testCRUD_MultiLicense(OwnerType.APPLICATION, appPublicId, application.getId());
  }

  @Test
  public void testCRUD_Organization() throws Exception {
    Organization organization = tempEntity.newOrganization("PolicyMonitoringResourceTest");

    setLicenseProducts(ProductLicenseDetails.PRODUCT_LIFECYCLE_SAAS);
    testCRUD(OwnerType.ORGANIZATION, organization.getId(), organization.getId());
    setLicenseProducts(ProductLicenseDetails.PRODUCT_SBOM_MANAGER_SAAS);
    testCRUD_ComplianceStage(OwnerType.ORGANIZATION, organization.getId(), organization.getId());
    setLicenseProducts(ProductLicenseDetails.PRODUCT_SBOM_MANAGER);
    testCRUD_ComplianceStage(OwnerType.ORGANIZATION, organization.getId(), organization.getId());
    setLicenseProducts(ProductLicenseDetails.PRODUCT_LIFECYCLE_SAAS, ProductLicenseDetails.PRODUCT_SBOM_MANAGER_SAAS);
    testCRUD_MultiLicense(OwnerType.ORGANIZATION, organization.getId(), organization.getId());
  }

  private void testCRUD(OwnerType ownerType, String ownerPublicId, String ownerId) throws Exception {
    // Create
    PolicyMonitoring policyMonitoring = new PolicyMonitoring(null /* ownerId */, Stage.ID_RELEASE);
    HttpResponse response = restRequest(ownerType, ownerPublicId).body(policyMonitoring).put();
    assertResponseStatus(200, response);
    policyMonitoring = response.getBody(PolicyMonitoring.class);
    assertPolicyMonitoring(ownerId, Stage.ID_RELEASE, policyMonitoring);

    // Get
    response = restRequest(ownerType, ownerPublicId).get();
    assertResponseStatus(200, response);
    PolicyMonitoring[] policyMonitorings = response.getBody(PolicyMonitoring[].class);
    assertThat(policyMonitorings).hasSize(1);
    assertPolicyMonitoring(ownerId, Stage.ID_RELEASE, policyMonitorings[0]);

    // Update
    policyMonitoring = new PolicyMonitoring(null /* ownerId */, Stage.ID_BUILD);
    response = restRequest(ownerType, ownerPublicId).body(policyMonitoring).put();
    assertResponseStatus(200, response);
    policyMonitoring = response.getBody(PolicyMonitoring.class);
    assertPolicyMonitoring(ownerId, Stage.ID_BUILD, policyMonitoring);

    // Get
    response = restRequest(ownerType, ownerPublicId).get();
    assertResponseStatus(200, response);
    policyMonitorings = response.getBody(PolicyMonitoring[].class);
    assertThat(policyMonitorings).hasSize(1);
    assertPolicyMonitoring(ownerId, Stage.ID_BUILD, policyMonitorings[0]);

    // Delete
    response =
        restRequest(ownerType, ownerPublicId).query("stageTypeId", policyMonitorings[0].getStageTypeId()).delete();
    assertResponseStatus(204, response);

    // Get
    response = restRequest(ownerType, ownerPublicId).get();
    assertResponseStatus(200, response);
    policyMonitorings = response.getBody(PolicyMonitoring[].class);
    assertThat(policyMonitorings).isEmpty();
  }

  private void testCRUD_ComplianceStage(OwnerType ownerType, String ownerPublicId, String ownerId) throws Exception {
    // Create
    PolicyMonitoring policyMonitoring = new PolicyMonitoring(null /* ownerId */, Stage.ID_COMPLIANCE);
    HttpResponse response = restRequest(ownerType, ownerPublicId).body(policyMonitoring).put();
    assertResponseStatus(200, response);
    policyMonitoring = response.getBody(PolicyMonitoring.class);
    assertPolicyMonitoring(ownerId, Stage.ID_COMPLIANCE, policyMonitoring);

    // Get
    response = restRequest(ownerType, ownerPublicId).get();
    assertResponseStatus(200, response);
    PolicyMonitoring[] policyMonitorings = response.getBody(PolicyMonitoring[].class);
    assertThat(policyMonitorings).hasSize(1);
    assertPolicyMonitoring(ownerId, Stage.ID_COMPLIANCE, policyMonitorings[0]);

    // Update
    policyMonitoring = new PolicyMonitoring(null /* ownerId */, Stage.ID_COMPLIANCE);
    response = restRequest(ownerType, ownerPublicId).body(policyMonitoring).put();
    assertResponseStatus(200, response);
    policyMonitoring = response.getBody(PolicyMonitoring.class);
    assertPolicyMonitoring(ownerId, Stage.ID_COMPLIANCE, policyMonitoring);

    // Get
    response = restRequest(ownerType, ownerPublicId).get();
    assertResponseStatus(200, response);
    policyMonitorings = response.getBody(PolicyMonitoring[].class);
    assertThat(policyMonitorings).hasSize(1);
    assertPolicyMonitoring(ownerId, Stage.ID_COMPLIANCE, policyMonitorings[0]);

    // Delete
    response =
        restRequest(ownerType, ownerPublicId).query("stageTypeId", policyMonitorings[0].getStageTypeId()).delete();
    assertResponseStatus(204, response);

    // Get
    response = restRequest(ownerType, ownerPublicId).get();
    assertResponseStatus(200, response);
    policyMonitorings = response.getBody(PolicyMonitoring[].class);
    assertThat(policyMonitorings).isEmpty();
  }

  private void testCRUD_MultiLicense(OwnerType ownerType, String ownerPublicId, String ownerId) throws Exception {
    // Create Lifecycle (LC) Continuous Monitoring (CM)
    PolicyMonitoring policyMonitoring = new PolicyMonitoring(null /* ownerId */, Stage.ID_RELEASE);
    HttpResponse response = restRequest(ownerType, ownerPublicId).body(policyMonitoring).put();
    assertResponseStatus(200, response);
    policyMonitoring = response.getBody(PolicyMonitoring.class);
    assertPolicyMonitoring(ownerId, Stage.ID_RELEASE, policyMonitoring);

    // Create SBOM Manager (SM) Continuous Monitoring (CM)
    policyMonitoring = new PolicyMonitoring(null /* ownerId */, Stage.ID_COMPLIANCE);
    response = restRequest(ownerType, ownerPublicId).body(policyMonitoring).put();
    assertResponseStatus(200, response);
    policyMonitoring = response.getBody(PolicyMonitoring.class);
    assertPolicyMonitoring(ownerId, Stage.ID_COMPLIANCE, policyMonitoring);

    // Get both
    response = restRequest(ownerType, ownerPublicId).get();
    assertResponseStatus(200, response);
    PolicyMonitoring[] policyMonitorings = response.getBody(PolicyMonitoring[].class);
    assertThat(policyMonitorings).hasSize(2);
    assertPolicyMonitoring(ownerId, Stage.ID_COMPLIANCE, policyMonitorings[0]);
    assertPolicyMonitoring(ownerId, Stage.ID_RELEASE, policyMonitorings[1]);

    // Update both
    policyMonitoring = new PolicyMonitoring(null /* ownerId */, Stage.ID_BUILD);
    response = restRequest(ownerType, ownerPublicId).body(policyMonitoring).put();
    assertResponseStatus(200, response);
    policyMonitoring = response.getBody(PolicyMonitoring.class);
    assertPolicyMonitoring(ownerId, Stage.ID_BUILD, policyMonitoring);
    policyMonitoring = new PolicyMonitoring(null /* ownerId */, Stage.ID_COMPLIANCE);
    response = restRequest(ownerType, ownerPublicId).body(policyMonitoring).put();
    assertResponseStatus(200, response);
    policyMonitoring = response.getBody(PolicyMonitoring.class);
    assertPolicyMonitoring(ownerId, Stage.ID_COMPLIANCE, policyMonitoring);

    // Get both
    response = restRequest(ownerType, ownerPublicId).get();
    assertResponseStatus(200, response);
    policyMonitorings = response.getBody(PolicyMonitoring[].class);
    assertThat(policyMonitorings).hasSize(2);
    assertPolicyMonitoring(ownerId, Stage.ID_BUILD, policyMonitorings[0]);
    assertPolicyMonitoring(ownerId, Stage.ID_COMPLIANCE, policyMonitorings[1]);

    // Delete both
    response =
        restRequest(ownerType, ownerPublicId).query("stageTypeId", policyMonitorings[0].getStageTypeId()).delete();
    assertResponseStatus(204, response);
    response =
        restRequest(ownerType, ownerPublicId).query("stageTypeId", policyMonitorings[1].getStageTypeId()).delete();
    assertResponseStatus(204, response);

    // Get
    response = restRequest(ownerType, ownerPublicId).get();
    assertResponseStatus(200, response);
    policyMonitorings = response.getBody(PolicyMonitoring[].class);
    assertThat(policyMonitorings).isEmpty();
  }

  @Test
  public void testDelete_NotSet_Organization() throws Exception {
    Organization organization = tempEntity.newOrganization("PolicyMonitoringResourceTest");
    PolicyMonitoring policyMonitoring = new PolicyMonitoring(organization.getId(), ReleaseStageType.ID);
    HttpResponse response = restRequest(OwnerType.ORGANIZATION, organization.getId()).body(policyMonitoring).delete();
    assertResponseStatus(404, response);
    assertThat(response.getBodyText())
        .isEqualTo("Policy monitoring was not set for owner ID " + organization.getId() + ".");
  }

  @Test
  public void testDelete_NotSet_Application() throws Exception {
    String appPublicId = "PolicyMonitoringResourceTest_AppId";
    Application application = tempEntity.newApplicationWithParent(appPublicId);
    PolicyMonitoring policyMonitoring = new PolicyMonitoring(application.getId(), ReleaseStageType.ID);
    HttpResponse response = restRequest(OwnerType.APPLICATION, appPublicId).body(policyMonitoring).delete();
    assertResponseStatus(404, response);
    assertThat(response.getBodyText())
        .isEqualTo("Policy monitoring was not set for owner ID " + application.getId() + ".");
  }

  @Test
  public void testGetApplicable() throws Exception {
    Organization organization = tempEntity.newOrganization("testGetApplicablePolicyMonitoringOrgId");
    Owner organizationParent = ownerDAO.getParentOwner(organization);
    Application application = tempEntity.newApplication("testGetApplicablePolicyMonitoringAppId",
        "testGetApplicablePolicyMonitoringAppId", organization.getId());

    // no Policy Monitoring set
    HttpResponse response = restRequest(OwnerType.APPLICATION, application.getPublicId()).path("applicable").get();
    assertResponseStatus(200, response);
    ApplicablePolicyMonitors applicablePolicyMonitors = response.getBody(ApplicablePolicyMonitors.class);

    assertThat(applicablePolicyMonitors.policyMonitoringByOwner).hasSize(3);
    assertEmptyPolicyMonitoringByOwner(application, applicablePolicyMonitors.policyMonitoringByOwner.get(0));
    assertEmptyPolicyMonitoringByOwner(organization, applicablePolicyMonitors.policyMonitoringByOwner.get(1));
    assertEmptyPolicyMonitoringByOwner(organizationParent, applicablePolicyMonitors.policyMonitoringByOwner.get(2));

    response = restRequest(OwnerType.ORGANIZATION, organization.getId()).path("applicable").get();
    assertResponseStatus(200, response);
    applicablePolicyMonitors = response.getBody(ApplicablePolicyMonitors.class);

    assertThat(applicablePolicyMonitors.policyMonitoringByOwner).hasSize(2);
    assertEmptyPolicyMonitoringByOwner(organization, applicablePolicyMonitors.policyMonitoringByOwner.get(0));
    assertEmptyPolicyMonitoringByOwner(organizationParent, applicablePolicyMonitors.policyMonitoringByOwner.get(1));

    response = restRequest(OwnerType.ORGANIZATION, organizationParent.getId()).path("applicable").get();
    assertResponseStatus(200, response);
    applicablePolicyMonitors = response.getBody(ApplicablePolicyMonitors.class);

    assertThat(applicablePolicyMonitors.policyMonitoringByOwner).hasSize(1);
    assertEmptyPolicyMonitoringByOwner(organizationParent, applicablePolicyMonitors.policyMonitoringByOwner.get(0));

    // Root Org only Policy Monitoring set
    tempEntity.newPolicyMonitoring(organizationParent.getId(), Stage.ID_RELEASE);
    response = restRequest(OwnerType.ORGANIZATION, organizationParent.getId()).path("applicable").get();
    assertResponseStatus(200, response);
    applicablePolicyMonitors = response.getBody(ApplicablePolicyMonitors.class);

    assertThat(applicablePolicyMonitors.policyMonitoringByOwner).hasSize(1);
    assertPolicyMonitoringByOwner(organizationParent, Stage.ID_RELEASE,
        applicablePolicyMonitors.policyMonitoringByOwner.get(0));

    response = restRequest(OwnerType.ORGANIZATION, organization.getId()).path("applicable").get();
    assertResponseStatus(200, response);
    applicablePolicyMonitors = response.getBody(ApplicablePolicyMonitors.class);

    assertThat(applicablePolicyMonitors.policyMonitoringByOwner).hasSize(2);
    assertEmptyPolicyMonitoringByOwner(organization, applicablePolicyMonitors.policyMonitoringByOwner.get(0));
    assertPolicyMonitoringByOwner(organizationParent, Stage.ID_RELEASE,
        applicablePolicyMonitors.policyMonitoringByOwner.get(1));

    response = restRequest(OwnerType.APPLICATION, application.getPublicId()).path("applicable").get();
    assertResponseStatus(200, response);
    applicablePolicyMonitors = response.getBody(ApplicablePolicyMonitors.class);

    assertThat(applicablePolicyMonitors.policyMonitoringByOwner).hasSize(3);
    assertEmptyPolicyMonitoringByOwner(application, applicablePolicyMonitors.policyMonitoringByOwner.get(0));
    assertEmptyPolicyMonitoringByOwner(organization, applicablePolicyMonitors.policyMonitoringByOwner.get(1));
    assertPolicyMonitoringByOwner(organizationParent, Stage.ID_RELEASE,
        applicablePolicyMonitors.policyMonitoringByOwner.get(2));

    // Root and Org Policy Monitoring set
    tempEntity.newPolicyMonitoring(organization.getId(), Stage.ID_STAGE_RELEASE);
    response = restRequest(OwnerType.ORGANIZATION, organizationParent.getId()).path("applicable").get();
    assertResponseStatus(200, response);
    applicablePolicyMonitors = response.getBody(ApplicablePolicyMonitors.class);

    assertThat(applicablePolicyMonitors.policyMonitoringByOwner).hasSize(1);
    assertPolicyMonitoringByOwner(organizationParent, Stage.ID_RELEASE,
        applicablePolicyMonitors.policyMonitoringByOwner.get(0));

    response = restRequest(OwnerType.ORGANIZATION, organization.getId()).path("applicable").get();
    assertResponseStatus(200, response);
    applicablePolicyMonitors = response.getBody(ApplicablePolicyMonitors.class);

    assertThat(applicablePolicyMonitors.policyMonitoringByOwner).hasSize(2);
    assertPolicyMonitoringByOwner(organization, Stage.ID_STAGE_RELEASE,
        applicablePolicyMonitors.policyMonitoringByOwner.get(0));
    assertPolicyMonitoringByOwner(organizationParent, Stage.ID_RELEASE,
        applicablePolicyMonitors.policyMonitoringByOwner.get(1));

    response = restRequest(OwnerType.APPLICATION, application.getPublicId()).path("applicable").get();
    assertResponseStatus(200, response);
    applicablePolicyMonitors = response.getBody(ApplicablePolicyMonitors.class);

    assertThat(applicablePolicyMonitors.policyMonitoringByOwner).hasSize(3);
    assertEmptyPolicyMonitoringByOwner(application, applicablePolicyMonitors.policyMonitoringByOwner.get(0));
    assertPolicyMonitoringByOwner(organization, Stage.ID_STAGE_RELEASE,
        applicablePolicyMonitors.policyMonitoringByOwner.get(1));
    assertPolicyMonitoringByOwner(organizationParent, Stage.ID_RELEASE,
        applicablePolicyMonitors.policyMonitoringByOwner.get(2));

    // App, Org and Root Org all have Policy Monitoring set
    tempEntity.newPolicyMonitoring(application.getId(), Stage.ID_BUILD);
    response = restRequest(OwnerType.ORGANIZATION, organizationParent.getId()).path("applicable").get();
    assertResponseStatus(200, response);
    applicablePolicyMonitors = response.getBody(ApplicablePolicyMonitors.class);

    assertThat(applicablePolicyMonitors.policyMonitoringByOwner).hasSize(1);
    assertPolicyMonitoringByOwner(organizationParent, Stage.ID_RELEASE,
        applicablePolicyMonitors.policyMonitoringByOwner.get(0));

    response = restRequest(OwnerType.ORGANIZATION, organization.getId()).path("applicable").get();
    assertResponseStatus(200, response);
    applicablePolicyMonitors = response.getBody(ApplicablePolicyMonitors.class);

    assertThat(applicablePolicyMonitors.policyMonitoringByOwner).hasSize(2);
    assertPolicyMonitoringByOwner(organization, Stage.ID_STAGE_RELEASE,
        applicablePolicyMonitors.policyMonitoringByOwner.get(0));
    assertPolicyMonitoringByOwner(organizationParent, Stage.ID_RELEASE,
        applicablePolicyMonitors.policyMonitoringByOwner.get(1));

    response = restRequest(OwnerType.APPLICATION, application.getPublicId()).path("applicable").get();
    assertResponseStatus(200, response);
    applicablePolicyMonitors = response.getBody(ApplicablePolicyMonitors.class);

    assertThat(applicablePolicyMonitors.policyMonitoringByOwner).hasSize(3);
    assertPolicyMonitoringByOwner(application, Stage.ID_BUILD, applicablePolicyMonitors.policyMonitoringByOwner.get(0));
    assertPolicyMonitoringByOwner(organization, Stage.ID_STAGE_RELEASE,
        applicablePolicyMonitors.policyMonitoringByOwner.get(1));
    assertPolicyMonitoringByOwner(organizationParent, Stage.ID_RELEASE,
        applicablePolicyMonitors.policyMonitoringByOwner.get(2));
  }

  @Test
  public void testSet_SbomManagerLicenseOnly() throws Exception {
    setLicenseProducts(ProductLicenseDetails.PRODUCT_SBOM_MANAGER_SAAS);
    Organization organization = tempEntity.newOrganization("PolicyMonitoringResourceTest_OrgName");

    PolicyMonitoring policyMonitoring = new PolicyMonitoring(null /* ownerId */, Stage.ID_COMPLIANCE);
    HttpResponse response = restRequest(OwnerType.ORGANIZATION, organization.getId()).body(policyMonitoring).put();
    assertResponseStatus(200, response);
  }

  @Test
  public void testSet_MultiLicense() throws Exception {
    setLicenseProducts(ProductLicenseDetails.PRODUCT_LIFECYCLE_SAAS, ProductLicenseDetails.PRODUCT_SBOM_MANAGER_SAAS);
    Organization organization = tempEntity.newOrganization("PolicyMonitoringResourceTest_OrgName");

    PolicyMonitoring policyMonitoring = new PolicyMonitoring(null /* ownerId */, Stage.ID_COMPLIANCE);
    HttpResponse response = restRequest(OwnerType.ORGANIZATION, organization.getId()).body(policyMonitoring).put();
    assertResponseStatus(200, response);

    policyMonitoring = new PolicyMonitoring(null /* ownerId */, Stage.ID_RELEASE);
    response = restRequest(OwnerType.ORGANIZATION, organization.getId()).body(policyMonitoring).put();
    assertResponseStatus(200, response);
  }

  @Test
  public void testSet_VariousStagesWithInvalidLicenses() throws Exception {
    // Try setting a compliance stage record with a Lifecycle only license
    String orgName = "PolicyMonitoringResourceTest_OrgName";
    Organization organization = tempEntity.newOrganization(orgName);

    PolicyMonitoring policyMonitoring = new PolicyMonitoring(null /* ownerId */, Stage.ID_COMPLIANCE);
    HttpResponse response = restRequest(OwnerType.ORGANIZATION, organization.getId()).body(policyMonitoring).put();

    assertResponseStatus(402, response);
    assertThat(response.getBodyText()).isEqualTo("policy monitoring for stage 'compliance' " +
        "is not supported with your license.");
    assertThat(policyMonitoringDAO.getAll()).isEmpty();

    // Try setting a non-compliance stage record with a SBOM Manager only license
    setLicenseProducts(ProductLicenseDetails.PRODUCT_SBOM_MANAGER);
    policyMonitoring = new PolicyMonitoring(null /* ownerId */, Stage.ID_RELEASE);
    response = restRequest(OwnerType.ORGANIZATION, organization.getId()).body(policyMonitoring).put();

    assertResponseStatus(402, response);
    assertThat(response.getBodyText()).isEqualTo("policy monitoring for stage 'release' " +
        "is not supported with your license.");
    assertThat(policyMonitoringDAO.getAll()).isEmpty();
  }

  @Test
  public void testSet_InvalidApplicationStageTypeId() throws Exception {
    String appPublicId = "PolicyMonitoringResourceTest_AppId";
    Application application = tempEntity.newApplicationWithParent(appPublicId);

    PolicyMonitoring policyMonitoring = new PolicyMonitoring(null /* ownerId */, Stage.ID_PROXY);
    HttpResponse response = restRequest(application.getType(), appPublicId).body(policyMonitoring).put();

    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).isEqualTo("Invalid stage id=proxy");
    assertThat(policyMonitoringDAO.getAll()).isEmpty();
  }

  @Test
  public void testSet_InvalidRepositoryStageTypeId() throws Exception {
    String repoPublicId = "PolicyMonitoringResourceTest_RepoId";
    Repository repository = tempEntity.newRepository(repoPublicId);

    PolicyMonitoring policyMonitoring = new PolicyMonitoring(null /* ownerId */, Stage.ID_RELEASE);
    HttpResponse response = restRequest(repository.getType(), repository.getId()).body(policyMonitoring).put();

    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).isEqualTo("Invalid stage id=release");
    assertThat(policyMonitoringDAO.getAll()).isEmpty();
  }

  @Test
  public void testSet_InvalidOrganizationStageTypeId() throws Exception {
    String orgName = "PolicyMonitoringResourceTest_OrgName";
    Organization organization = tempEntity.newOrganization(orgName);

    PolicyMonitoring policyMonitoring = new PolicyMonitoring(null /* ownerId */, Stage.ID_PROXY);
    HttpResponse response = restRequest(OwnerType.ORGANIZATION, organization.getId()).body(policyMonitoring).put();

    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).isEqualTo("Invalid stage id=proxy");
    assertThat(policyMonitoringDAO.getAll()).isEmpty();
  }

  @Test
  public void testSet_RootOrgProxyStageTypeId() throws Exception {
    PolicyMonitoring policyMonitoring = new PolicyMonitoring(null /* ownerId */, Stage.ID_PROXY);
    HttpResponse response =
        restRequest(OwnerType.ORGANIZATION, Organization.ROOT_ORGANIZATION_ID).body(policyMonitoring).put();

    assertResponseStatus(200, response);
    assertThat(policyMonitoringDAO.getAll()).hasSize(1);
  }

  @Test
  public void testSet_RootOrgNonProxyStageTypeId() throws Exception {
    PolicyMonitoring policyMonitoring = new PolicyMonitoring(null /* ownerId */, Stage.ID_RELEASE);
    HttpResponse response =
        restRequest(OwnerType.ORGANIZATION, Organization.ROOT_ORGANIZATION_ID).body(policyMonitoring).put();

    assertResponseStatus(200, response);
    assertThat(policyMonitoringDAO.getAll()).hasSize(1);
  }

  @Test
  public void testSet_RepositoryProxyStageTypeId() throws Exception {
    String repoPublicId = "PolicyMonitoringResourceTest_RepoId";
    Repository repository = tempEntity.newRepository(repoPublicId);

    PolicyMonitoring policyMonitoring = new PolicyMonitoring(null /* ownerId */, Stage.ID_PROXY);
    HttpResponse response =
        restRequest(OwnerType.REPOSITORY, repository.getId()).body(policyMonitoring).put();

    assertResponseStatus(200, response);
    assertThat(policyMonitoringDAO.getAll()).hasSize(1);
  }

  private void assertPolicyMonitoring(String ownerId, String stageTypeId, PolicyMonitoring actual) {
    assertThat(actual.getOwnerId()).isEqualTo(ownerId);
    assertThat(actual.getStageTypeId()).isEqualTo(stageTypeId);
  }

  private void assertEmptyPolicyMonitoringByOwner(Owner owner, PolicyMonitoringByOwner policyMonitoringByOwner) {
    assertThat(policyMonitoringByOwner.ownerName).isEqualTo(owner.getName());
    assertThat(policyMonitoringByOwner.policyMonitorings).isEmpty();
  }

  private void assertPolicyMonitoringByOwner(
      Owner owner,
      String stageTypeId,
      PolicyMonitoringByOwner policyMonitoringByOwner)
  {
    assertThat(policyMonitoringByOwner.ownerName).isEqualTo(owner.getName());
    assertThat(policyMonitoringByOwner.policyMonitorings).isNotNull();
    for (PolicyMonitoring policyMonitoring : policyMonitoringByOwner.policyMonitorings) {
      assertPolicyMonitoring(owner.getId(), stageTypeId, policyMonitoring);
    }
  }
}
