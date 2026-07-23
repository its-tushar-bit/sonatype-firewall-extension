/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.global;

/**
 * Public-facing entity types in the Global Search suggest response.
 *
 * <p>
 * Distinct from the internal {@link com.sonatype.insight.brain.search.index.ItemType} enum: the API
 * exposes {@code COMPONENT} and {@code VULNERABILITY} as user-facing taxonomy, where
 * {@code COMPONENT} maps internally to either catalog components or IQ-local
 * {@code NON_VULNERABLE_COMPONENT} rows, and {@code VULNERABILITY} to catalog vulnerabilities or
 * IQ-local {@code SECURITY_VULNERABILITY} rows. {@code VIOLATION} is a merged surface for both
 * {@code POLICY_VIOLATION} and {@code LEGAL_VIOLATION} internal item types.
 *
 * <p>
 * The enum order matches the fixed presentation order of the suggest groups: Vulnerability,
 * Component, Application, Violation, Waiver. Organizations and policies are not surfaced as groups;
 * they remain queryable via filter chips (see the AST parser + field map).
 */
public enum SuggestItemType
{
  VULNERABILITY,
  COMPONENT,
  APPLICATION,
  VIOLATION,
  WAIVER
}
