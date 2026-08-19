/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto.githubapp;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link Manifest} JSON serialization behavior.
 * <p>
 * GitHub's App Manifest API rejects {@code null} values for {@code hook_attributes} and
 * {@code default_events} — these fields must be omitted entirely when not present. These tests
 * verify that the {@code @JsonInclude(NON_NULL)} annotation on {@link Manifest} correctly
 * excludes null fields from serialization.
 */
public class ManifestTest
{
  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  public void testSerialization_nullHookAttributesAndDefaultEvents_omitsFields() throws Exception {
    Manifest manifest = new Manifest(
        "Test App",
        "https://example.com",
        "https://example.com/redirect",
        "https://example.com/setup",
        List.of("https://example.com/callback"),
        true,
        "Test description",
        false,
        Map.of("contents", "read"),
        true,
        null, // hook_attributes
        null // default_events
    );

    String json = objectMapper.writeValueAsString(manifest);

    assertThat(json).doesNotContain("hook_attributes");
    assertThat(json).doesNotContain("default_events");
  }

  @Test
  public void testSerialization_withHookAttributesAndDefaultEvents_includesFields() throws Exception {
    Manifest manifest = new Manifest(
        "Test App",
        "https://example.com",
        "https://example.com/redirect",
        "https://example.com/setup",
        List.of("https://example.com/callback"),
        true,
        "Test description",
        false,
        Map.of("contents", "read"),
        true,
        new Manifest.HookAttributes("https://relay.example.com/webhook", true),
        List.of("pull_request", "push"));

    String json = objectMapper.writeValueAsString(manifest);

    assertThat(json).contains("hook_attributes");
    assertThat(json).contains("default_events");
    assertThat(json).contains("https://relay.example.com/webhook");
    assertThat(json).contains("pull_request");
  }
}
