/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.lock;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import jakarta.inject.Named;
import jakarta.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

import com.sonatype.insight.brain.dataaccess.lock.ClusterLockId.CompoundId;
import com.sonatype.insight.brain.dataaccess.lock.ClusterLockId.SimpleId;
import com.sonatype.insight.brain.tenancy.Tenant;
import com.sonatype.insight.brain.tenancy.TenantThreadLocal;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

/**
 * Implementation of cluster-wide application locks using Postgres advisory locks.
 *
 * Important note: postgres' advisory locks are database-wide, and cannot be inherently separated by schema/tenant.
 * Therefore this class encodes the tenant into the classid (the first param in the queries below) to ensure that
 * locks are per-tenant.
 */
@Named
@Singleton
public class PostgresAdvisoryLockDAO
{
  private static final Logger log = LoggerFactory.getLogger(PostgresAdvisoryLockDAO.class);

  private static final String EXCLUSIVE_LOCK_QUERY = "SELECT pg_advisory_xact_lock(?, ?);";

  private static final String SHARED_LOCK_QUERY = "SELECT pg_advisory_xact_lock_shared(?, ?);";

  private static final String EXCLUSIVE_NOWAIT_LOCK_QUERY = "SELECT pg_try_advisory_xact_lock(?, ?);";

  private static final String SHARED_NOWAIT_LOCK_QUERY = "SELECT pg_try_advisory_xact_lock_shared(?, ?);";

  /*
   * These two masks are used to construct the high byte of the database classid. They distinguish between
   * simple and compound lock ids.
   */
  private static final byte SIMPLE_ID_CLASS_NUM_MASK = 0;

  private static final byte COMPOUND_ID_CLASS_NUM_MASK = (byte) 0b10000000;

  private record CollisionMapValue(Tenant tenant, ClusterLockId clusterLockId)
  {
  }

  private record CollisionMapKey(int dbClassId, int dbObjId)
  {
  }

  /*
   * This map aids in finding possible key collisions. The reality is that the postgres API only takes
   * ints as the lock values, and our existing API for locking uses strings, so there is a risk that two things
   * intended to be different locks may be the same lock in postgres. This mechanism will warn when that is detected,
   * but note that it is not guaranteed to be detected if the locks in question come from different nodes or if
   * the older lock ages out of the cache before the newer lock appears.
   *
   * This map is capped at 2^16 entries to prevent it from growing indefinitely
   */
  private final Cache<CollisionMapKey, CollisionMapValue> knownLockMappings = CacheBuilder.newBuilder()
      .maximumSize(1 << 16)
      .build();

  /**
   * @param connection a JDBC connection which must have auto-commit set to false
   */
  public void acquireLock(Connection connection, ClusterLockId clusterLockId, ClusterLock.LockType lockType) {
    acquireLock(connection, clusterLockId, lockType, true);
  }

  /**
   * @param connection a JDBC connection which must have auto-commit set to false
   *
   * @return whether or not the lock was acquired. This will always be true when `waitForLock` is true
   */
  public boolean tryAcquireLock(Connection connection, ClusterLockId clusterLockId, ClusterLock.LockType lockType) {
    return acquireLock(connection, clusterLockId, lockType, false);
  }

  private boolean acquireLock(
      Connection connection,
      ClusterLockId clusterLockId,
      ClusterLock.LockType lockType,
      boolean waitForLock)
  {
    validateNonNull(clusterLockId);
    validateAutoCommit(connection);

    var tenant = TenantThreadLocal.getTenant();
    var dbClassId = computeDbClassId(tenant, clusterLockId);
    var dbObjId = computeDbObjId(clusterLockId);
    var query = switch (lockType) {
      case EXCLUSIVE -> waitForLock ? EXCLUSIVE_LOCK_QUERY : EXCLUSIVE_NOWAIT_LOCK_QUERY;
      case SHARED -> waitForLock ? SHARED_LOCK_QUERY : SHARED_NOWAIT_LOCK_QUERY;
    };

    logAndCheckForLockCollision(tenant, clusterLockId, dbClassId, dbObjId);

    try (PreparedStatement stmt = connection.prepareStatement(query)) {
      stmt.setInt(1, dbClassId);
      stmt.setInt(2, dbObjId);

      try (ResultSet results = stmt.executeQuery()) {
        if (waitForLock) {
          return true;
        }
        else {
          results.next();
          return results.getBoolean(1);
        }
      }
    }
    catch (SQLException e) {
      throw new RuntimeException("Error acquiring lock", e);
    }
  }

  private void validateNonNull(ClusterLockId clusterLockId) {
    if (clusterLockId == null) {
      throw new NullPointerException("ClusterLockId must not be null");
    }
  }

  private void validateAutoCommit(Connection connection) {
    try {
      if (connection.getAutoCommit()) {
        throw new IllegalArgumentException("Connection must have auto-commit set to false");
      }
    }
    catch (SQLException e) {
      throw new RuntimeException("Error checking auto-commit status", e);
    }
  }

  /**
   * The classid passed to postgres is constructed as follows:
   * - The high bit is used to distinguish between simple and compound lock ids
   * - The next 7 bits are used to store the enum ordinal of the lock class
   * - The remaining 24 bits are used to store the low bits of the tenant hashcode
   */
  private int computeDbClassId(Tenant tenant, ClusterLockId clusterLockId) {
    byte lockClassByte = 0;
    if (clusterLockId instanceof CompoundId compoundId) {
      lockClassByte = (byte) (COMPOUND_ID_CLASS_NUM_MASK | compoundId.lockClass().ordinal());
    }
    else if (clusterLockId instanceof SimpleId simpleId) {
      lockClassByte = (byte) (SIMPLE_ID_CLASS_NUM_MASK | simpleId.ordinal());
    }

    var tenantHashCode = tenant.hashCode();
    return (lockClassByte << 24) | (tenantHashCode & 0x00FFFFFF);
  }

  private int computeDbObjId(ClusterLockId clusterLockId) {
    /*
     * In Java 21 this could be:
     * var lockObjInt = switch (clusterLockId) {
     * case SimpleId simple -> 0;
     * case CompoundId compound -> compound.lockObjId().hashCode();
     * }
     */
    if (clusterLockId instanceof CompoundId compoundId) {
      return compoundId.lockObjId().hashCode();
    }
    else {
      return 0;
    }
  }

  private void logAndCheckForLockCollision(Tenant tenant, ClusterLockId clusterLockId, int dbClassId, int dbObjId) {
    var newMappingVal = new CollisionMapValue(tenant, clusterLockId);
    var existingMappingVal = knownLockMappings.asMap()
        .putIfAbsent(new CollisionMapKey(dbClassId, dbObjId), newMappingVal);

    if (existingMappingVal != null && !existingMappingVal.equals(newMappingVal)) {
      log.warn("""
          Lock collision detected: existing lock on {} for tenant "{}" has same database value \
          as new lock on {} for tenant "{}\"""",
          existingMappingVal.clusterLockId(), existingMappingVal.tenant().tenantSlug, clusterLockId, tenant.tenantSlug);
    }
    else {
      // For non-colliding locks, log at debug level if we've never seen them before, or trace otherwise
      log
          .atLevel(existingMappingVal == null ? Level.DEBUG : Level.TRACE)
          .log("Acquiring lock on {} for tenant \"{}\" with classid {} (0x{}) and objid {} (0x{})",
              clusterLockId, tenant.tenantSlug,
              Integer.toUnsignedString(dbClassId), Integer.toHexString(dbClassId),
              Integer.toUnsignedString(dbObjId), Integer.toHexString(dbObjId));
    }
  }
}
