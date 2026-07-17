/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.solution;

import com.sonatype.insight.brain.dashboard.DashboardUtils;
import com.sonatype.insight.brain.service.BaseUrlConfiguration;
import com.sonatype.insight.brain.service.Configuration;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

public class SolutionUrlResolverTest
{
  private static final String BASE_URL = "https://locahost:8070";

  @Before
  public void setup() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  public void testFullUrlsWithTrailingSlashInBaseUrl() {
    // given: no base url set in config
    BaseUrlConfiguration baseUrlConfig = new BaseUrlConfiguration(BASE_URL + '/', false);
    Configuration mockConfiguration = Mockito.mock(Configuration.class);
    DashboardUtils mockDashboardUtils = Mockito.mock(DashboardUtils.class);
    when(mockConfiguration.getBaseUrlConfiguration()).thenReturn(baseUrlConfig);
    when(mockDashboardUtils.isDashboardDisabled()).thenReturn(false);
    SolutionUrlResolver urlResolver = new SolutionUrlResolver(mockConfiguration, mockDashboardUtils);

    // when: get the URLs for the solutions
    String developerUrl = urlResolver.getUrlForSolution(Solution.DEVELOPER, false);
    String firewallUrl = urlResolver.getUrlForSolution(Solution.FIREWALL, false);
    String lifecycleUrl = urlResolver.getUrlForSolution(Solution.LIFECYCLE, false);
    String repoManagerUrl = urlResolver.getUrlForSolution(Solution.REPO_MANAGER, false);
    String sbomManagerUrl = urlResolver.getUrlForSolution(Solution.SBOM_MANAGER, false);
    String guideUrl = urlResolver.getUrlForSolution(Solution.GUIDE, false);

    // then:
    assertThat(developerUrl).isEqualTo("https://locahost:8070/ui/links/developer/dashboard");
    assertThat(firewallUrl).isEqualTo("https://locahost:8070/ui/links/firewall/dashboard");
    assertThat(lifecycleUrl).isEqualTo("https://locahost:8070/ui/links/lifecycle/dashboard");
    assertThat(repoManagerUrl).isBlank();
    assertThat(sbomManagerUrl).isEqualTo("https://locahost:8070/ui/links/sbomManager/dashboard");
    assertThat(guideUrl).isEqualTo("https://locahost:8070/assets/guide/index.html#/");
  }

  @Test
  public void testFullUrlsWithoutTrailingSlashInBaseUrl() {
    // given: no base url set in config
    BaseUrlConfiguration baseUrlConfig = new BaseUrlConfiguration(BASE_URL, false);
    Configuration mockConfiguration = Mockito.mock(Configuration.class);
    DashboardUtils mockDashboardUtils = Mockito.mock(DashboardUtils.class);
    when(mockConfiguration.getBaseUrlConfiguration()).thenReturn(baseUrlConfig);
    when(mockDashboardUtils.isDashboardDisabled()).thenReturn(false);
    SolutionUrlResolver urlResolver = new SolutionUrlResolver(mockConfiguration, mockDashboardUtils);

    // when: get the URLs for the solutions
    String developerUrl = urlResolver.getUrlForSolution(Solution.DEVELOPER, false);
    String firewallUrl = urlResolver.getUrlForSolution(Solution.FIREWALL, false);
    String lifecycleUrl = urlResolver.getUrlForSolution(Solution.LIFECYCLE, false);
    String repoManagerUrl = urlResolver.getUrlForSolution(Solution.REPO_MANAGER, false);
    String sbomManagerUrl = urlResolver.getUrlForSolution(Solution.SBOM_MANAGER, false);
    String guideUrl = urlResolver.getUrlForSolution(Solution.GUIDE, false);

    // then:
    assertThat(developerUrl).isEqualTo("https://locahost:8070/ui/links/developer/dashboard");
    assertThat(firewallUrl).isEqualTo("https://locahost:8070/ui/links/firewall/dashboard");
    assertThat(lifecycleUrl).isEqualTo("https://locahost:8070/ui/links/lifecycle/dashboard");
    assertThat(repoManagerUrl).isBlank();
    assertThat(sbomManagerUrl).isEqualTo("https://locahost:8070/ui/links/sbomManager/dashboard");
    assertThat(guideUrl).isEqualTo("https://locahost:8070/assets/guide/index.html#/");
  }

  @Test
  public void testWithoutBaseUrlAndRelativePathsAllowed() {
    // given: no base url set in config
    final String baseUrl = null;
    final boolean allowRelativePaths = true;

    BaseUrlConfiguration baseUrlConfig = new BaseUrlConfiguration(baseUrl, false);
    Configuration mockConfiguration = Mockito.mock(Configuration.class);
    DashboardUtils mockDashboardUtils = Mockito.mock(DashboardUtils.class);
    when(mockConfiguration.getBaseUrlConfiguration()).thenReturn(baseUrlConfig);
    when(mockDashboardUtils.isDashboardDisabled()).thenReturn(false);
    SolutionUrlResolver urlResolver = new SolutionUrlResolver(mockConfiguration, mockDashboardUtils);

    // when:
    String developerUrl = urlResolver.getUrlForSolution(Solution.DEVELOPER, allowRelativePaths);
    String firewallUrl = urlResolver.getUrlForSolution(Solution.FIREWALL, allowRelativePaths);
    String lifecycleUrl = urlResolver.getUrlForSolution(Solution.LIFECYCLE, allowRelativePaths);
    String repoManagerUrl = urlResolver.getUrlForSolution(Solution.REPO_MANAGER, allowRelativePaths);
    String sbomManagerUrl = urlResolver.getUrlForSolution(Solution.SBOM_MANAGER, allowRelativePaths);
    String guideUrl = urlResolver.getUrlForSolution(Solution.GUIDE, allowRelativePaths);

    // then:
    assertThat(developerUrl).isEqualTo("/ui/links/developer/dashboard");
    assertThat(firewallUrl).isEqualTo("/ui/links/firewall/dashboard");
    assertThat(lifecycleUrl).isEqualTo("/ui/links/lifecycle/dashboard");
    assertThat(repoManagerUrl).isBlank();
    assertThat(sbomManagerUrl).isEqualTo("/ui/links/sbomManager/dashboard");
    assertThat(guideUrl).isEqualTo("/assets/guide/index.html#/");
  }

  @Test
  public void testWithoutBaseUrlAndRelativePathsNotAllowed() {
    // given: no base url set in config
    final boolean relativePathsNotAllowed = false;

    BaseUrlConfiguration baseUrlConfig = new BaseUrlConfiguration(null, false);
    Configuration mockConfiguration = Mockito.mock(Configuration.class);
    DashboardUtils mockDashboardUtils = Mockito.mock(DashboardUtils.class);
    when(mockConfiguration.getBaseUrlConfiguration()).thenReturn(baseUrlConfig);
    when(mockDashboardUtils.isDashboardDisabled()).thenReturn(false);
    SolutionUrlResolver urlResolver = new SolutionUrlResolver(mockConfiguration, mockDashboardUtils);

    // when:
    String developerUrl = urlResolver.getUrlForSolution(Solution.DEVELOPER, relativePathsNotAllowed);
    String firewallUrl = urlResolver.getUrlForSolution(Solution.FIREWALL, relativePathsNotAllowed);
    String lifecycleUrl = urlResolver.getUrlForSolution(Solution.LIFECYCLE, relativePathsNotAllowed);
    String repoManagerUrl = urlResolver.getUrlForSolution(Solution.REPO_MANAGER, relativePathsNotAllowed);
    String sbomManagerUrl = urlResolver.getUrlForSolution(Solution.SBOM_MANAGER, relativePathsNotAllowed);
    String guideUrl = urlResolver.getUrlForSolution(Solution.GUIDE, relativePathsNotAllowed);

    // then:
    assertThat(developerUrl).isBlank();
    assertThat(firewallUrl).isBlank();
    assertThat(lifecycleUrl).isBlank();
    assertThat(repoManagerUrl).isBlank();
    assertThat(sbomManagerUrl).isBlank();
    assertThat(guideUrl).isBlank();
  }

  @Test
  public void testFullUrls_DashboardUnavailable() {
    // given: no base url set in config and dashboard is unavailable
    BaseUrlConfiguration baseUrlConfig = new BaseUrlConfiguration(BASE_URL + '/', false);
    Configuration mockConfiguration = Mockito.mock(Configuration.class);
    DashboardUtils mockDashboardUtils = Mockito.mock(DashboardUtils.class);
    when(mockConfiguration.getBaseUrlConfiguration()).thenReturn(baseUrlConfig);
    when(mockDashboardUtils.isDashboardDisabled()).thenReturn(true);
    SolutionUrlResolver urlResolver = new SolutionUrlResolver(mockConfiguration, mockDashboardUtils);

    // when: get the URLs for the solutions
    String developerUrl = urlResolver.getUrlForSolution(Solution.DEVELOPER, true);
    String firewallUrl = urlResolver.getUrlForSolution(Solution.FIREWALL, true);
    String lifecycleUrl = urlResolver.getUrlForSolution(Solution.LIFECYCLE, true);
    String repoManagerUrl = urlResolver.getUrlForSolution(Solution.REPO_MANAGER, true);
    String sbomManagerUrl = urlResolver.getUrlForSolution(Solution.SBOM_MANAGER, true);
    String guideUrl = urlResolver.getUrlForSolution(Solution.GUIDE, true);

    // then: only lifecycle has an alternative path
    assertThat(developerUrl).isEqualTo("https://locahost:8070/ui/links/developer/dashboard");
    assertThat(firewallUrl).isEqualTo("https://locahost:8070/ui/links/firewall/dashboard");
    assertThat(lifecycleUrl).isEqualTo("https://locahost:8070/ui/links/lifecycle/reports");
    assertThat(repoManagerUrl).isBlank();
    assertThat(sbomManagerUrl).isEqualTo("https://locahost:8070/ui/links/sbomManager/dashboard");
    assertThat(guideUrl).isEqualTo("https://locahost:8070/assets/guide/index.html#/");
  }

  @Test
  public void testAiDeveloperUrlMatchesGuideSpa() {
    // AI Developer lives in the Guide SPA, so its tile always resolves to the same URL as Guide.
    Configuration mockConfiguration = Mockito.mock(Configuration.class);
    DashboardUtils mockDashboardUtils = Mockito.mock(DashboardUtils.class);
    when(mockDashboardUtils.isDashboardDisabled()).thenReturn(false);

    // with a base url
    when(mockConfiguration.getBaseUrlConfiguration()).thenReturn(new BaseUrlConfiguration(BASE_URL, false));
    SolutionUrlResolver withBase = new SolutionUrlResolver(mockConfiguration, mockDashboardUtils);
    assertThat(withBase.getUrlForSolution(Solution.AI_DEVELOPER, false))
        .isEqualTo("https://locahost:8070/assets/guide/index.html#/")
        .isEqualTo(withBase.getUrlForSolution(Solution.GUIDE, false));

    // no base url, relative paths allowed
    when(mockConfiguration.getBaseUrlConfiguration()).thenReturn(new BaseUrlConfiguration(null, false));
    SolutionUrlResolver noBase = new SolutionUrlResolver(mockConfiguration, mockDashboardUtils);
    assertThat(noBase.getUrlForSolution(Solution.AI_DEVELOPER, true))
        .isEqualTo("/assets/guide/index.html#/")
        .isEqualTo(noBase.getUrlForSolution(Solution.GUIDE, true));

    // no base url, relative paths not allowed
    assertThat(noBase.getUrlForSolution(Solution.AI_DEVELOPER, false)).isBlank();
  }
}
