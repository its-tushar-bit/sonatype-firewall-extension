/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.util.Arrays;
import java.util.Collection;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.configuration.ldap.TestLdapServer;
import com.sonatype.insight.brain.model.configuration.ldap.LdapServer;
import com.sonatype.insight.brain.security.UserSessionResource.AuthenticationStatus;
import com.sonatype.insight.brain.service.AbstractBrainServiceTest;
import com.sonatype.insight.brain.service.ErrorResponseGenerator;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.ReverseProxyAuthenticationConfig;
import com.sonatype.insight.brain.service.TestInsightBrainService.Configurator;
import com.sonatype.insight.test.LogOutput;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

@RunWith(Parameterized.class)
public class ReverseProxyAuthcTest
    extends AbstractBrainServiceTest
{
  @Rule
  public TestLdapServer ldapServer = new TestLdapServer();

  @Rule
  public LogOutput logOutput = new LogOutput(ReverseProxyAuthenticationFilter.class);

  private boolean setupLdap;

  private boolean localUser;

  private final Configurator ENABLED = new Configurator()
  {
    @Override
    public void configure(InsightConfig config) {
      config.getReverseProxyAuthentication().setEnabled(true);
    }
  };

  public ReverseProxyAuthcTest(boolean setupLdap, boolean localUser) {
    this.setupLdap = setupLdap;
    this.localUser = localUser;
  }

  @Parameterized.Parameters
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][]{
        // only authenticate local users
        {false, true},
        // only authenticate ldap users
        {true, false},
        // authenticated local users go before ldap users
        {true, true}
    });
  }

  @Override
  public void initTest() throws Exception {
    if (setupLdap) {
      ldapServer.start();
      ldapServer.loadData("/ReverseProxyAuthcTest/ldap_users.ldif");
      LdapServer ldapServer = tempEntity.newLdapServer("LDAP");
      tempEntity.newLdapConnection(ldapServer.getId(), this.ldapServer.getPort());
      tempEntity.newLdapUserMapping(ldapServer.getId());
    }
    if (localUser) {
      // Be sure to keep the detail in-sync with the ldap defined user details for the testuser
      tempEntity.newUser("testuser", "John", "Doe", "test.user@company.com");
    }

    // defer brain server initialization to actual test method
  }

  @Test
  public void testDisabledByDefault() throws Exception {
    initServer(null);

    ReverseProxyAuthenticationConfig reverseProxyAuthcConfig = new ReverseProxyAuthenticationConfig();
    assertThat(reverseProxyAuthcConfig.isEnabled(), is(false));
    assertThat(reverseProxyAuthcConfig.getUsernameHeader(), is("REMOTE_USER"));
    HttpResponse response = restRequest().path("rest/anything").header("REMOTE_USER", "testuser").anon().get();
    assertResponseStatus(401, response);
    assertThat(response.getBodyText(), is(ErrorResponseGenerator.MSG_LOGIN_FAILURE_DEFAULT));
  }

  @Test
  public void testEnabled_DefaultHeader() throws Exception {
    initServer(ENABLED);

    HttpRequest request = restRequest().header("REMOTE_USER", "testuser").anon();
    HttpResponse response = request.subpath(UserSessionResource.RESOURCE_PATH).get();
    assertResponseStatus(200, response);
    assertThat(response.getSessionCookie(), is(notNullValue()));
    AuthenticationStatus authStatus = response.getBody(AuthenticationStatus.class);
    assertThat(authStatus.isAuthenticated(), is(true));
    assertThat(authStatus.isClmUser(), is(localUser));
    assertThat(authStatus.getDisplayName(), is("John Doe"));

    response = request.subpath(PublicApiPaths.ORG_RESOURCE_PATH).get();
    assertResponseStatus(200, response);
    assertThat(response.getSessionCookie(), is(nullValue()));
  }

  @Test
  public void testEnabled_CustomHeader() throws Exception {
    initServer(new Configurator()
    {
      @Override
      public void configure(InsightConfig config) {
        config.getReverseProxyAuthentication().setEnabled(true);
        config.getReverseProxyAuthentication().setUsernameHeader("X-Remote-User");
      }
    });

    HttpRequest request = restRequest().header("X-Remote-User", "testuser").anon();
    HttpResponse response = request.subpath(UserSessionResource.RESOURCE_PATH).get();
    assertResponseStatus(200, response);
    assertThat(response.getSessionCookie(), is(notNullValue()));
    AuthenticationStatus authStatus = response.getBody(AuthenticationStatus.class);
    assertThat(authStatus.isAuthenticated(), is(true));
    assertThat(authStatus.isClmUser(), is(localUser));
    assertThat(authStatus.getDisplayName(), is("John Doe"));
  }

  @Test
  public void testEnabled_SessionCreatedForAnyRequest() throws Exception {
    initServer(ENABLED);

    HttpRequest request = restRequest().header("REMOTE_USER", "testuser").anon();
    HttpResponse response = request.subpath("rest/anything").get();
    assertResponseStatus(404, response);
    assertThat(response.getSessionCookie(), is(notNullValue()));
  }

  @Test
  public void testEnabled_UserMustExist() throws Exception {
    initServer(ENABLED);

    HttpRequest request = restRequest().header("REMOTE_USER", "unknown-user").anon();
    HttpResponse response = request.subpath("rest/anything").get();
    assertResponseStatus(401, response);
    assertThat(response.getBodyText(), is(ErrorResponseGenerator.MSG_LOGIN_FAILURE_DEFAULT));
  }

  @Test
  public void testEnabled_HeaderWithValidUserDoesNotMatchSession() throws Exception {
    initServer(ENABLED);

    // explicitly call the before method, since DropWizard ignores the LogOutput configuration
    logOutput.before();

    tempEntity.newUser("Beta", "Beta", "User", "beta.user@company.com");

    HttpRequest request = restRequest().header("REMOTE_USER", "testuser").anon();
    HttpResponse response = request.subpath(UserSessionResource.RESOURCE_PATH).get();

    HttpRequest mismatchRequest =
        restRequest().cookie(response.getSessionCookie()).header("REMOTE_USER", "Beta").anon();
    HttpResponse mismatchResponse = mismatchRequest.subpath(UserSessionResource.RESOURCE_PATH).get();
    assertResponseStatus(200, mismatchResponse);
    AuthenticationStatus authenticationStatus = mismatchResponse.getBody(AuthenticationStatus.class);
    assertThat(authenticationStatus.getUsername(), is("Beta"));
    logOutput.assertInfo("Detected mismatch between user specified by reverse proxy authentication (Beta) and user specified by session cookie (testuser)");
  }
}
