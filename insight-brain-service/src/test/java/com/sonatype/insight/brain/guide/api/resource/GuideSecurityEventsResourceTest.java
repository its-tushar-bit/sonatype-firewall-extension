/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.guide.api.resource;

import java.util.List;
import java.util.Map;

import com.sonatype.guide.api.dto.AffectedComponentVersion;
import com.sonatype.guide.api.dto.ApiSearchResponse;
import com.sonatype.guide.api.dto.SecurityEventDetailDocument;
import com.sonatype.guide.api.dto.SecurityEventDocument;
import com.sonatype.insight.brain.guide.api.dto.GuideAffectedAsset;
import com.sonatype.insight.brain.guide.api.dto.GuideAffectedComponentVersion;
import com.sonatype.insight.brain.guide.api.dto.GuideAffectedComponentVersionRequest;
import com.sonatype.insight.brain.guide.api.dto.GuideAffectedComponentVersionSearchResponse;
import com.sonatype.insight.brain.guide.api.dto.GuideSecurityEventDetailDocument;
import com.sonatype.insight.brain.guide.api.dto.GuideSecurityEventDocument;
import com.sonatype.insight.brain.guide.api.dto.GuideSecurityEventSearchRequest;
import com.sonatype.insight.brain.guide.api.dto.GuideSecurityEventSearchResponse;
import com.sonatype.insight.brain.guide.api.error.GuideApiException;
import com.sonatype.insight.brain.guide.core.SearchApiClient;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link GuideSecurityEventsResource}.
 * <p>
 * Integration tests covering auth, license enforcement, and the full HTTP stack are in
 * {@link com.sonatype.insight.brain.guide.GuideSecurityEventsResourceIT}.
 */
@ExtendWith(MockitoExtension.class)
public class GuideSecurityEventsResourceTest
{
  @Mock
  private SearchApiClient searchApiClient;

  private GuideSecurityEventsResource underTest;

  @BeforeEach
  public void setUp() {
    underTest = new GuideSecurityEventsResource(searchApiClient);
  }

  @Test
  public void searchSecurityEvents_delegatesToClientWithMappedRequest() throws Exception {
    GuideSecurityEventDocument event = minimalEvent("SEC-2026-001");
    GuideSecurityEventSearchResponse upstream = new GuideSecurityEventSearchResponse(
        List.of(event), 1L, 0, 25, Map.of());
    when(searchApiClient.searchSecurityEvents(any(GuideSecurityEventSearchRequest.class)))
        .thenReturn(upstream);

    ApiSearchResponse<SecurityEventDocument> result = underTest.searchSecurityEvents(
        "log4j", 10, 50, "publishedDate", "desc",
        List.of("critical"), List.of("VULNERABILITY"), true, List.of("maven"));

    assertThat(result.hits()).containsExactly(event);
    ArgumentCaptor<GuideSecurityEventSearchRequest> captor =
        ArgumentCaptor.forClass(GuideSecurityEventSearchRequest.class);
    verify(searchApiClient).searchSecurityEvents(captor.capture());
    GuideSecurityEventSearchRequest request = captor.getValue();
    assertThat(request.query()).isEqualTo("log4j");
    assertThat(request.offset()).isEqualTo(10);
    assertThat(request.limit()).isEqualTo(50);
    assertThat(request.sortField()).isEqualTo("publishedDate");
    assertThat(request.sortOrder()).isEqualTo("desc");
    assertThat(request.severities()).containsExactly("critical");
    assertThat(request.threatTypes()).containsExactly("VULNERABILITY");
    assertThat(request.knownExploited()).isTrue();
    assertThat(request.affectedEcosystems()).containsExactly("maven");
  }

  @Test
  public void getSecurityEventById_missingId_returns400() {
    assertThatThrownBy(() -> underTest.getSecurityEventById(null))
        .isInstanceOf(GuideApiException.class)
        .hasMessageContaining("eventId is required")
        .extracting(e -> ((GuideApiException) e).getResponse().getStatus())
        .isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
    verifyNoInteractions(searchApiClient);
  }

  @Test
  public void getSecurityEventById_blankId_returns400() {
    assertThatThrownBy(() -> underTest.getSecurityEventById("   "))
        .isInstanceOf(GuideApiException.class)
        .hasMessageContaining("eventId is required");
    verifyNoInteractions(searchApiClient);
  }

  @Test
  public void getSecurityEventById_delegatesToClient() throws Exception {
    GuideSecurityEventDetailDocument upstream = minimalDetail("SEC-2026-001");
    when(searchApiClient.getSecurityEventById("SEC-2026-001")).thenReturn(upstream);

    SecurityEventDetailDocument result = underTest.getSecurityEventById("SEC-2026-001");

    assertThat(result).isSameAs(upstream);
    verify(searchApiClient).getSecurityEventById("SEC-2026-001");
  }

  @Test
  public void getSecurityEventAffectedComponents_missingId_returns400() {
    assertThatThrownBy(() -> underTest.getSecurityEventAffectedComponents(
        null, null, null, null, null, null))
            .isInstanceOf(GuideApiException.class)
            .hasMessageContaining("id is required")
            .extracting(e -> ((GuideApiException) e).getResponse().getStatus())
            .isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
    verifyNoInteractions(searchApiClient);
  }

  @Test
  public void getSecurityEventAffectedComponents_blankId_returns400() {
    assertThatThrownBy(() -> underTest.getSecurityEventAffectedComponents(
        "  ", null, null, null, null, null))
            .isInstanceOf(GuideApiException.class)
            .hasMessageContaining("id is required")
            .extracting(e -> ((GuideApiException) e).getResponse().getStatus())
            .isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
    verifyNoInteractions(searchApiClient);
  }

  @Test
  public void getSecurityEventAffectedComponents_delegatesToClientWithMappedRequest() throws Exception {
    GuideAffectedComponentVersion hit = new GuideAffectedComponentVersion(
        "npm", null, "left-pad", "1.3.0", "left-pad", null,
        Boolean.TRUE, List.of(new GuideAffectedAsset("tgz", null, Boolean.TRUE)));
    GuideAffectedComponentVersionSearchResponse upstream = new GuideAffectedComponentVersionSearchResponse(
        List.of(hit), 1L, 0, 25, Map.of());
    when(searchApiClient.getSecurityEventAffectedComponents(any(GuideAffectedComponentVersionRequest.class)))
        .thenReturn(upstream);

    ApiSearchResponse<AffectedComponentVersion> result = underTest.getSecurityEventAffectedComponents(
        "SEC-2026-001", "left-pad", 10, 25, "packageName", "asc");

    assertThat(result.hits()).containsExactly(hit);
    ArgumentCaptor<GuideAffectedComponentVersionRequest> captor =
        ArgumentCaptor.forClass(GuideAffectedComponentVersionRequest.class);
    verify(searchApiClient).getSecurityEventAffectedComponents(captor.capture());
    GuideAffectedComponentVersionRequest request = captor.getValue();
    assertThat(request.id()).isEqualTo("SEC-2026-001");
    assertThat(request.query()).isEqualTo("left-pad");
    assertThat(request.offset()).isEqualTo(10);
    assertThat(request.limit()).isEqualTo(25);
    assertThat(request.sortField()).isEqualTo("packageName");
    assertThat(request.sortOrder()).isEqualTo("asc");
  }

  private static GuideSecurityEventDocument minimalEvent(String eventId) {
    return new GuideSecurityEventDocument(
        eventId, "Log4j Critical Vulnerability", "Critical remote code execution in Log4j",
        null, null, "critical", "VULNERABILITY", true);
  }

  private static GuideSecurityEventDetailDocument minimalDetail(String eventId) {
    return new GuideSecurityEventDetailDocument(
        eventId, "Log4j Critical Vulnerability", "Critical remote code execution in Log4j",
        null, null, "critical", "VULNERABILITY", true,
        null, null, null,
        List.of("CVE-2021-44228"), List.of(), List.of(), List.of(), List.of("maven"), null);
  }
}
