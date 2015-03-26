/*
 * Copyright (c) 2011-2015 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;

import com.sonatype.insight.brain.model.security.UserPrincipal;

import org.apache.shiro.SecurityUtils;

/**
 * Helps to get some information about the current user.
 */
@Named
public class CurrentUser
{
  static final String XFF_HEADER = "X-Forwarded-For";

  /**
   * Gets the internal name for the user associated with the calling thread.
   */
  public String getUsername() {
    Object principal = SecurityUtils.getSubject().getPrincipal();
    if (principal == null) {
      return "anonymous";
    }
    return ((UserPrincipal) principal).getUsername();
  }

  public boolean isAnonymous() {
    return SecurityUtils.getSubject().getPrincipal() == null;
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
