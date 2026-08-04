/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.components;

/**
 * Request body for Nexus One component where-used APIs (CLM-43959).
 * <p>
 * {@code page} is 0-based.
 */
public class ComponentUsageRequestDTO
{
  /** Exact component hash (required). */
  public String componentHash;

  /** 0-based page index. */
  public Integer page;

  /** Page size. */
  public Integer pageSize;
}
