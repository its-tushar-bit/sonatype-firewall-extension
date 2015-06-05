/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.shiro.web.filter.authc.BasicHttpAuthenticationFilter;

/**
 * Specialized BASIC auth filter that ignores session cookies and instead requires a valid authc header on each request.
 * To be used for the public REST API (stateless).
 */
public class BasicHttpAuthenticationMandatoryFilter
    extends BasicHttpAuthenticationFilter
{
  @Override
  protected boolean isAccessAllowed(ServletRequest request, ServletResponse response, Object mappedValue) {
    return false;
  }
}
