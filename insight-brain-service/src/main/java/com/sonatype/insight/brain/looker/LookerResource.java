/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.looker;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;

import com.codahale.metrics.annotation.Timed;

@Named
@Timed
@Path(LookerResource.RESOURCE_PATH)
public class LookerResource
{
  public static final String RESOURCE_PATH = "rest/looker";

  public static final String SSO_EMBED_URL_PATH = "ssoEmbedUrl";

  public static final String CONFIG_PATH = "config";

  private final LookerService lookerHdsService;

  @Inject
  public LookerResource(final LookerService lookerHdsService) {
    this.lookerHdsService = lookerHdsService;
  }

  @POST
  @Path(SSO_EMBED_URL_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.VIEW_LOOKER_DASHBOARD)
  public SSOEmbedUrlDTO createSSOEmbedUrl(LookerDashboardDTO lookerDashboardDTO) {
    return lookerHdsService.createSSOEmbedUrl(lookerDashboardDTO);
  }

  @GET
  @Path(CONFIG_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  public LookerConfigDTO getLookerConfig() {
    return lookerHdsService.getLookerConfig();
  }
}
