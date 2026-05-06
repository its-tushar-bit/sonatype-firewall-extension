/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.mcp;

import java.util.Map;

import com.sonatype.insight.brain.hds.HdsClient;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SearchApiClientImplTest
{
  private static final String PURL = "pkg:maven/org.example/lib@1.0.0";

  @Mock
  private HdsClient hdsClient;

  private SearchApiClientImpl underTest;

  @Before
  public void setUp() {
    underTest = new SearchApiClientImpl(hdsClient);
  }

  @Test
  public void testGetComponentByPurl_delegatesToHdsGet() {
    when(hdsClient.get(String.class, "rest/search/components/detail", Map.of("purl", PURL)))
        .thenReturn("{\"component\":\"data\"}");

    String result = underTest.getComponentByPurl(PURL);

    assertThat(result).isEqualTo("{\"component\":\"data\"}");
    verify(hdsClient).get(String.class, "rest/search/components/detail", Map.of("purl", PURL));
  }

  @Test
  public void testGetLatestComponentVersion_delegatesToHdsPost() {
    when(hdsClient.post(String.class, "rest/search/components/latest-version", Map.of("purl", PURL)))
        .thenReturn("{\"latestVersion\":\"2.0.0\"}");

    String result = underTest.getLatestComponentVersion(PURL);

    assertThat(result).isEqualTo("{\"latestVersion\":\"2.0.0\"}");
    verify(hdsClient).post(String.class, "rest/search/components/latest-version", Map.of("purl", PURL));
  }

  @Test
  public void testGetRecommendations_delegatesToHdsPost() {
    when(hdsClient.post(String.class, "rest/search/recommendations", Map.of("purl", PURL)))
        .thenReturn("{\"recommendations\":[]}");

    String result = underTest.getRecommendations(PURL);

    assertThat(result).isEqualTo("{\"recommendations\":[]}");
    verify(hdsClient).post(String.class, "rest/search/recommendations", Map.of("purl", PURL));
  }

  @Test
  public void testGetComponentByPurl_propagatesHdsExceptions() {
    when(hdsClient.get(String.class, "rest/search/components/detail", Map.of("purl", PURL)))
        .thenThrow(new RuntimeException("Connection refused"));

    assertThatThrownBy(() -> underTest.getComponentByPurl(PURL))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("Connection refused");
  }
}
