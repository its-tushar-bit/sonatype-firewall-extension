/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ApiLegacyViolationStatusDTOTest
{
  private final ObjectMapper mapper = new ObjectMapper();

  @Test
  public void testSerialization_includesReadOnlyFields() throws Exception {
    ApiLegacyViolationStatusDTO dto = new ApiLegacyViolationStatusDTO();
    dto.enabled = Boolean.TRUE;
    dto.enabledInParent = Boolean.FALSE;
    dto.inheritedFromOrganizationName = "Root Org";
    dto.allowOverride = true;
    dto.allowChange = false;

    String json = mapper.writeValueAsString(dto);

    assertThat(json).contains("\"enabled\":true");
    assertThat(json).contains("\"enabledInParent\":false");
    assertThat(json).contains("\"inheritedFromOrganizationName\":\"Root Org\"");
    assertThat(json).contains("\"allowOverride\":true");
    assertThat(json).contains("\"allowChange\":false");
  }

  @Test
  public void testDeserialization_stripsReadOnlyFields() throws Exception {
    String json = "{\"enabled\":true,\"enabledInParent\":false,"
        + "\"inheritedFromOrganizationName\":\"Root Org\","
        + "\"allowOverride\":true,\"allowChange\":true}";

    ApiLegacyViolationStatusDTO dto = mapper.readValue(json, ApiLegacyViolationStatusDTO.class);

    assertThat(dto.enabled).isEqualTo(Boolean.TRUE);
    assertThat(dto.allowOverride).isTrue();
    assertThat(dto.enabledInParent).isNull();
    assertThat(dto.inheritedFromOrganizationName).isNull();
    assertThat(dto.allowChange).isFalse();
  }

  @Test
  public void testNullsOmittedWhenInheritedNameIsNull() throws Exception {
    ApiLegacyViolationStatusDTO dto = new ApiLegacyViolationStatusDTO();
    String json = mapper.writeValueAsString(dto);
    assertThat(json).doesNotContain("inheritedFromOrganizationName");
  }
}
