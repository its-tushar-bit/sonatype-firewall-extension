/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiLicensedSolutionDTO;
import com.sonatype.insight.brain.developer.integrationdashboard.DeveloperEnablementService;
import com.sonatype.insight.license.model.ProductLicenseDetails;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@IqH2Test
class IqH2ApiLicensedSolutionsResourceTest
{
  private IqTestContext ctx;

  @BeforeEach
  void setUp() {
    when(ctx.lookup(DeveloperEnablementService.class).shouldEnableDeveloperProduct()).thenReturn(true);
  }

  private com.sonatype.insight.brain.HttpRequest restRequest() {
    return ctx.restRequest().path(PublicApiPaths.LICENSED_SOLUTIONS_RESOURCE_PATH);
  }

  @Test
  void testGetLicensedSolutions_expectRelativeUrls() throws Exception {
    // given: config with no base url and licensed for firewall
    ctx.setBaseUrl(null);
    ctx.setLicenseProducts(
        ProductLicenseDetails.PRODUCT_FIREWALL);

    // when:
    final boolean allowRelativeUrls = true;
    HttpResponse response = restRequest().query("allowRelativeUrls", allowRelativeUrls).get();

    // then: results have firewall with relative URL
    ctx.assertResponseStatus(200, response);
    List<ApiLicensedSolutionDTO> licensedSolutions = response.getBodyList(ApiLicensedSolutionDTO.class);
    assertThat(licensedSolutions).hasSize(2);
    assertThat(toMap(licensedSolutions))
        .containsEntry("firewall", "/ui/links/firewall/dashboard");
  }

  @Test
  void testGetLicensedSolutions_expectFullUrls() throws Exception {
    // given: config with a base url and licensed for lifecycle and developer
    ctx.setBaseUrl("https://localhost:8443");
    ctx.setLicenseProducts(
        ProductLicenseDetails.PRODUCT_TEAMS_EDITION);

    // when:
    final boolean allowRelativeUrls = false;
    HttpResponse response = restRequest().query("allowRelativeUrls", allowRelativeUrls).get();

    // then: results have solutions with full URLs
    ctx.assertResponseStatus(200, response);
    List<ApiLicensedSolutionDTO> licensedSolutions = response.getBodyList(ApiLicensedSolutionDTO.class);
    assertThat(licensedSolutions).hasSize(2);
    assertThat(toMap(licensedSolutions))
        .containsEntry("developer", "https://localhost:8443/ui/links/developer/dashboard")
        .containsEntry("lifecycle", "https://localhost:8443/ui/links/lifecycle/dashboard");
  }

  @Test
  void testGetLicensedSolutions_expectFullUrlsEvenIfRelativeAllowed() throws Exception {
    // given: config with a base url and licensed for sbom manager
    ctx.setBaseUrl("https://localhost:8443");
    ctx.setLicenseProducts(
        ProductLicenseDetails.PRODUCT_SBOM_MANAGER,
        ProductLicenseDetails.PRODUCT_SBOM_MANAGER_SAAS);

    // when:
    final boolean allowRelativeUrls = true;
    HttpResponse response = restRequest().query("allowRelativeUrls", allowRelativeUrls).get();

    // then: results have solutions with full URLs
    ctx.assertResponseStatus(200, response);
    List<ApiLicensedSolutionDTO> licensedSolutions = response.getBodyList(ApiLicensedSolutionDTO.class);
    assertThat(licensedSolutions).hasSize(2);
    assertThat(toMap(licensedSolutions))
        .containsEntry("sbom", "https://localhost:8443/ui/links/sbomManager/dashboard");
  }

  @Test
  void testGetLicensedSolutions_expectNoResultsWithBaseUrlNotSet() throws Exception {
    // given: config with no base url and licensed for sbom manager
    ctx.setBaseUrl(null);
    ctx.setLicenseProducts(
        ProductLicenseDetails.PRODUCT_SBOM_MANAGER,
        ProductLicenseDetails.PRODUCT_SBOM_MANAGER_SAAS);

    // when:
    final boolean allowRelativeUrls = false;
    HttpResponse response = restRequest().query("allowRelativeUrls", allowRelativeUrls).get();

    // then: there are no results since relative URLs not allowed
    ctx.assertResponseStatus(200, response);
    List<ApiLicensedSolutionDTO> licensedSolutions = response.getBodyList(ApiLicensedSolutionDTO.class);
    assertThat(licensedSolutions).isEmpty();
  }

  private Map<String, String> toMap(List<ApiLicensedSolutionDTO> licensedSolutions) {
    Map<String, String> solutionMap = new HashMap<>();
    for (ApiLicensedSolutionDTO licensedSolution : licensedSolutions) {
      solutionMap.put(licensedSolution.id, licensedSolution.url);
    }
    return solutionMap;
  }
}
