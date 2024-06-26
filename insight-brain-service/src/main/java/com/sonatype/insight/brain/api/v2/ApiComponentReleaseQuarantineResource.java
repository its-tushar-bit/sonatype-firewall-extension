/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentReleasedFromQuarantineDTO;
import com.sonatype.insight.brain.api.v2.service.ApiComponentReleaseQuarantineService;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;
import com.sonatype.insight.brain.product.license.ProductLicenseEnforcementPoint;
import com.sonatype.insight.license.model.LicensedFeature;

import com.codahale.metrics.annotation.Timed;

/**
 * @since 1.78
 */
@Named
@Timed
@Path(PublicApiPaths.COMPONENT_QUARANTINE_RELEASE_PATH_V2)
@ProductLicenseEnforcementPoint(LicensedFeature.FIREWALL)
public class ApiComponentReleaseQuarantineResource
{
  private final ApiComponentReleaseQuarantineService componentReleaseQuarantineServiceV2;

  @Inject
  public ApiComponentReleaseQuarantineResource(
      final ApiComponentReleaseQuarantineService componentReleaseQuarantineServiceV2)
  {
    this.componentReleaseQuarantineServiceV2 = componentReleaseQuarantineServiceV2;
  }

  @POST
  @Consumes(MediaType.TEXT_PLAIN)
  @Produces(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.RELEASE_QUARANTINE)
  public ApiComponentReleasedFromQuarantineDTO releaseQuarantineWithoutReEval(
      @PathParam("quarantineId") final String quarantineId,
      final String comment)
  {
    return componentReleaseQuarantineServiceV2.releaseQuarantineWithoutReEval(quarantineId, comment);
  }
}
