package com.sonatype.insight.brain.security;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import com.sonatype.insight.error.exception.BadRequestException;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.codec.Base64;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

  private static final Logger log = LoggerFactory.getLogger(LoginResource.class);

  @Inject
  public LoginResource() {
  }

  /**
   * Logs in a user, using credentials stored in base64 in the Authorization header
   * Typical HTTP Basic Authentication.
   */
  @Path("login")
  @GET
  public Response login(@HeaderParam("Authorization") String authHeader) {
    if (!StringUtils.hasLength(authHeader)) {
      throw new BadRequestException("Authorization header not provided!");
    }

    // Dump the Basic part, we only want the user/pass
    String decoded = Base64.decodeToString(authHeader).substring("Basic ".length());
    String a = decoded.substring(0, decoded.indexOf(":"));
    String p = decoded.substring(a.length() + 1);

    Subject subject = SecurityUtils.getSubject();

    try {
      if (!subject.isAuthenticated()) {
        subject.login(new UsernamePasswordToken(a, p));
      }

      log.debug("account {} passed login", a);

      return Response.ok().build();
    }
    catch (AuthenticationException e) {
      log.info("account {} failed login", a);
      log.debug("authentication exception:", e);
      // we don't want to send any info back about why the login request failed
      return Response.status(401).build();
    }
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
}
