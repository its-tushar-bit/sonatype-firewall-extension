/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.relay;

import com.sonatype.insight.brain.dataaccess.githubapp.GitHubAppDAO;
import com.sonatype.insight.brain.model.githubapp.RelayLinkState;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class RelayLinkRetrySweepTaskTest
{
  @Mock
  private GitHubAppDAO gitHubAppDAO;

  @Mock
  private RelayRegistrationService relayRegistrationService;

  private RelayLinkRetrySweepTask task;

  @Before
  public void before() {
    task = new RelayLinkRetrySweepTask(gitHubAppDAO, relayRegistrationService);
  }

  @Test
  public void run_featureGateClosed_skipsSweep() {
    when(relayRegistrationService.isFeatureGateOpen()).thenReturn(false);

    int promoted = task.run();

    assertThat(promoted).isZero();
    verify(gitHubAppDAO, never()).updateRelayLinkStateBulk(any(), any());
  }

  @Test
  public void run_featureGateOpen_promotesFailedToErrorAndResetsAttempts() {
    when(relayRegistrationService.isFeatureGateOpen()).thenReturn(true);
    when(gitHubAppDAO.updateRelayLinkStateBulk(RelayLinkState.FAILED, RelayLinkState.ERROR)).thenReturn(3);

    int promoted = task.run();

    assertThat(promoted).isEqualTo(3);
    verify(gitHubAppDAO).updateRelayLinkStateBulk(RelayLinkState.FAILED, RelayLinkState.ERROR);
  }

  @Test
  public void run_featureGateOpen_zeroFailedRows_returnsZero() {
    when(relayRegistrationService.isFeatureGateOpen()).thenReturn(true);
    when(gitHubAppDAO.updateRelayLinkStateBulk(RelayLinkState.FAILED, RelayLinkState.ERROR)).thenReturn(0);

    int promoted = task.run();

    assertThat(promoted).isZero();
    verify(gitHubAppDAO).updateRelayLinkStateBulk(RelayLinkState.FAILED, RelayLinkState.ERROR);
  }

  @Test
  public void getJobName_isStable() {
    assertThat(task.getJobName()).isEqualTo("RelayLinkRetrySweepTask");
  }
}
