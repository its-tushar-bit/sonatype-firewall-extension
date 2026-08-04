/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.components;

import java.util.ArrayList;
import java.util.List;

/**
 * One application that contains the requested component hash.
 */
public class ComponentUsageApplicationRowDTO
{
  public String applicationId;

  public String applicationPublicId;

  public String applicationName;

  public String organizationId;

  public String organizationName;

  /** Stage type ids where the component appears (e.g. build, release). */
  public List<String> stageTypeIds = new ArrayList<>();

  /** Latest owner_component.time for this application + hash, epoch millis. */
  public Long lastSeenTime;
}
