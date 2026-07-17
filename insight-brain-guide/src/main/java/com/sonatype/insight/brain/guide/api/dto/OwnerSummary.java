/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.guide.api.dto;

import java.util.List;

/**
 * Common contract for owner summaries returned by the owner-picker endpoints. Sealed to the
 * two concrete records {@link OrgSummary} and {@link AppSummary} so that
 * {@code /api/v2/policy-context/owners/{ownerId}} can return either without exposing the
 * shared type as an interface.
 */
public sealed interface OwnerSummary
    permits OrgSummary, AppSummary
{
  String id();

  String publicId();

  String name();

  String type();

  List<OwnerPathEntry> ancestorPath();
}
