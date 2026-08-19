/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.session;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Collections;
import java.util.HexFormat;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicLong;

import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.UserPrincipal;
import com.sonatype.insight.brain.search.lucene.LuceneRbacFilterQueryBuilder;
import com.sonatype.insight.brain.security.AuthorizationChecker;
import com.sonatype.insight.brain.security.PermissionService;
import com.sonatype.insight.brain.tenancy.TenantManaged;
import com.sonatype.insight.brain.tenancy.TenantReference;

import com.google.common.base.Ticker;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.apache.lucene.search.Query;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * Caches authorization resolution for index read sessions.
 * <p>
 * Global/unrestricted principals short-circuit via
 * {@link AuthorizationChecker#isPermitted(UserPrincipal, Permission, Map)} with an empty context
 * (same pattern as the SQL dashboard metrics track) and never enumerate readable contexts.
 * Restricted principals cache resolved context IDs and the compiled Lucene RBAC filter keyed by
 * principal scope hash + authorization epoch. Membership mutations and application create/move
 * bump the epoch so descendant ID lists stay permission-correct.
 */
public class ReadableContextAuthzCache
    implements TenantManaged
{
  private static final long DEFAULT_MAXIMUM_SIZE = 1_000;

  private static final Duration DEFAULT_TTL = Duration.ofMinutes(5);

  private final AuthorizationChecker authorizationChecker;

  private final PermissionService permissionService;

  private final OwnerDAO ownerDAO;

  private final long maximumSize;

  private final Duration ttl;

  private final Ticker ticker;

  private final TenantReference<AtomicLong> epochs = new TenantReference<>(AtomicLong::new);

  private final TenantReference<Cache<ScopeKey, CachedResolution>> resolutionCaches = new TenantReference<>();

  public ReadableContextAuthzCache(
      final AuthorizationChecker authorizationChecker,
      final PermissionService permissionService,
      final OwnerDAO ownerDAO)
  {
    this(authorizationChecker, permissionService, ownerDAO, DEFAULT_MAXIMUM_SIZE, DEFAULT_TTL, Ticker.systemTicker());
  }

  ReadableContextAuthzCache(
      final AuthorizationChecker authorizationChecker,
      final PermissionService permissionService,
      final OwnerDAO ownerDAO,
      final long maximumSize,
      final Duration ttl,
      final Ticker ticker)
  {
    this.authorizationChecker = authorizationChecker;
    this.permissionService = permissionService;
    this.ownerDAO = ownerDAO;
    if (maximumSize <= 0) {
      throw new IllegalArgumentException("maximumSize must be positive");
    }
    if (ttl.isZero() || ttl.isNegative()) {
      throw new IllegalArgumentException("ttl must be positive");
    }
    this.maximumSize = maximumSize;
    this.ttl = ttl;
    this.ticker = ticker;
  }

  /**
   * Invalidates all cached resolutions for the current tenant by advancing the authorization epoch.
   */
  public void bumpEpoch() {
    epochs.get().incrementAndGet();
    Cache<ScopeKey, CachedResolution> resolutions = resolutionCaches.getIfPresent();
    if (resolutions != null) {
      resolutions.invalidateAll();
    }
  }

  public long currentEpoch() {
    return epochs.get().get();
  }

  /**
   * Clears tenant-scoped cache state on deregister.
   * <p>
   * {@link com.sonatype.insight.brain.tenancy.TenantManager#deregisterTenant} binds each tenant via
   * {@code runAs} before calling deregister, so per-tenant cleanup here is intentional.
   */
  @Override
  public void deregister() {
    Cache<ScopeKey, CachedResolution> resolutions = resolutionCaches.remove();
    if (resolutions != null) {
      resolutions.invalidateAll();
    }
    epochs.remove();
  }

  /**
   * Resolved readable contexts for the principal.
   * {@link Optional#empty()} means unrestricted (global) access; an empty map means fail-closed.
   */
  public Optional<Map<String, OwnerType>> resolveReadableContexts(final UserPrincipal principal) {
    return resolve(principal).contexts();
  }

  /**
   * Compiled Lucene RBAC filter for the principal. Unrestricted principals get a match-all filter.
   */
  public Query compiledRbacFilter(final UserPrincipal principal) {
    return resolve(principal).filter();
  }

  private CachedResolution resolve(final UserPrincipal principal) {
    Objects.requireNonNull(principal, "principal");
    long currentEpoch = epochs.get().get();
    ScopeKey key = ScopeKey.of(principal, currentEpoch);
    try {
      // Use Cache.get (not asMap().computeIfAbsent) so expireAfterWrite/Access are honored.
      return resolutions().get(key, () -> resolveUncached(principal));
    }
    catch (ExecutionException e) {
      Throwable cause = e.getCause() == null ? e : e.getCause();
      if (cause instanceof RuntimeException runtimeException) {
        throw runtimeException;
      }
      throw new IllegalStateException("Failed to resolve readable-context authz cache entry", cause);
    }
  }

  private Cache<ScopeKey, CachedResolution> resolutions() {
    return resolutionCaches.computeIfAbsent(ignored -> CacheBuilder.newBuilder()
        .maximumSize(maximumSize)
        // Write TTL alone bounds worst-case staleness for continuously active searchers.
        // expireAfterAccess with the same duration is a no-op (write time <= last access), so omit it.
        .expireAfterWrite(ttl.toMillis(), MILLISECONDS)
        .ticker(ticker)
        .build());
  }

  private CachedResolution resolveUncached(final UserPrincipal principal) {
    // Global short-circuit: one cheap permission check, skip context enumeration entirely.
    if (authorizationChecker.isPermitted(principal, Permission.READ, Collections.emptyMap())) {
      Optional<Map<String, OwnerType>> unrestricted = Optional.empty();
      return new CachedResolution(unrestricted, LuceneRbacFilterQueryBuilder.build(unrestricted));
    }

    Set<String> contextIdsWithReadPermission =
        permissionService.getContextIdsForUserWithPermission(principal, Permission.READ);

    Optional<Map<String, OwnerType>> contexts;
    if (contextIdsWithReadPermission.contains(MembershipMapping.GLOBAL_CONTEXT_ID) ||
        contextIdsWithReadPermission.contains(Organization.ROOT_ORGANIZATION_ID))
    {
      contexts = Optional.empty();
    }
    else {
      contexts = Optional.of(Map.copyOf(ownerDAO.expandReadableContexts(contextIdsWithReadPermission)));
    }

    return new CachedResolution(contexts, LuceneRbacFilterQueryBuilder.build(contexts));
  }

  private record ScopeKey(String principalScopeHash, long authorizationEpoch)
  {
    static ScopeKey of(final UserPrincipal principal, final long authorizationEpoch) {
      MessageDigest digest = sha256();
      updateDigest(digest, principal.getUsername());
      updateDigest(digest, principal.getRealmId());
      for (String membership : new TreeSet<>(principal.getMembership())) {
        updateDigest(digest, membership);
      }
      return new ScopeKey(HexFormat.of().formatHex(digest.digest()), authorizationEpoch);
    }

    private static MessageDigest sha256() {
      try {
        return MessageDigest.getInstance("SHA-256");
      }
      catch (NoSuchAlgorithmException e) {
        throw new IllegalStateException("SHA-256 is unavailable", e);
      }
    }

    private static void updateDigest(final MessageDigest digest, final String value) {
      byte[] bytes = Objects.requireNonNull(value, "principal scope field (username/realmId/membership)")
          .getBytes(StandardCharsets.UTF_8);
      digest.update(ByteBuffer.allocate(Integer.BYTES).putInt(bytes.length).array());
      digest.update(bytes);
    }
  }

  private record CachedResolution(
      Optional<Map<String, OwnerType>> contexts,
      Query filter)
  {
  }
}
