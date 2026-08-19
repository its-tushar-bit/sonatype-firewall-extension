/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.guide.api.dto.policy;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.sonatype.insight.brain.guide.api.dto.Iso8601InstantDeserializer;
import com.sonatype.insight.brain.guide.api.dto.Iso8601InstantSerializer;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record GuideWaiverInfo(
    String scopeOwnerType,
    String scopeOwnerId,
    @JsonDeserialize(using = Iso8601InstantDeserializer.class) @JsonSerialize(
        using = Iso8601InstantSerializer.class) Instant expiryTime,
    String comment)
{
}
