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
import com.sonatype.insight.brain.dataaccess.repository.ProxyRepositoryComponentDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.policy.PersistedPolicyEvaluationPollingResult;
import com.sonatype.insight.brain.model.repository.ProprietaryComponentNamePattern;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.ProxyRepositoryComponent;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.repository.RepositoryPolicyEvaluator;
import com.sonatype.insight.brain.variant.IqH2Test;
import com.sonatype.insight.brain.variant.IqTestContext;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.purl.PackageUrlIdentifier;

import org.cyclonedx.Version;
import org.cyclonedx.generators.json.BomJsonGenerator;
import org.cyclonedx.model.Bom;
import org.cyclonedx.model.Component;
import org.cyclonedx.model.Metadata;
import org.cyclonedx.model.Property;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.sonatype.clm.dto.model.component.RepositoryComponentEvaluationDataRequestList.ADHOC;
import static com.sonatype.clm.dto.model.repository.container.image.FirewallContainerImageUtils.SONATYPE_NEXUS_REPOSITORY_BASE_URL_PROPERTY_NAME;
import static com.sonatype.insight.brain.model.Organization.ROOT_ORGANIZATION_ID;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * H2 port of {@code RepositoryResourceTest} plus every {@code @Test} inherited (via
 * {@code AbstractRepositoryResourceTest}) from the legacy JUnit4 base class.
 */
@IqH2Test
class IqH2RepositoryResourceTest
{
  private IqTestContext ctx;

  private static final String REPO_PUBLIC_ID = "publicId";

  private com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO repositoryDAO;

  private com.sonatype.insight.brain.dataaccess.repository.ProprietaryComponentNamePatternDAO proprietaryComponentNamePatternDAO;

  private com.sonatype.insight.brain.dataaccess.repository.RepositoryManagerDAO repositoryManagerDAO;

  private ProxyRepositoryComponentDAO proxyRepositoryComponentDAO;

  private OrganizationDAO organizationDAO;

  private PersistedPolicyEvaluationPollingResultDAO persistedPolicyEvaluationPollingResultDAO;

  @BeforeEach
  void setUp() {
    repositoryDAO = ctx.lookup(com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO.class);
    proprietaryComponentNamePatternDAO =
        ctx.lookup(com.sonatype.insight.brain.dataaccess.repository.ProprietaryComponentNamePatternDAO.class);
    repositoryManagerDAO = ctx.lookup(com.sonatype.insight.brain.dataaccess.repository.RepositoryManagerDAO.class);
    proxyRepositoryComponentDAO = ctx.lookup(ProxyRepositoryComponentDAO.class);
    organizationDAO = ctx.lookup(OrganizationDAO.class);
    persistedPolicyEvaluationPollingResultDAO = ctx.lookup(PersistedPolicyEvaluationPollingResultDAO.class);
  }

  private HttpRequest restRequest() {
    // Integration REST endpoints don't use/require CSRF
    return ctx.restRequest().path(RepositoryResource.RESOURCE_PATH).noCsrfToken();
  }

  private String getUserAgent() {
    return "Nexus/3.60.0-01 (PRO; Mac OS X; 10.16; x86_64; 1.8.0_292)";
  }

  private ConfigureRepositoriesRequest createConfigureRepositoriesRequest(RepositoryDTO repositoryDTO) {
    return new ConfigureRepositoriesRequest("Nexus", "3.60.0-01", "http://localhost:8081",
        Collections.singletonList(repositoryDTO));
  }

  private HttpRequest summaryRequest() {
    return restRequest().path(AbstractRepositoryResource.SUMMARY_PATH);
  }

  private HttpRequest repositoryResultsUrlRequest() {
    return restRequest().path(AbstractRepositoryResource.REPOSITORY_RESULTS_URL);
  }

  private HttpRequest quarantineRequest() {
    return restRequest().path(AbstractRepositoryResource.QUARANTINE_PATH);
  }

  private HttpRequest enableAuditRequest() {
    return restRequest().path(AbstractRepositoryResource.AUDIT_ENABLE_PATH);
  }

  private HttpRequest evaluateComponentsRequest() {
    return restRequest().path(AbstractRepositoryResource.EVALUATE_COMPONENTS_PATH);
  }

  private HttpRequest componentsRequest() {
    return restRequest().path(AbstractRepositoryResource.COMPONENTS_PATH);
  }

  private HttpRequest unquarantinedComponentsRequest() {
    return restRequest().path(AbstractRepositoryResource.UNQUARANTINED_COMPONENTS_PATH);
  }

  private HttpRequest proprietaryNamesRequest() {
    return restRequest().path(AbstractRepositoryResource.PROPRIETARY_NAMES_PATH);
  }

  private HttpRequest quarantinedComponentReportUrlRequest() {
    return restRequest().path(AbstractRepositoryResource.QUARANTINED_COMPONENT_REPORT_URL_PATH);
  }

  private HttpRequest configureRepositoriesRequest() {
    return restRequest().path(AbstractRepositoryResource.CONFIGURE_REPOSITORIES_PATH);
  }

  @Test
  void testGetIgnorePatterns() throws Exception {
    // Prepare request and mock the HDS request
    FirewallIgnorePatterns hdsResult = new FirewallIgnorePatterns();
    hdsResult.regexpsByRepositoryFormat = new HashMap<>();
    hdsResult.regexpsByRepositoryFormat.put("foo", Collections.singletonList("bar"));
    ctx.hdsRespondWith(hdsResult).atUri(FirewallIgnorePatternUpdater.HDS_IGNORE_PATTERNS_PATH);

    HttpResponse response = restRequest().path(RepositoryResource.IGNORE_PATTERNS_PATH).get();
    ctx.assertResponseStatus(200, response);
    assertThat(response.getBody(FirewallIgnorePatterns.class).regexpsByRepositoryFormat)
        .isEqualTo(hdsResult.regexpsByRepositoryFormat);
  }

  @Test
  void testEvaluateComponentsAdhoc() throws Exception {
    // Setup the mocked hds return
    ComponentEvaluationDataList hdsResult = new ComponentEvaluationDataList();
    ComponentEvaluationData componentEvaluationData = new ComponentEvaluationData();
    componentEvaluationData.hash = "hash";
    componentEvaluationData.matchState = MatchState.EXACT.getId();
    componentEvaluationData.declaredLicenses = new HashSet<>();
    componentEvaluationData.observedLicenses = new HashSet<>();
    componentEvaluationData.securityVulnerabilities = createSecurityVulnerabilities();
    hdsResult.components.add(componentEvaluationData);
    ctx.hdsRespondWith(hdsResult).atUri(RepositoryPolicyEvaluator.HDS_COMPONENT_DETAILS_PATH);
    ctx.tempEntity().newPolicy(ROOT_ORGANIZATION_ID);

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

    RepositoryManager repositoryManager = ctx.tempEntity().newRepositoryManager();
    Repository repository = ctx.tempEntity().newRepository(repositoryManager, "repoPublicId", false, false);

    HttpResponse response = restRequest()
        .path(RepositoryResource.EVALUATE_COMPONENTS_ADHOC_PATH)
        .parameter(repositoryManager.getInstanceId(), repository.getPublicId())
        .body(componentEvaluationDataRequestList)
        .post();

    ctx.assertResponseStatus(200, response);
    RepositoryComponentEvaluationDataList responseBody = response.getBody(RepositoryComponentEvaluationDataList.class);
    assertThat(responseBody.componentEvalResults).hasSize(1);
    assertThat(responseBody.componentEvalResults.get(0).policyAlerts).hasSize(1);
  }

  @Test
  void testEvaluateAuditFiltersComponentsWithNullPathname() throws Exception {

    ComponentEvaluationDataList hdsResult = new ComponentEvaluationDataList();
    ComponentEvaluationData componentEvaluationData = new ComponentEvaluationData();
    componentEvaluationData.hash = "validHash";
    componentEvaluationData.matchState = MatchState.EXACT.getId();
    componentEvaluationData.declaredLicenses = new HashSet<>();
    componentEvaluationData.observedLicenses = new HashSet<>();
    componentEvaluationData.securityVulnerabilities = createSecurityVulnerabilities();
    hdsResult.components.add(componentEvaluationData);
    ctx.hdsRespondWith(hdsResult).atUri(RepositoryPolicyEvaluator.HDS_COMPONENT_DETAILS_PATH);
    ctx.tempEntity().newPolicy(ROOT_ORGANIZATION_ID);

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

    RepositoryManager repositoryManager = ctx.tempEntity().newRepositoryManager();
    Repository repository = ctx.tempEntity().newRepository(repositoryManager, "repoPublicId", false, false);

    HttpResponse response = restRequest()
        .path(AbstractRepositoryResource.EVALUATE_COMPONENTS_PATH)
        .parameter(repositoryManager.getInstanceId(), repository.getPublicId())
        .body(componentEvaluationDataRequestList)
        .post();

    ctx.assertResponseStatus(204, response);
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
  void testRemoveProprietaryComponentNames() throws Exception {
    RepositoryManager repoManager = ctx.tempEntity().newRepositoryManager();
    Repository repo = ctx.tempEntity().newRepository(repoManager, "testPublicId", RepositoryType.hosted, "npm");
    proprietaryComponentNamePatternDAO.insert(new ProprietaryComponentNamePattern(repo.getId(), "npm"));

    HttpResponse response =
        restRequest().path(RepositoryResource.PROPRIETARY_NAMES_PATH)
            .parameter(repoManager.getInstanceId(), repo.getPublicId())
            .delete();
    ctx.assertResponseStatus(204, response);

    assertThat(proprietaryComponentNamePatternDAO.getByFormat("npm")).isEmpty();
  }

  @Test
  void testRemoveExtraComponents() throws Exception {
    Date now = new Date();
    RepositoryManager repoManager = ctx.tempEntity().newRepositoryManager("testRepoManagerInstanceId");
    Repository repository1 = ctx.tempEntity().newRepository(repoManager, "testRepoPublicId1", true);
    ProxyRepositoryComponent componentRepo1ToKeep =
        ctx.tempEntity().newRepositoryComponent(repository1.getId(), "pathname1_1", now);
    ProxyRepositoryComponent componentRepo1ToDelete =
        ctx.tempEntity().newRepositoryComponent(repository1.getId(), "pathname1_2", now);
    ProxyRepositoryComponent componentRepo1ToKeepBecauseItIsNewer =
        ctx.tempEntity().newRepositoryComponent(repository1.getId(), "pathname1_3", new Date(now.getTime() + 1));

    Repository repository2 = ctx.tempEntity().newRepository(repoManager, "testRepoPublicId2", true);
    ProxyRepositoryComponent componentRepo2 =
        ctx.tempEntity().newRepositoryComponent(repository2.getId(), "pathname2_1", now);

    com.sonatype.clm.dto.model.component.RepositoryComponentPathnames repositoryComponentPathnames =
        new com.sonatype.clm.dto.model.component.RepositoryComponentPathnames();
    repositoryComponentPathnames.time = now;
    repositoryComponentPathnames.pathnames.add(componentRepo1ToKeep.getPathname());

    HttpResponse response = restRequest().path(RepositoryResource.REMOVE_EXTRA_COMPONENTS_PATH)
        .parameter("testRepoManagerInstanceId", "testRepoPublicId1")
        .body(repositoryComponentPathnames)
        .post();

    ctx.assertResponseStatus(204, response);

    assertThat(proxyRepositoryComponentDAO.getById(componentRepo1ToKeep.getId())).isNotNull();
    assertThat(proxyRepositoryComponentDAO.getById(componentRepo1ToDelete.getId())).isNull();
    assertThat(proxyRepositoryComponentDAO.getById(componentRepo1ToKeepBecauseItIsNewer.getId())).isNotNull();
    assertThat(proxyRepositoryComponentDAO.getById(componentRepo2.getId())).isNotNull();
  }

  @Test
  void testIsContainerImageQuarantined_noFeatureFlag() throws Exception {
    ctx.setFeatures(LicensedFeature.CONTAINER_IMAGES_EVALUATION);
    SystemConfigurationPropertyFeature.CONTAINER_IMAGES_EVAL_ENABLED.setEnabled(false);

    HttpResponse response = restRequest().path(RepositoryResource.IS_QUARANTINED_CONTAINER_IMAGE_PATH)
        .parameter("test-repo-manager", "test-repo", "test-image")
        .get();

    ctx.assertResponseStatus(404, response);
  }

  @Test
  void testIsContainerImageQuarantined_noLicensedFeature() throws Exception {
    ctx.setMissingFeature(LicensedFeature.CONTAINER_IMAGES_EVALUATION);
    SystemConfigurationPropertyFeature.CONTAINER_IMAGES_EVAL_ENABLED.setEnabled(true);

    HttpResponse response = restRequest().path(RepositoryResource.IS_QUARANTINED_CONTAINER_IMAGE_PATH)
        .parameter("test-repo-manager", "test-repo", "test-image")
        .get();

    ctx.assertResponseStatus(402, response);
  }

  @Test
  void testIsContainerImageQuarantined() throws Exception {
    ctx.setFeatures(LicensedFeature.CONTAINER_IMAGES_EVALUATION);
    SystemConfigurationPropertyFeature.CONTAINER_IMAGES_EVAL_ENABLED.setEnabled(true);

    RepositoryManager repositoryManager = ctx.tempEntity().newRepositoryManager();
    Repository repository =
        ctx.tempEntity().newRepository(repositoryManager, "docker-repo", RepositoryType.proxy, "docker");
    Application application = ctx.tempEntity().newApplicationWithParent();
    repository.setRelatedOrganizationId(application.getOrganizationId());
    repositoryDAO.update(repository);

    HttpResponse response = restRequest().path(RepositoryResource.IS_QUARANTINED_CONTAINER_IMAGE_PATH)
        .parameter(repositoryManager.getInstanceId(), repository.getPublicId(), application.getPublicId())
        .get();

    ctx.assertResponseStatus(200, response);
    assertThat(response.getBody(Boolean.class)).isFalse();
  }

  @Test
  void testEvaluateContainerImage_noFeatureFlag() throws Exception {
    ctx.setFeatures(LicensedFeature.CONTAINER_IMAGES_EVALUATION);
    SystemConfigurationPropertyFeature.CONTAINER_IMAGES_EVAL_ENABLED.setEnabled(false);

    HttpResponse response = restRequest().path(RepositoryResource.EVALUATE_CONTAINER_IMAGE_PATH)
        .parameter("test-repo-manager", "test-repo")
        .body(createValidBomJson())
        .post();

    ctx.assertResponseStatus(404, response);
  }

  @Test
  void testEvaluateContainerImage_noLicensedFeature() throws Exception {
    ctx.setMissingFeature(LicensedFeature.CONTAINER_IMAGES_EVALUATION);
    SystemConfigurationPropertyFeature.CONTAINER_IMAGES_EVAL_ENABLED.setEnabled(true);

    HttpResponse response = restRequest().path(RepositoryResource.EVALUATE_CONTAINER_IMAGE_PATH)
        .parameter("test-repo-manager", "test-repo")
        .body(createValidBomJson())
        .post();

    ctx.assertResponseStatus(402, response);
  }

  @Test
  void testEvaluateContainerImage_success() throws Exception {
    ctx.setFeatures(LicensedFeature.CONTAINER_IMAGES_EVALUATION);
    SystemConfigurationPropertyFeature.CONTAINER_IMAGES_EVAL_ENABLED.setEnabled(true);

    RepositoryManager repositoryManager = ctx.tempEntity().newRepositoryManagerWithBaseUrl("https://nexus.example.com");
    Repository repository =
        ctx.tempEntity().newRepository(repositoryManager, "docker-proxy", RepositoryType.proxy, "docker");

    HttpResponse response = restRequest().path(RepositoryResource.EVALUATE_CONTAINER_IMAGE_PATH)
        .parameter(repositoryManager.getInstanceId(), repository.getPublicId())
        .body(createValidBomJson())
        .post();

    ctx.assertResponseStatus(200, response);
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
  void testPollContainerImageEvaluationResult_noFeatureFlag() throws Exception {
    ctx.setFeatures(LicensedFeature.CONTAINER_IMAGES_EVALUATION);
    SystemConfigurationPropertyFeature.CONTAINER_IMAGES_EVAL_ENABLED.setEnabled(false);

    HttpResponse response = restRequest().path(RepositoryResource.EVALUATION_STATUS_CONTAINER_IMAGE_PATH)
        .parameter("test-image-id", "test-status-id")
        .get();

    ctx.assertResponseStatus(404, response);
  }

  @Test
  void testPollContainerImageEvaluationResult_noLicensedFeature() throws Exception {
    ctx.setMissingFeature(LicensedFeature.CONTAINER_IMAGES_EVALUATION);
    SystemConfigurationPropertyFeature.CONTAINER_IMAGES_EVAL_ENABLED.setEnabled(true);

    HttpResponse response = restRequest().path(RepositoryResource.EVALUATION_STATUS_CONTAINER_IMAGE_PATH)
        .parameter("test-image-id", "test-status-id")
        .get();

    ctx.assertResponseStatus(402, response);
  }

  @Test
  void testPollContainerImageEvaluationResult_success() throws Exception {
    ctx.setFeatures(LicensedFeature.CONTAINER_IMAGES_EVALUATION);
    SystemConfigurationPropertyFeature.CONTAINER_IMAGES_EVAL_ENABLED.setEnabled(true);

    RepositoryManager repositoryManager = ctx.tempEntity().newRepositoryManager();
    Repository repository =
        ctx.tempEntity().newRepository(repositoryManager, "docker-repo", RepositoryType.proxy, "docker");

    Organization organization = ctx.tempEntity().newOrganization();
    organization.setRelatedRepositoryId(repository.getId());
    organizationDAO.update(organization);

    Application application = ctx.tempEntity().newApplicationWithParent(organization);

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

    ctx.assertResponseStatus(200, response);
    PolicyEvaluationPollingResult responseBody = response.getBody(PolicyEvaluationPollingResult.class);

    assertThat(responseBody).isNotNull();
    assertThat(responseBody.getStatus()).isEqualTo(PolicyEvaluationStatus.COMPLETED);
  }

  @Test
  void testGetContainerImageReportUrl_noFeatureFlag() throws Exception {
    ctx.setFeatures(LicensedFeature.CONTAINER_IMAGES_EVALUATION);
    SystemConfigurationPropertyFeature.CONTAINER_IMAGES_EVAL_ENABLED.setEnabled(false);

    HttpResponse response = restRequest().path(RepositoryResource.CONTAINER_IMAGE_REPORT_PATH)
        .parameter("test-repo-manager", "test-repo", "test-image")
        .get();

    ctx.assertResponseStatus(404, response);
  }

  @Test
  void testGetContainerImageReportUrl_noLicensedFeature() throws Exception {
    ctx.setMissingFeature(LicensedFeature.CONTAINER_IMAGES_EVALUATION);
    SystemConfigurationPropertyFeature.CONTAINER_IMAGES_EVAL_ENABLED.setEnabled(true);

    HttpResponse response = restRequest().path(RepositoryResource.CONTAINER_IMAGE_REPORT_PATH)
        .parameter("test-repo-manager", "test-repo", "test-image")
        .get();

    ctx.assertResponseStatus(402, response);
  }

  @Test
  void testGetContainerImageReportUrl_success() throws Exception {
    ctx.setFeatures(LicensedFeature.CONTAINER_IMAGES_EVALUATION);
    SystemConfigurationPropertyFeature.CONTAINER_IMAGES_EVAL_ENABLED.setEnabled(true);

    RepositoryManager repositoryManager = ctx.tempEntity().newRepositoryManager();
    Repository repository =
        ctx.tempEntity().newRepository(repositoryManager, "docker-repo", RepositoryType.proxy, "docker");
    Application application = ctx.tempEntity().newApplicationWithParent();
    repository.setRelatedOrganizationId(application.getOrganizationId());
    repositoryDAO.update(repository);

    String scanId = "scan-123";
    ctx.tempEntity().newPolicyEvaluation(application.getId(), Stage.ID_PROXY, scanId);

    HttpResponse response = restRequest().path(RepositoryResource.CONTAINER_IMAGE_REPORT_PATH)
        .parameter(repositoryManager.getInstanceId(), repository.getPublicId(), application.getPublicId())
        .get();

    ctx.assertResponseStatus(200, response);
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

  @Test
  void testSetAuditEnabled_True() throws Exception {
    RepositoryManager repositoryManager = ctx.tempEntity().newRepositoryManager();
    Repository repository = ctx.tempEntity().newRepository(repositoryManager, REPO_PUBLIC_ID, false);

    HttpResponse response =
        enableAuditRequest().parameter(repositoryManager.getInstanceId(), repository.getPublicId(), true)
            .post();
    ctx.assertResponseStatus(200, response);

    com.sonatype.insight.brain.api.v2.dto.ApiRepositoryDTO repositoryDTO =
        response.getBody(com.sonatype.insight.brain.api.v2.dto.ApiRepositoryDTO.class);
    assertThat(repositoryDTO).isNotNull();
    assertThat(repositoryDTO.publicId).isEqualTo(REPO_PUBLIC_ID);
    assertThat(repositoryDTO.repositoryId).isNotBlank();

    repository = repositoryDAO.getById(repository.getId());
    RepositoryManager foundRepositoryManager = repositoryManagerDAO.getById(repositoryManager.getId());

    assertThat(repository).isNotNull();
    assertThat(repository.isAuditEnabled()).isTrue();
    assertThat(foundRepositoryManager.getUserAgent()).isNull();
  }

  @Test
  void testSetAuditEnabled_WithClientUserAgent() throws Exception {
    String userAgent = getUserAgent();
    RepositoryManager repositoryManager = ctx.tempEntity().newRepositoryManager();
    Repository repository = ctx.tempEntity().newRepository(repositoryManager, REPO_PUBLIC_ID, false);

    HttpResponse response =
        enableAuditRequest().parameter(repositoryManager.getInstanceId(), repository.getPublicId(), true)
            .header(jakarta.ws.rs.core.HttpHeaders.USER_AGENT, userAgent)
            .post();
    ctx.assertResponseStatus(200, response);

    RepositoryManager foundRepositoryManager = repositoryManagerDAO.getById(repositoryManager.getId());

    assertThat(foundRepositoryManager.getUserAgent()).isEqualTo(userAgent);
  }

  @Test
  void testSetQuarantine() throws Exception {
    RepositoryManager repositoryManager = ctx.tempEntity().newRepositoryManager();
    Repository repository = ctx.tempEntity().newRepository(repositoryManager, REPO_PUBLIC_ID, true);

    HttpResponse response = quarantineRequest()
        .parameter(repositoryManager.getInstanceId(), repository.getPublicId(), true)
        .post();
    ctx.assertResponseStatus(204, response);

    repository = repositoryDAO.getById(repository.getId());
    RepositoryManager foundRepositoryManager = repositoryManagerDAO.getById(repositoryManager.getId());

    assertThat(repository).isNotNull();
    assertThat(repository.isQuarantineEnabled()).isTrue();
    assertThat(foundRepositoryManager.getUserAgent()).isNull();
  }

  @Test
  void testSetQuarantine_WithClientUserAgent() throws Exception {
    String userAgent = getUserAgent();
    RepositoryManager repositoryManager = ctx.tempEntity().newRepositoryManager();
    Repository repository = ctx.tempEntity().newRepository(repositoryManager, REPO_PUBLIC_ID, true);

    HttpResponse response = quarantineRequest()
        .parameter(repositoryManager.getInstanceId(), repository.getPublicId(), true)
        .header(jakarta.ws.rs.core.HttpHeaders.USER_AGENT, userAgent)
        .post();
    ctx.assertResponseStatus(204, response);

    RepositoryManager foundRepositoryManager = repositoryManagerDAO.getById(repositoryManager.getId());

    assertThat(foundRepositoryManager.getUserAgent()).isEqualTo(userAgent);
  }

  @Test
  void testGetPolicyEvaluationSummary() throws Exception {
    RepositoryManager repositoryManager = ctx.tempEntity().newRepositoryManager();
    Repository repository = ctx.tempEntity().newRepository(repositoryManager, REPO_PUBLIC_ID, true);
    ctx.tempEntity()
        .newRepositoryPolicyViolation(repository.getId(), 8, "path1",
            ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1"));
    ctx.tempEntity()
        .newRepositoryPolicyViolation(repository.getId(), 4, "path2",
            ComponentIdentifier.createMavenCoordinates("g2", "a2", "v2"));
    ctx.tempEntity()
        .newRepositoryPolicyViolation(repository.getId(), 3, "path3",
            ComponentIdentifier.createMavenCoordinates("g3", "a3", "v3"));
    ctx.tempEntity()
        .newRepositoryPolicyViolation(repository.getId(), 1, "path4",
            ComponentIdentifier.createMavenCoordinates("g4", "a4", "v4"));
    ctx.tempEntity().newRepositoryComponent(repository.getId(), "/blah", new Date(), null);

    HttpResponse response = summaryRequest().parameter(repositoryManager.getInstanceId(), repository.getPublicId())
        .get();

    ctx.assertResponseStatus(200, response);
    com.sonatype.clm.dto.model.policy.RepositoryPolicyEvaluationSummary policyEvaluationSummary = response
        .getBody(com.sonatype.clm.dto.model.policy.RepositoryPolicyEvaluationSummary.class);

    RepositoryManager foundRepositoryManager = repositoryManagerDAO.getById(repositoryManager.getId());

    assertThat(foundRepositoryManager.getUserAgent()).isNull();
    assertThat(policyEvaluationSummary.getCriticalComponentCount()).isEqualTo(1);
    assertThat(policyEvaluationSummary.getSevereComponentCount()).isEqualTo(1);
    assertThat(policyEvaluationSummary.getModerateComponentCount()).isEqualTo(1);
    assertThat(policyEvaluationSummary.getAffectedComponentCount()).isEqualTo(3);
    assertThat(policyEvaluationSummary.getQuarantinedComponentCount()).isEqualTo(1);
  }

  @Test
  void testGetPolicyEvaluationSummary_WithClientUserAgent() throws Exception {
    String userAgent = getUserAgent();
    RepositoryManager repositoryManager = ctx.tempEntity().newRepositoryManager();
    Repository repository = ctx.tempEntity().newRepository(repositoryManager, REPO_PUBLIC_ID, true);
    ctx.tempEntity()
        .newRepositoryPolicyViolation(repository.getId(), 8, "path1",
            ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1"));
    ctx.tempEntity()
        .newRepositoryPolicyViolation(repository.getId(), 4, "path2",
            ComponentIdentifier.createMavenCoordinates("g2", "a2", "v2"));
    ctx.tempEntity()
        .newRepositoryPolicyViolation(repository.getId(), 3, "path3",
            ComponentIdentifier.createMavenCoordinates("g3", "a3", "v3"));
    ctx.tempEntity()
        .newRepositoryPolicyViolation(repository.getId(), 1, "path4",
            ComponentIdentifier.createMavenCoordinates("g4", "a4", "v4"));
    ctx.tempEntity().newRepositoryComponent(repository.getId(), "/blah", new Date(), null);

    HttpResponse response = summaryRequest().parameter(repositoryManager.getInstanceId(), repository.getPublicId())
        .header(jakarta.ws.rs.core.HttpHeaders.USER_AGENT, userAgent)
        .get();

    ctx.assertResponseStatus(200, response);

    RepositoryManager foundRepositoryManager = repositoryManagerDAO.getById(repositoryManager.getId());

    assertThat(foundRepositoryManager.getUserAgent()).isEqualTo(userAgent);
  }

  @Test
  void testGetPolicyEvaluationSummary_NoRepository() throws Exception {
    RepositoryManager repositoryManager = ctx.tempEntity().newRepositoryManager();
    String repositoryId = "NonExistentRepositoryId";

    HttpResponse response = summaryRequest().parameter(repositoryManager.getInstanceId(), repositoryId).get();

    ctx.assertResponseStatus(404, response);
    assertThat(response.getBodyText()).isEqualTo(
        "Cannot find a repository with repositoryManagerInstanceId=" + repositoryManager.getInstanceId() +
            " and publicId=" + repositoryId + ".");
  }

  @Test
  void testGetPolicyEvaluationSummary_RepositoryDisabled() throws Exception {
    RepositoryManager repositoryManager = ctx.tempEntity().newRepositoryManager();
    Repository repository = ctx.tempEntity().newRepository(repositoryManager, REPO_PUBLIC_ID, false);
    String repositoryId = repository.getPublicId();

    HttpResponse response = summaryRequest().parameter(repositoryManager.getInstanceId(), repositoryId).get();

    ctx.assertResponseStatus(400, response);
    assertThat(response.getBodyText()).isEqualTo("Repository " + repositoryId + " is disabled.");
  }

  @Test
  void testGetRepositoryResultsUrl() throws Exception {
    RepositoryManager repositoryManager = ctx.tempEntity().newRepositoryManager();
    Repository repository = ctx.tempEntity().newRepository(repositoryManager, REPO_PUBLIC_ID, true);

    HttpResponse response = repositoryResultsUrlRequest()
        .parameter(repositoryManager.getInstanceId(), repository.getPublicId())
        .get();

    ctx.assertResponseStatus(200, response);
    String repositoryResultsUrl = response.getBodyText();

    assertThat(repositoryResultsUrl).isEqualTo("ui/links/repository/" + repository.getId() + "/result");
  }

  @Test
  void testGetRepositoryResultsUrl_WithClientUserAgent() throws Exception {
    String userAgent = getUserAgent();
    RepositoryManager repositoryManager = ctx.tempEntity().newRepositoryManager();
    Repository repository = ctx.tempEntity().newRepository(repositoryManager, REPO_PUBLIC_ID, true);

    HttpResponse response = repositoryResultsUrlRequest()
        .parameter(repositoryManager.getInstanceId(), repository.getPublicId())
        .header(jakarta.ws.rs.core.HttpHeaders.USER_AGENT, userAgent)
        .get();

    ctx.assertResponseStatus(200, response);
    String repositoryResultsUrl = response.getBodyText();

    RepositoryManager foundRepositoryManager = repositoryManagerDAO.getById(repositoryManager.getId());

    assertThat(foundRepositoryManager.getUserAgent()).isEqualTo(userAgent);
    assertThat(repositoryResultsUrl).isEqualTo("ui/links/repository/" + repository.getId() + "/result");
  }

  @Test
  void testGetRepositoryResultsUrl_NoRepository() throws Exception {
    RepositoryManager repositoryManager = ctx.tempEntity().newRepositoryManager();
    String repositoryId = "NonExistentRepositoryId";

    HttpResponse response = repositoryResultsUrlRequest()
        .parameter(repositoryManager.getInstanceId(), repositoryId)
        .get();

    ctx.assertResponseStatus(404, response);
    assertThat(response.getBodyText()).isEqualTo(
        "Cannot find a repository with repositoryManagerInstanceId=" + repositoryManager.getInstanceId() +
            " and publicId=" + repositoryId + ".");
  }

  @Test
  void testGetRepositoryResultsUrl_RepositoryDisabled() throws Exception {
    RepositoryManager repositoryManager = ctx.tempEntity().newRepositoryManager();
    Repository repository = ctx.tempEntity().newRepository(repositoryManager, REPO_PUBLIC_ID, false);
    String repositoryId = repository.getPublicId();

    HttpResponse response = repositoryResultsUrlRequest()
        .parameter(repositoryManager.getInstanceId(), repositoryId)
        .get();

    ctx.assertResponseStatus(400, response);
    assertThat(response.getBodyText()).isEqualTo("Repository " + repositoryId + " is disabled.");
  }

  @Test
  void testEvaluateComponents() throws Exception {
    RepositoryManager repositoryManager = ctx.tempEntity().newRepositoryManager();
    Repository repository = ctx.tempEntity().newRepository(repositoryManager, REPO_PUBLIC_ID, false);

    com.sonatype.clm.dto.model.component.ComponentEvaluationDataRequestList componentEvaluationDataRequestList =
        new com.sonatype.clm.dto.model.component.ComponentEvaluationDataRequestList();

    HttpResponse response = evaluateComponentsRequest()
        .parameter(repositoryManager.getInstanceId(), repository.getPublicId())
        .body(componentEvaluationDataRequestList)
        .post();

    RepositoryManager foundRepositoryManager = repositoryManagerDAO.getById(repositoryManager.getId());

    ctx.assertResponseStatus(204, response);
    assertThat(foundRepositoryManager.getUserAgent()).isNull();
  }

  @Test
  void testEvaluateComponents_WithClientUserAgent() throws Exception {
    String userAgent = getUserAgent();
    RepositoryManager repositoryManager = ctx.tempEntity().newRepositoryManager();
    Repository repository = ctx.tempEntity().newRepository(repositoryManager, REPO_PUBLIC_ID, false);

    com.sonatype.clm.dto.model.component.ComponentEvaluationDataRequestList componentEvaluationDataRequestList =
        new com.sonatype.clm.dto.model.component.ComponentEvaluationDataRequestList();

    HttpResponse response = evaluateComponentsRequest()
        .parameter(repositoryManager.getInstanceId(), repository.getPublicId())
        .body(componentEvaluationDataRequestList)
        .header(jakarta.ws.rs.core.HttpHeaders.USER_AGENT, userAgent)
        .post();

    RepositoryManager foundRepositoryManager = repositoryManagerDAO.getById(repositoryManager.getId());

    ctx.assertResponseStatus(204, response);
    assertThat(foundRepositoryManager.getUserAgent()).isEqualTo(userAgent);
  }

  @Test
  void testRemoveComponent() throws Exception {
    RepositoryManager repositoryManager = ctx.tempEntity().newRepositoryManager();
    Repository repository = ctx.tempEntity().newRepository(repositoryManager, REPO_PUBLIC_ID);

    HttpResponse response = componentsRequest()
        .parameter(repositoryManager.getInstanceId(), repository.getPublicId(), "somepath/subpath")
        .delete();

    RepositoryManager foundRepositoryManager = repositoryManagerDAO.getById(repositoryManager.getId());

    ctx.assertResponseStatus(204, response);
    assertThat(foundRepositoryManager.getUserAgent()).isNull();
  }

  @Test
  void testRemoveComponent_WithClientUserAgent() throws Exception {
    String userAgent = getUserAgent();
    RepositoryManager repositoryManager = ctx.tempEntity().newRepositoryManager();
    Repository repository = ctx.tempEntity().newRepository(repositoryManager, REPO_PUBLIC_ID);

    HttpResponse response = componentsRequest()
        .parameter(repositoryManager.getInstanceId(), repository.getPublicId(), "somepath/subpath")
        .header(jakarta.ws.rs.core.HttpHeaders.USER_AGENT, userAgent)
        .delete();

    RepositoryManager foundRepositoryManager = repositoryManagerDAO.getById(repositoryManager.getId());

    ctx.assertResponseStatus(204, response);
    assertThat(foundRepositoryManager.getUserAgent()).isEqualTo(userAgent);
  }

  @Test
  void testGetUnquarantinedComponents() throws Exception {
    RepositoryManager repositoryManager = ctx.tempEntity().newRepositoryManager();
    Repository repository = ctx.tempEntity().newRepository(repositoryManager, REPO_PUBLIC_ID);
    Date now = new Date();
    String pathname = "test/pathname";
    ctx.tempEntity().newRepositoryComponent(repository.getId(), pathname, now, now);

    HttpResponse response = unquarantinedComponentsRequest()
        .parameter(repositoryManager.getInstanceId(), repository.getPublicId())
        .query("sinceUtcTimestamp=" + (now.getTime()))
        .get();
    ctx.assertResponseStatus(200, response);
    com.sonatype.clm.dto.model.component.UnquarantinedComponentList result =
        response.getBody(com.sonatype.clm.dto.model.component.UnquarantinedComponentList.class);

    RepositoryManager foundRepositoryManager = repositoryManagerDAO.getById(repositoryManager.getId());

    assertThat(result.pathnames).containsExactly(pathname);
    assertThat(foundRepositoryManager.getUserAgent()).isNull();
  }

  @Test
  void testGetUnquarantinedComponents_WithClientUserAgent() throws Exception {
    String userAgent = getUserAgent();
    RepositoryManager repositoryManager = ctx.tempEntity().newRepositoryManager();
    Repository repository = ctx.tempEntity().newRepository(repositoryManager, REPO_PUBLIC_ID);
    Date now = new Date();
    String pathname = "test/pathname";
    ctx.tempEntity().newRepositoryComponent(repository.getId(), pathname, now, now);

    HttpResponse response = unquarantinedComponentsRequest()
        .parameter(repositoryManager.getInstanceId(), repository.getPublicId())
        .query("sinceUtcTimestamp=" + (now.getTime()))
        .header(jakarta.ws.rs.core.HttpHeaders.USER_AGENT, userAgent)
        .get();
    ctx.assertResponseStatus(200, response);
    com.sonatype.clm.dto.model.component.UnquarantinedComponentList result =
        response.getBody(com.sonatype.clm.dto.model.component.UnquarantinedComponentList.class);

    RepositoryManager foundRepositoryManager = repositoryManagerDAO.getById(repositoryManager.getId());

    assertThat(result.pathnames).containsExactly(pathname);
    assertThat(foundRepositoryManager.getUserAgent()).isEqualTo(userAgent);
  }

  @Test
  void testAddProprietaryComponentNames() throws Exception {
    RepositoryManager repoManager = ctx.tempEntity().newRepositoryManager();
    Repository repo = ctx.tempEntity().newRepository(repoManager, "testPublicId", RepositoryType.hosted, "npm");
    com.sonatype.clm.dto.model.component.ProprietaryComponentNames proprietaryComponentNames =
        new com.sonatype.clm.dto.model.component.ProprietaryComponentNames("npm", "name1", "name2");

    HttpResponse response =
        proprietaryNamesRequest().parameter(repoManager.getInstanceId(), repo.getPublicId())
            .body(proprietaryComponentNames)
            .post();
    ctx.assertResponseStatus(204, response);

    List<ProprietaryComponentNamePattern> patterns = proprietaryComponentNamePatternDAO.getByFormat("npm");
    assertThat(patterns).allSatisfy(pattern -> {
      assertThat(pattern.getFormat()).isEqualTo("npm");
      assertThat(pattern.getNamespacePattern()).isNull();
      assertThat(pattern.getRepositoryId()).isEqualTo(repo.getId());
    }).extracting(ProprietaryComponentNamePattern::getNamePattern).containsExactlyInAnyOrder("name1", "name2");
  }

  @Test
  void testGetQuarantinedComponentReportUrl() throws Exception {
    RepositoryManager repoManager = ctx.tempEntity().newRepositoryManager();
    Repository repository = ctx.tempEntity().newRepository(repoManager, "repo-repo");
    ctx.tempEntity().newRepositoryComponent(repository.getId());

    HttpResponse response = quarantinedComponentReportUrlRequest()
        .parameter(repoManager.getInstanceId(), repository.getPublicId(), "path")
        .get();
    ctx.assertResponseStatus(200, response);

    assertThat(response.getBody(com.sonatype.clm.dto.model.repository.QuarantinedComponentReport.class).getReportUrl())
        .matches("ui/links/firewall/repositories/quarantinedComponent/.+");
  }

  @Test
  void testEvaluateComponentMetadata() throws Exception {
    RepositoryManager repositoryManager = ctx.tempEntity().newRepositoryManager();
    Repository repository = ctx.tempEntity().newRepository(repositoryManager, "testRepoPublicId", true, true);

    com.sonatype.clm.dto.model.component.ComponentEvaluationDataRequestList componentEvaluationDataRequestList =
        new com.sonatype.clm.dto.model.component.ComponentEvaluationDataRequestList();

    HttpResponse response = restRequest().path(AbstractRepositoryResource.EVALUATE_COMPONENT_METADATA_PATH)
        .parameter(repositoryManager.getInstanceId(), repository.getPublicId())
        .body(componentEvaluationDataRequestList)
        .post();
    ctx.assertResponseStatus(200, response);
  }

  @Test
  void testConfigureRepositories() throws Exception {
    String clientUserAgent = getUserAgent();
    RepositoryManager repositoryManager = ctx.tempEntity().newRepositoryManager();
    RepositoryDTO repositoryDTO = new RepositoryDTO();
    repositoryDTO.name = "testRepoName";
    repositoryDTO.format = "npm";
    repositoryDTO.type = RepositoryType.hosted;
    repositoryDTO.auditEnabled = false;
    repositoryDTO.quarantineEnabled = false;
    repositoryDTO.policyCompliantComponentSelectionEnabled = false;
    repositoryDTO.namespaceConfusionProtectionEnabled = true;
    ConfigureRepositoriesRequest configureRepositoriesRequest = createConfigureRepositoriesRequest(repositoryDTO);

    HttpResponse response = configureRepositoriesRequest().parameter(repositoryManager.getInstanceId())
        .header(jakarta.ws.rs.core.HttpHeaders.USER_AGENT, clientUserAgent)
        .body(configureRepositoriesRequest)
        .post();
    ctx.assertResponseStatus(204, response);

    repositoryManager = repositoryManagerDAO.getById(repositoryManager.getId());
    assertThat(repositoryManager.getUserAgent()).isEqualTo(clientUserAgent);

    repositoryManager = repositoryManagerDAO.getById(repositoryManager.getId());
    assertThat(repositoryManager.getProductName()).isEqualTo(configureRepositoriesRequest.repositoryManagerProductName);
    assertThat(repositoryManager.getProductVersion())
        .isEqualTo(configureRepositoriesRequest.repositoryManagerProductVersion);

    List<Repository> repositories = repositoryDAO.getByRepositoryManagerId(repositoryManager.getId());
    assertThat(repositories).hasSize(1);
    Repository repository = repositories.get(0);
    assertThat(repository.getName()).isEqualTo("testRepoName");
    assertThat(repository.getFormat()).isEqualTo("npm");
    assertThat(repository.getRepositoryType()).isEqualTo(RepositoryType.hosted);
    assertThat(repository.isAuditEnabled()).isFalse();
    assertThat(repository.isQuarantineEnabled()).isFalse();
    assertThat(repository.isPolicyCompliantComponentSelectionEnabled()).isFalse();
    assertThat(repository.isNamespaceConfusionProtectionEnabled()).isTrue();
  }

  @Test
  void testRemoveRepository() throws Exception {
    RepositoryManager repositoryManager = ctx.tempEntity().newRepositoryManager();
    Repository repository = ctx.tempEntity().newRepository(repositoryManager, "testRepoMaven", "maven2");

    HttpResponse response =
        restRequest().path(RepositoryResource.REPOSITORY_PATH)
            .parameter(repositoryManager.getInstanceId(), repository.getPublicId())
            .delete();

    ctx.assertResponseStatus(204, response);

    Repository foundRepository = repositoryDAO.getById(repository.getId());
    assertThat(foundRepository).isNull();
  }

  @Test
  void testGetConfiguredRepositories() throws Exception {
    Date may5th20239AM =
        Date.from(java.time.LocalDateTime.of(2023, 5, 1, 9, 0, 0).atZone(java.time.ZoneId.systemDefault()).toInstant());
    Date may5th202310AM =
        Date.from(
            java.time.LocalDateTime.of(2023, 5, 1, 10, 0, 0).atZone(java.time.ZoneId.systemDefault()).toInstant());
    Date may5th202311AM =
        Date.from(
            java.time.LocalDateTime.of(2023, 5, 1, 11, 0, 0).atZone(java.time.ZoneId.systemDefault()).toInstant());

    RepositoryManager repositoryManager = ctx.tempEntity().newRepositoryManager();
    ctx.tempEntity()
        .newRepository(repositoryManager, "testRepoNpm", RepositoryType.proxy, "npm",
            may5th20239AM);
    Repository repository =
        ctx.tempEntity()
            .newRepository(repositoryManager, "testRepoMaven", RepositoryType.proxy, "maven", may5th202311AM);
    String clientUserAgent = getUserAgent();

    HttpResponse response = restRequest().path(AbstractRepositoryResource.GET_CONFIGURED_REPOSITORIES_PATH)
        .parameter(repositoryManager.getInstanceId())
        .query("sinceUtcTimestamp", may5th202310AM.getTime())
        .header(jakarta.ws.rs.core.HttpHeaders.USER_AGENT, clientUserAgent)
        .get();

    ctx.assertResponseStatus(200, response);

    repositoryManager = repositoryManagerDAO.getById(repositoryManager.getId());
    assertThat(repositoryManager.getUserAgent()).isEqualTo(clientUserAgent);

    List<RepositoryDTO> repositoryDTOS = response.getBodyList(RepositoryDTO.class);
    assertThat(repositoryDTOS).hasSize(1);
    RepositoryDTO repositoryDTO = repositoryDTOS.get(0);
    assertThat(repositoryDTO.name).isEqualTo(repository.getName());
    assertThat(repositoryDTO.format).isEqualTo(repository.getFormat());
    assertThat(repositoryDTO.type).isEqualTo(repository.getRepositoryType());
    assertThat(repositoryDTO.auditEnabled).isEqualTo(repository.isAuditEnabled());
    assertThat(repositoryDTO.quarantineEnabled).isEqualTo(repository.isQuarantineEnabled());
    assertThat(repositoryDTO.policyCompliantComponentSelectionEnabled).isEqualTo(
        repository.isPolicyCompliantComponentSelectionEnabled());
    assertThat(repositoryDTO.namespaceConfusionProtectionEnabled).isEqualTo(
        repository.isNamespaceConfusionProtectionEnabled());
  }
}
