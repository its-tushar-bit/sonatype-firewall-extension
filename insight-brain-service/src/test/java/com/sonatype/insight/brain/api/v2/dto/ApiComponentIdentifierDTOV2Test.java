/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A compatibility test to highlight when changes to the internal {@link ComponentIdentifier} break the public API.
 *
 * Since we are exposing the non-public {@link ComponentIdentifier} coordinates map to the public API, we need to make
 * sure the
 * names (keys) of the coordinates don't change. If they do we'll need to correct, potentially by transforming them.
 *
 * NOTE: Don't use the {@link ComponentIdentifier} constants that define the names (keys) in order to validate that
 * expectations
 * of API clients have not changed.
 */
public class ApiComponentIdentifierDTOV2Test
{
  @Test
  public void testMavenComponentIdentifierNamesAreAsExpected() {
    ComponentIdentifier identifier = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "e1");
    ApiComponentIdentifierDTOV2 apiComponentIdentifier = ApiComponentIdentifierDTOV2
        .fromComponentIdentifier(identifier);

    assertThat(apiComponentIdentifier.getFormat()).isEqualTo("maven");
    assertThat(apiComponentIdentifier.getCoordinates().get("groupId")).isEqualTo("g1");
    assertThat(apiComponentIdentifier.getCoordinates().get("artifactId")).isEqualTo("a1");
    assertThat(apiComponentIdentifier.getCoordinates().get("version")).isEqualTo("v1");
    assertThat(apiComponentIdentifier.getCoordinates().get("classifier")).isEqualTo("c1");
    assertThat(apiComponentIdentifier.getCoordinates().get("extension")).isEqualTo("e1");
  }

  @Test
  public void testNuGetComponentIdentifierNamesAreAsExpected() {
    ComponentIdentifier identifier = ComponentIdentifier.createNugetCoordinates("p1", "v1");
    ApiComponentIdentifierDTOV2 apiComponentIdentifier = ApiComponentIdentifierDTOV2
        .fromComponentIdentifier(identifier);

    assertThat(apiComponentIdentifier.getFormat()).isEqualTo("nuget");
    assertThat(apiComponentIdentifier.getCoordinates().get("packageId")).isEqualTo("p1");
    assertThat(apiComponentIdentifier.getCoordinates().get("version")).isEqualTo("v1");
  }
}
