/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard;

/**
 * @since 1.24.0
 */
public class DashboardFilterErrorResponseDTO
{
  public String name;

  public String errorMessage;

  public int status;

  public DashboardFilterErrorResponseDTO() {
  }

  public DashboardFilterErrorResponseDTO(final String name, final String errorMessage, final int status) {
    this.name = name;
    this.errorMessage = errorMessage;
    this.status = status;
  }
}
