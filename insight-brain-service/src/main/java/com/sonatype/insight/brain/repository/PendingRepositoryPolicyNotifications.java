/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.model.policy.notifications.PolicyNotification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since 1.21
 */
@Named
@Singleton
public class PendingRepositoryPolicyNotifications
{
  private final Logger log = LoggerFactory.getLogger(PendingRepositoryPolicyNotifications.class);

  private Map<String, List<PolicyNotification>> repositoryNotificationMap = new LinkedHashMap<>();

  public synchronized void add(String repositoryId, PolicyNotification policyNotification) {
    List<PolicyNotification> notifications = repositoryNotificationMap.get(repositoryId);

    if (notifications == null) {
      notifications = new ArrayList<>();
      repositoryNotificationMap.put(repositoryId, notifications);
    }

    notifications.add(policyNotification);
    log.debug("Added new repository policy notification for repository {}.", repositoryId);
  }

  public synchronized Map<String, List<PolicyNotification>> remove() {
    Map<String, List<PolicyNotification>> map = repositoryNotificationMap;
    repositoryNotificationMap = new LinkedHashMap<>();
    return map;
  }
}
