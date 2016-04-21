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

import com.sonatype.clm.dto.model.policy.PolicyAlert;

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

  private Map<String, List<PolicyAlert>> repositoryAlertMap = new LinkedHashMap<>();

  public synchronized void add(String repositoryId, PolicyAlert alert) {
    List<PolicyAlert> alerts = repositoryAlertMap.get(repositoryId);

    if (alerts == null) {
      alerts = new ArrayList<>();
      repositoryAlertMap.put(repositoryId, alerts);
    }

    alerts.add(alert);
    log.debug("Added new repository policy alert for repository {}.", repositoryId);
  }

  public synchronized Map<String, List<PolicyAlert>> remove() {
    Map<String, List<PolicyAlert>> map = repositoryAlertMap;
    repositoryAlertMap = new LinkedHashMap<>();
    return map;
  }
}
