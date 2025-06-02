/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.zscaler;

import javax.inject.Inject;

import com.sonatype.insight.brain.hds.HdsClient;
import com.sonatype.insight.error.exception.BadGatewayException;
import com.sonatype.insight.test.LogOutput;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class HDSMaliciousUrlFetcherTest
{
  @Rule
  public LogOutput logOutput = new LogOutput(HDSMaliciousUrlFetcher.class);

  @Mock
  private HdsClient hdsClient;

  @Inject
  private HDSMaliciousUrlFetcher underTest;

  @Before
  public void setUp() {
    underTest = new HDSMaliciousUrlFetcher(hdsClient);
  }

  @Test
  public void testFetchMaliciousUrls() {
    InputStream expectedResponse = new ByteArrayInputStream("maliciousUrls".getBytes(StandardCharsets.UTF_8));
    when(hdsClient.get(InputStream.class, "rest/maliciousUrls/active/maven"))
        .thenReturn(expectedResponse);

    InputStream actualResponse = underTest.fetchMaliciousUrls(ZScalerSupportedFormat.MAVEN);

    assertEquals(expectedResponse, actualResponse);
    assertThat(logOutput).atDebugLevel().contains("Updating zScaler Malicious URLs for format: MAVEN");
  }

  @Test
  public void testFetchMaliciousUrls_badGatewayException() {
    when(hdsClient.get(InputStream.class, "rest/maliciousUrls/active/npm"))
        .thenThrow(new BadGatewayException("Bad Gateway"));

    InputStream inputStream = underTest.fetchMaliciousUrls(ZScalerSupportedFormat.NPM);

    assertThat(inputStream).isNull();
    assertThat(logOutput).atWarnLevel().contains("Failed to get zScaler malicious URLs");
  }
}
