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
import com.sonatype.insight.brain.api.v2.service.ApiReverseProxyAuthenticationConfigurationService;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.configuration.ReverseProxyAuthenticationConfiguration;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.security.PasswordService;
import com.sonatype.insight.brain.service.ErrorResponseGenerator;
import com.sonatype.insight.test.reverseproxy.ReverseProxyServer;

import com.google.common.collect.ImmutableList;
import org.apache.http.client.HttpResponseException;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 * The primary set of tests for the {@link DefaultPolicyEvaluator} in expanded coverage mode.
 *
 * This set of test cases powers not only the regular unit tests (see @{@link
 * JUnitDefaultPolicyEvaluatorReverseProxyAuthTest}, but also the native image configuration and testing. This allows us
 * to have one set of tests which covers all three cases.
 */
@RunWith(Parameterized.class)
public abstract class DefaultPolicyEvaluatorReverseProxyAuthTest
    extends AbstractPolicyEvaluatorTest
{
  @Rule
  public SslSettings sslSettings = new SslSettings();

  private ReverseProxyServer reverseProxy;

  private final boolean rutEnabled;

  public DefaultPolicyEvaluatorReverseProxyAuthTest(boolean rutEnabled) {
    this.rutEnabled = rutEnabled;
  }

  @Parameterized.Parameters(name = "RUT: {0}")
  public static List<Boolean> data() {
    return Arrays.asList(false, true);
  }

  @Override
  protected void startIqTestServer() throws Exception {
    startIqTestServer(config -> config.setImportRefrencePoliciesFromHDS(false));
    tempEntity.newReverseProxyAuthenticationConfiguration(rutEnabled,
          ReverseProxyAuthenticationConfiguration.DEFAULT_USERNAME_HEADER, false, null);
    getCLMServer().getInstance(ApiReverseProxyAuthenticationConfigurationService.class)
        .applyReverseProxyAuthenticationConfigurationToClients();
  }

  @Before
  public void before() throws Exception {
    sslSettings.use();
    createAppAndAuthorizedUser("the-app-id", "mmurdock", "pa55word");

    // start proxy server
    reverseProxy = new ReverseProxyServer(getCLMServer().getPort(), true);
    reverseProxy.start();
  }

  @After
  public void after() throws Exception {
    if (reverseProxy != null) {
      reverseProxy.stop();
    }
  }

  @Test
  public void testPkiAuth() throws Exception {
    List<String> params = ImmutableList.of("--pki-authentication", "-s", reverseProxy.getSslUrl(), "-i",
        "the-app-id", "src/test/data/artifact.jar");

    if (rutEnabled) {
      withTestRunner(params)
          .expectPolicyEvaluationResult(newPolicyEvaluationResultWithOneComponent())
          .doPolicyEvaluationRun();
    }
    else {
      withTestRunner(params)
          .expectFailExit()
          .expectException(HttpResponseException.class, ErrorResponseGenerator.MSG_MISSING_CREDENTIALS)
          .expectErrorLog(
              String.format("The IQ Server %s rejected the supplied credentials.", reverseProxy.getSslUrl()))
          .doPolicyEvaluationRun();
    }
  }

  @Test
  public void testBasicAuth() throws Exception {
    // use certs for SSL, basic auth with bad credentials
    reverseProxy.expectClientAuth();
    List<String> params = ImmutableList.of("-s", reverseProxy.getSslUrl(), "-a", "mrbasic:secret", "-i",
        "another_app", "src/test/data/artifact.jar");

    withTestRunner(params)
        .expectFailExit()
        .expectErrorLog(
            String.format("The IQ Server %s rejected the supplied credentials.", reverseProxy.getSslUrl()))
        .expectException(HttpResponseException.class, "Invalid credentials. Please try again.")
        .doPolicyEvaluationRun();

    // same, but with good credentials
    createAppAndAuthorizedUser("another_app", "mrbasic", "secret");

    withTestRunner(params)
        .expectPolicyEvaluationResult(newPolicyEvaluationResultWithOneComponent())
        .doPolicyEvaluationRun();
  }

  private PolicyEvaluationResult newPolicyEvaluationResultWithOneComponent() {
    PolicyEvaluationResult result = new PolicyEvaluationResult();
    result.setTotalComponentCount(1);
    return result;
  }

  @Test
  public void testAnonymousAccess() throws Exception {
    reverseProxy.defaultReverseProxyHandler();
    tempEntity.newApplication("yet_another", Organization.ROOT_ORGANIZATION_ID);
    List<String> params = ImmutableList.of("-s", reverseProxy.getSslUrl(), "-i", "yet_another",
        "src/test/data/artifact.jar");
    withTestRunner(params)
        .expectFailExit()
        .expectException(HttpResponseException.class, ErrorResponseGenerator.MSG_MISSING_CREDENTIALS)
        .expectErrorLog(
            String.format("The IQ Server %s rejected the supplied credentials.", reverseProxy.getSslUrl()))
        .doPolicyEvaluationRun();
  }

  private void createAppAndAuthorizedUser(String appId, String username, String password) {
    tempEntity.newUser(username, new PasswordService().hashPassword(password), "a", "b", "a@b");
    Application anotherApp = tempEntity.newApplication(appId, Organization.ROOT_ORGANIZATION_ID);
    Role role = tempEntity.newRole(false /* global */, Permission.EVALUATE_APPLICATION);
    tempEntity.newMembershipMapping(anotherApp.getId(), role.getId(), username);
  }
}
