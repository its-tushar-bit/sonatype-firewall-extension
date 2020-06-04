/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.scan.cli;

import java.util.Arrays;
import java.util.List;

import com.sonatype.clm.dto.model.policy.PolicyEvaluationResult;
import com.sonatype.insight.brain.SslSettings;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.security.PasswordService;
import com.sonatype.insight.brain.service.ErrorResponseGenerator;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.TestCLMServer;
import com.sonatype.insight.brain.service.TestInsightBrainService.Configurator;
import com.sonatype.insight.test.reverseproxy.ReverseProxyServer;

import org.apache.http.client.HttpResponseException;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@RunWith(Parameterized.class)
public class DefaultPolicyEvaluatorReverseProxyAuthTest
    extends AbstractPolicyEvaluatorTest
{
  @Rule
  public SslSettings sslSettings = new SslSettings();

  private ReverseProxyServer reverseProxy;

  private boolean rutEnabled;

  @Parameterized.Parameters(name = "RUT: {0}")
  public static List<Boolean> data() {
    return Arrays.asList(false, true);
  }

  public DefaultPolicyEvaluatorReverseProxyAuthTest(boolean rutEnabled) {
    this.rutEnabled = rutEnabled;
  }

  @Override
  @Before
  public void setUp() throws Exception {
    super.setUp();

    sslSettings.use();
    createAppAndAuthorizedUser("the-app-id", "mmurdock", "pa55word");
  }

  @After
  public void after() throws Exception {
    if (reverseProxy != null) {
      reverseProxy.stop();
    }
    stopInsightServer();
  }

  @Test
  public void testPkiAuth() throws Exception {
    Parameters params = new Parameters("--pki-authentication", "-s", reverseProxy.getSslUrl(), "-i",
        "the-app-id", "src/test/data/artifact.jar");

    if (rutEnabled) {
      evaluator.run(params);
      assertLogSummary(new PolicyEvaluationResult());
    }
    else {
      assertThatExceptionOfType(ExitException.class).isThrownBy(() -> {
        evaluator.run(params);
      }).withCauseInstanceOf(HttpResponseException.class).satisfies(
          e -> assertThat(e.getCause().getMessage()).isEqualTo(ErrorResponseGenerator.MSG_MISSING_CREDENTIALS));
    }
  }

  @Test
  public void testBasicAuth() throws Exception {
    // use certs for SSL, basic auth with bad credentials
    reverseProxy.expectClientAuth();
    Parameters params = new Parameters("-s", reverseProxy.getSslUrl(), "-a", "mrbasic:secret", "-i",
        "another_app", "src/test/data/artifact.jar");

    assertThatExceptionOfType(ExitException.class).isThrownBy(() -> {
      evaluator.run(params);
    }).withCauseInstanceOf(HttpResponseException.class)
        .satisfies(e -> assertThat(e.getCause().getMessage()).isEqualTo("Invalid credentials. Please try again."));

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
    assertThatExceptionOfType(ExitException.class).isThrownBy(() -> {
      evaluator.run(params);
    }).withCauseInstanceOf(HttpResponseException.class).satisfies(
        e -> assertThat(e.getCause().getMessage()).isEqualTo(ErrorResponseGenerator.MSG_MISSING_CREDENTIALS));
  }

  @Override
  protected void startInsightServer() throws Exception {
    // start Insight server
    testInsightServer = new TestCLMServer(false, getBrainModules(), new Configurator()
    {
      @Override
      public void configure(InsightConfig config) {
        config.getReverseProxyAuthentication().setEnabled(rutEnabled);
        config.setImportRefrencePoliciesFromHDS(false);
      }
    });
    testInsightServer.start();

    // start proxy server
    reverseProxy = new ReverseProxyServer(testInsightServer.getCLMServer().getPort(), true);
    reverseProxy.start();
  }

  private void createAppAndAuthorizedUser(String appId, String username, String password) {
    tempEntity.newUser(username, new PasswordService().hashPassword(password), "a", "b", "a@b");
    Application anotherApp = tempEntity.newApplication(appId, Organization.ROOT_ORGANIZATION_ID);
    Role role = tempEntity.newRole(false /* global */, Permission.EVALUATE_APPLICATION);
    tempEntity.newMembershipMapping(anotherApp.getId(), role.getId(), username);
  }
}
