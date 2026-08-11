/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.vulnerabilities;

import java.util.Comparator;
import java.util.List;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.dashboard.DashboardResource;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.report.ReportTestUtils;
import com.sonatype.insight.brain.search.index.SearchIndexClient;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.brain.service.InsightWork;

import org.junit.After;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class VulnerabilitiesListResourceTest
    extends AbstractResourceTest
{
  @After
  public void tearDownPreviewFlag() {
    tempEntity.deleteSystemConfigurationProperty(
        SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.getPropertyName());
  }

  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(DashboardResource.RESOURCE_PATH);
  }

  @Test
  public void listVulnerabilities_flagOn_returnsDistinctEstateRows() throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);

    Organization org = tempEntity.newOrganization("VulnListTribe");
    Application app = tempEntity.newApplication("Vuln App", "vuln-app", org.getId());
    seedComponentsReport(app, "vulnListReport");
    User reader = readerOn(org, "vuln-list-reader");
    VulnerabilitiesListTestSupport.populateIndex(lookup(SearchIndexClient.class));

    VulnerabilitiesListResponseDTO body = post(new VulnerabilitiesListRequestDTO(), reader)
        .getBody(VulnerabilitiesListResponseDTO.class);

    assertThat(body.source).isEqualTo(VulnerabilitiesListResponseDTO.SOURCE_INDEX);
    assertThat(body.total).isEqualTo(4);
    assertThat(body.vulnerabilities).hasSize(4);
    assertThat(body.vulnerabilities).extracting(row -> row.vulnerabilityId).doesNotHaveDuplicates();
    assertThat(body.vulnerabilities.get(0).vulnerabilityId).isNotBlank();
    assertThat(body.vulnerabilities.get(0).cvssScore).isNotNull();
    assertThat(body.vulnerabilities.get(0).severity).isNotBlank();
    assertThat(body.facets).isNotNull();
    assertThat(body.facets.totalVulnerabilities).isEqualTo(4);
  }

  @Test
  public void listVulnerabilities_flagOff_returns404() throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(false);

    HttpResponse response = post(new VulnerabilitiesListRequestDTO());

    assertResponseStatus(404, response);
  }

  @Test
  public void listVulnerabilities_defaultSort_ordersByCvssDescending() throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);

    Organization org = tempEntity.newOrganization("VulnSortTribe");
    Application app = tempEntity.newApplication("Sort Vuln App", "sort-vuln-app", org.getId());
    seedComponentsReport(app, "vulnSortReport");
    User reader = readerOn(org, "vuln-sort-reader");
    VulnerabilitiesListTestSupport.populateIndex(lookup(SearchIndexClient.class));

    VulnerabilitiesListResponseDTO body = post(new VulnerabilitiesListRequestDTO(), reader)
        .getBody(VulnerabilitiesListResponseDTO.class);

    List<Float> scores = body.vulnerabilities.stream().map(row -> row.cvssScore).toList();
    assertThat(scores).doesNotContainNull();
    assertThat(scores.stream().distinct().count()).isGreaterThan(1);
    assertThat(scores).isSortedAccordingTo(Comparator.nullsLast(Comparator.reverseOrder()));
  }

  @Test
  public void listVulnerabilities_unsupportedOrderBy_returns400() throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);

    VulnerabilitiesListRequestDTO request = new VulnerabilitiesListRequestDTO();
    request.orderBy = "-severity";

    assertResponseStatus(400, post(request));
  }

  @Test
  public void listVulnerabilities_sameCveAcrossApplications_countsOnce() throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);

    Organization org = tempEntity.newOrganization("VulnEstateTribe");
    Application appOne = tempEntity.newApplication("Estate App One", "estate-app-1", org.getId());
    Application appTwo = tempEntity.newApplication("Estate App Two", "estate-app-2", org.getId());
    seedComponentsReport(appOne, "estateVulnOne");
    seedComponentsReport(appTwo, "estateVulnTwo");
    User reader = readerOn(org, "vuln-estate-reader");
    VulnerabilitiesListTestSupport.populateIndex(lookup(SearchIndexClient.class));

    VulnerabilitiesListResponseDTO body = post(new VulnerabilitiesListRequestDTO(), reader)
        .getBody(VulnerabilitiesListResponseDTO.class);

    assertThat(body.total).isEqualTo(4);
    assertThat(body.vulnerabilities).hasSize(4);
    assertThat(body.vulnerabilities).extracting(row -> row.vulnerabilityId).doesNotHaveDuplicates();
  }

  @Test
  public void listVulnerabilities_catalogTab_usesCatalogSourceWithoutBlockingMyScanDataShape() throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);

    Organization org = tempEntity.newOrganization("VulnCatalogTribe");
    Application app = tempEntity.newApplication("Catalog App", "catalog-app", org.getId());
    seedComponentsReport(app, "catalogStubReport");
    User reader = readerOn(org, "vuln-catalog-reader");
    VulnerabilitiesListTestSupport.populateIndex(lookup(SearchIndexClient.class));

    VulnerabilitiesListRequestDTO request = new VulnerabilitiesListRequestDTO();
    request.tab = "catalog";
    VulnerabilitiesListResponseDTO body = post(request, reader).getBody(VulnerabilitiesListResponseDTO.class);

    // Catalog is Guide/HDS-backed; local fixtures may yield zero hits, but source must be catalog
    // and must not return My Scan Data index rows.
    assertThat(body.source).isEqualTo(VulnerabilitiesListResponseDTO.SOURCE_CATALOG);
    assertThat(body.page).isZero();
    assertThat(body.pageSize).isEqualTo(VulnerabilitiesListService.DEFAULT_PAGE_SIZE);
    assertThat(body.vulnerabilities).isNotNull();
  }

  @Test
  public void listVulnerabilities_failsClosed_userWithNoReadContexts() throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);

    Organization org = tempEntity.newOrganization("VulnFailClosedTribe");
    Application app = tempEntity.newApplication("Fail Closed App", "fail-closed-app", org.getId());
    seedComponentsReport(app, "failClosedVulnReport");
    User noPerms = tempEntity.newUser("vuln-no-permissions");
    VulnerabilitiesListTestSupport.populateIndex(lookup(SearchIndexClient.class));

    VulnerabilitiesListResponseDTO body = post(new VulnerabilitiesListRequestDTO(), noPerms)
        .getBody(VulnerabilitiesListResponseDTO.class);

    assertThat(body.total).isZero();
    assertThat(body.vulnerabilities).isEmpty();
  }

  @Test
  public void listVulnerabilities_unauthenticated_returns401() throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);

    HttpResponse response = restRequest()
        .path(VulnerabilitiesListResource.VULNERABILITIES_LIST_PATH)
        .body(new VulnerabilitiesListRequestDTO())
        .anon()
        .post();

    assertResponseStatus(401, response);
  }

  @Test
  public void listVulnerabilities_severityFilter_narrowsResultsAndKeepsSiblingFacets() throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);

    Organization org = tempEntity.newOrganization("VulnSeverityTribe");
    Application app = tempEntity.newApplication("Severity Vuln App", "severity-vuln-app", org.getId());
    seedComponentsReport(app, "severityFilterReport");
    User reader = readerOn(org, "vuln-severity-reader");
    VulnerabilitiesListTestSupport.populateIndex(lookup(SearchIndexClient.class));

    VulnerabilitiesListResponseDTO all = post(new VulnerabilitiesListRequestDTO(), reader)
        .getBody(VulnerabilitiesListResponseDTO.class);
    assertThat(all.total).isEqualTo(4);
    assertThat(all.facets.severities).isNotEmpty();

    String band = all.facets.severities.entrySet()
        .stream()
        .filter(entry -> entry.getValue() > 0 && entry.getValue() < all.total)
        .map(java.util.Map.Entry::getKey)
        .findFirst()
        .orElseThrow(() -> new AssertionError("Expected a non-empty partial severity band in facets"));

    VulnerabilitiesListRequestDTO request = new VulnerabilitiesListRequestDTO();
    request.severities = java.util.Set.of(band);
    VulnerabilitiesListResponseDTO body = post(request, reader).getBody(VulnerabilitiesListResponseDTO.class);

    assertThat(body.total).isEqualTo(all.facets.severities.get(band));
    assertThat(body.vulnerabilities).isNotEmpty();
    assertThat(body.vulnerabilities).allMatch(row -> band.equals(row.severity));
    assertThat(body.facets.severities.keySet())
        .contains("critical", "high", "medium", "low", "none");
    // Selecting a band must not zero the others, or the rail would offer nothing to widen to.
    assertThat(body.facets.severities).isEqualTo(all.facets.severities);
  }

  @Test
  public void listVulnerabilities_unsupportedFilter_returns400() throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);

    VulnerabilitiesListRequestDTO request = new VulnerabilitiesListRequestDTO();
    request.knownExploited = true;

    assertResponseStatus(400, post(request));
  }

  @Test
  public void listVulnerabilities_catalogOnlyFiltersOnMyScanData_return400() throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);

    VulnerabilitiesListRequestDTO malware = new VulnerabilitiesListRequestDTO();
    malware.malware = true;
    assertResponseStatus(400, post(malware));

    VulnerabilitiesListRequestDTO published = new VulnerabilitiesListRequestDTO();
    published.publishedWindow = "90d";
    assertResponseStatus(400, post(published));

    VulnerabilitiesListRequestDTO cwes = new VulnerabilitiesListRequestDTO();
    cwes.cwes = java.util.Set.of("CWE-79");
    assertResponseStatus(400, post(cwes));
  }

  @Test
  public void listVulnerabilities_catalogKevFilter_isAccepted() throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);

    Organization org = tempEntity.newOrganization("VulnCatalogKevTribe");
    Application app = tempEntity.newApplication("Catalog Kev App", "catalog-kev-app", org.getId());
    seedComponentsReport(app, "catalogKevStubReport");
    User reader = readerOn(org, "vuln-catalog-kev-reader");

    VulnerabilitiesListRequestDTO request = new VulnerabilitiesListRequestDTO();
    request.tab = "catalog";
    request.knownExploited = true;
    VulnerabilitiesListResponseDTO body = post(request, reader).getBody(VulnerabilitiesListResponseDTO.class);

    assertThat(body.source).isEqualTo(VulnerabilitiesListResponseDTO.SOURCE_CATALOG);
  }

  @Test
  public void listVulnerabilities_catalogEpssMinGreaterThanMax_returns400() throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);

    VulnerabilitiesListRequestDTO request = new VulnerabilitiesListRequestDTO();
    request.tab = "catalog";
    request.minEpssScore = 0.9f;
    request.maxEpssScore = 0.1f;

    assertResponseStatus(400, post(request));
  }

  @Test
  public void listVulnerabilities_catalogEpssOutsideDomain_returns400() throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);

    VulnerabilitiesListRequestDTO request = new VulnerabilitiesListRequestDTO();
    request.tab = "catalog";
    request.minEpssScore = -0.1f;

    assertResponseStatus(400, post(request));
  }

  @Test
  public void listVulnerabilities_catalogEpssMinOnly_isAccepted() throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);

    Organization org = tempEntity.newOrganization("VulnCatalogEpssTribe");
    Application app = tempEntity.newApplication("Catalog Epss App", "catalog-epss-app", org.getId());
    seedComponentsReport(app, "catalogEpssStubReport");
    User reader = readerOn(org, "vuln-catalog-epss-reader");

    VulnerabilitiesListRequestDTO request = new VulnerabilitiesListRequestDTO();
    request.tab = "catalog";
    request.minEpssScore = 0.5f;
    VulnerabilitiesListResponseDTO body = post(request, reader).getBody(VulnerabilitiesListResponseDTO.class);

    assertThat(body.source).isEqualTo(VulnerabilitiesListResponseDTO.SOURCE_CATALOG);
  }

  @Test
  public void listVulnerabilities_invalidSeverity_returns400() throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);

    VulnerabilitiesListRequestDTO request = new VulnerabilitiesListRequestDTO();
    request.severities = java.util.Set.of("urgent");

    assertResponseStatus(400, post(request));
  }

  @Test
  public void listVulnerabilities_cvssRangeFilter_narrowsResults() throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);

    Organization org = tempEntity.newOrganization("VulnCvssTribe");
    Application app = tempEntity.newApplication("Cvss Vuln App", "cvss-vuln-app", org.getId());
    seedComponentsReport(app, "cvssFilterReport");
    User reader = readerOn(org, "vuln-cvss-reader");
    VulnerabilitiesListTestSupport.populateIndex(lookup(SearchIndexClient.class));

    VulnerabilitiesListResponseDTO all = post(new VulnerabilitiesListRequestDTO(), reader)
        .getBody(VulnerabilitiesListResponseDTO.class);
    assertThat(all.total).isEqualTo(4);

    Float maxScore = all.vulnerabilities.stream()
        .map(row -> row.cvssScore)
        .filter(java.util.Objects::nonNull)
        .max(Float::compare)
        .orElseThrow();

    VulnerabilitiesListRequestDTO request = new VulnerabilitiesListRequestDTO();
    request.minCvssScore = maxScore;
    request.maxCvssScore = 10.0f;
    VulnerabilitiesListResponseDTO body = post(request, reader).getBody(VulnerabilitiesListResponseDTO.class);

    assertThat(body.total).isGreaterThan(0).isLessThanOrEqualTo(all.total);
    assertThat(body.vulnerabilities).isNotEmpty();
    assertThat(body.vulnerabilities).allMatch(row -> row.cvssScore != null && row.cvssScore >= maxScore);
  }

  @Test
  public void listVulnerabilities_ecosystemFilter_narrowsResults() throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);

    Organization org = tempEntity.newOrganization("VulnEcosystemTribe");
    Application app = tempEntity.newApplication("Ecosystem Vuln App", "ecosystem-vuln-app", org.getId());
    seedComponentsReport(app, "ecosystemFilterReport");
    User reader = readerOn(org, "vuln-ecosystem-reader");
    VulnerabilitiesListTestSupport.populateIndex(lookup(SearchIndexClient.class));

    VulnerabilitiesListResponseDTO all = post(new VulnerabilitiesListRequestDTO(), reader)
        .getBody(VulnerabilitiesListResponseDTO.class);
    assertThat(all.facets).isNotNull();
    assertThat(all.facets.ecosystems).isNotEmpty();

    String ecosystem = all.facets.ecosystems.entrySet()
        .stream()
        .filter(entry -> entry.getValue() > 0)
        .map(java.util.Map.Entry::getKey)
        .findFirst()
        .orElseThrow(() -> new AssertionError("Expected a non-empty ecosystem facet"));

    VulnerabilitiesListRequestDTO request = new VulnerabilitiesListRequestDTO();
    request.ecosystems = java.util.Set.of(ecosystem);
    VulnerabilitiesListResponseDTO body = post(request, reader).getBody(VulnerabilitiesListResponseDTO.class);

    assertThat(body.total).isEqualTo(all.facets.ecosystems.get(ecosystem));
    assertThat(body.vulnerabilities).isNotEmpty();
    assertThat(body.vulnerabilities).allMatch(row -> ecosystem.equalsIgnoreCase(row.ecosystem));
  }

  @Test
  public void listVulnerabilities_scopeFacets_countDistinctVulnerabilitiesPerOrgAppAndStage() throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);

    Organization org = tempEntity.newOrganization("VulnScopeFacetTribe");
    Application app = tempEntity.newApplication("Scope Facet App", "scope-facet-app", org.getId());
    seedComponentsReport(app, "scopeFacetReport");
    User reader = readerOn(org, "vuln-scope-facet-reader");
    VulnerabilitiesListTestSupport.populateIndex(lookup(SearchIndexClient.class));

    VulnerabilitiesListResponseDTO all = post(new VulnerabilitiesListRequestDTO(), reader)
        .getBody(VulnerabilitiesListResponseDTO.class);

    // Every vulnerability in this estate belongs to the one seeded app/org, so each scope facet
    // must carry the full distinct total — not the per-hit count, which is larger.
    assertThat(all.facets.organizations).containsEntry(org.getId(), all.total);
    assertThat(all.facets.applications).containsEntry(app.getId(), all.total);
    assertThat(all.facets.organizationNames).containsEntry(org.getId(), org.getName());
    assertThat(all.facets.applicationNames).containsEntry(app.getId(), app.getName());
    assertThat(all.facets.stages).isNotEmpty();
    assertThat(all.facets.stages.values()).allMatch(count -> count > 0 && count <= all.total);
    assertThat(all.facets.stageNames.keySet()).isEqualTo(all.facets.stages.keySet());
  }

  @Test
  public void listVulnerabilities_organizationFilter_narrowsToThatOrgAndKeepsItsOwnFacet() throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);

    Organization matching = tempEntity.newOrganization("VulnOrgFilterMatch");
    Organization other = tempEntity.newOrganization("VulnOrgFilterOther");
    Application matchingApp = tempEntity.newApplication("Org Filter App", "org-filter-app", matching.getId());
    Application otherApp = tempEntity.newApplication("Org Filter Other App", "org-filter-other-app", other.getId());
    seedComponentsReport(matchingApp, "orgFilterReport");
    seedComponentsReport(otherApp, "orgFilterOtherReport");
    User reader = readerOn(List.of(matching, other), "vuln-org-filter-reader");
    VulnerabilitiesListTestSupport.populateIndex(lookup(SearchIndexClient.class));

    VulnerabilitiesListResponseDTO all = post(new VulnerabilitiesListRequestDTO(), reader)
        .getBody(VulnerabilitiesListResponseDTO.class);
    assertThat(all.facets.organizations).containsKeys(matching.getId(), other.getId());
    long expected = all.facets.organizations.get(matching.getId());

    VulnerabilitiesListRequestDTO request = new VulnerabilitiesListRequestDTO();
    request.organizationIds = java.util.Set.of(matching.getId());
    VulnerabilitiesListResponseDTO body = post(request, reader).getBody(VulnerabilitiesListResponseDTO.class);

    assertThat(body.total).isEqualTo(expected);
    assertThat(body.vulnerabilities).isNotEmpty();
    // The selected dimension is omitted from its own aggregation, so the unselected sibling org
    // stays in the rail and remains selectable.
    assertThat(body.facets.organizations).containsKeys(matching.getId(), other.getId());
  }

  @Test
  public void listVulnerabilities_applicationFilter_narrowsResults() throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);

    Organization org = tempEntity.newOrganization("VulnAppFilterTribe");
    Application matching = tempEntity.newApplication("App Filter Match", "app-filter-match", org.getId());
    Application other = tempEntity.newApplication("App Filter Other", "app-filter-other", org.getId());
    seedComponentsReport(matching, "appFilterReport");
    seedComponentsReport(other, "appFilterOtherReport");
    User reader = readerOn(org, "vuln-app-filter-reader");
    VulnerabilitiesListTestSupport.populateIndex(lookup(SearchIndexClient.class));

    VulnerabilitiesListResponseDTO all = post(new VulnerabilitiesListRequestDTO(), reader)
        .getBody(VulnerabilitiesListResponseDTO.class);
    assertThat(all.facets.applications).containsKeys(matching.getId(), other.getId());
    long expected = all.facets.applications.get(matching.getId());

    VulnerabilitiesListRequestDTO request = new VulnerabilitiesListRequestDTO();
    request.applicationIds = java.util.Set.of(matching.getId());
    VulnerabilitiesListResponseDTO body = post(request, reader).getBody(VulnerabilitiesListResponseDTO.class);

    assertThat(body.total).isEqualTo(expected);
    assertThat(body.vulnerabilities).isNotEmpty();
    assertThat(body.facets.applications).containsKeys(matching.getId(), other.getId());
  }

  @Test
  public void listVulnerabilities_stageFilter_narrowsResults() throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);

    Organization org = tempEntity.newOrganization("VulnStageFilterTribe");
    Application app = tempEntity.newApplication("Stage Filter App", "stage-filter-app", org.getId());
    seedComponentsReport(app, "stageFilterReport");
    User reader = readerOn(org, "vuln-stage-filter-reader");
    VulnerabilitiesListTestSupport.populateIndex(lookup(SearchIndexClient.class));

    VulnerabilitiesListResponseDTO all = post(new VulnerabilitiesListRequestDTO(), reader)
        .getBody(VulnerabilitiesListResponseDTO.class);
    assertThat(all.facets.stages).isNotEmpty();

    java.util.Map.Entry<String, Long> stage = all.facets.stages.entrySet().iterator().next();

    VulnerabilitiesListRequestDTO request = new VulnerabilitiesListRequestDTO();
    request.stageIds = java.util.Set.of(stage.getKey());
    VulnerabilitiesListResponseDTO body = post(request, reader).getBody(VulnerabilitiesListResponseDTO.class);

    assertThat(body.total).isEqualTo(stage.getValue());
    assertThat(body.vulnerabilities).isNotEmpty();
  }

  @Test
  public void listVulnerabilities_blankScopeId_returns400() throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);

    VulnerabilitiesListRequestDTO request = new VulnerabilitiesListRequestDTO();
    request.organizationIds = java.util.Set.of("  ");

    assertResponseStatus(400, post(request));
  }

  @Test
  public void exportVulnerabilities_myScanData_returnsBomCsvWithBlastRadiusRows() throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);

    Organization org = tempEntity.newOrganization("VulnExportTribe");
    Application appOne = tempEntity.newApplication("Export App One", "export-app-1", org.getId());
    Application appTwo = tempEntity.newApplication("Export App Two", "export-app-2", org.getId());
    seedComponentsReport(appOne, "exportVulnOne");
    seedComponentsReport(appTwo, "exportVulnTwo");
    User reader = readerOn(org, "vuln-export-reader");
    VulnerabilitiesListTestSupport.populateIndex(lookup(SearchIndexClient.class));

    VulnerabilitiesListResponseDTO list = post(new VulnerabilitiesListRequestDTO(), reader)
        .getBody(VulnerabilitiesListResponseDTO.class);
    assertThat(list.total).isEqualTo(4);

    HttpResponse response = restRequest()
        .auth(reader)
        .path(VulnerabilitiesListResource.VULNERABILITIES_EXPORT_PATH)
        .part("filter", new VulnerabilitiesListRequestDTO())
        .post();

    assertResponseStatus(200, response);
    String csv = response.getBodyText();
    assertThat(csv).startsWith("\uFEFF");
    assertThat(csv).contains(VulnerabilitiesBlastRadiusRowDTO.getCsvHeader());
    long dataRows = java.util.Arrays.stream(csv.split("\\r?\\n"))
        .skip(1)
        .filter(line -> !line.isBlank())
        .count();
    assertThat(dataRows).isGreaterThan(list.total);
  }

  @Test
  public void exportVulnerabilities_catalogTab_returns400() throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);

    VulnerabilitiesListRequestDTO request = new VulnerabilitiesListRequestDTO();
    request.tab = "catalog";

    HttpResponse response = restRequest()
        .path(VulnerabilitiesListResource.VULNERABILITIES_EXPORT_PATH)
        .part("filter", request)
        .post();

    assertResponseStatus(400, response);
  }

  @Test
  public void exportVulnerabilities_unauthenticated_returns401() throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);

    HttpResponse response = restRequest()
        .path(VulnerabilitiesListResource.VULNERABILITIES_EXPORT_PATH)
        .part("filter", new VulnerabilitiesListRequestDTO())
        .anon()
        .post();

    assertResponseStatus(401, response);
  }

  @Test
  public void exportVulnerabilities_failsClosed_userWithNoReadContexts() throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);

    Organization org = tempEntity.newOrganization("VulnExportFailClosedTribe");
    Application app = tempEntity.newApplication("Export Fail Closed App", "export-fail-closed-app", org.getId());
    seedComponentsReport(app, "exportFailClosedVuln");
    User noPerms = tempEntity.newUser("vuln-export-no-permissions");
    VulnerabilitiesListTestSupport.populateIndex(lookup(SearchIndexClient.class));

    HttpResponse response = restRequest()
        .auth(noPerms)
        .path(VulnerabilitiesListResource.VULNERABILITIES_EXPORT_PATH)
        .part("filter", new VulnerabilitiesListRequestDTO())
        .post();

    assertResponseStatus(200, response);
    String csv = response.getBodyText();
    long dataRows = java.util.Arrays.stream(csv.split("\\r?\\n"))
        .skip(1)
        .filter(line -> !line.isBlank())
        .count();
    assertThat(dataRows).isZero();
  }

  @Test
  public void exportVulnerabilities_scopedUser_seesOnlyReadableApplications() throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);

    Organization org = tempEntity.newOrganization("VulnExportScopedTribe");
    Application readable = tempEntity.newApplication("Export Readable App", "export-readable-app", org.getId());
    Application hidden = tempEntity.newApplication("Export Hidden App", "export-hidden-app", org.getId());
    seedComponentsReport(readable, "exportScopedReadable");
    seedComponentsReport(hidden, "exportScopedHidden");

    User reader = tempEntity.newUser("vuln-export-scoped-reader");
    Role readRole = tempEntity.newRole(false /* global */, Permission.READ);
    tempEntity.newMembershipMapping(readable.getId(), readRole.getId(), reader.getUsername());
    VulnerabilitiesListTestSupport.populateIndex(lookup(SearchIndexClient.class));

    HttpResponse response = restRequest()
        .auth(reader)
        .path(VulnerabilitiesListResource.VULNERABILITIES_EXPORT_PATH)
        .part("filter", new VulnerabilitiesListRequestDTO())
        .post();

    assertResponseStatus(200, response);
    String csv = response.getBodyText();
    assertThat(csv).contains("Export Readable App");
    assertThat(csv).doesNotContain("Export Hidden App");
    long dataRows = java.util.Arrays.stream(csv.split("\\r?\\n"))
        .skip(1)
        .filter(line -> !line.isBlank())
        .count();
    assertThat(dataRows).isGreaterThan(0);
  }

  private void seedComponentsReport(Application app, String scanId) throws Exception {
    PolicyEvaluation evaluation = tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, scanId);
    ReportTestUtils.createReportFile(evaluation.getOwnerId(), evaluation.getScanId(),
        ReportTestUtils.zipReportDir("/IndexSearchingTest/componentsMetricReport", tempDir),
        lookup(InsightWork.class));
  }

  private User readerOn(Organization org, String username) {
    return readerOn(List.of(org), username);
  }

  private User readerOn(List<Organization> orgs, String username) {
    User reader = tempEntity.newUser(username);
    Role readRole = tempEntity.newRole(false /* global */, Permission.READ);
    for (Organization org : orgs) {
      tempEntity.newMembershipMapping(org.getId(), readRole.getId(), reader.getUsername());
    }
    return reader;
  }

  private HttpResponse post(VulnerabilitiesListRequestDTO request) throws Exception {
    return restRequest()
        .path(VulnerabilitiesListResource.VULNERABILITIES_LIST_PATH)
        .body(request)
        .post();
  }

  private HttpResponse post(VulnerabilitiesListRequestDTO request, User user) throws Exception {
    return restRequest()
        .auth(user)
        .path(VulnerabilitiesListResource.VULNERABILITIES_LIST_PATH)
        .body(request)
        .post();
  }
}
