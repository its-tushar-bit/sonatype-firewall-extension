package com.sonatype.insight.brain.security;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

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
   * Logs in a user, using credentials stored in base64 in the Authorization header
   * Typical HTTP Basic Authentication.
   */
  @Path("login")
  @GET
  public Response login() {
    return Response.ok().build();
  }

  /**
   * Logout the currently logged in user
   */
  @Path("logout")
  @GET
  public Response logout() {
    Subject subject = SecurityUtils.getSubject();

    subject.logout();

    return Response.ok().build();
  }

  @Path("status")
  @Produces(MediaType.APPLICATION_JSON)
  @GET
  public AccountStatus getStatus() {
    Subject subject = SecurityUtils.getSubject();
    AccountStatus status = new AccountStatus();
    status.setAccount(subject.getPrincipal().toString());
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
