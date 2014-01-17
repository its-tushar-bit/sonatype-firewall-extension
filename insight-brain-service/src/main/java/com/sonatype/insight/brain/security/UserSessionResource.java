/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
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

import com.sonatype.insight.brain.model.security.UserPrincipal;
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
   */
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

    private String displayName;
    
    private boolean isAuthenticated;
    
    private boolean isClmUser;

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
        Object principal = subject.getPrincipal();
        if (principal instanceof UserPrincipal) {
          status.setUsername(((UserPrincipal) principal).username);
          status.setDisplayName(((UserPrincipal)principal).displayName);
          status.setClmUser(((UserPrincipal) principal).clmUser);
        } else {
          status.setUsername(subject.getPrincipal().toString());
        }
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

    public boolean isClmUser() {
      return isClmUser;
    }

    public void setClmUser(boolean isClmUser) {
      this.isClmUser = isClmUser;
    }
  }
}
