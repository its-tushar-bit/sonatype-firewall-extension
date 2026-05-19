/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import com.sonatype.insight.brain.service.AbstractComponentTest;

import org.apache.shiro.session.mgt.SimpleSession;
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
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SamlAuthenticatorForSamlEndpointTest
    extends AbstractComponentTest
{
  private SamlAuthenticatorForSamlEndpoint samlAuthenticatorForSamlEndpoint;

  private SamlSessionStoreForRedirect mockSessionStore;

  @Before
  public void before() {
    HttpFacade mockHttpFacade = mock(HttpFacade.class);
    when(mockHttpFacade.getRequest()).thenReturn(mock(Request.class));
    mockSessionStore = mock(SamlSessionStoreForRedirect.class);
    samlAuthenticatorForSamlEndpoint = new SamlAuthenticatorForSamlEndpoint(mockHttpFacade, null, mockSessionStore);
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

  @Test
  public void testCompleteAuthentication_restoresPreLoginSessionAttributes() {
    SimpleSession oldSession = spy(new SimpleSession());
    oldSession.setAttribute("redirect_uri", "/dashboard");
    SimpleSession newSession = new SimpleSession();
    when(subject.getSession(false)).thenReturn(oldSession);
    when(subject.getSession()).thenReturn(newSession);

    SamlSession mockSamlSession = mock(SamlSession.class);
    when(mockSamlSession.getPrincipal()).thenReturn(mock(SamlPrincipal.class));

    samlAuthenticatorForSamlEndpoint.completeAuthentication(mockSamlSession);

    verify(oldSession).stop();
    verify(mockSessionStore).refreshCachedSession();
    assertThat(newSession.getAttribute("redirect_uri")).isEqualTo("/dashboard");
  }

  @Test
  public void testCompleteAuthentication_doesNotOverwriteShiroLoginAttributes() {
    SimpleSession oldSession = spy(new SimpleSession());
    oldSession.setAttribute("shiro_attr", "old_value");
    SimpleSession newSession = new SimpleSession();
    newSession.setAttribute("shiro_attr", "login_value");
    when(subject.getSession(false)).thenReturn(oldSession);
    when(subject.getSession()).thenReturn(newSession);

    SamlSession mockSamlSession = mock(SamlSession.class);
    when(mockSamlSession.getPrincipal()).thenReturn(mock(SamlPrincipal.class));

    samlAuthenticatorForSamlEndpoint.completeAuthentication(mockSamlSession);

    verify(oldSession).stop();
    assertThat(newSession.getAttribute("shiro_attr")).isEqualTo("login_value");
  }

  @Test
  public void testCompleteAuthentication_doesNotRestoreAttributesForDifferentPrincipal() {
    when(subject.getPrincipal()).thenReturn("user-a").thenReturn("user-b");

    SimpleSession oldSession = spy(new SimpleSession());
    oldSession.setAttribute("redirect_uri", "/dashboard");
    SimpleSession newSession = new SimpleSession();
    when(subject.getSession(false)).thenReturn(oldSession);

    SamlSession mockSamlSession = mock(SamlSession.class);
    when(mockSamlSession.getPrincipal()).thenReturn(mock(SamlPrincipal.class));

    samlAuthenticatorForSamlEndpoint.completeAuthentication(mockSamlSession);

    verify(oldSession).stop();
    assertThat(newSession.getAttribute("redirect_uri")).isNull();
  }
}
