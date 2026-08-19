/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto.legal;

import com.sonatype.insight.brain.model.OwnerType;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

public class ApiArtifactoryConnectionDTO
{
  public String artifactoryConnectionId;

  public OwnerType ownerType;

  public String ownerId;

  public Boolean isAnonymous;

  public String baseUrl;

  public String username;

  @JsonInclude(Include.NON_NULL)
  public String password;
}
