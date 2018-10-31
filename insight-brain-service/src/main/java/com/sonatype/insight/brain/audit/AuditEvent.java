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
  REMOVE_COMPONENT_LABEL(Domain.GOVERNANCE_COMPONENT_LABEL, Type.REMOVE),

  CREATE_APPLICATION_CATEGORY(Domain.GOVERNANCE_APPLICATION_CATEGORY, Type.CREATE),
  UPDATE_APPLICATION_CATEGORY(Domain.GOVERNANCE_APPLICATION_CATEGORY, Type.UPDATE),
  DELETE_APPLICATION_CATEGORY(Domain.GOVERNANCE_APPLICATION_CATEGORY, Type.DELETE),
  IMPORT_APPLICATION_CATEGORY(Domain.GOVERNANCE_APPLICATION_CATEGORY, Type.IMPORT),

  CREATE_WAIVER(Domain.GOVERNANCE_WAIVER, Type.CREATE),
  DELETE_WAIVER(Domain.GOVERNANCE_WAIVER, Type.DELETE),

  CREATE_LABEL(Domain.GOVERNANCE_LABEL, Type.CREATE),
  UPDATE_LABEL(Domain.GOVERNANCE_LABEL, Type.UPDATE),
  DELETE_LABEL(Domain.GOVERNANCE_LABEL, Type.DELETE),
  IMPORT_LABEL(Domain.GOVERNANCE_LABEL, Type.IMPORT),

  EVALUATE_APPLICATION(Domain.GOVERNANCE_EVALUATION_APPLICATION, Type.EVALUATE),

  CONFIGURE_PROPRIETARY_COMPONENTS(Domain.GOVERNANCE_PROPRIETARY_COMPONENTS, Type.CONFIGURE),

  IMPORT(Domain.GOVERNANCE_IMPORT, Type.IMPORT),

  CREATE_LICENSE_THREAT_GROUP(Domain.GOVERNANCE_LICENSE_THREAT_GROUP, Type.CREATE),
  UPDATE_LICENSE_THREAT_GROUP(Domain.GOVERNANCE_LICENSE_THREAT_GROUP, Type.UPDATE),
  DELETE_LICENSE_THREAT_GROUP(Domain.GOVERNANCE_LICENSE_THREAT_GROUP, Type.DELETE),
  CONFIGURE_LICENSE_THREAT_GROUP_LICENSES(Domain.GOVERNANCE_LICENSE_THREAT_GROUP_LICENSES, Type.CONFIGURE),

  APPLY_GRANDFATHERING(Domain.GOVERNANCE_GRANDFATHERING, Type.APPLY),
  REVOKE_GRANDFATHERING(Domain.GOVERNANCE_GRANDFATHERING, Type.REVOKE),
  CONFIGURE_GRANDFATHERING(Domain.GOVERNANCE_GRANDFATHERING, Type.CONFIGURE),

  CONFIGURE_CONTINUOUS_MONITORING(Domain.GOVERNANCE_CONTINUOUS_MONITORING, Type.CONFIGURE);

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

    String GOVERNANCE_APPLICATION_CATEGORY = join(GOVERNANCE, "application-category");

    String GOVERNANCE_WAIVER = join(GOVERNANCE, "waiver");

    String GOVERNANCE_EVALUATION = join(GOVERNANCE, "evaluation");

    String GOVERNANCE_EVALUATION_APPLICATION = join(GOVERNANCE_EVALUATION, "application");

    String GOVERNANCE_COMPONENT_LICENSE = join(GOVERNANCE_COMPONENT, "license");

    String GOVERNANCE_IMPORT = join(GOVERNANCE, "import");

    String GOVERNANCE_GRANDFATHERING = join(GOVERNANCE, "grandfathering");

    String GOVERNANCE_PROPRIETARY_COMPONENTS = join(GOVERNANCE, "proprietary-components");

    String GOVERNANCE_CONTINUOUS_MONITORING = join(GOVERNANCE, "continuous-monitoring");

    String GOVERNANCE_LABEL = join(GOVERNANCE, "component-label");

    String GOVERNANCE_LICENSE_THREAT_GROUP = join(GOVERNANCE, "license-threat-group");

    String GOVERNANCE_LICENSE_THREAT_GROUP_LICENSES = join(GOVERNANCE_LICENSE_THREAT_GROUP, "licenses");

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

    String SET = "set";

    String UNSET = "unset";

    String ASSIGN = "assign";

    String REMOVE = "remove";

    String APPLY = "apply";

    String REVOKE = "revoke";

    String CONFIGURE = "configure";

    String CREATE = "create";

    String UPDATE = "update";

    String DELETE = "delete";

    String IMPORT = "import";
  }
}
