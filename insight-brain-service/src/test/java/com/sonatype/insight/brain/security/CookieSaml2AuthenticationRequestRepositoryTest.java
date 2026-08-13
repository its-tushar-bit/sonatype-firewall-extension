/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import jakarta.servlet.http.Cookie;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.saml2.provider.service.authentication.AbstractSaml2AuthenticationRequest;
import org.springframework.security.saml2.provider.service.authentication.Saml2PostAuthenticationRequest;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration;

import static org.assertj.core.api.Assertions.assertThat;

public class CookieSaml2AuthenticationRequestRepositoryTest
{
  private static final String SECRET = "shared-encryption-key";

  private final CookieSaml2AuthenticationRequestRepository repository =
      new CookieSaml2AuthenticationRequestRepository(() -> SECRET);

  @Test
  public void savedRequestRoundTripsThroughSignedCookie() {
    AbstractSaml2AuthenticationRequest saved = authenticationRequest();

    MockHttpServletResponse response = new MockHttpServletResponse();
    repository.saveAuthenticationRequest(saved, secureRequest(), response);

    AbstractSaml2AuthenticationRequest loaded =
        repository.loadAuthenticationRequest(requestWithCookiesFrom(response));

    assertThat(loaded).isNotNull();
    assertThat(loaded.getSamlRequest()).isEqualTo(saved.getSamlRequest());
    assertThat(loaded.getRelayState()).isEqualTo(saved.getRelayState());
    assertThat(loaded.getAuthenticationRequestUri()).isEqualTo(saved.getAuthenticationRequestUri());
    assertThat(loaded.getBinding()).isEqualTo(saved.getBinding());
  }

  @Test
  public void secureRequestGetsSameSiteNoneAndSecure() {
    MockHttpServletResponse response = new MockHttpServletResponse();
    repository.saveAuthenticationRequest(authenticationRequest(), secureRequest(), response);

    Cookie cookie = response.getCookie(CookieSaml2AuthenticationRequestRepository.COOKIE_NAME);
    assertThat(cookie).isNotNull();
    assertThat(cookie.getSecure()).isTrue();
    assertThat(cookie.isHttpOnly()).isTrue();
    // Required so the cookie flows on the IdP's cross-site HTTP-POST callback to the ACS.
    assertThat(cookie.getAttribute("SameSite")).isEqualTo("None");
  }

  @Test
  public void plainHttpRequestFallsBackToLaxWithoutSecure() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setSecure(false);
    MockHttpServletResponse response = new MockHttpServletResponse();
    repository.saveAuthenticationRequest(authenticationRequest(), request, response);

    Cookie cookie = response.getCookie(CookieSaml2AuthenticationRequestRepository.COOKIE_NAME);
    assertThat(cookie).isNotNull();
    // SameSite=None requires Secure; on plain HTTP fall back to Lax.
    assertThat(cookie.getSecure()).isFalse();
    assertThat(cookie.getAttribute("SameSite")).isEqualTo("Lax");
  }

  @Test
  public void tamperedCookieIsRejected() {
    MockHttpServletResponse response = new MockHttpServletResponse();
    repository.saveAuthenticationRequest(authenticationRequest(), secureRequest(), response);
    String value = response.getCookie(CookieSaml2AuthenticationRequestRepository.COOKIE_NAME).getValue();

    // Flip the first character of the signed payload so the HMAC no longer matches.
    char first = value.charAt(0);
    String tampered = (first == 'A' ? 'B' : 'A') + value.substring(1);

    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setCookies(new Cookie(CookieSaml2AuthenticationRequestRepository.COOKIE_NAME, tampered));

    assertThat(repository.loadAuthenticationRequest(request)).isNull();
  }

  @Test
  public void cookieSignedWithAnotherKeyIsRejected() {
    MockHttpServletResponse response = new MockHttpServletResponse();
    repository.saveAuthenticationRequest(authenticationRequest(), secureRequest(), response);

    CookieSaml2AuthenticationRequestRepository otherKeyRepository =
        new CookieSaml2AuthenticationRequestRepository(() -> "a-different-key");

    assertThat(otherKeyRepository.loadAuthenticationRequest(requestWithCookiesFrom(response))).isNull();
  }

  @Test
  public void unsignedCookieValueIsRejected() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    // No signature separator: reproduces an old/forged value that carries no HMAC tag.
    request.setCookies(new Cookie(CookieSaml2AuthenticationRequestRepository.COOKIE_NAME, "not-a-signed-value"));

    assertThat(repository.loadAuthenticationRequest(request)).isNull();
  }

  private static AbstractSaml2AuthenticationRequest authenticationRequest() {
    RelyingPartyRegistration registration = RelyingPartyRegistration
        .withRegistrationId("saml")
        .entityId("https://iq.example.com/api/v2/config/saml/metadata")
        .assertionConsumerServiceLocation("https://iq.example.com/saml")
        .assertingPartyMetadata(party -> party
            .entityId("https://idp.example.com/saml")
            .singleSignOnServiceLocation("https://idp.example.com/sso"))
        .build();

    return Saml2PostAuthenticationRequest.withRelyingPartyRegistration(registration)
        .samlRequest("PHNhbWxwOkF1dGhuUmVxdWVzdC8+")
        .relayState("relay-state-123")
        .build();
  }

  private static MockHttpServletRequest secureRequest() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setSecure(true);
    return request;
  }

  private static MockHttpServletRequest requestWithCookiesFrom(MockHttpServletResponse response) {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setSecure(true);
    request.setCookies(response.getCookies());
    return request;
  }
}
