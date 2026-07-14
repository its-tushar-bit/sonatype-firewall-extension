/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository.hosted;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Focused unit tests for the pathname-aware bom lookup used by the collapse gate. Kept in a
 * dedicated test class rather than the (heavy, integration-style)
 * {@link HostedComponentScanQueueConsumerTest} so the pure-function behaviour is exercised
 * without spinning up the queue consumer's DAO/HDS mock rig.
 */
public class HostedComponentScanQueueConsumerBomHashTest
{
  @Test
  public void extractBomHashForOuter_pathnameMatch_returnsMatchingEntrysHash() {
    // aaData[0] is an identified inner (actionpack-style scenario); the real outer lives at
    // aaData[1]. Pathname lookup must skip past aaData[0] and return the outer's hash.
    byte[] bom = ("{\"aaData\":["
        + "{\"hash\":\"inner_hash\",\"pathnames\":[\"gems/actionpack-5.2.0.gem\"]},"
        + "{\"hash\":\"outer_hash\",\"pathnames\":[\"gems/bundled-gem-app-1.0.0.gem\"]}"
        + "]}").getBytes();

    String result = HostedComponentScanQueueConsumer.extractBomHashForOuter(
        bom, "gems/bundled-gem-app-1.0.0.gem");

    assertThat(result).isEqualTo("outer_hash");
  }

  @Test
  public void extractBomHashForOuter_noPathnameMatch_fallsBackToAaData0Hash() {
    // Outer's pathname not present anywhere in aaData — recover to aaData[0] so callers that
    // downgrade further to outerComp.hash (via null override) still get a deterministic value.
    byte[] bom = ("{\"aaData\":["
        + "{\"hash\":\"first_hash\",\"pathnames\":[\"gems/foo-1.0.gem\"]},"
        + "{\"hash\":\"second_hash\",\"pathnames\":[\"gems/bar-1.0.gem\"]}"
        + "]}").getBytes();

    String result = HostedComponentScanQueueConsumer.extractBomHashForOuter(
        bom, "gems/does-not-exist.gem");

    assertThat(result).isEqualTo("first_hash");
  }

  @Test
  public void extractBomHashForOuter_nullOuterPathname_fallsBackToAaData0Hash() {
    // Preserves the plain extractBomOuterHash contract when the caller has no pathname signal.
    byte[] bom = ("{\"aaData\":[{\"hash\":\"only_hash\",\"pathnames\":[\"gems/foo.gem\"]}]}")
        .getBytes();

    String result = HostedComponentScanQueueConsumer.extractBomHashForOuter(bom, null);

    assertThat(result).isEqualTo("only_hash");
  }

  @Test
  public void extractBomHashForOuter_emptyOuterPathname_fallsBackToAaData0Hash() {
    byte[] bom = ("{\"aaData\":[{\"hash\":\"only_hash\",\"pathnames\":[\"gems/foo.gem\"]}]}")
        .getBytes();

    String result = HostedComponentScanQueueConsumer.extractBomHashForOuter(bom, "");

    assertThat(result).isEqualTo("only_hash");
  }

  @Test
  public void extractBomHashForOuter_entryWithoutPathnamesArray_skippedNotThrown() {
    // First entry has no pathnames field at all — must not NPE; walk continues to next entry.
    byte[] bom = ("{\"aaData\":["
        + "{\"hash\":\"first_hash\"},"
        + "{\"hash\":\"target_hash\",\"pathnames\":[\"gems/wanted.gem\"]}"
        + "]}").getBytes();

    String result = HostedComponentScanQueueConsumer.extractBomHashForOuter(
        bom, "gems/wanted.gem");

    assertThat(result).isEqualTo("target_hash");
  }

  @Test
  public void extractBomHashForOuter_unparseableJson_returnsNull() {
    byte[] garbage = "not-json".getBytes();
    assertThat(HostedComponentScanQueueConsumer.extractBomHashForOuter(garbage, "gems/foo.gem"))
        .isNull();
  }

  @Test
  public void extractBomHashForOuter_nullInput_returnsNull() {
    assertThat(HostedComponentScanQueueConsumer.extractBomHashForOuter(null, "gems/foo.gem"))
        .isNull();
  }

  @Test
  public void extractBomHashForOuter_emptyAaData_returnsNull() {
    byte[] bom = "{\"aaData\":[]}".getBytes();
    assertThat(HostedComponentScanQueueConsumer.extractBomHashForOuter(bom, "gems/foo.gem"))
        .isNull();
  }
}
