/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.guide.api.dto;

import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.sonatype.guide.api.dto.ComponentDetailDocument;
import com.sonatype.guide.api.dto.DtsDimensions;
import com.sonatype.insight.brain.guide.api.dto.policy.GuidePolicyCompliance;

/**
 * Detailed component document with all fields from HDS search-server.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record GuideComponentDetailDocument(
    String format,
    String originId,
    String namespace,
    String name,
    String version,
    String registryLink,
    @JsonDeserialize(contentAs = GuideComponentArtifact.class) List<GuideComponentArtifact> components,
    @JsonDeserialize(contentAs = GuideComponentLicense.class) List<GuideComponentLicense> licenses,
    List<String> categories,
    Boolean latestStable,
    Integer versionScore,
    Double maxCvss,
    @JsonDeserialize(using = Iso8601InstantDeserializer.class) @JsonSerialize(
        using = Iso8601InstantSerializer.class) Instant publishedDate,
    List<String> directDependencies,
    Boolean isMalware,
    @JsonProperty("dts") @JsonDeserialize(as = GuideDtsDimensions.class) DtsDimensions dtsDimensions,
    GuidePolicyCompliance policyCompliance)
    implements ComponentDetailDocument
{
}
