/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiLicensedSolutionDTO;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.license.model.ProductLicenseDetails;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

public class ApiLicensedSolutionsResourceTest
    extends AbstractResourceTest
{
  @Before
  public void setUp() {
    when(mockDeveloperEnablementService.shouldEnableDeveloperProduct()).thenReturn(true);
  }

  @Test
  public void testGetLicensedSolutions_expectRelativeUrls() throws Exception {
    // given: config with no base url and licensed for firewall
    setBaseUrl(null);
    licenseManager.setProducts(
        ProductLicenseDetails.PRODUCT_FIREWALL);

    // when:
    final boolean allowRelativeUrls = true;
    HttpResponse response = restRequest().query("allowRelativeUrls", allowRelativeUrls).get();

    // then: results have firewall with relative URL
    assertResponseStatus(200, response);
    List<ApiLicensedSolutionDTO> licensedSolutions = response.getBodyList(ApiLicensedSolutionDTO.class);
    assertThat(licensedSolutions).hasSize(2);
    assertThat(toMap(licensedSolutions))
        .containsEntry("firewall", "/ui/links/firewall/dashboard");
  }

  @Test
  public void testGetLicensedSolutions_expectFullUrls() throws Exception {
    // given: config with a base url and licensed for lifecycle and developer
    setBaseUrl("https://localhost:8443");
    licenseManager.setProducts(
        ProductLicenseDetails.PRODUCT_TEAMS_EDITION);

    // when:
    final boolean allowRelativeUrls = false;
    HttpResponse response = restRequest().query("allowRelativeUrls", allowRelativeUrls).get();

    // then: results have solutions with full URLs
    assertResponseStatus(200, response);
    List<ApiLicensedSolutionDTO> licensedSolutions = response.getBodyList(ApiLicensedSolutionDTO.class);
    assertThat(licensedSolutions).hasSize(2);
    assertThat(toMap(licensedSolutions))
        .containsEntry("developer", "https://localhost:8443/ui/links/developer/dashboard")
        .containsEntry("lifecycle", "https://localhost:8443/ui/links/lifecycle/dashboard");
  }

  @Test
  public void testGetLicensedSolutions_expectFullUrlsEvenIfRelativeAllowed() throws Exception {
    // given: config with a base url and licensed for sbom manager
    setBaseUrl("https://localhost:8443");
    licenseManager.setProducts(
        ProductLicenseDetails.PRODUCT_SBOM_MANAGER,
        ProductLicenseDetails.PRODUCT_SBOM_MANAGER_SAAS);

    // when:
    final boolean allowRelativeUrls = true;
    HttpResponse response = restRequest().query("allowRelativeUrls", allowRelativeUrls).get();

    // then: results have solutions with full URLs
    assertResponseStatus(200, response);
    List<ApiLicensedSolutionDTO> licensedSolutions = response.getBodyList(ApiLicensedSolutionDTO.class);
    assertThat(licensedSolutions).hasSize(2);
    assertThat(toMap(licensedSolutions))
        .containsEntry("sbom", "https://localhost:8443/ui/links/sbomManager/dashboard");
  }

  @Test
  public void testGetLicensedSolutions_expectNoResultsWithBaseUrlNotSet() throws Exception {
    // given: config with no base url and licensed for sbom manager
    setBaseUrl(null);
    licenseManager.setProducts(
        ProductLicenseDetails.PRODUCT_SBOM_MANAGER,
        ProductLicenseDetails.PRODUCT_SBOM_MANAGER_SAAS);

    // when:
    final boolean allowRelativeUrls = false;
    HttpResponse response = restRequest().query("allowRelativeUrls", allowRelativeUrls).get();

    // then: there are no results since relative URLs not allowed
    assertResponseStatus(200, response);
    List<ApiLicensedSolutionDTO> licensedSolutions = response.getBodyList(ApiLicensedSolutionDTO.class);
    assertThat(licensedSolutions).isEmpty();
  }

  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(PublicApiPaths.LICENSED_SOLUTIONS_RESOURCE_PATH);
  }

  private Map<String, String> toMap(List<ApiLicensedSolutionDTO> licensedSolutions) {
    Map<String, String> solutionMap = new HashMap<>();
    for (ApiLicensedSolutionDTO licensedSolution : licensedSolutions) {
      solutionMap.put(licensedSolution.id, licensedSolution.url);
    }
    return solutionMap;
  }
}
