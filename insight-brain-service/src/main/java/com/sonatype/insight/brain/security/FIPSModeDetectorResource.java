/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import jakarta.inject.Named;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import com.sonatype.insight.brain.product.license.UnlicensedPath;

import com.codahale.metrics.annotation.Timed;

@Named
@Timed
@Path(FIPSModeDetectorResource.RESOURCE_PATH)
@UnlicensedPath
public class FIPSModeDetectorResource
{
  public static final String RESOURCE_PATH = "rest/security/fipsMode";

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public FIPSModeStatus getFIPSModeStatus() {
    boolean isEnabled = FIPSModeDetector.isEnabled();
    return new FIPSModeStatus(isEnabled);
  }

  public record FIPSModeStatus(boolean enabled) {}
}
