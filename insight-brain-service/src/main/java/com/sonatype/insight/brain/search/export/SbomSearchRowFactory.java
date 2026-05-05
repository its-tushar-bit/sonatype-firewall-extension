/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.export;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.search.results.SearchResultItemDTO;

import static com.sonatype.insight.brain.search.export.SearchRowFactory.Header.*;

@Named
@Singleton
public class SbomSearchRowFactory
    extends SearchRowFactory
{
  private static final List<Header> EXPORT_SEARCH_HEADERS = Arrays.asList(
      ITEM_TYPE, ORGANIZATION, ORGANIZATION_LINK, APPLICATION, APPLICATION_LINK, APPLICATION_CATEGORY,
      APPLICATION_CATEGORY_LINK, POLICY, THREAT, POLICY_LINK, COMPONENT_NAME, SECURITY_ISSUE, SECURITY_ISSUE_ID,
      APPLICATION_VERSION, SBOM_SPECIFICATION,
      POLICY_VIOLATION_NAME, POLICY_VIOLATION_THREAT_CATEGORY, POLICY_VIOLATION_THREAT_LEVEL_EXPORT,
      POLICY_VIOLATION_WAIVER_STATUS,
      COMPONENT_EFFECTIVE_LICENSE, COMPONENT_LICENSE_THREAT_GROUP, COMPONENT_LICENSE_THREAT_LEVEL_EXPORT);

  private static final Set<Header> SBOM_EMPTY_COLUMNS = new HashSet<>(Arrays.asList(
      APPLICATION_CATEGORY, APPLICATION_CATEGORY_LINK, POLICY, THREAT, POLICY_LINK, SECURITY_ISSUE));

  public SbomSearchRowFactory() {
    super(EXPORT_SEARCH_HEADERS);
  }

  @Override
  protected void addColumn(
      List<String> row,
      Header header,
      SearchResultItemDTO searchResultItemDTO,
      String baseUrl)
  {
    if (!SBOM_EMPTY_COLUMNS.contains(header)) {
      // CLM-29683 - Once complete SBOM Manager will support all SBOM_EMPTY_COLUMNS columns, until then skip leaving an
      // empty string this will keep the CSV consistent without breaking changes
      super.addColumn(row, header, searchResultItemDTO, baseUrl);
    }
  }

  @Override
  protected boolean isSbomManager() {
    return true;
  }
}
