/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.scheduler;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.db.DataSourceFactory;
import com.sonatype.insight.brain.db.OperationalDataStoreProvider;
import com.sonatype.insight.brain.db.PostgresDatabaseEngine;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.product.license.ProductLicenseListener;
import com.sonatype.insight.db.DatabaseEngine;
import com.sonatype.insight.db.H2DatabaseEngine;
import com.sonatype.insight.license.model.LicensedFeature;

import org.quartz.JobPersistenceException;
import org.quartz.impl.jdbcjobstore.HSQLDBDelegate;
import org.quartz.impl.jdbcjobstore.InvalidConfigurationException;
import org.quartz.impl.jdbcjobstore.JobStoreTX;
import org.quartz.impl.jdbcjobstore.PostgreSQLDelegate;
import org.quartz.impl.jdbcjobstore.SchedulerStateRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
public class QuartzJobStoreTX
    extends JobStoreTX
    implements ProductLicenseListener
{
  private static final Logger log = LoggerFactory.getLogger(QuartzJobStoreTX.class);

  private static final String SHUTDOWN_THREAD_NAME = "Unclustered-Node-Shutdown";

  private static final int NODE_CLUSTERING_NOT_SUPPORTED_EXIT_STATUS = 13;

  // Visible for testing
  static final String NODE_CLUSTERING_NOT_SUPPORTED_MESSAGE =
      "Node clustering is not supported by the current license, shutting down this excess node.";

  static final String DATA_SOURCE_NAME = "ods";

  private final ProductLicense productLicense;

  private volatile boolean productLicenseLoaded;

  private volatile boolean isShuttingDown;

  @Inject
  public QuartzJobStoreTX(ProductLicense productLicense) throws InvalidConfigurationException {
    this.productLicense = productLicense;
    initialize();
  }

  // Visible for testing
  void initialize() throws InvalidConfigurationException {
    setDataSource(DATA_SOURCE_NAME);
    setTablePrefix(OperationalDataStoreProvider.ID + ".QRTZ_");
    setUseProperties("true");
    DatabaseEngine dbEngine = DataSourceFactory.getDatabaseEngine(OperationalDataStoreProvider.getDataSource());
    if (H2DatabaseEngine.INSTANCE.equals(dbEngine)) {
      setIsClustered(false);
      setDriverDelegateClass(HSQLDBDelegate.class.getName());
    }
    else if (PostgresDatabaseEngine.INSTANCE.equals(dbEngine)) {
      setIsClustered(true);
      setDriverDelegateClass(PostgreSQLDelegate.class.getName());
    }
  }

  @Override
  protected boolean doCheckin() throws JobPersistenceException {
    if (isShuttingDown) {
      return false;
    }
    if (shouldExitDueToOtherNodeInCluster()) {
      log.error(NODE_CLUSTERING_NOT_SUPPORTED_MESSAGE);
      // Need to trigger shutdown in a different thread otherwise it can deadlock because
      // - System.exit makes this thread (probably clusterManagementThread) wait for ApplicationShutdownHooks to finish
      // - ApplicationShutdownHooks includes org.eclipse.jetty.util.thread.ShutdownThread which stops TaskScheduler,
      //   TaskScheduler stops our org.quartz.Scheduler, which in turn waits for clusterManagementThread to finish
      // i.e. clusterManagementThread waits for ShutdownThread, and ShutdownThread waits for clusterManagementThread
      isShuttingDown = true;
      exitInNewThread();
      return false;
    }
    // Defer calling super.doCheckin() to allow us to check if other nodes checked-in after our previous check-in
    return super.doCheckin();
  }

  private boolean shouldExitDueToOtherNodeInCluster() throws JobPersistenceException {
    if (!productLicenseLoaded || productLicense.hasFeature(LicensedFeature.NODE_CLUSTERING)) {
      return false;
    }
    List<SchedulerStateRecord> schedulerStateRecords = getSchedulerStateRecords();
    SchedulerStateRecord myRecord = schedulerStateRecords.stream()
        .filter(schedulerStateRecord -> schedulerStateRecord.getSchedulerInstanceId().equals(getInstanceId()))
        .findFirst()
        .orElse(null);
    if (myRecord == null) {
      return false;
    }
    List<SchedulerStateRecord> otherRecordsToExitFor = schedulerStateRecords.stream()
        .filter(otherRecord -> !otherRecord.getSchedulerInstanceId().equals(getInstanceId()))
        .filter(otherRecord -> shouldExitDueToOtherNodeInCluster(myRecord, otherRecord))
        .collect(Collectors.toList());
    if (otherRecordsToExitFor.isEmpty()) {
      return false;
    }
    log.debug("Node clustering is not supported by the current license," +
            " but with our own scheduler state record {}" +
            " found other scheduler state records to cause us to exit [{}].",
        schedulerStateRecordToString(myRecord),
        otherRecordsToExitFor.stream().map(this::schedulerStateRecordToString).collect(Collectors.joining(",")));
    return true;
  }

  private boolean shouldExitDueToOtherNodeInCluster(
      SchedulerStateRecord myRecord,
      SchedulerStateRecord otherRecord)
  {
    return otherRecord.getCheckinTimestamp() > myRecord.getCheckinTimestamp() ||
        (otherRecord.getCheckinTimestamp() == myRecord.getCheckinTimestamp() &&
            myRecord.getSchedulerInstanceId().compareTo(otherRecord.getSchedulerInstanceId()) > 0);
  }

  private String schedulerStateRecordToString(SchedulerStateRecord schedulerStateRecord) {
    return schedulerStateRecord.getSchedulerInstanceId() + " - " + schedulerStateRecord.getCheckinTimestamp();
  }

  // Visible for testing
  List<SchedulerStateRecord> getSchedulerStateRecords() throws JobPersistenceException {
    Connection conn = getNonManagedTXConnection();
    try {
      return getDelegate().selectSchedulerStateRecords(conn, null);
    }
    catch (SQLException e) {
      throw new JobPersistenceException("Database error while reading cluster state.", e);
    }
    finally {
      cleanupConnection(conn);
    }
  }

  // Visible for testing
  void exitInNewThread() {
    new Thread(() -> System.exit(NODE_CLUSTERING_NOT_SUPPORTED_EXIT_STATUS), SHUTDOWN_THREAD_NAME).start();
  }

  @Override
  public void productLicenseChanged() {
    productLicenseLoaded = true;
  }
}
