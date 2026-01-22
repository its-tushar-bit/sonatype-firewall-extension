/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.integration.repository;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import jakarta.ws.rs.core.HttpHeaders;

import com.sonatype.clm.dto.model.component.ComponentEvaluationDataRequestList;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.component.ProprietaryComponentNames;
import com.sonatype.clm.dto.model.component.UnquarantinedComponentList;
import com.sonatype.clm.dto.model.policy.RepositoryPolicyEvaluationSummary;
import com.sonatype.clm.dto.model.repository.ConfigureRepositoriesRequest;
import com.sonatype.clm.dto.model.repository.QuarantinedComponentReport;
import com.sonatype.clm.dto.model.repository.RepositoryDTO;
import com.sonatype.clm.dto.model.repository.RepositoryType;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.v2.dto.ApiRepositoryDTO;
import com.sonatype.insight.brain.dataaccess.repository.ProprietaryComponentNamePatternDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryManagerDAO;
import com.sonatype.insight.brain.model.repository.ProprietaryComponentNamePattern;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public abstract class AbstractRepositoryResourceTest
    extends AbstractResourceTest
{
  private static final String REPO_PUBLIC_ID = "publicId";

  protected RepositoryDAO repositoryDAO;

  protected ProprietaryComponentNamePatternDAO proprietaryComponentNamePatternDAO;

  protected RepositoryManagerDAO repositoryManagerDAO;

  @Before
  public void setUp() {
    repositoryDAO = lookup(RepositoryDAO.class);
    proprietaryComponentNamePatternDAO = lookup(ProprietaryComponentNamePatternDAO.class);
    repositoryManagerDAO = lookup(RepositoryManagerDAO.class);
  }

  protected abstract String getUserAgent();

  @Override
  protected HttpRequest restRequest() {
    // Integration REST endpoints don't use/require CSRF
    return super.restRequest().noCsrfToken();
  }

  protected abstract ConfigureRepositoriesRequest createConfigureRepositoriesRequest(RepositoryDTO repositoryDTO);

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
  public void testSetAuditEnabled_True() throws Exception {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    Repository repository = tempEntity.newRepository(repositoryManager, REPO_PUBLIC_ID, false);

    HttpResponse response =
        enableAuditRequest().parameter(repositoryManager.getInstanceId(), repository.getPublicId(), true)
            .post();
    assertResponseStatus(200, response);

    ApiRepositoryDTO repositoryDTO = response.getBody(ApiRepositoryDTO.class);
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
  public void testSetAuditEnabled_WithClientUserAgent() throws Exception {
    String userAgent = getUserAgent();
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    Repository repository = tempEntity.newRepository(repositoryManager, REPO_PUBLIC_ID, false);

    HttpResponse response =
        enableAuditRequest().parameter(repositoryManager.getInstanceId(), repository.getPublicId(), true)
            .header(HttpHeaders.USER_AGENT, userAgent)
            .post();
    assertResponseStatus(200, response);

    RepositoryManager foundRepositoryManager = repositoryManagerDAO.getById(repositoryManager.getId());

    assertThat(foundRepositoryManager.getUserAgent()).isEqualTo(userAgent);
  }

  @Test
  public void testSetQuarantine() throws Exception {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    Repository repository = tempEntity.newRepository(repositoryManager, REPO_PUBLIC_ID, true);

    HttpResponse response = quarantineRequest()
        .parameter(repositoryManager.getInstanceId(), repository.getPublicId(), true).post();
    assertResponseStatus(204, response);

    repository = repositoryDAO.getById(repository.getId());
    RepositoryManager foundRepositoryManager = repositoryManagerDAO.getById(repositoryManager.getId());

    assertThat(repository).isNotNull();
    assertThat(repository.isQuarantineEnabled()).isTrue();
    assertThat(foundRepositoryManager.getUserAgent()).isNull();
  }

  @Test
  public void testSetQuarantine_WithClientUserAgent() throws Exception {
    String userAgent = getUserAgent();
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    Repository repository = tempEntity.newRepository(repositoryManager, REPO_PUBLIC_ID, true);

    HttpResponse response = quarantineRequest()
        .parameter(repositoryManager.getInstanceId(), repository.getPublicId(), true)
        .header(HttpHeaders.USER_AGENT, userAgent)
        .post();
    assertResponseStatus(204, response);

    RepositoryManager foundRepositoryManager = repositoryManagerDAO.getById(repositoryManager.getId());

    assertThat(foundRepositoryManager.getUserAgent()).isEqualTo(userAgent);
  }

  @Test
  public void testGetPolicyEvaluationSummary() throws Exception {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    Repository repository = tempEntity.newRepository(repositoryManager, REPO_PUBLIC_ID, true);
    tempEntity.newRepositoryPolicyViolation(repository.getId(), 8, "path1",
        ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1"));
    tempEntity.newRepositoryPolicyViolation(repository.getId(), 4, "path2",
        ComponentIdentifier.createMavenCoordinates("g2", "a2", "v2"));
    tempEntity.newRepositoryPolicyViolation(repository.getId(), 3, "path3",
        ComponentIdentifier.createMavenCoordinates("g3", "a3", "v3"));
    tempEntity.newRepositoryPolicyViolation(repository.getId(), 1, "path4",
        ComponentIdentifier.createMavenCoordinates("g4", "a4", "v4"));
    tempEntity.newRepositoryComponent(repository.getId(), "/blah", new Date(), null);

    HttpResponse response = summaryRequest().parameter(repositoryManager.getInstanceId(), repository.getPublicId())
        .get();

    assertResponseStatus(200, response);
    RepositoryPolicyEvaluationSummary policyEvaluationSummary = response
        .getBody(RepositoryPolicyEvaluationSummary.class);

    RepositoryManager foundRepositoryManager = repositoryManagerDAO.getById(repositoryManager.getId());

    assertThat(foundRepositoryManager.getUserAgent()).isNull();
    assertThat(policyEvaluationSummary.getCriticalComponentCount()).isEqualTo(1);
    assertThat(policyEvaluationSummary.getSevereComponentCount()).isEqualTo(1);
    assertThat(policyEvaluationSummary.getModerateComponentCount()).isEqualTo(1);
    assertThat(policyEvaluationSummary.getAffectedComponentCount()).isEqualTo(3);
    assertThat(policyEvaluationSummary.getQuarantinedComponentCount()).isEqualTo(1);
  }

  @Test
  public void testGetPolicyEvaluationSummary_WithClientUserAgent() throws Exception {
    String userAgent = getUserAgent();
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    Repository repository = tempEntity.newRepository(repositoryManager, REPO_PUBLIC_ID, true);
    tempEntity.newRepositoryPolicyViolation(repository.getId(), 8, "path1",
        ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1"));
    tempEntity.newRepositoryPolicyViolation(repository.getId(), 4, "path2",
        ComponentIdentifier.createMavenCoordinates("g2", "a2", "v2"));
    tempEntity.newRepositoryPolicyViolation(repository.getId(), 3, "path3",
        ComponentIdentifier.createMavenCoordinates("g3", "a3", "v3"));
    tempEntity.newRepositoryPolicyViolation(repository.getId(), 1, "path4",
        ComponentIdentifier.createMavenCoordinates("g4", "a4", "v4"));
    tempEntity.newRepositoryComponent(repository.getId(), "/blah", new Date(), null);

    HttpResponse response = summaryRequest().parameter(repositoryManager.getInstanceId(), repository.getPublicId())
        .header(HttpHeaders.USER_AGENT, userAgent)
        .get();

    assertResponseStatus(200, response);

    RepositoryManager foundRepositoryManager = repositoryManagerDAO.getById(repositoryManager.getId());

    assertThat(foundRepositoryManager.getUserAgent()).isEqualTo(userAgent);
  }

  @Test
  public void testGetPolicyEvaluationSummary_NoRepository() throws Exception {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    String repositoryId = "NonExistentRepositoryId";

    HttpResponse response = summaryRequest().parameter(repositoryManager.getInstanceId(), repositoryId).get();

    assertResponseStatus(404, response);
    assertThat(response.getBodyText()).isEqualTo(
        "Cannot find a repository with repositoryManagerInstanceId=" + repositoryManager.getInstanceId() +
            " and publicId=" + repositoryId + ".");
  }

  @Test
  public void testGetPolicyEvaluationSummary_RepositoryDisabled() throws Exception {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    Repository repository = tempEntity.newRepository(repositoryManager, REPO_PUBLIC_ID, false);
    String repositoryId = repository.getPublicId();

    HttpResponse response = summaryRequest().parameter(repositoryManager.getInstanceId(), repositoryId).get();

    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).isEqualTo("Repository " + repositoryId + " is disabled.");
  }

  @Test
  public void testGetRepositoryResultsUrl() throws Exception {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    Repository repository = tempEntity.newRepository(repositoryManager, REPO_PUBLIC_ID, true);

    HttpResponse response = repositoryResultsUrlRequest()
        .parameter(repositoryManager.getInstanceId(), repository.getPublicId())
        .get();

    assertResponseStatus(200, response);
    String repositoryResultsUrl = response.getBodyText();

    assertThat(repositoryResultsUrl).isEqualTo("ui/links/repository/" + repository.getId() + "/result");
  }

  @Test
  public void testGetRepositoryResultsUrl_WithClientUserAgent() throws Exception {
    String userAgent = getUserAgent();
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    Repository repository = tempEntity.newRepository(repositoryManager, REPO_PUBLIC_ID, true);

    HttpResponse response = repositoryResultsUrlRequest()
        .parameter(repositoryManager.getInstanceId(), repository.getPublicId())
        .header(HttpHeaders.USER_AGENT, userAgent)
        .get();

    assertResponseStatus(200, response);
    String repositoryResultsUrl = response.getBodyText();

    RepositoryManager foundRepositoryManager = repositoryManagerDAO.getById(repositoryManager.getId());

    assertThat(foundRepositoryManager.getUserAgent()).isEqualTo(userAgent);
    assertThat(repositoryResultsUrl).isEqualTo("ui/links/repository/" + repository.getId() + "/result");
  }

  @Test
  public void testGetRepositoryResultsUrl_NoRepository() throws Exception {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    String repositoryId = "NonExistentRepositoryId";

    HttpResponse response = repositoryResultsUrlRequest()
        .parameter(repositoryManager.getInstanceId(), repositoryId)
        .get();

    assertResponseStatus(404, response);
    assertThat(response.getBodyText()).isEqualTo(
        "Cannot find a repository with repositoryManagerInstanceId=" + repositoryManager.getInstanceId() +
            " and publicId=" + repositoryId + ".");
  }

  @Test
  public void testGetRepositoryResultsUrl_RepositoryDisabled() throws Exception {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    Repository repository = tempEntity.newRepository(repositoryManager, REPO_PUBLIC_ID, false);
    String repositoryId = repository.getPublicId();

    HttpResponse response = repositoryResultsUrlRequest()
        .parameter(repositoryManager.getInstanceId(), repositoryId)
        .get();

    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).isEqualTo("Repository " + repositoryId + " is disabled.");
  }

  @Test
  public void testEvaluateComponents() throws Exception {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    Repository repository = tempEntity.newRepository(repositoryManager, REPO_PUBLIC_ID, false);

    ComponentEvaluationDataRequestList componentEvaluationDataRequestList = new ComponentEvaluationDataRequestList();

    HttpResponse response = evaluateComponentsRequest()
        .parameter(repositoryManager.getInstanceId(), repository.getPublicId()).body(componentEvaluationDataRequestList)
        .post();

    RepositoryManager foundRepositoryManager = repositoryManagerDAO.getById(repositoryManager.getId());

    assertResponseStatus(204, response);
    assertThat(foundRepositoryManager.getUserAgent()).isNull();
  }

  @Test
  public void testEvaluateComponents_WithClientUserAgent() throws Exception {
    String userAgent = getUserAgent();
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    Repository repository = tempEntity.newRepository(repositoryManager, REPO_PUBLIC_ID, false);

    ComponentEvaluationDataRequestList componentEvaluationDataRequestList = new ComponentEvaluationDataRequestList();

    HttpResponse response = evaluateComponentsRequest()
        .parameter(repositoryManager.getInstanceId(), repository.getPublicId()).body(componentEvaluationDataRequestList)
        .header(HttpHeaders.USER_AGENT, userAgent)
        .post();

    RepositoryManager foundRepositoryManager = repositoryManagerDAO.getById(repositoryManager.getId());

    assertResponseStatus(204, response);
    assertThat(foundRepositoryManager.getUserAgent()).isEqualTo(userAgent);
  }

  @Test
  public void testRemoveComponent() throws Exception {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    Repository repository = tempEntity.newRepository(repositoryManager, REPO_PUBLIC_ID);

    HttpResponse response = componentsRequest()
        .parameter(repositoryManager.getInstanceId(), repository.getPublicId(), "somepath/subpath").delete();

    RepositoryManager foundRepositoryManager = repositoryManagerDAO.getById(repositoryManager.getId());

    assertResponseStatus(204, response);
    assertThat(foundRepositoryManager.getUserAgent()).isNull();
  }

  @Test
  public void testRemoveComponent_WithClientUserAgent() throws Exception {
    String userAgent = getUserAgent();
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    Repository repository = tempEntity.newRepository(repositoryManager, REPO_PUBLIC_ID);

    HttpResponse response = componentsRequest()
        .parameter(repositoryManager.getInstanceId(), repository.getPublicId(), "somepath/subpath")
        .header(HttpHeaders.USER_AGENT, userAgent).delete();

    RepositoryManager foundRepositoryManager = repositoryManagerDAO.getById(repositoryManager.getId());

    assertResponseStatus(204, response);
    assertThat(foundRepositoryManager.getUserAgent()).isEqualTo(userAgent);
  }

  @Test
  public void testGetUnquarantinedComponents() throws Exception {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    Repository repository = tempEntity.newRepository(repositoryManager, REPO_PUBLIC_ID);
    Date now = new Date();
    String pathname = "test/pathname";
    tempEntity.newRepositoryComponent(repository.getId(), pathname, now, now);

    HttpResponse response = unquarantinedComponentsRequest()
        .parameter(repositoryManager.getInstanceId(), repository.getPublicId())
        .query("sinceUtcTimestamp=" + (now.getTime())).get();
    assertResponseStatus(200, response);
    UnquarantinedComponentList result = response.getBody(UnquarantinedComponentList.class);

    RepositoryManager foundRepositoryManager = repositoryManagerDAO.getById(repositoryManager.getId());

    assertThat(result.pathnames).containsExactly(pathname);
    assertThat(foundRepositoryManager.getUserAgent()).isNull();
  }

  @Test
  public void testGetUnquarantinedComponents_WithClientUserAgent() throws Exception {
    String userAgent = getUserAgent();
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    Repository repository = tempEntity.newRepository(repositoryManager, REPO_PUBLIC_ID);
    Date now = new Date();
    String pathname = "test/pathname";
    tempEntity.newRepositoryComponent(repository.getId(), pathname, now, now);

    HttpResponse response = unquarantinedComponentsRequest()
        .parameter(repositoryManager.getInstanceId(), repository.getPublicId())
        .query("sinceUtcTimestamp=" + (now.getTime())).header(HttpHeaders.USER_AGENT, userAgent)
        .get();
    assertResponseStatus(200, response);
    UnquarantinedComponentList result = response.getBody(UnquarantinedComponentList.class);

    RepositoryManager foundRepositoryManager = repositoryManagerDAO.getById(repositoryManager.getId());

    assertThat(result.pathnames).containsExactly(pathname);
    assertThat(foundRepositoryManager.getUserAgent()).isEqualTo(userAgent);
  }

  @Test
  public void testAddProprietaryComponentNames() throws Exception {
    RepositoryManager repoManager = tempEntity.newRepositoryManager();
    Repository repo = tempEntity.newRepository(repoManager, "testPublicId", RepositoryType.hosted, "npm");
    ProprietaryComponentNames proprietaryComponentNames = new ProprietaryComponentNames("npm", "name1", "name2");

    HttpResponse response =
        proprietaryNamesRequest().parameter(repoManager.getInstanceId(), repo.getPublicId())
        .body(proprietaryComponentNames).post();
    assertResponseStatus(204, response);

    List<ProprietaryComponentNamePattern> patterns = proprietaryComponentNamePatternDAO.getByFormat("npm");
    assertThat(patterns).allSatisfy(pattern -> {
      assertThat(pattern.getFormat()).isEqualTo("npm");
      assertThat(pattern.getNamespacePattern()).isNull();
      assertThat(pattern.getRepositoryId()).isEqualTo(repo.getId());
    }).extracting(ProprietaryComponentNamePattern::getNamePattern).containsExactlyInAnyOrder("name1", "name2");
  }

  @Test
  public void testGetQuarantinedComponentReportUrl() throws Exception {
    RepositoryManager repoManager = tempEntity.newRepositoryManager();
    Repository repository = tempEntity.newRepository(repoManager, "repo-repo");
    tempEntity.newRepositoryComponent(repository.getId());

    HttpResponse response = quarantinedComponentReportUrlRequest()
        .parameter(repoManager.getInstanceId(), repository.getPublicId(), "path").get();
    assertResponseStatus(200, response);

    assertThat(response.getBody(QuarantinedComponentReport.class).getReportUrl())
        .matches("ui/links/firewall/repositories/quarantinedComponent/.+");
  }

  @Test
  public void testEvaluateComponentMetadata() throws Exception {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    Repository repository = tempEntity.newRepository(repositoryManager, "testRepoPublicId", true, true);

    ComponentEvaluationDataRequestList componentEvaluationDataRequestList = new ComponentEvaluationDataRequestList();

    HttpResponse response = restRequest().path(AbstractRepositoryResource.EVALUATE_COMPONENT_METADATA_PATH)
        .parameter(repositoryManager.getInstanceId(), repository.getPublicId()).body(componentEvaluationDataRequestList)
        .post();
    assertResponseStatus(200, response);
  }

  @Test
  public void testConfigureRepositories() throws Exception {
    String clientUserAgent = getUserAgent();
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
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
        .header(HttpHeaders.USER_AGENT, clientUserAgent).body(configureRepositoriesRequest).post();
    assertResponseStatus(204, response);

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
  public void testRemoveRepository() throws Exception {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    Repository repository = tempEntity.newRepository(repositoryManager, "testRepoMaven", "maven2");

    HttpResponse response =
        restRequest().path(RepositoryResource.REPOSITORY_PATH)
            .parameter(repositoryManager.getInstanceId(), repository.getPublicId()).delete();

    assertResponseStatus(204, response);

    Repository foundRepository = repositoryDAO.getById(repository.getId());
    assertThat(foundRepository).isNull();
  }

  @Test
  public void testGetConfiguredRepositories() throws Exception {
    Date may5th20239AM = Date.from(LocalDateTime.of(2023, 5, 1, 9, 0, 0).atZone(ZoneId.systemDefault()).toInstant());
    Date may5th202310AM = Date.from(LocalDateTime.of(2023, 5, 1, 10, 0, 0).atZone(ZoneId.systemDefault()).toInstant());
    Date may5th202311AM = Date.from(LocalDateTime.of(2023, 5, 1, 11, 0, 0).atZone(ZoneId.systemDefault()).toInstant());

    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    tempEntity.newRepository(repositoryManager, "testRepoNpm", RepositoryType.proxy, "npm",
        may5th20239AM);
    Repository repository =
        tempEntity.newRepository(repositoryManager, "testRepoMaven", RepositoryType.proxy, "maven", may5th202311AM);
    String clientUserAgent = getUserAgent();

    HttpResponse response = restRequest().path(AbstractRepositoryResource.GET_CONFIGURED_REPOSITORIES_PATH)
        .parameter(repositoryManager.getInstanceId())
        .query("sinceUtcTimestamp", may5th202310AM.getTime())
        .header(HttpHeaders.USER_AGENT, clientUserAgent)
        .get();

    assertResponseStatus(200, response);

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
