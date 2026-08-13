/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.guide.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.sonatype.guide.api.dto.AffectedAsset;

/** DTO representing an affected asset for a component version. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record GuideAffectedAsset(String extension, String classifier, Boolean isPrimary)
    implements AffectedAsset
{
}
