/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.experimental.legal;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.hds.HdsClient;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.license.dto.model.AnameAggregateFileGroup;
import com.sonatype.insight.license.dto.model.ComponentLegalCommentDTO;
import com.sonatype.insight.license.dto.model.ComponentLegalFileDTO;
import com.sonatype.insight.license.dto.model.LegalCommentDTO;
import com.sonatype.insight.license.dto.model.LicenseMetadataDTO;

import com.google.common.collect.Iterables;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;

@Named
public class ApiLicenseLegalHdsService
{
  static final String METADATA_URL = "/rest/license/metadata";

  static final String LEGAL_COMMENT_URL = "/rest/legal/comment";

  static final String LEGAL_ANAME_COMMENT_URL = "/rest/legal/aname/comment";

  static final String LEGAL_FILE_URL = "/rest/legal/file";

  private final HdsClient hdsClient;

  private final InsightConfig insightConfig;

  @Inject
  public ApiLicenseLegalHdsService(HdsClient hdsClient, InsightConfig insightConfig) {
    this.hdsClient = hdsClient;
    this.insightConfig = insightConfig;
  }

  public List<LicenseMetadataDTO> getLicenseMetadata(Collection<String> licenses) {
    return Arrays.asList(hdsClient.post(LicenseMetadataDTO[].class, METADATA_URL, licenses));
  }

  public Set<ComponentLegalCommentDTO> getComponentLegalComments(Collection<ComponentIdentifier> componentIdentifiers) {
    return StreamSupport.stream(
        Iterables.partition(componentIdentifiers, insightConfig.getLicenseLegalHdsRequestLimit()).spliterator(), true)
        .flatMap(
            partition -> Arrays.stream(hdsClient.post(ComponentLegalCommentDTO[].class, LEGAL_COMMENT_URL, partition)))
        .collect(Collectors.toCollection(LinkedHashSet::new));
  }

  public Set<ComponentLegalFileDTO> getComponentLegalFiles(Collection<ComponentIdentifier> componentIdentifiers) {
    return StreamSupport.stream(
        Iterables.partition(componentIdentifiers, insightConfig.getLicenseLegalHdsRequestLimit()).spliterator(), true)
        .flatMap(partition -> Arrays.stream(hdsClient.post(ComponentLegalFileDTO[].class, LEGAL_FILE_URL, partition)))
        .collect(Collectors.toCollection(LinkedHashSet::new));
  }

  /**
   * Returns comments for A-Name components.
   * <p>
   * Results received from HDS are post-processed to combine per-file comments into per-component and reconstructs
   * component hash for the resulting {@link ComponentLegalCommentDTO} from the provided component ID -> hash map.
   * <p>
   * For example following response
   * <pre>
   *   [{
   *      componentIdentifier: "1",
   *      hash: "1",
   *      comments: [{ 1 }]
   *   }, {
   *      componentIdentifier: "1",
   *      hash: "2",
   *      comments: [{ 2 }]
   *   }]
   * </pre>
   * will be converted to this:
   * <pre>
   *   [{
   *      componentIdentifier: "1",
   *      hash: "componentHash",
   *      comments: [{ 1 }, { 2 }]
   *   }]
   * </pre>
   * </p>
   *
   * @param anameAggregateFileGroups Set of {@link AnameAggregateFileGroup} to retrieve A-name comments
   * @param componentIdentifierHash  A Map from the component identifier to the component hash. If no component hash
   *                                 exist in the map for any given identifier, then {@literal null} will be used for
   *                                 the componentHash in the response
   * @return Set of {@link ComponentLegalCommentDTO}
   */
  public Set<ComponentLegalCommentDTO> getAnameComponentLegalComments(
      final Set<AnameAggregateFileGroup> anameAggregateFileGroups,
      final Map<ComponentIdentifier, String> componentIdentifierHash)
  {
    final List<AnameAggregateFileGroup> filteredGroups = anameAggregateFileGroups.stream()
        .filter(group -> !group.getAggregateHashes().isEmpty())
        .collect(Collectors.toList());

    if (filteredGroups.isEmpty()) {
      return Collections.emptySet();
    }

    final Set<ComponentLegalCommentDTO> componentComments = StreamSupport.stream(
        Iterables.partition(
            filteredGroups,
            insightConfig.getLicenseLegalHdsRequestLimit()).spliterator(),
        true)
        .flatMap(
            partition -> Arrays
                .stream(hdsClient.post(ComponentLegalCommentDTO[].class,
                        LEGAL_ANAME_COMMENT_URL,
                        partition)))
        .collect(Collectors.toCollection(LinkedHashSet::new));

    final Multimap<ComponentIdentifier, LegalCommentDTO> componentCommentMap = LinkedListMultimap.create();

    // combine all comments for the same component identifier together
    for (final ComponentLegalCommentDTO componentComment : componentComments) {
      final ComponentIdentifier cleanedComponentId =
          LegalComponentIdentifierUtil.removeClassifierAndExtension(componentComment.getComponentIdentifier());
      componentCommentMap.putAll(
          cleanedComponentId,
          componentComment.getComments());
    }

    return componentCommentMap.asMap().entrySet().stream()
        .map(entry -> entryToComponentLegalComment(
            entry,
            componentIdentifierHash.getOrDefault(entry.getKey(), null)))
        .collect(Collectors.toCollection(LinkedHashSet::new));
  }

  private ComponentLegalCommentDTO entryToComponentLegalComment(
      final Map.Entry<ComponentIdentifier, Collection<LegalCommentDTO>> entry,
      final String componentHash)
  {
    final ComponentLegalCommentDTO componentLegalCommentDTO = new ComponentLegalCommentDTO();
    componentLegalCommentDTO.setComponentIdentifier(entry.getKey());
    componentLegalCommentDTO.setComments(new LinkedHashSet<>(entry.getValue()));
    componentLegalCommentDTO.setHash(componentHash);
    return componentLegalCommentDTO;
  }
}
