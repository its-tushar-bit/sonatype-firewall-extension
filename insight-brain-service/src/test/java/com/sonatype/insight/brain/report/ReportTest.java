/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.report;

import java.util.Map;
import java.util.Set;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Sets;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

public class ReportTest
{
  private Set<Integer> depths(Integer... depths) {
    return Sets.newHashSet(depths);
  }

  @Test
  public void testParseDependencyDepths_PreferNewStructure() throws Exception {
    JsonNode dependenciesJson = new ObjectMapper().readTree(getClass().getResource("/ReportTest/dependencies.json"));
    assertThat(dependenciesJson.path("gavDepths").isObject(), is(true));

    Map<ComponentIdentifier, Set<Integer>> depthsByIdentifier = Report.parseDependencyDepths(dependenciesJson);
    assertThat(depthsByIdentifier,
        hasEntry(ComponentIdentifier.createMavenCoordinates("junit", "junit", "4.9", "", "jar"), depths(1)));
    assertThat(
        depthsByIdentifier,
        hasEntry(ComponentIdentifier.createMavenCoordinates("org.slf4j", "slf4j-api", "1.6", "", "jar"),
            depths(1, 2, 3)));
    assertThat(depthsByIdentifier.entrySet(), hasSize(2));
  }

  @Test
  public void testParseDependencyDepths_FallbackToOldStructure() throws Exception {
    JsonNode dependenciesJson = new ObjectMapper().readTree(getClass().getResource("/ReportTest/dependencies.json"));
    ((ObjectNode) dependenciesJson).remove("componentDepths");

    Map<ComponentIdentifier, Set<Integer>> depthsByIdentifier = Report.parseDependencyDepths(dependenciesJson);
    assertThat(depthsByIdentifier,
        hasEntry(ComponentIdentifier.createMavenCoordinates("junit", "junit", "4.9"), depths(1)));
    assertThat(depthsByIdentifier,
        hasEntry(ComponentIdentifier.createMavenCoordinates("org.slf4j", "slf4j-api", "1.6"), depths(1, 2, 3)));
    assertThat(depthsByIdentifier.entrySet(), hasSize(2));
  }
}
