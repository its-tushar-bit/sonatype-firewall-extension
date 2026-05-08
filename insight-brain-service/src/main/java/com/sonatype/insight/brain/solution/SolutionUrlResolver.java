/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.solution;

import java.util.Map;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dashboard.DashboardUtils;
import com.sonatype.insight.brain.landing.UserInterfaceLinksHelper;
import com.sonatype.insight.brain.service.Configuration;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.StringUtils;

@Named
@Singleton
public class SolutionUrlResolver
{
  @VisibleForTesting
  static final Map<Solution, String> SOLUTION_PATH_MAP = new ImmutableMap.Builder<Solution, String>()
      .put(Solution.DEVELOPER, UserInterfaceLinksHelper.getDeveloperHomePath())
      .put(Solution.FIREWALL, UserInterfaceLinksHelper.getFirewallHomePath())
      .put(Solution.GUIDE, UserInterfaceLinksHelper.getGuideHomePath())
      .put(Solution.LIFECYCLE, UserInterfaceLinksHelper.getLifecycleHomePath())
      .put(Solution.REPO_MANAGER, "")
      .put(Solution.SBOM_MANAGER, UserInterfaceLinksHelper.getSbomManagerHomePath())
      .build();

  private final Configuration configuration;

  private final DashboardUtils dashboardUtils;

  @Inject
  public SolutionUrlResolver(Configuration configuration, DashboardUtils dashboardUtils) {
    this.configuration = configuration;
    this.dashboardUtils = dashboardUtils;
  }

  /**
   * Computes the URL for the given solution
   *
   * @param solution reference to one of the solutions (i.e. firewall, lifecycle, developer, etc.)
   * @param allowRelativeUrls specifies how to handle the case where the baseUrl is not configured; true will
   *          return a relative Url; false will result in an empty result
   * @return full url, relative url, or blank string, depending on whether baseUrl is configured and whether relative
   *         urls are allowed
   */
  public String getUrlForSolution(Solution solution, boolean allowRelativeUrls) {
    if (StringUtils.isBlank(SOLUTION_PATH_MAP.get(solution))) {
      return "";
    }

    String baseUrl = configuration.getBaseUrlConfiguration().getBaseUrl();
    if (null == baseUrl) {
      baseUrl = "";
    }

    String result = "";

    if (StringUtils.isNotBlank(baseUrl) || allowRelativeUrls) {
      if (!baseUrl.endsWith("/")) {
        baseUrl = baseUrl + '/';
      }

      if (solution.equals(Solution.LIFECYCLE) && dashboardUtils.isDashboardDisabled()) {
        result = baseUrl + UserInterfaceLinksHelper.getLifecycleAltHomePath();
      }
      else {
        result = baseUrl + SOLUTION_PATH_MAP.get(solution);
      }
    }

    return result;
  }
}
