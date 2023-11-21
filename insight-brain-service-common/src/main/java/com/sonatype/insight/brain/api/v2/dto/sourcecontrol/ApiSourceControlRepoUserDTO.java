/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto.sourcecontrol;

import java.time.Instant;
import java.util.Collection;
import java.util.Map;

/**
 * @since 1.170
 */
public class ApiSourceControlRepoUserDTO
{
  public String publicId;

  public String repositoryUrl;

  public Map<String, Collection<Instant>> emailAndCommitDateMap;

  public ApiSourceControlRepoUserDTO(
          String publicId,
          String repositoryUrl,
          Map<String, Collection<Instant>> emailAndCommitDateMap)
  {
    this.publicId = publicId;
    this.repositoryUrl = repositoryUrl;
    this.emailAndCommitDateMap = emailAndCommitDateMap;
  }

  public ApiSourceControlRepoUserDTO() {
  }
}
