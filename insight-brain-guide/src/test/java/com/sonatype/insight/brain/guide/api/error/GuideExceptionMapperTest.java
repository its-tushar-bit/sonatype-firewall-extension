/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.guide.api.error;

import java.lang.reflect.Field;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sonatype.insight.brain.guide.api.dto.GuideErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class GuideExceptionMapperTest
{
  private static final String UUID_REGEX = "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}";

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Mock
  private HttpServletRequest httpRequest;

  private GuideExceptionMapper mapper;

  @Before
  public void setUp() throws Exception {
    when(httpRequest.getMethod()).thenReturn("GET");
    when(httpRequest.getRequestURI()).thenReturn("/api/v2/guide/components/detail");

    mapper = new GuideExceptionMapper();
    setRequest(mapper, httpRequest);
  }

  @Test
  public void clientError_returnsExceptionMessage_andSaasShape() throws Exception {
    Response response = mapper.toResponse(
        new GuideApiException(Response.Status.NOT_FOUND, "Vulnerability not found: CVE-1"));

    assertThat(response.getStatus()).isEqualTo(404);
    assertThat(response.getMediaType()).isEqualTo(MediaType.APPLICATION_JSON_TYPE);

    GuideErrorResponse body = (GuideErrorResponse) response.getEntity();
    assertThat(body.success()).isFalse();
    assertThat(body.message()).isEqualTo("Vulnerability not found: CVE-1");

    JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(body));
    assertThat(json.fieldNames()).toIterable().containsExactlyInAnyOrder("success", "message");
    assertThat(json.has("statusCode")).as("legacy field must not be present").isFalse();
  }

  @Test
  public void serverError_scrubsMessage_andEmbedsErrorLookupId() {
    Response response = mapper.toResponse(
        new GuideApiException(Response.Status.INTERNAL_SERVER_ERROR, "internal stack trace details"));

    assertThat(response.getStatus()).isEqualTo(500);

    GuideErrorResponse body = (GuideErrorResponse) response.getEntity();
    assertThat(body.success()).isFalse();
    assertThat(body.message())
        .matches("Internal Server Error \\(Error lookup ID: " + UUID_REGEX + "\\)")
        .doesNotContain("stack trace details");
  }

  // Reflection sets the @Context-injected `request` field directly, avoiding the cost of
  // booting a Jersey test container just to verify response-body construction. If the field
  // is renamed, this throws NoSuchFieldException — update the literal below to match.
  private static void setRequest(Object mapper, HttpServletRequest request) throws Exception {
    Field field = mapper.getClass().getDeclaredField("request");
    field.setAccessible(true);
    field.set(mapper, request);
  }
}
