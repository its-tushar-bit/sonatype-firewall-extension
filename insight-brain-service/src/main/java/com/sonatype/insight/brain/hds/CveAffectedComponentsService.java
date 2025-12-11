/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.hds;

import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.insight.error.exception.NotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;

/**
 * Service for retrieving components affected by CVE vulnerabilities from HDS.
 *
 * @since 1.178
 */
@Named
public class CveAffectedComponentsService
{
  private static final Logger log = LoggerFactory.getLogger(CveAffectedComponentsService.class);

  private static final String HDS_PATH = "/rest/vulnerability/affected/{cveId}";

  private final HdsClient hdsClient;

  @Inject
  public CveAffectedComponentsService(final HdsClient hdsClient) {
    this.hdsClient = hdsClient;
  }

  public List<AffectedComponentDTO> getAffectedComponents(final String cveId) {
    return getAffectedComponents(cveId, emptyMap());
  }

  /**
   * Retrieves all components affected by a given CVE vulnerability from HDS with additional query parameters.
   *
   * @param cveId       the CVE identifier (e.g., CVE-2025-55182)
   * @param queryParams additional query parameters for the request
   * @return list of affected components
   */
  public List<AffectedComponentDTO> getAffectedComponents(
      final String cveId,
      final Map<String, String> queryParams)
  {
    log.debug("Fetching affected components for CVE: {}", cveId);

    try {
      AffectedComponentList result = hdsClient.get(
          AffectedComponentList.class,
          HDS_PATH,
          queryParams,
          cveId.toUpperCase()
      );

      List<AffectedComponentDTO> affectedComponents = result.getComponents();
      log.debug("Found {} affected components for CVE: {}", affectedComponents.size(), cveId);

      return affectedComponents;
    }
    catch (NotFoundException e) {
      log.debug("No affected components found for CVE: {}", cveId);

      return emptyList();
    }
  }
}
