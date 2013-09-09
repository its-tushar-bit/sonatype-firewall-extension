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
  public static final String SERVICE_PATH = "account";

  @Inject
  public LoginResource() {
  }

  /**
   * Typical HTTP Basic Authentication.
   */
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

  @Path("status")
  @Produces(MediaType.APPLICATION_JSON)
  @GET
  public AccountStatus getStatus() {
    AccountStatus status = new AccountStatus();

    Subject subject = SecurityUtils.getSubject();
    if (subject.isAuthenticated()) {
      status.setAccount(subject.getPrincipal().toString());
    }

    return status;
  }

  public static final class AccountStatus
  {
    private String account;

    public String getAccount() {
      return account;
    }

    public void setAccount(String account) {
      this.account = account;
    }
  }
}
