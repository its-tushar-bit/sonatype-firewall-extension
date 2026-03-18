/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.sourcecontrol;

import com.fasterxml.jackson.core.type.TypeReference;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.model.HasStringId;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map.Entry;

/**
 * @since 1.186
 */
@Entity
@Table(name = "scm_user_mappings")
public class ScmUserMappings
    implements HasStringId
{
  @Id
  @Column(name = "scm_user_mappings_id")
  private String id;

  @Column(name = "organization_id")
  private String organizationId;

  @Column(name = "role_id")
  private String roleId;

  @Column(name = "mappings_json")
  private String mappingsJson;

  @Transient
  private List<Entry<String, String>> mappings;

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setId(String id) {
    this.id = id;
  }

  public String getOrganizationId() {
    return organizationId;
  }

  public void setOrganizationId(String organizationId) {
    this.organizationId = organizationId;
  }

  public String getRoleId() {
    return roleId;
  }

  public void setRoleId(String roleId) {
    this.roleId = roleId;
  }

  public String getMappingsJson() {
    return mappingsJson;
  }

  public void setMappingsJson(List<Entry<String, String>> mappings) {
    this.mappingsJson = JsonUtils.format(mappings);
  }

  public List<Entry<String, String>> getMappings() {
    if (this.mappings == null) {
      try {
        this.mappings = JsonUtils.parse(this.mappingsJson, new TypeReference<List<Entry<String, String>>>()
        {
        });
      }
      catch (IOException e) {
        throw new UncheckedIOException("Failed to read user mappings " + id, e);
      }
    }
    return mappings;
  }
}
