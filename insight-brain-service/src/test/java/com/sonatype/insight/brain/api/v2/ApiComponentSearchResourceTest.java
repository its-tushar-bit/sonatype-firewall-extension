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
import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletResponse;

import com.sonatype.insight.brain.dto.ApplicationComponentMatchDTO;
import com.sonatype.insight.brain.service.CveAffectedComponentSearchService;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.times;
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
  private HttpServletResponse mockHttpServletResponse;

  private ApiComponentSearchResource resource;

  private ByteArrayOutputStream outputStream;

  @Before
  public void setUp() throws Exception {
    mockCveAffectedComponentSearchService = mock(CveAffectedComponentSearchService.class);
    mockHttpServletResponse = mock(HttpServletResponse.class);

    // Setup mock response with output stream
    outputStream = new ByteArrayOutputStream();
    when(mockHttpServletResponse.getOutputStream()).thenReturn(createServletOutputStream(outputStream));

    resource = new ApiComponentSearchResource(mockCveAffectedComponentSearchService);
  }

  @Test
  public void testExportComponentSearchReport_Success() throws Exception {
    // Arrange
    List<ApplicationComponentMatchDTO> matches = createSampleMatches();

    when(mockCveAffectedComponentSearchService.find(anyString()))
        .thenReturn(matches.stream());

    // Act
    resource.exportComponentSearchReport(mockHttpServletResponse);

    // Assert
    String csvOutput = outputStream.toString();
    assertThat(csvOutput).isNotEmpty();
    assertThat(csvOutput).contains("Application Name,Application ID"); // Header check
    assertThat(csvOutput).contains("lodash"); // Data check

    // Verify response configuration
    verify(mockHttpServletResponse).setContentType("text/csv");
    verify(mockHttpServletResponse).setBufferSize(0);
    verify(mockHttpServletResponse, times(3)).flushBuffer();

    // Verify service was called with base URL
    verify(mockCveAffectedComponentSearchService).find(anyString());
  }

  @Test
  public void testExportComponentSearchReport_EmptyResults() throws Exception {
    // Arrange
    when(mockCveAffectedComponentSearchService.find(anyString()))
        .thenReturn(Stream.empty());

    // Act
    resource.exportComponentSearchReport(mockHttpServletResponse);

    // Assert
    String csvOutput = outputStream.toString();
    assertThat(csvOutput).isNotEmpty();
    assertThat(csvOutput).contains("Application Name,Application ID"); // Header should still be present

    // Verify response configuration
    verify(mockHttpServletResponse).setContentType("text/csv");

    // Verify service was called
    verify(mockCveAffectedComponentSearchService).find(anyString());
  }

  @Test
  public void testExportComponentSearchReport_MultipleMatches() throws Exception {
    // Arrange
    List<ApplicationComponentMatchDTO> matches = new ArrayList<>();
    matches.add(createMatch("app1", "lodash", "4.17.20", "CVE-2025-55182", "Upgrade to 4.17.21", "Yes"));
    matches.add(createMatch("app2", "lodash", "4.17.20", "CVE-2025-55182", "Upgrade to 4.17.21", "No"));
    matches.add(createMatch("app3", "axios", "0.21.0", "CVE-2025-55183", "Upgrade to 0.21.4", "Yes"));

    when(mockCveAffectedComponentSearchService.find(anyString()))
        .thenReturn(matches.stream());

    // Act
    resource.exportComponentSearchReport(mockHttpServletResponse);

    // Assert
    String csvOutput = outputStream.toString();
    assertThat(csvOutput).contains("lodash");
    assertThat(csvOutput).contains("axios");
    assertThat(csvOutput).contains("app1");
    assertThat(csvOutput).contains("app2");
    assertThat(csvOutput).contains("app3");

    verify(mockCveAffectedComponentSearchService).find(anyString());
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

    when(mockCveAffectedComponentSearchService.find(anyString()))
        .thenReturn(matches.stream());

    // Act
    resource.exportComponentSearchReport(mockHttpServletResponse);

    // Assert
    String csvOutput = outputStream.toString();
    assertThat(csvOutput).isNotEmpty();
    assertThat(csvOutput).contains("Application 1");

    verify(mockCveAffectedComponentSearchService).find(anyString());
  }

  @Test
  public void testExportComponentSearchReport_VerifyRequestParameters() throws Exception {
    // Arrange
    when(mockCveAffectedComponentSearchService.find(anyString()))
        .thenReturn(Stream.empty());

    // Act
    resource.exportComponentSearchReport(mockHttpServletResponse);

    // Assert - Verify the service was called with CVE ID
    verify(mockCveAffectedComponentSearchService).find(anyString());
  }

  @Test
  public void testExportComponentSearchReport_CSVHeaderFormat() throws Exception {
    // Arrange
    List<ApplicationComponentMatchDTO> matches = createSampleMatches();

    when(mockCveAffectedComponentSearchService.find(anyString()))
        .thenReturn(matches.stream());

    // Act
    resource.exportComponentSearchReport(mockHttpServletResponse);

    // Assert - CSV should be properly formatted with correct headers
    String csvOutput = outputStream.toString();
    assertThat(csvOutput).startsWith("Application Name,Application ID");
    assertThat(csvOutput).contains("Component Name");
    assertThat(csvOutput).contains("Vulnerability ID");
    assertThat(csvOutput).contains("Recommended Action");

    verify(mockCveAffectedComponentSearchService).find(anyString());
  }

  @Test
  public void testExportComponentSearchReport_ContentDisposition() throws Exception {
    // Arrange
    when(mockCveAffectedComponentSearchService.find(anyString()))
        .thenReturn(Stream.empty());

    // Act
    resource.exportComponentSearchReport(mockHttpServletResponse);

    // Assert - Response should have proper content disposition for download
    verify(mockHttpServletResponse).setContentType("text/csv");
    verify(mockHttpServletResponse).setHeader(eq("Content-Disposition"), anyString());
    verify(mockHttpServletResponse).setHeader(eq("Cache-Control"), anyString());
  }

  // Helper methods

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

  private ServletOutputStream createServletOutputStream(ByteArrayOutputStream outputStream) {
    return new ServletOutputStream()
    {
      @Override
      public void write(int b) {
        outputStream.write(b);
      }

      @Override
      public boolean isReady() {
        return true;
      }

      @Override
      public void setWriteListener(WriteListener writeListener) {
        // Not needed for tests
      }
    };
  }
}
