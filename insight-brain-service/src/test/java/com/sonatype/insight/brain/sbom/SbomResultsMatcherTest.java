/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sbom;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.IdentificationSource;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFileCoordinate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SbomResultsMatcherTest
{
  private static final ObjectMapper MAPPER = new ObjectMapper();

  @Test
  public void testBestMatch_BetterMatchPurlWins() throws Exception {
    ThirdPartyFileCoordinate sbomComponent =
        createSbomComponent("pypi", "orange", "1.0.1", "hash",
            "pkg:pypi/citrus/orange@1.0.1?extension=whl&qualifier=py2.py3-none-any");

    List<Pair<ComponentIdentifier, JsonNode>> mockResults = new ArrayList<>();
    ComponentIdentifier id1 =
        ComponentIdentifier.createPypiCoordinates("orange", "1.0.1", "py2.py3-none-1", "whl");
    // qualifiers match + namespace doesn't match
    JsonNode rNode1 =
        createResultNode("pkg:pypi/orange@1.0.1?qualifier=py2.py3-none-any&extension=whl", "hash3");
    mockResults.add(Pair.of(id1, rNode1));

    ComponentIdentifier id2 =
        ComponentIdentifier.createPypiCoordinates("orange", "1.0.1", "py2.py3-none-2", "whl");
    // qualifiers partially match + namespace doesn't match
    JsonNode rNode2 =
        createResultNode("pkg:pypi/orange@1.0.1?extension=whl&qualifier=py2.py3-none-x86_64", "hash2");
    mockResults.add(Pair.of(id2, rNode2));

    ComponentIdentifier id3 =
        ComponentIdentifier.createPypiCoordinates("orange", "1.0.1", "py2.py3-none-3", "whl");
    // qualifiers partially match + namespace match
    JsonNode rNode3 =
        createResultNode("pkg:pypi/citrus/orange@1.0.1?extension=egg&qualifier=py2.py3-none-any", "hash1");
    mockResults.add(Pair.of(id3, rNode3));

    ComponentIdentifier id4 =
        ComponentIdentifier.createPypiCoordinates("orange", "1.0.1", "py2.py3-none-4", "whl");
    // qualifiers fully match + namespace match
    JsonNode rNode4 =
        createResultNode("pkg:pypi/citrus/orange@1.0.1?extension=whl&qualifier=py2.py3-none-any&arch=x86_64", "hash3");
    mockResults.add(Pair.of(id4, rNode4));

    SbomResultsMatcherTelemetry telemetry = new SbomResultsMatcherTelemetry();
    Pair<ComponentIdentifier, JsonNode> bestMatch =
        SbomResultsMatcher.bestMatch(sbomComponent, mockResults, telemetry);
    assertThat(bestMatch.getKey()).isEqualTo(id4);
    assertThat(telemetry.getWinnerStat().purlMatchScore).isEqualTo(20f);
    assertThat(telemetry.getMatchStats().stream()).extracting("purlMatchScore")
        .containsExactly(17.5f, 16.25f, 18.75f, 20f);
    assertThat(telemetry.getMatchStats().stream()).extracting("hashMatchScore").containsExactly(0f, 0f, 0f, 0f);
    assertThat(telemetry.getMatchStats().stream()).extracting("coordMatchScore").containsExactly(15f, 15f, 15f, 15f);
  }

  @Test
  public void testBestMatch_PurlAndHashBetterThanPurl() throws Exception {
    ThirdPartyFileCoordinate sbomComponent =
        createSbomComponent("pypi", "orange", "1.0.1", "hash", "pkg:pypi/citrus/orange@1.0.1?extension=whl");

    List<Pair<ComponentIdentifier, JsonNode>> mockResults = new ArrayList<>();

    ComponentIdentifier id1 =
        ComponentIdentifier.createPypiCoordinates("orange", "1.0.1", "py2.py3-none-1", "whl");
    JsonNode rNode1 =
        createResultNode("pkg:pypi/citrus/orange@1.0.1?extension=whl&qualifier=py2.py3-none-any", "hash1");
    mockResults.add(Pair.of(id1, rNode1));

    ComponentIdentifier id2 =
        ComponentIdentifier.createPypiCoordinates("orange", "1.0.1", "py2.py3-none-2", "whl");
    JsonNode rNode2 =
        createResultNode("pkg:pypi/orange@1.0.1?extension=whl&qualifier=py2.py3-none-any", "hash");
    mockResults.add(Pair.of(id2, rNode2));

    SbomResultsMatcherTelemetry telemetry = new SbomResultsMatcherTelemetry();
    Pair<ComponentIdentifier, JsonNode> bestMatch =
        SbomResultsMatcher.bestMatch(sbomComponent, mockResults, telemetry);
    assertThat(bestMatch.getKey()).isEqualTo(id2);
    assertThat(telemetry.getWinnerStat().purlMatchScore).isEqualTo(17.5f);
    assertThat(telemetry.getWinnerStat().hashMatchScore).isEqualTo(20.0f);
    assertThat(telemetry.getWinnerStat().coordMatchScore).isEqualTo(15f);
    assertThat(telemetry.getMatchStats().stream()).extracting("purlMatchScore").containsExactly(20.0f, 17.5f);
    assertThat(telemetry.getMatchStats().stream()).extracting("hashMatchScore").containsExactly(0.0f, 20.0f);
    assertThat(telemetry.getMatchStats().stream()).extracting("coordMatchScore").containsExactly(15.0f, 15.0f);
  }

  @Test
  public void testBestMatch_NameAndHashBetterThanName() throws Exception {
    ThirdPartyFileCoordinate sbomComponent =
        createSbomComponent("pypi", "orange", "1.0.1", "hash", "");

    List<Pair<ComponentIdentifier, JsonNode>> mockResults = new ArrayList<>();

    ComponentIdentifier id1 =
        ComponentIdentifier.createPypiCoordinates("orange", "1.0.1", "py2.py3-none-1", "whl");
    JsonNode rNode1 =
        createResultNode("pkg:pypi/citrus/orange@1.0.1?extension=whl&qualifier=py2.py3-none-any", "hash1");
    mockResults.add(Pair.of(id1, rNode1));

    ComponentIdentifier id2 =
        ComponentIdentifier.createPypiCoordinates("orange", "1.0.1", "py2.py3-none-2", "whl");
    JsonNode rNode2 =
        createResultNode("pkg:pypi/orange@1.0.1?extension=whl&qualifier=py2.py3-none-any", "hash");
    mockResults.add(Pair.of(id2, rNode2));

    SbomResultsMatcherTelemetry telemetry = new SbomResultsMatcherTelemetry();
    Pair<ComponentIdentifier, JsonNode> bestMatch =
        SbomResultsMatcher.bestMatch(sbomComponent, mockResults, telemetry);
    assertThat(bestMatch.getKey()).isEqualTo(id2);
    assertThat(telemetry.getWinnerStat().purlMatchScore).isEqualTo(0f);
    assertThat(telemetry.getWinnerStat().hashMatchScore).isEqualTo(20.0f);
    assertThat(telemetry.getWinnerStat().coordMatchScore).isEqualTo(15.0f);
    assertThat(telemetry.getMatchStats().stream()).extracting("purlMatchScore").containsExactly(0.0f, 0.0f);
    assertThat(telemetry.getMatchStats().stream()).extracting("hashMatchScore").containsExactly(0.0f, 20.0f);
    assertThat(telemetry.getMatchStats().stream()).extracting("coordMatchScore").containsExactly(15.0f, 15.0f);
  }

  @Test
  public void testBestMatch_OnlyConsiderSonatypeIdentifiedHash() throws Exception {
    ThirdPartyFileCoordinate sbomComponent =
        createSbomComponent("pypi", "orange", "1.0.1", "hash", "");

    List<Pair<ComponentIdentifier, JsonNode>> mockResults = new ArrayList<>();

    ComponentIdentifier id1 =
        ComponentIdentifier.createPypiCoordinates("orange", "1.0.1", "py2.py3-none-1", "whl");
    JsonNode rNode1 =
        createResultNode("", "hash", "Sonatype"); // same hash with Sonatype id source
    mockResults.add(Pair.of(id1, rNode1));

    ComponentIdentifier id2 =
        ComponentIdentifier.createPypiCoordinates("orange", "1.0.1", "py2.py3-none-2", "whl");
    JsonNode rNode2 =
        createResultNode("", "hash", "Other"); // same hash bother other id source
    mockResults.add(Pair.of(id2, rNode2));

    SbomResultsMatcherTelemetry telemetry = new SbomResultsMatcherTelemetry();
    Pair<ComponentIdentifier, JsonNode> bestMatch =
        SbomResultsMatcher.bestMatch(sbomComponent, mockResults, telemetry);
    assertThat(bestMatch.getKey()).isEqualTo(id1);
    assertThat(telemetry.getWinnerStat().purlMatchScore).isEqualTo(0f);
    assertThat(telemetry.getWinnerStat().hashMatchScore).isEqualTo(20.0f);
    assertThat(telemetry.getWinnerStat().coordMatchScore).isEqualTo(15.0f);
    assertThat(telemetry.getMatchStats().stream()).extracting("purlMatchScore").containsExactly(0.0f, 0.0f);
    assertThat(telemetry.getMatchStats().stream()).extracting("hashMatchScore").containsExactly(20.0f, 0.0f);
    assertThat(telemetry.getMatchStats().stream()).extracting("coordMatchScore").containsExactly(15.0f, 15.0f);
  }

  private JsonNode createResultNode(String purl, String hash) throws JsonProcessingException {
    return createResultNode(purl, hash, IdentificationSource.SONATYPE.getId());
  }

  private JsonNode createResultNode(
      String purl,
      String hash,
      String identificationSource) throws JsonProcessingException
  {
    Map<String, String> nodeData = new HashMap<>();
    nodeData.put("packageUrl", purl);
    nodeData.put("hash", hash);
    nodeData.put("identificationSource", identificationSource);
    return MAPPER.readTree(MAPPER.writeValueAsString(nodeData));
  }

  private ThirdPartyFileCoordinate createSbomComponent(
      String format,
      String name,
      String version,
      String hash,
      String purl)
  {
    ThirdPartyFileCoordinate fileCoordinate =
        new ThirdPartyFileCoordinate(hash, "source", format, name, version, "tpId");
    fileCoordinate.setPackageUrl(purl);
    return fileCoordinate;
  }
}
