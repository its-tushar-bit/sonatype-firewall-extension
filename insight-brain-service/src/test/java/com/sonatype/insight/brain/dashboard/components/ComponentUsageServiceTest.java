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
import com.sonatype.insight.brain.dataaccess.OwnerComponentDAO.PagedOrganizationsByHash;
import com.sonatype.insight.brain.dataaccess.OwnerComponentDAO.PagedOwnersByHash;
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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
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

  @Before
  public void setUp() {
    service = new ComponentUsageService(
        ownerComponentDAO,
        applicationService,
        organizationDAO,
        productLicense,
        readableContextAuthzCache,
        currentUser);
    doNothing().when(productLicense).validateFeature(LicensedFeature.DASHBOARD);
    when(currentUser.getUserPrincipal()).thenReturn(principal);
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
    when(ownerComponentDAO.findDistinctOwnersByHashPaged(eq("hash1"), isNull(), eq(0), eq(25)))
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
    verify(ownerComponentDAO).findDistinctOwnersByHashPaged("hash1", null, 0, 25);
  }

  @Test
  public void listApplications_restricted_pagesReadableOwners() {
    when(readableContextAuthzCache.resolveReadableContexts(principal)).thenReturn(Optional.of(Map.of(
        "app-1", OwnerType.APPLICATION,
        "org-1", OwnerType.ORGANIZATION)));

    Application app = application("app-1", "org-1");
    when(ownerComponentDAO.findDistinctOwnersByHashPaged(eq("hash1"), eq(Set.of("app-1")), eq(0), eq(25)))
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
    when(ownerComponentDAO.findDistinctOwnersByHashPaged(eq("hash1"), eq(Set.of()), eq(0), eq(25)))
        .thenReturn(new PagedOwnersByHash(0L, List.of()));

    ComponentUsageRequestDTO request = new ComponentUsageRequestDTO();
    request.componentHash = "hash1";

    ComponentUsageApplicationsResponseDTO response = service.listApplications(request);

    assertThat(response.total).isZero();
    assertThat(response.applications).isEmpty();
    assertThat(response.hasNextPage).isFalse();
    verify(ownerComponentDAO).findDistinctOwnersByHashPaged("hash1", Set.of(), 0, 25);
  }

  @Test
  public void listApplications_hasNextPageUsesLongArithmetic() {
    when(readableContextAuthzCache.resolveReadableContexts(principal)).thenReturn(Optional.empty());
    when(ownerComponentDAO.findDistinctOwnersByHashPaged(
        eq("hash1"), isNull(), eq(ComponentUsageService.MAX_PAGE * 100), eq(100)))
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

    when(ownerComponentDAO.findDistinctOrganizationsByHashPaged(eq("hash1"), eq(Set.of("app-1")), eq(0), eq(25)))
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
    when(ownerComponentDAO.findDistinctOrganizationsByHashPaged(eq("hash1"), eq(Set.of()), eq(0), eq(25)))
        .thenReturn(new PagedOrganizationsByHash(0L, List.of()));

    ComponentUsageRequestDTO request = new ComponentUsageRequestDTO();
    request.componentHash = "hash1";

    ComponentUsageOrganizationsResponseDTO response = service.listOrganizations(request);

    assertThat(response.total).isZero();
    assertThat(response.organizations).isEmpty();
    verify(ownerComponentDAO).findDistinctOrganizationsByHashPaged("hash1", Set.of(), 0, 25);
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
}
