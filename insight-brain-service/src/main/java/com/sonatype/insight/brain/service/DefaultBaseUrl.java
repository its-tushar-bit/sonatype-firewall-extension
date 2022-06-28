/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.UriBuilder;

import com.sonatype.insight.brain.api.v2.service.ApiConfigurationService;
import com.sonatype.insight.brain.api.v2.service.ConfigurationListener;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;

import com.google.common.collect.ImmutableSet;

import static org.apache.commons.lang3.StringUtils.isBlank;

@Named
@Singleton
public class DefaultBaseUrl
    implements BaseUrl, ConfigurationListener
{
  public static final String ERR_MSG_BASE_URL_NOT_CONFIGURED = "The server base URL (baseUrl) is not configured. "
      + "More information at https://links.sonatype.com/products/clm/docs/base-url";

  private final ApiConfigurationService configurationService;

  private final AtomicReference<Map<String, Object>> baseUrlConfigurationAtomicReference =
      new AtomicReference<>();

  private final ThreadLocal<HttpServletRequest> currentHttpRequest = new ThreadLocal<>();

  @Inject
  public DefaultBaseUrl(final ApiConfigurationService configurationService) {
    this.configurationService = configurationService;
    configurationChanged(
        new HashSet<>(Arrays.asList(SystemConfigurationProperty.BASE_URL, SystemConfigurationProperty.FORCE_BASE_URL)));
  }

  @Override
  public void capture(HttpServletRequest httpRequest) {
    currentHttpRequest.set(httpRequest);
  }

  @Override
  public void release() {
    currentHttpRequest.remove();
  }

  private HttpServletRequest getHttpRequest() {
    HttpServletRequest httpRequest = currentHttpRequest.get();
    if (httpRequest == null) {
      throw new IllegalStateException("Not inside a request");
    }
    return httpRequest;
  }

  @Override
  public String get() {
    Map<String, Object> baseUrlConfiguration = baseUrlConfigurationAtomicReference.get();
    if (!(boolean) baseUrlConfiguration.get(SystemConfigurationProperty.FORCE_BASE_URL)) {
      String url = tryGetBaseUriWithEndingForwardSlash();
      if (url != null) {
        return url;
      }
    }
    return getConfigured(baseUrlConfiguration);
  }

  @Override
  public String getConfigured() {
    return getConfigured(baseUrlConfigurationAtomicReference.get());
  }

  private String getConfigured(Map<String, Object> baseUrlConfiguration) {
    String url = (String) baseUrlConfiguration.get(SystemConfigurationProperty.BASE_URL);
    if (!isBlank(url)) {
      return url;
    }
    throw new IllegalStateException(ERR_MSG_BASE_URL_NOT_CONFIGURED);
  }

  private String tryGetBaseUriWithEndingForwardSlash() {
    try {
      HttpServletRequest httpRequest = getHttpRequest();
      StringBuffer requestUrl = httpRequest.getRequestURL();
      String requestUri = httpRequest.getRequestURI();
      String contextPath = httpRequest.getContextPath();
      String url = requestUrl.substring(0, requestUrl.length() - requestUri.length() + contextPath.length());
      if (!url.endsWith("/")) {
        url += '/';
      }
      return url;
    }
    catch (IllegalStateException e) {
      // no request in scope
      return null;
    }
  }

  @Override
  public UriBuilder redirect() {
    return UriBuilder.fromUri(get()).replaceQuery(getHttpRequest().getQueryString());
  }

  @Override
  public void configurationChanged(Set<String> propertyNames) {
    if (propertyNames.contains(SystemConfigurationProperty.BASE_URL) ||
        propertyNames.contains(SystemConfigurationProperty.FORCE_BASE_URL)) {
      baseUrlConfigurationAtomicReference.set(configurationService.getConfigurationNoAuthz(
          ImmutableSet.of(SystemConfigurationProperty.BASE_URL, SystemConfigurationProperty.FORCE_BASE_URL)));
    }
  }
}
