/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;

import com.sonatype.insight.brain.model.security.UserPrincipal;

import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.util.ThreadContext;

/**
 * Helps to get some information about the current user.
 */
@Named
public class CurrentUser
{
  public static String SYSTEM = "system";

  public static String ANONYMOUS = "anonymous";

  static final String XFF_HEADER = "X-Forwarded-For";

  /**
   * Gets the internal name for the user associated with the calling thread.
   */
  public String getUsername() {
    UserPrincipal principal = getUserPrincipal();
    if (principal == null) {
      return ANONYMOUS;
    }
    return principal.getUsername();
  }

  /**
   * Gets the realm ID for the user associated with the calling thread.
   */
  public String getRealmId() {
    UserPrincipal principal = getUserPrincipal();
    if (principal == null) {
      return null;
    }
    return principal.getRealmId();
  }

  /**
   * Gets the UserPrincipal for the user associated with the calling thread.
   */
  public UserPrincipal getUserPrincipal() {
    return (UserPrincipal) SecurityUtils.getSubject().getPrincipal();
  }

  /**
   * Gets the internal name for the user associated with the calling thread if SecurityManager is accessible.
   * If no principal is associated with the Subject, returns 'anonymous'. If no SecurityManager is accessible, returns
   * 'system'.
   */
  public String getUsernameOrSystem() {
    // Check for Subject, not SecurityManager.
    // Since Jakarta EE migration, there's a static SecurityManager that's always non-null,
    // but system threads won't have a Subject bound to them.
    if (ThreadContext.getSubject() == null) {
      return SYSTEM;
    }
    return getUsername();
  }

  public boolean isAnonymous() {
    return getUserPrincipal() == null;
  }

  /**
   * Makes a best effort at getting the IP of the user who initiated the given request.
   */
  public String getIP(final HttpServletRequest request) {
    String ip = null;
    final String xff = request.getHeader(XFF_HEADER);
    if (xff != null && xff.length() > 0) {
      ip = resolveIP(xff.split("\\s*,\\s*"));
    }
    return ip != null ? ip : request.getRemoteAddr();
  }

  public String getDisplayNameOrUsername() {
    if (ThreadContext.getSecurityManager() == null) {
      return SYSTEM;
    }
    UserPrincipal userPrincipal = getUserPrincipal();
    if (userPrincipal == null) {
      return ANONYMOUS;
    }
    if (StringUtils.isNotBlank(userPrincipal.getDisplayName())) {
      return userPrincipal.getDisplayName();
    }
    return userPrincipal.getUsername();
  }

  static String resolveIP(final String... ips) {
    if (ips != null) {
      for (final String ip : ips) {
        final InetAddress address;
        try {
          address = InetAddress.getByName(ip);
        }
        catch (final UnknownHostException e) {
          continue;
        }
        if (address instanceof Inet4Address || address instanceof Inet6Address) {
          return ip;
        }
      }
    }
    return null;
  }
}
