/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.session;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.UserPrincipal;
import com.sonatype.insight.brain.security.AuthorizationChecker;
import com.sonatype.insight.brain.security.PermissionService;

import com.google.common.base.Ticker;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.MatchNoDocsQuery;
import org.apache.lucene.search.Query;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ReadableContextAuthzCacheTest
{
  @Test
  public void globalShortCircuit_skipsContextEnumeration() {
    UserPrincipal principal = new UserPrincipal("admin", "Admin", "default", Set.of());
    AuthorizationChecker authorizationChecker = mock(AuthorizationChecker.class);
    PermissionService permissionService = mock(PermissionService.class);
    OwnerDAO ownerDAO = mock(OwnerDAO.class);
    when(authorizationChecker.isPermitted(eq(principal), eq(Permission.READ), eq(Collections.emptyMap())))
        .thenReturn(true);

    ReadableContextAuthzCache cache =
        new ReadableContextAuthzCache(authorizationChecker, permissionService, ownerDAO);

    Optional<Map<String, OwnerType>> contexts = cache.resolveReadableContexts(principal);
    Query filter = cache.compiledRbacFilter(principal);

    assertThat(contexts).isEmpty();
    assertThat(filter).isInstanceOf(MatchAllDocsQuery.class);
    verify(permissionService, never()).getContextIdsForUserWithPermission(any(), any());
    verify(ownerDAO, never()).expandReadableContexts(any());
  }

  @Test
  public void restrictedPrincipal_cachesCompiledFilterUntilEpochBump() {
    UserPrincipal principal = new UserPrincipal("restricted", "Restricted", "default", Set.of());
    AuthorizationChecker authorizationChecker = mock(AuthorizationChecker.class);
    PermissionService permissionService = mock(PermissionService.class);
    OwnerDAO ownerDAO = mock(OwnerDAO.class);
    when(authorizationChecker.isPermitted(eq(principal), eq(Permission.READ), eq(Collections.emptyMap())))
        .thenReturn(false);
    when(permissionService.getContextIdsForUserWithPermission(principal, Permission.READ))
        .thenReturn(Collections.emptySet());

    ReadableContextAuthzCache cache =
        new ReadableContextAuthzCache(authorizationChecker, permissionService, ownerDAO);

    Query first = cache.compiledRbacFilter(principal);
    Query second = cache.compiledRbacFilter(principal);

    assertThat(first).isInstanceOf(MatchNoDocsQuery.class);
    assertThat(second).isSameAs(first);
    verify(permissionService, times(1)).getContextIdsForUserWithPermission(principal, Permission.READ);

    cache.bumpEpoch();
    Query third = cache.compiledRbacFilter(principal);

    assertThat(third).isInstanceOf(MatchNoDocsQuery.class);
    assertThat(third).isNotSameAs(first);
    verify(permissionService, times(2)).getContextIdsForUserWithPermission(principal, Permission.READ);
  }

  @Test
  public void rootOrganizationSeed_stillTreatsAsUnrestrictedAfterEnumeration() {
    UserPrincipal principal = new UserPrincipal("root-reader", "Root Reader", "default", Set.of());
    AuthorizationChecker authorizationChecker = mock(AuthorizationChecker.class);
    PermissionService permissionService = mock(PermissionService.class);
    OwnerDAO ownerDAO = mock(OwnerDAO.class);
    when(authorizationChecker.isPermitted(eq(principal), eq(Permission.READ), eq(Collections.emptyMap())))
        .thenReturn(false);
    when(permissionService.getContextIdsForUserWithPermission(principal, Permission.READ))
        .thenReturn(Set.of(Organization.ROOT_ORGANIZATION_ID));

    ReadableContextAuthzCache cache =
        new ReadableContextAuthzCache(authorizationChecker, permissionService, ownerDAO);

    assertThat(cache.resolveReadableContexts(principal)).isEmpty();
    assertThat(cache.compiledRbacFilter(principal)).isInstanceOf(MatchAllDocsQuery.class);
    verify(ownerDAO, never()).walkChildren(any());
  }

  @Test
  public void principalIdentityAndMembership_arePartOfCacheScope() {
    UserPrincipal first = new UserPrincipal("reader", "Reader", "realm-a", Set.of("group-a"));
    UserPrincipal second = new UserPrincipal("reader", "Reader", "realm-b", Set.of("group-a"));
    UserPrincipal third = new UserPrincipal("reader", "Reader", "realm-a", Set.of("group-b"));
    AuthorizationChecker authorizationChecker = mock(AuthorizationChecker.class);
    PermissionService permissionService = mock(PermissionService.class);
    OwnerDAO ownerDAO = mock(OwnerDAO.class);
    when(permissionService.getContextIdsForUserWithPermission(any(), eq(Permission.READ)))
        .thenReturn(Collections.emptySet());
    ReadableContextAuthzCache cache =
        new ReadableContextAuthzCache(authorizationChecker, permissionService, ownerDAO);

    cache.compiledRbacFilter(first);
    cache.compiledRbacFilter(second);
    cache.compiledRbacFilter(third);
    cache.compiledRbacFilter(first);
    cache.compiledRbacFilter(second);
    cache.compiledRbacFilter(third);

    verify(permissionService, times(3)).getContextIdsForUserWithPermission(any(), eq(Permission.READ));
  }

  @Test
  public void cacheIsSizeBounded() {
    UserPrincipal first = new UserPrincipal("reader-a", "Reader A", "default", Set.of());
    UserPrincipal second = new UserPrincipal("reader-b", "Reader B", "default", Set.of());
    AuthorizationChecker authorizationChecker = mock(AuthorizationChecker.class);
    PermissionService permissionService = mock(PermissionService.class);
    OwnerDAO ownerDAO = mock(OwnerDAO.class);
    when(permissionService.getContextIdsForUserWithPermission(any(), eq(Permission.READ)))
        .thenReturn(Collections.emptySet());
    ReadableContextAuthzCache cache = new ReadableContextAuthzCache(
        authorizationChecker, permissionService, ownerDAO, 1, Duration.ofMinutes(5), Ticker.systemTicker());

    cache.compiledRbacFilter(first);
    cache.compiledRbacFilter(second);
    cache.compiledRbacFilter(first);

    verify(permissionService, times(2)).getContextIdsForUserWithPermission(first, Permission.READ);
  }

  @Test
  public void cacheExpiresAfterTtl() {
    UserPrincipal principal = new UserPrincipal("reader", "Reader", "default", Set.of());
    AuthorizationChecker authorizationChecker = mock(AuthorizationChecker.class);
    PermissionService permissionService = mock(PermissionService.class);
    OwnerDAO ownerDAO = mock(OwnerDAO.class);
    when(permissionService.getContextIdsForUserWithPermission(principal, Permission.READ))
        .thenReturn(Collections.emptySet());
    AtomicLong elapsedNanos = new AtomicLong();
    Ticker ticker = new Ticker()
    {
      @Override
      public long read() {
        return elapsedNanos.get();
      }
    };
    ReadableContextAuthzCache cache = new ReadableContextAuthzCache(
        authorizationChecker, permissionService, ownerDAO, 10, Duration.ofMinutes(5), ticker);

    cache.compiledRbacFilter(principal);
    elapsedNanos.addAndGet(TimeUnit.MINUTES.toNanos(6));
    cache.compiledRbacFilter(principal);

    verify(permissionService, times(2)).getContextIdsForUserWithPermission(principal, Permission.READ);
  }

  @Test
  public void cacheExpiresAfterWriteEvenWhenAccessedContinuously() {
    UserPrincipal principal = new UserPrincipal("reader", "Reader", "default", Set.of());
    AuthorizationChecker authorizationChecker = mock(AuthorizationChecker.class);
    PermissionService permissionService = mock(PermissionService.class);
    OwnerDAO ownerDAO = mock(OwnerDAO.class);
    when(permissionService.getContextIdsForUserWithPermission(principal, Permission.READ))
        .thenReturn(Collections.emptySet());
    AtomicLong elapsedNanos = new AtomicLong();
    Ticker ticker = new Ticker()
    {
      @Override
      public long read() {
        return elapsedNanos.get();
      }
    };
    ReadableContextAuthzCache cache = new ReadableContextAuthzCache(
        authorizationChecker, permissionService, ownerDAO, 10, Duration.ofMinutes(5), ticker);

    cache.compiledRbacFilter(principal);
    // Continuous access must not prevent expireAfterWrite from reclaiming the entry.
    for (int i = 0; i < 4; i++) {
      elapsedNanos.addAndGet(TimeUnit.MINUTES.toNanos(1));
      cache.compiledRbacFilter(principal);
    }
    verify(permissionService, times(1)).getContextIdsForUserWithPermission(principal, Permission.READ);

    elapsedNanos.addAndGet(TimeUnit.MINUTES.toNanos(2)); // past 5-minute write TTL
    cache.compiledRbacFilter(principal);

    verify(permissionService, times(2)).getContextIdsForUserWithPermission(principal, Permission.READ);
  }

  @Test
  public void tenantDeregistrationEvictsCacheAndEpoch() {
    UserPrincipal principal = new UserPrincipal("reader", "Reader", "default", Set.of());
    AuthorizationChecker authorizationChecker = mock(AuthorizationChecker.class);
    PermissionService permissionService = mock(PermissionService.class);
    OwnerDAO ownerDAO = mock(OwnerDAO.class);
    when(permissionService.getContextIdsForUserWithPermission(principal, Permission.READ))
        .thenReturn(Collections.emptySet());
    ReadableContextAuthzCache cache =
        new ReadableContextAuthzCache(authorizationChecker, permissionService, ownerDAO);

    cache.compiledRbacFilter(principal);
    cache.bumpEpoch();
    cache.deregister();
    cache.compiledRbacFilter(principal);

    assertThat(cache.currentEpoch()).isZero();
    verify(permissionService, times(2)).getContextIdsForUserWithPermission(principal, Permission.READ);
  }

  @Test
  public void cachedContextsCannotBeMutatedByCallers() {
    UserPrincipal principal = new UserPrincipal("reader", "Reader", "default", Set.of());
    AuthorizationChecker authorizationChecker = mock(AuthorizationChecker.class);
    PermissionService permissionService = mock(PermissionService.class);
    OwnerDAO ownerDAO = mock(OwnerDAO.class);
    when(permissionService.getContextIdsForUserWithPermission(principal, Permission.READ))
        .thenReturn(Set.of("context-id"));
    when(ownerDAO.expandReadableContexts(Set.of("context-id")))
        .thenReturn(Map.of("context-id", OwnerType.ORGANIZATION));
    ReadableContextAuthzCache cache =
        new ReadableContextAuthzCache(authorizationChecker, permissionService, ownerDAO);

    Map<String, OwnerType> contexts = cache.resolveReadableContexts(principal).orElseThrow();

    assertThatThrownBy(() -> contexts.clear()).isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  public void cachedAndFallbackPaths_agreeForGlobalRestrictedAndNoPermission() {
    UserPrincipal globalPrincipal = new UserPrincipal("admin", "Admin", "default", Set.of());
    UserPrincipal restrictedPrincipal = new UserPrincipal("restricted", "Restricted", "default", Set.of());
    UserPrincipal noPermissionPrincipal = new UserPrincipal("none", "None", "default", Set.of());

    AuthorizationChecker authorizationChecker = mock(AuthorizationChecker.class);
    PermissionService permissionService = mock(PermissionService.class);
    OwnerDAO ownerDAO = mock(OwnerDAO.class);

    when(authorizationChecker.isPermitted(globalPrincipal, Permission.READ, Collections.emptyMap())).thenReturn(true);
    when(authorizationChecker.isPermitted(restrictedPrincipal, Permission.READ, Collections.emptyMap()))
        .thenReturn(false);
    when(authorizationChecker.isPermitted(noPermissionPrincipal, Permission.READ, Collections.emptyMap()))
        .thenReturn(false);

    when(permissionService.getContextIdsForUserWithPermission(restrictedPrincipal, Permission.READ))
        .thenReturn(Set.of("org-1"));
    when(permissionService.getContextIdsForUserWithPermission(noPermissionPrincipal, Permission.READ))
        .thenReturn(Collections.emptySet());
    when(ownerDAO.expandReadableContexts(Set.of("org-1")))
        .thenReturn(Map.of("org-1", OwnerType.ORGANIZATION, "app-1", OwnerType.APPLICATION));
    when(ownerDAO.expandReadableContexts(Collections.emptySet())).thenReturn(Collections.emptyMap());

    ReadableContextAuthzCache cache =
        new ReadableContextAuthzCache(authorizationChecker, permissionService, ownerDAO);

    assertThat(cache.resolveReadableContexts(globalPrincipal)).isEmpty();
    assertThat(fallbackResolve(globalPrincipal, authorizationChecker, permissionService, ownerDAO)).isEmpty();

    assertThat(cache.resolveReadableContexts(restrictedPrincipal).orElseThrow())
        .containsExactlyEntriesOf(Map.of("org-1", OwnerType.ORGANIZATION, "app-1", OwnerType.APPLICATION));
    assertThat(fallbackResolve(restrictedPrincipal, authorizationChecker, permissionService, ownerDAO).orElseThrow())
        .containsExactlyEntriesOf(Map.of("org-1", OwnerType.ORGANIZATION, "app-1", OwnerType.APPLICATION));

    assertThat(cache.resolveReadableContexts(noPermissionPrincipal)).hasValue(Collections.emptyMap());
    assertThat(fallbackResolve(noPermissionPrincipal, authorizationChecker, permissionService, ownerDAO))
        .hasValue(Collections.emptyMap());
  }

  private static Optional<Map<String, OwnerType>> fallbackResolve(
      final UserPrincipal principal,
      final AuthorizationChecker authorizationChecker,
      final PermissionService permissionService,
      final OwnerDAO ownerDAO)
  {
    if (authorizationChecker.isPermitted(principal, Permission.READ, Collections.emptyMap())) {
      return Optional.empty();
    }
    Set<String> contextIdsWithReadPermission =
        permissionService.getContextIdsForUserWithPermission(principal, Permission.READ);
    if (contextIdsWithReadPermission.contains(MembershipMapping.GLOBAL_CONTEXT_ID) ||
        contextIdsWithReadPermission.contains(Organization.ROOT_ORGANIZATION_ID))
    {
      return Optional.empty();
    }
    return Optional.of(ownerDAO.expandReadableContexts(contextIdsWithReadPermission));
  }
}
