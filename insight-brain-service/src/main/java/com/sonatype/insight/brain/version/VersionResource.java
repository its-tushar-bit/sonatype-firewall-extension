/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.version;

import java.util.Properties;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import com.sonatype.insight.brain.product.license.UnlicensedPath;

import com.codahale.metrics.annotation.Timed;

@Named
@Timed
@Path(VersionResource.RESOURCE_PATH)
@UnlicensedPath
public class VersionResource
{
  public static final String RESOURCE_PATH = "rest/product/version";

  private VersionService versionService;

  @Inject
  public VersionResource(VersionService versionService) {
    this.versionService = versionService;
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Properties getVersionInfo() throws Exception {
    return versionService.getProperties();
  }
}
