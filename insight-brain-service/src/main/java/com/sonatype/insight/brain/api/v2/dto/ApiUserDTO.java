/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

public class ApiUserDTO
{
  public String username;

  @JsonInclude(Include.NON_NULL)
  public String password;

  public String firstName;

  public String lastName;

  public String email;

  @JsonInclude(Include.NON_NULL)
  public String realm;
}
