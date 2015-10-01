/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.client;

import java.util.ArrayList;
import java.util.HashSet;

import com.sonatype.clm.dto.model.License;
import com.sonatype.clm.dto.model.component.ComponentEvaluationDataList;
import com.sonatype.clm.dto.model.component.ComponentEvaluationDataList.ComponentEvaluationData;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.component.RepositoryComponentEvaluationDataRequestList;
import com.sonatype.clm.dto.model.component.RepositoryComponentEvaluationDataRequestList.RepositoryComponentEvaluationDataRequest;
import com.sonatype.clm.dto.model.component.RepositoryComponentEvaluationDataList;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationSummary;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryComponentDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryComponent;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.service.AbstractBrainServiceTest;
import com.sonatype.insight.client.utils.HttpClientUtils.Configuration;
import com.sonatype.insight.client.utils.SimpleAuthentication;

import org.apache.http.client.HttpResponseException;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class FirewallClientTest
    extends AbstractBrainServiceTest
{

  private static final String REPOSITORY_PUBLIC_ID = "central";

  private String rmInstanceId;

  private RepositoryManager repositoryManager;

  private RepositoryDAO repositoryDAO = new RepositoryDAO();

  @Before
  public void start() {
    repositoryManager = tempEntity.newRepositoryManager();
    rmInstanceId = repositoryManager.getInstanceId();
  }

  @Test
  public void testEnableRepository() throws Exception {
    FirewallClient client = new FirewallClient(getConfiguration(), rmInstanceId, REPOSITORY_PUBLIC_ID);

    client.enableRepository();

    Repository repo = new RepositoryDAO()
        .getByRepositoryManagerInstanceIdAndPublicId(rmInstanceId, REPOSITORY_PUBLIC_ID);
    assertEquals(REPOSITORY_PUBLIC_ID, repo.getPublicId());
    assertTrue(repo.isEnabled());
  }

  @Test
  public void testEnableRepository_Error() throws Exception {
    FirewallClient client = new FirewallClient(getCLMServer().getClientConfiguration(), rmInstanceId,
        REPOSITORY_PUBLIC_ID);

    try {
      client.enableRepository();
      fail("Did not throw the expected exception");
    }
    catch (HttpResponseException e) {
      assertEquals(401, e.getStatusCode());
    }
  }

  @Test
  public void testSetQuarantine_Enable() throws Exception {
    Repository repository = tempEntity.newRepository(repositoryManager, REPOSITORY_PUBLIC_ID, true);

    // Check that the initial value is false
    assertThat(repository.isQuarantineEnabled(), is(false));

    FirewallClient client = new FirewallClient(getConfiguration(), rmInstanceId, REPOSITORY_PUBLIC_ID);
    client.setQuarantine(true);

    Repository repo = repositoryDAO.getById(repository.getId());
    assertThat(repo.isQuarantineEnabled(), is(true));
  }

  @Test
  public void testSetQuarantine_Disable() throws Exception {
    Repository repository = tempEntity.newRepository(repositoryManager, REPOSITORY_PUBLIC_ID, true, true);

    // Check that the initial value is true
    assertThat(repository.isQuarantineEnabled(), is(true));

    FirewallClient client = new FirewallClient(getConfiguration(), rmInstanceId, REPOSITORY_PUBLIC_ID);
    client.setQuarantine(false);

    Repository repo = repositoryDAO.getById(repository.getId());
    assertThat(repo.isQuarantineEnabled(), is(false));
  }

  @Test
  public void testSetQuarantine_Error() throws Exception {
    FirewallClient client = new FirewallClient(getConfiguration(), rmInstanceId, REPOSITORY_PUBLIC_ID);
    try {
      client.setQuarantine(true);
      fail("Expected HttpResponseException");
    }
    catch (HttpResponseException e) {
      assertThat(e.getStatusCode(), is(404));
      assertThat(e.getMessage(),
          is("Unknown repository " + REPOSITORY_PUBLIC_ID + " for repositoryManagerInstanceId " + rmInstanceId + "."));
    }
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

    FirewallClient client = new FirewallClient(getConfiguration(), rmInstanceId, repository.getPublicId());
    PolicyEvaluationSummary policyEvaluationSummary = client.getPolicyEvaluationSummary();
    assertThat(policyEvaluationSummary.getCriticalComponentCount(), is(1));
    assertThat(policyEvaluationSummary.getSevereComponentCount(), is(1));
    assertThat(policyEvaluationSummary.getModerateComponentCount(), is(1));
    assertThat(policyEvaluationSummary.getAffectedComponentCount(), is(3));
  }

  @Test
  public void testGetPolicyEvaluationSummary_Error() throws Exception {
    FirewallClient client = new FirewallClient(getConfiguration(), rmInstanceId, REPOSITORY_PUBLIC_ID);
    try {
      client.getPolicyEvaluationSummary();
      fail("Expected HttpResponseException");
    }
    catch (HttpResponseException e) {
      assertThat(e.getStatusCode(), is(404));
      assertThat(e.getMessage(), is("Cannot find a repository with repositoryManagerInstanceId=" + rmInstanceId +
          " and publicId=" + REPOSITORY_PUBLIC_ID + "."));
    }
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
    final FirewallClient client = new FirewallClient(getConfiguration(), rmInstanceId, REPOSITORY_PUBLIC_ID);
    client.enableRepository();

    final RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList = new RepositoryComponentEvaluationDataRequestList();
    client.evaluateComponents(componentEvaluationDataRequestList);
  }

  @Test
  public void testEvaluateComponents_Error() throws Exception {
    final FirewallClient client = new FirewallClient(getConfiguration(), rmInstanceId, REPOSITORY_PUBLIC_ID);
    // do not enable repository

    final RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList = new RepositoryComponentEvaluationDataRequestList();

    try {
      client.evaluateComponents(componentEvaluationDataRequestList);
      fail("Expected exception");
    }
    catch (HttpResponseException e) {
      assertEquals(404, e.getStatusCode());
      assertEquals(
          "Unknown repository " + REPOSITORY_PUBLIC_ID + " for repositoryManagerInstanceId " + rmInstanceId + ".",
          e.getMessage());
    }
  }

  @Test
  public void testEvaluateComponentWithQuarantine() throws Exception {
    FirewallClient client = new FirewallClient(getConfiguration(), rmInstanceId, REPOSITORY_PUBLIC_ID);

    client.enableRepository();

    // Setup the mocked hds return
    ComponentEvaluationDataList hdsResult = new ComponentEvaluationDataList();
    hdsResult.components = new ArrayList<ComponentEvaluationData>();
    ComponentEvaluationData componentEvaluationData = new ComponentEvaluationData();
    componentEvaluationData.hash = "hash";
    componentEvaluationData.matchState = MatchState.EXACT.getId();
    componentEvaluationData.declaredLicenses = new HashSet<License>();
    componentEvaluationData.observedLicenses = new HashSet<License>();
    hdsResult.components.add(componentEvaluationData);
    setHdsResponseForURI("/rest/component/details/firewall", hdsResult, 200);

    RepositoryComponentEvaluationDataRequest repositoryComponentEvaluationDataRequest =
        new RepositoryComponentEvaluationDataRequest();
    repositoryComponentEvaluationDataRequest.format = "maven2";
    repositoryComponentEvaluationDataRequest.pathname = "path";
    repositoryComponentEvaluationDataRequest.hash = componentEvaluationData.hash;
    RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList =
        new RepositoryComponentEvaluationDataRequestList();
    componentEvaluationDataRequestList.components = new ArrayList<RepositoryComponentEvaluationDataRequest>();
    componentEvaluationDataRequestList.components.add(repositoryComponentEvaluationDataRequest);

    RepositoryComponentEvaluationDataList repositoryComponentEvaluationResult =
        client.evaluateComponentWithQuarantine(componentEvaluationDataRequestList);
    assertThat(repositoryComponentEvaluationResult.componentEvalResults, hasSize(1));
    assertThat(repositoryComponentEvaluationResult.componentEvalResults.get(0).quarantine, is(false));
  }

  @Test
  public void testEvaluateComponentWithQuarantine_Error() throws Exception {
    FirewallClient client = new FirewallClient(getConfiguration(), rmInstanceId, REPOSITORY_PUBLIC_ID);
    RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList =
        new RepositoryComponentEvaluationDataRequestList();
    componentEvaluationDataRequestList.components = new ArrayList<RepositoryComponentEvaluationDataRequest>();

    try {
      client.evaluateComponentWithQuarantine(componentEvaluationDataRequestList);
      fail("Expected exception");
    }
    catch (HttpResponseException e) {
      assertEquals(404, e.getStatusCode());
      assertEquals(
          "Unknown repository " + REPOSITORY_PUBLIC_ID + " for repositoryManagerInstanceId " + rmInstanceId + ".",
          e.getMessage());
    }
  }

  @Test
  public void testRemoveComponent() throws Exception {
    Repository repository = tempEntity.newRepository(repositoryManager, REPOSITORY_PUBLIC_ID);
    String pathname = "somepath";
    RepositoryComponent repositoryComponent = tempEntity.newRepositoryComponent(repository.getId(), pathname);

    FirewallClient client = new FirewallClient(getConfiguration(), rmInstanceId, REPOSITORY_PUBLIC_ID);
    client.removeComponent(pathname);

    repositoryComponent = new RepositoryComponentDAO().getById(repositoryComponent.getId());
    assertThat(repositoryComponent, nullValue());
  }

  @Test
  public void testRemoveComponent_Error() throws Exception {
    FirewallClient client = new FirewallClient(getConfiguration(), rmInstanceId, REPOSITORY_PUBLIC_ID);
    try {
      client.removeComponent("somepath");
      fail("Expected HttpResponseException");
    }
    catch (HttpResponseException e) {
      assertThat(e.getStatusCode(), is(404));
      assertThat(e.getMessage(), is("Unknown repository " + REPOSITORY_PUBLIC_ID + " for repositoryManagerInstanceId "
          + rmInstanceId + "."));
    }
  }
}
