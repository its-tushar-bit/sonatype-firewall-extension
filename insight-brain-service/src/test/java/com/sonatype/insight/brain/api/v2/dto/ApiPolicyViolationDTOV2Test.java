/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto;

import java.util.Date;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ApiPolicyViolationDTOV2Test
{
  @Test
  public void testSerialize_IncludesOpenWaiveFixLegacyTimes() throws Exception {
    ApiPolicyViolationDTOV2 dto = new ApiPolicyViolationDTOV2();
    dto.policyId = "policyId";
    dto.openTime = new Date(0);
    dto.waiveTime = new Date(2);
    dto.fixTime = new Date(3);
    dto.legacyViolationTime = new Date(1);
    ObjectMapper objectMapper = new ObjectMapper();

    String result = objectMapper.writeValueAsString(dto);

    JsonNode jsonNode = objectMapper.readTree(result);
    assertThat(jsonNode.path("policyId").asText()).isEqualTo("policyId");
    assertThat(jsonNode.path("openTime").asText()).isEqualTo("1970-01-01T00:00:00.000+0000");
    assertThat(jsonNode.path("waiveTime").asText()).isEqualTo("1970-01-01T00:00:00.002+0000");
    assertThat(jsonNode.path("fixTime").asText()).isEqualTo("1970-01-01T00:00:00.003+0000");
    assertThat(jsonNode.path("legacyViolationTime").asText()).isEqualTo("1970-01-01T00:00:00.001+0000");
  }

  @Test
  public void testSerialize_ExcludesNullOpenWaiveFixLegacyTimes() throws Exception {
    ApiPolicyViolationDTOV2 dto = new ApiPolicyViolationDTOV2();
    dto.policyId = "policyId";
    dto.openTime = null;
    dto.waiveTime = null;
    dto.fixTime = null;
    dto.legacyViolationTime = null;
    ObjectMapper objectMapper = new ObjectMapper();

    String result = objectMapper.writeValueAsString(dto);

    JsonNode jsonNode = objectMapper.readTree(result);
    assertThat(jsonNode.path("policyId").asText()).isEqualTo("policyId");
    assertThat(jsonNode.has("openTime")).isFalse();
    assertThat(jsonNode.has("waiveTime")).isFalse();
    assertThat(jsonNode.has("fixTime")).isFalse();
    assertThat(jsonNode.has("legacyViolationTime")).isFalse();
  }
}
