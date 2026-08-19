/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.core.MediaType;

import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.license.model.LicensedFeature;

import inet.ipaddr.IPAddress;
import inet.ipaddr.IPAddressString;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Filters incoming requests by IP address depending on customer and system allowlist
 *
 * @since 1.152
 */
@Named
@Singleton
public class ClientIPAddressFilter
    implements Filter
{
  public static final String ACCESS_DENIED_MSG = "Access from this IP is not allowed, please contact an administrator.";

  private static final Logger log = LoggerFactory.getLogger(ClientIPAddressFilter.class);

  private final CurrentUser currentUser;

  private final Configuration configuration;

  private final InsightConfig insightConfig;

  private final ProductLicense productLicense;

  @Inject
  public ClientIPAddressFilter(
      CurrentUser currentUser,
      Configuration configuration,
      InsightConfig insightConfig,
      ProductLicense productLicense)
  {
    this.currentUser = currentUser;
    this.configuration = configuration;
    this.insightConfig = insightConfig;
    this.productLicense = productLicense;
  }

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
    // no op
  }

  @Override
  public void doFilter(
      ServletRequest request,
      ServletResponse response,
      FilterChain chain) throws IOException, ServletException
  {
    HttpServletRequest httpRequest = (HttpServletRequest) request;
    HttpServletResponse httpResponse = (HttpServletResponse) response;

    if (!productLicense.hasFeature(LicensedFeature.IP_ALLOWLIST)) {
      chain.doFilter(request, response);
      return;
    }

    List<AllowedIp> allowlist = Stream.concat(configuration.getAccessAllowlist().stream(),
        insightConfig.getSystemAllowlist().stream()).collect(Collectors.toList());
    if (allowlist.isEmpty()) {
      // If the allowlist is not configured allow un-filtered access
      chain.doFilter(request, response);
      return;
    }

    String currentUserIp = currentUser.getIP(httpRequest);
    // InetAddress.getByName can take an IP address literal, getHostAddress returns a normal IP address
    String requestHostAddr = InetAddress.getByName(currentUserIp).getHostAddress();
    IPAddress currentUserAddr = new IPAddressString(requestHostAddr).getAddress();
    if (allowlist.stream().anyMatch(allowedIp -> isAddressInRange(allowedIp, currentUserAddr))) {
      chain.doFilter(request, response);
      return;
    }

    if (isRequestFromLocalhost(httpRequest)) {
      chain.doFilter(request, response);
      return;
    }

    log.warn("Rejecting request from {} as the IP was not found in the configured allowlist", currentUserIp);
    httpResponse.setStatus(HttpServletResponse.SC_FORBIDDEN);
    httpResponse.setContentType(MediaType.TEXT_PLAIN);
    try (PrintWriter writer = httpResponse.getWriter()) {
      writer.print(ACCESS_DENIED_MSG);
    }
  }

  private boolean isAddressInRange(AllowedIp allowlistIP, IPAddress requestIPAddress) {
    String rawIpAddress = allowlistIP.getIpAddress();
    if (StringUtils.isNotEmpty(rawIpAddress)) {
      IPAddress ipAddress = new IPAddressString(allowlistIP.getIpAddress()).getAddress();
      if (ipAddress != null) {
        return ipAddress.contains(requestIPAddress);
      }
    }

    log.error("Invalid IP Address in Allowlist: {} {}", rawIpAddress, allowlistIP.getDescription());
    return false;
  }

  private boolean isRequestFromLocalhost(HttpServletRequest request) throws IOException {
    InetAddress addr = InetAddress.getByName(request.getRemoteAddr());
    return addr.isLoopbackAddress();
  }

  @Override
  public void destroy() {
    // no op
  }
}
