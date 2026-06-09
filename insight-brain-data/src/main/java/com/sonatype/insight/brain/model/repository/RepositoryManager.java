/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.repository;

import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.sonatype.insight.brain.model.Nameable;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.model.HasStringId;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "repository_manager")
public class RepositoryManager
    extends Nameable
    implements HasStringId, Owner
{
  @Id
  @Column(name = "repository_manager_id")
  private String id;

  @Column(name = "instance_id")
  private String instanceId;

  @Column(name = "product_name")
  private String productName;

  @Column(name = "product_version")
  private String productVersion;

  @Column(name = "user_agent")
  private String userAgent;

  @Column(name = "configured")
  private boolean configured;

  @Column(name = "configure_time")
  private Date configureTime;

  @Column(name = "base_url")
  private String baseUrl;

  @Column(name = "related_organization_id")
  private String relatedOrganizationId;

  @Column(name = "manager_type")
  @Enumerated(EnumType.STRING)
  private ManagerType managerType;

  public RepositoryManager() {
  }

  public RepositoryManager(String instanceId) {
    this.instanceId = instanceId;
    this.configured = true;
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setId(String id) {
    this.id = id;
  }

  public String getInstanceId() {
    return instanceId;
  }

  public void setInstanceId(String instanceId) {
    this.instanceId = instanceId;
  }

  public String getUserAgent() {
    return userAgent;
  }

  public void setUserAgent(String userAgent) {
    this.userAgent = userAgent;
  }

  public boolean isConfigured() {
    return configured;
  }

  public void setConfigured(boolean configured) {
    this.configured = configured;
  }

  public Date getConfigureTime() {
    return configureTime;
  }

  public void setConfigureTime(Date configureTime) {
    this.configureTime = configureTime;
  }

  public String getProductName() {
    return productName;
  }

  public void setProductName(String productName) {
    this.productName = productName;
  }

  public String getProductVersion() {
    return productVersion;
  }

  public void setProductVersion(String productVersion) {
    this.productVersion = productVersion;
  }

  @Override
  public String getName() {
    return name != null ? name : instanceId;
  }

  @JsonIgnore
  public String getRawName() {
    return name;
  }

  @Override
  @JsonIgnore
  public String getPublicId() {
    return id;
  }

  @Override
  @JsonIgnore
  public String getParentOwnerId() {
    return RepositoryContainer.REPOSITORY_CONTAINER_ID;
  }

  @Override
  @JsonIgnore
  public boolean canHaveChildren() {
    return true;
  }

  @Override
  @JsonIgnore
  public OwnerType getType() {
    return OwnerType.REPOSITORY_MANAGER;
  }

  public String getBaseUrl() {
    return baseUrl;
  }

  public void setBaseUrl(final String baseUrl) {
    this.baseUrl = baseUrl;
  }

  public String getRelatedOrganizationId() {
    return relatedOrganizationId;
  }

  public void setRelatedOrganizationId(final String relatedOrganizationId) {
    this.relatedOrganizationId = relatedOrganizationId;
  }

  public ManagerType getManagerType() {
    return managerType;
  }

  public void setManagerType(final ManagerType managerType) {
    this.managerType = managerType;
  }
}
