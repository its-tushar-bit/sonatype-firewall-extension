/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess;

import java.util.Date;

import com.sonatype.insight.brain.db.rule.DatabaseRuleAnnotations.PostgresTest;
import com.sonatype.insight.brain.model.PerpetualLock;

import org.junit.Before;
import org.junit.Test;

import static java.lang.System.currentTimeMillis;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * PostgreSQL-backed tests relocated from {@link PerpetualLockDAOTest} (CLM-45228).
 */
@PostgresTest
public class PerpetualLockDAOPgTest
    extends AbstractDbDAOTest
{
  private static final String LOCK_CATEGORY = "testing";

  // test subject
  private PerpetualLockDAO perpetualLockDAO;

  @Before
  @Override
  public void setup() {
    super.setup();
    perpetualLockDAO = daoFactory.createPerpetualLockDAO();
    assertThat(perpetualLockDAO.getAll()).isEmpty();
  }

  // --- tryAcquireOrRenewLock tests (Postgres) ---

  @Test
  public void testTryAcquireOrRenewLock_newLockAndRenewal_postgres() {
    verifyNewLockAndRenewal();
  }

  @Test
  public void testTryAcquireOrRenewLock_rejectDifferentOwner_postgres() {
    verifyRejectDifferentOwner();
  }

  @Test
  public void testTryAcquireOrRenewLock_takeoverExpiredAndUnassigned_postgres() {
    verifyTakeoverExpiredAndUnassigned();
  }

  // --- shared helpers ---

  private void verifyNewLockAndRenewal() {
    String lockId = "upsert-new";
    Date shortExpiration = new Date(currentTimeMillis() + 10_000);
    Date longExpiration = new Date(currentTimeMillis() + 30_000);

    // insert new lock
    assertThat(perpetualLockDAO.tryAcquireOrRenewLock(lockId, LOCK_CATEGORY, "owner-1", longExpiration)).isTrue();
    assertThat(perpetualLockDAO.getPerpetualLockById(lockId).getOwner()).isEqualTo("owner-1");

    // renew with shorter expiration - GREATEST keeps the longer one
    assertThat(perpetualLockDAO.tryAcquireOrRenewLock(lockId, LOCK_CATEGORY, "owner-1", shortExpiration)).isTrue();
    assertThat(perpetualLockDAO.getPerpetualLockById(lockId).getExpirationTime()).isAfterOrEqualTo(longExpiration);

    // renew with longer expiration - extends
    Date longerExpiration = new Date(currentTimeMillis() + 60_000);
    assertThat(perpetualLockDAO.tryAcquireOrRenewLock(lockId, LOCK_CATEGORY, "owner-1", longerExpiration)).isTrue();
    assertThat(perpetualLockDAO.getPerpetualLockById(lockId).getExpirationTime()).isAfterOrEqualTo(longerExpiration);
  }

  private void verifyRejectDifferentOwner() {
    String lockId = "upsert-reject";
    Date expiration = new Date(currentTimeMillis() + 30_000);

    perpetualLockDAO.tryAcquireOrRenewLock(lockId, LOCK_CATEGORY, "owner-1", expiration);

    assertThat(perpetualLockDAO.tryAcquireOrRenewLock(lockId, LOCK_CATEGORY, "owner-2", expiration)).isFalse();
    assertThat(perpetualLockDAO.getPerpetualLockById(lockId).getOwner()).isEqualTo("owner-1");
  }

  private void verifyTakeoverExpiredAndUnassigned() {
    String lockId = "upsert-takeover";

    // expired lock can be taken over
    Date pastExpiration = new Date(currentTimeMillis() - 5_000);
    perpetualLockDAO.createPerpetualLock(lockId, LOCK_CATEGORY, "owner-1", pastExpiration);

    Date newExpiration = new Date(currentTimeMillis() + 30_000);
    assertThat(perpetualLockDAO.tryAcquireOrRenewLock(lockId, LOCK_CATEGORY, "owner-2", newExpiration)).isTrue();
    assertThat(perpetualLockDAO.getPerpetualLockById(lockId).getOwner()).isEqualTo("owner-2");

    // release the lock (sets owner=null), then a third owner can acquire
    perpetualLockDAO.releasePerpetualLockForOwner(lockId, "owner-2");
    assertThat(perpetualLockDAO.tryAcquireOrRenewLock(lockId, LOCK_CATEGORY, "owner-3", newExpiration)).isTrue();
    PerpetualLock acquired = perpetualLockDAO.getPerpetualLockById(lockId);
    assertThat(acquired.getOwner()).isEqualTo("owner-3");
    assertThat(acquired.getExpirationTime()).isAfterOrEqualTo(newExpiration);
  }
}
