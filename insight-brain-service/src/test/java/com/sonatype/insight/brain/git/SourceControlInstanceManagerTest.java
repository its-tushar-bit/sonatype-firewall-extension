/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import com.sonatype.insight.brain.dataaccess.ClusterLock;
import com.sonatype.insight.brain.db.OperationalDataStoreProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SourceControlInstanceManagerTest
{
  @Before
  public void before() {
    OperationalDataStoreProvider.init(null, false);
  }

  @After
  public void after() {
    ClusterLock.LOCKS_BY_ID.clear();
  }

  @Test
  public void testCanPoll() {
    // given: two source control instance managers
    SourceControlInstanceManager instanceManager1 = new SourceControlInstanceManager();
    SourceControlInstanceManager instanceManager2 = new SourceControlInstanceManager();

    // then: first one to ask can poll, the other cannot
    assertThat(instanceManager1.canPoll()).isTrue();
    assertThat(instanceManager2.canPoll()).isFalse();

    // when: release instance1 and try the reverse order
    instanceManager1.releaseInstance();

    // then: first one to ask can poll, the other cannot
    assertThat(instanceManager2.canPoll()).isTrue();
    assertThat(instanceManager1.canPoll()).isFalse();
  }

  @Test
  public void testCanProcessEvents() {
    // given: two source control instance managers
    SourceControlInstanceManager instanceManager1 = new SourceControlInstanceManager();
    SourceControlInstanceManager instanceManager2 = new SourceControlInstanceManager();

    // then: first one to ask can process events, the other cannot
    assertThat(instanceManager1.canProcessEvents()).isTrue();
    assertThat(instanceManager2.canProcessEvents()).isFalse();

    // when: release instance1 and try the reverse order
    instanceManager1.releaseInstance();

    // then: first one to ask can process events, the other cannot
    assertThat(instanceManager2.canProcessEvents()).isTrue();
    assertThat(instanceManager1.canProcessEvents()).isFalse();
  }

  @Test
  public void testPollAndProcessInteraction() {
    // given: two source control instance managers
    SourceControlInstanceManager instanceManager1 = new SourceControlInstanceManager();
    SourceControlInstanceManager instanceManager2 = new SourceControlInstanceManager();

    // then: first one to ask can poll AND process events, the other cannot
    assertThat(instanceManager1.canProcessEvents()).isTrue();
    assertThat(instanceManager2.canProcessEvents()).isFalse();
    assertThat(instanceManager1.canPoll()).isTrue();
    assertThat(instanceManager2.canPoll()).isFalse();

    // when: release instance1 and try the reverse order
    instanceManager1.releaseInstance();

    // then: first one to ask can process events, the other cannot
    assertThat(instanceManager2.canPoll()).isTrue();
    assertThat(instanceManager1.canPoll()).isFalse();
    assertThat(instanceManager2.canProcessEvents()).isTrue();
    assertThat(instanceManager1.canProcessEvents()).isFalse();
  }
}
