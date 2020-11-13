/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.experimental.legal;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.hds.HdsClient;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.license.dto.model.ComponentLegalCommentDTO;
import com.sonatype.insight.license.dto.model.ComponentLegalFileDTO;
import com.sonatype.insight.license.dto.model.LicenseMetadataDTO;

import com.google.common.collect.Iterables;

@Named
public class ApiLicenseLegalHdsService
{
  static final String METADATA_URL = "/rest/license/metadata";

  static final String LEGAL_COMMENT_URL = "/rest/legal/comment";

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
}
