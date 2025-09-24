/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * DTO representing a component's progress status within a cascade re-evaluation.
 *
 * @since 1.196
 */
@JsonInclude(Include.NON_NULL)
public class CascadeComponentProgressDTO
{
  /**
   * Repository manager identifier.
   */
  public String repositoryManagerId;

  /**
   * Repository identifier.
   */
  public String repositoryId;

  /**
   * Repository component identifier.
   */
  public String componentId;

  /**
   * Whether the component is quarantined.
   */
  public Boolean quarantined;
}
