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

import com.sonatype.insight.brain.dataaccess.ClusterLock;
import com.sonatype.insight.brain.db.DataSourceFactory;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.product.license.ProductLicenseListener;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.db.DatabaseEngine;
import com.sonatype.insight.db.H2DatabaseEngine;
import com.sonatype.insight.db.PostgresDatabaseEngine;
import com.sonatype.insight.license.model.LicensedFeature;

import org.quartz.JobPersistenceException;
import org.quartz.Trigger;
import org.quartz.impl.jdbcjobstore.InvalidConfigurationException;
import org.quartz.impl.jdbcjobstore.JobStoreTX;
import org.quartz.impl.jdbcjobstore.SchedulerStateRecord;
import org.quartz.spi.OperableTrigger;
import org.quartz.utils.DBConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
public class QuartzJobStoreTX
    extends JobStoreTX
    implements ProductLicenseListener
{
  private static final Logger log = LoggerFactory.getLogger(QuartzJobStoreTX.class);

  // Visible for testing
  static final String UNCLUSTERED_NODE_SHUTDOWN_THREAD_NAME = "Unclustered-Node-Shutdown";

  // Visible for testing
  static final int NODE_CLUSTERING_NOT_ENABLED_EXIT_STATUS = 13;

  // Visible for testing
  static final String SCHEMA_MIGRATION_UNFINISHED_SHUTDOWN_THREAD_NAME = "Schema-Migration-Unfinished-Shutdown";

  // Visible for testing
  static final int SCHEMA_MIGRATION_UNFINISHED_EXIT_STATUS = 15;

  // Visible for testing
  static final String NODE_CLUSTERING_NOT_SUPPORTED_MESSAGE =
      "Node clustering is not supported by the current license.";

  // Visible for testing
  static final String CLUSTER_DIRECTORY_NOT_SET_BY_USER_MESSAGE = "Cluster directory has not been set by the user.";

  // Visible for testing
  static final String SHUTTING_DOWN_EXCESS_NODE_MESSAGE = "Shutting down this excess node.";

  // Visible for testing
  static final String SCHEMA_MIGRATION_UNFINISHED_MESSAGE =
      "Database schema migration is unfinished. Shutting down this node.";

  public static final long CLUSTER_CHECKIN_INTERVAL_MILLIS = 7500;

  private static final String DATA_SOURCE_NAME = "ods";

  // Visible for testing
  static final String TABLE_PREFIX = OperationalDataStore.ID + ".QRTZ_";

  private final ProductLicense productLicense;

  private final InsightConfig insightConfig;

  protected final OperationalDataStore operationalDataStore;

  private volatile boolean productLicenseLoaded;

  private volatile boolean isShuttingDown;

  @Inject
  public QuartzJobStoreTX(
      ProductLicense productLicense,
      InsightConfig insightConfig,
      OperationalDataStore operationalDataStore)
      throws InvalidConfigurationException
  {
    this.productLicense = productLicense;
    this.insightConfig = insightConfig;
    this.operationalDataStore = operationalDataStore;
    initialize();
  }

  // Visible for testing
  void initialize() throws InvalidConfigurationException {
    setDataSource(DATA_SOURCE_NAME);
    setTablePrefix(OperationalDataStore.ID + ".QRTZ_");
    setUseProperties("true");
    setClusterCheckinInterval(CLUSTER_CHECKIN_INTERVAL_MILLIS);
    DatabaseEngine dbEngine = DataSourceFactory.getDatabaseEngine(operationalDataStore.getDataSource());
    if (H2DatabaseEngine.INSTANCE.equals(dbEngine)) {
      setIsClustered(false);
      setDriverDelegateClass(QuartzHSQLDBDelegate.class.getName());
    }
    else if (PostgresDatabaseEngine.INSTANCE.equals(dbEngine)) {
      setIsClustered(true);
      setDriverDelegateClass(QuartzPostgreSQLDelegate.class.getName());
    }
    DBConnectionManager.getInstance().addConnectionProvider(getDataSource(), new QuartzConnectionProvider());
  }

  @Override
  protected boolean doCheckin() throws JobPersistenceException {
    if (isShuttingDown) {
      return false;
    }
    if (shouldExitDueToOtherNodeInCluster()) {
      // Need to trigger shutdown in a different thread otherwise it can deadlock because
      // - System.exit makes this thread (probably clusterManagementThread) wait for ApplicationShutdownHooks to finish
      // - ApplicationShutdownHooks includes org.eclipse.jetty.util.thread.ShutdownThread which stops TaskScheduler,
      //   TaskScheduler stops our org.quartz.Scheduler, which in turn waits for clusterManagementThread to finish
      // i.e. clusterManagementThread waits for ShutdownThread, and ShutdownThread waits for clusterManagementThread
      isShuttingDown = true;
      exitInNewThread(NODE_CLUSTERING_NOT_ENABLED_EXIT_STATUS, UNCLUSTERED_NODE_SHUTDOWN_THREAD_NAME);
      return false;
    }
    if (shouldExitDueToSchemaMigration()) {
      isShuttingDown = true;
      exitInNewThread(SCHEMA_MIGRATION_UNFINISHED_EXIT_STATUS, SCHEMA_MIGRATION_UNFINISHED_SHUTDOWN_THREAD_NAME);
      return false;
    }
    // Defer calling super.doCheckin() to allow us to check if other nodes checked-in after our previous check-in
    return super.doCheckin();
  }

  private boolean shouldExitDueToSchemaMigration() {
    boolean schemaMigrationUnfinished = ClusterLock.lockExists(ClusterLock.getLockIdForSchemaMigrationInProgress());
    if (schemaMigrationUnfinished) {
      log.error(SCHEMA_MIGRATION_UNFINISHED_MESSAGE);
    }
    return schemaMigrationUnfinished;
  }

  private boolean shouldExitDueToOtherNodeInCluster() throws JobPersistenceException {
    if (!productLicenseLoaded) {
      return false;
    }
    String potentialErrorMessage = getPotentialErrorMessage();
    if (potentialErrorMessage == null) {
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
    log.debug("Node clustering is not enabled," +
            " but with our own scheduler state record {}" +
            " found other scheduler state records to cause us to exit [{}].",
        schedulerStateRecordToString(myRecord),
        otherRecordsToExitFor.stream().map(this::schedulerStateRecordToString).collect(Collectors.joining(",")));
    log.error(potentialErrorMessage);
    return true;
  }

  private String getPotentialErrorMessage() {
    String potentialErrorMessage = "";
    if (!productLicense.hasFeature(LicensedFeature.NODE_CLUSTERING)) {
      potentialErrorMessage += NODE_CLUSTERING_NOT_SUPPORTED_MESSAGE + " ";
    }
    if (!insightConfig.isClusterDirectorySetByUser()) {
      potentialErrorMessage += CLUSTER_DIRECTORY_NOT_SET_BY_USER_MESSAGE + " ";
    }
    if (potentialErrorMessage.isEmpty()) {
      return null;
    }
    return potentialErrorMessage + SHUTTING_DOWN_EXCESS_NODE_MESSAGE;
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

  public List<SchedulerStateRecord> getSchedulerStateRecords() throws JobPersistenceException {
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
  void exitInNewThread(int status, String threadName) {
    new Thread(() -> System.exit(status), threadName).start();
  }

  @Override
  protected List<OperableTrigger> acquireNextTrigger(Connection conn, long noLaterThan, int maxCount, long timeWindow)
      throws JobPersistenceException
  {
    List<OperableTrigger> operableTriggers = super.acquireNextTrigger(conn, noLaterThan, maxCount, timeWindow);
    operableTriggers.forEach(this::vetoIfTriggerForOtherNode);
    return operableTriggers;
  }

  private void vetoIfTriggerForOtherNode(Trigger trigger) {
    // we don't want to actually execute the job meant for the other node, we just need to drive the trigger state
    // to completion such that it and eventually the job get removed from the scheduler
    String nodeId = trigger.getJobDataMap().getString(TaskScheduler.QUARTZ_NODE_ID);
    if (nodeId != null && !instanceId.equals(nodeId)) {
      trigger.getJobDataMap().putAsString(QuartzTriggerListener.QUARTZ_VETO, true);
    }
  }

  @Override
  public void productLicenseChanged() {
    productLicenseLoaded = true;
  }
}
