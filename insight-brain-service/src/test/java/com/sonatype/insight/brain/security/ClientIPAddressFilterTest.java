/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.core.MediaType;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.testing.AbstractBrainServiceIntegrationTest;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.test.LogOutput;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ClientIPAddressFilterTest
    extends AbstractBrainServiceIntegrationTest
{
  private static final String REST_PATH = "rest/ci/scan/testApp";

  private static final String ALLOW_IPV4 = "192.168.33.10";

  private static final String DENY_IPV4 = "192.168.33.11";

  private static final String ALLOW_IPV6 = "8ed5:9e96:1da1:f53b:587e:9f4d:a7f9:817e";

  private static final String DENY_IPV6 = "8ed5:9e96:1da1:f53b:587e:9f4d:a7f9:717e";

  @Rule
  public LogOutput logOutput = new LogOutput(ClientIPAddressFilter.class);

  private static final ObjectMapper JSON =
      new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

  @Test
  public void testClientIPAddressFilter_AccessAllowedWhenListIsNull() throws Exception {
    setAccessAllowlist(null);
    setSystemAllowlist(null);

    HttpResponse response = restRequest().header(CurrentUser.XFF_HEADER, ALLOW_IPV4).auth().put();
    assertAccessIsAllowed(response);
  }

  @Test
  public void testClientIPAddressFilter_AccessAllowedWhenListIsEmpty() throws Exception {
    setAllowlist(new ArrayList<>());
    setSystemAllowlist(new ArrayList<>());

    HttpResponse response = restRequest().header(CurrentUser.XFF_HEADER, ALLOW_IPV4).auth().put();
    assertAccessIsAllowed(response);
  }

  @Test
  public void testClientIPAddressFilter_AccessIsAllowedWhenIPv4IsInXFFHeader() throws Exception {
    setAllowlist(getTestAccessAllowlist());

    HttpResponse response = restRequest().header(CurrentUser.XFF_HEADER, ALLOW_IPV4).auth().put();
    assertAccessIsAllowed(response);
  }

  @Test
  public void testClientIPAddressFilter_AccessIsDeniedWhenIPv4IsNotMatched() throws Exception {
    setAllowlist(getTestAccessAllowlist());

    HttpResponse response = restRequest().header(CurrentUser.XFF_HEADER, DENY_IPV4).auth().put();
    assertAccessIsDenied(response, DENY_IPV4);
  }

  @Test
  public void testClientIPAddressFilter_AccessIsAllowedWhenIPv6IsInXFFHeader() throws Exception {
    setAllowlist(getTestAccessAllowlist());

    HttpResponse response = restRequest().header(CurrentUser.XFF_HEADER, ALLOW_IPV6).auth().put();
    assertAccessIsAllowed(response);
  }

  @Test
  public void testClientIPAddressFilter_AccessIsAllowedWhenIPv6LiteralIsInXFFHeader() throws Exception {
    setAllowlist(getTestAccessAllowlist());

    HttpResponse response = restRequest().header(CurrentUser.XFF_HEADER, "[" + ALLOW_IPV6 + "]").auth().put();
    assertAccessIsAllowed(response);
  }

  @Test
  public void testClientIPAddressFilter_AccessIsDeniedWhenIPv6IsNotMatched() throws Exception {
    setAllowlist(getTestAccessAllowlist());

    HttpResponse response = restRequest().header(CurrentUser.XFF_HEADER, DENY_IPV6).auth().put();
    assertAccessIsDenied(response, DENY_IPV6);
  }

  @Test
  public void testClientIPAddressFilter_AccessIsAllowedWhenIPv4IsInRange() throws Exception {
    setAllowlist(getTestAccessAllowlist());

    // CIDR 15.177.32.0/28 has a range of 15.177.32.0 to 15.177.32.15
    HttpResponse response = restRequest().header(CurrentUser.XFF_HEADER, "15.177.32.0").auth().put();
    assertAccessIsAllowed(response);

    response = restRequest().header(CurrentUser.XFF_HEADER, "15.177.32.15").auth().put();
    assertAccessIsAllowed(response);
  }

  @Test
  public void testClientIPAddressFilter_AccessIsDeniedWhenIPv4IsNotInRange() throws Exception {
    setAllowlist(getTestAccessAllowlist());

    // CIDR 15.177.32.0/28 has a range of 15.177.32.0 to 15.177.32.15
    String lowOutOfRangeIp = "15.177.31.255";
    String upperOutOfRangeIp = "15.177.32.16";

    HttpResponse response = restRequest().header(CurrentUser.XFF_HEADER, lowOutOfRangeIp).auth().put();
    assertAccessIsDenied(response, lowOutOfRangeIp);

    response = restRequest().header(CurrentUser.XFF_HEADER, upperOutOfRangeIp).auth().put();
    assertAccessIsDenied(response, upperOutOfRangeIp);
  }

  @Test
  public void testClientIPAddressFilter_AccessIsAllowedWhenIPv6IsInRange() throws Exception {
    setAllowlist(getTestAccessAllowlist());

    // CIDR b712:f3e3:7cc5:957c:e444:8773:157f:0000/124 has a range of b712:f3e3:7cc5:957c:e444:8773:157f:0000 to
    // b712:f3e3:7cc5:957c:e444:8773:157f:000f
    HttpResponse response = restRequest()
        .header(CurrentUser.XFF_HEADER, "b712:f3e3:7cc5:957c:e444:8773:157f:0000")
        .auth()
        .put();
    assertAccessIsAllowed(response);

    response = restRequest()
        .header(CurrentUser.XFF_HEADER, "b712:f3e3:7cc5:957c:e444:8773:157f:000f")
        .auth()
        .put();
    assertAccessIsAllowed(response);
  }

  @Test
  public void testClientIPAddressFilter_AccessIsDeniedWhenIPv6IsNotInRange() throws Exception {
    setAllowlist(getTestAccessAllowlist());

    // CIDR b712:f3e3:7cc5:957c:e444:8773:157f:0000/124 has a range of b712:f3e3:7cc5:957c:e444:8773:157f:0000 to
    // b712:f3e3:7cc5:957c:e444:8773:157f:000f
    String lowOutOfRangeIp = "b712:f3e3:7cc5:957c:e444:8773:157e:ffff";
    String upperOutOfRangeIp = "b712:f3e3:7cc5:957c:e444:8773:1580:0000";
    HttpResponse response = restRequest()
        .header(CurrentUser.XFF_HEADER, lowOutOfRangeIp)
        .auth()
        .put();
    assertAccessIsDenied(response, lowOutOfRangeIp);

    response = restRequest()
        .header(CurrentUser.XFF_HEADER, upperOutOfRangeIp)
        .auth()
        .put();
    assertAccessIsDenied(response, upperOutOfRangeIp);
  }

  @Test
  public void testClientIPAddressFilter_AccessIsAllowedWhenIPIsMatchedInSystemAllowlist() throws Exception {
    setAllowlist(null);
    setSystemAllowlist(getTestAccessAllowlist());

    HttpResponse response = restRequest().header(CurrentUser.XFF_HEADER, ALLOW_IPV4).auth().put();
    assertAccessIsAllowed(response);
  }

  @Test
  public void testClientIPAddressFilter_AccessIsDeniedWhenIPIsNotMatchedInSystemAllowlist() throws Exception {
    setAllowlist(null);
    setSystemAllowlist(getTestAccessAllowlist());

    HttpResponse response = restRequest().header(CurrentUser.XFF_HEADER, DENY_IPV4).auth().put();
    assertAccessIsDenied(response, DENY_IPV4);
  }

  @Test
  public void testClientIPAddressFilter_AccessIsAllowedFromLocalhost() throws Exception {
    setAllowlist(getTestAccessAllowlist());

    HttpResponse response = restRequest().auth().put();
    assertAccessIsAllowed(response);
  }

  @Test
  public void testClientIPAddressFilter_AccessIsAllowedWithRemoteAddrIPv4() throws Exception {
    setAllowlist(getTestAccessAllowlist());

    FilterChain mockFilterChain = mock(FilterChain.class);

    HttpServletRequest mockHttpServletRequest = mock(HttpServletRequest.class);
    when(mockHttpServletRequest.getRemoteAddr()).thenReturn(ALLOW_IPV4);

    HttpServletResponse mockHttpServletResponse = mock(HttpServletResponse.class);

    getCLMServer().getInstance(ClientIPAddressFilter.class)
        .doFilter(mockHttpServletRequest, mockHttpServletResponse, mockFilterChain);

    verify(mockFilterChain).doFilter(mockHttpServletRequest, mockHttpServletResponse);
  }

  @Test
  public void testClientIPAddressFilter_AccessIsDeniedWithRemoteAddrIPv4() throws Exception {
    setAllowlist(getTestAccessAllowlist());

    FilterChain mockFilterChain = mock(FilterChain.class);

    HttpServletRequest mockHttpServletRequest = mock(HttpServletRequest.class);
    when(mockHttpServletRequest.getRemoteAddr()).thenReturn(DENY_IPV4);

    PrintWriter mockPrintWriter = mock(PrintWriter.class);
    HttpServletResponse mockHttpServletResponse = mock(HttpServletResponse.class);
    when(mockHttpServletResponse.getWriter()).thenReturn(mockPrintWriter);

    getCLMServer().getInstance(ClientIPAddressFilter.class)
        .doFilter(mockHttpServletRequest, mockHttpServletResponse, mockFilterChain);

    verify(mockHttpServletResponse).setStatus(HttpServletResponse.SC_FORBIDDEN);
    verify(mockHttpServletResponse).setContentType(MediaType.TEXT_PLAIN);
    verify(mockPrintWriter).print(ClientIPAddressFilter.ACCESS_DENIED_MSG);
    assertThat(logOutput).atWarnLevel()
        .containsPattern(
            String.format("Rejecting request from %s as the IP was not found in the configured allowlist", DENY_IPV4));
    verify(mockFilterChain, never()).doFilter(mockHttpServletRequest, mockHttpServletResponse);
  }

  @Test
  public void testClientIPAddressFilter_AccessIsAllowedWithRemoteAddrIPv6() throws Exception {
    setAllowlist(getTestAccessAllowlist());

    FilterChain mockFilterChain = mock(FilterChain.class);

    HttpServletRequest mockHttpServletRequest = mock(HttpServletRequest.class);
    when(mockHttpServletRequest.getRemoteAddr()).thenReturn(ALLOW_IPV6);

    HttpServletResponse mockHttpServletResponse = mock(HttpServletResponse.class);

    getCLMServer().getInstance(ClientIPAddressFilter.class)
        .doFilter(mockHttpServletRequest, mockHttpServletResponse, mockFilterChain);

    verify(mockFilterChain).doFilter(mockHttpServletRequest, mockHttpServletResponse);
  }

  @Test
  public void testClientIPAddressFilter_AccessIsDeniedWithRemoteAddrIPv6() throws Exception {
    setAllowlist(getTestAccessAllowlist());

    FilterChain mockFilterChain = mock(FilterChain.class);

    HttpServletRequest mockHttpServletRequest = mock(HttpServletRequest.class);
    when(mockHttpServletRequest.getRemoteAddr()).thenReturn(DENY_IPV6);

    PrintWriter mockPrintWriter = mock(PrintWriter.class);
    HttpServletResponse mockHttpServletResponse = mock(HttpServletResponse.class);
    when(mockHttpServletResponse.getWriter()).thenReturn(mockPrintWriter);

    getCLMServer().getInstance(ClientIPAddressFilter.class)
        .doFilter(mockHttpServletRequest, mockHttpServletResponse, mockFilterChain);

    verify(mockHttpServletResponse).setStatus(HttpServletResponse.SC_FORBIDDEN);
    verify(mockHttpServletResponse).setContentType(MediaType.TEXT_PLAIN);
    verify(mockPrintWriter).print(ClientIPAddressFilter.ACCESS_DENIED_MSG);
    assertThat(logOutput).atWarnLevel()
        .containsPattern(
            String.format("Rejecting request from %s as the IP was not found in the configured allowlist", DENY_IPV6));
    verify(mockFilterChain, never()).doFilter(mockHttpServletRequest, mockHttpServletResponse);
  }

  @Test
  public void testClientIPAddressFilter_AccessIsAllowedWithoutLicensedFeatureIP_ALLOWLIST() throws Exception {
    // Remove the PRODUCT_LIFECYCLE_CLOUD feature flag LicensedFeature.IP_ALLOWLIST
    setFeatures(LicensedFeature.DASHBOARD);
    setSystemAllowlist(getTestAccessAllowlist());

    FilterChain mockFilterChain = mock(FilterChain.class);

    HttpServletRequest mockHttpServletRequest = mock(HttpServletRequest.class);
    when(mockHttpServletRequest.getRemoteAddr()).thenReturn(DENY_IPV4);

    HttpServletResponse mockHttpServletResponse = mock(HttpServletResponse.class);

    getCLMServer().getInstance(ClientIPAddressFilter.class)
        .doFilter(mockHttpServletRequest, mockHttpServletResponse, mockFilterChain);

    verify(mockFilterChain).doFilter(mockHttpServletRequest, mockHttpServletResponse);
  }

  @Test
  public void testClientIPAddressFilter_FilterHandlesInvalidIPsInAllowlist() throws Exception {
    setAllowlist(getTestAccessAllowlist());

    List<AllowedIp> systemAllowlist = new ArrayList<>();
    systemAllowlist.add(new AllowedIp("192.168.33.999", "Invalid IPv4 Address description"));
    systemAllowlist.add(new AllowedIp("2600:1f18:3fff:f800", "Invalid IPv6 Address description"));
    systemAllowlist.add(new AllowedIp(null, "null IP Address description"));
    systemAllowlist.add(new AllowedIp("", "Empty IP Address description"));
    setSystemAllowlist(systemAllowlist);

    HttpResponse response = restRequest().header(CurrentUser.XFF_HEADER, DENY_IPV4).auth().put();
    assertThat(logOutput).atErrorLevel()
        .containsPattern("Invalid IP Address in Allowlist: 192.168.33.999 Invalid IPv4 Address description")
        .containsPattern("Invalid IP Address in Allowlist: 2600:1f18:3fff:f800 Invalid IPv6 Address description")
        .containsPattern("Invalid IP Address in Allowlist: null null IP Address description")
        .containsPattern("Invalid IP Address in Allowlist:  Empty IP Address description");

    assertAccessIsDenied(response, DENY_IPV4);
  }

  private List<AllowedIp> getTestAccessAllowlist() {
    List<AllowedIp> result = new ArrayList<>();
    result.add(new AllowedIp(ALLOW_IPV4, "Test IPv4 address"));
    result.add(new AllowedIp(ALLOW_IPV6, "Test IPv6 address"));
    result.add(new AllowedIp("15.177.32.0/28", "Test IPv4 CIDR"));
    result.add(new AllowedIp("b712:f3e3:7cc5:957c:e444:8773:157f:0000/124", "Test IPv6 CIDR"));

    return result;
  }

  private void setAllowlist(List<AllowedIp> allowlist) {
    List<Map<String, String>> map = JSON.convertValue(allowlist, new TypeReference<List<Map<String, String>>>()
    {
    });
    setAccessAllowlist(map);
  }

  private void setSystemAllowlist(List<AllowedIp> allowlist) {
    getCLMServer().getConfiguration().setSystemAllowlist(allowlist);
  }

  private void assertAccessIsAllowed(HttpResponse response) {
    // since we aren't providing a proper app id, we will get an error message back from the DAO, which means the
    // request filter passed.
    assertThat(response.getBodyText()).isEqualTo("Could not find an application with public ID testApp.");
    // 404 since we are providing an app id that can't be found.
    assertResponseStatus(HttpServletResponse.SC_NOT_FOUND, response);
  }

  private void assertAccessIsDenied(HttpResponse response, String rejectedIp) {
    assertThat(response.getBodyText()).isEqualTo(ClientIPAddressFilter.ACCESS_DENIED_MSG);
    assertResponseStatus(HttpServletResponse.SC_FORBIDDEN, response);

    assertThat(logOutput).atWarnLevel()
        .containsPattern(
            String.format("Rejecting request from %s as the IP was not found in the configured allowlist", rejectedIp));
  }

  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(REST_PATH).anon();
  }
}
