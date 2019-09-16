/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.io.PrintWriter;

import javax.servlet.http.HttpServletResponse;

import com.sonatype.insight.brain.service.ErrorResponseGenerator;

import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MissingAuthenticationFilterTest
{
  @Test
  public void testOnAccessDenied_SendsAnUnauthorizedMissingCredentialsResponse() throws Exception {
    HttpServletResponse mockHttpServletResponse = mock(HttpServletResponse.class);
    PrintWriter mockPrintWriter = mock(PrintWriter.class);
    when(mockHttpServletResponse.getWriter()).thenReturn(mockPrintWriter);

    new MissingAuthenticationFilter().onAccessDenied(null, mockHttpServletResponse);

    verify(mockHttpServletResponse).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    verify(mockPrintWriter).print(ErrorResponseGenerator.MSG_MISSING_CREDENTIALS);
  }
}
