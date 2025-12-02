/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto;

import java.util.Date;

import com.sonatype.insight.json.store.ApiDateFormat;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_NULL)
public class ApiUserTokenDTO
{
  public String userCode;

  public String passCode;

  public String username;

  public String realm;

  @ApiDateFormat
  public Date createTime;

  @ApiDateFormat
  public Date lastAccessTime;

  @ApiDateFormat
  public Date expirationDate;
}
