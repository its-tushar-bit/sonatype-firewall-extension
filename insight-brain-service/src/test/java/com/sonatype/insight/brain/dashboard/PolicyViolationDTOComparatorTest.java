/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import com.google.common.collect.Lists;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class PolicyViolationDTOComparatorTest
{
  @Test
  public void testPolicyViolationDTOComparison() {
    PolicyViolationDTO dto9AA = buildPolicyViolationDTO(9, "A", "A", null, null, null, null);
    PolicyViolationDTO dto8AA = buildPolicyViolationDTO(8, "A", "A", null, null, null, null);
    PolicyViolationDTO dto8BA = buildPolicyViolationDTO(8, "B", "A", null, null, null, null);
    PolicyViolationDTO dto8BB = buildPolicyViolationDTO(8, "B", "B", null, null, null, null);
    PolicyViolationDTO dto7AAAAA = buildPolicyViolationDTO(7, "A", "A", "A", "A", "A", null);
    PolicyViolationDTO dto7AAAAB = buildPolicyViolationDTO(7, "A", "A", "A", "A", "B", null);
    PolicyViolationDTO dto7AAA_NULL_A = buildPolicyViolationDTO(7, "A", "A", "A", null, "A", null);
    PolicyViolationDTO dto7AAA_NULL_B = buildPolicyViolationDTO(7, "A", "A", "A", null, "B", null);
    PolicyViolationDTO dto7BAAAB = buildPolicyViolationDTO(7, "B", "A", "A", "A", "B", null);
    PolicyViolationDTO dto7BAACA = buildPolicyViolationDTO(7, "B", "A", "A", "C", "A", null);
    PolicyViolationDTO dto7CCCCC_A = buildPolicyViolationDTO(7, "C", "C", "C", "C", "C", "A");
    PolicyViolationDTO dto7CCCCC_BB = buildPolicyViolationDTO(7, "C", "C", "C", "C", "C", "BB");
    PolicyViolationDTO dto7CCCCC_B = buildPolicyViolationDTO(7, "C", "C", "C", "C", "C", "B");
    PolicyViolationDTO dto7CCCCC_BA = buildPolicyViolationDTO(7, "C", "C", "C", "C", "C", "BA");

    List<PolicyViolationDTO> sorted = Lists.newArrayList(dto7CCCCC_A, dto7AAA_NULL_A, dto7BAACA, dto7CCCCC_BB, dto8BA,
        dto7BAAAB, dto9AA, dto7AAAAB, dto7CCCCC_B, dto7AAA_NULL_B, dto7CCCCC_BA, dto8AA, dto8BB, dto7AAAAA);

    Collections.sort(sorted, new PolicyViolationDTOComparator());

    List<PolicyViolationDTO> expected = Lists.newArrayList(dto9AA, dto8AA, dto8BA, dto8BB, dto7AAAAA, dto7AAAAB,
        dto7AAA_NULL_A, dto7AAA_NULL_B, dto7BAAAB, dto7BAACA, dto7CCCCC_A, dto7CCCCC_B, dto7CCCCC_BA, dto7CCCCC_BB);

    assertThat(sorted, is(expected));
  }

  private PolicyViolationDTO buildPolicyViolationDTO(int threatLevel, String policyName, String applicationName,
      String groupId, String artifactId, String version, String hash)
  {
    PolicyViolationDTO dto = new PolicyViolationDTO();
    dto.id = UUID.randomUUID().toString();
    dto.policyName = policyName;
    dto.applicationName = applicationName;
    dto.threatLevel = threatLevel;
    dto.groupId = groupId;
    dto.artifactId = artifactId;
    dto.version = version;
    dto.hash = hash;
    return dto;
  }
}
