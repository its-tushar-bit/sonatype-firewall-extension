/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.hds;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.Collections.emptySet;

/**
 * Service for retrieving components affected by CVE vulnerabilities from HDS.
 *
 * @since 1.178
 */
@Named
public class CveAffectedComponentsService
{
  private static final Logger log = LoggerFactory.getLogger(CveAffectedComponentsService.class);

  private static final String HDS_PATH = "/rest/vulnerability/affected";

  private final HdsClient hdsClient;

  @Inject
  public CveAffectedComponentsService(final HdsClient hdsClient) {
    this.hdsClient = hdsClient;
  }

  /**
   * Retrieves affected component coordinates for multiple CVE IDs.
   *
   * <p>First attempts batch query with all CVEs (?refId=CVE-A&refId=CVE-B).
   * If HDS returns 400 "Only one RefId is allowed", falls back to individual queries.
   *
   * @param cveIds set of CVE identifiers
   * @return map of CVE ID to set of affected component coordinates
   */
  public Map<String, Set<AffectedCoordinates>> fetchAffectedComponentsForMultipleCves(final Set<String> cveIds) {
    log.debug("Fetching affected components for {} CVE(s)", cveIds.size());

    if (cveIds.size() == 1) {
      return fetchAffectedComponentsForIndividualCves(cveIds);
    }

    try {
      return fetchAffectedComponentsForAllCves(cveIds);
    }
    catch (BadRequestException e) {
      if (e.getMessage() != null && e.getMessage().contains("Only one RefId is allowed")) {
        log.debug("Batch not supported, falling back to individual queries");
        return fetchAffectedComponentsForIndividualCves(cveIds);
      }
      throw e;
    }
  }

  private Map<String, Set<AffectedCoordinates>> fetchAffectedComponentsForAllCves(final Set<String> cveIds) {
    log.debug("Attempting batch query with {} CVEs", cveIds.size());

    Multimap<String, String> params = HashMultimap.create();
    cveIds.forEach(cveId -> params.put("refId", cveId.toUpperCase()));

    Set<AffectedComponentDTO> components = fetchAffectedComponentsWithPagination(HDS_PATH, params);

    Map<String, Set<AffectedCoordinates>> results = components.stream()
        .flatMap(component -> {
          Collection<String> refIds = component.refIds();
          if (refIds == null || refIds.isEmpty()) {
            log.warn("Component {} missing refIds in multi-CVE HDS response. " +
                "Falling back to all queried CVEs, which may be inaccurate. Component: {}",
                component.getCoordinates(), component);
            refIds = cveIds;
          }
          return refIds.stream().map(refId -> Map.entry(refId, component.getCoordinates()));
        })
        .collect(Collectors.groupingBy(
            Map.Entry::getKey,
            Collectors.mapping(Map.Entry::getValue, Collectors.toSet())
        ));

    cveIds.forEach(cveId -> results.putIfAbsent(cveId, new HashSet<>()));

    return results;
  }

  private Map<String, Set<AffectedCoordinates>> fetchAffectedComponentsForIndividualCves(final Set<String> cveIds) {
    Map<String, Set<AffectedCoordinates>> results = new HashMap<>();

    for (String cveId : cveIds) {
      try {
        Set<AffectedCoordinates> coordinates = fetchAffectedComponentForIndividualCve(cveId);
        results.put(cveId, coordinates);
      }
      catch (Exception e) {
        log.debug("Unable to fetch components for CVE ID: {}. Error: {}", cveId, e.getMessage());
      }
    }

    return results;
  }

  private Set<AffectedCoordinates> fetchAffectedComponentForIndividualCve(final String cveId) {
    Multimap<String, String> params = HashMultimap.create();
    params.put("refId", cveId.toUpperCase());
    Set<AffectedComponentDTO> components = fetchAffectedComponentsWithPagination(HDS_PATH, params);
    return components.stream()
        .map(AffectedComponentDTO::getCoordinates)
        .collect(Collectors.toSet());
  }

  private Set<AffectedComponentDTO> fetchAffectedComponentsWithPagination(
      final String url,
      final Multimap<String, String> params)
  {
    Set<AffectedComponentDTO> allComponents = new HashSet<>();
    String cursor = null;
    boolean hasMore = true;

    try {
      while (hasMore) {
        Multimap<String, String> requestParams = params;

        if (cursor != null) {
          requestParams = HashMultimap.create(params);
          requestParams.put("cursor", cursor);
        }

        AffectedComponentList result = hdsClient.getWithMultimap(AffectedComponentList.class, url, requestParams);

        if (result.getComponents() != null) {
          allComponents.addAll(result.getComponents());
        }

        if (result.getHasMore() != null && result.getHasMore() &&
            result.getNextCursor() != null && !result.getNextCursor().isEmpty()) {
          cursor = result.getNextCursor();
        }
        else {
          hasMore = false;
        }
      }

      log.debug("Fetched {} total components", allComponents.size());
      return allComponents;
    }
    catch (NotFoundException e) {
      log.debug("No components found");
      return emptySet();
    }
  }
}
