/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto;

/**
 * Describes the search criteria that was processed.
 *
 * @since 1.13.0
 */
public class ApiSearchCriteriaDTOV2
{
  public String stageId;

  public String hash;

  public ApiComponentIdentifierDTOV2 componentIdentifier;

  public String packageUrl;
}
