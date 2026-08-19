/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.admin.authorization;

import java.io.IOException;
import java.security.interfaces.RSAPublicKey;
import java.util.Optional;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.sonatype.insight.brain.api.admin.authorization.provider.MultiTenantJwkProvider;

import com.auth0.jwk.Jwk;
import com.auth0.jwk.JwkException;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.entity.ContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.insight.brain.api.admin.authorization.AuthContextProperties.SUBJECT_USER;
import static com.sonatype.insight.brain.api.admin.authorization.AuthJWTClaims.SUBJECT_CLAIM;
import static com.sonatype.insight.brain.api.admin.authorization.AuthJWTClaims.USER_EMAIL_CLAIM;

/**
 * Admin Servlet filter that authorizes requests based on a simple resource-based JWT bearer authorization scheme
 */
@Named
public class JwtHttpAuthorizationFilter
    extends HttpFilter
{
  private static final Logger log = LoggerFactory.getLogger(JwtHttpAuthorizationFilter.class.getName());

  private static final String BEARER_PREFIX = "Bearer ";

  private final MultiTenantJwkProvider multiTenantJwkProvider;

  @Inject
  public JwtHttpAuthorizationFilter(MultiTenantJwkProvider multiTenantJwkProvider) {
    this.multiTenantJwkProvider = multiTenantJwkProvider;
  }

  @Override
  protected void doFilter(
      HttpServletRequest req,
      HttpServletResponse res,
      FilterChain chain) throws IOException, ServletException
  {
    if (shouldPassThrough(req)) {
      chain.doFilter(req, res);
      return;
    }

    if (multiTenantJwkProvider.denyRequest()) {
      log.warn("Can't authorize request for {} to {} {}", req.getRemoteAddr(), req.getMethod(), req.getServletPath());
      forbid(res, "No authorization provider configured");
      return;
    }

    Optional<DecodedJWT> jwt = validateJwt(req, res);
    // TODO add fine-grained validations according to roles definition, e.g. resource and method access CLM-25676
    if (jwt.isPresent()) {
      log.debug("Permit request for {} to {} {}", req.getRemoteAddr(), req.getMethod(), req.getServletPath());
      req.setAttribute(SUBJECT_USER, getSubjectUserClaim(jwt.get()));
      chain.doFilter(req, res);
    }
    else {
      logDeniedRequest(req);
    }
  }

  private Optional<DecodedJWT> validateJwt(
      final HttpServletRequest req,
      final HttpServletResponse res) throws IOException
  {
    final String authorizationHeader = req.getHeader(HttpHeaders.AUTHORIZATION);

    if (authorizationHeader == null || !authorizationHeader.trim().startsWith(BEARER_PREFIX)) {
      challenge(res, "Bearer authorization required");
      return Optional.empty();
    }

    DecodedJWT decodedJWT = JWT.decode(authorizationHeader.trim().substring(BEARER_PREFIX.length()));

    try {
      Jwk jwk = multiTenantJwkProvider.getJsonWebKey(decodedJWT.getKeyId());
      RSAPublicKey rsaPublicKey = (RSAPublicKey) jwk.getPublicKey();
      Algorithm algorithm = Algorithm.RSA256(rsaPublicKey, null);
      JWTVerifier jwtVerifier = JWT.require(algorithm)
          .withIssuer(multiTenantJwkProvider.getIssuers())
          .build();

      jwtVerifier.verify(decodedJWT);
    }
    catch (JWTVerificationException | JwkException e) {
      log.warn("Can't authorize request for {} to {} {}", req.getRemoteAddr(), req.getMethod(), req.getServletPath());
      challenge(res, e.getMessage());
      return Optional.empty();
    }

    return Optional.of(decodedJWT);
  }

  private static boolean shouldPassThrough(final HttpServletRequest req) {
    String requestUri = req.getRequestURI();
    return requestUri == null || !requestUri.startsWith("/api/admin/");
  }

  // Tell the client to come back with Bearer authorization
  private static void challenge(final HttpServletResponse res, final String reason) throws IOException {
    res.setHeader("WWW-Authenticate", "Bearer");
    res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    res.setContentType(ContentType.TEXT_PLAIN.getMimeType());
    res.getWriter().print(reason);
  }

  // Tell client that their authorization is insufficient
  private static void forbid(final HttpServletResponse res, final String reason) throws IOException {
    res.setStatus(HttpServletResponse.SC_FORBIDDEN);
    res.setContentType(ContentType.TEXT_PLAIN.getMimeType());
    res.getWriter().print(reason);
  }

  private static void logDeniedRequest(final HttpServletRequest req) {
    log.debug("Deny request for {} to {} {}", req.getRemoteAddr(), req.getMethod(), req.getServletPath());
  }

  private static String normalizeClaim(final Claim claim) {
    return StringUtils.replace(claim.toString(), "\"", "");
  }

  // The subject user could vary according to the received token
  private static String getSubjectUserClaim(DecodedJWT jwt) {
    Claim subjectUserClaim = jwt.getClaim(USER_EMAIL_CLAIM.getClaim());

    if (subjectUserClaim.isMissing() || subjectUserClaim.isNull()) {
      subjectUserClaim = jwt.getClaim(SUBJECT_CLAIM.getClaim()); // defaults to "sub" claim
    }

    return normalizeClaim(subjectUserClaim);
  }
}
