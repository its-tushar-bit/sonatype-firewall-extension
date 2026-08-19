/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.guide.api.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.sonatype.guide.api.dto.MethodSignature;
import com.sonatype.guide.api.dto.RecommendationVulnerableMethod;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record GuideRecommendationVulnerableMethod(
    String refid,
    @JsonDeserialize(contentAs = GuideMethodSignature.class) List<? extends MethodSignature> methodSignatures)
    implements RecommendationVulnerableMethod
{
}
