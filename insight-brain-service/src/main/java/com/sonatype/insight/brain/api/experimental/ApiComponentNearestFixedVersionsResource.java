/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.experimental;

import java.util.List;
import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sonatype.clm.dto.model.component.ComponentNearestFixedVersions;
import com.sonatype.insight.brain.api.experimental.doc.ApiComponentNearestFixedVersionsResourceDoc;

import com.codahale.metrics.annotation.Timed;

/**
 * @since 1.144.0
 */
@Named
@Timed
@Path(ApiComponentNearestFixedVersionsResource.RESOURCE_PATH)
public class ApiComponentNearestFixedVersionsResource
    implements ApiComponentNearestFixedVersionsResourceDoc
{
  static final String RESOURCE_PATH = "/api/experimental/components/vulnerabilities/nearestFixedVersions";

  private final ApiComponentNearestFixedVersionsService service;

  @Inject
  public ApiComponentNearestFixedVersionsResource(ApiComponentNearestFixedVersionsService service) {
    this.service = service;
  }

  @Override
  @POST
  @Consumes({MediaType.APPLICATION_JSON})
  @Produces({MediaType.APPLICATION_JSON})
  public List<ComponentNearestFixedVersions> getNearestFixedVersions(
      ApiComponentNearestFixedVersionsRequestListDto listDto)
  {
    return service.getNearestFixedVersions(listDto);
  }
}
