/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.scale;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.sonatype.insight.brain.concurrent.PerpetualLockManager;
import com.sonatype.insight.brain.model.PerpetualLock;

import com.google.common.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PartitionHelper
{
  private static final Logger log = LoggerFactory.getLogger(PartitionHelper.class);

  private static final int DEFAULT_IDEAL_PARTITION_COUNT = 1;

  public static final int DEFAULT_PARTITION_ANALYSIS_INTERVAL_SECONDS = 60;

  private final String category;

  private final String instanceId;

  private final Set<String> myActivePartitions = new LinkedHashSet<>();

  private final Set<String> otherInstanceActivePartitions = new HashSet<>();

  private final Set<String> activeInstanceIds = new HashSet<>();

  private int partitionDistribution = DEFAULT_IDEAL_PARTITION_COUNT;

  private boolean isSingleInstanceMode = true;

  private boolean isThereCapacityElsewhere = false;

  private int partitionAnalysisIntervalSeconds = DEFAULT_PARTITION_ANALYSIS_INTERVAL_SECONDS;

  private long partitionAnalysisTimeout = -1;

  private PerpetualLockManager perpetualLockManager;

  public PartitionHelper(String category, String instanceId) {
    this.category = category;
    this.instanceId = instanceId;
  }

  /**
   * determines whether or not the caller can 'try' to use a partition. This is meant to short-circuit those cases
   * where either (a) we know the caller won't be able to get a lock for a partition or (b) when we need to balance
   * partitions away from this instance (i.e. self-throttling).
   *
   * The caller will still need to try to get the lock from the PerpetualLockManager. This method/class does not grant
   * access to the locks.
   *
   * @param partitionId the partition in question.
   * @return false if (a) another instance already known to have the lock or (b) this instance is at capacity and it
   *         'looks' like another instance my have capacity; true otherwise
   */
  public synchronized boolean canTryToUsePartition(String partitionId) {
    // we may need to force a partition analysis for new partitions when the system is evenly balanced
    if (isNewPartition(partitionId) && amAtOrAboveCapacity()) {
      doPartitionAnalysis();
    }
    else {
      doPartitionAnalysisIfOutdated();
    }

    if (isSingleInstanceMode) {
      logCanUse(partitionId, true, "single instance mode");
      return true;
    }

    if (log.isDebugEnabled()) {
      log.debug("my active partitions: {}",
          myActivePartitions.stream().map(p -> p.substring(0, 5)).collect(Collectors.joining("|")));
    }

    if (isMyActivePartition(partitionId)) {
      logCanUse(partitionId, true, "my active partition");
      return true;
    }

    if (doesThisInstanceHaveCapacity() && !isExcluded(partitionId)) {
      logCanUse(partitionId, true, "has capacity");
      return true;
    }

    if (isExcluded(partitionId)) {
      logCanUse(partitionId, false, "excluded");
      return false;
    }

    // if we've gotten here that means that (a) this instance is at 'capacity', (b) there is a partition that is
    // unclaimed, and (c) we can't guarantee whether another instance will pick it up or perhaps has already. best
    // thing to do is to allow it if we know there's not capacity in other instances
    logCanUse(partitionId, !isThereCapacityElsewhere, "capacity elsewhere?");
    return !isThereCapacityElsewhere;
  }

  /**
   *
   * @return the set of active IQ server instance IDs, as identified during partition analysis by the heartbeat locks
   */
  public Set<String> getActiveInstanceIds() {
    return new HashSet<>(activeInstanceIds);
  }

  /**
   * tell this helper whether or not a given partition was able to be reserved by this instance; this helps to keep
   * our partition analysis 'fresh' between analysis cycles without needing to query the partition locks table
   *
   * @param partitionId the partition in question
   * @param wasReserved true if the partition was reserved for this instance, false otherwise
   */
  public void setPartitionReservationOutcome(String partitionId, boolean wasReserved) {
    if (wasReserved) {
      markActive(partitionId);
    }
    else {
      markUnavailable(partitionId);
    }
  }

  public PartitionHelper withPerpetualLockManager(PerpetualLockManager perpetualLockManager) {
    this.perpetualLockManager = perpetualLockManager;
    return this;
  }

  @VisibleForTesting
  PartitionHelper resetPartitionAnalysis() {
    partitionAnalysisTimeout = -1;
    return this;
  }

  @VisibleForTesting
  PartitionHelper withPartitionAnalysisInterval(int partitionAnalysisIntervalSeconds) {
    this.partitionAnalysisIntervalSeconds = partitionAnalysisIntervalSeconds;
    return this;
  }

  private boolean amAtOrAboveCapacity() {
    return myActivePartitions.size() >= partitionDistribution;
  }

  private void doPartitionAnalysis() {
    activeInstanceIds.clear();
    otherInstanceActivePartitions.clear();

    List<PerpetualLock> perpetualLocks = perpetualLockManager.getAllActivePerpetualLocksForCategory(category);

    int totalPartitionCount = 0;

    Set<String> myDiscoveredActivePartitions = new LinkedHashSet<>();

    // iterate thru the active locks, counting which are (a) instance heartbeat locks, (b) partition locks for this
    // instance, or (c) partition locks for other instances
    for (PerpetualLock perpetualLock : perpetualLocks) {
      activeInstanceIds.add(perpetualLock.getOwner());
      if (!isHeartbeatLock(perpetualLock)) {
        totalPartitionCount++;
        if (isMyInstanceLock(perpetualLock)) {
          myDiscoveredActivePartitions.add(perpetualLock.getId());
        }
        else {
          otherInstanceActivePartitions.add(perpetualLock.getId());
        }
      }
    }

    // we currently balance by the number of partitions and IQ instances; we don't take into account how active
    // any particular partition is, which may or may not be something we'd care to look at in the future and it would
    // add some complexity
    partitionDistribution = computeIdealPartitionCount(totalPartitionCount, activeInstanceIds.size());

    // if there is only one active IQ instance we enter single instance mode, which short-circuits the approvals
    // for partition use/access
    isSingleInstanceMode = activeInstanceIds.size() <= 1;

    // due to load balancing this instance may be over committed, in which case we need to decide which partitions
    // to keep and which to allow other instance to use
    resolveMyActivePartitions(myDiscoveredActivePartitions);

    // determine if there's available capacity in the other instances
    int otherInstancePartitionCapacity = (activeInstanceIds.size() - 1) * partitionDistribution;
    isThereCapacityElsewhere = otherInstancePartitionCapacity > otherInstanceActivePartitions.size();

    log.debug("Load balancing partition analysis completed : isSingleInstance = {}, partitions = {}({}), " +
        "instance count = {}, distribution = {}, capacity elsewhere = {}",
        isSingleInstanceMode, totalPartitionCount, myActivePartitions.size(), activeInstanceIds.size(),
        partitionDistribution, isThereCapacityElsewhere);

    updatePartitionAnalysisTimeout();
  }

  boolean isNewPartition(String partitionId) {
    return !myActivePartitions.contains(partitionId) && !otherInstanceActivePartitions.contains(partitionId);
  }

  private void logCanUse(String partitionId, final boolean canUse, String reason) {
    if (log.isTraceEnabled()) {
      log.trace("instance {} can use partition {} = {}, {}", instanceId.substring(0, 5), partitionId.substring(0, 5),
          canUse, reason);
    }
  }

  private void resolveMyActivePartitions(Set<String> discoveredPartitions) {
    if (isSingleInstanceMode || discoveredPartitions.size() <= partitionDistribution) {
      myActivePartitions.clear();
      myActivePartitions.addAll(discoveredPartitions);
    }
    else {
      Set<String> resolvedPartitions = new LinkedHashSet<>();

      // first, let's retain any of our current active partitions that are still active
      for (String partition : myActivePartitions) {
        if (discoveredPartitions.contains(partition)) {
          resolvedPartitions.add(partition);
        }
        if (resolvedPartitions.size() >= partitionDistribution) {
          break;
        }
      }

      // next, add in discovered partitions until we reach the distribution target
      if (resolvedPartitions.size() < partitionDistribution) {
        for (String partition : discoveredPartitions) {
          resolvedPartitions.add(partition);
          if (resolvedPartitions.size() >= partitionDistribution) {
            break;
          }
        }
      }

      myActivePartitions.clear();
      myActivePartitions.addAll(resolvedPartitions);
    }
  }

  private void doPartitionAnalysisIfOutdated() {
    if (null == perpetualLockManager || !isPartitionAnalysisOutdated()) {
      return;
    }
    doPartitionAnalysis();
  }

  @VisibleForTesting
  int computeIdealPartitionCount(int totalPartitionCount, int instanceCount) {
    double averagePartitionsPerOwner = Math.ceil((double) totalPartitionCount / Math.max(1, instanceCount));
    return (int) Math.max(DEFAULT_IDEAL_PARTITION_COUNT, averagePartitionsPerOwner);
  }

  private boolean doesThisInstanceHaveCapacity() {
    return myActivePartitions.size() < partitionDistribution;
  }

  private boolean isExcluded(String partitionId) {
    return otherInstanceActivePartitions.contains(partitionId);
  }

  private boolean isHeartbeatLock(PerpetualLock perpetualLock) {
    return perpetualLock.getId().equals(perpetualLock.getOwner());
  }

  private boolean isMyActivePartition(String partitionId) {
    return myActivePartitions.contains(partitionId);
  }

  private boolean isMyInstanceLock(PerpetualLock perpetualLock) {
    return instanceId.equals(perpetualLock.getOwner());
  }

  private boolean isPartitionAnalysisOutdated() {
    return System.currentTimeMillis() > partitionAnalysisTimeout;
  }

  private void markActive(String partitionId) {
    myActivePartitions.add(partitionId);
  }

  private void markUnavailable(String partitionId) {
    otherInstanceActivePartitions.add(partitionId);
    myActivePartitions.remove(partitionId);
  }

  private void updatePartitionAnalysisTimeout() {
    partitionAnalysisTimeout = System.currentTimeMillis() + partitionAnalysisIntervalSeconds * 1_000L;
  }
}
