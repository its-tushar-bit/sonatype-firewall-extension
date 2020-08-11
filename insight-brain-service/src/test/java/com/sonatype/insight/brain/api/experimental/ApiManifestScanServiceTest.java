/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.experimental;

import java.util.List;

import javax.inject.Inject;

import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlEventDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.InsightConfig.Feature;

import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.*;

public class ApiManifestScanServiceTest
    extends AbstractComponentTest
{
  @Inject
  ApiManifestScanService apiManifestScanService;

  @Inject
  private InsightConfig config;

  @Before
  public void setup() {
    config.setExperimentalFeatures(ImmutableMap.of(Feature.MANIFEST_SCAN.getFlag(), Boolean.TRUE));
  }

  @Test
  public void testApiManifestScanService() {
    Application app = tempEntity.newApplicationWithParent();

    apiManifestScanService.performManifestScan(app.getId(), "stage");

    List<SourceControlEvent> sourceControlEvents = new SourceControlEventDAO().getAll();
    assertThat(sourceControlEvents.size()).isOne();
    SourceControlEvent sourceControlEvent = sourceControlEvents.get(0);
    assertThat(sourceControlEvent.getApplicationId()).isEqualTo(app.getId());
    assertThat(sourceControlEvent.getStageTypeId()).isEqualTo("stage");
    assertThat(sourceControlEvent.getEventType()).isEqualTo(SourceControlEvent.MANIFEST_SCAN_EVENT);
  }
}
