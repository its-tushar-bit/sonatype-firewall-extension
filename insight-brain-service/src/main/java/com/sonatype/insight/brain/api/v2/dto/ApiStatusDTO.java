/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto;

import jakarta.ws.rs.core.Response;

public class ApiStatusDTO
{
  public int code;

  public String message;

  public static ApiStatusDTO fromStatusType(Response.StatusType status) {
    ApiStatusDTO dto = new ApiStatusDTO();
    dto.code = status.getStatusCode();
    dto.message = status.getReasonPhrase();
    return dto;
  }
}
