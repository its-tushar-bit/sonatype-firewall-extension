/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository.hosted;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class HostedEvaluationResultTest
{
  private ObjectMapper objectMapper;

  @Before
  public void setUp() {
    objectMapper = new ObjectMapper();
    objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
  }

  @Test
  public void jsonRoundTrip_blockedResult_preservesAllFields() throws Exception {
    HostedEvaluationResult original = new HostedEvaluationResult(
        true,
        "FAIL",
        9,
        "https://iq.example.com/assets/index.html#/hostedRepos/org-1/mon-1/components?repositoryPublicId=maven-releases",
        List.of(
            new HostedBlockingViolation("Critical Security Policy", "Critical CVSS",
                "Component contains a critical security vulnerability.",
                "pkg:maven/com.acme/internal-lib@1.2.3")),
        "nxrm-upload-789",
        "iq-component-abc");

    String json = objectMapper.writeValueAsString(original);
    HostedEvaluationResult roundTripped = objectMapper.readValue(json, HostedEvaluationResult.class);

    assertThat(roundTripped).isEqualTo(original);
  }

  @Test
  public void jsonRoundTrip_allowedResult_preservesAllFields() throws Exception {
    HostedEvaluationResult original = new HostedEvaluationResult(
        false,
        null,
        0,
        "https://iq.example.com/assets/index.html#/hostedRepos/org-1/mon-1/components?repositoryPublicId=maven-releases",
        List.of(),
        "nxrm-upload-790",
        "iq-component-def");

    String json = objectMapper.writeValueAsString(original);
    HostedEvaluationResult roundTripped = objectMapper.readValue(json, HostedEvaluationResult.class);

    assertThat(roundTripped).isEqualTo(original);
  }

  @Test
  public void constructor_withNullViolationList_treatsAsEmpty() {
    HostedEvaluationResult result = new HostedEvaluationResult(
        false, null, 0, "https://iq.example.com/report/1", null, "corr-1", "comp-1");

    assertThat(result.blockingViolations()).isEmpty();
  }

  @Test
  public void constructor_listContainingNullElements_silentlyDropsThem() {
    // Documents the constructor's null-element contract: a partially-populated source list
    // should not abort the entire response (CLM-37961 hardening pattern). Null entries are
    // filtered out; the response still carries the well-formed violations.
    List<HostedBlockingViolation> sourceWithNulls = new ArrayList<>();
    sourceWithNulls.add(null);
    sourceWithNulls.add(new HostedBlockingViolation("Critical Security Policy", "CVSS", "reason",
        "pkg:maven/a/b@1"));
    sourceWithNulls.add(null);

    HostedEvaluationResult result = new HostedEvaluationResult(
        true, "FAIL", 9, "https://iq.example.com/report/1", sourceWithNulls, "corr-1", "comp-1");

    assertThat(result.blockingViolations()).hasSize(1);
    assertThat(result.blockingViolations().get(0).policyName()).isEqualTo("Critical Security Policy");
  }

  @Test
  public void constructor_violationListIsDefensivelyCopied() {
    List<HostedBlockingViolation> mutableSource = new ArrayList<>();
    mutableSource.add(new HostedBlockingViolation("p", "c", "r", "pkg:maven/a/b@1"));

    HostedEvaluationResult result = new HostedEvaluationResult(
        true, "FAIL", 9, "https://iq.example.com/report/1", mutableSource, "corr-1", "comp-1");

    // Mutating the source after construction must not mutate the record's list.
    mutableSource.add(new HostedBlockingViolation("p2", "c2", "r2", "pkg:maven/x/y@1"));
    assertThat(result.blockingViolations()).hasSize(1);

    // The returned list is itself unmodifiable (backed by List.copyOf).
    assertThatThrownBy(() -> result.blockingViolations()
        .add(
            new HostedBlockingViolation("p3", "c3", "r3", "pkg:maven/z/w@1")))
                .isInstanceOf(UnsupportedOperationException.class);
  }
}
