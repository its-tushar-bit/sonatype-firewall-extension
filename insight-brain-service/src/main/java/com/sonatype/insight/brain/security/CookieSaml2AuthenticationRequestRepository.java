/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputFilter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Base64;
import java.util.stream.Stream;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.saml2.provider.service.authentication.AbstractSaml2AuthenticationRequest;
import org.springframework.security.saml2.provider.service.web.Saml2AuthenticationRequestRepository;

/**
 * Stores the SAML {@code AuthnRequest} in a short-lived, secure, HttpOnly cookie rather than the HTTP
 * session.
 *
 * <p>
 * IQ persists Shiro sessions to the database as JSON (see {@code PersistedUserSession}); Spring's
 * immutable {@link AbstractSaml2AuthenticationRequest} types are not Jackson-serializable, so the
 * default {@code HttpSessionSaml2AuthenticationRequestRepository} would break session persistence. The
 * request types are {@link java.io.Serializable}, so a Base64-encoded serialized cookie keeps the
 * SP-initiated correlation ({@code InResponseTo}/{@code RelayState}) working without touching the
 * persisted session.
 *
 * <p>
 * Because the cookie value is client-controllable, its contents are integrity-protected with an
 * HMAC-SHA256 tag keyed by the deployment's shared {@link EncryptionKeyStore} secret (DB/secret-store
 * backed, so it is stable across restarts and consistent across HA nodes and per MTIQ tenant). The
 * {@code InResponseTo} request id is meaningful only if the server can trust it was the one it issued;
 * signing prevents a client from forging or tampering with the saved request, which would otherwise
 * defeat the SP-initiated replay/CSRF protection.
 *
 * <p>
 * Because the tag is keyed off the {@link EncryptionKeyStore} secret, an encryption-key rotation
 * (e.g. {@code RotateEncryptionKeyTask}) invalidates any in-flight AuthnRequest cookies, causing transient
 * SP-initiated login failures during the rotation window; affected users simply restart login.
 *
 * <p>
 * The cookie is written with {@code SameSite=None} on secure requests so it survives the IdP's
 * cross-site {@code HTTP-POST} callback to the assertion consumer service (a top-level cross-site POST,
 * on which {@code SameSite=Lax} cookies are not sent). {@code SameSite=None} requires {@code Secure},
 * so plain-HTTP requests fall back to {@code Lax}.
 */
public class CookieSaml2AuthenticationRequestRepository
    implements Saml2AuthenticationRequestRepository<AbstractSaml2AuthenticationRequest>
{
  static final String COOKIE_NAME = "SAML2_AUTHN_REQUEST";

  // Bounds how long a user may spend at the IdP (MFA, password reset, consent) before the SP-initiated
  // AuthnRequest cookie expires and login must be restarted. The assertion's own NotOnOrAfter bounds replay.
  private static final int MAX_AGE_SECONDS = (int) Duration.ofMinutes(15).toSeconds();

  private static final String HMAC_ALGORITHM = "HmacSHA256";

  // Separates the Base64URL payload from its Base64URL HMAC tag. The Base64URL alphabet never
  // contains '.', so a single dot unambiguously splits the two halves.
  private static final char SIGNATURE_SEPARATOR = '.';

  /**
   * The cookie value is client-controllable, so restrict {@link ObjectInputStream} deserialization to the exact
   * Spring SAML2 authentication-request classes plus core {@code java.lang} types, rejecting everything else
   * (deserialization gadget chains live in other packages). Exact class names are listed rather than a package
   * wildcard so subpackages (e.g. {@code ...authentication.logout.*}) are not matched. These request types
   * serialize only {@code String} fields, so no {@code java.util} collection types are needed. This filter is
   * defense-in-depth: {@link #verifiedPayload} rejects any unsigned or tampered cookie via HMAC before
   * deserialization runs at all.
   */
  private static final ObjectInputFilter DESERIALIZATION_FILTER = ObjectInputFilter.Config.createFilter(
      "maxbytes=16384;maxdepth=10;"
          + "org.springframework.security.saml2.provider.service.authentication.Saml2PostAuthenticationRequest;"
          + "org.springframework.security.saml2.provider.service.authentication.Saml2RedirectAuthenticationRequest;"
          + "org.springframework.security.saml2.provider.service.authentication.AbstractSaml2AuthenticationRequest;"
          + "java.lang.*;!*");

  private final EncryptionKeyStore encryptionKeyStore;

  public CookieSaml2AuthenticationRequestRepository(EncryptionKeyStore encryptionKeyStore) {
    this.encryptionKeyStore = encryptionKeyStore;
  }

  @Override
  public AbstractSaml2AuthenticationRequest loadAuthenticationRequest(HttpServletRequest request) {
    Cookie[] cookies = request.getCookies();
    if (cookies == null) {
      return null;
    }
    return Stream.of(cookies)
        .filter(cookie -> COOKIE_NAME.equals(cookie.getName()))
        .map(Cookie::getValue)
        .filter(value -> value != null && !value.isEmpty())
        .map(this::deserialize)
        .filter(java.util.Objects::nonNull)
        .findFirst()
        .orElse(null);
  }

  @Override
  public void saveAuthenticationRequest(
      AbstractSaml2AuthenticationRequest authenticationRequest,
      HttpServletRequest request,
      HttpServletResponse response)
  {
    if (authenticationRequest == null) {
      removeAuthenticationRequest(request, response);
      return;
    }
    Cookie cookie = new Cookie(COOKIE_NAME, serialize(authenticationRequest));
    applyCookieAttributes(cookie, request, MAX_AGE_SECONDS);
    response.addCookie(cookie);
  }

  @Override
  public AbstractSaml2AuthenticationRequest removeAuthenticationRequest(
      HttpServletRequest request,
      HttpServletResponse response)
  {
    AbstractSaml2AuthenticationRequest existing = loadAuthenticationRequest(request);
    Cookie cookie = new Cookie(COOKIE_NAME, "");
    applyCookieAttributes(cookie, request, 0);
    response.addCookie(cookie);
    return existing;
  }

  private static void applyCookieAttributes(Cookie cookie, HttpServletRequest request, int maxAgeSeconds) {
    cookie.setPath(cookiePath(request));
    cookie.setSecure(request.isSecure());
    cookie.setHttpOnly(true);
    cookie.setMaxAge(maxAgeSeconds);
    // SameSite=None lets the cookie flow on the IdP's cross-site HTTP-POST callback to the ACS;
    // browsers reject SameSite=None without Secure (Chrome 80+, Firefox 79+), so fall back to Lax on
    // plain HTTP (where a real cross-site IdP round-trip does not happen anyway).
    cookie.setAttribute("SameSite", request.isSecure() ? "None" : "Lax");
  }

  private static String cookiePath(HttpServletRequest request) {
    String contextPath = request.getContextPath();
    return (contextPath == null || contextPath.isEmpty()) ? "/" : contextPath;
  }

  private String serialize(AbstractSaml2AuthenticationRequest authenticationRequest) {
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
      out.writeObject(authenticationRequest);
    }
    catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    String payload = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes.toByteArray());
    String signature = Base64.getUrlEncoder().withoutPadding().encodeToString(hmac(payload));
    return payload + SIGNATURE_SEPARATOR + signature;
  }

  private AbstractSaml2AuthenticationRequest deserialize(String value) {
    String payload = verifiedPayload(value);
    if (payload == null) {
      return null;
    }
    try {
      byte[] data = Base64.getUrlDecoder().decode(payload);
      try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(data))) {
        in.setObjectInputFilter(DESERIALIZATION_FILTER);
        Object object = in.readObject();
        return object instanceof AbstractSaml2AuthenticationRequest request ? request : null;
      }
    }
    catch (IOException | ClassNotFoundException | IllegalArgumentException e) {
      // Corrupt, tampered, expired, or disallowed cookie: treat as no saved request.
      return null;
    }
  }

  /**
   * Verify the cookie's HMAC tag and return the payload half, or {@code null} if the value is malformed,
   * unsigned, or the signature does not match (tampered, forged, or signed under a rotated/foreign key).
   */
  private String verifiedPayload(String value) {
    int separator = value.indexOf(SIGNATURE_SEPARATOR);
    if (separator <= 0 || separator == value.length() - 1) {
      return null;
    }
    String payload = value.substring(0, separator);
    byte[] presented;
    try {
      presented = Base64.getUrlDecoder().decode(value.substring(separator + 1));
    }
    catch (IllegalArgumentException e) {
      return null;
    }
    // MessageDigest.isEqual is constant-time, avoiding a timing side channel on the tag comparison.
    return MessageDigest.isEqual(hmac(payload), presented) ? payload : null;
  }

  private byte[] hmac(String payload) {
    try {
      Mac mac = Mac.getInstance(HMAC_ALGORITHM);
      mac.init(new SecretKeySpec(encryptionKeyStore.getKey().getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
      return mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
    }
    catch (GeneralSecurityException e) {
      throw new IllegalStateException("Failed to sign SAML authentication request cookie", e);
    }
  }
}
