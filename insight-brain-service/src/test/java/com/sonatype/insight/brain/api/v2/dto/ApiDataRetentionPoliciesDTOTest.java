/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ApiDataRetentionPoliciesDTOTest
{
  @Test
  public void testDeserialization_EmptyObject() throws Exception {
    ApiDataRetentionPoliciesDTO dto = new ObjectMapper().readValue("{}", ApiDataRetentionPoliciesDTO.class);
    assertThat(dto.applicationReports).isNull();
    assertThat(dto.successMetrics).isNull();
  }
}
