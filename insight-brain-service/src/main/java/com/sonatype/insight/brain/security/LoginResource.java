/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sonatype.insight.brain.product.license.UnlicensedPath;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;

/**
 * Allows an account to login and logout for access to the server
 * 
 * @since 1.7
 */
@Path(LoginResource.SERVICE_PATH)
@Named
public class LoginResource
{
  public static final String SERVICE_PATH = "rest/user";

  @Inject
  public LoginResource() {
  }

  /**
   * Typical HTTP Basic Authentication.
   */
  @UnlicensedPath
  @Path("login")
  @POST
  public void login() {
    // shiro handles all the work here
  }

  /**
   * Logout the currently logged in user
   */
  @Path("logout")
  @POST
  public void logout() {
    SecurityUtils.getSubject().logout();
  }

  /**
   * Get the status of the current account, will always return an AccountStatus
   * object, if not logged in, username will be null
   * @return AccountStatus
   */
  @UnlicensedPath
  @Path("status")
  @Produces(MediaType.APPLICATION_JSON)
  @GET
  public UserStatus getStatus() {
    return new UserStatus(SecurityUtils.getSubject());
  }

  public static final class UserStatus
  {
    private String username;
    
    public UserStatus() {
    }

    public UserStatus(Subject subject) {
      if (subject.isAuthenticated()) {
        username = subject.getPrincipal().toString();
      }
    }
    
    public void setUsername( String username ) {
      this.username = username;
    }

    public String getUsername() {
      return username;
    }
  }
}
