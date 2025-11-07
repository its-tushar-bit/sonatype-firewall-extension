/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto;

import com.sonatype.insight.brain.model.security.MemberType;

/**
 * @since 1.197.0
 */
public record ApiMemberWithDetailsDTO(
    MemberType type,
    String internalName,
    String displayName,
    String email,
    String realm)
{
}
