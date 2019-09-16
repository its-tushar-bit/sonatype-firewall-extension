/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import org.junit.Test;
import org.keycloak.adapters.saml.profile.webbrowsersso.BrowserHandler;
import org.keycloak.adapters.spi.HttpFacade;
import org.keycloak.adapters.spi.HttpFacade.Request;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SamlAuthenticatorForNonSamlEndpointTest
{
  @Test
  public void testCreateBrowserHandler_ReturnsBrowserHandler() {
    HttpFacade mockHttpFacade = mock(HttpFacade.class);
    when(mockHttpFacade.getRequest()).thenReturn(mock(Request.class));

    assertThat(new SamlAuthenticatorForNonSamlEndpoint(mockHttpFacade, null, null)
        .createBrowserHandler(null, null, null)).isInstanceOf(BrowserHandler.class);
  }
}
