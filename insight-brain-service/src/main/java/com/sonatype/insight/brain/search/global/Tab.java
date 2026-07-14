/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.global;

/**
 * Tabs supported by Global Search. {@link #ALL} packs rows from every other tab in fixed order.
 *
 * <p>
 * {@link #VIOLATION} maps to BOTH the {@code POLICY_VIOLATION} and {@code LEGAL_VIOLATION} index
 * item types unioned into one query; the item types stay distinct in the index but share a wire
 * shape. Organizations and policies are not tabs — they remain queryable via
 * {@code organizationName:} / {@code policyId:} filter chips in the {@code q=} string.
 */
public enum Tab
{
  ALL,
  APPLICATION,
  COMPONENT,
  VULNERABILITY,
  VIOLATION,
  WAIVER;
}
