/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.metrics.sql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.stream.Collectors;

import com.sonatype.insight.brain.dashboard.metrics.DashboardMetricsRequestDTO;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryManagerDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.model.security.UserPrincipal;
import com.sonatype.insight.brain.organization.OrganizationService;
import com.sonatype.insight.brain.repository.RepositoryService;
import com.sonatype.insight.brain.security.AuthzFilter.Context;
import com.sonatype.insight.brain.security.AuthorizationChecker;
import com.sonatype.insight.brain.security.CurrentUser;

import org.junit.Test;

public class DashboardMetricsScopeResolverTest
{
  @Test
  public void globalPrincipalReturnsGlobalScope() {
    Fixture fixture = new Fixture();
    fixture.globallyAuthorized = true;
    fixture.owners = List.of(fixture.organization("org"), fixture.application("app"));

    ResolvedScope scope = fixture.resolve(new DashboardMetricsRequestDTO());

    assertThat(scope.kind()).isEqualTo(ResolvedScope.Kind.GLOBAL);
    assertThat(scope.ownerIds()).containsExactlyInAnyOrder("org", "app");
  }

  @Test
  public void globalPrincipalWithFiltersReturnsRestrictedScope() {
    Fixture fixture = new Fixture();
    fixture.globallyAuthorized = true;
    fixture.owners = List.of(fixture.organization("org"), fixture.application("app"));
    DashboardMetricsRequestDTO request = new DashboardMetricsRequestDTO();
    request.applicationIds = Set.of("app");

    ResolvedScope scope = fixture.resolve(request);

    assertThat(scope.kind()).isEqualTo(ResolvedScope.Kind.RESTRICTED);
    assertThat(scope.requestFiltersApplied()).isTrue();
  }

  @Test
  public void restrictedSubtreeReturnsAuthorizedOwnerOrgAndAppIds() {
    Fixture fixture = new Fixture();
    fixture.owners = List.of(fixture.organization("child-org"), fixture.application("child-app"));
    DashboardMetricsRequestDTO request = new DashboardMetricsRequestDTO();
    request.organizationIds = Set.of("parent-org");
    fixture.expandedOrganizations = Set.of("parent-org", "child-org");

    ResolvedScope scope = fixture.resolve(request);

    assertThat(scope.kind()).isEqualTo(ResolvedScope.Kind.RESTRICTED);
    assertThat(scope.ownerIds()).containsExactlyInAnyOrder("child-org", "child-app");
    assertThat(scope.organizationIds()).containsExactly("child-org");
    assertThat(scope.applicationIds()).containsExactly("child-app");
  }

  @Test
  public void organizationIdsExcludeUnreadableParentClosureButRetainDirectlyReadableRoot() {
    Fixture fixture = new Fixture();
    Owner childOrganization = fixture.organization("child-org");
    Owner childApplication = fixture.application("child-app");
    Organization parentOrganization = fixture.parentOrganization("parent-org");
    Organization rootOrganization = fixture.parentOrganization(Organization.ROOT_ORGANIZATION_ID);
    fixture.owners = List.of(childOrganization, childApplication, rootOrganization);
    fixture.parentOrganizations = Map.of(parentOrganization.getId(), parentOrganization);

    ResolvedScope scope = fixture.resolve(new DashboardMetricsRequestDTO());

    assertThat(scope.ownerIds()).containsExactlyInAnyOrder(
        "child-org", "child-app", "parent-org", Organization.ROOT_ORGANIZATION_ID);
    assertThat(scope.policyOwnerIds()).containsExactlyInAnyOrder(
        "child-org", "child-app", Organization.ROOT_ORGANIZATION_ID);
    assertThat(scope.organizationIds()).containsExactlyInAnyOrder(
        "child-org", Organization.ROOT_ORGANIZATION_ID);
  }

  @Test
  public void noReadableOwnersReturnsDenyAllNoAccess() {
    Fixture fixture = new Fixture();

    ResolvedScope scope = fixture.resolve(new DashboardMetricsRequestDTO());

    assertThat(scope.kind()).isEqualTo(ResolvedScope.Kind.DENY_ALL);
    assertThat(scope.denyReason()).isEqualTo(ResolvedScope.DenyReason.NO_ACCESS);
  }

  @Test
  public void resolverExceptionReturnsDenyAllResolutionFailed() {
    Fixture fixture = new Fixture();
    when(fixture.ownerDAO.getAllAppsAndOrgs()).thenThrow(new IllegalStateException("database unavailable"));

    ResolvedScope scope = fixture.resolve(new DashboardMetricsRequestDTO());

    assertThat(scope.kind()).isEqualTo(ResolvedScope.Kind.DENY_ALL);
    assertThat(scope.denyReason()).isEqualTo(ResolvedScope.DenyReason.RESOLUTION_FAILED);
    verify(fixture.telemetry).recordScopeResolutionFailure();
    verify(fixture.telemetry).recordScopeResolution(anyLong(), eq(ResolvedScope.Kind.DENY_ALL));
  }

  @Test
  public void scopeResolutionTimingRecordsResolvedPrincipalShape() {
    Fixture fixture = new Fixture();
    fixture.globallyAuthorized = true;
    fixture.owners = List.of(fixture.application("app"));

    assertThat(fixture.resolve(new DashboardMetricsRequestDTO()).kind()).isEqualTo(ResolvedScope.Kind.GLOBAL);

    verify(fixture.telemetry).recordScopeResolution(anyLong(), eq(ResolvedScope.Kind.GLOBAL));
  }

  @Test
  public void telemetryFailureDoesNotReplaceResolvedScope() {
    Fixture fixture = new Fixture();
    fixture.globallyAuthorized = true;
    fixture.owners = List.of(fixture.application("app"));
    doThrow(new IllegalStateException("telemetry unavailable"))
        .when(fixture.telemetry)
        .recordScopeResolution(anyLong(), any());

    ResolvedScope scope = fixture.resolve(new DashboardMetricsRequestDTO());

    assertThat(scope.kind()).isEqualTo(ResolvedScope.Kind.GLOBAL);
  }

  @Test
  public void telemetryFailureDoesNotEscapeResolutionFailurePath() {
    Fixture fixture = new Fixture();
    when(fixture.ownerDAO.getAllAppsAndOrgs()).thenThrow(new IllegalStateException("database unavailable"));
    doThrow(new IllegalStateException("telemetry unavailable"))
        .when(fixture.telemetry)
        .recordScopeResolutionFailure();
    doThrow(new IllegalStateException("telemetry unavailable"))
        .when(fixture.telemetry)
        .recordScopeResolution(anyLong(), any());

    ResolvedScope scope = fixture.resolve(new DashboardMetricsRequestDTO());

    assertThat(scope.kind()).isEqualTo(ResolvedScope.Kind.DENY_ALL);
    assertThat(scope.denyReason()).isEqualTo(ResolvedScope.DenyReason.RESOLUTION_FAILED);
  }

  @Test
  public void tagFilteredPrincipalIncludesAuthorizedApplications() {
    Fixture fixture = new Fixture();
    Owner tagCandidate = fixture.application("tag-candidate");
    fixture.owners = List.of(tagCandidate);
    fixture.authorizedOwners = List.of(tagCandidate);
    DashboardMetricsRequestDTO request = new DashboardMetricsRequestDTO();
    request.tagIds = Set.of("tag");

    ResolvedScope scope = fixture.resolve(request);

    assertThat(scope.applicationIds()).containsExactly("tag-candidate");
    verify(fixture.ownerDAO).getOwnersByAppTagsAndOrgs(eq(null), eq(Set.of("tag")), eq(null));
    verify(fixture.authorizationChecker, atLeastOnce()).filterByPermission(
        eq(fixture.principal), eq(Permission.READ), eq(List.of(tagCandidate)));
  }

  @Test
  public void organizationFilterExpandsDescendants() {
    Fixture fixture = new Fixture();
    fixture.expandedOrganizations = Set.of("parent-org", "child-org");
    fixture.owners = List.of(fixture.organization("child-org"), fixture.application("child-app"));
    DashboardMetricsRequestDTO request = new DashboardMetricsRequestDTO();
    request.organizationIds = Set.of("parent-org");

    ResolvedScope scope = fixture.resolve(request);

    assertThat(scope.organizationIds()).containsExactly("child-org");
    assertThat(scope.applicationIds()).containsExactly("child-app");
  }

  @Test
  public void organizationFilterIncludesAuthorizedApplicationsInExpandedSubtree() {
    Fixture fixture = new Fixture();
    fixture.expandedOrganizations = Set.of("parent-org", "child-org");
    fixture.owners = List.of(fixture.organization("child-org"));
    fixture.organizationApplications = List.of(fixture.applicationEntity("child-app", "child-org"));
    DashboardMetricsRequestDTO request = new DashboardMetricsRequestDTO();
    request.organizationIds = Set.of("parent-org");

    ResolvedScope scope = fixture.resolve(request);

    assertThat(scope.applicationIds()).containsExactly("child-app");
  }

  @Test
  public void organizationFilterWithOnlyReadableOrgChildAppsIsNotDenied() {
    Fixture fixture = new Fixture();
    fixture.expandedOrganizations = Set.of("parent-org", "child-org");
    fixture.owners = List.of();
    fixture.organizationApplications = List.of(fixture.applicationEntity("child-app", "child-org"));
    DashboardMetricsRequestDTO request = new DashboardMetricsRequestDTO();
    request.organizationIds = Set.of("parent-org");

    ResolvedScope scope = fixture.resolve(request);

    assertThat(scope.kind()).isEqualTo(ResolvedScope.Kind.RESTRICTED);
    assertThat(scope.denyReason()).isNull();
    assertThat(scope.ownerIds()).isEmpty();
    assertThat(scope.organizationIds()).isEmpty();
    assertThat(scope.applicationIds()).containsExactly("child-app");
  }

  @Test
  public void unknownOrganizationReturnsDenyAllNoAccess() {
    Fixture fixture = new Fixture();
    fixture.expandedOrganizations = Set.of();
    DashboardMetricsRequestDTO request = new DashboardMetricsRequestDTO();
    request.organizationIds = Set.of("unknown-org");

    ResolvedScope scope = fixture.resolve(request);

    assertThat(scope.kind()).isEqualTo(ResolvedScope.Kind.DENY_ALL);
    assertThat(scope.denyReason()).isEqualTo(ResolvedScope.DenyReason.NO_ACCESS);
  }

  @Test
  public void organizationAndApplicationFiltersUnionApplicationIds() {
    Fixture fixture = new Fixture();
    fixture.expandedOrganizations = Set.of("selected-org");
    fixture.owners = List.of(
        fixture.organization("selected-org"),
        fixture.application("org-app"),
        fixture.application("explicit-app"));
    DashboardMetricsRequestDTO request = new DashboardMetricsRequestDTO();
    request.organizationIds = Set.of("selected-org");
    request.applicationIds = Set.of("explicit-app");

    ResolvedScope scope = fixture.resolve(request);

    assertThat(scope.organizationIds()).containsExactly("selected-org");
    assertThat(scope.applicationIds()).containsExactlyInAnyOrder("org-app", "explicit-app");
  }

  @Test
  public void applicationFilterDoesNotNarrowOrganizationIds() {
    Fixture fixture = new Fixture();
    fixture.expandedOrganizations = Set.of("selected-org");
    fixture.owners = List.of(
        fixture.organization("selected-org"),
        fixture.application("explicit-app"));
    DashboardMetricsRequestDTO request = new DashboardMetricsRequestDTO();
    request.organizationIds = Set.of("selected-org");
    request.applicationIds = Set.of("explicit-app");

    ResolvedScope scope = fixture.resolve(request);

    assertThat(scope.organizationIds()).containsExactly("selected-org");
  }

  @Test
  public void applicationOnlyFilterDoesNotNarrowOrganizationIds() {
    Fixture fixture = new Fixture();
    fixture.filteredOwners = List.of(fixture.application("explicit-app"));
    fixture.allOwners = List.of(
        fixture.organization("org-a"),
        fixture.organization("org-b"),
        fixture.application("explicit-app"));
    DashboardMetricsRequestDTO request = new DashboardMetricsRequestDTO();
    request.applicationIds = Set.of("explicit-app");

    ResolvedScope scope = fixture.resolve(request);

    assertThat(scope.organizationIds()).containsExactlyInAnyOrder("org-a", "org-b");
    assertThat(scope.applicationIds()).containsExactly("explicit-app");
  }

  @Test
  public void applicationOnlyFilterRetainsReadableRootOrganization() {
    Fixture fixture = new Fixture();
    fixture.filteredOwners = List.of(fixture.application("explicit-app"));
    fixture.allOwners = List.of(
        fixture.organization(Organization.ROOT_ORGANIZATION_ID),
        fixture.organization("org-a"),
        fixture.application("explicit-app"));
    DashboardMetricsRequestDTO request = new DashboardMetricsRequestDTO();
    request.applicationIds = Set.of("explicit-app");

    ResolvedScope scope = fixture.resolve(request);

    assertThat(scope.organizationIds()).containsExactlyInAnyOrder(Organization.ROOT_ORGANIZATION_ID, "org-a");
  }

  @Test
  public void unreadableApplicationFilterRetainsIndependentlyReadableOrganizations() {
    Fixture fixture = new Fixture();
    fixture.filteredOwners = List.of();
    fixture.allOwners = List.of(fixture.organization("readable-org"));
    DashboardMetricsRequestDTO request = new DashboardMetricsRequestDTO();
    request.applicationIds = Set.of("missing-or-unreadable-app");

    ResolvedScope scope = fixture.resolve(request);

    assertThat(scope.kind()).isEqualTo(ResolvedScope.Kind.RESTRICTED);
    assertThat(scope.organizationIds()).containsExactly("readable-org");
    assertThat(scope.applicationIds()).isEmpty();
    assertThat(scope.ownerIds()).isEmpty();
    assertThat(scope.policyOwnerIds()).isEmpty();
    assertThat(scope.requestFiltersApplied()).isTrue();
  }

  @Test
  public void tagFilteredAccessRequiresAuthorizationGrant() {
    Fixture fixture = new Fixture();
    fixture.owners = List.of(fixture.application("tag-candidate"));
    fixture.authorizedOwners = List.of();
    DashboardMetricsRequestDTO request = new DashboardMetricsRequestDTO();
    request.tagIds = Set.of("tag");

    ResolvedScope scope = fixture.resolve(request);

    assertThat(scope.kind()).isEqualTo(ResolvedScope.Kind.DENY_ALL);
    assertThat(scope.denyReason()).isEqualTo(ResolvedScope.DenyReason.NO_ACCESS);
  }

  @Test
  public void ownerIdsMatchExistingWaiverResolver() {
    Fixture fixture = new Fixture();
    fixture.owners = List.of(fixture.organization("org"), fixture.application("app"));

    DashboardMetricsRequestDTO request = new DashboardMetricsRequestDTO();

    assertThat(fixture.resolve(request).ownerIds())
        .isEqualTo(fixture.legacyOwnerIds(request));
  }

  @Test
  public void resolverOwnerIdsMatchLegacyOracleForUnfilteredLifecycleOwners() {
    Fixture fixture = new Fixture();
    fixture.owners = List.of(fixture.organization("org"), fixture.application("app"));

    assertThat(fixture.resolve(new DashboardMetricsRequestDTO()).ownerIds())
        .isEqualTo(fixture.legacyOwnerIds(new DashboardMetricsRequestDTO()));
  }

  @Test
  public void resolverOwnerIdsMatchLegacyOracleForHierarchyParentExpansion() {
    Fixture fixture = new Fixture();
    Owner childApplication = fixture.application("child-app");
    Organization parent = fixture.parentOrganization("parent-org");
    fixture.owners = List.of(childApplication);
    fixture.parentOrganizations = Map.of(parent.getId(), parent);
    DashboardMetricsRequestDTO request = new DashboardMetricsRequestDTO();
    request.organizationIds = Set.of("parent-org");
    fixture.expandedOrganizations = Set.of("parent-org", "child-org");

    assertThat(fixture.resolve(request).ownerIds()).isEqualTo(fixture.legacyOwnerIds(request));
  }

  @Test
  public void resolverOwnerIdsMatchLegacyOracleForRequestTagFiltering() {
    Fixture fixture = new Fixture();
    Owner candidate = fixture.application("tag-candidate");
    Owner readable = fixture.application("readable-tagged-app");
    fixture.owners = List.of(candidate);
    fixture.authorizedOwners = List.of(readable);
    DashboardMetricsRequestDTO request = new DashboardMetricsRequestDTO();
    request.tagIds = Set.of("tag");

    assertThat(fixture.resolve(request).ownerIds()).isEqualTo(fixture.legacyOwnerIds(request));
  }

  @Test
  public void resolverOwnerIdsMatchLegacyOracleForRepositoriesManagersAndContainer() {
    Fixture fixture = new Fixture();
    fixture.repositories = List.of(fixture.repository("repository"));
    fixture.repositoryManagers = List.of(fixture.repositoryManager("repository-manager"));
    fixture.repositoryContainerReadable = true;
    fixture.rootOrganization = fixture.parentOrganization(Organization.ROOT_ORGANIZATION_ID);

    assertThat(fixture.resolve(new DashboardMetricsRequestDTO()).ownerIds())
        .isEqualTo(fixture.legacyOwnerIds(new DashboardMetricsRequestDTO()));
  }

  @Test
  public void resolverOwnerIdsMatchLegacyOracleForAuthorizedStandaloneRepositoryManagers() {
    Fixture fixture = new Fixture();
    RepositoryManager authorizedManager = fixture.repositoryManager("authorized-manager");
    RepositoryManager unauthorizedManager = fixture.repositoryManager("unauthorized-manager");
    fixture.repositoryManagers = List.of(authorizedManager, unauthorizedManager);
    fixture.authorizedStandaloneRepositoryManagers = List.of(authorizedManager);

    ResolvedScope scope = fixture.resolve(new DashboardMetricsRequestDTO());

    assertThat(scope.ownerIds()).contains("authorized-manager").doesNotContain("unauthorized-manager");
    assertThat(scope.ownerIds()).isEqualTo(fixture.legacyOwnerIds(new DashboardMetricsRequestDTO()));
    assertThat(fixture.standaloneManagerCandidates)
        .containsExactly(authorizedManager, unauthorizedManager);
  }

  @Test
  public void resolverOwnerIdsMatchLegacyOracleForAuthorizationFiltering() {
    Fixture fixture = new Fixture();
    Owner unreadable = fixture.application("unreadable-app");
    Owner readable = fixture.application("readable-app");
    fixture.owners = List.of(unreadable, readable);
    fixture.authorizedOwners = List.of(readable);

    assertThat(fixture.resolve(new DashboardMetricsRequestDTO()).ownerIds())
        .isEqualTo(fixture.legacyOwnerIds(new DashboardMetricsRequestDTO()));
  }

  private static class Fixture
  {
    private final ApplicationDAO applicationDAO = mock(ApplicationDAO.class);

    private final OrganizationDAO organizationDAO = mock(OrganizationDAO.class);

    private final OrganizationService organizationService = mock(OrganizationService.class);

    private final OwnerDAO ownerDAO = mock(OwnerDAO.class);

    private final RepositoryManagerDAO repositoryManagerDAO = mock(RepositoryManagerDAO.class);

    private final RepositoryService repositoryService = mock(RepositoryService.class);

    private final AuthorizationChecker authorizationChecker = mock(AuthorizationChecker.class);

    private final CurrentUser currentUser = mock(CurrentUser.class);

    private final DashboardMetricsSqlTelemetry telemetry = mock(DashboardMetricsSqlTelemetry.class);

    private final DashboardMetricsScopeResolver resolver =
        new DashboardMetricsScopeResolver(
            organizationDAO,
            applicationDAO,
            organizationService,
            ownerDAO,
            repositoryManagerDAO,
            repositoryService,
            authorizationChecker,
            currentUser,
            telemetry);

    private List<RepositoryManager> standaloneManagerCandidates;

    private List<Owner> owners = List.of();

    private List<Owner> allOwners;

    private List<Owner> filteredOwners;

    private Collection<? extends Owner> authorizedOwners;

    private Map<String, Organization> parentOrganizations = Map.of();

    private List<Repository> repositories = List.of();

    private List<RepositoryManager> repositoryManagers = List.of();

    private List<RepositoryManager> authorizedStandaloneRepositoryManagers;

    private Organization rootOrganization;

    private boolean repositoryContainerReadable;

    private Set<String> expandedOrganizations;

    private List<Application> organizationApplications = List.of();

    private boolean globallyAuthorized;

    private final UserPrincipal principal = new UserPrincipal("scope-user", "scope-user", User.INTERNAL_REALM_ID);

    private Fixture() {
      when(currentUser.getUserPrincipal()).thenReturn(principal);
      when(organizationService.getAllParentOrgsNoAuthz(any(), eq(null), eq(null)))
          .thenAnswer(invocation -> parentOrganizations);
      when(repositoryService.getRepositoriesWithReadPermissionByIds(null)).thenAnswer(invocation -> repositories);
      when(repositoryService.checkReadPermissionRepositoryContainer())
          .thenAnswer(invocation -> repositoryContainerReadable);
      when(repositoryManagerDAO.getAll()).thenAnswer(invocation -> repositoryManagers);
      when(repositoryManagerDAO.getByRepositoryIds(any())).thenAnswer(invocation -> repositoryManagers);
      when(organizationDAO.getById(Organization.ROOT_ORGANIZATION_ID)).thenAnswer(invocation -> rootOrganization);
      when(authorizationChecker.filterByPermission(eq(principal), eq(Permission.READ), any())).thenAnswer(
          invocation -> authorizedOwners == null ? invocation.getArgument(2) : authorizedOwners);
      when(authorizationChecker.filterByPermission(
          eq(principal), eq(Permission.READ), any(), eq(Context.REPOSITORY_MANAGER))).thenAnswer(invocation -> {
            Iterable<? extends RepositoryManager> entities = invocation.getArgument(2);
            List<RepositoryManager> managers = new ArrayList<>();
            entities.forEach(managers::add);
            standaloneManagerCandidates = managers;
            return authorizedStandaloneRepositoryManagers == null
                ? managers
                : authorizedStandaloneRepositoryManagers;
          });
      when(authorizationChecker.isPermitted(eq(principal), eq(Permission.READ), eq(Map.of())))
          .thenAnswer(invocation -> globallyAuthorized);
      when(ownerDAO.getAllAppsAndOrgs()).thenAnswer(invocation -> allOwners == null ? owners : allOwners);
      when(ownerDAO.getOwnersByAppTagsAndOrgs(any(), any(), any()))
          .thenAnswer(invocation -> filteredOwners == null ? owners : filteredOwners);
      when(applicationDAO.getByOrganizationIds(any())).thenAnswer(invocation -> organizationApplications);
      when(organizationDAO.getAllChildOrganizationIds(any())).thenAnswer(
          invocation -> expandedOrganizations == null ? invocation.getArgument(0) : expandedOrganizations);
    }

    private ResolvedScope resolve(final DashboardMetricsRequestDTO request) {
      return resolver.resolve(request);
    }

    private Set<String> legacyOwnerIds(final DashboardMetricsRequestDTO request) {
      Set<String> requestOrganizationIds = request == null ? null : request.organizationIds;
      Set<String> organizationIds = hasValues(requestOrganizationIds)
          ? organizationDAO.getAllChildOrganizationIds(requestOrganizationIds)
          : requestOrganizationIds;
      if (hasValues(requestOrganizationIds) && !hasValues(organizationIds)) {
        return Set.of();
      }
      Set<String> applicationIds = request == null ? null : request.applicationIds;
      Set<String> tagIds = request == null ? null : request.tagIds;
      BooleanSupplier isOwnerFilterEmpty = () -> !hasValues(organizationIds)
          && !hasValues(applicationIds)
          && !hasValues(tagIds);
      Map<String, Owner> resolvedOwners = new HashMap<>();
      List<? extends Owner> lifecycleOwners = isOwnerFilterEmpty.getAsBoolean()
          ? ownerDAO.getAllAppsAndOrgs()
          : ownerDAO.getOwnersByAppTagsAndOrgs(applicationIds, tagIds, organizationIds);
      Collection<? extends Owner> readableOwners =
          authorizationChecker.filterByPermission(principal, Permission.READ, lifecycleOwners);
      resolvedOwners.putAll(readableOwners.stream()
          .collect(
              Collectors.toMap(Owner::getId, owner -> owner, (existing, replacement) -> existing)));
      resolvedOwners.putAll(organizationService.getAllParentOrgsNoAuthz(readableOwners, null, null));

      List<Repository> readableRepositories = isOwnerFilterEmpty.getAsBoolean()
          ? repositoryService.getRepositoriesWithReadPermissionByIds(null)
          : List.of();
      List<RepositoryManager> readableRepositoryManagers = !readableRepositories.isEmpty()
          ? repositoryManagerDAO.getByRepositoryIds(
              readableRepositories.stream().map(Repository::getId).collect(Collectors.toSet()))
          : isOwnerFilterEmpty.getAsBoolean()
              ? authorizedStandaloneRepositoryManagers == null
                  ? repositoryManagerDAO.getAll()
                  : authorizedStandaloneRepositoryManagers
              : List.of();
      resolvedOwners.putAll(readableRepositories.stream()
          .collect(
              Collectors.toMap(Owner::getId, owner -> owner, (existing, replacement) -> existing)));
      resolvedOwners.putAll(readableRepositoryManagers.stream()
          .collect(
              Collectors.toMap(Owner::getId, owner -> owner, (existing, replacement) -> existing)));
      if (!readableRepositories.isEmpty()
          || (isOwnerFilterEmpty.getAsBoolean() && repositoryService.checkReadPermissionRepositoryContainer()))
      {
        resolvedOwners.put(RepositoryContainer.REPOSITORY_CONTAINER_ID, RepositoryContainer.SINGLETON);
        resolvedOwners.computeIfAbsent(Organization.ROOT_ORGANIZATION_ID, organizationDAO::getById);
      }
      return resolvedOwners.keySet();
    }

    private static boolean hasValues(final Set<String> ids) {
      return ids != null && !ids.isEmpty();
    }

    private Owner organization(final String id) {
      return owner(id, OwnerType.ORGANIZATION);
    }

    private Owner application(final String id) {
      return owner(id, OwnerType.APPLICATION);
    }

    private Application applicationEntity(final String id, final String organizationId) {
      Application application = mock(Application.class);
      when(application.getId()).thenReturn(id);
      when(application.getType()).thenReturn(OwnerType.APPLICATION);
      when(application.getOrganizationId()).thenReturn(organizationId);
      return application;
    }

    private Organization parentOrganization(final String id) {
      Organization organization = mock(Organization.class);
      when(organization.getId()).thenReturn(id);
      when(organization.getType()).thenReturn(OwnerType.ORGANIZATION);
      return organization;
    }

    private Repository repository(final String id) {
      Repository repository = mock(Repository.class);
      when(repository.getId()).thenReturn(id);
      when(repository.getType()).thenReturn(OwnerType.REPOSITORY);
      return repository;
    }

    private RepositoryManager repositoryManager(final String id) {
      RepositoryManager repositoryManager = mock(RepositoryManager.class);
      when(repositoryManager.getId()).thenReturn(id);
      when(repositoryManager.getType()).thenReturn(OwnerType.REPOSITORY_MANAGER);
      return repositoryManager;
    }

    private Owner owner(final String id, final OwnerType type) {
      Owner owner = mock(Owner.class);
      when(owner.getId()).thenReturn(id);
      when(owner.getType()).thenReturn(type);
      return owner;
    }
  }

}
