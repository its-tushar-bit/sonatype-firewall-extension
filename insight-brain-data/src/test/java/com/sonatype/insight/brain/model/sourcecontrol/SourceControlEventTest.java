/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.sourcecontrol;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.sonatype.insight.json.store.JsonUtils;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SourceControlEventTest
{
  @Test
  public void testSetScanTargetsJson() {
    SourceControlEvent sourceControlEvent = new SourceControlEvent();

    List<String> scanTargets = Arrays.asList("scanTarget1", "scanTarget2");
    String scanTargetsJson = JsonUtils.writeUnformatted(scanTargets);
    sourceControlEvent.setScanTargetsJson(scanTargetsJson);
    assertThat(sourceControlEvent.getScanTargetsJson()).isEqualTo(scanTargetsJson);
    assertThat(sourceControlEvent.getScanTargets()).isEqualTo(scanTargets);
  }

  @Test
  public void testSetScanTargetsJson_Null() {
    SourceControlEvent sourceControlEvent = new SourceControlEvent();

    sourceControlEvent.setScanTargetsJson(null);

    assertThat(sourceControlEvent.getScanTargetsJson()).isNull();
    assertThat(sourceControlEvent.getScanTargets()).isNull();
  }

  @Test
  public void testSetScanTargetsJson_Empty() {
    SourceControlEvent sourceControlEvent = new SourceControlEvent();

    sourceControlEvent.setScanTargetsJson(" ");

    assertThat(sourceControlEvent.getScanTargetsJson()).isNull();
    assertThat(sourceControlEvent.getScanTargets()).isNull();
  }

  @Test
  public void testSetScanTargets() {
    SourceControlEvent sourceControlEvent = new SourceControlEvent();

    List<String> scanTargets = Arrays.asList("scanTarget1", "scanTarget2");
    String scanTargetsJson = JsonUtils.writeUnformatted(scanTargets);
    sourceControlEvent.setScanTargets(scanTargets);
    assertThat(sourceControlEvent.getScanTargetsJson()).isEqualTo(scanTargetsJson)
        .doesNotContain("\n", "\r", "\\n",
            "\\r");
    assertThat(sourceControlEvent.getScanTargets()).isEqualTo(scanTargets);
  }

  @Test
  public void testSetScanTargets_Null() {
    SourceControlEvent sourceControlEvent = new SourceControlEvent();

    sourceControlEvent.setScanTargets(null);

    assertThat(sourceControlEvent.getScanTargetsJson()).isNull();
    assertThat(sourceControlEvent.getScanTargets()).isNull();
  }

  @Test
  public void testSetScanTargets_Empty() {
    SourceControlEvent sourceControlEvent = new SourceControlEvent();

    sourceControlEvent.setScanTargets(Collections.emptyList());

    assertThat(sourceControlEvent.getScanTargetsJson()).isNull();
    assertThat(sourceControlEvent.getScanTargets()).isNull();
  }

  @Test
  public void copyAsNew_propagatesFailureClassification() {
    SourceControlEvent original = new SourceControlEvent()
        .setApplicationId("app-1")
        .setEventType(SourceControlEvent.REMEDIATION_PULL_REQUEST_EVENT)
        .setEventFailureCategory("MANIFEST_COMPONENT_NOT_FOUND")
        .setEventIsRetryable(false);
    original.setEventStatus(SourceControlEvent.EVENT_STATUS_ERROR);

    SourceControlEvent copy = original.copyAsNew();

    assertThat(copy.getEventStatus()).isEqualTo(SourceControlEvent.EVENT_STATUS_NEW);
    assertThat(copy.getEventFailureCategory()).isEqualTo("MANIFEST_COMPONENT_NOT_FOUND");
    assertThat(copy.getEventIsRetryable()).isFalse();
    assertThat(copy.getApplicationId()).isEqualTo("app-1");
  }

  @Test
  public void copyAsNew_nullClassificationStaysNull() {
    SourceControlEvent original = new SourceControlEvent()
        .setApplicationId("app-1")
        .setEventType(SourceControlEvent.REMEDIATION_PULL_REQUEST_EVENT);
    original.setEventStatus(SourceControlEvent.EVENT_STATUS_ERROR);

    SourceControlEvent copy = original.copyAsNew();

    assertThat(copy.getEventFailureCategory()).isNull();
    assertThat(copy.getEventIsRetryable()).isNull();
  }
}
