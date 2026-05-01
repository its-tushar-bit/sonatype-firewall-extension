/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.configuration.scanhealth;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * DTO for Scan Health configuration.
 * <p>
 * A null value for {@code failOnZeroComponents} means "inherit from parent".
 */
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ScanHealthConfigDTO(Boolean failOnZeroComponents)
{

  /**
   * Default constructor that creates an empty configuration (inherit from parent).
   */
  public ScanHealthConfigDTO() {
    this((Boolean) null);
  }
}
