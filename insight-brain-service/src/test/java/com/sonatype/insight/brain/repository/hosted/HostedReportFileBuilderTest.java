/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository.hosted;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sonatype.insight.brain.model.policy.RepositoryPolicyViolation;
import com.sonatype.insight.brain.model.repository.RepositoryComponent;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class HostedReportFileBuilderTest
{
  private static final ObjectMapper MAPPER = new ObjectMapper();

  private static RepositoryComponent component(String hash) {
    RepositoryComponent c = new RepositoryComponent();
    c.setHash(hash);
    c.setPathname("com/example/lib-1.0.jar");
    return c;
  }

  private static RepositoryPolicyViolation violation(String policyId, String policyName, int threatLevel) {
    RepositoryPolicyViolation v = new RepositoryPolicyViolation();
    v.setId("vid-" + policyId);
    v.setPolicyId(policyId);
    v.setPolicyName(policyName);
    v.setThreatLevel(threatLevel);
    v.setWaived(false);
    return v;
  }

  @Test
  public void buildPolicyThreats_multipleViolations_groupUsesHighestThreatViolation() throws Exception {
    RepositoryComponent comp = component("abc123");
    RepositoryPolicyViolation low = violation("pol-low", "Low Policy", 3);
    RepositoryPolicyViolation high = violation("pol-high", "High Policy", 10);
    RepositoryPolicyViolation mid = violation("pol-mid", "Mid Policy", 7);

    byte[] result = HostedReportFileBuilder.build("policythreats.json", comp, List.of(low, high, mid));

    JsonNode root = MAPPER.readTree(result);
    JsonNode group = root.path("aaData").get(0);

    assertThat(group.path("policyId").asText()).isEqualTo("pol-high");
    assertThat(group.path("policyName").asText()).isEqualTo("High Policy");
    assertThat(group.path("policyThreatLevel").asInt()).isEqualTo(10);
  }

  @Test
  public void buildPolicyThreats_singleViolation_groupUseThatViolation() throws Exception {
    RepositoryComponent comp = component("abc123");
    RepositoryPolicyViolation only = violation("pol-only", "Only Policy", 5);

    byte[] result = HostedReportFileBuilder.build("policythreats.json", comp, List.of(only));

    JsonNode root = MAPPER.readTree(result);
    JsonNode group = root.path("aaData").get(0);

    assertThat(group.path("policyId").asText()).isEqualTo("pol-only");
    assertThat(group.path("policyThreatLevel").asInt()).isEqualTo(5);
  }

  @Test
  public void buildPolicyThreats_allViolationsContainedInGroup() throws Exception {
    RepositoryComponent comp = component("abc123");
    RepositoryPolicyViolation v1 = violation("pol-a", "Policy A", 3);
    RepositoryPolicyViolation v2 = violation("pol-b", "Policy B", 10);

    byte[] result = HostedReportFileBuilder.build("policythreats.json", comp, List.of(v1, v2));

    JsonNode root = MAPPER.readTree(result);
    JsonNode group = root.path("aaData").get(0);

    assertThat(group.path("allViolations").size()).isEqualTo(2);
    assertThat(group.path("activeViolations").size()).isEqualTo(2);
    assertThat(group.path("waivedViolations").size()).isEqualTo(0);
  }

  @Test
  public void buildPolicyThreats_noViolations_emptyAaData() throws Exception {
    RepositoryComponent comp = component("abc123");

    byte[] result = HostedReportFileBuilder.build("policythreats.json", comp, List.of());

    JsonNode root = MAPPER.readTree(result);
    assertThat(root.path("aaData").size()).isEqualTo(0);
  }
}
