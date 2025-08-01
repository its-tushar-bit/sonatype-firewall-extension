/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * @since 1.83
 */
public class ApiMailConfigurationDTO
{
  public String hostname;

  public int port;

  public String username;

  @Schema(type = "string")
  public char[] password;

  public boolean passwordIsIncluded;

  public boolean sslEnabled;

  public boolean startTlsEnabled;

  public String systemEmail;
}
