/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.organization;

import java.util.List;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@SuppressWarnings("serial")
class ApplicationMigrationException
    extends WebApplicationException
{
  public ApplicationMigrationException(List<String> issues) {
    super(Response.status(Response.Status.CONFLICT).entity(issues).type(MediaType.APPLICATION_JSON).build());
  }
}
