/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.violation;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class PolicyViolationLogDTOTest
{
  @Test
  public void testJsonSerialization_NullFieldsAreMissing() throws Exception {
    PolicyViolationLogDTO dto = new PolicyViolationLogDTO();
    String json = new ObjectMapper().writeValueAsString(dto);
    assertThat(json).isEqualTo("{}");
  }
}
