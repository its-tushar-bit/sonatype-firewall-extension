/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import com.sonatype.insight.brain.service.AbstractComponentTest;

import org.junit.Before;
import org.junit.Test;
import org.keycloak.adapters.saml.SamlPrincipal;
import org.keycloak.adapters.saml.SamlSession;
import org.keycloak.adapters.saml.profile.webbrowsersso.SamlEndpoint;
import org.keycloak.adapters.spi.HttpFacade;
import org.keycloak.adapters.spi.HttpFacade.Request;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SamlAuthenticatorForSamlEndpointTest
    extends AbstractComponentTest
{
  private SamlAuthenticatorForSamlEndpoint samlAuthenticatorForSamlEndpoint;

  @Before
  public void before() {
    HttpFacade mockHttpFacade = mock(HttpFacade.class);
    when(mockHttpFacade.getRequest()).thenReturn(mock(Request.class));
    samlAuthenticatorForSamlEndpoint = new SamlAuthenticatorForSamlEndpoint(mockHttpFacade, null, null);
  }

  @Test
  public void testCreateBrowserHandler_ReturnsSamlEndpoint() {
    assertThat(samlAuthenticatorForSamlEndpoint.createBrowserHandler(null, null, null))
        .isInstanceOf(SamlEndpoint.class);
  }

  @Test
  public void testCompleteAuthentication_CallsLoginWithSamlAuthenticationToken() {
    SamlSession mockSamlSession = mock(SamlSession.class);
    SamlPrincipal mockSamlPrincipal = mock(SamlPrincipal.class);
    when(mockSamlSession.getPrincipal()).thenReturn(mockSamlPrincipal);

    samlAuthenticatorForSamlEndpoint.completeAuthentication(mockSamlSession);

    ArgumentCaptor<SamlAuthenticationToken> samlAuthenticationTokenArgumentCaptor =
        ArgumentCaptor.forClass(SamlAuthenticationToken.class);
    verify(subject).login(samlAuthenticationTokenArgumentCaptor.capture());
    assertThat(samlAuthenticationTokenArgumentCaptor.getValue().getPrincipal()).isSameAs(mockSamlPrincipal);
  }
}
