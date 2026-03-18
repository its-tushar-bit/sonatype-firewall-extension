/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.integration;

import com.sonatype.clm.dto.model.application.ApplicationSummary;
import com.sonatype.clm.dto.model.application.ApplicationSummaryList;
import com.sonatype.clm.dto.model.repository.RepositoryType;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.v2.dto.ApiVerifyOrCreateApplicationForContainerImageFirewallDTO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryManagerDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.license.model.LicensedFeature;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ApplicationSummaryResourceTest
    extends AbstractResourceTest
{
  private OrganizationDAO organizationDAO;

  private RepositoryManagerDAO repositoryManagerDAO;

  private RepositoryDAO repositoryDAO;

  @Before
  public void setUp() {
    organizationDAO = lookup(OrganizationDAO.class);
    repositoryManagerDAO = lookup(RepositoryManagerDAO.class);
    repositoryDAO = lookup(RepositoryDAO.class);
  }

  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(ApplicationSummaryResource.RESOURCE_PATH);
  }

  private HttpRequest summaryRequest(Goal goal) {
    return restRequest().query("goal", goal);
  }

  private HttpRequest underOrgRequest(String organizationId) {
    return restRequest().query("organizationId", organizationId);
  }

  private HttpRequest summaryUnderOrgRequest(Goal goal, String organizationId) {
    return underOrgRequest(organizationId).query("goal", goal);
  }

  @Test
  public void testGetApplications_EvaluateApplication() throws Exception {
    Application application = tempEntity.newApplicationWithParent();

    HttpResponse response = summaryRequest(Goal.EVALUATE_APPLICATION).get();
    assertResponseStatus(200, response);

    ApplicationSummaryList applicationListDTO = response.getBody(ApplicationSummaryList.class);
    assertApplicationSummaryList(applicationListDTO, application);
  }

  @Test
  public void testGetApplications_EvaluateComponent() throws Exception {
    Application application = tempEntity.newApplicationWithParent();

    HttpResponse response = summaryRequest(Goal.EVALUATE_COMPONENT).get();
    assertResponseStatus(200, response);

    ApplicationSummaryList applicationListDTO = response.getBody(ApplicationSummaryList.class);
    assertApplicationSummaryList(applicationListDTO, application);
  }

  @Test
  public void testGetApplications_NoGoalSpecified() throws Exception {
    Application application = tempEntity.newApplicationWithParent();

    HttpResponse response = restRequest().get();
    assertResponseStatus(200, response);

    ApplicationSummaryList applicationListDTO = response.getBody(ApplicationSummaryList.class);
    assertApplicationSummaryList(applicationListDTO, application);
  }

  @Test
  public void testGetApplicationsByOrganization_EvaluateApplication() throws Exception {
    Application application = tempEntity.newApplicationWithParent();

    HttpResponse response = summaryUnderOrgRequest(Goal.EVALUATE_APPLICATION, application.getOrganizationId()).get();
    assertResponseStatus(200, response);

    ApplicationSummaryList applicationListDTO = response.getBody(ApplicationSummaryList.class);
    assertApplicationSummaryList(applicationListDTO, application);
  }

  @Test
  public void testGetApplicationsByOrganization_EvaluateComponent() throws Exception {
    Application application = tempEntity.newApplicationWithParent();

    HttpResponse response = summaryUnderOrgRequest(Goal.EVALUATE_COMPONENT, application.getOrganizationId()).get();
    assertResponseStatus(200, response);

    ApplicationSummaryList applicationListDTO = response.getBody(ApplicationSummaryList.class);
    assertApplicationSummaryList(applicationListDTO, application);
  }

  @Test
  public void testGetApplicationsByOrganization_NoGoalSpecified() throws Exception {
    Application application = tempEntity.newApplicationWithParent();

    HttpResponse response = underOrgRequest(application.getOrganizationId()).get();
    assertResponseStatus(200, response);

    ApplicationSummaryList applicationListDTO = response.getBody(ApplicationSummaryList.class);
    assertApplicationSummaryList(applicationListDTO, application);
  }

  private void assertApplicationSummaryList(ApplicationSummaryList actual, Application expected) {
    assertThat(actual).isNotNull();
    assertThat(actual.getApplicationSummaries()).hasSize(1);
    ApplicationSummary applicationSummary = actual.getApplicationSummaries().get(0);
    assertThat(applicationSummary.getId()).isEqualTo(expected.getId());
    assertThat(applicationSummary.getPublicId()).isEqualTo(expected.getPublicId());
    assertThat(applicationSummary.getName()).isEqualTo(expected.getName());
  }

  @Test
  public void testVerifyOrCreateApplication_EvaluateApplication() throws Exception {
    Application app = tempEntity.newApplicationWithParent();

    HttpResponse response = restRequest()
        .path(ApplicationSummaryResource.VERIFY_OR_CREATE_APPLICATION_PATH)
        .parameter(app.getPublicId())
        .query("goal", Goal.EVALUATE_APPLICATION)
        .post();
    assertResponseStatus(200, response);

    assertThat(response.getBody(String.class)).isEqualTo("true");
  }

  @Test
  public void testVerifyOrCreateApplicationForContainerImageFirewall_success() throws Exception {
    SystemConfigurationPropertyFeature.CONTAINER_IMAGES_EVAL_ENABLED.setEnabled(true);

    RepositoryManager repositoryManager = tempEntity.newRepositoryManager("instance1");
    repositoryManager.setBaseUrl("baseUrl1");
    repositoryManagerDAO.update(repositoryManager);

    Repository repository =
        tempEntity.newRepository(repositoryManager, "proxy-docker-repository", RepositoryType.proxy, "docker");

    ApiVerifyOrCreateApplicationForContainerImageFirewallDTO dto =
        new ApiVerifyOrCreateApplicationForContainerImageFirewallDTO();

    dto.setRepositoryManagerInstanceId(repositoryManager.getInstanceId());
    dto.setRepositoryPublicId(repository.getPublicId());
    dto.setContainerImageName("image1");
    dto.setContainerImageNamespace("namespace1");
    dto.setContainerImageVersion("version1");
    dto.setBaseUrl("baseUrl1");

    HttpResponse response = restRequest()
        .path(ApplicationSummaryResource.VERIFY_OR_CREATE_APP_FOR_CONTAINER_IMAGE_PATH)
        .body(dto)
        .post();
    assertResponseStatus(200, response);
    assertThat(response.getBodyText()).isNotNull();
  }

  @Test
  public void testVerifyOrCreateApplicationForContainerImageFirewall_defaultsQuarantineToTrueWhenMissing() throws Exception {
    SystemConfigurationPropertyFeature.CONTAINER_IMAGES_EVAL_ENABLED.setEnabled(true);

    RepositoryManager repositoryManager = tempEntity.newRepositoryManager("instance1");
    repositoryManager.setBaseUrl("baseUrl1");
    repositoryManagerDAO.update(repositoryManager);

    Repository repository =
        tempEntity.newRepository(repositoryManager, "proxy-docker-repository", RepositoryType.proxy, "docker");
    repository.setAuditEnabled(false);
    repository.setQuarantineEnabled(false);
    repositoryDAO.update(repository);

    ApiVerifyOrCreateApplicationForContainerImageFirewallDTO dto =
        new ApiVerifyOrCreateApplicationForContainerImageFirewallDTO();
    dto.setRepositoryManagerInstanceId(repositoryManager.getInstanceId());
    dto.setRepositoryPublicId(repository.getPublicId());
    dto.setContainerImageName("image1");
    dto.setContainerImageNamespace("namespace1");
    dto.setContainerImageVersion("version1");
    dto.setBaseUrl("baseUrl1");
    // quarantineEnabled intentionally omitted to validate default behavior.

    HttpResponse response = restRequest()
        .path(ApplicationSummaryResource.VERIFY_OR_CREATE_APP_FOR_CONTAINER_IMAGE_PATH)
        .body(dto)
        .post();
    assertResponseStatus(200, response);

    Repository updatedRepository = repositoryDAO.getByRepositoryManagerInstanceIdAndPublicIdNotNull(
        repositoryManager.getInstanceId(), repository.getPublicId());
    assertThat(updatedRepository.isAuditEnabled()).isTrue();
    assertThat(updatedRepository.isQuarantineEnabled()).isTrue();
  }

  @Test
  public void testVerifyOrCreateApplicationForContainerImageFirewall_honorsExplicitFalseQuarantine() throws Exception {
    SystemConfigurationPropertyFeature.CONTAINER_IMAGES_EVAL_ENABLED.setEnabled(true);

    RepositoryManager repositoryManager = tempEntity.newRepositoryManager("instance1");
    repositoryManager.setBaseUrl("baseUrl1");
    repositoryManagerDAO.update(repositoryManager);

    Repository repository =
        tempEntity.newRepository(repositoryManager, "proxy-docker-repository", RepositoryType.proxy, "docker");
    repository.setAuditEnabled(false);
    repository.setQuarantineEnabled(false);
    repositoryDAO.update(repository);

    ApiVerifyOrCreateApplicationForContainerImageFirewallDTO dto =
        new ApiVerifyOrCreateApplicationForContainerImageFirewallDTO();
    dto.setRepositoryManagerInstanceId(repositoryManager.getInstanceId());
    dto.setRepositoryPublicId(repository.getPublicId());
    dto.setContainerImageName("image1");
    dto.setContainerImageNamespace("namespace1");
    dto.setContainerImageVersion("version1");
    dto.setBaseUrl("baseUrl1");
    dto.setQuarantineEnabled(false);

    HttpResponse response = restRequest()
        .path(ApplicationSummaryResource.VERIFY_OR_CREATE_APP_FOR_CONTAINER_IMAGE_PATH)
        .body(dto)
        .post();
    assertResponseStatus(200, response);

    Repository updatedRepository = repositoryDAO.getByRepositoryManagerInstanceIdAndPublicIdNotNull(
        repositoryManager.getInstanceId(), repository.getPublicId());
    assertThat(updatedRepository.isAuditEnabled()).isTrue();
    assertThat(updatedRepository.isQuarantineEnabled()).isFalse();
  }

  @Test
  public void testVerifyOrCreateApplicationForContainerImageFirewall_FeatureFlagDisabled() throws Exception {
    SystemConfigurationPropertyFeature.CONTAINER_IMAGES_EVAL_ENABLED.setEnabled(false);

    HttpResponse response = restRequest()
        .path(ApplicationSummaryResource.VERIFY_OR_CREATE_APP_FOR_CONTAINER_IMAGE_PATH)
        .body(new ApiVerifyOrCreateApplicationForContainerImageFirewallDTO())
        .post();

    assertResponseStatus(404, response);
    assertThat(response.getBodyText()).isEqualTo("Feature not supported.");
  }

  @Test
  public void testVerifyOrCreateApplicationForContainerImage_MissingLicenseFeature() throws Exception {
    testProductLicense.setMissingFeatures(LicensedFeature.CONTAINER_IMAGES_EVALUATION);

    HttpResponse response = restRequest()
        .path(ApplicationSummaryResource.VERIFY_OR_CREATE_APP_FOR_CONTAINER_IMAGE_PATH)
        .body(new ApiVerifyOrCreateApplicationForContainerImageFirewallDTO())
        .post();

    assertResponseStatus(402, response);
    assertThat(response.getBodyText()).isEqualTo("Your IQ Server license does not enable this feature.");
  }

  @Test
  public void testVerifyOrCreateApplicationForContainerImageFirewall_NoRepositoryExist() throws Exception {
    SystemConfigurationPropertyFeature.CONTAINER_IMAGES_EVAL_ENABLED.setEnabled(true);
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager("instance1");
    repositoryManager.setBaseUrl("baseUrl1");
    repositoryManagerDAO.update(repositoryManager);

    Organization org = tempEntity.newOrganization("Firewall For Docker");
    Organization org1 = tempEntity.newOrganization("baseUrl1");

    org.setRelatedRepositoryId(RepositoryContainer.SINGLETON.getId());
    org1.setRelatedRepositoryManagerId(repositoryManager.getId());

    RepositoryContainer.SINGLETON.setRelatedOrganizationId(org.getId());
    repositoryManager.setRelatedOrganizationId(org1.getId());

    org.setParentOrganizationId(Organization.ROOT_ORGANIZATION_ID);
    org1.setParentOrganizationId(org.getId());

    organizationDAO.update(org);
    organizationDAO.update(org1);

    repositoryManagerDAO.update(repositoryManager);

    ApiVerifyOrCreateApplicationForContainerImageFirewallDTO dto =
        new ApiVerifyOrCreateApplicationForContainerImageFirewallDTO();
    dto.setRepositoryManagerInstanceId("instance1");
    dto.setRepositoryPublicId("repositoryPublicId");
    dto.setContainerImageName("image1");
    dto.setContainerImageNamespace("namespace1");
    dto.setContainerImageVersion("version1");
    dto.setBaseUrl("baseUrl1");

    HttpResponse response = restRequest()
        .path(ApplicationSummaryResource.VERIFY_OR_CREATE_APP_FOR_CONTAINER_IMAGE_PATH)
        .body(dto)
        .post();

    assertResponseStatus(404, response);
    assertThat(response.getBodyText())
        .isEqualTo("Cannot find a repository with repositoryManagerInstanceId=instance1 " +
            "and publicId=repositoryPublicId.");
  }
}
