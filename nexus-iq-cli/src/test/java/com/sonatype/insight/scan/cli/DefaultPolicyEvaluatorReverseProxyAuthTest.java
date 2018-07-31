/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.scan.cli;

import java.util.Arrays;
import java.util.Collection;

import com.sonatype.clm.dto.model.policy.PolicyEvaluationResult;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.security.InternalRealm;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.TestCLMServer;
import com.sonatype.insight.brain.service.TestInsightBrainService.Configurator;
import com.sonatype.insight.test.reverseproxy.ReverseProxyServer;

import org.apache.http.client.HttpResponseException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

@RunWith(Parameterized.class)
public class DefaultPolicyEvaluatorReverseProxyAuthTest
    extends AbstractPolicyEvaluatorTest
{
  private ReverseProxyServer reverseProxy;

  private boolean rutEnabled;

  private boolean anonymousAllowed;

  @Parameterized.Parameters(name = "RUT: {0}, anon: {1}")
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][]{
        {false, false},
        {true, false}
    });
  }

  public DefaultPolicyEvaluatorReverseProxyAuthTest(boolean rutEnabled, boolean anonymousAllowed) {
    this.rutEnabled = rutEnabled;
    this.anonymousAllowed = anonymousAllowed;
  }

  @Override
  @Before
  public void setUp() throws Exception {
    super.setUp();

    createAppAndAuthorizedUser("the-app-id", "mmurdock", "pa55word");
  }

  @After
  public void after() throws Exception {
    reverseProxy.stop();
    stopInsightServer();
  }

  @Test
  public void testPkiAuth() throws Exception {
    Parameters params = new Parameters("--pki-authentication", "-s", reverseProxy.getSslUrl(), "-i",
        "the-app-id", "src/test/data/artifact.jar");

    if (rutEnabled || anonymousAllowed) {
      evaluator.run(params);
      assertLogSummary(new PolicyEvaluationResult());
    }
    else {
      try {
        evaluator.run(params);
        fail("Reverse proxy authentication is disabled and anonymous not allowed - auth should fail");
      }
      catch (ExitException ee) {
        assertThat(ee.getCause(), is(instanceOf(HttpResponseException.class)));
        assertThat(ee.getCause().getMessage(), is("Unauthorized"));
      }
    }
  }

  @Test
  public void testBasicAuth() throws Exception {
    // use certs for SSL, basic auth with bad credentials
    reverseProxy.expectClientAuth();
    Parameters params = new Parameters("-s", reverseProxy.getSslUrl(), "-a", "mrbasic:secret", "-i",
        "another_app", "src/test/data/artifact.jar");

    try {
      evaluator.run(params);
      fail("User doesn't exist yet, so basic auth should have failed");
    }
    catch (ExitException ee) {
      assertThat(ee.getCause(), is(instanceOf(HttpResponseException.class)));
      assertThat(ee.getCause().getMessage(), is("Invalid credentials. Please try again."));
    }

    // same, but with good credentials
    createAppAndAuthorizedUser("another_app", "mrbasic", "secret");

    evaluator.run(params);
    assertLogSummary(new PolicyEvaluationResult());
  }

  @Test
  public void testAnonymousAccess() throws Exception {
    reverseProxy.defaultReverseProxyHandler();
    tempEntity.newApplication("yet_another", Organization.ROOT_ORGANIZATION_ID);
    Parameters params = new Parameters("-s", reverseProxy.getSslUrl(), "-i", "yet_another",
        "src/test/data/artifact.jar");
    if (anonymousAllowed) {
      evaluator.run(params);
      logOutput.assertInfo("Summary of policy violations: 0 critical, 0 severe, 0 moderate");
    }
    else {
      try {
        evaluator.run(params);
        fail("Anonymous access not enabled, auth should fail");
      }
      catch (ExitException ee) {
        assertThat(ee.getCause(), is(instanceOf(HttpResponseException.class)));
        assertThat(ee.getCause().getMessage(), is("Unauthorized"));
      }
    }
  }

  @Override
  protected void startInsightServer() throws Exception {
    // start Insight server
    testInsightServer = new TestCLMServer(false, null, new Configurator()
    {
      @Override
      public void configure(InsightConfig config) {
        config.getReverseProxyAuthentication().setEnabled(rutEnabled);
        config.setAnonymousClientAccessAllowed(anonymousAllowed);
        config.setImportRefrencePoliciesFromHDS(false);
      }
    });
    testInsightServer.start();

    // start proxy server
    reverseProxy = new ReverseProxyServer(testInsightServer.getCLMServer().getPort(), true);
    reverseProxy.start();
  }

  private void createAppAndAuthorizedUser(String appId, String username, String password) {
    tempEntity.newUser(username, new InternalRealm().encryptPassword(password), "a", "b", "a@b");
    Application anotherApp = tempEntity.newApplication(appId, Organization.ROOT_ORGANIZATION_ID);
    Role role = tempEntity.newRole(false /* global */, Permission.EVALUATE_APPLICATION);
    tempEntity.newMembershipMapping(anotherApp.getId(), role.getId(), username);
  }
}
