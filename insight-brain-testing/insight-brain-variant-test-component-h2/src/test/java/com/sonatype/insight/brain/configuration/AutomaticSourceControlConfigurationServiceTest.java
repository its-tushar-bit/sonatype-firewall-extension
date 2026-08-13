/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.sonatype.insight.brain.dataaccess.configuration.AutomaticSourceControlConfigurationDAO;
import com.sonatype.insight.brain.variant.AbstractComponentH2Test;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.brain.variant.ComponentH2Test;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;
import jakarta.inject.Inject;
import java.util.Date;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;

@ComponentH2Test
public class AutomaticSourceControlConfigurationServiceTest
    extends AbstractComponentH2Test
{
  @Inject
  private AutomaticSourceControlConfigurationService service;

  @Inject
  private AutomaticSourceControlConfigurationDAO automaticSourceControlConfigurationDAO;

  @Mock
  private TelemetrySender telemetrySenderMock;

  @Test
  public void testUpdate() {
    // make sure it is false first
    automaticSourceControlConfigurationDAO.setSourceControlConfigurationEnabled(false);

    AutomaticSourceControlConfiguration updated = service.update(new AutomaticSourceControlConfiguration(true));

    assertThat(updated.isEnabled()).isTrue();
    assertThat(automaticSourceControlConfigurationDAO.isSourceControlConfigurationEnabled()).isTrue();
  }

  @Test
  public void testUpdate_TelemetryEventsAreSent() {
    final InvocationOnMock[] invocation = new InvocationOnMock[1];
    doAnswer(x -> invocation[0] = x).when(telemetrySenderMock).send(any(TelemetryData.class));

    // make sure it is false first
    automaticSourceControlConfigurationDAO.setSourceControlConfigurationEnabled(false);

    // Enable Auto SCM - a telemetry event should be sent
    Date before = new Date();
    service.update(new AutomaticSourceControlConfiguration(true));
    Date after = new Date();
    assertTelemetryEvent(invocation[0], TelemetryPurpose.AUTOMATIC_ONBOARDING,
        AutomaticSourceControlConfigurationService.AUTO_SCM_CONFIGURATION_ENABLED_TELEMETRY_ATTR,
        before, after, true);
    clearInvocations(telemetrySenderMock);

    // No changes to Auto SCM state - a telemetry event should NOT be sent
    service.update(new AutomaticSourceControlConfiguration(true));
    verifyNoMoreInteractions(telemetrySenderMock);

    // Disable Auto SCM - a telemetry event should be sent
    before = new Date();
    service.update(new AutomaticSourceControlConfiguration(false));
    after = new Date();
    assertTelemetryEvent(invocation[0], TelemetryPurpose.AUTOMATIC_ONBOARDING,
        AutomaticSourceControlConfigurationService.AUTO_SCM_CONFIGURATION_ENABLED_TELEMETRY_ATTR,
        before, after, false);
    clearInvocations(telemetrySenderMock);

    // No changes to Auto SCM state - a telemetry event should NOT be sent
    service.update(new AutomaticSourceControlConfiguration(false));
    verifyNoMoreInteractions(telemetrySenderMock);
  }

  private void assertTelemetryEvent(
      InvocationOnMock invocation,
      TelemetryPurpose telemetryPurpose,
      String telemetryAttr,
      Date before,
      Date after,
      boolean expected)
  {
    TelemetryData telemetryData = (TelemetryData) invocation.getArgument(0);
    assertThat(telemetryData.getPurpose()).isEqualTo(telemetryPurpose);
    assertThat(telemetryData.getAttributes()).hasSize(1).containsEntry(telemetryAttr, String.valueOf(expected));
    assertThat(telemetryData.getTimestamp()).isGreaterThanOrEqualTo(before.getTime())
        .isLessThanOrEqualTo(after.getTime());
  }

  @Test
  public void testGet() {
    automaticSourceControlConfigurationDAO.setSourceControlConfigurationEnabled(true);

    AutomaticSourceControlConfiguration configuration = service.get();
    assertThat(configuration.isEnabled()).isTrue();
  }
}
