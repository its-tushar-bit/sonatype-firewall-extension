/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.configuration;

import java.util.Date;

import javax.inject.Inject;

import com.sonatype.insight.brain.dataaccess.configuration.AutomaticSourceControlConfigurationDAO;
import com.sonatype.insight.brain.hds.HdsClient;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

import com.google.inject.Binder;
import org.apache.http.HttpEntity;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;

import static com.sonatype.insight.brain.configuration.AutomaticApplicationsConfigurationServiceTest.assertTelemetryEvent;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class AutomaticSourceControlConfigurationServiceTest
    extends AbstractComponentTest
{
  @Inject
  private AutomaticSourceControlConfigurationService service;

  @Inject
  private AutomaticSourceControlConfigurationDAO automaticSourceControlConfigurationDAO;

  private HdsClient mockHdsClient = mock(HdsClient.class);

  @Override
  public void configure(Binder binder) {
    binder.bind(HdsClient.class).toInstance(mockHdsClient);
    super.configure(binder);
  }

  @Test
  public void testUpdate() {
    // make sure it is false first
    automaticSourceControlConfigurationDAO.setSourceControlConfigurationEnabled(false);

    AutomaticSourceControlConfiguration updated = service.update(new AutomaticSourceControlConfiguration(true));

    assertThat(updated.isEnabled()).isTrue();
    assertThat(automaticSourceControlConfigurationDAO.isSourceControlConfigurationEnabled()).isTrue();
  }

  @Test
  public void testUpdate_TelemetryEventsAreSent() throws Exception {
    final InvocationOnMock[] invocation = new InvocationOnMock[1];
    doAnswer(x -> invocation[0] = x).when(mockHdsClient).post(eq(TelemetrySender.RESOURCE_PATH), any(HttpEntity.class),
        eq(null));

    // make sure it is false first
    automaticSourceControlConfigurationDAO.setSourceControlConfigurationEnabled(false);

    // Enable Auto SCM - a telemetry event should be sent
    Date before = new Date();
    service.update(new AutomaticSourceControlConfiguration(true));
    Date after = new Date();
    assertTelemetryEvent(invocation[0], TelemetryPurpose.AUTOMATIC_ONBOARDING,
        AutomaticSourceControlConfigurationService.AUTO_SCM_CONFIGURATION_ENABLED_TELEMETRY_ATTR,
        before, after, true);
    clearInvocations(mockHdsClient);

    // No changes to Auto SCM state - a telemetry event should NOT be sent
    service.update(new AutomaticSourceControlConfiguration(true));
    verifyNoMoreInteractions(mockHdsClient);

    // Disable Auto SCM - a telemetry event should be sent
    before = new Date();
    service.update(new AutomaticSourceControlConfiguration(false));
    after = new Date();
    assertTelemetryEvent(invocation[0], TelemetryPurpose.AUTOMATIC_ONBOARDING,
        AutomaticSourceControlConfigurationService.AUTO_SCM_CONFIGURATION_ENABLED_TELEMETRY_ATTR,
        before, after, false);
    clearInvocations(mockHdsClient);

    // No changes to Auto SCM state - a telemetry event should NOT be sent
    service.update(new AutomaticSourceControlConfiguration(false));
    verifyNoMoreInteractions(mockHdsClient);
  }

  @Test
  public void testGet() {
    automaticSourceControlConfigurationDAO.setSourceControlConfigurationEnabled(true);

    AutomaticSourceControlConfiguration configuration = service.get();
    assertThat(configuration.isEnabled()).isTrue();
  }
}
