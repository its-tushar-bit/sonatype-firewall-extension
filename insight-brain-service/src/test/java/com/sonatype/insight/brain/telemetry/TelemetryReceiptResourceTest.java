/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.telemetry.TelemetryReceiptService.TelemetryReceiptDTO;
import com.sonatype.insight.brain.telemetry.TelemetryReceiptService.TelemetryReceiptsDTO;
import jakarta.ws.rs.core.Response;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public class TelemetryReceiptResourceTest
    extends AbstractComponentTest
{
  @Mock
  private TelemetryReceiptService mockReceiptService;

  private TelemetryReceiptResource telemetryReceiptResource;

  @Before
  public void setup() {
    telemetryReceiptResource = new TelemetryReceiptResource(mockReceiptService);
  }

  @Test
  public void testGetTelemetryReceipts() {
    // Create mock receipts DTO
    TelemetryReceiptsDTO mockReceiptsDTO = createMockReceiptsDTO();
    when(mockReceiptService.getReceipts(List.of())).thenReturn(mockReceiptsDTO);

    // Call the endpoint
    Response response = telemetryReceiptResource.getTelemetryReceipts("");

    // Verify response
    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(response.getHeaders().get("Cache-Control")).contains("no-cache");

    // Verify response entity is a JSON string
    String jsonResponse = (String) response.getEntity();
    assertThat(jsonResponse).isNotNull();

    // Parse and verify JSON content
    JsonObject json = JsonParser.parseString(jsonResponse).getAsJsonObject();
    assertThat(json.has("document")).isTrue();
    assertThat(json.get("document").getAsString()).isEqualTo("Telemetry Receipts");
    assertThat(json.has("revision")).isTrue();
    assertThat(json.has("requestTime")).isTrue();
    assertThat(json.has("receipts")).isTrue();
    assertThat(json.getAsJsonArray("receipts").size()).isEqualTo(1);
  }

  @Test
  public void testDisableTelemetryReceipts() {
    // Call the endpoint
    String result = telemetryReceiptResource.disableTelemetryReceipts();

    // Verify service was called
    verify(mockReceiptService).disable();

    // Verify response
    assertThat(result).isEqualTo("Telemetry receipts disabled");
  }

  @Test
  public void testEnableTelemetryReceipts_DefaultValues() {
    // Set up mock service
    when(mockReceiptService.enable(anyInt())).thenReturn(1);

    // Call the endpoint with default values
    String result = telemetryReceiptResource.enableTelemetryReceipts(1);

    // Verify service was called with correct parameters
    verify(mockReceiptService).enable(1);

    // Verify response
    assertThat(result).isEqualTo("Telemetry receipts enabled for 1 hour(s)");
  }

  /**
   * Helper method to create a mock TelemetryReceiptsDTO for testing
   */
  private TelemetryReceiptsDTO createMockReceiptsDTO() {
    LocalDateTime now = LocalDateTime.now();

    // Create a mock receipt
    TelemetryReceiptDTO receiptDTO = mock(TelemetryReceiptDTO.class);
    when(receiptDTO.submitTime()).thenReturn(now);
    when(receiptDTO.queueTimeMs()).thenReturn(100L);
    when(receiptDTO.httpTimeMs()).thenReturn(200L);
    when(receiptDTO.errorMessage()).thenReturn(null);

    // Create the receipts DTO
    List<TelemetryReceiptDTO> receipts = Collections.singletonList(receiptDTO);
    TelemetryReceiptsDTO receiptsDTO = mock(TelemetryReceiptsDTO.class);
    when(receiptsDTO.captureStartTime()).thenReturn(now.minusHours(1));
    when(receiptsDTO.captureExpirationTime()).thenReturn(now.plusHours(1));
    when(receiptsDTO.isLocalEnv()).thenReturn(true);
    when(receiptsDTO.isUsingProdHds()).thenReturn(false);
    when(receiptsDTO.telemetryReceipts()).thenReturn(receipts);

    return receiptsDTO;
  }
}
