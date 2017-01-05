/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.webhook;

import java.util.List;
import java.util.Map;

import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.configuration.webhook.WebhookEvent;
import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.brain.model.license.LicenseThreatGroup;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.brain.security.Member;

public class ManagementEvent
    extends WebhookEvent
{
  public EventAction action;

  public String ownerId;

  public static class OwnerEvent
      extends ManagementEvent
  {
    public Owner owner;
  }

  public static class TagEvent
      extends ManagementEvent
  {
    public Tag tag;
  }

  public static class LabelEvent
      extends ManagementEvent
  {
    public Label label;
  }

  public static class LicenseThreatGroupEvent
      extends ManagementEvent
  {
    public LicenseThreatGroup licenseThreatGroup;
  }

  public static class PolicyEvent
      extends ManagementEvent
  {
    public Policy policy;
  }

  public static class RoleEvent
      extends ManagementEvent
  {
    public Map<String, List<Member>> roleIdToMemberMap;
  }

  @Override
  public String toString() {
    return getClass().getName() + "{ownerId=" + ownerId + "}";
  }
}
