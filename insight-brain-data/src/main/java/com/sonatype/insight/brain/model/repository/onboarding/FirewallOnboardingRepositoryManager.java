/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.repository.onboarding;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import com.sonatype.insight.model.HasStringId;

@Entity
@Table(name = "firewall_onboarding_repository_manager")
public class FirewallOnboardingRepositoryManager
    implements HasStringId
{
  @Id
  @Column(name = "firewall_onboarding_repository_manager_id")
  private String id;

  @Column(name = "instance_id")
  private String instanceId;

  @Column(name = "request_time")
  private Date requestTime;

  @Column(name = "request_username")
  private String requestUsername;

  @Column(name = "configure_time")
  private Date configureTime;

  @Column(name = "configure_username")
  private String configureUsername;

  @Column(name = "request_user_agent")
  private String requestUserAgent;

  public FirewallOnboardingRepositoryManager() {
  }

  public FirewallOnboardingRepositoryManager(String instanceId, String requestUsername, String requestUserAgent) {
    this.instanceId = instanceId;
    this.requestUsername = requestUsername;
    this.requestUserAgent = requestUserAgent;
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

  public Date getRequestTime() {
    return requestTime;
  }

  public void setRequestTime(Date requestTime) {
    this.requestTime = requestTime;
  }

  public String getRequestUsername() {
    return requestUsername;
  }

  public void setRequestUsername(String requestUsername) {
    this.requestUsername = requestUsername;
  }

  public Date getConfigureTime() {
    return configureTime;
  }

  public void setConfigureTime(Date configureTime) {
    this.configureTime = configureTime;
  }

  public String getConfigureUsername() {
    return configureUsername;
  }

  public void setConfigureUsername(String configureUsername) {
    this.configureUsername = configureUsername;
  }

  public String getRequestUserAgent() {
    return requestUserAgent;
  }

  public void setRequestUserAgent(String requestUserAgent) {
    this.requestUserAgent = requestUserAgent;
  }
}
