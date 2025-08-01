/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * @since 1.139
 */
public class ApiJiraConfigurationDTO
{
  public String url;

  public String username;

  @JsonInclude(Include.NON_NULL)
  @Schema(type = "string")
  public char[] password;

  public Map<String, Object> customFields;
}
