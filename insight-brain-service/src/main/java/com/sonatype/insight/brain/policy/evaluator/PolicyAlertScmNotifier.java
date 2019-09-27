/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.policy.evaluator;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentIdentifierDTOV2;
import com.sonatype.insight.brain.api.v2.dto.remediation.ApiComponentRemediationDTO;
import com.sonatype.insight.brain.api.v2.dto.remediation.options.ApiVersionChangeOptionDTO;
import com.sonatype.insight.brain.api.v2.service.ApiComponentRemediationService;
import com.sonatype.insight.brain.git.PullRequestFeatureCheck;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.policy.notifications.PolicyNotification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class to send notifications of policy alerts to Source Code Management
 * systems like github
 */
public class PolicyAlertScmNotifier
{
  private static final Logger log = LoggerFactory.getLogger(PolicyAlertScmNotifier.class);

  private final PullRequestFeatureCheck pullRequestFeatureCheck;

  private final ApiComponentRemediationService remediationService;

  private final PolicyAlertSourceCodeOrganizer policyAlertSourceCodeOrganizer;

  @Inject
  public PolicyAlertScmNotifier(final PullRequestFeatureCheck pullRequestFeatureCheck,
                                final ApiComponentRemediationService remediationService,
                                final PolicyAlertSourceCodeOrganizer policyAlertSourceCodeOrganizer)
  {
    this.pullRequestFeatureCheck = pullRequestFeatureCheck;
    this.remediationService = remediationService;
    this.policyAlertSourceCodeOrganizer = policyAlertSourceCodeOrganizer;
  }

  public void sendNotifications(final Application app,
                                final List<PolicyNotification> policyNotifications)
      throws IOException
  {
    if (!pullRequestFeatureCheck.isPullRequestFeatureSupported(app)) {
      return;
    }

    // for each component, check to see if remediation options are available
    String ownerId = app.getId();
    Map<ComponentIdentifier, List<PolicyNotification>> sortedComponentAlerts =
        policyAlertSourceCodeOrganizer.getNotificationsForScm(policyNotifications);
    for (Map.Entry<ComponentIdentifier, List<PolicyNotification>> entry : sortedComponentAlerts.entrySet()) {
      List<ApiVersionChangeOptionDTO> remediationOptions = getRemediationList(entry.getKey(), ownerId);
      if (remediationOptions.isEmpty()) {
        log.debug("No remediation options found for component [{}]", entry.getKey());
        continue;
      }
      // TODO invoke PR engine
      log.debug("Invoke PR engine to construct a PR for [{}]", entry.getKey());
    }

  }

  private List<ApiVersionChangeOptionDTO> getRemediationList(ComponentIdentifier componentIdentifier,
                                 String ownerId)
  {
    ApiComponentDTOV2 componentDto = new ApiComponentDTOV2();
    componentDto.componentIdentifier = ApiComponentIdentifierDTOV2.fromComponentIdentifier(componentIdentifier);
    OwnerType ownerType = OwnerType.APPLICATION;
    String stageId = null;
    ApiComponentRemediationDTO remediationDTO =
        remediationService.getSuggestedRemediationForComponent(componentDto, ownerType, ownerId, stageId);
    return remediationDTO.remediation.versionChanges;
  }
}
