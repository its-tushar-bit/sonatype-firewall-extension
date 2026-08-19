/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto;

import java.util.List;

/**
 * DTO for waiver expiration notification configuration.
 * <p>
 * When {@code inheritConfig} is true, the owner inherits from its parent and the other fields
 * reflect the effective (inherited) values. When false, the owner has a custom configuration.
 */
public class ApiWaiverExpirationNotificationConfigDTO
{
  private boolean inheritConfig;

  private List<Integer> notificationDays;

  private String recipientType;

  private List<String> directEmails;

  private List<String> roleIds;

  public boolean isInheritConfig() {
    return inheritConfig;
  }

  public void setInheritConfig(final boolean inheritConfig) {
    this.inheritConfig = inheritConfig;
  }

  public List<Integer> getNotificationDays() {
    return notificationDays;
  }

  public void setNotificationDays(final List<Integer> notificationDays) {
    this.notificationDays = notificationDays;
  }

  public String getRecipientType() {
    return recipientType;
  }

  public void setRecipientType(final String recipientType) {
    this.recipientType = recipientType;
  }

  public List<String> getDirectEmails() {
    return directEmails;
  }

  public void setDirectEmails(final List<String> directEmails) {
    this.directEmails = directEmails;
  }

  public List<String> getRoleIds() {
    return roleIds;
  }

  public void setRoleIds(final List<String> roleIds) {
    this.roleIds = roleIds;
  }
}
