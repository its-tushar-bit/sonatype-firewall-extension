/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.components;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.OwnerComponentDAO;
import com.sonatype.insight.brain.dataaccess.OwnerComponentDAO.ComponentOrganizationUsageRow;
import com.sonatype.insight.brain.dataaccess.OwnerComponentDAO.ComponentOwnerUsageRow;
import com.sonatype.insight.brain.dataaccess.OwnerComponentDAO.ComponentReportUsageRow;
import com.sonatype.insight.brain.dataaccess.OwnerComponentDAO.PagedOrganizationsByHash;
import com.sonatype.insight.brain.dataaccess.OwnerComponentDAO.PagedOwnersByHash;
import com.sonatype.insight.brain.dataaccess.OwnerComponentDAO.PagedReportsByHashAndOwner;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.security.UserPrincipal;
import com.sonatype.insight.brain.organization.ApplicationService;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.search.session.ReadableContextAuthzCache;
import com.sonatype.insight.brain.security.CurrentUser;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.license.model.LicensedFeature;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ComponentUsageServiceTest
{
  @Mock
  private OwnerComponentDAO ownerComponentDAO;

  @Mock
  private ApplicationService applicationService;

  @Mock
  private OrganizationDAO organizationDAO;

  @Mock
  private ProductLicense productLicense;

  @Mock
  private ReadableContextAuthzCache readableContextAuthzCache;

  @Mock
  private CurrentUser currentUser;

  private final UserPrincipal principal = new UserPrincipal("admin", "Admin", "default");

  private ComponentUsageService service;

  @BeforeEach
  public void setUp() {
    service = new ComponentUsageService(
        ownerComponentDAO,
        applicationService,
        organizationDAO,
        productLicense,
        readableContextAuthzCache,
        currentUser);
    lenient().doNothing().when(productLicense).validateFeature(LicensedFeature.DASHBOARD);
    lenient().when(currentUser.getUserPrincipal()).thenReturn(principal);
  }

  @Test
  public void listApplications_requiresComponentHash() {
    assertThatThrownBy(() -> service.listApplications(new ComponentUsageRequestDTO()))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("componentHash");
  }

  @Test
  public void listOrganizations_requiresComponentHash() {
    assertThatThrownBy(() -> service.listOrganizations(new ComponentUsageRequestDTO()))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("componentHash");
  }

  @Test
  public void listReports_rejectsBlankApplicationId() {
    ComponentUsageReportsRequestDTO request = new ComponentUsageReportsRequestDTO();
    request.componentHash = "hash1";
    request.applicationId = " ";

    assertThatThrownBy(() -> service.listReports(request))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("applicationId");
  }

  @Test
  public void listReports_rejectsHashLongerThanOwnerComponentColumn() {
    ComponentUsageReportsRequestDTO request = reportsRequest(
        "a".repeat(ComponentUsageService.MAX_COMPONENT_HASH_LENGTH + 1), "app-1");

    assertThatThrownBy(() -> service.listReports(request))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("maximum length of " + ComponentUsageService.MAX_COMPONENT_HASH_LENGTH);
  }

  @Test
  public void listReports_rejectsOversizedPage() {
    ComponentUsageReportsRequestDTO request = reportsRequest("hash1", "app-1");
    request.pageSize = 101;

    assertThatThrownBy(() -> service.listReports(request))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("page size");
  }

  @Test
  public void listReports_failClosedWhenAppIsNotReadable() {
    when(readableContextAuthzCache.resolveReadableContexts(principal)).thenReturn(Optional.of(Map.of(
        "app-2", OwnerType.APPLICATION)));

    ComponentUsageReportsRequestDTO request = reportsRequest("hash1", "app-1");

    ComponentUsageReportsResponseDTO response = service.listReports(request);

    assertThat(response.total).isZero();
    assertThat(response.reports).isEmpty();
    assertThat(response.page).isZero();
    assertThat(response.pageSize).isEqualTo(ComponentUsageService.DEFAULT_PAGE_SIZE);
    assertThat(response.hasNextPage).isFalse();
    assertThat(response.applicationId).isEqualTo("app-1");
    assertThat(response.applicationPublicId).isNull();
    verify(ownerComponentDAO, never()).findReportsByHashAndOwnerPaged(any(), any(), anyInt(), anyInt());
    verify(applicationService, never()).getAppsByIds(any(), any(), any());
  }

  @Test
  public void listReports_unrestricted_usesApplicationIdWithoutOwnerScopeCheck() {
    when(readableContextAuthzCache.resolveReadableContexts(principal)).thenReturn(Optional.empty());
    when(ownerComponentDAO.findReportsByHashAndOwnerPaged(eq("hash1"), eq("app-1"), eq(0), eq(25)))
        .thenReturn(new PagedReportsByHashAndOwner(1L, List.of(
            new ComponentReportUsageRow("report-1", "build", new Date(1_700_000_000_000L)))));
    when(applicationService.getAppsByIds(isNull(), eq(Set.of("app-1")), isNull()))
        .thenReturn(List.of(application("app-1", "org-1")));

    ComponentUsageReportsResponseDTO response = service.listReports(reportsRequest("hash1", "app-1"));

    assertThat(response.total).isEqualTo(1L);
    assertThat(response.reports).hasSize(1);
    assertThat(response.applicationId).isEqualTo("app-1");
    assertThat(response.applicationPublicId).isEqualTo("public-1");
    verify(ownerComponentDAO).findReportsByHashAndOwnerPaged("hash1", "app-1", 0, 25);
  }

  @Test
  public void listReports_returnsPagedRows_whenAppReadable() {
    when(readableContextAuthzCache.resolveReadableContexts(principal)).thenReturn(Optional.of(Map.of(
        "app-1", OwnerType.APPLICATION)));
    when(ownerComponentDAO.findReportsByHashAndOwnerPaged(eq("hash1"), eq("app-1"), eq(25), eq(25)))
        .thenReturn(new PagedReportsByHashAndOwner(40L, List.of(
            new ComponentReportUsageRow("report-1", "build", new Date(1_700_000_000_000L)))));
    when(applicationService.getAppsByIds(isNull(), eq(Set.of("app-1")), isNull()))
        .thenReturn(List.of(application("app-1", "org-1")));

    ComponentUsageReportsRequestDTO request = reportsRequest("hash1", "app-1");
    request.page = 1;

    ComponentUsageReportsResponseDTO response = service.listReports(request);

    assertThat(response.total).isEqualTo(40L);
    assertThat(response.page).isEqualTo(1);
    assertThat(response.pageSize).isEqualTo(ComponentUsageService.DEFAULT_PAGE_SIZE);
    // offset 25 + 1 row < 40 → another page remains
    assertThat(response.hasNextPage).isTrue();
    assertThat(response.applicationPublicId).isEqualTo("public-1");
    assertThat(response.reports).hasSize(1);
    assertThat(response.reports.get(0).reportId).isEqualTo("report-1");
    assertThat(response.reports.get(0).stageTypeId).isEqualTo("build");
    assertThat(response.reports.get(0).evaluationTime).isEqualTo(1_700_000_000_000L);
    verify(applicationService, never()).getApplications();
  }

  @Test
  public void listReports_hasNextPage_whenPartialFirstPage() {
    when(readableContextAuthzCache.resolveReadableContexts(principal)).thenReturn(Optional.of(Map.of(
        "app-1", OwnerType.APPLICATION)));
    when(ownerComponentDAO.findReportsByHashAndOwnerPaged(eq("hash1"), eq("app-1"), eq(0), eq(1)))
        .thenReturn(new PagedReportsByHashAndOwner(3L, List.of(
            new ComponentReportUsageRow("report-1", "build", new Date(1_700_000_000_000L)))));
    when(applicationService.getAppsByIds(isNull(), eq(Set.of("app-1")), isNull()))
        .thenReturn(List.of(application("app-1", "org-1")));

    ComponentUsageReportsRequestDTO request = reportsRequest("hash1", "app-1");
    request.page = 0;
    request.pageSize = 1;

    ComponentUsageReportsResponseDTO response = service.listReports(request);

    assertThat(response.total).isEqualTo(3L);
    assertThat(response.page).isZero();
    assertThat(response.pageSize).isEqualTo(1);
    assertThat(response.hasNextPage).isTrue();
    assertThat(response.reports).hasSize(1);
  }

  @Test
  public void listReports_rejectsApplicationIdLongerThanColumn() {
    ComponentUsageReportsRequestDTO request = reportsRequest(
        "hash1", "a".repeat(ComponentUsageService.MAX_APPLICATION_ID_LENGTH + 1));

    assertThatThrownBy(() -> service.listReports(request))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("maximum length of " + ComponentUsageService.MAX_APPLICATION_ID_LENGTH);
  }

  @Test
  public void listApplications_rejectsHashLongerThanOwnerComponentColumn() {
    ComponentUsageRequestDTO request = new ComponentUsageRequestDTO();
    request.componentHash = "a".repeat(ComponentUsageService.MAX_COMPONENT_HASH_LENGTH + 1);

    assertThatThrownBy(() -> service.listApplications(request))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("maximum length of " + ComponentUsageService.MAX_COMPONENT_HASH_LENGTH);
  }

  @Test
  public void listApplications_unrestricted_usesNullOwnerScope() {
    when(readableContextAuthzCache.resolveReadableContexts(principal)).thenReturn(Optional.empty());

    Application app = application("app-1", "org-1");
    when(ownerComponentDAO.findDistinctOwnersByHashPaged(eq("hash1"), isNull(), eq(0), eq(25), isNull(), isNull()))
        .thenReturn(new PagedOwnersByHash(1L, List.of(
            new ComponentOwnerUsageRow("app-1", new Date(1_700_000_000_000L)))));
    when(applicationService.getAppsByIds(isNull(), any(), isNull())).thenReturn(List.of(app));
    when(ownerComponentDAO.getStageTypeIdsByOwnerIdForHash(eq("hash1"), any()))
        .thenReturn(Map.of("app-1", List.of("build")));
    when(organizationDAO.getByIds(any())).thenReturn(List.of(organization("org-1", "Org One")));

    ComponentUsageRequestDTO request = new ComponentUsageRequestDTO();
    request.componentHash = "hash1";

    ComponentUsageApplicationsResponseDTO response = service.listApplications(request);

    assertThat(response.total).isEqualTo(1);
    assertThat(response.applications).hasSize(1);
    verify(applicationService, never()).getApplications();
    verify(ownerComponentDAO).findDistinctOwnersByHashPaged("hash1", null, 0, 25, null, null);
  }

  @Test
  public void listApplications_restricted_pagesReadableOwners() {
    when(readableContextAuthzCache.resolveReadableContexts(principal)).thenReturn(Optional.of(Map.of(
        "app-1", OwnerType.APPLICATION,
        "org-1", OwnerType.ORGANIZATION)));

    Application app = application("app-1", "org-1");
    when(ownerComponentDAO.findDistinctOwnersByHashPaged(eq("hash1"), eq(Set.of("app-1")), eq(0), eq(25), isNull(),
        isNull()))
            .thenReturn(new PagedOwnersByHash(1L, List.of(
                new ComponentOwnerUsageRow("app-1", new Date(1_700_000_000_000L)))));
    when(applicationService.getAppsByIds(isNull(), any(), isNull())).thenReturn(List.of(app));
    when(ownerComponentDAO.getStageTypeIdsByOwnerIdForHash(eq("hash1"), any()))
        .thenReturn(Map.of("app-1", List.of("build", "release")));
    when(organizationDAO.getByIds(any())).thenReturn(List.of(organization("org-1", "Org One")));

    ComponentUsageRequestDTO request = new ComponentUsageRequestDTO();
    request.componentHash = "hash1";

    ComponentUsageApplicationsResponseDTO response = service.listApplications(request);

    assertThat(response.total).isEqualTo(1);
    assertThat(response.hasNextPage).isFalse();
    assertThat(response.applications).hasSize(1);
    assertThat(response.applications.get(0).applicationPublicId).isEqualTo("public-1");
    assertThat(response.applications.get(0).organizationName).isEqualTo("Org One");
    assertThat(response.applications.get(0).stageTypeIds).containsExactly("build", "release");
    verify(applicationService, never()).getApplications();
  }

  @Test
  public void listApplications_failClosedWhenNoReadableContexts() {
    when(readableContextAuthzCache.resolveReadableContexts(principal)).thenReturn(Optional.of(Map.of()));
    when(ownerComponentDAO.findDistinctOwnersByHashPaged(eq("hash1"), eq(Set.of()), eq(0), eq(25), isNull(), isNull()))
        .thenReturn(new PagedOwnersByHash(0L, List.of()));

    ComponentUsageRequestDTO request = new ComponentUsageRequestDTO();
    request.componentHash = "hash1";

    ComponentUsageApplicationsResponseDTO response = service.listApplications(request);

    assertThat(response.total).isZero();
    assertThat(response.applications).isEmpty();
    assertThat(response.hasNextPage).isFalse();
    verify(ownerComponentDAO).findDistinctOwnersByHashPaged("hash1", Set.of(), 0, 25, null, null);
  }

  @Test
  public void listApplications_hasNextPageUsesLongArithmetic() {
    when(readableContextAuthzCache.resolveReadableContexts(principal)).thenReturn(Optional.empty());
    when(ownerComponentDAO.findDistinctOwnersByHashPaged(
        eq("hash1"), isNull(), eq(ComponentUsageService.MAX_PAGE * 100), eq(100), isNull(), isNull()))
            .thenReturn(new PagedOwnersByHash(2_147_483_701L, List.of(
                new ComponentOwnerUsageRow("app-1", new Date(1L)))));
    when(applicationService.getAppsByIds(isNull(), any(), isNull()))
        .thenReturn(List.of(application("app-1", "org-1")));
    when(ownerComponentDAO.getStageTypeIdsByOwnerIdForHash(eq("hash1"), any())).thenReturn(Map.of());
    when(organizationDAO.getByIds(any())).thenReturn(List.of(organization("org-1", "Org One")));

    ComponentUsageRequestDTO request = new ComponentUsageRequestDTO();
    request.componentHash = "hash1";
    request.page = ComponentUsageService.MAX_PAGE;
    request.pageSize = 100;

    ComponentUsageApplicationsResponseDTO response = service.listApplications(request);

    // Without (long) cast, offset + size overflows int and hasNextPage would be wrong.
    assertThat(response.hasNextPage).isTrue();
  }

  @Test
  public void listOrganizations_pagesReadableOrgs() {
    when(readableContextAuthzCache.resolveReadableContexts(principal)).thenReturn(Optional.of(Map.of(
        "app-1", OwnerType.APPLICATION)));

    when(ownerComponentDAO.findDistinctOrganizationsByHashPaged(eq("hash1"), eq(Set.of("app-1")), eq(0), eq(25),
        isNull()))
            .thenReturn(new PagedOrganizationsByHash(1L, List.of(
                new ComponentOrganizationUsageRow("org-1", 3L, new Date(1_700_000_000_000L)))));
    when(organizationDAO.getByIds(any())).thenReturn(List.of(organization("org-1", "Org One")));

    ComponentUsageRequestDTO request = new ComponentUsageRequestDTO();
    request.componentHash = "hash1";

    ComponentUsageOrganizationsResponseDTO response = service.listOrganizations(request);

    assertThat(response.total).isEqualTo(1);
    assertThat(response.hasNextPage).isFalse();
    assertThat(response.organizations).hasSize(1);
    assertThat(response.organizations.get(0).organizationId).isEqualTo("org-1");
    assertThat(response.organizations.get(0).organizationName).isEqualTo("Org One");
    assertThat(response.organizations.get(0).applicationCount).isEqualTo(3L);
    verify(applicationService, never()).getApplications();
  }

  @Test
  public void listOrganizations_failClosedWhenNoReadableContexts() {
    when(readableContextAuthzCache.resolveReadableContexts(principal)).thenReturn(Optional.of(Map.of()));
    when(ownerComponentDAO.findDistinctOrganizationsByHashPaged(eq("hash1"), eq(Set.of()), eq(0), eq(25), isNull()))
        .thenReturn(new PagedOrganizationsByHash(0L, List.of()));

    ComponentUsageRequestDTO request = new ComponentUsageRequestDTO();
    request.componentHash = "hash1";

    ComponentUsageOrganizationsResponseDTO response = service.listOrganizations(request);

    assertThat(response.total).isZero();
    assertThat(response.organizations).isEmpty();
    verify(ownerComponentDAO).findDistinctOrganizationsByHashPaged("hash1", Set.of(), 0, 25, null);
  }

  @Test
  public void listApplications_rejectsOversizedPage() {
    ComponentUsageRequestDTO request = new ComponentUsageRequestDTO();
    request.componentHash = "hash1";
    request.pageSize = 101;

    assertThatThrownBy(() -> service.listApplications(request))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("page size");
  }

  @Test
  public void listOrganizations_rejectsOversizedPage() {
    ComponentUsageRequestDTO request = new ComponentUsageRequestDTO();
    request.componentHash = "hash1";
    request.pageSize = 101;

    assertThatThrownBy(() -> service.listOrganizations(request))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("page size");
  }

  @Test
  public void listApplications_rejectsPageThatWouldOverflowOffset() {
    ComponentUsageRequestDTO request = new ComponentUsageRequestDTO();
    request.componentHash = "hash1";
    request.page = ComponentUsageService.MAX_PAGE + 1;
    request.pageSize = 100;

    assertThatThrownBy(() -> service.listApplications(request))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("Page must be <=");
  }

  @Test
  public void listApplications_passesNameSearchAndOrganizationId() {
    when(readableContextAuthzCache.resolveReadableContexts(principal)).thenReturn(Optional.empty());
    when(ownerComponentDAO.findDistinctOwnersByHashPaged(
        eq("hash1"), isNull(), eq(0), eq(25), eq("webgoat"), eq("org-1")))
            .thenReturn(new PagedOwnersByHash(1L, List.of(
                new ComponentOwnerUsageRow("app-1", new Date(1L)))));
    when(applicationService.getAppsByIds(isNull(), any(), isNull()))
        .thenReturn(List.of(application("app-1", "org-1")));
    when(ownerComponentDAO.getStageTypeIdsByOwnerIdForHash(eq("hash1"), any())).thenReturn(Map.of());
    when(organizationDAO.getByIds(any())).thenReturn(List.of(organization("org-1", "Org One")));

    ComponentUsageRequestDTO request = new ComponentUsageRequestDTO();
    request.componentHash = "hash1";
    request.nameSearch = " webgoat ";
    request.organizationId = " org-1 ";

    ComponentUsageApplicationsResponseDTO response = service.listApplications(request);

    assertThat(response.applications).hasSize(1);
    verify(ownerComponentDAO).findDistinctOwnersByHashPaged("hash1", null, 0, 25, "webgoat", "org-1");
  }

  @Test
  public void listApplications_mergesIncludeIdsAheadOfPage() {
    when(readableContextAuthzCache.resolveReadableContexts(principal)).thenReturn(Optional.empty());
    when(ownerComponentDAO.findDistinctOwnersByHashPaged(
        eq("hash1"), isNull(), eq(0), eq(25), isNull(), isNull()))
            .thenReturn(new PagedOwnersByHash(2L, List.of(
                new ComponentOwnerUsageRow("app-page", new Date(2L)))));
    when(ownerComponentDAO.findDistinctOwnersByHashAndIds(
        eq("hash1"), isNull(), eq(Set.of("app-selected")), isNull(), isNull()))
            .thenReturn(List.of(new ComponentOwnerUsageRow("app-selected", new Date(1L))));
    Application pageApp = application("app-page", "org-1");
    pageApp.setPublicId("public-page");
    pageApp.setName("Page App");
    Application selected = application("app-selected", "org-1");
    selected.setPublicId("public-selected");
    selected.setName("Selected App");
    when(applicationService.getAppsByIds(isNull(), any(), isNull())).thenReturn(List.of(pageApp, selected));
    when(ownerComponentDAO.getStageTypeIdsByOwnerIdForHash(eq("hash1"), any())).thenReturn(Map.of());
    when(organizationDAO.getByIds(any())).thenReturn(List.of(organization("org-1", "Org One")));

    ComponentUsageRequestDTO request = new ComponentUsageRequestDTO();
    request.componentHash = "hash1";
    request.includeIds = List.of("app-selected");

    ComponentUsageApplicationsResponseDTO response = service.listApplications(request);

    assertThat(response.applications).extracting(row -> row.applicationId)
        .containsExactly("app-selected", "app-page");
    assertThat(response.total).isEqualTo(2L);
  }

  @Test
  public void listApplications_includeIdsOutsideNameSearchCountInTotal() {
    when(readableContextAuthzCache.resolveReadableContexts(principal)).thenReturn(Optional.empty());
    when(ownerComponentDAO.findDistinctOwnersByHashPaged(
        eq("hash1"), isNull(), eq(0), eq(25), eq("zzz"), isNull()))
            .thenReturn(new PagedOwnersByHash(0L, List.of()));
    when(ownerComponentDAO.findDistinctOwnersByHashAndIds(
        eq("hash1"), isNull(), eq(Set.of("app-selected")), isNull(), isNull()))
            .thenReturn(List.of(new ComponentOwnerUsageRow("app-selected", new Date(1L))));
    when(ownerComponentDAO.findDistinctOwnersByHashAndIds(
        eq("hash1"), isNull(), eq(Set.of("app-selected")), isNull(), eq("zzz")))
            .thenReturn(List.of());
    Application selected = application("app-selected", "org-1");
    selected.setPublicId("public-selected");
    selected.setName("Selected App");
    when(applicationService.getAppsByIds(isNull(), any(), isNull())).thenReturn(List.of(selected));
    when(ownerComponentDAO.getStageTypeIdsByOwnerIdForHash(eq("hash1"), any())).thenReturn(Map.of());
    when(organizationDAO.getByIds(any())).thenReturn(List.of(organization("org-1", "Org One")));

    ComponentUsageRequestDTO request = new ComponentUsageRequestDTO();
    request.componentHash = "hash1";
    request.includeIds = List.of("app-selected");
    request.nameSearch = "zzz";

    ComponentUsageApplicationsResponseDTO response = service.listApplications(request);

    assertThat(response.applications).extracting(row -> row.applicationId).containsExactly("app-selected");
    assertThat(response.total).isEqualTo(1L);
    assertThat(response.hasNextPage).isFalse();
  }

  private static Application application(final String id, final String orgId) {
    Application app = new Application();
    app.setId(id);
    app.setPublicId("public-1");
    app.setName("App One");
    app.setOrganizationId(orgId);
    return app;
  }

  private static Organization organization(final String id, final String name) {
    Organization org = new Organization();
    org.setId(id);
    org.setName(name);
    return org;
  }

  private static ComponentUsageReportsRequestDTO reportsRequest(final String hash, final String applicationId) {
    ComponentUsageReportsRequestDTO request = new ComponentUsageReportsRequestDTO();
    request.componentHash = hash;
    request.applicationId = applicationId;
    return request;
  }
}
