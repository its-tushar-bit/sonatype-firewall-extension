/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.stream.Stream;
import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletResponse;

import com.sonatype.insight.brain.dto.ApplicationComponentMatchDTO;
import com.sonatype.insight.brain.service.BaseUrlProvider;
import com.sonatype.insight.brain.service.CveAffectedComponentSearchService;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Test that verifies keep-alive spaces are written to CSV during slow processing.
 */
@RunWith(MockitoJUnitRunner.class)
public class ApiComponentSearchResourceKeepAliveTest
{
  private static final long KEEP_ALIVE_INTERVAL_MS = 200; // Trigger keep-alive after 200ms of inactivity

  private static final long KEEP_ALIVE_CHECK_INTERVAL_MS = 100; // Check every 100ms

  // Test DTOs with specific CSV output
  private static final ApplicationComponentMatchDTO DTO_1 = createTestDto("app-1", "Record1", "id-1");

  private static final ApplicationComponentMatchDTO DTO_2 = createTestDto("app-2", "Record2", "id-2");

  private static final ApplicationComponentMatchDTO DTO_3 = createTestDto("app-3", "Record3", "id-3");

  @Mock
  private CveAffectedComponentSearchService cveAffectedComponentSearchService;

  @Mock
  private BaseUrlProvider baseUrlProvider;

  @Mock
  private HttpServletResponse mockHttpServletResponse;

  private ApiComponentSearchResource resource;

  private ByteArrayOutputStream outputStream;

  private static ApplicationComponentMatchDTO createTestDto(String appId, String appName, String reportId) {
    ApplicationComponentMatchDTO dto = new ApplicationComponentMatchDTO(
        appId,                          // applicationPublicId
        appName,                        // applicationName
        appId,                          // applicationInternalId
        null,                           // stage
        "2024-12-11T10:00:00Z",        // evaluationDate
        null,                           // packageUrl
        "com.example:lib",             // componentDisplayName
        null,                           // hash
        null,                           // matchedName
        "1.0.0",                       // matchedVersion
        "CVE-2025-55182",              // vulnerabilityIds
        "Upgrade",                      // recommendedAction
        "2.0.0",                       // recommendedVersion
        "false",                        // activeWaiver
        "src/Main.java",               // implicatedFiles
        reportId                        // reportId
    );
    dto.setBaseUrl("http://localhost:8070");
    return dto;
  }

  @Before
  public void setup() throws Exception {
    resource = new ApiComponentSearchResource(cveAffectedComponentSearchService, baseUrlProvider);
    when(baseUrlProvider.getBaseUrl()).thenReturn("http://localhost:8070");

    outputStream = new ByteArrayOutputStream();
    when(mockHttpServletResponse.getOutputStream()).thenReturn(createServletOutputStream(outputStream));
  }

  @Test
  public void testKeepAliveSpacesWrittenDuringSlowProcessing() throws Exception {
    // Create test data with different timing scenarios
    // Final record with null dto represents EOF delay without emitting data
    List<TimedRecord> records = List.of(
        new TimedRecord(DTO_1, 0),      // Immediate
        new TimedRecord(DTO_2, 100),    // 100ms later - no keep-alive (< 200ms threshold)
        new TimedRecord(DTO_3, 300),    // 300ms later - 1 keep-alive space
        new TimedRecord(null, 600)      // 600ms EOF delay - 3 keep-alive spaces on Record3
    );

    when(cveAffectedComponentSearchService.find(eq("CVE-2025-55182"), any()))
        .thenReturn(createTimedStream(records));

    resource.streamCsvReport(mockHttpServletResponse, KEEP_ALIVE_INTERVAL_MS, KEEP_ALIVE_CHECK_INTERVAL_MS);

    String actualOutput = outputStream.toString("UTF-8");
    String[] lines = actualOutput.split("\r\n");

    // Verify we have exactly header + 3 records (no empty lines)
    assertThat(lines)
        .as("Should have exactly 4 lines: header + 3 records")
        .hasSize(4);

    assertThat(lines[0])
        .as("Should have header")
        .contains("Application Name,Application ID");

    assertThat(lines[1])
        .as("Should have Record1")
        .contains("Record1,app-1");

    assertThat(lines[2])
        .as("Should have Record2")
        .contains("Record2,app-2");

    assertThat(lines[3])
        .as("Should have Record3")
        .contains("Record3,app-3");

    // Verify keep-alive behavior on each line
    // Spaces accumulate DURING the wait before the NEXT line is written
    int spacesOnHeader = countTrailingSpaces(lines[0]);
    int spacesOnRecord1 = countTrailingSpaces(lines[1]);
    int spacesOnRecord2 = countTrailingSpaces(lines[2]);
    int spacesOnRecord3 = countTrailingSpaces(lines[3]);

    assertThat(spacesOnHeader)
        .as("Header followed by immediate Record1 (0ms wait), should have no keep-alive spaces")
        .isEqualTo(0);

    assertThat(spacesOnRecord1)
        .as("Record1 followed by Record2 after 100ms wait (< 200ms threshold), should have no keep-alive spaces")
        .isEqualTo(0);

    assertThat(spacesOnRecord2)
        .as("Record2 followed by Record3 after 300ms wait (~1 keep-alive cycle), should have 1-2 spaces")
        .isBetween(1, 2);

    assertThat(spacesOnRecord3)
        .as("Record3 followed by 600ms final wait (~3 keep-alive cycles), should have 2-3 spaces")
        .isBetween(2, 3);
  }

  /**
   * Counts trailing spaces after the last comma in a CSV line.
   */
  private int countTrailingSpaces(String line) {
    if (!line.contains(",")) {
      return 0;
    }

    int lastComma = line.lastIndexOf(',');
    int count = 0;
    for (int i = lastComma + 1; i < line.length(); i++) {
      if (line.charAt(i) == ' ') {
        count++;
      }
    }
    return count;
  }

  /**
   * Creates a stream that emits records with specified delays. Records with null dto cause delays but emit nothing (for
   * EOF simulation).
   */
  private Stream<ApplicationComponentMatchDTO> createTimedStream(List<TimedRecord> records) {
    return records.stream().flatMap(record -> {
      // Sleep for the specified delay
      if (record.delayMs > 0) {
        try {
          Thread.sleep(record.delayMs);
        }
        catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
      }

      // Return data or empty stream
      if (record.dto != null) {
        return Stream.of(record.dto);
      }
      else {
        // used for EOF, with potential delay prior
        return Stream.empty();
      }
    });
  }

  /**
   * Record with timing information. Null dto represents EOF delay without emitting data.
   */
  private record TimedRecord(ApplicationComponentMatchDTO dto, long delayMs) { }

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
