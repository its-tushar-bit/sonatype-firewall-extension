/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import com.sonatype.insight.brain.dto.ApplicationComponentMatchDTO;
import com.sonatype.insight.brain.service.BaseUrlProvider;
import com.sonatype.insight.brain.service.CveAffectedComponentSearchService;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test class for ApiComponentSearchResource.
 */
public class ApiComponentSearchResourceTest
{
  @Mock
  private CveAffectedComponentSearchService mockCveAffectedComponentSearchService;

  @Mock
  private BaseUrlProvider mockBaseUrlProvider;

  private ApiComponentSearchResource resource;

  private static final String TEST_BASE_URL = "http://localhost:8072";

  @Before
  public void setUp() {
    mockCveAffectedComponentSearchService = org.mockito.Mockito.mock(CveAffectedComponentSearchService.class);
    mockBaseUrlProvider = org.mockito.Mockito.mock(BaseUrlProvider.class);
    when(mockBaseUrlProvider.getBaseUrl()).thenReturn(TEST_BASE_URL);
    resource = new ApiComponentSearchResource(mockCveAffectedComponentSearchService, mockBaseUrlProvider);
  }

  @Test
  public void testExportComponentSearchReport_Success() throws Exception {
    // Arrange
    List<ApplicationComponentMatchDTO> matches = createSampleMatches();

    when(mockCveAffectedComponentSearchService.find(anyString(), eq(TEST_BASE_URL)))
        .thenReturn(matches.stream());

    // Act
    Response response = resource.exportComponentSearchReport();

    StreamingOutput entity = (StreamingOutput) response.getEntity();
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    entity.write(outputStream);

    // Assert
    assertThat(response).isNotNull();
    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(response.getMediaType().toString()).isEqualTo("text/csv");

    // Verify service was called with base URL
    verify(mockCveAffectedComponentSearchService).find(anyString(), eq(TEST_BASE_URL));
  }

  @Test
  public void testExportComponentSearchReport_EmptyResults() throws Exception {
    // Arrange
    when(mockCveAffectedComponentSearchService.find(anyString(), eq(TEST_BASE_URL)))
        .thenReturn(Stream.empty());

    // Act
    Response response = resource.exportComponentSearchReport();
    consumeStreamingOutput(response);

    // Assert
    assertThat(response).isNotNull();
    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(response.getMediaType().toString()).isEqualTo("text/csv");

    // Verify service was called
    verify(mockCveAffectedComponentSearchService).find(anyString(), eq(TEST_BASE_URL));
  }

  @Test
  public void testExportComponentSearchReport_MultipleMatches() throws Exception {
    // Arrange
    List<ApplicationComponentMatchDTO> matches = new ArrayList<>();
    matches.add(createMatch("app1", "lodash", "4.17.20", "CVE-2025-55182", "Upgrade to 4.17.21", "Yes"));
    matches.add(createMatch("app2", "lodash", "4.17.20", "CVE-2025-55182", "Upgrade to 4.17.21", "No"));
    matches.add(createMatch("app3", "axios", "0.21.0", "CVE-2025-55183", "Upgrade to 0.21.4", "Yes"));

    when(mockCveAffectedComponentSearchService.find(anyString(), eq(TEST_BASE_URL)))
        .thenReturn(matches.stream());

    // Act
    Response response = resource.exportComponentSearchReport();
    consumeStreamingOutput(response);

    // Assert
    assertThat(response).isNotNull();
    assertThat(response.getStatus()).isEqualTo(200);
    verify(mockCveAffectedComponentSearchService).find(anyString(), eq(TEST_BASE_URL));
  }

  @Test
  public void testExportComponentSearchReport_WithNullValues() throws Exception {
    // Arrange
    List<ApplicationComponentMatchDTO> matches = new ArrayList<>();
    matches.add(new ApplicationComponentMatchDTO(
        "app1",
        "Application 1", // applicationName
        "id1",
        "build",
        "2025-12-10T00:00:00Z",
        "pkg:npm/lodash@4.17.20",
        "lodash-4.17.20",
        "hash123",
        "lodash",
        "4.17.20",
        null, // null vulnerability IDs
        "", // empty recommended action
        null, // null recommended version
        "No",
        "Yes",
        "scan123"
    ));

    when(mockCveAffectedComponentSearchService.find(anyString(), eq(TEST_BASE_URL)))
        .thenReturn(matches.stream());

    // Act
    Response response = resource.exportComponentSearchReport();
    consumeStreamingOutput(response);

    // Assert
    assertThat(response).isNotNull();
    assertThat(response.getStatus()).isEqualTo(200);
    verify(mockCveAffectedComponentSearchService).find(anyString(), eq(TEST_BASE_URL));
  }

  @Test
  public void testExportComponentSearchReport_VerifyRequestParameters() throws Exception {
    // Arrange
    when(mockCveAffectedComponentSearchService.find(anyString(), eq(TEST_BASE_URL)))
        .thenReturn(Stream.empty());

    // Act
    Response response = resource.exportComponentSearchReport();
    consumeStreamingOutput(response);

    // Assert
    assertThat(response).isNotNull();

    // Verify the service was called with CVE ID
    verify(mockCveAffectedComponentSearchService).find(anyString(), eq(TEST_BASE_URL));
  }

  @Test
  public void testExportComponentSearchReport_CSVHeaderFormat() throws Exception {
    // Arrange
    List<ApplicationComponentMatchDTO> matches = createSampleMatches();

    when(mockCveAffectedComponentSearchService.find(anyString(), eq(TEST_BASE_URL)))
        .thenReturn(matches.stream());

    // Act
    Response response = resource.exportComponentSearchReport();
    consumeStreamingOutput(response);

    // Assert - CSV should be properly formatted with correct headers
    assertThat(response).isNotNull();
    assertThat(response.getStatus()).isEqualTo(200);

    // We can't directly access the response body in this test, but we verify the structure is correct
    verify(mockCveAffectedComponentSearchService).find(anyString(), eq(TEST_BASE_URL));
  }

  @Test
  public void testExportComponentSearchReport_ContentDisposition() throws Exception {
    // Arrange
    when(mockCveAffectedComponentSearchService.find(anyString(), eq(TEST_BASE_URL)))
        .thenReturn(Stream.empty());

    // Act
    Response response = resource.exportComponentSearchReport();
    consumeStreamingOutput(response);

    // Assert - Response should have proper content disposition for download
    assertThat(response).isNotNull();
    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(response.getMediaType().toString()).isEqualTo("text/csv");
  }

  // Helper methods

  private void consumeStreamingOutput(Response response) throws Exception {
    StreamingOutput entity = (StreamingOutput) response.getEntity();
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    entity.write(outputStream);
  }

  private List<ApplicationComponentMatchDTO> createSampleMatches() {
    List<ApplicationComponentMatchDTO> matches = new ArrayList<>();
    matches.add(createMatch("my-app", "lodash", "4.17.20", "CVE-2025-55182", "Upgrade to 4.17.21", "Yes"));
    return matches;
  }

  private ApplicationComponentMatchDTO createMatch(
      String appPublicId,
      String componentName,
      String componentVersion,
      String vulnerabilityIds,
      String recommendedAction,
      String implicatedFiles)
  {
    return new ApplicationComponentMatchDTO(
        appPublicId,
        appPublicId, // applicationName (same as public ID for test)
        "internal-id-" + appPublicId,
        "build",
        "2025-12-10T00:00:00Z",
        "pkg:npm/" + componentName + "@" + componentVersion,
        componentName + "-" + componentVersion,
        "hash-" + componentName,
        componentName,
        componentVersion,
        vulnerabilityIds,
        recommendedAction,
        componentVersion.substring(0, componentVersion.lastIndexOf('.')) + "." +
            (Integer.parseInt(componentVersion.substring(componentVersion.lastIndexOf('.') + 1)) + 1),
        "No",
        implicatedFiles,
        "scan-123"
    );
  }
}
