/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.report;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.model.license.LicenseOverrideStatus;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Sets;
import org.junit.Rule;
import org.junit.Test;

import static com.sonatype.insight.brain.model.license.LicenseOverrideStatus.OVERRIDDEN;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

public class ReportTest
{
  @Rule
  public TemporaryEntity tempEntity = new TemporaryEntity();
  
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

  @Test
  public void testAppendCacheBustingParams() throws Exception {
    String indexContent = "<script type='text/javascript' src='../brain/policy-assets/js/brain.client.js'></script>"
        + "<script type='text/javascript' src='../brain/policy-assets/js/cip-loader.js'></script>";
    String expectedIndexContent = "<script type='text/javascript' src='../brain/policy-assets/js/brain.client.js?1.0'></script>"
        + "<script type='text/javascript' src='../brain/policy-assets/js/cip-loader.js?1.0'></script>";

    ReportEntry entry = new ReportEntry("index.html", System.currentTimeMillis(), indexContent.getBytes("UTF-8"));
    entry = Report.appendCacheBustingParams(entry, "1.0");

    assertThat(entry.buf, is(expectedIndexContent.getBytes("UTF-8")));
  }

  @Test
  public void testAugmentModified_NoLicenseOverrides() throws Exception {
    JsonNode bomJson = new ObjectMapper().readTree(getClass().getResource("/ReportTest/bom.json"));

    JsonNode bomJsonAugmented = bomJson.deepCopy();
    Report.augmentModified(new HashSet<>(), bomJsonAugmented);

    assertThat(bomJson, is(equalTo(bomJsonAugmented)));
    assertThat(bomJsonAugmented.get("aaData").get(0).has("modified"), is(false));
    assertThat(bomJsonAugmented.get("aaData").get(1).has("modified"), is(false));
  }

  @Test
  public void testAugmentModified_AppLicenseOverride() throws Exception {
    ComponentIdentifier anameHawk111 = ComponentIdentifier.createAnameCoordinates("hawk", "", "1.1.1");
    tempEntity.newLicenseOverride(tempEntity.newApplicationWithParent().getId(), anameHawk111, OVERRIDDEN, "Beerware");
    JsonNode bomJson = new ObjectMapper().readTree(getClass().getResource("/ReportTest/bom.json"));

    JsonNode bomJsonAugmented = bomJson.deepCopy();
    Report.augmentModified(Sets.newHashSet(anameHawk111), bomJsonAugmented);

    assertThat(bomJson, is(not(equalTo(bomJsonAugmented))));
    assertThat(bomJsonAugmented.get("aaData").get(0).has("modified"), is(true));
    assertThat(bomJsonAugmented.get("aaData").get(1).has("modified"), is(false));
  }

  @Test
  public void testAugmentModified_OrgLicenseOverrides() throws Exception {
    ComponentIdentifier npmHawk111 = ComponentIdentifier.createNpmCoordinates("hawk", "1.1.1");
    tempEntity.newLicenseOverride(tempEntity.newApplicationWithParent().getParentOwnerId(), npmHawk111,
        LicenseOverrideStatus.OVERRIDDEN, "Beerware");
    JsonNode bomJson = new ObjectMapper().readTree(getClass().getResource("/ReportTest/bom.json"));

    JsonNode bomJsonAugmented = bomJson.deepCopy();
    Report.augmentModified(Sets.newHashSet(npmHawk111), bomJsonAugmented);

    assertThat(bomJson, is(not(equalTo(bomJsonAugmented))));
    assertThat(bomJsonAugmented.get("aaData").get(0).has("modified"), is(false));
    assertThat(bomJsonAugmented.get("aaData").get(1).has("modified"), is(true));
  }
}
