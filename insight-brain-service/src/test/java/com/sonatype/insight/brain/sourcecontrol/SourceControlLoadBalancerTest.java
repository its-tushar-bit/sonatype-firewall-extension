/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sourcecontrol;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import com.sonatype.insight.brain.concurrent.PerpetualLockManager;
import com.sonatype.insight.brain.dataaccess.PerpetualLockDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlEventDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent;
import com.sonatype.insight.brain.scale.HeartbeatPartitionManager;
import com.sonatype.insight.brain.scale.SelfThrottlingLoadBalancer;
import com.sonatype.insight.brain.tenancy.Tenant;
import com.sonatype.insight.brain.tenancy.TenantTestHelper;
import com.sonatype.insight.brain.tenancy.TenantUtil;
import com.sonatype.insight.brain.testing.BrainInjectedTest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;

import static java.lang.Thread.sleep;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import com.sonatype.insight.brain.common.test.SlowTest;
import org.junit.experimental.categories.Category;

@Category(SlowTest.class)
public class SourceControlLoadBalancerTest
    extends BrainInjectedTest
{
  private List<SourceControlLoadBalancer> activeLoadBalancers = new ArrayList<>();

  @Before
  public void setup() {
    MockitoAnnotations.openMocks(this);
  }

  @After
  public void cleanup() {
    activeLoadBalancers.forEach(SelfThrottlingLoadBalancer::stop);
    activeLoadBalancers.clear();
  }

  @Test
  public void testCanPollForPullRequests_twoInstancesSameUser() {
    // given : an scm user and two new load balancers
    final String scmUsername = "someUser";
    SourceControlLoadBalancer instance1LoadBalancer = createLoadBalancer();
    SourceControlLoadBalancer instance2LoadBalancer = createLoadBalancer();

    // when: instance 1 is polling on behalf of a given user
    assertThat(instance1LoadBalancer.canPollForPullRequests(scmUsername)).isTrue();

    // then: instance 2 cannot poll on behalf of that user
    assertThat(instance2LoadBalancer.canPollForPullRequests(scmUsername)).isFalse();

    // and: instance 1 can still poll on behalf of given user
    assertThat(instance1LoadBalancer.canPollForPullRequests(scmUsername)).isTrue();
  }

  @Test
  public void testCanPollForPullRequests_twoInstancesDifferentUsers() {
    // given : two load balancers that are 'aware' of each other
    SourceControlLoadBalancer balancerOne = createLoadBalancer();
    SourceControlLoadBalancer balancerTwo = createLoadBalancer();
    balancerOne.rebalance();

    // given: each balancer has a partition
    assertThat(balancerOne.canPollForPullRequests("user-1")).isTrue();
    assertThat(balancerTwo.canPollForPullRequests("user-2")).isTrue();

    // then: balancer one can pickup a NEW partition
    assertThat(balancerOne.canPollForPullRequests("user-3")).isTrue();

    // then: balancer one can't pickup another partition since balancer two has capacity
    assertThat(balancerOne.canPollForPullRequests("user-4")).isFalse();
    assertThat(balancerTwo.canPollForPullRequests("user-4")).isTrue();
  }

  @Test
  public void testCanPollForPullRequests_newInstanceComesOnline() throws InterruptedException {
    // given: a balancer with a number of partitions
    SourceControlLoadBalancer balancerOne = createLoadBalancer();
    balancerOne.setPartitionReservationSeconds(5);
    assertThat(balancerOne.canPollForPullRequests("user-1")).isTrue();
    sleep(100);
    assertThat(balancerOne.canPollForPullRequests("user-2")).isTrue();
    sleep(100);
    assertThat(balancerOne.canPollForPullRequests("user-3")).isTrue();
    sleep(100);
    assertThat(balancerOne.canPollForPullRequests("user-4")).isTrue();

    // when: a new balancer comes online and balancer 1 becomes aware of it
    SourceControlLoadBalancer balancerTwo = createLoadBalancer();
    balancerTwo.setPartitionReservationSeconds(5);
    balancerOne.rebalance();

    // then: some of balancer 1's partitions have been released
    assertThat(balancerOne.canPollForPullRequests("user-1")).isTrue();
    assertThat(balancerOne.canPollForPullRequests("user-2")).isTrue();
    assertThat(balancerOne.canPollForPullRequests("user-3")).isFalse(); // this partition lock will eventually expire
    assertThat(balancerOne.canPollForPullRequests("user-4")).isFalse(); // this partition lock will eventually expire

    // and: balancer 2 can't obtain any of the partitions until the locks expire
    assertThat(balancerTwo.canPollForPullRequests("user-1")).isFalse();
    assertThat(balancerTwo.canPollForPullRequests("user-2")).isFalse();
    assertThat(balancerTwo.canPollForPullRequests("user-3")).isFalse();
    assertThat(balancerTwo.canPollForPullRequests("user-4")).isFalse();

    // when: balancer 1 locks are refreshed and/or allowed to expire
    sleep(1_000);
    balancerOne.rebalance();

    assertThat(balancerOne.canPollForPullRequests("user-1")).isTrue();
    assertThat(balancerOne.canPollForPullRequests("user-2")).isTrue();
    assertThat(balancerOne.canPollForPullRequests("user-3")).isFalse(); // this partition lock will eventually expire
    assertThat(balancerOne.canPollForPullRequests("user-4")).isFalse(); // this partition lock will eventually expire

    assertThat(balancerTwo.canPollForPullRequests("user-3")).isFalse();
    assertThat(balancerTwo.canPollForPullRequests("user-4")).isFalse();

    sleep(1_000);
    balancerOne.rebalance();

    assertThat(balancerOne.canPollForPullRequests("user-1")).isTrue();
    assertThat(balancerOne.canPollForPullRequests("user-2")).isTrue();
    assertThat(balancerOne.canPollForPullRequests("user-3")).isFalse(); // this partition lock will eventually expire
    assertThat(balancerOne.canPollForPullRequests("user-4")).isFalse(); // this partition lock will eventually expire

    assertThat(balancerTwo.canPollForPullRequests("user-3")).isFalse();
    assertThat(balancerTwo.canPollForPullRequests("user-4")).isFalse();

    sleep(3_000);
    balancerTwo.rebalance();

    // then: balancer two can pick up the expired locks
    assertThat(balancerTwo.canPollForPullRequests("user-3")).isTrue();
    assertThat(balancerOne.canPollForPullRequests("user-3")).isFalse();

    assertThat(balancerTwo.canPollForPullRequests("user-4")).isTrue();
    assertThat(balancerOne.canPollForPullRequests("user-4")).isFalse();

    assertThat(balancerOne.canPollForPullRequests("user-1")).isTrue();
    assertThat(balancerTwo.canPollForPullRequests("user-1")).isFalse();

    assertThat(balancerOne.canPollForPullRequests("user-2")).isTrue();
    assertThat(balancerTwo.canPollForPullRequests("user-2")).isFalse();
  }

  @Test
  public void testCanPollForPullRequests_instanceGoesOffline() {
    // given: two load balancers, each with a number of partitions
    SourceControlLoadBalancer balancerOne = createLoadBalancer();
    SourceControlLoadBalancer balancerTwo = createLoadBalancer();
    balancerOne.rebalance();

    assertThat(balancerOne.canPollForPullRequests("user-1")).isTrue();
    assertThat(balancerTwo.canPollForPullRequests("user-2")).isTrue();
    assertThat(balancerOne.canPollForPullRequests("user-3")).isTrue();
    assertThat(balancerTwo.canPollForPullRequests("user-4")).isTrue();
    assertThat(balancerOne.canPollForPullRequests("user-5")).isTrue();
    assertThat(balancerTwo.canPollForPullRequests("user-6")).isTrue();

    // when: balancer two goes offline, its partition locks expire, and balance one has done a partition analysis
    balancerTwo.stop();
    balancerOne.rebalance(); // so we don't have to wait for the next balancer analysis cycle

    // then: balancer one can pickup balancer two's partitions
    assertThat(balancerOne.canPollForPullRequests("user-1")).isTrue();
    assertThat(balancerOne.canPollForPullRequests("user-2")).isTrue();
    assertThat(balancerOne.canPollForPullRequests("user-3")).isTrue();
    assertThat(balancerOne.canPollForPullRequests("user-4")).isTrue();
    assertThat(balancerOne.canPollForPullRequests("user-5")).isTrue();
    assertThat(balancerOne.canPollForPullRequests("user-6")).isTrue();
  }

  @Test
  public void testReserveEvent_twoInstancesSameUser() {
    // given: two load balancers and some events for the same user
    SourceControlLoadBalancer balancerOne = createLoadBalancer();
    SourceControlLoadBalancer balancerTwo = createLoadBalancer();
    balancerOne.rebalance();
    SourceControlEvent user1Event1 = createEvent("user-1");
    SourceControlEvent user1Event2 = createEvent("user-1");

    // when: balancer one reserves an event for a given user
    assertThat(balancerOne.reserveEvent(user1Event1)).isTrue();

    // then: balancer two cannot reserve that event or other events for the same user
    assertThat(balancerTwo.reserveEvent(user1Event1)).isFalse();
    assertThat(balancerTwo.reserveEvent(user1Event2)).isFalse();
  }

  @Test
  public void testReserveEvent_twoInstancesDifferentUsers() {
    // given: two load balancers and some events for two users
    SourceControlLoadBalancer balancerOne = createLoadBalancer();
    SourceControlLoadBalancer balancerTwo = createLoadBalancer();
    balancerOne.rebalance(); // so we don't have to wait for balancerOne's next analysis cycle
    SourceControlEvent user1Event1 = createEvent("user-1");
    SourceControlEvent user1Event2 = createEvent("user-1");
    SourceControlEvent user2Event3 = createEvent("user-2");
    SourceControlEvent user2Event4 = createEvent("user-2");

    // when: balancer one reserves an event for a given user
    assertThat(balancerOne.reserveEvent(user1Event1)).isTrue();

    // then: balancer two cannot reserve that event or other events for the same user but can for the other user
    assertThat(balancerTwo.reserveEvent(user1Event1)).isFalse();
    assertThat(balancerTwo.reserveEvent(user1Event2)).isFalse();

    assertThat(balancerTwo.reserveEvent(user2Event3)).isTrue();

    // and: balancer one cannot reserve events for user 2
    assertThat(balancerOne.reserveEvent(user2Event3)).isFalse();
    assertThat(balancerOne.reserveEvent(user2Event4)).isFalse();
  }

  @Test
  public void testReserveEvent_newInstanceComesOnline() throws InterruptedException {
    // given: a load balancer and some events for multiple users for which balancer 1 has the partitions
    SourceControlLoadBalancer balancerOne = createLoadBalancer();
    balancerOne.setPartitionReservationSeconds(3);
    SourceControlEvent user1Event1 = createEvent("user-1");
    SourceControlEvent user2Event3 = createEvent("user-2");
    SourceControlEvent user3Event5 = createEvent("user-3");

    assertThat(balancerOne.reserveEvent(user1Event1)).isTrue();
    // so the partition reservations have different expiration times, which makes the validations deterministic
    sleep(500);
    assertThat(balancerOne.reserveEvent(user2Event3)).isTrue();
    sleep(500);
    assertThat(balancerOne.reserveEvent(user3Event5)).isTrue();

    // when: balancerTwo comes online and some new events for the same users are available
    SourceControlLoadBalancer balancerTwo = createLoadBalancer();
    balancerOne.rebalance(); // so we don't have to wait for balancerONe to become aware of balancerTwo
    SourceControlEvent user1Event2 = createEvent("user-1");
    SourceControlEvent user2Event4 = createEvent("user-2");
    SourceControlEvent user3Event6 = createEvent("user-3"); // will let the associated lock expire

    // then: balancerOne can still reserve events for two of the users, but not the third
    assertThat(balancerOne.reserveEvent(user1Event2)).isTrue(); // lock will be renewed
    assertThat(balancerOne.reserveEvent(user2Event4)).isTrue(); // lock will be renewed
    assertThat(balancerOne.reserveEvent(user3Event6)).isFalse(); // lock will NOT be renewed

    // and: balancer two can't reserve any of the events yet and we renew the locks for users 1 and two
    SourceControlEvent user1Event7 = createEvent("user-1");
    SourceControlEvent user2Event8 = createEvent("user-2");
    assertThat(balancerTwo.reserveEvent(user1Event7)).isFalse();
    assertThat(balancerTwo.reserveEvent(user2Event8)).isFalse();
    assertThat(balancerTwo.reserveEvent(user3Event6)).isFalse();

    // and: we can renew the locks for users 1 and 2 for balancerOne
    assertThat(balancerOne.reserveEvent(user1Event7)).isTrue();
    assertThat(balancerOne.reserveEvent(user2Event8)).isTrue();

    // when: we let the lock for user3 expire
    sleep(2_500);
    balancerTwo.rebalance();

    // then:
    assertThat(balancerTwo.reserveEvent(user3Event6)).isTrue();
  }

  @Test
  public void testReserveEvent_instanceGoesOffline() throws InterruptedException {
    // given: two load balancers and some events for multiple users
    SourceControlLoadBalancer balancerOne = createLoadBalancer();
    SourceControlLoadBalancer balancerTwo = createLoadBalancer();

    assertThat(balancerOne.reserveEvent(createEvent("user-1"))).isTrue();
    sleep(100); // so the partition reservations have different expiration times, which makes the validations
    // deterministic
    assertThat(balancerTwo.reserveEvent(createEvent("user-2"))).isTrue();
    sleep(100);
    assertThat(balancerOne.reserveEvent(createEvent("user-3"))).isTrue();
    sleep(100); // so the partition reservations have different expiration times, which makes the validations
    // deterministic
    assertThat(balancerTwo.reserveEvent(createEvent("user-4"))).isTrue();
    sleep(100);

    // when: a balancer goes offline
    balancerOne.stop();
    balancerTwo.rebalance(); // so we don't have to wait

    // then: balancer two can reserve events for all users
    assertThat(balancerTwo.reserveEvent(createEvent("user-1"))).isTrue();
    assertThat(balancerTwo.reserveEvent(createEvent("user-2"))).isTrue();
    assertThat(balancerTwo.reserveEvent(createEvent("user-3"))).isTrue();
    assertThat(balancerTwo.reserveEvent(createEvent("user-4"))).isTrue();
  }

  @Test
  public void testReleaseRelatedEvents_letLocksExpire() throws InterruptedException {
    // given: two load balancers, one with events reserved
    final int reservationSeconds = 2;
    SourceControlLoadBalancer balancerOne = createLoadBalancer();
    balancerOne.setPartitionReservationSeconds(reservationSeconds);
    SourceControlLoadBalancer balancerTwo = createLoadBalancer();
    balancerOne.rebalance();

    SourceControlEvent event1 = createEvent("user-1");
    SourceControlEvent event2 = createEvent("user-1");
    SourceControlEvent event3 = createEvent("user-1");
    assertThat(balancerOne.reserveEvent(event1)).isTrue();
    assertThat(balancerOne.reserveEvent(event2)).isTrue();
    assertThat(balancerOne.reserveEvent(event3)).isTrue();

    // when: balancer one releases events, but not the associate lock
    balancerOne.releaseRelatedEvents(event1, false);

    // then: balancer two cannot reserve the events
    assertThat(balancerTwo.reserveEvent(event1)).isFalse();
    assertThat(balancerTwo.reserveEvent(event2)).isFalse();
    assertThat(balancerTwo.reserveEvent(event3)).isFalse();

    // when: let the lock expire
    sleep(reservationSeconds * 1_000 + 100);
    balancerTwo.rebalance();

    // then: balancer two can reserve the events
    assertThat(balancerTwo.reserveEvent(event1)).isTrue();
    assertThat(balancerTwo.reserveEvent(event2)).isTrue();
    assertThat(balancerTwo.reserveEvent(event3)).isTrue();
  }

  @Test
  public void testReleaseRelatedEvents_alsoClearLocks() {
    // given: two load balancers, one with events reserved
    SourceControlLoadBalancer balancerOne = createLoadBalancer();
    SourceControlLoadBalancer balancerTwo = createLoadBalancer();
    balancerOne.rebalance();

    SourceControlEvent event1 = createEvent("user-1");
    SourceControlEvent event2 = createEvent("user-1");
    SourceControlEvent event3 = createEvent("user-1");
    assertThat(balancerOne.reserveEvent(event1)).isTrue();
    assertThat(balancerOne.reserveEvent(event2)).isTrue();
    assertThat(balancerOne.reserveEvent(event3)).isTrue();

    // when: balancer one releases events, but not the associate lock
    balancerOne.releaseRelatedEvents(event1, false);

    // then: balancer two cannot reserve the events
    assertThat(balancerTwo.reserveEvent(event1)).isFalse();
    assertThat(balancerTwo.reserveEvent(event2)).isFalse();
    assertThat(balancerTwo.reserveEvent(event3)).isFalse();

    // when: balancer one releases events, and also the associate lock
    balancerOne.releaseRelatedEvents(event1, true);
    balancerTwo.rebalance();

    // then: balancer two can reserve the events
    assertThat(balancerTwo.reserveEvent(event1)).isTrue();
    assertThat(balancerTwo.reserveEvent(event2)).isTrue();
    assertThat(balancerTwo.reserveEvent(event3)).isTrue();
  }

  @Test
  public void testGetEventsToProcess_twoInstancesSameUser() {
    // given: two load balancers and events for a user
    SourceControlLoadBalancer balancerOne = createLoadBalancer();
    SourceControlLoadBalancer balancerTwo = createLoadBalancer();
    balancerOne.rebalance();
    createEvent("user-1");
    createEvent("user-1");

    // then: balancerOne can get the events to process
    assertThat(balancerOne.acquireEventsToProcess()).hasSize(2);

    // then: the list is empty on subsequent request
    assertThat(balancerOne.acquireEventsToProcess()).isEmpty();

    // when: add another event for the same user
    createEvent("user-1");

    // then: balancer two cannot fetch it but balancer one can
    assertThat(balancerTwo.acquireEventsToProcess()).isEmpty();
    assertThat(balancerOne.acquireEventsToProcess()).hasSize(1);
  }

  @Test
  public void testGetEventsToProcess_twoInstancesDifferentUsers() {
    // given: two load balancers and events for different users
    SourceControlLoadBalancer balancerOne = createLoadBalancer();
    SourceControlLoadBalancer balancerTwo = createLoadBalancer();
    balancerOne.rebalance();
    createEvent("user-1");
    createEvent("user-1");
    createEvent("user-2");
    createEvent("user-2");
    createEvent("user-2");

    // when: get events to process for each balancer
    List<SourceControlEvent> balancerOneEvents = balancerOne.acquireEventsToProcess();
    List<SourceControlEvent> balancerTwoEvents = balancerTwo.acquireEventsToProcess();

    // then: each balancer got events
    assertThat(balancerOneEvents).isNotEmpty();
    assertThat(balancerTwoEvents).isNotEmpty();

    // and: each balancer had a distinct user
    Set<String> balancerOneUsers = balancerOneEvents.stream()
        .map(SourceControlEvent::getScmUsername)
        .collect(Collectors.toSet());
    Set<String> balancerTwoUsers = balancerTwoEvents.stream()
        .map(SourceControlEvent::getScmUsername)
        .collect(Collectors.toSet());
    assertThat(balancerOneUsers).hasSize(1);
    assertThat(balancerTwoUsers).hasSize(1);
    balancerOneUsers.retainAll(balancerTwoUsers);
    assertThat(balancerOneUsers).isEmpty();
  }

  @Test
  public void testGetEventsToProcess_newInstanceComesOnline() throws InterruptedException {
    // given: a load balancer and some events for multiple users
    SourceControlLoadBalancer balancerOne = createLoadBalancer();
    balancerOne.setPartitionReservationSeconds(3);
    createEvent("user-1");
    createEvent("user-2");

    // then:
    assertThat(balancerOne.acquireEventsToProcess()).hasSize(2);

    // given: another load balancer comes online and new events are available to process
    SourceControlLoadBalancer balancerTwo = createLoadBalancer();
    balancerOne.rebalance();
    createEvent("user-1");
    createEvent("user-2");

    // then: balancer one can pick up one of the events
    assertThat(balancerOne.acquireEventsToProcess()).hasSize(1);

    // but: balancer two can't pick up events until the locks expire
    assertThat(balancerTwo.acquireEventsToProcess()).isEmpty();

    // when: the locks expire
    sleep(3_500);
    balancerTwo.rebalance();

    // then: balancerTwo can pick up the remaining event
    assertThat(balancerTwo.acquireEventsToProcess()).hasSize(1);
  }

  @Test
  public void testAcquireEventsToProcess_maintenanceLockRunsAsGlobalTenant() {
    // given: a load balancer with a SPY on PerpetualLockManager so we can capture the raw thread-
    // local tenant at the moment the maintenance lock is requested. Running as a real test tenant
    // (via TenantTestHelper.testAsTenant) is important here -- if the thread is SINGLE_TENANT when
    // runAsGlobal is invoked, runAsGlobal is a no-op (see TenantThreadLocal.runAsWithoutValidation)
    // so the test couldn't distinguish "wrapped in runAsGlobal" from "not wrapped".
    // The maintenance lock MUST be acquired against the global schema because it coordinates
    // stale-event cleanup across all mtiq-batch instances and across tenants; without the
    // runAsGlobal wrapper the lock row would go into whatever tenant schema the caller happened
    // to be running under, breaking cross-tenant coordination and polluting per-tenant perpetual
    // lock tables.
    OperationalDataStore ods = databaseContainerRule.getOperationalDataStore();
    PerpetualLockManager spyPerpetualLockManager = spy(new PerpetualLockManager(new PerpetualLockDAO(ods)));
    HeartbeatPartitionManager heartbeatPartitionManager = new HeartbeatPartitionManager(spyPerpetualLockManager);
    SourceControlLoadBalancer loadBalancer = new SourceControlLoadBalancer(
        heartbeatPartitionManager,
        spyPerpetualLockManager,
        new SourceControlEventDAO(ods),
        mock(TenantUtil.class));
    loadBalancer.start();
    activeLoadBalancers.add(loadBalancer);

    // Capture the raw thread-local tenant at the moment tryAcquireLock is called. We must use
    // TenantTestHelper.assertTenantSet (which reads via getTenantWithoutValidation, package-
    // private to the tenancy package) rather than TenantThreadLocal.getTenant(); the validated
    // form collapses everything to SINGLE_TENANT in non-MT mode and would hide whether
    // runAsGlobal was actually invoked.
    AtomicReference<Throwable> capturedFailure = new AtomicReference<>();
    doAnswer(invocation -> {
      try {
        TenantTestHelper.assertTenantSet(Tenant.GLOBAL_TENANT);
      }
      catch (Throwable t) {
        capturedFailure.set(t);
      }
      return invocation.callRealMethod();
    }).when(spyPerpetualLockManager)
        .tryAcquireLock(
            eq(SourceControlLoadBalancer.SOURCE_CONTROL_EVENT_MAINTENANCE_LOCK),
            anyString(),
            anyString(),
            anyLong());

    TenantTestHelper.testAsNewTenant("maintenance-lock-test-tenant", tenant -> {
      // Sanity-check: at the entry point we are NOT running as global. Otherwise the test would
      // pass trivially whether or not tryGetMaintenanceLock wraps its call in runAsGlobal.
      TenantTestHelper.assertTenantSet(tenant);

      // when: acquire events to process, which internally triggers resetStaleEvents ->
      // tryGetMaintenanceLock
      loadBalancer.acquireEventsToProcess();
    });

    // then: the maintenance lock request was made while the thread-local tenant was global
    assertThat(capturedFailure.get())
        .as("tryGetMaintenanceLock should have been invoked with the global tenant active")
        .isNull();
  }

  @Test
  public void testGetEventsToProcess_instanceGoesOffline() {
    // given: two load balancers, each processing events for different users
    SourceControlLoadBalancer balancerOne = createLoadBalancer();
    SourceControlLoadBalancer balancerTwo = createLoadBalancer();
    balancerOne.rebalance();
    createEvent("user-1");
    createEvent("user-1");
    assertThat(balancerOne.acquireEventsToProcess()).hasSize(2);
    createEvent("user-2");
    assertThat(balancerTwo.acquireEventsToProcess()).hasSize(1);

    // when: one of the balancers goes offline and there are new events to process for both users
    balancerTwo.stop();
    createEvent("user-1");
    createEvent("user-2");

    // then: the other balancer will pick up both events
    assertThat(balancerOne.acquireEventsToProcess()).hasSize(2);
  }

  private SourceControlEvent createEvent(String scmUsername) {
    long now = System.currentTimeMillis();
    Application application = tempEntity.newApplicationWithParent();
    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(
        application.getId(),
        BuildStageType.ID,
        "sourceScan",
        new Date(now - 5000),
        "sourceCommit");
    return tempEntity.newSourceControlEvent(application, policyEvaluation, scmUsername);
  }

  private SourceControlLoadBalancer createLoadBalancer() {
    OperationalDataStore ods = databaseContainerRule.getOperationalDataStore();
    PerpetualLockManager perpetualLockManager = new PerpetualLockManager(new PerpetualLockDAO(ods));
    HeartbeatPartitionManager heartbeatPartitionManager = new HeartbeatPartitionManager(perpetualLockManager);
    SourceControlLoadBalancer loadBalancer = new SourceControlLoadBalancer(
        heartbeatPartitionManager,
        perpetualLockManager,
        new SourceControlEventDAO(ods),
        mock(TenantUtil.class));
    loadBalancer.start();
    try {
      sleep(1_000);
    }
    catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }

    activeLoadBalancers.add(loadBalancer);

    return loadBalancer;
  }
}
