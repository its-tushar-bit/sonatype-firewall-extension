/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.organization;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import com.sonatype.insight.brain.api.v2.dto.ApiMoveApplicationResponseDTOV2;

@SuppressWarnings("serial")
class ApplicationMoveException
    extends WebApplicationException
{
  public ApplicationMoveException(ApiMoveApplicationResponseDTOV2 dto) {
    super(Response.status(Response.Status.CONFLICT).entity(dto).type(MediaType.APPLICATION_JSON).build());
  }
}
