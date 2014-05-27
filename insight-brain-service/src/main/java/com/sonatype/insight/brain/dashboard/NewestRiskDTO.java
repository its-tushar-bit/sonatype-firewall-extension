/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.dashboard;

import java.util.ArrayList;
import java.util.List;

/**
 * Carries the data backing the Newest Risk view.
 * 
 * @since 1.11.0
 */
public class NewestRiskDTO
{
  public String applicationPublicId;

  public String applicationName;

  public int threatLevel;

  public long time;

  public String policyId;

  public String policyName;

  public String hash;

  public GavDTO gav;

  public List<String> pathnames;

  public List<StageDetailDTO> stageDetails = new ArrayList<>();

  static class StageDetailDTO
  {
    public String stageTypeId;

    public long time;

    public String actionTypeId;

    public String scanId;
  }
}
