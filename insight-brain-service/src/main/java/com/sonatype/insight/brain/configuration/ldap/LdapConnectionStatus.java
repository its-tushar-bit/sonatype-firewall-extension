/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.configuration.ldap;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class LdapConnectionStatus
{
  public static enum Status
  {
    @SuppressWarnings("hiding")
    OK, FAILURE;
  }

  public static final LdapConnectionStatus OK = new LdapConnectionStatus(Status.OK, null);

  @JsonProperty
  private final Status status;

  @JsonProperty
  private final String message;

  @JsonCreator
  public LdapConnectionStatus(@JsonProperty("status") Status status, @JsonProperty("message") String message) {
    this.status = status;
    this.message = message;
  }

  public Status getStatus() {
    return status;
  }

  public String getMessage() {
    return message;
  }
}
