/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ApiCrossStageViolationDTOV2Test
{
  private final ObjectMapper mapper = new ObjectMapper();

  @Test
  public void hrcIdOmittedFromJsonWhenNull() throws Exception {
    ApiCrossStageViolationDTOV2 dto = new ApiCrossStageViolationDTOV2();
    dto.applicationPublicId = "app-1";
    dto.applicationName = "App One";
    dto.organizationName = "Org One";

    String json = mapper.writeValueAsString(dto);

    assertThat(json).doesNotContain("hrcId");
    assertThat(json).contains("\"applicationPublicId\":\"app-1\"");
  }

  @Test
  public void hrcIdSerializedWhenPresent() throws Exception {
    ApiCrossStageViolationDTOV2 dto = new ApiCrossStageViolationDTOV2();
    dto.hrcId = "hrc-42";

    String json = mapper.writeValueAsString(dto);

    assertThat(json).contains("\"hrcId\":\"hrc-42\"");
  }
}
