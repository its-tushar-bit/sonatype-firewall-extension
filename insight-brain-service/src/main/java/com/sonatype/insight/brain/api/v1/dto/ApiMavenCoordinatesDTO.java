/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v1.dto;

import com.sonatype.insight.brain.api.v2.dto.ApiComponentIdentifierDTOV2;

/**
 * @deprecated since 1.13.0, use {@link ApiComponentIdentifierDTOV2}
 */
@Deprecated
public class ApiMavenCoordinatesDTO
{
  public String groupId;

  public String artifactId;

  public String version;
}
