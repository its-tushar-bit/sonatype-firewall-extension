/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.shiro.web.servlet.AdviceFilter;

import static com.google.common.net.HttpHeaders.SET_COOKIE;
import static com.sonatype.insight.brain.security.SecurityModule.SESSION_COOKIE_NAME;

/**
 * @since 1.16.0
 */
@Named
@Singleton
public class SecureCookiesFilter
    extends AdviceFilter
{
  // Matches a SameSite attribute (case-insensitive) and its value, including the leading semicolon + optional space
  // Stops at semicolon to handle compact-format cookies (e.g. ";SameSite=Lax;HttpOnly" with no spaces)
  private static final Pattern SAMESITE_PATTERN =
      Pattern.compile(";\\s*SameSite\\s*=\\s*[^;\\s]+", Pattern.CASE_INSENSITIVE);

  // Matches the Secure attribute (case-insensitive), including the leading semicolon + optional space
  private static final Pattern SECURE_PATTERN =
      Pattern.compile(";\\s*Secure\\b", Pattern.CASE_INSENSITIVE);

  private final FrameEmbeddingDetector frameEmbeddingDetector;

  @Inject
  public SecureCookiesFilter(FrameEmbeddingDetector frameEmbeddingDetector) {
    this.frameEmbeddingDetector = frameEmbeddingDetector;
  }

  /**
   * Perform filtering as a post handler.
   *
   * Performing the filtering after the other filters have finished ensures that the secure flags are always set
   * even if the filter chain is stopped.
   */
  @Override
  protected void postHandle(final ServletRequest request, final ServletResponse response) throws Exception {
    // session cookies are expected to be set already by another filter
    filterCookies(request, response);
  }

  /**
   * Cookie normalization is only performed on secure (HTTPS) requests; non-secure requests pass
   * through untouched. On secure requests, every {@code Set-Cookie} header on the response is
   * rewritten as follows:
   * <ul>
   * <li>Ensure {@code Secure} is present on the cookie (append it if missing, leave alone if already there).</li>
   * <li>When cross-origin iframe embedding is enabled ({@code frameAncestorsAllowlist} non-empty):
   * {@code CLMSESSIONID} gets {@code SameSite=None} on every path, so cookies flow on cross-site
   * iframe requests (Jenkins plugin, IDE plugins, etc.).</li>
   * <li>Otherwise, for {@code CLMSESSIONID} on {@code /saml/**} paths: set {@code SameSite=None}
   * (required for SAML IdP cross-site POST callbacks).</li>
   * <li>Otherwise, for {@code CLMSESSIONID} on all other secure paths: set {@code SameSite=Lax}.</li>
   * <li>For all other cookies: preserve any existing {@code SameSite}; do not inject one.</li>
   * </ul>
   */
  private void filterCookies(final ServletRequest request, final ServletResponse response) {
    if (request.isSecure() && response instanceof HttpServletResponse) {
      String requestPath = getRequestPath((HttpServletRequest) request);
      normalizeCookies((HttpServletResponse) response, requestPath);
    }
  }

  private String getRequestPath(HttpServletRequest request) {
    return request.getRequestURI().substring(request.getContextPath().length());
  }

  // Matches the /saml/** subpaths routed through the SAML filter chain in SecurityModule.
  // Note: the bare /saml path is also a SAML endpoint (the ACS that receives the IdP's cross-site POST),
  // but SameSite=Lax is safe there: the cross-site POST is already handled by the time the response
  // sets the session cookie, and the subsequent redirect back to IQ Server is same-site.
  private boolean isSamlPath(String requestPath) {
    return requestPath != null && requestPath.startsWith("/saml/");
  }

  private void normalizeCookies(final HttpServletResponse response, final String requestPath) {
    final Collection<String> cookies = response.getHeaders(SET_COOKIE);
    if (cookies.isEmpty()) {
      return;
    }

    String sessionSameSite = selectSessionSameSite(requestPath);
    List<String> normalized = new ArrayList<>(cookies.size());
    for (final String cookie : cookies) {
      normalized.add(normalizeCookie(cookie, sessionSameSite));
    }

    boolean first = true;
    for (final String cookie : normalized) {
      if (first) {
        response.setHeader(SET_COOKIE, cookie);
        first = false;
      }
      else {
        response.addHeader(SET_COOKIE, cookie);
      }
    }
  }

  /**
   * Pick the SameSite attribute value for the session cookie (CLMSESSIONID).
   *
   * When {@code frameAncestorsAllowlist} is configured, IQ may be embedded in cross-origin iframes
   * (Jenkins plugin, IDE plugins, etc.); those iframes make same-browser cross-site requests back to
   * IQ and need the session cookie to flow, which requires {@code SameSite=None}. Otherwise we
   * prefer {@code Lax} for all paths except {@code /saml/**}, where cross-site IdP POSTs need None.
   */
  private String selectSessionSameSite(String requestPath) {
    // Callers (filterCookies) have already verified request.isSecure() == true, so emitting
    // SameSite=None here is safe per the Chrome 80+ / Firefox 79+ requirement that None cookies
    // must also carry Secure. Sibling filters (AntiCsrfFilter, SessionExpirationCookieFilter) check
    // isSecure() inline where they call the detector; this filter's guard is at the entry point.
    if (frameEmbeddingDetector.isFrameEmbeddingEnabled()) {
      return "None";
    }
    return isSamlPath(requestPath) ? "None" : "Lax";
  }

  private String normalizeCookie(String cookie, String sessionSameSiteValue) {
    String cookieName = extractCookieName(cookie);

    String normalizedCookie = ensureSecure(cookie);

    if (SESSION_COOKIE_NAME.equalsIgnoreCase(cookieName)) {
      normalizedCookie = replaceSameSite(normalizedCookie, sessionSameSiteValue);
    }
    // Other cookies: preserve existing SameSite, do not inject one

    return normalizedCookie;
  }

  private String extractCookieName(String cookie) {
    int eqIdx = cookie.indexOf('=');
    if (eqIdx < 0) {
      // Malformed cookie (no '=' at all): return the whole trimmed string so it won't match
      // SESSION_COOKIE_NAME and SameSite normalization is silently skipped (fail-safe).
      return cookie.trim();
    }
    return cookie.substring(0, eqIdx).trim();
  }

  private String ensureSecure(String cookie) {
    if (SECURE_PATTERN.matcher(cookie).find()) {
      return cookie;
    }
    return cookie + "; Secure";
  }

  private String replaceSameSite(String cookie, String sameSiteValue) {
    Matcher matcher = SAMESITE_PATTERN.matcher(cookie);
    String stripped = matcher.replaceAll("");
    return stripped + "; SameSite=" + sameSiteValue;
  }
}
