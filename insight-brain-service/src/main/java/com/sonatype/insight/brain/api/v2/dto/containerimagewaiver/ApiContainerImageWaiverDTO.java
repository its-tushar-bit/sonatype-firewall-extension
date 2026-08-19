/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto.containerimagewaiver;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.util.Date;

import com.sonatype.insight.json.store.ISODateSerializer;

public class ApiContainerImageWaiverDTO
{
  @JsonSerialize(using = ISODateSerializer.class)
  public Date expiryTime;

  public String waiverReasonId;

  public String comment;
}
