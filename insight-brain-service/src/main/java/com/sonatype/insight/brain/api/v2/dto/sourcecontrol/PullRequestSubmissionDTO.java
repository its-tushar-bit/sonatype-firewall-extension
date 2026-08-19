/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto.sourcecontrol;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;

public record PullRequestSubmissionDTO(
    String applicationId,
    String scanId,
    ComponentIdentifier componentIdentifier,
    String targetVersion,
    String identificationSource,
    Boolean isDirectDependency)
{
}
