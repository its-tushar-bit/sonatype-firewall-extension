/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sonatype.insight.brain.version.VersionService;


/**
 * Servlet filter that adds the "Server" header to all responses.
 */
@Named
public class MultiTenantServerHeaderFilter
    extends ServerHeaderFilter
{
  @Inject
  public MultiTenantServerHeaderFilter(VersionService versionService) {
    super(versionService);
    headerValue = "NexusIQ/" + versionService.getVersion() + "-" + versionService.getBuild();
  }
}
