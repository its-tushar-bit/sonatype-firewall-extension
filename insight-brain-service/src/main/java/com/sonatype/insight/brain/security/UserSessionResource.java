/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sonatype.insight.brain.product.license.UnlicensedPath;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;

/**
 * Manages user account authentication sessions which provide access to the server.
 * 
 * @since 1.7
 */
@Path(UserSessionResource.SERVICE_PATH)
@UnlicensedPath
@Named
public class UserSessionResource
{
  public static final String SERVICE_PATH = "rest/user/session";

  @Inject
  public UserSessionResource() {
  }

  /**
   * Typical HTTP Basic Authentication.
   */
  @POST
  public void login() {
    // Shiro handles all the work here.
  }

  /**
   * Logout the currently logged in user
   */
  @DELETE
  public void logout() {
    SecurityUtils.getSubject().logout();
  }

  /**
   * Get the authentication status of the current account.
   * 
   * The REST implementation uses a sub-resource (path) in order to allow anonymous access.  This is due to Shiro not 
   * having authentication based on specified HTTP methods.
   * 
   * For background see the discussion on list [1] which called for an improvement to configure authentication for 
   * specific http methods.  As a result, SHIRO-200 [2] was filed with a patch supplied.  It is not resolved due to an 
   * open question on an appropriate way to configure http method for arbitrary filters.
   * 
   * [1] http://shiro-developer.582600.n2.nabble.com/HTTP-method-dependent-Basic-authentication-td5635284.html
   * [2] https://issues.apache.org/jira/browse/SHIRO-200
   */
  @Path("status")
  @Produces(MediaType.APPLICATION_JSON)
  @GET
  public AuthenticationStatus getStatus() {
    return AuthenticationStatus.fromSubject(SecurityUtils.getSubject());
  }

  /**
   * The authentication status of an account.
   * 
   * @since 1.7
   */
  public static final class AuthenticationStatus
  {
    private String username;
    
    private boolean isAuthenticated;
    
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
      
      // Supply username if it's available.  Will be useful when a user is remembered but not authenticated.
      if(subject.getPrincipal() != null) {
        status.setUsername(subject.getPrincipal().toString());
      }

      return status;
    }
    
    public void setUsername(String username) {
      this.username = username;
    }

    public String getUsername() {
      return username;
    }

    public boolean isAuthenticated() {
      return isAuthenticated;
    }

    public void setAuthenticated(boolean isAuthenticated) {
      this.isAuthenticated = isAuthenticated;
    }
  }
}
