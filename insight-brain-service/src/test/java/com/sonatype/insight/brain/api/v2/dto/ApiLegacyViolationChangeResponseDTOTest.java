/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ApiLegacyViolationChangeResponseDTOTest
{
  @Test
  public void testSerializesChangedPolicyViolationCount() throws Exception {
    ApiLegacyViolationChangeResponseDTO dto = new ApiLegacyViolationChangeResponseDTO(7);
    String json = new ObjectMapper().writeValueAsString(dto);
    assertThat(json).isEqualTo("{\"changedPolicyViolationCount\":7}");
  }

  @Test
  public void testZeroSerializes() throws Exception {
    ApiLegacyViolationChangeResponseDTO dto = new ApiLegacyViolationChangeResponseDTO(0);
    assertThat(new ObjectMapper().writeValueAsString(dto)).isEqualTo("{\"changedPolicyViolationCount\":0}");
  }
}
