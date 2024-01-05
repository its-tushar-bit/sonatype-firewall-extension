/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import javax.inject.Inject;

import com.sonatype.insight.brain.dataaccess.PerpetualLockDAO;
import com.sonatype.insight.brain.testing.BrainInjectedTest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SourceControlInstanceManagerTest
    extends BrainInjectedTest
{
  @Inject
  private PerpetualLockDAO perpetualLockDAO;

  // test subject 1
  SourceControlInstanceManager instanceManager1;

  // test subject 2
  SourceControlInstanceManager instanceManager2;

  @Before
  public void before() {
    instanceManager1 = new SourceControlInstanceManager(perpetualLockDAO)
        .setInstanceLockCacheExpirationForTesting(1);
    instanceManager2 = new SourceControlInstanceManager(perpetualLockDAO)
        .setInstanceLockCacheExpirationForTesting(1);
  }

  @After
  public void after() {
    instanceManager2.releaseInstance();
    instanceManager1.releaseInstance();
  }

  @Test
  public void testCanPoll() throws InterruptedException {
    // given: two source control instance managers

    // then: first one to ask can poll, the other cannot
    assertThat(instanceManager1.canPoll()).isTrue();
    assertThat(instanceManager2.canPoll()).isFalse();

    // when: release instance1 and try the reverse order
    instanceManager1.releaseInstance();
    Thread.sleep(1_100);

    // then: first one to ask can poll, the other cannot
    assertThat(instanceManager2.canPoll()).isTrue();
    assertThat(instanceManager1.canPoll()).isFalse();
  }

  @Test
  public void testCanProcessEvents() throws InterruptedException {
    // given: two source control instance managers

    // then: polling must get the lock
    assertThat(instanceManager1.canPoll()).isTrue();
    assertThat(instanceManager1.canProcessEvents()).isTrue();
    assertThat(instanceManager2.canProcessEvents()).isFalse();

    // when: release instance1 and try the reverse order
    instanceManager1.releaseInstance();
    Thread.sleep(1_100);

    // then: polling gets the lock and event processing can use it
    assertThat(instanceManager2.canPoll()).isTrue();
    assertThat(instanceManager2.canProcessEvents()).isTrue();
    assertThat(instanceManager1.canProcessEvents()).isFalse();
  }

  @Test
  public void testPollAndProcessInteraction() throws InterruptedException {
    // given: two source control instance managers

    // then: first one to ask can poll AND process events, the other cannot
    assertThat(instanceManager1.canPoll()).isTrue();
    assertThat(instanceManager1.canProcessEvents()).isTrue();
    assertThat(instanceManager2.canProcessEvents()).isFalse();
    assertThat(instanceManager2.canPoll()).isFalse();

    // when: release instance1 and try the reverse order
    instanceManager1.releaseInstance();
    Thread.sleep(1_100);

    // then: first one to ask can process events, the other cannot
    assertThat(instanceManager2.canPoll()).isTrue();
    assertThat(instanceManager1.canPoll()).isFalse();
    assertThat(instanceManager2.canProcessEvents()).isTrue();
    assertThat(instanceManager1.canProcessEvents()).isFalse();
  }
}
