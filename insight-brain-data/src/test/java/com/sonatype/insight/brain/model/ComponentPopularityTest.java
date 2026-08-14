/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ComponentPopularityTest
{
  @Test
  public void validateGAVDeserialization() throws Exception {
    String json = "{\"groupId\":\"gid\",\"artifactId\":\"aid\",\"version\":\"ver\"}";

    ComponentPopularity componentPopularity = new ObjectMapper().readValue(json, ComponentPopularity.class);

    assertThat(componentPopularity.getComponentIdentifier())
        .isEqualTo(ComponentIdentifier.createMavenCoordinates("gid", "aid", "ver"));
  }
}
