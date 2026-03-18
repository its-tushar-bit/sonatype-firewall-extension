/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.hds;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.remediation.VersionScoringDTO;
import com.sonatype.clm.dto.model.remediation.VersionScoringDTO.ToVersionData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
public class VersionScoringService
{
  private static final Logger log = LoggerFactory.getLogger(VersionScoringService.class);

  public static final String HDS_BULK_SCORE_VERSIONING_PATH = "rest/component/version-scoring/list";

  // Currently HDS only supports Maven version scoring.
  // Here at client side will have better performance if filtering out non-Maven formats.
  // Change this to false when:
  // 1. HDS supports more formats.
  // 2. IQ considers golden versions of formats other than Maven.
  static final boolean MAVEN_VERSION_SCORE_ONLY = true;

  private final HdsClient hdsClient;

  @Inject
  public VersionScoringService(HdsClient hdsClient) {
    this.hdsClient = hdsClient;
  }

  /**
   * @param componentIdentifiers a list of components with "from-versions"
   * @return a map whose keys are each of the "from-version" from input,
   *         values are their corresponding "to-versions", sorted by scores, from highest to lowest.
   */
  public Map<ComponentIdentifier, List<String>> getSortedNonBreakingVersionsNoAuth(
      Collection<ComponentIdentifier> componentIdentifiers)
  {
    Map<ComponentIdentifier, List<String>> result = new HashMap<>();
    if (MAVEN_VERSION_SCORE_ONLY) {
      componentIdentifiers = componentIdentifiers.stream()
          .filter(ComponentIdentifier::isMaven)
          .toList();
    }
    for (VersionScoringDTO versionScoringDTO : getVersionScoringNoAuth(componentIdentifiers)) {
      Map<String, ToVersionData> toVersionsNonBreaking = versionScoringDTO.getToVersionsNonBreaking();
      List<String> versionSortedByScore = toVersionsNonBreaking.entrySet()
          .parallelStream()
          .sorted(Entry.comparingByValue(Comparator.comparingInt(ToVersionData::getToComponentVersionScore).reversed()))
          .map(Entry::getKey)
          .toList();
      result.put(versionScoringDTO.getComponentIdentifier(), versionSortedByScore);
    }
    return result;
  }

  private Collection<VersionScoringDTO> getVersionScoringNoAuth(Collection<ComponentIdentifier> componentIdentifiers) {
    final Map<String, String> queryParams = Map.of("stableVersionsOnly", "true");
    final List<VersionScoringDTO> versionScoringData = List.of(
        hdsClient.post(VersionScoringDTO[].class, HDS_BULK_SCORE_VERSIONING_PATH, componentIdentifiers, queryParams));
    log.debug("Received {} version scoring entries from HDS", versionScoringData.size());
    return versionScoringData;
  }
}
