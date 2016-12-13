/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.webhook;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.eventbus.AsyncEventBus;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.brain.model.license.LicenseThreatGroup;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.brain.security.CurrentUser;
import com.sonatype.insight.brain.security.Member;
import com.sonatype.insight.brain.webhook.ManagementEvent.LabelEvent;
import com.sonatype.insight.brain.webhook.ManagementEvent.LicenseThreatGroupEvent;
import com.sonatype.insight.brain.webhook.ManagementEvent.OwnerEvent;
import com.sonatype.insight.brain.webhook.ManagementEvent.PolicyEvent;
import com.sonatype.insight.brain.webhook.ManagementEvent.RoleEvent;
import com.sonatype.insight.brain.webhook.ManagementEvent.TagEvent;

@Named
@Singleton
public class ManagementEventService
{
  private final AsyncEventBus eventBus;

  private final CurrentUser currentUser;

  @Inject
  public ManagementEventService(final AsyncEventBus eventBus,
                                final CurrentUser currentUser)
  {
    this.eventBus = eventBus;
    this.currentUser = currentUser;
  }

  public void postEvent(final EventAction action, final Tag tag) {
    TagEvent tagEvent = buildOwnerManagementEvent(action, tag.getOrganizationId(), new TagEvent());
    tagEvent.tag = tag;
    eventBus.post(tagEvent);
  }

  public void postEvent(final EventAction action, final Label label) {
    LabelEvent labelEvent = buildOwnerManagementEvent(action, label.getOwnerId(), new LabelEvent());
    labelEvent.label = label;
    eventBus.post(labelEvent);
  }

  public void postEvent(final EventAction action, final LicenseThreatGroup ltg) {
    LicenseThreatGroupEvent licenseThreatGroupEvent = buildOwnerManagementEvent(action, ltg.getOwnerId(),
        new LicenseThreatGroupEvent());
    licenseThreatGroupEvent.licenseThreatGroup = ltg;
    eventBus.post(licenseThreatGroupEvent);
  }

  public void postEvent(final EventAction action, final Owner owner) {
    OwnerEvent ownerEvent = buildOwnerManagementEvent(action, owner.getId(), new OwnerEvent());
    ownerEvent.owner = owner;
    eventBus.post(ownerEvent);
  }

  public void postEvent(final EventAction action, final Policy policy) {
    PolicyEvent policyEvent = buildOwnerManagementEvent(action, policy.getOwnerId(), new PolicyEvent());
    policyEvent.policy = policy;
    eventBus.post(policyEvent);
  }

  public void postEvent(final EventAction action,
                        final Map<String, List<Member>> roleToMembers,
                        final String internalOwnerId)
  {
    RoleEvent roleEvent = buildOwnerManagementEvent(action, internalOwnerId, new RoleEvent());
    roleEvent.roleIdToMemberMap = roleToMembers;
    eventBus.post(roleEvent);
  }


  private <T extends ManagementEvent> T buildOwnerManagementEvent(final EventAction action,
                                                                  final String ownerId,
                                                                  final T event)
  {
    event.action = action;
    event.ownerId = ownerId;
    event.initiator = currentUser.getUsernameOrSystem();

    return event;
  }
}
