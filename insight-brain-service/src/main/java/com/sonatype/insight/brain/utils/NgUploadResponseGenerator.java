/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.utils;

import java.util.concurrent.Callable;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.security.AntiCsrfFilter;
import com.sonatype.insight.jaxrs.error.ErrorResponse;
import com.sonatype.insight.jaxrs.error.ErrorResponseGenerator;
import com.sonatype.insight.json.store.JsonUtils;

/**
 * A utility to generate a Json response for Ajax form uploads with the ability to fallback to ngUpload's
 * iFrame implementation for IE9. IE9 requests must be returned as TEXT_PLAIN or else IE will attempt to open
 * the response in a new tab. See @{link https://github.com/twilson63/ngUpload#some-rule-of-thumb} - Working in IE.
 *
 * @since 1.19.0
 */
@Named
public class NgUploadResponseGenerator
{
  private final ErrorResponseGenerator errorResponseGenerator;

  private final AntiCsrfFilter antiCsrfFilter;

  @Inject
  public NgUploadResponseGenerator(ErrorResponseGenerator errorResponseGenerator, AntiCsrfFilter antiCsrfFilter) {
    this.errorResponseGenerator = errorResponseGenerator;
    this.antiCsrfFilter = antiCsrfFilter;
  }

  /**
   * Generates a response for Ajax and ngUpload iFrame posts. Callable can return null which will be deserialized by
   * ngUpload into empty content.
   */
  public Response run(
      String csrfToken,
      HttpHeaders httpHeaders,
      boolean noFormData,
      Callable<?> apply) throws Exception
  {
    try {
      antiCsrfFilter.validate(csrfToken, httpHeaders);
      Object result = apply.call();
      if (noFormData) {
        return Response.ok(JsonUtils.format(result), MediaType.TEXT_PLAIN).build();
      }
      else {
        if (result != null) {
          return Response.ok(result, MediaType.APPLICATION_JSON).build();
        }
        else {
          return Response.ok().build();
        }
      }
    }
    catch (Exception e) {
      if (noFormData) {
        ErrorResponse errorResponse = errorResponseGenerator.mapException(e);
        AuditData.get().setHttpStatus(errorResponse.getStatusCode());
        return Response.ok(JsonUtils.format(errorResponse.getMessageBody()), MediaType.TEXT_PLAIN).build();
      }
      throw e;
    }
  }
}
