/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.index;

/**
 * Budget-exempt index filter applied after parse + RBAC (CLM-44783). Multiple restrictions on a
 * read are ANDed; an {@link IndexOrTermSetGroup} is one restriction whose alternatives are ORed.
 */
public sealed interface IndexFilterRestriction
    permits IndexTermSetRestriction, IndexOrTermSetGroup
{
}
