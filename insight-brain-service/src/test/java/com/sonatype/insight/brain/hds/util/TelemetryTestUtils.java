/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.hds.util;

import com.sonatype.insight.brain.telemetry.ClusterIdentificationService;
import com.sonatype.insight.brain.telemetry.ClusterIdentificationService.IdResolutionResult;
import com.sonatype.insight.brain.telemetry.ClusterIdentificationService.ResolutionOutcome;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TelemetryTestUtils
{
  public static ClusterIdentificationService setupReflectiveMockClusterIdentificationService() {
    var mockClusterIdentificationService = mock(ClusterIdentificationService.class);
    when(mockClusterIdentificationService.resolveClusterIdentity(nullable(String.class), anyString()))
        .thenAnswer(invocation -> {
          String assignedClusterId = invocation.getArgument(0);
          String assignedTelemetryId = invocation.getArgument(1);
          return new IdResolutionResult(assignedClusterId, assignedTelemetryId, ResolutionOutcome.NO_CHANGE);
        });
    return mockClusterIdentificationService;
  }
}
