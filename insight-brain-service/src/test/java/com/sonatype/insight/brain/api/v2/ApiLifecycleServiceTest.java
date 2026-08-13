/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import com.sonatype.clm.dto.model.repository.RepositoryType;
import com.sonatype.insight.brain.api.v2.dto.ApiLifecycleRepositoryManagerDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiLifecycleRepositoryManagerListDTO;
import com.sonatype.insight.brain.dataaccess.repository.HostedRepositoryComponentDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryManagerDAO;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.UserPrincipal;
import com.sonatype.insight.brain.security.PermissionService;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ApiLifecycleServiceTest
{
  private static final String RM_ID = "rm-internal-id";

  private static final String RM_INSTANCE_ID = "rm-instance-id";

  private static final String REPO_ID = "repo-internal-id";

  @Mock
  private RepositoryManagerDAO repositoryManagerDAO;

  @Mock
  private RepositoryDAO repositoryDAO;

  @Mock
  private HostedRepositoryComponentDAO hostedRepositoryComponentDAO;

  @Mock
  private PermissionService permissionService;

  @Mock
  private Subject subject;

  @Mock
  private UserPrincipal userPrincipal;

  @InjectMocks
  private ApiLifecycleService service;

  @BeforeEach
  public void setUp() {
    // Bind a mock Shiro Subject so SecurityUtils.getSubject() returns it from requireAccess().
    ThreadContext.bind(subject);
    lenient().when(subject.isAuthenticated()).thenReturn(true);
    lenient().when(subject.getPrincipal()).thenReturn(userPrincipal);
    // Default: user holds READ on at least one owner — covers Policy Admin / Owner / Developer.
    lenient().when(permissionService.getContextIdsForUserWithPermission(any(UserPrincipal.class), eq(Permission.READ)))
        .thenReturn(java.util.Set.of("some-org-id"));
    // Default: user does NOT hold CONFIGURE_SYSTEM (the READ branch is the default path).
    lenient().when(permissionService.validatePermission(
        any(Subject.class), eq(OwnerType.GLOBAL), eq(MembershipMapping.GLOBAL_CONTEXT_ID),
        eq(EnumSet.of(Permission.CONFIGURE_SYSTEM))))
        .thenReturn(EnumSet.noneOf(Permission.class));
  }

  @AfterEach
  public void tearDown() {
    ThreadContext.unbindSubject();
  }

  @Test
  public void lastActivityTime_isNull_whenNoHostedRepos() {
    RepositoryManager rm = repositoryManager(RM_ID, RM_INSTANCE_ID);
    when(repositoryManagerDAO.getAll()).thenReturn(List.of(rm));
    when(repositoryDAO.getByRepositoryType(RepositoryType.hosted)).thenReturn(Collections.emptyList());
    when(hostedRepositoryComponentDAO.getLastScanTimesByRepositoryIds(anyList())).thenReturn(Collections.emptyMap());

    ApiLifecycleRepositoryManagerListDTO result = service.getRepositoryManagers();

    assertThat(result.repositoryManagers).hasSize(1);
    assertThat(result.repositoryManagers.get(0).lastActivityTime).isNull();
  }

  @Test
  public void lastActivityTime_isNull_whenRepoHasNoActivity() {
    RepositoryManager rm = repositoryManager(RM_ID, RM_INSTANCE_ID);
    Repository repo = hostedRepo(REPO_ID, RM_ID, null);
    when(repositoryManagerDAO.getAll()).thenReturn(List.of(rm));
    when(repositoryDAO.getByRepositoryType(RepositoryType.hosted)).thenReturn(List.of(repo));
    when(hostedRepositoryComponentDAO.getLastScanTimesByRepositoryIds(anyList())).thenReturn(Collections.emptyMap());

    ApiLifecycleRepositoryManagerListDTO result = service.getRepositoryManagers();

    assertThat(result.repositoryManagers).hasSize(1);
    assertThat(result.repositoryManagers.get(0).lastActivityTime).isNull();
  }

  @Test
  public void lastActivityTime_reflectsScanTime() {
    long scanEpoch = 1_700_000_000_000L;
    RepositoryManager rm = repositoryManager(RM_ID, RM_INSTANCE_ID);
    Repository repo = hostedRepo(REPO_ID, RM_ID, null);
    when(repositoryManagerDAO.getAll()).thenReturn(List.of(rm));
    when(repositoryDAO.getByRepositoryType(RepositoryType.hosted)).thenReturn(List.of(repo));
    when(hostedRepositoryComponentDAO.getLastScanTimesByRepositoryIds(anyList()))
        .thenReturn(Map.of(REPO_ID, new Date(scanEpoch)));

    ApiLifecycleRepositoryManagerListDTO result = service.getRepositoryManagers();

    ApiLifecycleRepositoryManagerDTO dto = result.repositoryManagers.get(0);
    assertThat(dto.lastActivityTime).isEqualTo(scanEpoch);
  }

  @Test
  public void lastActivityTime_reflectsManualConfigureTime_whenMoreRecentThanScan() {
    long scanEpoch = 1_700_000_000_000L;
    long configEpoch = 1_700_000_100_000L; // 100 seconds later
    RepositoryManager rm = repositoryManager(RM_ID, RM_INSTANCE_ID);
    Repository repo = hostedRepo(REPO_ID, RM_ID, new Date(configEpoch));
    when(repositoryManagerDAO.getAll()).thenReturn(List.of(rm));
    when(repositoryDAO.getByRepositoryType(RepositoryType.hosted)).thenReturn(List.of(repo));
    when(hostedRepositoryComponentDAO.getLastScanTimesByRepositoryIds(anyList()))
        .thenReturn(Map.of(REPO_ID, new Date(scanEpoch)));

    ApiLifecycleRepositoryManagerListDTO result = service.getRepositoryManagers();

    ApiLifecycleRepositoryManagerDTO dto = result.repositoryManagers.get(0);
    assertThat(dto.lastActivityTime).isEqualTo(configEpoch);
  }

  @Test
  public void lastActivityTime_usesScanTime_whenMoreRecentThanManualConfigureTime() {
    long configEpoch = 1_700_000_000_000L;
    long scanEpoch = 1_700_000_100_000L; // 100 seconds later
    RepositoryManager rm = repositoryManager(RM_ID, RM_INSTANCE_ID);
    Repository repo = hostedRepo(REPO_ID, RM_ID, new Date(configEpoch));
    when(repositoryManagerDAO.getAll()).thenReturn(List.of(rm));
    when(repositoryDAO.getByRepositoryType(RepositoryType.hosted)).thenReturn(List.of(repo));
    when(hostedRepositoryComponentDAO.getLastScanTimesByRepositoryIds(anyList()))
        .thenReturn(Map.of(REPO_ID, new Date(scanEpoch)));

    ApiLifecycleRepositoryManagerListDTO result = service.getRepositoryManagers();

    ApiLifecycleRepositoryManagerDTO dto = result.repositoryManagers.get(0);
    assertThat(dto.lastActivityTime).isEqualTo(scanEpoch);
  }

  @Test
  public void lastActivityTime_isMaxAcrossAllReposForRM() {
    long earlyEpoch = 1_700_000_000_000L;
    long laterEpoch = 1_700_000_200_000L;
    String repoId2 = "repo-2";
    RepositoryManager rm = repositoryManager(RM_ID, RM_INSTANCE_ID);
    Repository repo1 = hostedRepo(REPO_ID, RM_ID, null);
    Repository repo2 = hostedRepo(repoId2, RM_ID, null);
    when(repositoryManagerDAO.getAll()).thenReturn(List.of(rm));
    when(repositoryDAO.getByRepositoryType(RepositoryType.hosted)).thenReturn(List.of(repo1, repo2));
    when(hostedRepositoryComponentDAO.getLastScanTimesByRepositoryIds(anyList()))
        .thenReturn(Map.of(REPO_ID, new Date(earlyEpoch), repoId2, new Date(laterEpoch)));

    ApiLifecycleRepositoryManagerListDTO result = service.getRepositoryManagers();

    ApiLifecycleRepositoryManagerDTO dto = result.repositoryManagers.get(0);
    assertThat(dto.lastActivityTime).isEqualTo(laterEpoch);
  }

  @Test
  public void lastActivityTime_isIsolatedPerRM() {
    long rmAEpoch = 1_700_000_100_000L;
    long rmBEpoch = 1_700_000_900_000L;
    String rmBId = "rm-b-internal-id";
    String rmBInstanceId = "rm-b-instance-id";
    String rmBRepoId = "repo-rm-b";
    RepositoryManager rmA = repositoryManager(RM_ID, RM_INSTANCE_ID);
    RepositoryManager rmB = repositoryManager(rmBId, rmBInstanceId);
    Repository repoA = hostedRepo(REPO_ID, RM_ID, null);
    Repository repoB = hostedRepo(rmBRepoId, rmBId, null);
    when(repositoryManagerDAO.getAll()).thenReturn(List.of(rmA, rmB));
    when(repositoryDAO.getByRepositoryType(RepositoryType.hosted)).thenReturn(List.of(repoA, repoB));
    when(hostedRepositoryComponentDAO.getLastScanTimesByRepositoryIds(anyList()))
        .thenReturn(Map.of(REPO_ID, new Date(rmAEpoch), rmBRepoId, new Date(rmBEpoch)));

    ApiLifecycleRepositoryManagerListDTO result = service.getRepositoryManagers();

    ApiLifecycleRepositoryManagerDTO dtoA = result.repositoryManagers.stream()
        .filter(d -> RM_INSTANCE_ID.equals(d.instanceId))
        .findFirst()
        .orElseThrow();
    ApiLifecycleRepositoryManagerDTO dtoB = result.repositoryManagers.stream()
        .filter(d -> rmBInstanceId.equals(d.instanceId))
        .findFirst()
        .orElseThrow();
    assertThat(dtoA.lastActivityTime).isEqualTo(rmAEpoch);
    assertThat(dtoB.lastActivityTime).isEqualTo(rmBEpoch);
  }

  private static RepositoryManager repositoryManager(final String id, final String instanceId) {
    RepositoryManager rm = new RepositoryManager();
    rm.setId(id);
    rm.setInstanceId(instanceId);
    rm.setConfigured(true);
    return rm;
  }

  @Test
  public void getRepositoryManagers_anonymousUser_throwsUnauthenticated() {
    when(subject.isAuthenticated()).thenReturn(false);

    assertThatThrownBy(() -> service.getRepositoryManagers())
        .isInstanceOf(UnauthenticatedException.class);
  }

  @Test
  public void getRepositoryManagers_userWithoutConfigureSystemOrRead_throwsUnauthorized() {
    when(permissionService.getContextIdsForUserWithPermission(any(UserPrincipal.class), eq(Permission.READ)))
        .thenReturn(java.util.Set.of());
    when(permissionService.validatePermission(
        any(Subject.class), eq(OwnerType.GLOBAL), eq(MembershipMapping.GLOBAL_CONTEXT_ID),
        eq(EnumSet.of(Permission.CONFIGURE_SYSTEM))))
            .thenReturn(EnumSet.noneOf(Permission.class));

    assertThatThrownBy(() -> service.getRepositoryManagers())
        .isInstanceOf(UnauthorizedException.class);
  }

  @Test
  public void getRepositoryManagers_userWithOnlyConfigureSystem_succeeds() {
    // CONFIGURE_SYSTEM is checked first; short-circuits before the READ branch runs.
    when(permissionService.validatePermission(
        any(Subject.class), eq(OwnerType.GLOBAL), eq(MembershipMapping.GLOBAL_CONTEXT_ID),
        eq(EnumSet.of(Permission.CONFIGURE_SYSTEM))))
            .thenReturn(EnumSet.of(Permission.CONFIGURE_SYSTEM));
    when(repositoryManagerDAO.getAll()).thenReturn(Collections.emptyList());
    when(repositoryDAO.getByRepositoryType(RepositoryType.hosted)).thenReturn(Collections.emptyList());
    when(hostedRepositoryComponentDAO.getLastScanTimesByRepositoryIds(anyList())).thenReturn(Collections.emptyMap());

    ApiLifecycleRepositoryManagerListDTO result = service.getRepositoryManagers();

    assertThat(result).isNotNull();
  }

  @Test
  public void getRepositoryManagers_userWithReadOnAnyOwner_succeeds() {
    // Explicit setup: user has READ on at least one owner, no CONFIGURE_SYSTEM.
    when(permissionService.getContextIdsForUserWithPermission(any(UserPrincipal.class), eq(Permission.READ)))
        .thenReturn(java.util.Set.of("some-org-id"));
    when(repositoryManagerDAO.getAll()).thenReturn(Collections.emptyList());
    when(repositoryDAO.getByRepositoryType(RepositoryType.hosted)).thenReturn(Collections.emptyList());
    when(hostedRepositoryComponentDAO.getLastScanTimesByRepositoryIds(anyList())).thenReturn(Collections.emptyMap());

    ApiLifecycleRepositoryManagerListDTO result = service.getRepositoryManagers();

    assertThat(result).isNotNull();
  }

  @Test
  public void lastActivityTime_excludesNonMonitoredRepos() {
    long monitoredEpoch = 1_700_000_000_000L;
    long nonMonitoredEpoch = 1_700_000_900_000L; // later, but should be ignored
    String nonMonitoredRepoId = "repo-non-monitored";
    RepositoryManager rm = repositoryManager(RM_ID, RM_INSTANCE_ID);
    Repository monitored = hostedRepo(REPO_ID, RM_ID, null);
    Repository nonMonitored = nonMonitoredHostedRepo(nonMonitoredRepoId, RM_ID);
    when(repositoryManagerDAO.getAll()).thenReturn(List.of(rm));
    when(repositoryDAO.getByRepositoryType(RepositoryType.hosted)).thenReturn(List.of(monitored, nonMonitored));
    when(hostedRepositoryComponentDAO.getLastScanTimesByRepositoryIds(anyList()))
        .thenReturn(Map.of(REPO_ID, new Date(monitoredEpoch), nonMonitoredRepoId, new Date(nonMonitoredEpoch)));

    ApiLifecycleRepositoryManagerListDTO result = service.getRepositoryManagers();

    assertThat(result.repositoryManagers.get(0).lastActivityTime).isEqualTo(monitoredEpoch);
  }

  private static Repository hostedRepo(
      final String id,
      final String repositoryManagerId,
      final Date lastManualConfigureTime)
  {
    Repository repo = new Repository();
    repo.setId(id);
    repo.setRepositoryManagerId(repositoryManagerId);
    repo.setMonitoringEnabled(true);
    repo.setLastManualConfigureTime(lastManualConfigureTime);
    return repo;
  }

  private static Repository nonMonitoredHostedRepo(final String id, final String repositoryManagerId) {
    Repository repo = new Repository();
    repo.setId(id);
    repo.setRepositoryManagerId(repositoryManagerId);
    repo.setMonitoringEnabled(false);
    return repo;
  }
}
