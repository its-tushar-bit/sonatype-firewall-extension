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
  LOGIN(Domain.AUTHENTICATION, Type.LOGIN),
  LOGOUT(Domain.AUTHENTICATION, Type.LOGOUT),

  UPDATE_COMPONENT_VULNERABILITY(Domain.GOVERNANCE_COMPONENT_VULNERABILITY, Type.UPDATE),
  UPDATE_COMPONENT_LICENSE(Domain.GOVERNANCE_COMPONENT_LICENSE, Type.UPDATE),
  SET_COMPONENT_IDENTITY(Domain.GOVERNANCE_COMPONENT_IDENTITY, Type.SET),
  UNSET_COMPONENT_IDENTITY(Domain.GOVERNANCE_COMPONENT_IDENTITY, Type.UNSET),
  ASSIGN_COMPONENT_LABEL(Domain.GOVERNANCE_COMPONENT_LABEL, Type.ASSIGN),

  EVALUATE_APPLICATION(Domain.GOVERNANCE_EVALUATION_APPLICATION, Type.EVALUATE);

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

  interface Domain
  {
    String AUTHENTICATION = "authentication";

    String GOVERNANCE = "governance";

    String GOVERNANCE_COMPONENT = join(GOVERNANCE, "component");

    String GOVERNANCE_COMPONENT_VULNERABILITY = join(GOVERNANCE_COMPONENT, "vulnerability");

    String GOVERNANCE_COMPONENT_IDENTITY = join(GOVERNANCE_COMPONENT, "identity");

    String GOVERNANCE_COMPONENT_LABEL = join(GOVERNANCE_COMPONENT, "label");

    String GOVERNANCE_EVALUATION = join(GOVERNANCE, "evaluation");

    String GOVERNANCE_EVALUATION_APPLICATION = join(GOVERNANCE_EVALUATION, "application");

    String GOVERNANCE_COMPONENT_LICENSE = join(GOVERNANCE_COMPONENT, "license");

    static String join(String parent, String child) {
      return parent + "." + child;
    }
  }

  private interface Type
  {
    String FAILURE = "failure";

    String LOGIN = "login";

    String LOGOUT = "logout";

    String EVALUATE = "evaluate";

    String UPDATE = "update";

    String SET = "set";

    String UNSET = "unset";

    String ASSIGN = "assign";
  }
}
