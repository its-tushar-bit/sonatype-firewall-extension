/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.about;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import com.sonatype.insight.brain.product.license.UnlicensedPath;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Path(AboutResource.SERVICE_PATH)
@UnlicensedPath
public class AboutResource
{
  public static final String SERVICE_PATH = "about";

  private final AboutService aboutService;

  @Inject
  public AboutResource(AboutService aboutService) {
    this.aboutService = aboutService;
  }

  @GET
  public Response home() {
    return Response.seeOther(aboutService.getDestination()).build();
  }
}