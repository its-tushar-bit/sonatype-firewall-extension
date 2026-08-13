/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.relay;

import java.time.Duration;

import com.sonatype.insight.brain.dataaccess.relay.RelayEventLogDAO;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class RelayEventLogCleanupTaskTest
{
  @Mock
  private RelayEventLogDAO relayEventLogDAO;

  @Mock
  private RelayRegistrationService relayRegistrationService;

  private RelayEventLogCleanupTask task;

  @BeforeEach
  public void before() {
    task = new RelayEventLogCleanupTask(relayEventLogDAO, relayRegistrationService, Duration.ofDays(7));
  }

  @Test
  public void run_featureGateClosed_skipsCleanup() {
    when(relayRegistrationService.isFeatureGateOpen()).thenReturn(false);

    int deleted = task.run();

    assertThat(deleted).isZero();
    verify(relayEventLogDAO, never()).deleteOlderThan(any());
  }

  @Test
  public void run_featureGateOpen_invokesDaoWithRetention() {
    when(relayRegistrationService.isFeatureGateOpen()).thenReturn(true);
    when(relayEventLogDAO.deleteOlderThan(Duration.ofDays(7))).thenReturn(42);

    int deleted = task.run();

    assertThat(deleted).isEqualTo(42);
    verify(relayEventLogDAO).deleteOlderThan(Duration.ofDays(7));
  }

  @Test
  public void run_featureGateOpen_zeroDeleted_returnsZero() {
    when(relayRegistrationService.isFeatureGateOpen()).thenReturn(true);
    when(relayEventLogDAO.deleteOlderThan(any())).thenReturn(0);

    int deleted = task.run();

    assertThat(deleted).isZero();
  }

  @Test
  public void getJobName_isStable() {
    assertThat(task.getJobName()).isEqualTo("RelayEventLogCleanupTask");
  }
}
