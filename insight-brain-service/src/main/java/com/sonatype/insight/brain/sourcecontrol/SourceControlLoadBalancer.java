/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sourcecontrol;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.concurrent.PerpetualLockManager;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlEventDAO;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent;
import com.sonatype.insight.brain.scale.HeartbeatPartitionManager;
import com.sonatype.insight.brain.scale.SelfThrottlingLoadBalancer;
import com.sonatype.insight.brain.tenancy.TenantThreadLocal;
import com.sonatype.insight.brain.tenancy.TenantUtil;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.shiro.util.CollectionUtils;

import static com.sonatype.insight.brain.git.PullRequestPollingScheduler.PULL_REQUEST_DISCOVERY_INTERVAL_SECONDS;

@Named
@Singleton
public class SourceControlLoadBalancer
    extends SelfThrottlingLoadBalancer
{
  public static final int SOURCE_CONTROL_INSTANCE_RESERVATION_SECONDS = PULL_REQUEST_DISCOVERY_INTERVAL_SECONDS + 5;

  public static final int SOURCE_CONTROL_EVENT_PROCESSING_INTERVAL_SECONDS = 15;

  // Public so callers (e.g. SourceControlStaleEventResetJob) can read this category to compute the
  // active-instance-id set. The load balancer is the writer of these heartbeat / partition-reservation
  // rows; the cleanup job is a reader. Sharing one constant prevents silent drift if the literal ever
  // changes (CLAUDE.md §15).
  public static final String LOAD_BALANCER_CATEGORY_FOR_SCM = "source-control";

  private final SourceControlEventDAO sourceControlEventDAO;

  @Inject
  public SourceControlLoadBalancer(
      HeartbeatPartitionManager heartbeatPartitionManager,
      PerpetualLockManager perpetualLockManager,
      SourceControlEventDAO sourceControlEventDAO,
      TenantUtil tenantUtil)
  {
    super(heartbeatPartitionManager, perpetualLockManager, LOAD_BALANCER_CATEGORY_FOR_SCM, tenantUtil);
    setPartitionReservationSeconds(SOURCE_CONTROL_INSTANCE_RESERVATION_SECONDS);
    this.sourceControlEventDAO = sourceControlEventDAO;
  }

  /**
   * Obtains a list of available/unassigned source control events that this instance can process. This entails:
   * - finding the unassigned events
   * - checking each event to see if it can be processed by this instance (i.e. we have capacity and can get the lock)
   *
   * @return
   */
  public List<SourceControlEvent> acquireEventsToProcess() {
    List<SourceControlEvent> result = new ArrayList<>();
    List<SourceControlEvent> availableEvents = sourceControlEventDAO.getUnassignedEventsToProcess();
    if (!CollectionUtils.isEmpty(availableEvents)) {
      // as we iterate thru the events and discover which ones can and cannot be processed by this instance remember
      // the outcomes for the associated partition keys so we can avoid trying to reserve any related events
      // when we already know we won't be able to
      Map<String, Boolean> partitionAllowState = new HashMap<>();
      for (SourceControlEvent event : availableEvents) {
        String partitionKey = toPartitionKey(event.getScmUsername());
        if (!partitionAllowState.containsKey(partitionKey)) {
          boolean canProcessEvent = reserveEvent(event);
          partitionAllowState.put(partitionKey, canProcessEvent);
          if (canProcessEvent) {
            result.add(event);
          }
        }
        else if (Boolean.TRUE.equals(partitionAllowState.get(partitionKey))) {
          reserveEventForInstance(event);
          result.add(event);
        }
      }
    }
    return result;
  }

  /**
   * determines whether or not this instance is allowed to poll for pull requests on behalf of the given scm user
   *
   * @param scmUsername
   * @return
   */
  public boolean canPollForPullRequests(String scmUsername) {
    return super.canUsePartition(toPartitionKey(scmUsername));
  }

  /**
   * determine whether or not this instance can reserve the given event; this comes down to whether or not this
   * instance has 'capacity', per the load balancing rules, and can get a perpetual lock for the partition represented
   * by this event
   *
   * @param sourceControlEvent
   * @return true if this instance has capacity and can get the lock
   */
  public boolean reserveEvent(SourceControlEvent sourceControlEvent) {
    boolean canUsePartition = super.canUsePartition(toPartitionKey(sourceControlEvent.getScmUsername()));
    if (canUsePartition) {
      reserveEventForInstance(sourceControlEvent);
    }
    return canUsePartition;
  }

  private void reserveEventForInstance(SourceControlEvent sourceControlEvent) {
    sourceControlEventDAO.reserveEventForInstance(sourceControlEvent, getInstanceId());
  }

  /**
   * Related events are source control events for the same SCM user. This method instructs the load balancer
   * to release (unassign) events for the associated user so they can be picked up by other instances in
   * the cluster
   *
   * @param sourceControlEvent an event representative of the SCM user
   * @param releasePerpetualLock this flag tells the load balancer whether or not to immediately release the associated
   *          perpetual lock; `true` signifies there is no scm event work currently in flight for
   *          the associated SCM user by this instance; `false` means that there IS still scm event
   *          work being processed by this instance for the associated scm user and that the
   *          perpetual lock for the related partition should be allowed to expire
   */
  public void releaseRelatedEvents(SourceControlEvent sourceControlEvent, boolean releasePerpetualLock) {
    sourceControlEventDAO.releaseRelatedEvents(sourceControlEvent);
    if (releasePerpetualLock) {
      perpetualLockManager.releasePerpetualLock(toPartitionKey(sourceControlEvent.getScmUsername()), getInstanceId());
    }
  }

  private String toPartitionKey(String scmUsername) {
    return DigestUtils.sha256Hex(String.format("%s:%s", TenantThreadLocal.getTenant().tenantSlug, scmUsername));
  }
}
