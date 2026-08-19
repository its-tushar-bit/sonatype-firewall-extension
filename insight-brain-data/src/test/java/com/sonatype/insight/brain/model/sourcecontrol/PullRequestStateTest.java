/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.sourcecontrol;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

public class PullRequestStateTest
{
  @Test
  public void testFromSCMState() {
    assertThat(PullRequestState.fromSCMState(null)).isNull();
    assertThat(PullRequestState.fromSCMState(com.sonatype.nexus.scm.api.model.PullRequestState.UNKNOWN)).isNull();
    assertThat(PullRequestState.fromSCMState(com.sonatype.nexus.scm.api.model.PullRequestState.CLOSED))
        .isEqualTo(PullRequestState.CLOSED);
    assertThat(PullRequestState.fromSCMState(com.sonatype.nexus.scm.api.model.PullRequestState.LOCKED))
        .isEqualTo(PullRequestState.LOCKED);
    assertThat(PullRequestState.fromSCMState(com.sonatype.nexus.scm.api.model.PullRequestState.MERGED))
        .isEqualTo(PullRequestState.MERGED);
    assertThat(PullRequestState.fromSCMState(com.sonatype.nexus.scm.api.model.PullRequestState.OPEN))
        .isEqualTo(PullRequestState.OPEN);
    assertThatNoException().isThrownBy(() -> Arrays.stream(com.sonatype.nexus.scm.api.model.PullRequestState.values())
        .map(PullRequestState::fromSCMState)
        .toList());
  }
}
