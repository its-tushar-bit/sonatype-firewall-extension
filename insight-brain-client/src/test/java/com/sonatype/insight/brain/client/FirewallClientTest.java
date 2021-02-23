/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.concurrent.TimeUnit;

import com.sonatype.clm.dto.model.SecurityVulnerability;
import com.sonatype.clm.dto.model.component.ComponentEvaluationDataList;
import com.sonatype.clm.dto.model.component.ComponentEvaluationDataList.ComponentEvaluationData;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.component.ProprietaryComponentNames;
import com.sonatype.clm.dto.model.component.RepositoryComponentEvaluationDataList;
import com.sonatype.clm.dto.model.component.RepositoryComponentEvaluationDataRequestList;
import com.sonatype.clm.dto.model.component.RepositoryComponentEvaluationDataRequestList.RepositoryComponentEvaluationDataRequest;
import com.sonatype.clm.dto.model.component.UnquarantinedComponentList;
import com.sonatype.clm.dto.model.policy.RepositoryPolicyEvaluationSummary;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryComponentDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryComponent;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.repository.RepositoryPolicyEvaluator;
import com.sonatype.insight.brain.service.AbstractBrainServiceTest;
import com.sonatype.insight.client.utils.HttpClientUtils.Configuration;
import com.sonatype.insight.client.utils.SimpleAuthentication;
import com.sonatype.insight.license.model.LicensedFeature;

import org.apache.http.client.HttpResponseException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assumptions.assumeThat;

@RunWith(Parameterized.class)
public class FirewallClientTest
    extends AbstractBrainServiceTest
{
  private static final String REPOSITORY_PUBLIC_ID = "central";

  private String rmInstanceId;

  private RepositoryManager repositoryManager;

  private RepositoryDAO repositoryDAO = new RepositoryDAO();
  
  private String resourcePath;

  public FirewallClientTest(final String resourcePath) {
    this.resourcePath = resourcePath;
  }

  @Parameterized.Parameters(name = "resourcePath: {0}")
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][]{
        {FirewallClient.NEXUS_RESOURCE_PATH},
        {FirewallClient.ARTIFACTORY_RESOURCE_PATH}
    });
  }

  @Before
  public void start() {
    if (resourcePath.equals(FirewallClient.ARTIFACTORY_RESOURCE_PATH)) {
      getTestProductLicenseManager().setFeatures(LicensedFeature.FIREWALL_FOR_ARTIFACTORY);
    }
    repositoryManager = tempEntity.newRepositoryManager();
    rmInstanceId = repositoryManager.getInstanceId();
  }

  @Test
  public void testSetEnabled_True() throws Exception {
    FirewallClient client = new FirewallClient(getConfiguration(), rmInstanceId, REPOSITORY_PUBLIC_ID, resourcePath);

    client.setEnabled(true);

    Repository repo = new RepositoryDAO().getByRepositoryManagerInstanceIdAndPublicId(rmInstanceId,
        REPOSITORY_PUBLIC_ID);
    assertThat(repo.getPublicId()).isEqualTo(REPOSITORY_PUBLIC_ID);
    assertThat(repo.isEnabled()).isTrue();
  }

  @Test
  public void testSetEnabled_TrueError() throws Exception {
    Configuration configuration = getCLMServer().getClientConfiguration();
    configuration.setServerAuth(null);
    FirewallClient client =
        new FirewallClient(configuration, rmInstanceId, REPOSITORY_PUBLIC_ID, resourcePath);

    assertThatExceptionOfType(HttpResponseException.class).isThrownBy(() -> {
      client.setEnabled(true);
    }).satisfies(e -> assertThat(e.getStatusCode()).isEqualTo(401));
  }

  @Test
  public void testSetEnabled_False() throws Exception {
    FirewallClient client = new FirewallClient(getConfiguration(), rmInstanceId, REPOSITORY_PUBLIC_ID, resourcePath);

    client.setEnabled(false);

    Repository repo = new RepositoryDAO().getByRepositoryManagerInstanceIdAndPublicId(rmInstanceId,
        REPOSITORY_PUBLIC_ID);
    assertThat(repo.getPublicId()).isEqualTo(REPOSITORY_PUBLIC_ID);
    assertThat(repo.isEnabled()).isFalse();
  }

  @Test
  public void testSetQuarantine_Enable() throws Exception {
    Repository repository = tempEntity.newRepository(repositoryManager, REPOSITORY_PUBLIC_ID, true);

    // Check that the initial value is false
    assertThat(repository.isQuarantineEnabled()).isFalse();

    FirewallClient client = new FirewallClient(getConfiguration(), rmInstanceId, REPOSITORY_PUBLIC_ID, resourcePath);
    client.setQuarantine(true);

    Repository repo = repositoryDAO.getById(repository.getId());
    assertThat(repo.isQuarantineEnabled()).isTrue();
  }

  @Test
  public void testSetQuarantine_Disable() throws Exception {
    Repository repository = tempEntity.newRepository(repositoryManager, REPOSITORY_PUBLIC_ID, true, true);

    // Check that the initial value is true
    assertThat(repository.isQuarantineEnabled()).isTrue();

    FirewallClient client = new FirewallClient(getConfiguration(), rmInstanceId, REPOSITORY_PUBLIC_ID, resourcePath);
    client.setQuarantine(false);

    Repository repo = repositoryDAO.getById(repository.getId());
    assertThat(repo.isQuarantineEnabled()).isFalse();
  }

  @Test
  public void testSetQuarantine_Error() throws Exception {
    FirewallClient client = new FirewallClient(getConfiguration(), rmInstanceId, REPOSITORY_PUBLIC_ID, resourcePath);
    assertThatExceptionOfType(HttpResponseException.class).isThrownBy(() -> {
      client.setQuarantine(true);
    }).withMessage(RepositoryDAO.getErrMsgMissingRepo(rmInstanceId, REPOSITORY_PUBLIC_ID))
        .satisfies(e -> assertThat(e.getStatusCode()).isEqualTo(404));
  }

  @Test
  public void testGetPolicyEvaluationSummary() throws Exception {
    Repository repository = tempEntity.newRepository(repositoryManager, REPOSITORY_PUBLIC_ID, true);
    tempEntity.newRepositoryPolicyViolation(repository.getId(), 8, "path1",
        ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1"));
    tempEntity.newRepositoryPolicyViolation(repository.getId(), 4, "path2",
        ComponentIdentifier.createMavenCoordinates("g2", "a2", "v2"));
    tempEntity.newRepositoryPolicyViolation(repository.getId(), 3, "path3",
        ComponentIdentifier.createMavenCoordinates("g3", "a3", "v3"));
    tempEntity.newRepositoryPolicyViolation(repository.getId(), 1, "path4",
        ComponentIdentifier.createMavenCoordinates("g4", "a4", "v4"));
    tempEntity.newRepositoryComponent(repository.getId(), "/quarantined", new Date(), null);

    FirewallClient client =
        new FirewallClient(getConfiguration(), rmInstanceId, repository.getPublicId(), resourcePath);
    RepositoryPolicyEvaluationSummary policyEvaluationSummary = client.getPolicyEvaluationSummary();
    assertThat(policyEvaluationSummary.getCriticalComponentCount()).isEqualTo(1);
    assertThat(policyEvaluationSummary.getSevereComponentCount()).isEqualTo(1);
    assertThat(policyEvaluationSummary.getModerateComponentCount()).isEqualTo(1);
    assertThat(policyEvaluationSummary.getAffectedComponentCount()).isEqualTo(3);
    assertThat(policyEvaluationSummary.getQuarantinedComponentCount()).isEqualTo(1);
  }

  @Test
  public void testGetPolicyEvaluationSummary_Error() throws Exception {
    FirewallClient client = new FirewallClient(getConfiguration(), rmInstanceId, REPOSITORY_PUBLIC_ID, resourcePath);
    assertThatExceptionOfType(HttpResponseException.class).isThrownBy(() -> {
      client.getPolicyEvaluationSummary();
    }).withMessage(RepositoryDAO.getErrMsgMissingRepo(rmInstanceId, REPOSITORY_PUBLIC_ID))
        .satisfies(e -> assertThat(e.getStatusCode()).isEqualTo(404));
  }

  private Configuration getConfiguration() {
    Configuration config = getCLMServer().getClientConfiguration();
    SimpleAuthentication auth = new SimpleAuthentication();
    auth.setPassword("admin123");
    auth.setUsername("admin");
    config.setServerAuth(auth);
    return config;
  }

  @Test
  public void testEvaluateComponents_Empty() throws Exception {
    final FirewallClient client =
        new FirewallClient(getConfiguration(), rmInstanceId, REPOSITORY_PUBLIC_ID, resourcePath);
    client.setEnabled(true);

    final RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList =
        new RepositoryComponentEvaluationDataRequestList();
    client.evaluateComponents(componentEvaluationDataRequestList);
  }

  @Test
  public void testEvaluateComponents_Error() throws Exception {
    final FirewallClient client =
        new FirewallClient(getConfiguration(), rmInstanceId, REPOSITORY_PUBLIC_ID, resourcePath);
    // do not enable repository

    final RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList =
        new RepositoryComponentEvaluationDataRequestList();

    assertThatExceptionOfType(HttpResponseException.class).isThrownBy(() -> {
      client.evaluateComponents(componentEvaluationDataRequestList);
    }).withMessage(RepositoryDAO.getErrMsgMissingRepo(rmInstanceId, REPOSITORY_PUBLIC_ID))
        .satisfies(e -> assertThat(e.getStatusCode()).isEqualTo(404));
  }

  @Test
  public void testEvaluateComponentsAdhoc() throws Exception {
    assumeThat(resourcePath).as("evaluateComponentsAdhoc is not available for Artifactory")
        .isEqualTo(FirewallClient.NEXUS_RESOURCE_PATH);

    FirewallClient client = new FirewallClient(getConfiguration(), rmInstanceId, REPOSITORY_PUBLIC_ID,
        FirewallClient.NEXUS_RESOURCE_PATH);

    tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID);

    // Setup the mocked hds return
    ComponentEvaluationDataList hdsResult = new ComponentEvaluationDataList();
    ComponentEvaluationData componentEvaluationData = new ComponentEvaluationData();
    componentEvaluationData.hash = "hash";
    componentEvaluationData.matchState = MatchState.EXACT.getId();
    componentEvaluationData.declaredLicenses = new HashSet<>();
    componentEvaluationData.observedLicenses = new HashSet<>();
    componentEvaluationData.securityVulnerabilities = new ArrayList<>();
    componentEvaluationData.securityVulnerabilities.add(new SecurityVulnerability("refid", "source", 10F));
    hdsResult.components.add(componentEvaluationData);
    hdsRespondWith(hdsResult).atUri(RepositoryPolicyEvaluator.HDS_COMPONENT_DETAILS_PATH);

    RepositoryComponentEvaluationDataRequest repositoryComponentEvaluationDataRequest =
        new RepositoryComponentEvaluationDataRequest();
    repositoryComponentEvaluationDataRequest.format = "npm";
    repositoryComponentEvaluationDataRequest.pathname = "foobar";
    repositoryComponentEvaluationDataRequest.hash = componentEvaluationData.hash;
    RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList =
        new RepositoryComponentEvaluationDataRequestList();
    componentEvaluationDataRequestList.cause = RepositoryComponentEvaluationDataRequestList.ADHOC;
    componentEvaluationDataRequestList.components.add(repositoryComponentEvaluationDataRequest);

    RepositoryComponentEvaluationDataList repositoryComponentEvaluationResult = client
        .evaluateComponentsAdhoc(componentEvaluationDataRequestList);
    assertThat(repositoryComponentEvaluationResult.componentEvalResults).hasSize(1);
    assertThat(repositoryComponentEvaluationResult.componentEvalResults.get(0).policyAlerts).hasSize(1);
  }

  @Test
  public void testEvaluateComponentWithQuarantine() throws Exception {
    FirewallClient client = new FirewallClient(getConfiguration(), rmInstanceId, REPOSITORY_PUBLIC_ID, resourcePath);

    client.setEnabled(true);

    // Setup the mocked hds return
    ComponentEvaluationDataList hdsResult = new ComponentEvaluationDataList();
    hdsResult.components = new ArrayList<>();
    ComponentEvaluationData componentEvaluationData = new ComponentEvaluationData();
    componentEvaluationData.hash = "hash";
    componentEvaluationData.matchState = MatchState.EXACT.getId();
    componentEvaluationData.declaredLicenses = new HashSet<>();
    componentEvaluationData.observedLicenses = new HashSet<>();
    hdsResult.components.add(componentEvaluationData);
    hdsRespondWith(hdsResult).atUri("/rest/component/details/firewall");

    RepositoryComponentEvaluationDataRequest repositoryComponentEvaluationDataRequest =
        new RepositoryComponentEvaluationDataRequest();
    repositoryComponentEvaluationDataRequest.format = "maven2";
    repositoryComponentEvaluationDataRequest.pathname = "path";
    repositoryComponentEvaluationDataRequest.hash = componentEvaluationData.hash;
    RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList =
        new RepositoryComponentEvaluationDataRequestList();
    componentEvaluationDataRequestList.components = new ArrayList<>();
    componentEvaluationDataRequestList.components.add(repositoryComponentEvaluationDataRequest);

    RepositoryComponentEvaluationDataList repositoryComponentEvaluationResult = client
        .evaluateComponentWithQuarantine(componentEvaluationDataRequestList);
    assertThat(repositoryComponentEvaluationResult.componentEvalResults).hasSize(1);
    assertThat(repositoryComponentEvaluationResult.componentEvalResults.get(0).quarantine).isFalse();
  }

  @Test
  public void testEvaluateComponentWithQuarantine_Error() throws Exception {
    FirewallClient client = new FirewallClient(getConfiguration(), rmInstanceId, REPOSITORY_PUBLIC_ID, resourcePath);
    RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList =
        new RepositoryComponentEvaluationDataRequestList();
    componentEvaluationDataRequestList.components = new ArrayList<>();

    assertThatExceptionOfType(HttpResponseException.class).isThrownBy(() -> {
      client.evaluateComponentWithQuarantine(componentEvaluationDataRequestList);
    }).withMessage(RepositoryDAO.getErrMsgMissingRepo(rmInstanceId, REPOSITORY_PUBLIC_ID))
        .satisfies(e -> assertThat(e.getStatusCode()).isEqualTo(404));
  }

  @Test
  public void testGetUnquarantinedComponents() throws Exception {
    long timestamp = System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(1);
    Repository repository = tempEntity.newRepository(repositoryManager, REPOSITORY_PUBLIC_ID);
    RepositoryComponent component = tempEntity
        .newRepositoryComponent(repository.getId(), "pathname", new Date(timestamp), new Date());

    FirewallClient client = new FirewallClient(getConfiguration(), rmInstanceId, REPOSITORY_PUBLIC_ID, resourcePath);
    UnquarantinedComponentList components = client.getUnquarantinedComponents(timestamp);
    assertThat(components.pathnames).containsExactly(component.getPathname());
  }

  @Test
  public void testGetUnquarantinedComponents_Error() throws Exception {
    FirewallClient client = new FirewallClient(getConfiguration(), rmInstanceId, REPOSITORY_PUBLIC_ID, resourcePath);

    assertThatExceptionOfType(HttpResponseException.class).isThrownBy(() -> {
      client.getUnquarantinedComponents(System.currentTimeMillis());
    }).withMessage(RepositoryDAO.getErrMsgMissingRepo(rmInstanceId, REPOSITORY_PUBLIC_ID))
        .satisfies(e -> assertThat(e.getStatusCode()).isEqualTo(404));
  }

  @Test
  public void testRemoveComponent() throws Exception {
    Repository repository = tempEntity.newRepository(repositoryManager, REPOSITORY_PUBLIC_ID);
    String pathname = "somepath";
    RepositoryComponent repositoryComponent = tempEntity.newRepositoryComponent(repository.getId(), pathname);

    FirewallClient client = new FirewallClient(getConfiguration(), rmInstanceId, REPOSITORY_PUBLIC_ID, resourcePath);
    client.removeComponent(pathname);

    repositoryComponent = new RepositoryComponentDAO().getById(repositoryComponent.getId());
    assertThat(repositoryComponent).isNull();
  }

  @Test
  public void testRemoveComponent_Error() throws Exception {
    FirewallClient client = new FirewallClient(getConfiguration(), rmInstanceId, REPOSITORY_PUBLIC_ID, resourcePath);
    assertThatExceptionOfType(HttpResponseException.class).isThrownBy(() -> {
      client.removeComponent("somepath");
    }).withMessage(RepositoryDAO.getErrMsgMissingRepo(rmInstanceId, REPOSITORY_PUBLIC_ID))
        .satisfies(e -> assertThat(e.getStatusCode()).isEqualTo(404));
  }

  @Test
  public void testAddProprietaryComponentNames() throws Exception {
    FirewallClient client = new FirewallClient(getConfiguration(), rmInstanceId, REPOSITORY_PUBLIC_ID, resourcePath);
    client.addProprietaryComponentNames(new ProprietaryComponentNames("npm", "internal-name"));
  }
}
