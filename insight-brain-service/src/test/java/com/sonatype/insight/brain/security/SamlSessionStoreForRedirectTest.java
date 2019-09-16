/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.junit.Test;
import org.keycloak.adapters.servlet.FilterSessionStore;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SamlSessionStoreForRedirectTest
{
  @Test
  public void testSaveRequest_SetsTheSessionAttributeRedirectURI() throws Exception {
    HttpServletRequest mockHttpServletRequest = mock(HttpServletRequest.class);
    String originalDestination = "http://localhost:8070/assets/index.html";
    when(mockHttpServletRequest.getHeader("Referer")).thenReturn(originalDestination);
    HttpSession mockHttpSession = mock(HttpSession.class);
    when(mockHttpServletRequest.getSession(true)).thenReturn(mockHttpSession);

    new SamlSessionStoreForRedirect(mockHttpServletRequest, null, 0, null).saveRequest();

    verify(mockHttpSession).setAttribute(FilterSessionStore.REDIRECT_URI, originalDestination);
  }
}
