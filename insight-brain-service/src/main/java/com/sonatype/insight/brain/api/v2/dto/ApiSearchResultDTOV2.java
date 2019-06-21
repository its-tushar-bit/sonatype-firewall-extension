/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto;

/**
 * Describes a single component within an application that matches the search criteria.
 *
 * @since 1.13.0
 */
public class ApiSearchResultDTOV2
{
  public String applicationId;

  public String applicationName;

  public String reportUrl;

  public String hash;

  public ApiComponentIdentifierDTOV2 componentIdentifier;

  public String packageUrl;

  public Integer threatLevel;
}
