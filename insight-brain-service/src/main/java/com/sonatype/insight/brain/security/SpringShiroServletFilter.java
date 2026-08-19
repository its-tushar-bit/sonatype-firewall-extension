/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import org.apache.shiro.web.filter.mgt.FilterChainResolver;
import org.apache.shiro.web.mgt.DefaultWebSecurityManager;
import org.apache.shiro.web.servlet.AbstractShiroFilter;

/**
 * Shiro servlet filter used by Spring-managed servlet registrations.
 */
public class SpringShiroServletFilter
    extends AbstractShiroFilter
{
  public SpringShiroServletFilter(
      DefaultWebSecurityManager securityManager,
      FilterChainResolver filterChainResolver)
  {
    setSecurityManager(securityManager);
    setFilterChainResolver(filterChainResolver);
  }
}
