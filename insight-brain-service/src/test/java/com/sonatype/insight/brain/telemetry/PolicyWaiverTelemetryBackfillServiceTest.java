/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import static com.sonatype.insight.brain.telemetry.PolicyWaiverTelemetryBackfillService.WAIVER_REASON_MAPPING;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO.WaiverReasonData;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverReasonDAO;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.PolicyWaiverReason;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.telemetry.model.TelemetryData;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

public class PolicyWaiverTelemetryBackfillServiceTest
    extends AbstractComponentTest
{
  // we need to be able to do a reverse lookup on the waiver reason id to get the reason text
  private static Map<String, PolicyWaiverReason> waiverReasonMap;

  @Mock
  private TelemetrySender mockTelemetrySender;

  @Inject
  private PolicyWaiverReasonDAO policyWaiverReasonDAO;

  @Inject
  private PolicyWaiverTelemetryBackfillService testSubject;

  @Before
  public void loadWaiverReasons() {
    if (null == waiverReasonMap) {
      waiverReasonMap = policyWaiverReasonDAO.getAll()
          .stream()
          .collect(Collectors.toMap(PolicyWaiverReason::getId, reason -> reason));
    }
  }

  @Test
  public void testCollectAndSendPolicyWaiverBackfillTelemetry() {
    // given: a set of policy waivers - one for each reason
    final var waivers = createWaiversWithReasons(waiverReasonMap.values());
    final var recordsPerBatch = 2;
    final var expectedTelemetryEventCount = (int) Math.ceil((double) waiverReasonMap.size() / recordsPerBatch);
    assertThat(expectedTelemetryEventCount).isPositive();
    testSubject.recordsPerBatch = recordsPerBatch;

    // determine expected waiver reasons with proper ordering
    final var expectedWaiverReasons = getExpectedWaiverReasons(waivers);

    // when: do the backfill
    final long recordsSent = testSubject.collectAndSendPolicyWaiverBackfillTelemetry();

    // then: we sent the correct number of telemetry events with the expected data in them
    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<TelemetryData>> telemetryDataArgumentCaptor = ArgumentCaptor.forClass(List.class);

    assertThat(recordsSent).isEqualTo(expectedTelemetryEventCount);
    verify(mockTelemetrySender, times(expectedTelemetryEventCount)).send(telemetryDataArgumentCaptor.capture());
    var allCapturedTelemetryData = telemetryDataArgumentCaptor.getAllValues();

    // Check each captured TelemetryData against the expected waiver reasons
    for (int i = 0; i < allCapturedTelemetryData.size(); i++) {
      List<TelemetryData> capturedTelemetryData = allCapturedTelemetryData.get(i);
      @SuppressWarnings("unchecked")
      var capturedWaiverReasons = (List<WaiverReasonData>) capturedTelemetryData.get(0)
          .getAttributes()
          .get(WAIVER_REASON_MAPPING);

      // since we've configured the test subject to send 2 records per batch we need to peel off 2 records
      // at a time from the expected reasons for the validation
      int startIndex = i * testSubject.recordsPerBatch;
      int endIndex = Math.min((i + 1) * testSubject.recordsPerBatch, expectedWaiverReasons.size());
      List<WaiverReasonData> expectedReasons = expectedWaiverReasons.subList(startIndex, endIndex);
      assertThat(capturedWaiverReasons).isEqualTo(expectedReasons);
    }

    // when: try to process the backfill again
    final long retryCount = testSubject.collectAndSendPolicyWaiverBackfillTelemetry();

    // then: no telemetry is sent since the backfill is already processed
    assertThat(retryCount).isZero();
    verifyNoMoreInteractions(mockTelemetrySender);
  }

  private List<PolicyWaiver> createWaiversWithReasons(Collection<PolicyWaiverReason> reasons) {
    final var org = tempEntity.newOrganization();
    final var app = tempEntity.newApplication(org.getId());
    final var waivers = new ArrayList<PolicyWaiver>();

    for (var reason : reasons) {
      var policy = tempEntity.newPolicy(org);
      var waiver = new PolicyWaiver();
      waiver.setId(reason.getReasonText());
      waiver.setOwnerId(app.getId());
      waiver.setPolicyId(policy.getId());
      waiver.setWaiverReasonId(reason.getId());
      waivers.add(tempEntity.newWaiver(waiver));
    }

    return waivers;
  }

  private List<WaiverReasonData> getExpectedWaiverReasons(List<PolicyWaiver> waivers) {
    return waivers.stream()
        .map(waiver -> new WaiverReasonData(
            waiver.getId(),
            waiverReasonMap.get(waiver.getWaiverReasonId()).getReasonText()))
        .sorted(Comparator.comparing(WaiverReasonData::policyWaiverId))
        .toList();
  }
}
