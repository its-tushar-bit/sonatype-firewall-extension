/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.zscaler;

import static com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature.MALICIOUS_URLS_PARTNER_ACCESS;
import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sonatype.insight.brain.hds.HdsClient;
import com.sonatype.insight.brain.variant.AbstractComponentH2Test;
import com.sonatype.insight.brain.variant.ComponentH2Test;
import com.sonatype.insight.error.exception.BadGatewayException;
import com.sonatype.insight.test.LogOutput;
import jakarta.inject.Inject;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.Rule;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

@ComponentH2Test
public class HDSMaliciousUrlFetcherTest
    extends AbstractComponentH2Test
{
  @Rule
  public LogOutput logOutput = new LogOutput(HDSMaliciousUrlFetcher.class);

  @Mock
  private HdsClient hdsClient;

  @Inject
  private HDSMaliciousUrlFetcher underTest;

  @Test
  public void testFetchMaliciousUrls_withoutPartnerAccessFeatureFlag() {
    MALICIOUS_URLS_PARTNER_ACCESS.setEnabled(false);
    InputStream expectedResponse = new ByteArrayInputStream("maliciousUrls".getBytes(StandardCharsets.UTF_8));
    when(hdsClient.get(eq(InputStream.class), eq("rest/maliciousUrls/active/maven"), eq(emptyMap())))
        .thenReturn(expectedResponse);

    InputStream actualResponse = underTest.fetchMaliciousUrls(ZScalerSupportedFormat.MAVEN);

    assertEquals(expectedResponse, actualResponse);
    assertThat(logOutput).atDebugLevel().contains("Updating zScaler Malicious URLs for format: MAVEN");

    verify(hdsClient).get(eq(InputStream.class), eq("rest/maliciousUrls/active/maven"), eq(emptyMap()));
  }

  @Test
  public void testFetchMaliciousUrls_withPartnerAccessFeatureFlag() {
    MALICIOUS_URLS_PARTNER_ACCESS.setEnabled(true);
    Map<String, String> queryParams = Map.of("isPartnerAccess", "true");

    InputStream expectedResponse = new ByteArrayInputStream("maliciousUrls".getBytes(StandardCharsets.UTF_8));
    when(hdsClient.get(eq(InputStream.class), eq("rest/maliciousUrls/active/maven"), eq(queryParams)))
        .thenReturn(expectedResponse);

    InputStream actualResponse = underTest.fetchMaliciousUrls(ZScalerSupportedFormat.MAVEN);

    assertEquals(expectedResponse, actualResponse);
    assertThat(logOutput).atDebugLevel().contains("Updating zScaler Malicious URLs for format: MAVEN");

    verify(hdsClient).get(eq(InputStream.class), eq("rest/maliciousUrls/active/maven"), eq(queryParams));
  }

  @Test
  public void testFetchMaliciousUrls_badGatewayException() {
    when(hdsClient.get(eq(InputStream.class), eq("rest/maliciousUrls/active/npm"), anyMap()))
        .thenThrow(new BadGatewayException("Bad Gateway"));

    InputStream inputStream = underTest.fetchMaliciousUrls(ZScalerSupportedFormat.NPM);

    assertThat(inputStream).isNull();
    assertThat(logOutput).atWarnLevel().contains("Failed to get zScaler malicious URLs");
  }
}
