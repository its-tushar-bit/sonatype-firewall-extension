/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto;

import com.sonatype.insight.brain.model.sourcecontrol.SourceControlProvider;

public class ApiSourceControlDTO
{
  public String id;

  public String applicationId;

  public String ownerId;

  public String repositoryUrl;

  public String token;

  public SourceControlProvider provider;
}
