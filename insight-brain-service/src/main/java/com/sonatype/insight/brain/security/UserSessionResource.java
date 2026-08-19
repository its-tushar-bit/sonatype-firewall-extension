/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.net.URI;
import java.util.Set;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import com.sonatype.insight.brain.model.configuration.ReverseProxyAuthenticationConfiguration;
import com.sonatype.insight.brain.model.security.UserPrincipal;
import com.sonatype.insight.brain.product.license.UnlicensedPath;
import com.sonatype.insight.brain.service.Configuration;

import com.codahale.metrics.annotation.Timed;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.session.Session;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.web.session.mgt.DefaultWebSessionManager;

/**
 * Manages user account authentication sessions which provide access to the server.
 *
 * @since 1.7
 */
@Path(UserSessionResource.RESOURCE_PATH)
@UnlicensedPath
@Named
@Singleton
@Timed
public class UserSessionResource
{
  public static final String RESOURCE_PATH = "rest/user/session";

  public static final String LOGOUT_PATH = "logout";

  private final Configuration configuration;

  private final IdPLogoutUrlBuilder idPLogoutUrlBuilder;

  private final DefaultWebSessionManager defaultWebSessionManager;

  @Inject
  public UserSessionResource(
      Configuration configuration,
      IdPLogoutUrlBuilder idPLogoutUrlBuilder,
      DefaultWebSessionManager defaultWebSessionManager)
  {
    this.configuration = configuration;
    this.idPLogoutUrlBuilder = idPLogoutUrlBuilder;
    this.defaultWebSessionManager = defaultWebSessionManager;
  }

  /**
   * Typical HTTP Basic Authentication.
   */
  @POST
  public void login() {
    // Shiro handles all the work here.
  }

  /**
   * Logout the currently logged in user.
   *
   * Consciously not doing this in a RESTful manner. The use of a separate resource path is because we want to allow
   * anonymous access but Shiro doesn't have the capability to apply different authc per HTTP method. See
   * https://issues.apache.org/jira/browse/SHIRO-200 .
   */
  @DELETE
  @Path(LOGOUT_PATH)
  public Response logout() {
    SecurityUtils.getSubject().logout();
    ReverseProxyAuthenticationConfiguration reverseProxyAuthenticationConfiguration =
        configuration.getReverseProxyAuthenticationConfiguration();
    if (reverseProxyAuthenticationConfiguration != null &&
        reverseProxyAuthenticationConfiguration.isEnabled() &&
        reverseProxyAuthenticationConfiguration.getLogoutUrl() != null)
    {
      return Response.status(Status.NO_CONTENT)
          .location(URI.create(reverseProxyAuthenticationConfiguration.getLogoutUrl()))
          .build();
    }

    URI idpLogoutURI = idPLogoutUrlBuilder.buildIdPLogoutUrl();

    if (idpLogoutURI != null) {
      return Response.status(Status.NO_CONTENT).location(idpLogoutURI).build();
    }

    return Response.status(Status.NO_CONTENT).build();
  }

  /**
   * Get the authentication status of the current account.
   */
  @Produces(MediaType.APPLICATION_JSON)
  @GET
  public AuthenticationStatus getStatus() {
    AuthenticationStatus authenticationStatus = AuthenticationStatus.fromSubject(SecurityUtils.getSubject());
    if (authenticationStatus.sessionTimeoutMilliseconds == null) {
      authenticationStatus.setSessionTimeoutMilliseconds(defaultWebSessionManager.getGlobalSessionTimeout());
    }
    return authenticationStatus;
  }

  /**
   * The authentication status of an account.
   *
   * @since 1.7
   */
  public static final class AuthenticationStatus
  {
    private String username;

    private String displayName;

    private boolean isAuthenticated;

    private boolean isInternalUser;

    private Set<String> groups;

    private Long sessionTimeoutMilliseconds;

    /**
     * Status for a user that is not authenticated.
     */
    public AuthenticationStatus() {
    }

    /**
     * Create a status based on the {@link Subject}.
     *
     * If the user is not authenticated {@link #isAuthenticated()} will be false; {@link #getUsername()} may be null.
     */
    public static AuthenticationStatus fromSubject(Subject subject) {
      AuthenticationStatus status = new AuthenticationStatus();
      status.setAuthenticated(subject.isAuthenticated());

      // Supply username if it's available. Will be useful when a user is remembered but not authenticated.
      if (subject.getPrincipal() != null) {
        Object principal = subject.getPrincipal();
        if (principal instanceof UserPrincipal) {
          status.setUsername(((UserPrincipal) principal).getUsername());
          status.setDisplayName(((UserPrincipal) principal).getDisplayName());
          status.setInternalUser(InternalRealm.ID.equals(((UserPrincipal) principal).getRealmId()));
          status.setGroups(((UserPrincipal) principal).getMembership());
        }
        else {
          status.setUsername(subject.getPrincipal().toString());
        }
      }

      Session session = subject.getSession(false);
      if (session != null) {
        status.sessionTimeoutMilliseconds = session.getTimeout();
      }

      return status;
    }

    public void setUsername(String username) {
      this.username = username;
    }

    public String getUsername() {
      return username;
    }

    public String getDisplayName() {
      return displayName;
    }

    public void setDisplayName(final String displayName) {
      this.displayName = displayName;
    }

    public boolean isAuthenticated() {
      return isAuthenticated;
    }

    public void setAuthenticated(boolean isAuthenticated) {
      this.isAuthenticated = isAuthenticated;
    }

    public boolean isInternalUser() {
      return isInternalUser;
    }

    public void setInternalUser(boolean isInternalUser) {
      this.isInternalUser = isInternalUser;
    }

    public Set<String> getGroups() {
      return groups;
    }

    public void setGroups(Set<String> groups) {
      this.groups = groups;
    }

    public Long getSessionTimeoutMilliseconds() {
      return sessionTimeoutMilliseconds;
    }

    public void setSessionTimeoutMilliseconds(Long sessionTimeoutMilliseconds) {
      this.sessionTimeoutMilliseconds = sessionTimeoutMilliseconds;
    }
  }
}
