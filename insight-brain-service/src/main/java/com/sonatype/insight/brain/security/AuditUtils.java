/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

import javax.servlet.http.HttpServletRequest;

import com.sonatype.insight.brain.model.security.UserPrincipal;

import org.apache.shiro.SecurityUtils;

/**
 * Helps to populate the audit log.
 * 
 * @since 1.10
 */
public final class AuditUtils
{
  public static final String XFF_HEADER = "X-Forwarded-For";

  private AuditUtils() {
  }

  public static String findUser() {
    Object principal = SecurityUtils.getSubject().getPrincipal();
    if (principal == null) {
      return "anonymous";
    }
    return ((UserPrincipal) principal).username;
  }

  public static String findIP(final HttpServletRequest request) {
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
