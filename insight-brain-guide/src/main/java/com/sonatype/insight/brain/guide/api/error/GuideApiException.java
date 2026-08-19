/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.guide.api.error;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

public class GuideApiException
    extends WebApplicationException
{

  public GuideApiException(Response.Status status, String message) {
    super(message, Response.status(status).build());
  }
}
