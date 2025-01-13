/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.webhook;

import java.time.LocalDateTime;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.api.v2.dto.ApiRequestPolicyWaiverDTO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverReasonDAO;
import com.sonatype.insight.brain.eventbus.AsyncEventBus;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.PolicyWaiverReason;
import com.sonatype.insight.brain.security.CurrentUser;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;

import org.apache.commons.lang3.StringUtils;

/**
 * @since 1.164
 */
@Named
@Singleton
public class RequestPolicyWaiverEventService
{
  private final AsyncEventBus eventBus;

  private final CurrentUser currentUser;

  private final PolicyViolationDAO policyViolationDAO;

  private final PolicyWaiverReasonDAO policyWaiverReasonDAO;

  @Inject
  public RequestPolicyWaiverEventService(
          final AsyncEventBus eventBus,
          final CurrentUser currentUser,
          final PolicyViolationDAO policyViolationDAO,
          final PolicyWaiverReasonDAO policyWaiverReasonDAO)
  {
    this.eventBus = eventBus;
    this.currentUser = currentUser;
    this.policyViolationDAO = policyViolationDAO;
    this.policyWaiverReasonDAO = policyWaiverReasonDAO;
  }

  public void postRequestPolicyWaiverEvent(
      final String policyViolationId,
      final ApiRequestPolicyWaiverDTO apiRequestWaiverDTO)
  {
    verifyDtoContainsRequiredInformation(apiRequestWaiverDTO);
    verifyMaxCommentLength(apiRequestWaiverDTO);
    String ownerId = getEventOwnerIdWithNoAuthChecks(policyViolationId);

    Map<String, PolicyWaiverReason> policyWaiverReasonMap = policyWaiverReasonDAO
            .getPolicyWaiverReasonIdToPolicyWaiverReasonMap();

    WaiverRequestEvent waiverRequestEvent = new WaiverRequestEvent();
    waiverRequestEvent.initiator = currentUser.getUsername();
    waiverRequestEvent.timestamp = LocalDateTime.now();
    waiverRequestEvent.comment = apiRequestWaiverDTO.comment;
    waiverRequestEvent.policyViolationId = policyViolationId;
    waiverRequestEvent.policyViolationLink = apiRequestWaiverDTO.policyViolationLink;
    waiverRequestEvent.addWaiverLink = apiRequestWaiverDTO.addWaiverLink;
    waiverRequestEvent.ownerId = ownerId;
    waiverRequestEvent.reasonId = apiRequestWaiverDTO.reasonId;
    if (apiRequestWaiverDTO.reasonId != null && policyWaiverReasonMap.containsKey(apiRequestWaiverDTO.reasonId)) {
      waiverRequestEvent.reasonText = policyWaiverReasonMap.get(apiRequestWaiverDTO.reasonId).getReasonText();
    }

    eventBus.post(waiverRequestEvent);
  }

  private void verifyDtoContainsRequiredInformation(final ApiRequestPolicyWaiverDTO apiRequestWaiverDTO) {
    if (apiRequestWaiverDTO == null || StringUtils.isBlank(apiRequestWaiverDTO.addWaiverLink) ||
        StringUtils.isBlank(apiRequestWaiverDTO.policyViolationLink)) {
      throw new BadRequestException("both addWaiverLink and policyViolationLink are required");
    }
  }

  private void verifyMaxCommentLength(final ApiRequestPolicyWaiverDTO apiRequestWaiverDTO) {
    if (StringUtils.isNotBlank(apiRequestWaiverDTO.comment) && apiRequestWaiverDTO.comment.length() > 1000) {
      throw new BadRequestException("Comment length must not exceed 1000 characters.");
    }
  }

  private String getEventOwnerIdWithNoAuthChecks(final String policyViolationId) {
    PolicyViolation applicationPolicyViolation = policyViolationDAO.getById(policyViolationId);
    if (applicationPolicyViolation != null) {
      return applicationPolicyViolation.getOwnerId();
    }
    throw new NotFoundException("Could not find associated policy violation");
  }
}
