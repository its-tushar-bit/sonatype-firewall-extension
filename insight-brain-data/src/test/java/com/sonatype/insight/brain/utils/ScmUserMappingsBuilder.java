/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.utils;

import com.sonatype.insight.brain.db.IdUtil;
import com.sonatype.insight.brain.model.sourcecontrol.ScmUserMappings;

import java.util.List;
import java.util.Map;

public class ScmUserMappingsBuilder
{
  private String id;

  private String organizationId;

  private String roleId;

  private List<Map.Entry<String, String>> mappings;

  public ScmUserMappingsBuilder() {

  }

  public ScmUserMappings build() {
    ScmUserMappings scmUserMappings = new ScmUserMappings();

    scmUserMappings.setId(this.id);
    scmUserMappings.setRoleId(this.roleId);
    scmUserMappings.setOrganizationId(this.organizationId);
    scmUserMappings.setMappingsJson(this.mappings);

    return scmUserMappings;
  }

  public ScmUserMappingsBuilder withId() {
    this.id = IdUtil.newUUID();
    return this;
  }

  public ScmUserMappingsBuilder withId(String id) {
    this.id = id;
    return this;
  }

  public ScmUserMappingsBuilder withOrganizationId(String organizationId) {
    this.organizationId = organizationId;
    return this;
  }

  public ScmUserMappingsBuilder withMappings(List<Map.Entry<String, String>> mappings) {
    this.mappings = mappings;
    return this;
  }

  public ScmUserMappingsBuilder withRoleId(String roleId) {
    this.roleId = roleId;
    return this;
  }
}
