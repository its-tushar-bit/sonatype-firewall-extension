/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sonatype.insight.brain.api.v2.dto.ApiLicensedSolutionDTO;
import com.sonatype.insight.brain.dashboard.DashboardUtils;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.service.BaseUrlConfiguration;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.brain.solution.SolutionResolver;
import com.sonatype.insight.brain.solution.SolutionUrlResolver;
import com.sonatype.insight.license.model.ProductLicenseDetails;

import org.junit.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class ApiLicensedSolutionsServiceTest
{

  @Test
  public void testGetLicensedSolutions_expectRelativeUrls() {
    // given: config with no base url and licensed for all products
    final String baseUrl = "";
    final boolean licenseHasProducts = true;
    ApiLicensedSolutionService solutionService = new ApiLicensedSolutionService(
        createSolutionResolver(licenseHasProducts),
        createSolutionUrlResolver(baseUrl));

    // when:
    final boolean allowRelativeUrls = true;
    List<ApiLicensedSolutionDTO> licensedSolutions = solutionService.getLicensedSolutions(allowRelativeUrls);

    // then: results have all solutions with relative URLs (Guide excluded: GUIDE_UI flag requires integration context)
    assertThat(licensedSolutions).hasSize(4); // repo manager and guide not included in unit tests
    assertThat(toMap(licensedSolutions))
        .containsEntry("developer", "/ui/links/developer/dashboard")
        .containsEntry("firewall", "/ui/links/firewall/dashboard")
        .containsEntry("lifecycle", "/ui/links/lifecycle/dashboard")
        .containsEntry("sbom", "/ui/links/sbomManager/dashboard");
  }

  @Test
  public void testGetLicensedSolutions_expectFullUrls() {
    // given: config with a base url and licensed for all products
    final String baseUrl = "https://localhost:8443";
    final boolean licenseHasProducts = true;
    ApiLicensedSolutionService solutionService = new ApiLicensedSolutionService(
        createSolutionResolver(licenseHasProducts),
        createSolutionUrlResolver(baseUrl));

    // when:
    final boolean allowRelativeUrls = false;
    List<ApiLicensedSolutionDTO> licensedSolutions = solutionService.getLicensedSolutions(allowRelativeUrls);

    // then: results have all solutions with full URLs (Guide excluded: GUIDE_UI flag requires integration context)
    assertThat(licensedSolutions).hasSize(4); // repo manager and guide not included in unit tests
    assertThat(toMap(licensedSolutions))
        .containsEntry("developer", "https://localhost:8443/ui/links/developer/dashboard")
        .containsEntry("firewall", "https://localhost:8443/ui/links/firewall/dashboard")
        .containsEntry("lifecycle", "https://localhost:8443/ui/links/lifecycle/dashboard")
        .containsEntry("sbom", "https://localhost:8443/ui/links/sbomManager/dashboard");
  }

  @Test
  public void testGetLicensedSolutions_expectFullUrlsEvenIfRelativeAllowed() {
    // given: config with a base url and licensed for all products
    final String baseUrl = "https://localhost:8443";
    final boolean licenseHasProducts = true;
    ApiLicensedSolutionService solutionService = new ApiLicensedSolutionService(
        createSolutionResolver(licenseHasProducts),
        createSolutionUrlResolver(baseUrl));

    // when:
    final boolean allowRelativeUrls = true;
    List<ApiLicensedSolutionDTO> licensedSolutions = solutionService.getLicensedSolutions(allowRelativeUrls);

    // then: results have all solutions with full URLs (Guide excluded: GUIDE_UI flag requires integration context)
    assertThat(licensedSolutions).hasSize(4); // repo manager and guide not included in unit tests
    assertThat(toMap(licensedSolutions))
        .containsEntry("developer", "https://localhost:8443/ui/links/developer/dashboard")
        .containsEntry("firewall", "https://localhost:8443/ui/links/firewall/dashboard")
        .containsEntry("lifecycle", "https://localhost:8443/ui/links/lifecycle/dashboard")
        .containsEntry("sbom", "https://localhost:8443/ui/links/sbomManager/dashboard");
  }

  @Test
  public void testGetLicensedSolutions_expectNoResults() {
    // these combinations of values should produce an empty result set
    //
    // baseUrl hasProducts allowRelativeUrls
    verifyNoResults("", true, false); // no baseUrl and relative URLs not allowed

    verifyNoResults("", false, false); // the license doesn't contain any products
    verifyNoResults("", false, true); // same
    verifyNoResults("https://localhost:8443", false, false); // same
    verifyNoResults("https://localhost:8443", false, true); // same
  }

  private void verifyNoResults(
      String baseUrl,
      boolean hasLicensedProducts,
      boolean allowRelativeUrls)
  {
    // given: config with no base url and license for all products
    ApiLicensedSolutionService solutionService = new ApiLicensedSolutionService(
        createSolutionResolver(hasLicensedProducts),
        createSolutionUrlResolver(baseUrl));

    // when:
    List<ApiLicensedSolutionDTO> licensedSolutions = solutionService.getLicensedSolutions(allowRelativeUrls);

    // then:
    assertThat(licensedSolutions).isEmpty();
  }

  private Map<String, String> toMap(List<ApiLicensedSolutionDTO> licensedSolutions) {
    Map<String, String> solutionMap = new HashMap<>();
    for (ApiLicensedSolutionDTO licensedSolution : licensedSolutions) {
      solutionMap.put(licensedSolution.id, licensedSolution.url);
    }
    return solutionMap;
  }

  private SolutionResolver createSolutionResolver(boolean withProducts) {
    ProductLicense productLicense = Mockito.mock(ProductLicense.class);
    when(productLicense.hasProduct(any())).thenReturn(withProducts);
    // Guide requires GUIDE_UI feature flag backed by a DAO — exclude from unit tests
    when(productLicense.hasProduct(ProductLicenseDetails.PRODUCT_GUIDE_SELF_HOSTED)).thenReturn(false);
    return new SolutionResolver(productLicense);
  }

  private SolutionUrlResolver createSolutionUrlResolver(String baseUrl) {
    BaseUrlConfiguration baseUrlConfiguration = new BaseUrlConfiguration(baseUrl, false);
    Configuration mockConfiguration = Mockito.mock(Configuration.class);
    DashboardUtils dashboardUtils = Mockito.mock(DashboardUtils.class);
    when(mockConfiguration.getBaseUrlConfiguration()).thenReturn(baseUrlConfiguration);
    return new SolutionUrlResolver(mockConfiguration, dashboardUtils);
  }
}
