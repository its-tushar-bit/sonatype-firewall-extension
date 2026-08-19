/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.notifications.Notifications;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

public class PolicyMaintenanceTelemetry
{
  private static final String OWNER_ATTR = "owner_id";

  private static final String ACTION_ATTR = "event_action";

  private static final String POLICY_NAME_ATTR = "policy_name";

  private static final String POLICY_THREAT_LEVEL = "threat_level";

  private static final String POLICY_CONSTRAINTS_ATTR = "policy_constraints";

  private static final String POLICY_ACTIONS_ATTR = "policy_actions";

  private static final String POLICY_ACTIONS_OVERRIDES_ATTR = "policy_actions_overrides";

  private static final String POLICY_NOTIFICATIONS_ATTR = "policy_notifications";

  private static final String POLICY_NOTIFICATIONS_OVERRIDES_ATTR = "policy_notifications_overrides";

  public enum Action
  {
    CREATE,
    UPDATE,
    DELETE
  }

  public static TelemetryData getTelemetry(final Action action, final String obfuscatedOwnerId, final Policy policy) {
    TelemetryData telemetryData = new TelemetryData(TelemetryPurpose.POLICY_MAINTENANCE);
    telemetryData.put(OWNER_ATTR, obfuscatedOwnerId);
    telemetryData.put(ACTION_ATTR, action.name());
    telemetryData.put(POLICY_NAME_ATTR, policy.getName());
    telemetryData.put(POLICY_THREAT_LEVEL, policy.getThreatLevel());
    telemetryData.put(POLICY_CONSTRAINTS_ATTR, policy.getConstraints());
    telemetryData.put(POLICY_ACTIONS_ATTR, getActionsList(Collections.singletonList(policy.getActions())));
    telemetryData.put(POLICY_ACTIONS_OVERRIDES_ATTR,
        policy.getPolicyActionsOverrides() != null
            ? getActionsList(policy.getPolicyActionsOverrides().values())
            : List.of());
    telemetryData.put(POLICY_NOTIFICATIONS_ATTR,
        getNotificationTypes(Collections.singletonList(policy.getNotifications())));
    telemetryData.put(POLICY_NOTIFICATIONS_OVERRIDES_ATTR,
        policy.getPolicyNotificationsOverrides() != null
            ? getNotificationTypes(policy.getPolicyNotificationsOverrides().values())
            : Set.of());
    return telemetryData;
  }

  public static List<Map<String, String>> getActionsList(Collection<Map<String, String>> actions) {
    if (actions == null || actions.isEmpty()) {
      return List.of();
    }
    ArrayList<Map<String, String>> list = new ArrayList<>();
    for (Map<String, String> action : actions) {
      for (Map.Entry<String, String> entry : action.entrySet()) {
        list.add(Map.of("stage", entry.getKey(), "action", entry.getValue()));
      }
    }
    return list;
  }

  public static Set<String> getNotificationTypes(Collection<Notifications> notifications) {
    if (notifications == null || notifications.isEmpty()) {
      return Set.of();
    }
    return notifications.stream()
        .flatMap(n -> n.getAllNotifications().stream())
        .map(n -> n.getClass().getSimpleName())
        .collect(Collectors.toSet());
  }
}
