/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.admin.authorization;

import java.io.PrintWriter;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.sonatype.insight.brain.api.admin.authorization.provider.MultiTenantJwkProvider;

import com.auth0.jwk.Jwk;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.apache.http.HttpHeaders;
import org.junit.Before;
import org.junit.Test;

import static com.sonatype.insight.brain.api.admin.authorization.AuthContextProperties.SUBJECT_USER;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class JwtHttpAuthorizationFilterTest
{
  private final MultiTenantJwkProvider multiTenantJwkProvider = mock(MultiTenantJwkProvider.class);

  private final FilterChain filterChain = mock(FilterChain.class);

  private final HttpServletResponse response = mock(HttpServletResponse.class);

  private final HttpServletRequest request = mock(HttpServletRequest.class);

  private final PrintWriter responseWriter = mock(PrintWriter.class);

  private JwtHttpAuthorizationFilter underTest;

  @Before
  public void before() {
    underTest = new JwtHttpAuthorizationFilter(multiTenantJwkProvider);
    when(request.getRequestURI()).thenReturn("/api/admin/test");
    when(request.getServletPath()).thenReturn("/api/admin/test");
  }

  @Test
  public void testFilter_denyRequest_WhenProviderIsNotConfigured() throws Exception {
    when(multiTenantJwkProvider.denyRequest()).thenReturn(true);
    when(response.getWriter()).thenReturn(responseWriter);

    underTest.doFilter(request, response, filterChain);

    verify(responseWriter).print("No authorization provider configured");
  }

  @Test
  public void testFilter_validatesJwt_RegularFlow() throws Exception {
    String jwt = AuthorizationTestHelper.createJwt();

    prepareMultiTenantJwkProvider(jwt);

    underTest.doFilter(request, response, filterChain);

    verify(request).setAttribute(SUBJECT_USER, "test@test.com");
  }

  @Test
  public void testFilter_validatesJwt_DifferentValidIssuers() throws Exception {
    String jwt1 = AuthorizationTestHelper.createJwt("test1/");
    String jwt2 = AuthorizationTestHelper.createJwt("test2/");

    prepareMultiTenantJwkProvider(jwt1, jwt2);

    // Send first JWT
    when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer " + jwt1);
    when(response.getWriter()).thenReturn(responseWriter);
    underTest.doFilter(request, response, filterChain);

    // Send second JWT
    when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer " + jwt2);
    when(response.getWriter()).thenReturn(responseWriter);
    underTest.doFilter(request, response, filterChain);

    verify(request, times(2)).setAttribute(SUBJECT_USER, "test@test.com");
  }

  @Test
  public void testFilter_validatesJwt_RequiresBearerAuth() throws Exception {
    String jwt = AuthorizationTestHelper.createJwt();

    prepareMultiTenantJwkProvider(jwt);
    when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn(jwt);

    underTest.doFilter(request, response, filterChain);

    verify(responseWriter).print("Bearer authorization required");
  }

  @Test
  public void testFilter_validatesJwt_AlgorithmMissMatch() throws Exception {
    Algorithm rejectedAlgorithm = Algorithm.HMAC256("rejectedAlgorithm");
    Map<String, Object> jwtValues = new HashMap<>();
    jwtValues.put("kid", "A1B2C3");

    String jwt = AuthorizationTestHelper.createJwt(jwtValues, rejectedAlgorithm);
    prepareMultiTenantJwkProvider(jwt);

    underTest.doFilter(request, response, filterChain);

    verify(responseWriter).print("The provided Algorithm doesn't match the one defined in the JWT's Header.");
  }

  @Test
  public void testFilter_validatesJwt_IssuerMissMatch() throws Exception {
    Map<String, Object> jwtValues = new HashMap<>();
    jwtValues.put("kid", "A1B2C3");
    jwtValues.put("iss", "rejectedIssuer");
    String jwt = AuthorizationTestHelper.createJwt(jwtValues, AuthorizationTestHelper.getRSA256Algorithm());

    prepareMultiTenantJwkProvider(jwt);
    when(multiTenantJwkProvider.getIssuers()).thenReturn(new String[]{"expectedIssuer"});

    underTest.doFilter(request, response, filterChain);

    verify(responseWriter).print("The Claim 'iss' value doesn't match the required issuer.");
  }

  @Test
  public void testFilter_validatesJwt_Expired() throws Exception {
    Date expirationDate = new Date(System.currentTimeMillis());
    Map<String, Object> jwtValues = new HashMap<>();
    jwtValues.put("kid", "ABC");
    jwtValues.put("iss", "issuer");
    jwtValues.put("exp", expirationDate);

    String jwt = AuthorizationTestHelper.createJwt(jwtValues, AuthorizationTestHelper.getRSA256Algorithm());
    prepareMultiTenantJwkProvider(jwt);

    underTest.doFilter(request, response, filterChain);

    DateTimeFormatter formatter = new DateTimeFormatterBuilder().appendInstant(0).toFormatter();
    verify(responseWriter).print("The Token has expired on " + formatter.format(expirationDate.toInstant()) + ".");
  }

  @Test
  public void testFilter_validatesJwt_MissingEmail_SwitchToSub() throws Exception {
    String expectedSub = "auth0|123456";
    Map<String, Object> jwtValues = new HashMap<>();
    jwtValues.put("kid", "ABC");
    jwtValues.put("iss", "issuer");
    jwtValues.put("exp", new Date(System.currentTimeMillis() + 100000L));
    jwtValues.put("sub", expectedSub);
    String jwt = AuthorizationTestHelper.createJwt(jwtValues, AuthorizationTestHelper.getRSA256Algorithm());

    prepareMultiTenantJwkProvider(jwt);

    underTest.doFilter(request, response, filterChain);

    verify(request).setAttribute(SUBJECT_USER, expectedSub);
  }

  private void prepareMultiTenantJwkProvider(String jwt) throws Exception {
    DecodedJWT decodedJWT = JWT.decode(jwt);
    Jwk jwk = AuthorizationTestHelper.createJwk(decodedJWT.getKeyId());

    when(multiTenantJwkProvider.denyRequest()).thenReturn(false);
    when(multiTenantJwkProvider.getJsonWebKey(decodedJWT.getKeyId())).thenReturn(jwk);
    when(multiTenantJwkProvider.getIssuers()).thenReturn(new String[]{decodedJWT.getIssuer()});
    when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer " + jwt);
    when(response.getWriter()).thenReturn(responseWriter);
  }

  private void prepareMultiTenantJwkProvider(String jwt1, String jwt2) throws Exception {
    DecodedJWT decodedJWT1 = JWT.decode(jwt1);
    DecodedJWT decodedJWT2 = JWT.decode(jwt2);
    Jwk jwk = AuthorizationTestHelper.createJwk(decodedJWT1.getKeyId());

    when(multiTenantJwkProvider.denyRequest()).thenReturn(false);
    when(multiTenantJwkProvider.getJsonWebKey(decodedJWT1.getKeyId())).thenReturn(jwk);
    when(multiTenantJwkProvider.getIssuers()).thenReturn(
        new String[]{decodedJWT1.getIssuer(), decodedJWT2.getIssuer()});
  }
}
