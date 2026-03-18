/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sonatype.insight.brain.api.v2.dto.ApiLicensedSolutionDTO;
import com.sonatype.insight.brain.solution.Solution;
import com.sonatype.insight.brain.solution.SolutionResolver;
import com.sonatype.insight.brain.solution.SolutionUrlResolver;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.StringUtils;

@Named
public class ApiLicensedSolutionService
{
  private static final Map<Solution, String> SOLUTION_ID_MAPPING = ImmutableMap.of(
      Solution.DEVELOPER, "developer",
      Solution.FIREWALL, "firewall",
      Solution.LIFECYCLE, "lifecycle",
      Solution.SBOM_MANAGER, "sbom");

  private final SolutionResolver solutionResolver;

  private final SolutionUrlResolver solutionUrlResolver;

  @Inject
  public ApiLicensedSolutionService(SolutionResolver solutionResolver, SolutionUrlResolver solutionUrlResolver) {
    this.solutionResolver = solutionResolver;
    this.solutionUrlResolver = solutionUrlResolver;
  }

  /**
   * Returns a list of ApiLicensedSolutionDTO objects representing the 'solutions' (i.e. firewall, lifecycle, etc.)
   * this instance of IQ is licensed for.
   *
   * If the baseUrl is configured and available it will form the prefix of the solution URLs returned. If the baseUrl
   * is not available the results depend on the allowRelativeUrls parameter, as explained next.
   *
   * @param allowRelativeUrls when true, relative Urls will be returned if the baseUrl is not configured; when false
   *          no results will be returned if the baseUrl is not available.
   * @return List of ApiLicensedSolutionDTO
   */
  public List<ApiLicensedSolutionDTO> getLicensedSolutions(boolean allowRelativeUrls) {
    List<ApiLicensedSolutionDTO> result = new ArrayList<>();

    Set<Solution> licensedSolutions = solutionResolver.getLicensedSolutions();

    licensedSolutions.forEach(solution -> {
      String url = solutionUrlResolver.getUrlForSolution(solution, allowRelativeUrls);
      if (StringUtils.isNotBlank(url)) {
        result.add(new ApiLicensedSolutionDTO(SOLUTION_ID_MAPPING.get(solution), url));
      }
    });

    return result;
  }
}
