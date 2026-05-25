/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto.firewall;

import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Request DTO for renewing waivers.
 *
 * @since 1.186
 */
public class RenewWaiversRequestDTO
{
  @JsonProperty
  public List<String> waiverIds;

  @JsonProperty
  public Date newExpiryTime;

  @JsonProperty
  public String comment;

  @JsonProperty
  public String reasonId;
}
