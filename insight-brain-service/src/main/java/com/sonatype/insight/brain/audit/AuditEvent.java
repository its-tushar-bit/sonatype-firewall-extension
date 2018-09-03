/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.audit;

/**
 * Enumeration of audited events, ensuring consistency when multiple code paths trigger the same event.
 */
public enum AuditEvent
{
  AUTHENTICATION_FAILURE(Domain.AUTHENTICATION, Type.FAILURE),

  LOGIN(Domain.AUTHENTICATION, Type.LOGIN), //
  LOGOUT(Domain.AUTHENTICATION, Type.LOGOUT), //

  DELETE_ORGANIZATION(Domain.ORGANIZATION, Type.DELETE), //

  CREATE_APPLICATION(Domain.APPLICATION, Type.CREATE), //
  EVALUATE_APPLICATION(Domain.APPLICATION, Type.EVALUATE), //
  MOVE_APPLICATION(Domain.APPLICATION, Type.MOVE), //
  DELETE_APPLICATION(Domain.APPLICATION, Type.DELETE), //

  // etc.
  ;

  private final String domain;

  private final String type;

  AuditEvent(String domain, String event) {
    this.domain = domain;
    this.type = event;
  }

  public String getDomain() {
    return domain;
  }

  public String getType() {
    return type;
  }

  private interface Domain
  {
    String AUTHENTICATION = "authentication";

    String ORGANIZATION = "organization";

    String APPLICATION = "application";
  }

  private interface Type
  {
    String FAILURE = "failure";

    String LOGIN = "login";

    String LOGOUT = "logout";

    String CREATE = "create";

    String UPDATE = "update";

    String DELETE = "delete";

    String MOVE = "move";

    String EVALUATE = "evaluate";
  }
}
