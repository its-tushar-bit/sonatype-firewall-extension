/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import javax.inject.Named;

import com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
public class ManifestScanService
{
  private static final Logger log = LoggerFactory.getLogger(ManifestScanService.class);

  public void onManifestScan(final SourceControlEvent event) {
    log.trace("Manifest scan executed for application '{}'", event.getApplicationId());
  }
}
