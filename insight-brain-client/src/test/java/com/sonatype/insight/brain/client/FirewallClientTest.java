/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.client;

import com.sonatype.clm.dto.model.component.RepositoryComponentEvaluationDataRequestList;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationSummary;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.service.AbstractBrainServiceTest;
import com.sonatype.insight.client.utils.HttpClientUtils.Configuration;
import com.sonatype.insight.client.utils.SimpleAuthentication;

import org.apache.http.client.HttpResponseException;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
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

}
