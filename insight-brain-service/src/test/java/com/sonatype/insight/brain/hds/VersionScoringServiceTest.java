/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.hds;

import java.util.List;
import java.util.Map;
import jakarta.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.remediation.VersionScoringDTO;
import com.sonatype.clm.dto.model.remediation.VersionScoringDTO.ToVersionData;
import com.sonatype.insight.brain.service.AbstractComponentTest;

import com.google.inject.Binder;
import org.junit.Test;
import org.mockito.Mock;

import static com.sonatype.insight.brain.hds.VersionScoringService.HDS_BULK_SCORE_VERSIONING_PATH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

public class VersionScoringServiceTest
    extends AbstractComponentTest
{
  @Mock
  private HdsClient mockHdsClient;

  @Inject
  private VersionScoringService versionScoringService;

  @Override
  public void configure(Binder binder) {
    binder.bind(HdsClient.class).toInstance(mockHdsClient);
    super.configure(binder);
  }

  @Test
  public void testGetSortedNonBreakingVersionsNoAuth() {
    ComponentIdentifier component0 = new ComponentIdentifier("maven", Map.of(
        "artifactId", "Artifact1",
        "groupId", "Group1",
        "version", "1.2.3"));
    List<ComponentIdentifier> fromVersions = List.of(component0);
    Map<String, ToVersionData> versionMap = Map.of(
        "1.2.4", new ToVersionData(1, 1, 1, 0, 20),
        "1.2.5", new ToVersionData(1, 1, 1, 0, 50),
        "1.2.6", new ToVersionData(1, 1, 1, 0, 30));
    VersionScoringDTO[] responsePayload = new VersionScoringDTO[]{
      new VersionScoringDTO(component0, 1, 1, 10, versionMap)
    };
    when(mockHdsClient.post(eq(VersionScoringDTO[].class), eq(HDS_BULK_SCORE_VERSIONING_PATH), eq(fromVersions),
        eq(Map.of("stableVersionsOnly", "true"))))
            .thenReturn(responsePayload);

    Map<ComponentIdentifier, List<String>> sortedNonBreakingVersions =
        versionScoringService.getSortedNonBreakingVersionsNoAuth(fromVersions);

    assertThat(sortedNonBreakingVersions).containsOnlyKeys(component0);
    assertThat(sortedNonBreakingVersions.get(component0)).containsExactly("1.2.5", "1.2.6", "1.2.4");
  }

  @Test
  public void testGetSortedNonBreakingVersionsNoAuth_multipleComponents() {
    ComponentIdentifier component0 = new ComponentIdentifier("maven", Map.of(
        "artifactId", "Artifact1",
        "groupId", "Group1",
        "version", "1.2.3"));
    ComponentIdentifier component1 = new ComponentIdentifier("maven", Map.of(
        "artifactId", "Artifact2",
        "groupId", "Group2",
        "version", "4.1.15"));
    List<ComponentIdentifier> fromVersions = List.of(component0, component1);
    Map<String, ToVersionData> versionMap0 = Map.of(
        "1.2.4", new ToVersionData(1, 1, 1, 0, 20),
        "1.2.5", new ToVersionData(1, 1, 1, 0, 50),
        "1.2.6", new ToVersionData(1, 1, 1, 0, 30));
    Map<String, ToVersionData> versionMap1 = Map.of(
        "4.1.25", new ToVersionData(1, 1, 1, 0, 100),
        "4.2.0", new ToVersionData(1, 1, 1, 0, 200),
        "4.3.10", new ToVersionData(1, 1, 1, 0, 300));
    VersionScoringDTO[] responsePayload = new VersionScoringDTO[]{
      new VersionScoringDTO(component0, 1, 1, 10, versionMap0),
      new VersionScoringDTO(component1, 1, 1, 10, versionMap1)
    };
    when(mockHdsClient.post(eq(VersionScoringDTO[].class), eq(HDS_BULK_SCORE_VERSIONING_PATH), eq(fromVersions),
        eq(Map.of("stableVersionsOnly", "true"))))
            .thenReturn(responsePayload);

    Map<ComponentIdentifier, List<String>> sortedNonBreakingVersions =
        versionScoringService.getSortedNonBreakingVersionsNoAuth(fromVersions);

    assertThat(sortedNonBreakingVersions.get(component0)).containsExactly("1.2.5", "1.2.6", "1.2.4");
    assertThat(sortedNonBreakingVersions.get(component1)).containsExactly("4.3.10", "4.2.0", "4.1.25");
  }
}
