/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.net.URI;

import com.sonatype.insight.brain.landing.LandingService;

import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.saml2.core.Saml2ParameterNames;
import org.springframework.security.saml2.provider.service.authentication.OpenSaml5AuthenticationProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SpringSamlAuthenticatingFilterTest
{
  private static final URI DEFAULT_LANDING = URI.create("https://iq.example.com/");

  private static final URI GUIDE_LANDING = URI.create("https://iq.example.com/guide/");

  @BeforeClass
  public static void initOpenSaml() {
    // Constructing the filter builds an OpenSaml5 request resolver/converter, which needs OpenSAML bootstrapped.
    new OpenSaml5AuthenticationProvider();
  }

  private SpringSamlAuthenticatingFilter newFilter() {
    LandingService landingService = mock(LandingService.class);
    when(landingService.getDestination()).thenReturn(DEFAULT_LANDING);
    when(landingService.getGuideDestination()).thenReturn(GUIDE_LANDING);
    return new SpringSamlAuthenticatingFilter(
        mock(SamlRelyingPartyRegistrationResolver.class),
        landingService,
        mock(SamlConfigurationCache.class),
        mock(EncryptionKeyStore.class));
  }

  private String resolve(String relayState) {
    MockHttpServletRequest request = new MockHttpServletRequest();
    if (relayState != null) {
      request.setParameter(Saml2ParameterNames.RELAY_STATE, relayState);
    }
    return newFilter().resolveDestination(request);
  }

  @Test
  public void defaultsToTrustedLandingWithoutRelayState() {
    String destination = resolve(null);
    assertThat(destination).startsWith("https://iq.example.com/");
    assertThat(URI.create(destination).getHost()).isEqualTo("iq.example.com");
  }

  @Test
  public void routesToGuideDestinationForGuideOrigin() {
    String destination = resolve("guide|#/dashboard");
    assertThat(destination).startsWith("https://iq.example.com/guide/");
    assertThat(destination).contains("dashboard");
  }

  @Test
  public void tamperedOriginCannotChangeRedirectHost() {
    // A crafted RelayState origin must not become the redirect host (open-redirect defense): only "guide"
    // selects the guide landing, anything else falls back to the trusted default landing.
    String destination = resolve("https://evil.example.org|#/x");
    assertThat(URI.create(destination).getHost()).isEqualTo("iq.example.com");
    assertThat(destination).doesNotContain("evil.example.org");
  }

  @Test
  public void hashCannotEscapeTrustedOrigin() {
    // A hash crafted to look like a host stays in the fragment of the trusted origin.
    String destination = resolve("|#//evil.example.org/path");
    assertThat(URI.create(destination).getHost()).isEqualTo("iq.example.com");
  }

  @Test
  public void accessToSamlEndpointIsDeniedWithoutWritingToResponse() {
    // isAccessAllowed must be a pure query for /saml: it returns false so onAccessDenied handles the request
    // (challenge, or home-redirect for an authenticated user). Redirecting here as well would commit the
    // response and then let onAccessDenied redirect again on the committed response -> IllegalStateException.
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/saml");
    request.setRequestURI("/saml");
    MockHttpServletResponse response = new MockHttpServletResponse();

    boolean allowed = newFilter().isAccessAllowed(request, response, null);

    assertThat(allowed).isFalse();
    assertThat(response.isCommitted()).isFalse();
    assertThat(response.getRedirectedUrl()).isNull();
  }
}
