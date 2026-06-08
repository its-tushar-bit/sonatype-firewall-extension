/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.guide.api.resource;

import java.util.List;
import java.util.Map;

import com.sonatype.guide.api.dto.RecommendationResponse;
import com.sonatype.guide.api.request.RecommendationRequest;
import com.sonatype.insight.brain.guide.api.dto.GuideRecommendationResult;
import com.sonatype.insight.brain.guide.api.dto.RecommendedVersionInfo;
import com.sonatype.insight.brain.guide.api.error.GuideApiException;
import com.sonatype.insight.brain.guide.api.error.GuideNotFoundException;
import com.sonatype.insight.brain.guide.core.SearchApiClient;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class GuideRecommendationsResourceTest
{
  private static final String PURL = "pkg:maven/org.apache.logging.log4j/log4j-core@2.14.1";

  @Mock
  private SearchApiClient searchApiClient;

  private GuideRecommendationsResource underTest;

  @Before
  public void setUp() {
    underTest = new GuideRecommendationsResource(searchApiClient);
  }

  @Test
  public void getRecommendations_returnsBadRequest_whenPurlIsNull() {
    RecommendationRequest request = new RecommendationRequest(null);

    assertThatThrownBy(() -> underTest.getRecommendations(request))
        .isInstanceOf(GuideApiException.class)
        .hasMessageContaining("Purl is required");
    verifyNoInteractions(searchApiClient);
  }

  @Test
  public void getRecommendations_returnsBadRequest_whenPurlIsBlank() {
    RecommendationRequest request = new RecommendationRequest("   ");

    assertThatThrownBy(() -> underTest.getRecommendations(request))
        .isInstanceOf(GuideApiException.class)
        .hasMessageContaining("Purl is required");
    verifyNoInteractions(searchApiClient);
  }

  @Test
  public void getRecommendations_propagatesNotFoundFromClient_withUpstreamMessage() throws Exception {
    RecommendationRequest request = new RecommendationRequest(PURL);
    when(searchApiClient.getRecommendations(PURL))
        .thenThrow(new GuideNotFoundException("Recommendations not found for PURL: " + PURL));

    assertThatThrownBy(() -> underTest.getRecommendations(request))
        .isInstanceOf(GuideNotFoundException.class)
        .hasMessage("Recommendations not found for PURL: " + PURL);
  }

  @Test
  public void getRecommendations_returnsResponse_whenClientReturnsData() throws Exception {
    RecommendationRequest request = new RecommendationRequest(PURL);
    GuideRecommendationResult expected = new GuideRecommendationResult(
        RecommendationResponse.Outcome.FOUND_RECOMMENDATIONS,
        new RecommendedVersionInfo("2.14.1", null, Map.of(), Map.of(), Map.of(), List.of(), 85, null),
        List.of(new RecommendedVersionInfo("2.21.1", null, null, null, Map.of(), List.of(), 99, null)));
    when(searchApiClient.getRecommendations(PURL)).thenReturn(expected);

    RecommendationResponse result = underTest.getRecommendations(request);

    assertThat(result).isSameAs(expected);
    verify(searchApiClient).getRecommendations(PURL);
  }
}
