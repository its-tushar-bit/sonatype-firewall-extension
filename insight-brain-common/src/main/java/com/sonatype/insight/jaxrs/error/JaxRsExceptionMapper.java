/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

// Vendored/copied from hosted-data-services/insight-jaxrs-utils
package com.sonatype.insight.jaxrs.error;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

/**
 * Ensures that any exceptions internally created by the JAX-RS container have a plain text body with the error message
 * like we do in the {@link ErrorResponseGenerator}.
 * 
 * If the mapped exception is an instance of java.lang.Error or was caused by an instance of java.lang.Error, then
 * it terminates the JVM. This can be disabled by setting the exitOnFatalError flag.
 */
@Named
@Singleton
public class JaxRsExceptionMapper
    implements ExceptionMapper<Throwable>
{
  private final ErrorResponseGenerator errorResponseGenerator;

  private final JavaLangErrorHandler javaLangErrorHandler;

  public JaxRsExceptionMapper() {
    this(new ErrorResponseGenerator(), new JavaLangErrorHandler());
  }

  @Inject
  public JaxRsExceptionMapper(ErrorResponseGenerator errorResponseGenerator,
                              JavaLangErrorHandler javaLangErrorHandler)
  {
    this.errorResponseGenerator = errorResponseGenerator;
    this.javaLangErrorHandler = javaLangErrorHandler;
  }

  @Override
  public Response toResponse(final Throwable exception) {
    javaLangErrorHandler.handle(exception);

    ErrorResponse response = errorResponseGenerator.mapExceptionAndLog(exception);

    return Response.status(response.getStatusCode()).type(ErrorResponse.CONTENT_TYPE).entity(response.getMessageBody())
        .build();
  }
}
