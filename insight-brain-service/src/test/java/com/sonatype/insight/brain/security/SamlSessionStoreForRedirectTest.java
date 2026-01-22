/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import org.junit.Test;
import org.keycloak.adapters.servlet.FilterSessionStore;
import org.keycloak.adapters.spi.HttpFacade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SamlSessionStoreForRedirectTest
{
  @Test
  public void testSaveRequest_SetsTheSessionAttributeRedirectURI() {
    HttpServletRequest mockHttpServletRequest = mock(HttpServletRequest.class);
    HttpSession mockHttpSession = mock(HttpSession.class);
    when(mockHttpServletRequest.getSession(true)).thenReturn(mockHttpSession);
    String originalDestination = "http://localhost:8070/assets/index.html";

    new SamlSessionStoreForRedirect(mockHttpServletRequest, null, 0, null, null, originalDestination).saveRequest();

    verify(mockHttpSession).setAttribute(FilterSessionStore.REDIRECT_URI, originalDestination);
  }

  @Test
  public void testGetRedirectUri_RemovesDuplicateEndingForwardSlashes() {
    HttpServletRequest mockHttpServletRequest = mock(HttpServletRequest.class);
    when(mockHttpServletRequest.getRequestURL()).thenReturn(new StringBuffer("http://localhost:8070/saml"));
    when(mockHttpServletRequest.getContextPath()).thenReturn("");
    when(mockHttpServletRequest.getSession(false)).thenReturn(mock(HttpSession.class));
    HttpFacade mockHttpFacade = mock(HttpFacade.class);
    when(mockHttpFacade.getRequest()).thenReturn(mock(HttpFacade.Request.class));

    assertThat(new SamlSessionStoreForRedirect(mockHttpServletRequest, mockHttpFacade, 0, null, null,
        null).getRedirectUri()).isEqualTo("http://localhost:8070/");
  }

  @Test
  public void testGetRedirectUri_HandlesNull() {
    assertThat(new SamlSessionStoreForRedirect(mock(HttpServletRequest.class), null, 0, null, null,
        null).getRedirectUri()).isNull();
  }
}
