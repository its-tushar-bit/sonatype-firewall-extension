/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.integration.repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import com.sonatype.clm.dto.model.SecurityVulnerability;
import com.sonatype.clm.dto.model.component.ComponentEvaluationDataList;
import com.sonatype.clm.dto.model.component.ComponentEvaluationDataList.ComponentEvaluationData;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.component.FirewallIgnorePatterns;
import com.sonatype.clm.dto.model.component.RepositoryComponentEvaluationDataList;
import com.sonatype.clm.dto.model.component.RepositoryComponentEvaluationDataRequestList;
import com.sonatype.clm.dto.model.component.RepositoryComponentEvaluationDataRequestList.RepositoryComponentEvaluationDataRequest;
import com.sonatype.clm.dto.model.component.RepositoryComponentPathnames;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationPollingResult;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationStatus;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationSummary;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.clm.dto.model.repository.ConfigureRepositoriesRequest;
import com.sonatype.clm.dto.model.repository.RepositoryDTO;
import com.sonatype.clm.dto.model.repository.RepositoryType;
import com.sonatype.clm.dto.model.repository.container.image.FirewallContainerImageEvaluationResponse;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PersistedPolicyEvaluationPollingResultDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryComponentDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.policy.PersistedPolicyEvaluationPollingResult;
import com.sonatype.insight.brain.model.repository.ProprietaryComponentNamePattern;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryComponent;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.repository.RepositoryPolicyEvaluator;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.purl.PackageUrlIdentifier;

import org.cyclonedx.Version;
import org.cyclonedx.generators.json.BomJsonGenerator;
import org.cyclonedx.model.Bom;
import org.cyclonedx.model.Component;
import org.cyclonedx.model.Metadata;
import org.cyclonedx.model.Property;
import org.junit.Before;
import org.junit.Test;

import static com.sonatype.clm.dto.model.component.RepositoryComponentEvaluationDataRequestList.ADHOC;
import static com.sonatype.clm.dto.model.repository.container.image.FirewallContainerImageUtils.SONATYPE_NEXUS_REPOSITORY_BASE_URL_PROPERTY_NAME;
import static com.sonatype.insight.brain.model.Organization.ROOT_ORGANIZATION_ID;
import static org.assertj.core.api.Assertions.assertThat;

public class RepositoryResourceTest
    extends AbstractRepositoryResourceTest
{
  private RepositoryComponentDAO repositoryComponentDAO;

  private OrganizationDAO organizationDAO;

  private PersistedPolicyEvaluationPollingResultDAO persistedPolicyEvaluationPollingResultDAO;

  @Before
  @Override
  public void setUp() {
    super.setUp();
    repositoryComponentDAO = lookup(RepositoryComponentDAO.class);
    organizationDAO = lookup(OrganizationDAO.class);
    persistedPolicyEvaluationPollingResultDAO = lookup(PersistedPolicyEvaluationPollingResultDAO.class);
  }

  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(RepositoryResource.RESOURCE_PATH);
  }

  @Test
  public void testGetIgnorePatterns() throws Exception {
    // Prepare request and mock the HDS request
    FirewallIgnorePatterns hdsResult = new FirewallIgnorePatterns();
    hdsResult.regexpsByRepositoryFormat = new HashMap<>();
    hdsResult.regexpsByRepositoryFormat.put("foo", Collections.singletonList("bar"));
    hdsRespondWith(hdsResult).atUri(FirewallIgnorePatternUpdater.HDS_IGNORE_PATTERNS_PATH);

    HttpResponse response = restRequest().path(RepositoryResource.IGNORE_PATTERNS_PATH).get();
    assertResponseStatus(200, response);
    assertThat(response.getBody(FirewallIgnorePatterns.class).regexpsByRepositoryFormat)
        .isEqualTo(hdsResult.regexpsByRepositoryFormat);
  }

  @Test
  public void testEvaluateComponentsAdhoc() throws Exception {
    // Setup the mocked hds return
    ComponentEvaluationDataList hdsResult = new ComponentEvaluationDataList();
    ComponentEvaluationData componentEvaluationData = new ComponentEvaluationData();
    componentEvaluationData.hash = "hash";
    componentEvaluationData.matchState = MatchState.EXACT.getId();
    componentEvaluationData.declaredLicenses = new HashSet<>();
    componentEvaluationData.observedLicenses = new HashSet<>();
    componentEvaluationData.securityVulnerabilities = createSecurityVulnerabilities();
    hdsResult.components.add(componentEvaluationData);
    hdsRespondWith(hdsResult).atUri(RepositoryPolicyEvaluator.HDS_COMPONENT_DETAILS_PATH);
    tempEntity.newPolicy(ROOT_ORGANIZATION_ID);

    // prepare request
    RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList =
        new RepositoryComponentEvaluationDataRequestList();
    RepositoryComponentEvaluationDataRequest repositoryComponentEvaluationDataRequest =
        new RepositoryComponentEvaluationDataRequest();
    repositoryComponentEvaluationDataRequest.format = "npm";
    repositoryComponentEvaluationDataRequest.pathname = "foobar";
    repositoryComponentEvaluationDataRequest.hash = "hash";
    componentEvaluationDataRequestList.cause = ADHOC;
    componentEvaluationDataRequestList.components.add(repositoryComponentEvaluationDataRequest);

    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    Repository repository = tempEntity.newRepository(repositoryManager, "repoPublicId", false, false);

    HttpResponse response = restRequest()
        .path(RepositoryResource.EVALUATE_COMPONENTS_ADHOC_PATH)
        .parameter(repositoryManager.getInstanceId(), repository.getPublicId())
        .body(componentEvaluationDataRequestList)
        .post();

    assertResponseStatus(200, response);
    RepositoryComponentEvaluationDataList responseBody = response.getBody(RepositoryComponentEvaluationDataList.class);
    assertThat(responseBody.componentEvalResults).hasSize(1);
    assertThat(responseBody.componentEvalResults.get(0).policyAlerts).hasSize(1);
  }

  @Test
  public void testEvaluateAuditFiltersComponentsWithNullPathname() throws Exception {

    ComponentEvaluationDataList hdsResult = new ComponentEvaluationDataList();
    ComponentEvaluationData componentEvaluationData = new ComponentEvaluationData();
    componentEvaluationData.hash = "validHash";
    componentEvaluationData.matchState = MatchState.EXACT.getId();
    componentEvaluationData.declaredLicenses = new HashSet<>();
    componentEvaluationData.observedLicenses = new HashSet<>();
    componentEvaluationData.securityVulnerabilities = createSecurityVulnerabilities();
    hdsResult.components.add(componentEvaluationData);
    hdsRespondWith(hdsResult).atUri(RepositoryPolicyEvaluator.HDS_COMPONENT_DETAILS_PATH);
    tempEntity.newPolicy(ROOT_ORGANIZATION_ID);

    RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList =
        new RepositoryComponentEvaluationDataRequestList();

    RepositoryComponentEvaluationDataRequest validRequest = new RepositoryComponentEvaluationDataRequest();
    validRequest.format = "npm";
    validRequest.pathname = "valid/path.jar";
    validRequest.hash = "validHash";

    RepositoryComponentEvaluationDataRequest invalidRequest = new RepositoryComponentEvaluationDataRequest();
    invalidRequest.format = "npm";
    invalidRequest.pathname = null;
    invalidRequest.hash = "invalidHash";

    componentEvaluationDataRequestList.components.add(validRequest);
    componentEvaluationDataRequestList.components.add(invalidRequest);

    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    Repository repository = tempEntity.newRepository(repositoryManager, "repoPublicId", false, false);

    HttpResponse response = restRequest()
        .path(AbstractRepositoryResource.EVALUATE_COMPONENTS_PATH)
        .parameter(repositoryManager.getInstanceId(), repository.getPublicId())
        .body(componentEvaluationDataRequestList)
        .post();

    assertResponseStatus(204, response);
  }

  private List<SecurityVulnerability> createSecurityVulnerabilities() {
    List<SecurityVulnerability> securityVulnerabilities = new ArrayList<>();
    SecurityVulnerability securityVulnerability = new SecurityVulnerability();
    securityVulnerability.setRefId("refId");
    securityVulnerability.setSeverity(5.0F);
    securityVulnerability.setSource("source");
    securityVulnerability.setUrl("test-url");
    securityVulnerabilities.add(securityVulnerability);
    return securityVulnerabilities;
  }

  @Test
  public void testRemoveProprietaryComponentNames() throws Exception {
    RepositoryManager repoManager = tempEntity.newRepositoryManager();
    Repository repo = tempEntity.newRepository(repoManager, "testPublicId", RepositoryType.hosted, "npm");
    proprietaryComponentNamePatternDAO.insert(new ProprietaryComponentNamePattern(repo.getId(), "npm"));

    HttpResponse response =
        restRequest().path(RepositoryResource.PROPRIETARY_NAMES_PATH)
            .parameter(repoManager.getInstanceId(), repo.getPublicId())
            .delete();
    assertResponseStatus(204, response);

    assertThat(proprietaryComponentNamePatternDAO.getByFormat("npm")).isEmpty();
  }

  @Test
  public void testRemoveExtraComponents() throws Exception {
    Date now = new Date();
    RepositoryManager repoManager = tempEntity.newRepositoryManager("testRepoManagerInstanceId");
    Repository repository1 = tempEntity.newRepository(repoManager, "testRepoPublicId1", true);
    RepositoryComponent componentRepo1ToKeep =
        tempEntity.newRepositoryComponent(repository1.getId(), "pathname1_1", now);
    RepositoryComponent componentRepo1ToDelete =
        tempEntity.newRepositoryComponent(repository1.getId(), "pathname1_2", now);
    RepositoryComponent componentRepo1ToKeepBecauseItIsNewer =
        tempEntity.newRepositoryComponent(repository1.getId(), "pathname1_3", new Date(now.getTime() + 1));

    Repository repository2 = tempEntity.newRepository(repoManager, "testRepoPublicId2", true);
    RepositoryComponent componentRepo2 = tempEntity.newRepositoryComponent(repository2.getId(), "pathname2_1", now);

    RepositoryComponentPathnames repositoryComponentPathnames = new RepositoryComponentPathnames();
    repositoryComponentPathnames.time = now;
    repositoryComponentPathnames.pathnames.add(componentRepo1ToKeep.getPathname());

    HttpResponse response = restRequest().path(RepositoryResource.REMOVE_EXTRA_COMPONENTS_PATH)
        .parameter("testRepoManagerInstanceId", "testRepoPublicId1")
        .body(repositoryComponentPathnames)
        .post();

    assertResponseStatus(204, response);

    assertThat(repositoryComponentDAO.getById(componentRepo1ToKeep.getId())).isNotNull();
    assertThat(repositoryComponentDAO.getById(componentRepo1ToDelete.getId())).isNull();
    assertThat(repositoryComponentDAO.getById(componentRepo1ToKeepBecauseItIsNewer.getId())).isNotNull();
    assertThat(repositoryComponentDAO.getById(componentRepo2.getId())).isNotNull();
  }

  @Test
  public void testIsContainerImageQuarantined_noFeatureFlag() throws Exception {
    setFeatures(LicensedFeature.CONTAINER_IMAGES_EVALUATION);
    SystemConfigurationPropertyFeature.CONTAINER_IMAGES_EVAL_ENABLED.setEnabled(false);

    HttpResponse response = restRequest().path(RepositoryResource.IS_QUARANTINED_CONTAINER_IMAGE_PATH)
        .parameter("test-repo-manager", "test-repo", "test-image")
        .get();

    assertResponseStatus(404, response);
  }

  @Test
  public void testIsContainerImageQuarantined_noLicensedFeature() throws Exception {
    setMissingFeature(LicensedFeature.CONTAINER_IMAGES_EVALUATION);
    SystemConfigurationPropertyFeature.CONTAINER_IMAGES_EVAL_ENABLED.setEnabled(true);

    HttpResponse response = restRequest().path(RepositoryResource.IS_QUARANTINED_CONTAINER_IMAGE_PATH)
        .parameter("test-repo-manager", "test-repo", "test-image")
        .get();

    assertResponseStatus(402, response);
  }

  @Test
  public void testIsContainerImageQuarantined() throws Exception {
    setFeatures(LicensedFeature.CONTAINER_IMAGES_EVALUATION);
    SystemConfigurationPropertyFeature.CONTAINER_IMAGES_EVAL_ENABLED.setEnabled(true);

    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    Repository repository = tempEntity.newRepository(repositoryManager, "docker-repo", RepositoryType.proxy, "docker");
    Application application = tempEntity.newApplicationWithParent();
    repository.setRelatedOrganizationId(application.getOrganizationId());
    repositoryDAO.update(repository);

    HttpResponse response = restRequest().path(RepositoryResource.IS_QUARANTINED_CONTAINER_IMAGE_PATH)
        .parameter(repositoryManager.getInstanceId(), repository.getPublicId(), application.getPublicId())
        .get();

    assertResponseStatus(200, response);
    assertThat(response.getBody(Boolean.class)).isFalse();
  }

  @Test
  public void testEvaluateContainerImage_noFeatureFlag() throws Exception {
    setFeatures(LicensedFeature.CONTAINER_IMAGES_EVALUATION);
    SystemConfigurationPropertyFeature.CONTAINER_IMAGES_EVAL_ENABLED.setEnabled(false);

    HttpResponse response = restRequest().path(RepositoryResource.EVALUATE_CONTAINER_IMAGE_PATH)
        .parameter("test-repo-manager", "test-repo")
        .body(createValidBomJson())
        .post();

    assertResponseStatus(404, response);
  }

  @Test
  public void testEvaluateContainerImage_noLicensedFeature() throws Exception {
    setMissingFeature(LicensedFeature.CONTAINER_IMAGES_EVALUATION);
    SystemConfigurationPropertyFeature.CONTAINER_IMAGES_EVAL_ENABLED.setEnabled(true);

    HttpResponse response = restRequest().path(RepositoryResource.EVALUATE_CONTAINER_IMAGE_PATH)
        .parameter("test-repo-manager", "test-repo")
        .body(createValidBomJson())
        .post();

    assertResponseStatus(402, response);
  }

  @Test
  public void testEvaluateContainerImage_success() throws Exception {
    setFeatures(LicensedFeature.CONTAINER_IMAGES_EVALUATION);
    SystemConfigurationPropertyFeature.CONTAINER_IMAGES_EVAL_ENABLED.setEnabled(true);

    RepositoryManager repositoryManager = tempEntity.newRepositoryManagerWithBaseUrl("https://nexus.example.com");
    Repository repository = tempEntity.newRepository(repositoryManager, "docker-proxy", RepositoryType.proxy, "docker");

    HttpResponse response = restRequest().path(RepositoryResource.EVALUATE_CONTAINER_IMAGE_PATH)
        .parameter(repositoryManager.getInstanceId(), repository.getPublicId())
        .body(createValidBomJson())
        .post();

    assertResponseStatus(200, response);
    FirewallContainerImageEvaluationResponse responseBody =
        response.getBody(FirewallContainerImageEvaluationResponse.class);

    assertThat(responseBody).isNotNull();
    assertThat(responseBody.getStatusId()).isNotBlank();
    assertThat(responseBody.getStatusUrl()).isNotBlank();
    assertThat(responseBody.getContainerImagePublicId()).isNotBlank();
    assertThat(responseBody.getStatusUrl()).contains(responseBody.getContainerImagePublicId());
    assertThat(responseBody.getStatusUrl()).contains(responseBody.getStatusId());
  }

  @Test
  public void testPollContainerImageEvaluationResult_noFeatureFlag() throws Exception {
    setFeatures(LicensedFeature.CONTAINER_IMAGES_EVALUATION);
    SystemConfigurationPropertyFeature.CONTAINER_IMAGES_EVAL_ENABLED.setEnabled(false);

    HttpResponse response = restRequest().path(RepositoryResource.EVALUATION_STATUS_CONTAINER_IMAGE_PATH)
        .parameter("test-image-id", "test-status-id")
        .get();

    assertResponseStatus(404, response);
  }

  @Test
  public void testPollContainerImageEvaluationResult_noLicensedFeature() throws Exception {
    setMissingFeature(LicensedFeature.CONTAINER_IMAGES_EVALUATION);
    SystemConfigurationPropertyFeature.CONTAINER_IMAGES_EVAL_ENABLED.setEnabled(true);

    HttpResponse response = restRequest().path(RepositoryResource.EVALUATION_STATUS_CONTAINER_IMAGE_PATH)
        .parameter("test-image-id", "test-status-id")
        .get();

    assertResponseStatus(402, response);
  }

  @Test
  public void testPollContainerImageEvaluationResult_success() throws Exception {
    setFeatures(LicensedFeature.CONTAINER_IMAGES_EVALUATION);
    SystemConfigurationPropertyFeature.CONTAINER_IMAGES_EVAL_ENABLED.setEnabled(true);

    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    Repository repository = tempEntity.newRepository(repositoryManager, "docker-repo", RepositoryType.proxy, "docker");

    Organization organization = tempEntity.newOrganization();
    organization.setRelatedRepositoryId(repository.getId());
    organizationDAO.update(organization);

    Application application = tempEntity.newApplicationWithParent(organization);

    String containerImagePublicId = application.getPublicId();
    String statusId = "test-status-id";

    PolicyEvaluationPollingResult policyEvaluationPollingResult = new PolicyEvaluationPollingResult();
    policyEvaluationPollingResult.setReason("reason");
    policyEvaluationPollingResult.setStatus(PolicyEvaluationStatus.COMPLETED);
    PersistedPolicyEvaluationPollingResult expected =
        new PersistedPolicyEvaluationPollingResult(application.getId(), statusId, policyEvaluationPollingResult);
    persistedPolicyEvaluationPollingResultDAO.insert(expected);

    HttpResponse response = restRequest().path(RepositoryResource.EVALUATION_STATUS_CONTAINER_IMAGE_PATH)
        .parameter(containerImagePublicId, statusId)
        .get();

    assertResponseStatus(200, response);
    PolicyEvaluationPollingResult responseBody = response.getBody(PolicyEvaluationPollingResult.class);

    assertThat(responseBody).isNotNull();
    assertThat(responseBody.getStatus()).isEqualTo(PolicyEvaluationStatus.COMPLETED);
  }

  @Test
  public void testGetContainerImageReportUrl_noFeatureFlag() throws Exception {
    setFeatures(LicensedFeature.CONTAINER_IMAGES_EVALUATION);
    SystemConfigurationPropertyFeature.CONTAINER_IMAGES_EVAL_ENABLED.setEnabled(false);

    HttpResponse response = restRequest().path(RepositoryResource.CONTAINER_IMAGE_REPORT_PATH)
        .parameter("test-repo-manager", "test-repo", "test-image")
        .get();

    assertResponseStatus(404, response);
  }

  @Test
  public void testGetContainerImageReportUrl_noLicensedFeature() throws Exception {
    setMissingFeature(LicensedFeature.CONTAINER_IMAGES_EVALUATION);
    SystemConfigurationPropertyFeature.CONTAINER_IMAGES_EVAL_ENABLED.setEnabled(true);

    HttpResponse response = restRequest().path(RepositoryResource.CONTAINER_IMAGE_REPORT_PATH)
        .parameter("test-repo-manager", "test-repo", "test-image")
        .get();

    assertResponseStatus(402, response);
  }

  @Test
  public void testGetContainerImageReportUrl_success() throws Exception {
    setFeatures(LicensedFeature.CONTAINER_IMAGES_EVALUATION);
    SystemConfigurationPropertyFeature.CONTAINER_IMAGES_EVAL_ENABLED.setEnabled(true);

    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    Repository repository = tempEntity.newRepository(repositoryManager, "docker-repo", RepositoryType.proxy, "docker");
    Application application = tempEntity.newApplicationWithParent();
    repository.setRelatedOrganizationId(application.getOrganizationId());
    repositoryDAO.update(repository);

    String scanId = "scan-123";
    tempEntity.newPolicyEvaluation(application.getId(), Stage.ID_PROXY, scanId);

    HttpResponse response = restRequest().path(RepositoryResource.CONTAINER_IMAGE_REPORT_PATH)
        .parameter(repositoryManager.getInstanceId(), repository.getPublicId(), application.getPublicId())
        .get();

    assertResponseStatus(200, response);
    PolicyEvaluationSummary summary = response.getBody(PolicyEvaluationSummary.class);

    assertThat(summary).isNotNull();
    assertThat(summary.getReportUrl()).isNotNull();
    assertThat(summary.getReportUrl()).contains(application.getPublicId());
    assertThat(summary.getReportUrl()).contains(scanId);
  }

  private String createValidBomJson() {
    Bom bom = new Bom();
    Metadata metadata = new Metadata();

    Component component = new Component();
    component.setType(Component.Type.CONTAINER);
    component.setPurl(PackageUrlIdentifier.toPackageUrl(
        ComponentIdentifier.createContainerCoordinates("test-namespace", "test-image", "1.0.0")));

    metadata.setComponent(component);

    Property property = new Property();
    property.setName(SONATYPE_NEXUS_REPOSITORY_BASE_URL_PROPERTY_NAME);
    property.setValue("https://nexus.example.com");
    metadata.setProperties(Collections.singletonList(property));

    bom.setMetadata(metadata);

    try {
      return new BomJsonGenerator(bom, Version.VERSION_16).toJsonString();
    }
    catch (Exception e) {
      throw new RuntimeException("Failed to generate BOM JSON", e);
    }
  }

  @Override
  protected String getUserAgent() {
    return "Nexus/3.60.0-01 (PRO; Mac OS X; 10.16; x86_64; 1.8.0_292)";
  }

  @Override
  protected ConfigureRepositoriesRequest createConfigureRepositoriesRequest(RepositoryDTO repositoryDTO) {
    return new ConfigureRepositoriesRequest("Nexus", "3.60.0-01", "http://localhost:8081",
        Collections.singletonList(repositoryDTO));
  }
}
