/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.integration.repository;

import java.util.Date;
import java.util.List;

import javax.ws.rs.core.HttpHeaders;

import com.sonatype.clm.dto.model.component.ComponentEvaluationDataRequestList;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.component.ProprietaryComponentNames;
import com.sonatype.clm.dto.model.component.UnquarantinedComponentList;
import com.sonatype.clm.dto.model.policy.RepositoryPolicyEvaluationSummary;
import com.sonatype.clm.dto.model.repository.QuarantinedComponentReport;
import com.sonatype.clm.dto.model.repository.onboarding.FirewallOnboardingRepositoryDTO;
import com.sonatype.clm.dto.model.repository.onboarding.FirewallOnboardingRepositoryManagerDTO;
import com.sonatype.clm.dto.model.repository.onboarding.FirewallOnboardingRepositoryType;
import com.sonatype.clm.dto.model.repository.onboarding.FirewallOnboardingRequest;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.v2.dto.ApiRepositoryDTO;
import com.sonatype.insight.brain.dataaccess.repository.ProprietaryComponentNamePatternDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryManagerDAO;
import com.sonatype.insight.brain.dataaccess.repository.onboarding.FirewallOnboardingRepositoryDAO;
import com.sonatype.insight.brain.dataaccess.repository.onboarding.FirewallOnboardingRepositoryManagerDAO;
import com.sonatype.insight.brain.model.repository.ProprietaryComponentNamePattern;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.model.repository.onboarding.FirewallOnboardingRepository;
import com.sonatype.insight.brain.model.repository.onboarding.FirewallOnboardingRepositoryManager;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public abstract class AbstractRepositoryResourceTest
    extends AbstractResourceTest
{
  private static final String REPO_PUBLIC_ID = "publicId";

  private static final RepositoryDAO repositoryDAO = new RepositoryDAO();

  protected abstract String getUserAgent();

  private HttpRequest summaryRequest() {
    return restRequest().path(AbstractRepositoryResource.SUMMARY_PATH);
  }

  private HttpRequest quarantineRequest() {
    return restRequest().path(AbstractRepositoryResource.QUARANTINE_PATH);
  }

  private HttpRequest enableRequest() {
    return restRequest().path(AbstractRepositoryResource.ENABLE_PATH);
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

  @Test
  public void testSetEnabled_True() throws Exception {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    Repository repository = tempEntity.newRepository(repositoryManager, REPO_PUBLIC_ID, false);

    HttpResponse response = enableRequest().parameter(repositoryManager.getInstanceId(), repository.getPublicId(), true)
        .post();
    assertResponseStatus(200, response);

    ApiRepositoryDTO repositoryDTO = response.getBody(ApiRepositoryDTO.class);
    assertThat(repositoryDTO).isNotNull();
    assertThat(repositoryDTO.publicId).isEqualTo(REPO_PUBLIC_ID);
    assertThat(repositoryDTO.repositoryId).isNotBlank();

    repository = repositoryDAO.getById(repository.getId());
    RepositoryManager foundRepositoryManager = new RepositoryManagerDAO().getById(repositoryManager.getId());

    assertThat(repository).isNotNull();
    assertThat(repository.isEnabled()).isTrue();
    assertThat(foundRepositoryManager.getUserAgent()).isNull();
  }

  @Test
  public void testSetEnabled_WithClientUserAgent() throws Exception {
    String userAgent = getUserAgent();
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    Repository repository = tempEntity.newRepository(repositoryManager, REPO_PUBLIC_ID, false);

    HttpResponse response = enableRequest().parameter(repositoryManager.getInstanceId(), repository.getPublicId(), true)
        .header(HttpHeaders.USER_AGENT, userAgent)
        .post();
    assertResponseStatus(200, response);

    RepositoryManager foundRepositoryManager = new RepositoryManagerDAO().getById(repositoryManager.getId());

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
    RepositoryManager foundRepositoryManager = new RepositoryManagerDAO().getById(repositoryManager.getId());

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

    RepositoryManager foundRepositoryManager = new RepositoryManagerDAO().getById(repositoryManager.getId());

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

    RepositoryManager foundRepositoryManager = new RepositoryManagerDAO().getById(repositoryManager.getId());

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

    RepositoryManager foundRepositoryManager = new RepositoryManagerDAO().getById(repositoryManager.getId());

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
  public void testEvaluateComponents() throws Exception {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    Repository repository = tempEntity.newRepository(repositoryManager, REPO_PUBLIC_ID, false);

    ComponentEvaluationDataRequestList componentEvaluationDataRequestList = new ComponentEvaluationDataRequestList();

    HttpResponse response = evaluateComponentsRequest()
        .parameter(repositoryManager.getInstanceId(), repository.getPublicId()).body(componentEvaluationDataRequestList)
        .post();

    RepositoryManager foundRepositoryManager = new RepositoryManagerDAO().getById(repositoryManager.getId());

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

    RepositoryManager foundRepositoryManager = new RepositoryManagerDAO().getById(repositoryManager.getId());

    assertResponseStatus(204, response);
    assertThat(foundRepositoryManager.getUserAgent()).isEqualTo(userAgent);
  }

  @Test
  public void testRemoveComponent() throws Exception {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    Repository repository = tempEntity.newRepository(repositoryManager, REPO_PUBLIC_ID);

    HttpResponse response = componentsRequest()
        .parameter(repositoryManager.getInstanceId(), repository.getPublicId(), "somepath/subpath").delete();

    RepositoryManager foundRepositoryManager = new RepositoryManagerDAO().getById(repositoryManager.getId());

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

    RepositoryManager foundRepositoryManager = new RepositoryManagerDAO().getById(repositoryManager.getId());

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

    RepositoryManager foundRepositoryManager = new RepositoryManagerDAO().getById(repositoryManager.getId());

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

    RepositoryManager foundRepositoryManager = new RepositoryManagerDAO().getById(repositoryManager.getId());

    assertThat(result.pathnames).containsExactly(pathname);
    assertThat(foundRepositoryManager.getUserAgent()).isEqualTo(userAgent);
  }

  @Test
  public void testAddProprietaryComponentNames() throws Exception {
    String repoManId = tempEntity.newRepositoryManager().getInstanceId();
    String repoId = "hosted-repo";
    ProprietaryComponentNames proprietaryComponentNames = new ProprietaryComponentNames("npm", "name1", "name2");

    HttpResponse response =
        proprietaryNamesRequest().parameter(repoManId, repoId)
        .body(proprietaryComponentNames).post();
    assertResponseStatus(204, response);

    List<ProprietaryComponentNamePattern> patterns = new ProprietaryComponentNamePatternDAO().getByFormat("npm");
    assertThat(patterns).allSatisfy(pattern -> {
      assertThat(pattern.getFormat()).isEqualTo("npm");
      assertThat(pattern.getNamespacePattern()).isNull();
      assertThat(pattern.getRepositoryManagerInstanceId()).isEqualTo(repoManId);
      assertThat(pattern.getRepositoryPublicId()).isEqualTo(repoId);
    }).extracting(ProprietaryComponentNamePattern::getNamePattern).containsExactlyInAnyOrder("name1", "name2");

    assertThat(new RepositoryDAO().getByRepositoryManagerInstanceIdAndPublicId(repoManId, repoId)).isNull();
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
        .matches("ui/links/repositories/quarantinedComponent/.+");
  }

  @Test
  public void testEvaluateComponentMetadata() throws Exception {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    Repository repository = tempEntity.newRepository(repositoryManager, "testRepoPublicId", true, true);

    ComponentEvaluationDataRequestList componentEvaluationDataRequestList = new ComponentEvaluationDataRequestList();

    HttpResponse response = restRequest().path(AbstractRepositoryResource.EVALUATE_COMPONENT_METADATA)
        .parameter(repositoryManager.getInstanceId(), repository.getPublicId()).body(componentEvaluationDataRequestList)
        .post();
    assertResponseStatus(200, response);
  }
  
  @Test
  public void testFirewallOnboarding() throws Exception {
    FirewallOnboardingRequest firewallOnboardingRequest = new FirewallOnboardingRequest();
    FirewallOnboardingRepositoryManagerDTO firewallOnboardingRepositoryManagerDTO =
        new FirewallOnboardingRepositoryManagerDTO();
    firewallOnboardingRepositoryManagerDTO.instanceId = "testInstanceId";
    firewallOnboardingRequest.repositoryManager = firewallOnboardingRepositoryManagerDTO;
    FirewallOnboardingRepositoryDTO firewallOnboardingRepositoryDTO1 = new FirewallOnboardingRepositoryDTO();
    firewallOnboardingRepositoryDTO1.name = "testName1";
    firewallOnboardingRepositoryDTO1.format = ComponentIdentifier.FORMAT_MAVEN;
    firewallOnboardingRepositoryDTO1.type = FirewallOnboardingRepositoryType.hosted;
    firewallOnboardingRequest.repositories.add(firewallOnboardingRepositoryDTO1);
    FirewallOnboardingRepositoryDTO firewallOnboardingRepositoryDTO2 = new FirewallOnboardingRepositoryDTO();
    firewallOnboardingRepositoryDTO2.name = "testName2";
    firewallOnboardingRepositoryDTO2.format = ComponentIdentifier.FORMAT_NPM;
    firewallOnboardingRepositoryDTO2.type = FirewallOnboardingRepositoryType.proxy;
    firewallOnboardingRequest.repositories.add(firewallOnboardingRepositoryDTO2);

    String userAgent = getUserAgent();

    try {
      Date before = new Date();
      HttpResponse response = restRequest().path(AbstractRepositoryResource.FIREWALL_ONBOARDING_PATH) //
          .header(HttpHeaders.USER_AGENT, userAgent) //
          .body(firewallOnboardingRequest) //
          .post();
      assertResponseStatus(204, response);
      Date after = new Date();

      FirewallOnboardingRepositoryManager repoManager =
          new FirewallOnboardingRepositoryManagerDAO().getByInstanceId("testInstanceId");
      assertThat(repoManager.getRequestTime()).isBetween(before, after, true, true);
      assertThat(repoManager.getRequestUsername()).isEqualTo("admin");
      assertThat(repoManager.getRequestUserAgent()).isEqualTo(userAgent);
      assertThat(repoManager.getConfigureTime()).isNull();
      assertThat(repoManager.getConfigureUsername()).isNull();

      FirewallOnboardingRepositoryDAO firewallOnboardingRepositoryDAO = new FirewallOnboardingRepositoryDAO();
      assertThat(firewallOnboardingRepositoryDAO.getByRepositoryManagerId(repoManager.getId())).hasSize(2);
      FirewallOnboardingRepository repository1 =
          firewallOnboardingRepositoryDAO.getByRepositoryManagerIdAndName(repoManager.getId(), "testName1");
      assertThat(repository1.getFormat()).isEqualTo(ComponentIdentifier.FORMAT_MAVEN);
      assertThat(repository1.getType()).isEqualTo(FirewallOnboardingRepositoryType.hosted);
      assertThat(repository1.isAuditEnabled()).isFalse();
      assertThat(repository1.isQuarantineEnabled()).isFalse();
      assertThat(repository1.isNamespaceConfusionProtectionEnabled()).isFalse();
      FirewallOnboardingRepository repository2 =
          firewallOnboardingRepositoryDAO.getByRepositoryManagerIdAndName(repoManager.getId(), "testName2");
      assertThat(repository2.getFormat()).isEqualTo(ComponentIdentifier.FORMAT_NPM);
      assertThat(repository2.getType()).isEqualTo(FirewallOnboardingRepositoryType.proxy);
      assertThat(repository2.isAuditEnabled()).isFalse();
      assertThat(repository2.isQuarantineEnabled()).isFalse();
      assertThat(repository2.isNamespaceConfusionProtectionEnabled()).isFalse();
    }
    finally {
      FirewallOnboardingRepositoryManagerDAO dao = new FirewallOnboardingRepositoryManagerDAO();
      FirewallOnboardingRepositoryManager repoManager = dao.getByInstanceId("testInstanceId");
      dao.delete(repoManager);
    }
  }
}
