/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.lock;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;

import com.sonatype.insight.brain.AbstractDataTest;
import com.sonatype.insight.brain.common.test.PostgresTestCategory;
import com.sonatype.insight.brain.common.test.SlowTest;
import com.sonatype.insight.brain.db.rule.DatabaseRuleAnnotations.PostgresTest;
import com.sonatype.insight.test.LogOutput;

import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static com.sonatype.insight.brain.tenancy.TenantTestHelper.testAsNewTenant;

import static org.assertj.core.api.Assertions.as;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@PostgresTest
@Category({SlowTest.class, PostgresTestCategory.class})
public class PostgresAdvisoryLockDAOTest
    extends AbstractDataTest
{
  private static final String TEST_LOCK_APP_ID = "PostgresAdvisoryLockDAOTest";

  private static final ClusterLockId TEST_LOCK = ClusterLockId.forPolicyViolations(TEST_LOCK_APP_ID);

  private static final int TEST_LOCK_OBJID = TEST_LOCK_APP_ID.hashCode();

  // on-prem IQ uses "notused" as the tenant name. The hashCode of "notused" is 0x7EF0F510.
  // The high byte there should be replaced with the ordinal of CompoundIdClass.POLICY_VIOLATIONS enum value, which is
  // 0, and the highest bit should also be set as this is a CompoundId and not a SimpleId.
  private static final int TEST_LOCK_ONPREM_CLASSID = 0x80F0F510;

  // get all currently locked advisory locks
  private static final String LOCK_QUERY =
      "SELECT classid, objid, mode FROM pg_locks WHERE locktype = 'advisory' AND granted = true;";

  private interface ThrowingConnectionConsumer
  {
    void accept(Connection c) throws Exception;
  }

  private class LockThread
      extends Thread
  {
    private final ThrowingConnectionConsumer connectionConsumer;

    private Throwable uncaughtException = null;

    public LockThread(ThrowingConnectionConsumer connectionConsumer) {
      this.connectionConsumer = connectionConsumer;
    }

    @Override
    public void run() {
      try (Connection connection = getConnection()) {
        connectionConsumer.accept(connection);
      }
      catch (Exception e) {
        uncaughtException = e;
      }
    }

    public void joinWithException(long millis) throws Exception {
      join(millis);
      if (uncaughtException != null) {
        throw new RuntimeException(uncaughtException);
      }
    }
  }

  @Rule
  public LogOutput logOutput = new LogOutput(PostgresAdvisoryLockDAO.class);

  private DataSource dataSource;

  @Before
  public void setUp() {
    dataSource = databaseRule.getOperationalDataStore().getDataSourceForLocks();
  }

  private Connection getConnection() throws SQLException {
    var conn = dataSource.getConnection();
    conn.setAutoCommit(false);
    return conn;
  }

  /**
   * Query the database and return the classid, objid, and mode of all existing advisory locks
   */
  private List<ImmutableTriple<Integer, Integer, String>> getExistingLocks() throws SQLException {
    try (Connection connection = getConnection();
        Statement stmt = connection.createStatement();
        ResultSet results = stmt.executeQuery(LOCK_QUERY))
    {
      List<ImmutableTriple<Integer, Integer, String>> retval = new ArrayList<>();
      while (results.next()) {
        retval.add(ImmutableTriple.of((int) results.getLong(1), (int) results.getLong(2), results.getString(3)));
      }

      return retval;
    }
  }

  @SafeVarargs
  private void assertExistingLocks(ImmutableTriple<Integer, Integer, String>... expectedLocks) throws SQLException {
    assertThat(getExistingLocks()).containsExactlyInAnyOrder(expectedLocks);
  }

  /**
   * Asserts that there is a single currently-locked lock whose classid is between the specified `from` (inclusive) and
   * `to` (exclusive)
   */
  private void assertLockClassidRange(int from, int to) throws SQLException {
    var currentLocks = getExistingLocks();
    assertThat(currentLocks)
        .singleElement()
        .extracting(ImmutableTriple::getLeft, as(InstanceOfAssertFactories.INTEGER))
        .isBetween(from, to - 1); // note both bounds to isBetween are inclusive
  }

  @Test
  public void testAcquireLock_Write_BlocksOtherWrite() throws Exception {
    PostgresAdvisoryLockDAO dao = new PostgresAdvisoryLockDAO();
    try (var connection1 = getConnection()) {
      dao.acquireLock(connection1, TEST_LOCK, ClusterLock.LockType.EXCLUSIVE);

      assertExistingLocks(ImmutableTriple.of(TEST_LOCK_ONPREM_CLASSID, TEST_LOCK_OBJID, "ExclusiveLock"));

      var otherThread = new LockThread((connection2) -> {
        dao.acquireLock(connection2, TEST_LOCK, ClusterLock.LockType.EXCLUSIVE);
      });
      otherThread.start();

      // Wait awhile to check that it appears to be blocked
      Thread.sleep(1000);
      assertThat(otherThread.isAlive()).isTrue();

      assertExistingLocks(ImmutableTriple.of(TEST_LOCK_ONPREM_CLASSID, TEST_LOCK_OBJID, "ExclusiveLock"));

      connection1.commit();
      otherThread.joinWithException(200);
      assertThat(otherThread.isAlive()).isFalse();

      assertThat(getExistingLocks()).isEmpty();
    }
  }

  @Test
  public void testAcquireLock_Write_BlocksOtherRead() throws Exception {
    PostgresAdvisoryLockDAO dao = new PostgresAdvisoryLockDAO();
    try (Connection connection1 = getConnection()) {
      dao.acquireLock(connection1, TEST_LOCK, ClusterLock.LockType.EXCLUSIVE);

      assertExistingLocks(ImmutableTriple.of(TEST_LOCK_ONPREM_CLASSID, TEST_LOCK_OBJID, "ExclusiveLock"));

      var otherThread = new LockThread((connection2) -> {
        dao.acquireLock(connection2, TEST_LOCK, ClusterLock.LockType.SHARED);
      });
      otherThread.start();

      // Wait awhile to check that it appears to be blocked
      Thread.sleep(1000);
      assertThat(otherThread.isAlive()).isTrue();

      assertExistingLocks(ImmutableTriple.of(TEST_LOCK_ONPREM_CLASSID, TEST_LOCK_OBJID, "ExclusiveLock"));

      connection1.commit();
      otherThread.joinWithException(200);
      assertThat(otherThread.isAlive()).isFalse();

      assertThat(getExistingLocks()).isEmpty();
    }
  }

  @Test(timeout = 1000)
  public void testAcquireLock_Write_NoBlocksReentrantWrite() throws Exception {
    PostgresAdvisoryLockDAO dao = new PostgresAdvisoryLockDAO();
    try (Connection connection1 = getConnection()) {
      dao.acquireLock(connection1, TEST_LOCK, ClusterLock.LockType.EXCLUSIVE);
      dao.acquireLock(connection1, TEST_LOCK, ClusterLock.LockType.EXCLUSIVE);

      assertExistingLocks(ImmutableTriple.of(TEST_LOCK_ONPREM_CLASSID, TEST_LOCK_OBJID, "ExclusiveLock"));
    }
  }

  @Test(timeout = 1000)
  public void testAcquireLock_Write_NoBlocksReentrantRead() throws Exception {
    PostgresAdvisoryLockDAO dao = new PostgresAdvisoryLockDAO();
    try (Connection connection1 = getConnection()) {
      dao.acquireLock(connection1, TEST_LOCK, ClusterLock.LockType.EXCLUSIVE);
      dao.acquireLock(connection1, TEST_LOCK, ClusterLock.LockType.SHARED);

      assertExistingLocks(
          ImmutableTriple.of(TEST_LOCK_ONPREM_CLASSID, TEST_LOCK_OBJID, "ExclusiveLock"),
          ImmutableTriple.of(TEST_LOCK_ONPREM_CLASSID, TEST_LOCK_OBJID, "ShareLock"));
    }
  }

  @Test
  public void testAcquireLock_Read_BlocksOtherWrite() throws Exception {
    PostgresAdvisoryLockDAO dao = new PostgresAdvisoryLockDAO();
    try (Connection connection1 = getConnection()) {
      dao.acquireLock(connection1, TEST_LOCK, ClusterLock.LockType.SHARED);

      assertExistingLocks(ImmutableTriple.of(TEST_LOCK_ONPREM_CLASSID, TEST_LOCK_OBJID, "ShareLock"));

      var otherThread = new LockThread((connection2) -> {
        dao.acquireLock(connection2, TEST_LOCK, ClusterLock.LockType.EXCLUSIVE);
      });
      otherThread.start();

      // Wait awhile to check that it appears to be blocked
      Thread.sleep(1000);
      assertThat(otherThread.isAlive()).isTrue();

      assertExistingLocks(ImmutableTriple.of(TEST_LOCK_ONPREM_CLASSID, TEST_LOCK_OBJID, "ShareLock"));

      connection1.commit();
      otherThread.joinWithException(200);
      assertThat(otherThread.isAlive()).isFalse();

      assertThat(getExistingLocks()).isEmpty();
    }
  }

  @Test
  public void testAcquireLock_Read_NoBlocksOtherRead() throws Exception {
    PostgresAdvisoryLockDAO dao = new PostgresAdvisoryLockDAO();
    try (Connection connection1 = getConnection()) {
      dao.acquireLock(connection1, TEST_LOCK, ClusterLock.LockType.SHARED);

      assertExistingLocks(ImmutableTriple.of(TEST_LOCK_ONPREM_CLASSID, TEST_LOCK_OBJID, "ShareLock"));

      var otherThread = new LockThread((connection2) -> {
        dao.acquireLock(connection2, TEST_LOCK, ClusterLock.LockType.SHARED);

        assertExistingLocks(
            ImmutableTriple.of(TEST_LOCK_ONPREM_CLASSID, TEST_LOCK_OBJID, "ShareLock"),
            ImmutableTriple.of(TEST_LOCK_ONPREM_CLASSID, TEST_LOCK_OBJID, "ShareLock"));
      });
      otherThread.start();

      otherThread.joinWithException(200);
      assertThat(otherThread.isAlive()).isFalse();

      assertExistingLocks(ImmutableTriple.of(TEST_LOCK_ONPREM_CLASSID, TEST_LOCK_OBJID, "ShareLock"));
    }
  }

  @Test(timeout = 1000)
  public void testAcquireLock_Read_NoBlocksReentrantWrite() throws Exception {
    PostgresAdvisoryLockDAO dao = new PostgresAdvisoryLockDAO();
    try (Connection connection1 = getConnection()) {
      dao.acquireLock(connection1, TEST_LOCK, ClusterLock.LockType.SHARED);
      dao.acquireLock(connection1, TEST_LOCK, ClusterLock.LockType.EXCLUSIVE);

      assertThat(getExistingLocks()).containsExactly(
          ImmutableTriple.of(TEST_LOCK_ONPREM_CLASSID, TEST_LOCK_OBJID, "ShareLock"),
          ImmutableTriple.of(TEST_LOCK_ONPREM_CLASSID, TEST_LOCK_OBJID, "ExclusiveLock"));
    }
  }

  @Test(timeout = 1000)
  public void testAcquireLock_Read_NoBlocksReentrantRead() throws Exception {
    PostgresAdvisoryLockDAO dao = new PostgresAdvisoryLockDAO();
    try (Connection connection1 = getConnection()) {
      dao.acquireLock(connection1, TEST_LOCK, ClusterLock.LockType.SHARED);
      dao.acquireLock(connection1, TEST_LOCK, ClusterLock.LockType.SHARED);

      assertExistingLocks(ImmutableTriple.of(TEST_LOCK_ONPREM_CLASSID, TEST_LOCK_OBJID, "ShareLock"));
    }
  }

  @Test
  public void testAcquireLock_AutoCommitConnection_ThrowsArgumentException() throws Exception {
    PostgresAdvisoryLockDAO dao = new PostgresAdvisoryLockDAO();
    try (Connection connection = getConnection()) {
      connection.setAutoCommit(true);
      assertThatThrownBy(() -> dao.acquireLock(connection, TEST_LOCK, ClusterLock.LockType.EXCLUSIVE))
          .isInstanceOf(IllegalArgumentException.class);
    }
  }

  @Test
  public void testAcquireLock_TransactionCommit_ReleasesLock() throws Exception {
    PostgresAdvisoryLockDAO dao = new PostgresAdvisoryLockDAO();
    try (Connection connection = getConnection()) {
      dao.acquireLock(connection, TEST_LOCK, ClusterLock.LockType.SHARED);

      assertExistingLocks(ImmutableTriple.of(TEST_LOCK_ONPREM_CLASSID, TEST_LOCK_OBJID, "ShareLock"));

      connection.commit();

      assertThat(getExistingLocks()).isEmpty();
    }
  }

  @Test
  public void testAcquireLock_TransactionRollback_ReleasesLock() throws Exception {
    PostgresAdvisoryLockDAO dao = new PostgresAdvisoryLockDAO();
    try (Connection connection = getConnection()) {
      dao.acquireLock(connection, TEST_LOCK, ClusterLock.LockType.SHARED);

      assertExistingLocks(ImmutableTriple.of(TEST_LOCK_ONPREM_CLASSID, TEST_LOCK_OBJID, "ShareLock"));

      connection.rollback();

      assertThat(getExistingLocks()).isEmpty();
    }
  }

  @Test
  public void testAcquireLock_NullClusterLockId() throws Exception {
    PostgresAdvisoryLockDAO dao = new PostgresAdvisoryLockDAO();
    try (Connection connection = getConnection()) {

      assertThatThrownBy(() -> dao.acquireLock(connection, null, ClusterLock.LockType.SHARED))
          .isInstanceOf(NullPointerException.class);
    }
  }

  @Test
  public void testTryAcquireLock_ReturnsTrueAndLocksDbIfLockAquired() throws Exception {
    PostgresAdvisoryLockDAO dao = new PostgresAdvisoryLockDAO();
    try (Connection connection = getConnection()) {
      assertThat(dao.tryAcquireLock(connection, TEST_LOCK, ClusterLock.LockType.EXCLUSIVE)).isTrue();

      assertExistingLocks(ImmutableTriple.of(TEST_LOCK_ONPREM_CLASSID, TEST_LOCK_OBJID, "ExclusiveLock"));
    }
  }

  @Test
  public void testTryAcquireLock_Write_ReturnsFalseIfAlreadyWriteLocked() throws Exception {
    PostgresAdvisoryLockDAO dao = new PostgresAdvisoryLockDAO();
    try (Connection connection1 = getConnection()) {
      dao.acquireLock(connection1, TEST_LOCK, ClusterLock.LockType.EXCLUSIVE);

      assertExistingLocks(ImmutableTriple.of(TEST_LOCK_ONPREM_CLASSID, TEST_LOCK_OBJID, "ExclusiveLock"));

      var otherThread = new LockThread((connection2) -> {
        assertThat(dao.tryAcquireLock(connection2, TEST_LOCK, ClusterLock.LockType.EXCLUSIVE)).isFalse();

        assertExistingLocks(ImmutableTriple.of(TEST_LOCK_ONPREM_CLASSID, TEST_LOCK_OBJID, "ExclusiveLock"));
      });
      otherThread.start();

      otherThread.joinWithException(200);
    }
  }

  @Test
  public void testTryAcquireLock_Write_ReturnsFalseIfAlreadyReadLocked() throws Exception {
    PostgresAdvisoryLockDAO dao = new PostgresAdvisoryLockDAO();
    try (Connection connection1 = getConnection()) {
      dao.acquireLock(connection1, TEST_LOCK, ClusterLock.LockType.SHARED);

      assertExistingLocks(ImmutableTriple.of(TEST_LOCK_ONPREM_CLASSID, TEST_LOCK_OBJID, "ShareLock"));

      var otherThread = new LockThread((connection2) -> {
        assertThat(dao.tryAcquireLock(connection2, TEST_LOCK, ClusterLock.LockType.EXCLUSIVE)).isFalse();

        assertExistingLocks(ImmutableTriple.of(TEST_LOCK_ONPREM_CLASSID, TEST_LOCK_OBJID, "ShareLock"));
      });
      otherThread.start();

      otherThread.joinWithException(200);
    }
  }

  @Test
  public void testTryAcquireLock_Read_ReturnsFalseIfAlreadyWriteLocked() throws Exception {
    PostgresAdvisoryLockDAO dao = new PostgresAdvisoryLockDAO();
    try (Connection connection1 = getConnection()) {
      dao.acquireLock(connection1, TEST_LOCK, ClusterLock.LockType.EXCLUSIVE);

      assertExistingLocks(ImmutableTriple.of(TEST_LOCK_ONPREM_CLASSID, TEST_LOCK_OBJID, "ExclusiveLock"));

      var otherThread = new LockThread((connection2) -> {
        assertThat(dao.tryAcquireLock(connection2, TEST_LOCK, ClusterLock.LockType.SHARED)).isFalse();

        assertExistingLocks(ImmutableTriple.of(TEST_LOCK_ONPREM_CLASSID, TEST_LOCK_OBJID, "ExclusiveLock"));
      });
      otherThread.start();

      otherThread.joinWithException(200);
    }
  }

  @Test
  public void testTryAcquireLock_Read_ReturnsTrueIfAlreadyReadLocked() throws Exception {
    PostgresAdvisoryLockDAO dao = new PostgresAdvisoryLockDAO();
    try (Connection connection1 = getConnection()) {
      dao.acquireLock(connection1, TEST_LOCK, ClusterLock.LockType.SHARED);

      assertExistingLocks(ImmutableTriple.of(TEST_LOCK_ONPREM_CLASSID, TEST_LOCK_OBJID, "ShareLock"));

      var otherThread = new LockThread((connection2) -> {
        assertThat(dao.tryAcquireLock(connection2, TEST_LOCK, ClusterLock.LockType.SHARED)).isTrue();

        assertExistingLocks(
            ImmutableTriple.of(TEST_LOCK_ONPREM_CLASSID, TEST_LOCK_OBJID, "ShareLock"),
            ImmutableTriple.of(TEST_LOCK_ONPREM_CLASSID, TEST_LOCK_OBJID, "ShareLock"));
      });
      otherThread.start();

      otherThread.joinWithException(200);
    }
  }

  @Test
  public void testTryAcquireLock_Write_ReturnsTrueIfReentrantWriteLocked() throws Exception {
    PostgresAdvisoryLockDAO dao = new PostgresAdvisoryLockDAO();
    try (Connection connection = getConnection()) {
      dao.acquireLock(connection, TEST_LOCK, ClusterLock.LockType.EXCLUSIVE);
      assertThat(dao.tryAcquireLock(connection, TEST_LOCK, ClusterLock.LockType.EXCLUSIVE)).isTrue();

      assertExistingLocks(ImmutableTriple.of(TEST_LOCK_ONPREM_CLASSID, TEST_LOCK_OBJID, "ExclusiveLock"));
    }
  }

  @Test
  public void testTryAcquireLock_Write_ReturnsTrueIfReentrantReadLocked() throws Exception {
    PostgresAdvisoryLockDAO dao = new PostgresAdvisoryLockDAO();
    try (Connection connection = getConnection()) {
      dao.acquireLock(connection, TEST_LOCK, ClusterLock.LockType.SHARED);
      assertThat(dao.tryAcquireLock(connection, TEST_LOCK, ClusterLock.LockType.EXCLUSIVE)).isTrue();

      assertThat(getExistingLocks()).containsExactlyInAnyOrder(
          ImmutableTriple.of(TEST_LOCK_ONPREM_CLASSID, TEST_LOCK_OBJID, "ExclusiveLock"),
          ImmutableTriple.of(TEST_LOCK_ONPREM_CLASSID, TEST_LOCK_OBJID, "ShareLock"));
    }
  }

  @Test
  public void testTryAcquireLock_Read_ReturnsTrueIfReentrantWriteLocked() throws Exception {
    PostgresAdvisoryLockDAO dao = new PostgresAdvisoryLockDAO();
    try (Connection connection = getConnection()) {
      dao.acquireLock(connection, TEST_LOCK, ClusterLock.LockType.EXCLUSIVE);
      assertThat(dao.tryAcquireLock(connection, TEST_LOCK, ClusterLock.LockType.SHARED)).isTrue();

      assertThat(getExistingLocks()).containsExactlyInAnyOrder(
          ImmutableTriple.of(TEST_LOCK_ONPREM_CLASSID, TEST_LOCK_OBJID, "ExclusiveLock"),
          ImmutableTriple.of(TEST_LOCK_ONPREM_CLASSID, TEST_LOCK_OBJID, "ShareLock"));
    }
  }

  @Test
  public void testTryAcquireLock_Read_ReturnsTrueIfReentrantReadLocked() throws Exception {
    PostgresAdvisoryLockDAO dao = new PostgresAdvisoryLockDAO();
    try (Connection connection = getConnection()) {
      dao.acquireLock(connection, TEST_LOCK, ClusterLock.LockType.SHARED);
      assertThat(dao.tryAcquireLock(connection, TEST_LOCK, ClusterLock.LockType.SHARED)).isTrue();

      assertExistingLocks(ImmutableTriple.of(TEST_LOCK_ONPREM_CLASSID, TEST_LOCK_OBJID, "ShareLock"));
    }
  }

  @Test
  public void testTryAcquireLock_AutoCommitConnection_ThrowsArgumentException() throws Exception {
    PostgresAdvisoryLockDAO dao = new PostgresAdvisoryLockDAO();
    try (Connection connection = getConnection()) {
      connection.setAutoCommit(true);
      assertThatThrownBy(() -> dao.tryAcquireLock(connection, TEST_LOCK, ClusterLock.LockType.EXCLUSIVE))
          .isInstanceOf(IllegalArgumentException.class);
    }
  }

  @Test
  public void testTryAcquireLock_TransactionCommit_ReleasesLock() throws Exception {
    PostgresAdvisoryLockDAO dao = new PostgresAdvisoryLockDAO();
    try (Connection connection = getConnection()) {
      dao.tryAcquireLock(connection, TEST_LOCK, ClusterLock.LockType.SHARED);

      assertExistingLocks(ImmutableTriple.of(TEST_LOCK_ONPREM_CLASSID, TEST_LOCK_OBJID, "ShareLock"));

      connection.commit();

      assertThat(getExistingLocks()).isEmpty();
    }
  }

  @Test
  public void testTryAcquireLock_TransactionRollback_ReleasesLock() throws Exception {
    PostgresAdvisoryLockDAO dao = new PostgresAdvisoryLockDAO();
    try (Connection connection = getConnection()) {
      dao.tryAcquireLock(connection, TEST_LOCK, ClusterLock.LockType.SHARED);

      assertExistingLocks(ImmutableTriple.of(TEST_LOCK_ONPREM_CLASSID, TEST_LOCK_OBJID, "ShareLock"));

      connection.rollback();

      assertThat(getExistingLocks()).isEmpty();
    }
  }

  @Test
  public void testTryAcquireLock_NullClusterLockId() throws Exception {
    PostgresAdvisoryLockDAO dao = new PostgresAdvisoryLockDAO();
    try (Connection connection = getConnection()) {

      assertThatThrownBy(() -> dao.tryAcquireLock(connection, null, ClusterLock.LockType.SHARED))
          .isInstanceOf(NullPointerException.class);
    }
  }

  @Test
  public void testCollisionDetection() throws Exception {
    PostgresAdvisoryLockDAO dao = new PostgresAdvisoryLockDAO();
    try (Connection connection1 = getConnection()) {
      dao.acquireLock(connection1, ClusterLockId.forPolicyViolations("Aa"), ClusterLock.LockType.EXCLUSIVE);
    }

    try (Connection connection2 = getConnection()) {
      // Different class and objid
      dao.acquireLock(connection2, ClusterLockId.forAuditJsonFileStore("asdfasfasd"), ClusterLock.LockType.EXCLUSIVE);
    }

    try (Connection connection3 = getConnection()) {
      // Same class as original lock, same objid as second one
      dao.acquireLock(connection3, ClusterLockId.forPolicyViolations("asdfasdfasd"), ClusterLock.LockType.EXCLUSIVE);
    }

    try (Connection connection4 = getConnection()) {
      // same as first lock. Should not log a warning
      dao.acquireLock(connection4, TEST_LOCK, ClusterLock.LockType.EXCLUSIVE);
    }

    // Locks 1 - 3 are entirely different. Lock 4 actually matches lock 1 so not an undesirable collision
    assertThat(logOutput).atWarnLevel().isEmpty();

    try (Connection connection5 = getConnection()) {
      // Collides with first lock since class is actually the same while objIds "Aa" and "BB" have the same hash.
      // Note: LockType shouldn't matter, collision should be detected despite that being different.
      dao.acquireLock(connection5, ClusterLockId.forPolicyViolations("BB"), ClusterLock.LockType.SHARED);
    }

    assertThat(logOutput).atWarnLevel()
        .contains("""
            Lock collision detected: existing lock on \
            CompoundId[lockClass=POLICY_VIOLATIONS, lockObjId=Aa] for tenant "notused" \
            has same database value as new lock on \
            CompoundId[lockClass=POLICY_VIOLATIONS, lockObjId=BB] for tenant "notused\"""");
  }

  @Test
  public void testDebugLogging() throws Exception {
    var expectedLogEntry = """
        Acquiring lock on \
        CompoundId[lockClass=POLICY_VIOLATIONS, lockObjId=PostgresAdvisoryLockDAOTest] for tenant "notused" with \
        classid 2163275024 (0x80f0f510) and objid 1289075911 (0x4cd5bcc7)""";

    PostgresAdvisoryLockDAO dao = new PostgresAdvisoryLockDAO();
    try (Connection connection = getConnection()) {
      dao.acquireLock(connection, TEST_LOCK, ClusterLock.LockType.EXCLUSIVE);
    }

    // prints out at debug level the first time
    assertThat(logOutput).atDebugLevel().contains(expectedLogEntry);

    // Note: LogOutput's support for capturing TRACE logs is broken, so we can't check it here
  }

  @Test
  public void testNoLockClassesCollide() throws Exception {
    PostgresAdvisoryLockDAO dao = new PostgresAdvisoryLockDAO();
    try (Connection connection = getConnection()) {

      // All the zero-param lock constructors
      dao.acquireLock(connection, ClusterLockId.forSchemaMigration(), ClusterLock.LockType.SHARED);
      dao.acquireLock(connection, ClusterLockId.forDataMigration(), ClusterLock.LockType.SHARED);
      dao.acquireLock(connection, ClusterLockId.forNewInstancePopulation(), ClusterLock.LockType.SHARED);
      dao.acquireLock(connection, ClusterLockId.forInactiveRepositoryViolationCleaner(), ClusterLock.LockType.SHARED);

      // the one-param lock constructors
      dao.acquireLock(connection, ClusterLockId.forPolicyViolations("test"), ClusterLock.LockType.SHARED);
      dao.acquireLock(connection, ClusterLockId.forPolicyViolationAggregations("test"), ClusterLock.LockType.SHARED);
      dao.acquireLock(connection, ClusterLockId.forRepositoryReevaluation("test"), ClusterLock.LockType.SHARED);
      dao.acquireLock(connection, ClusterLockId.forAuditJsonFileStore("test"), ClusterLock.LockType.SHARED);

      // the two-param lock constructors
      dao.acquireLock(connection, ClusterLockId.forPolicyViolations("test"), ClusterLock.LockType.SHARED);
      dao.acquireLock(connection, ClusterLockId.forRepositoryComponent("test", "test"), ClusterLock.LockType.SHARED);
      dao.acquireLock(connection, ClusterLockId.forPolicyEvaluation("test", "test"), ClusterLock.LockType.SHARED);
      dao.acquireLock(connection, ClusterLockId.forPdfGeneration("test", "test"), ClusterLock.LockType.SHARED);

      // Note that we can't feasibly check that the zero-param ids don't conflict with the one-param ids etc,
      // because that would require us to find different combinations of params that have the same hash code. But
      // with this approach we can at least check that the zero-param ids don't conflict with each other, and the
      // one-param ids don't conflict with each other, and the two-param ids don't conflict with each other.
      assertThat(logOutput).atWarnLevel().isEmpty();
    }
  }

  @Test
  public void testSeparateLocksPerTenant() throws Exception {
    PostgresAdvisoryLockDAO dao = new PostgresAdvisoryLockDAO();
    try (Connection connection1 = getConnection()) {
      dao.acquireLock(connection1, TEST_LOCK, ClusterLock.LockType.EXCLUSIVE);

      assertExistingLocks(ImmutableTriple.of(TEST_LOCK_ONPREM_CLASSID, TEST_LOCK_OBJID, "ExclusiveLock"));

      var otherThread = new LockThread((connection2) -> {
        testAsNewTenant("othertenant", tenant -> {
          dao.acquireLock(connection2, TEST_LOCK, ClusterLock.LockType.EXCLUSIVE);

          assertExistingLocks(
              ImmutableTriple.of(TEST_LOCK_ONPREM_CLASSID, TEST_LOCK_OBJID, "ExclusiveLock"),

              // "othertenant" hashCode = 0xD24FDBBA, and again we replace the top byte
              ImmutableTriple.of(0x884FDBBA, TEST_LOCK_OBJID, "ExclusiveLock"));
        });
      });
      otherThread.start();

      otherThread.joinWithException(200);
      assertThat(otherThread.isAlive()).isFalse();

      assertExistingLocks(ImmutableTriple.of(TEST_LOCK_ONPREM_CLASSID, TEST_LOCK_OBJID, "ExclusiveLock"));
    }
  }

  @Test
  public void testExpectedLockClassidRanges() throws Exception {
    PostgresAdvisoryLockDAO dao = new PostgresAdvisoryLockDAO();
    try (Connection connection = getConnection()) {
      dao.acquireLock(connection, ClusterLockId.forSchemaMigration(), ClusterLock.LockType.SHARED);
      assertLockClassidRange(0x00000000, 0x01000000);
      connection.commit();

      dao.acquireLock(connection, ClusterLockId.forDataMigration(), ClusterLock.LockType.SHARED);
      assertLockClassidRange(0x01000000, 0x02000000);
      connection.commit();

      dao.acquireLock(connection, ClusterLockId.forNewInstancePopulation(), ClusterLock.LockType.SHARED);
      assertLockClassidRange(0x02000000, 0x03000000);
      connection.commit();

      dao.acquireLock(connection, ClusterLockId.forInactiveRepositoryViolationCleaner(), ClusterLock.LockType.SHARED);
      assertLockClassidRange(0x03000000, 0x04000000);
      connection.commit();

      dao.acquireLock(connection, ClusterLockId.forPolicyViolations("test"), ClusterLock.LockType.SHARED);
      assertLockClassidRange(0x80000000, 0x81000000);
      connection.commit();

      dao.acquireLock(connection, ClusterLockId.forPolicyViolationAggregations("test"), ClusterLock.LockType.SHARED);
      assertLockClassidRange(0x81000000, 0x82000000);
      connection.commit();

      dao.acquireLock(connection, ClusterLockId.forRepositoryComponent("test", "test"), ClusterLock.LockType.SHARED);
      assertLockClassidRange(0x82000000, 0x83000000);
      connection.commit();

      dao.acquireLock(connection, ClusterLockId.forRepositoryReevaluation("test"), ClusterLock.LockType.SHARED);
      assertLockClassidRange(0x83000000, 0x84000000);
      connection.commit();

      dao.acquireLock(connection, ClusterLockId.forPolicyEvaluation("test", "test"), ClusterLock.LockType.SHARED);
      assertLockClassidRange(0x84000000, 0x85000000);
      connection.commit();

      dao.acquireLock(connection, ClusterLockId.forAuditJsonFileStore("test"), ClusterLock.LockType.SHARED);
      assertLockClassidRange(0x85000000, 0x86000000);
      connection.commit();

      dao.acquireLock(connection, ClusterLockId.forPdfGeneration("test", "test"), ClusterLock.LockType.SHARED);
      assertLockClassidRange(0x86000000, 0x87000000);
    }
  }
}
