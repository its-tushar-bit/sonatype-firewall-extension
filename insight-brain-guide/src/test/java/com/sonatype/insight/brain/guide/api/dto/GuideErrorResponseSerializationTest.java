/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.guide.api.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class GuideErrorResponseSerializationTest
{

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  public void serializesToExpectedJson() throws Exception {
    GuideErrorResponse error = new GuideErrorResponse(false, "Component not found");

    String json = objectMapper.writeValueAsString(error);

    assertThat(json).isEqualTo("{\"success\":false,\"message\":\"Component not found\"}");
  }

  @Test
  public void serializesNullMessage() throws Exception {
    GuideErrorResponse error = new GuideErrorResponse(false, null);

    String json = objectMapper.writeValueAsString(error);

    assertThat(json).isEqualTo("{\"success\":false,\"message\":null}");
  }
}
