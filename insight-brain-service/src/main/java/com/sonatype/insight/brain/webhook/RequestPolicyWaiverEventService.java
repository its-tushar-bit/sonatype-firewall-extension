/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.webhook;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.api.v2.dto.ApiRequestPolicyWaiverDTO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverReasonDAO;
import com.sonatype.insight.brain.dataaccess.policy.ProxyRepositoryPolicyViolationDAO;
import com.sonatype.insight.brain.model.policy.ProxyRepositoryPolicyViolation;
import com.sonatype.insight.brain.eventbus.AsyncEventBus;
import com.sonatype.insight.brain.landing.UserInterfaceLinksHelper;
import com.sonatype.insight.brain.model.configuration.webhook.Webhook;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.PolicyWaiverReason;
import com.sonatype.insight.brain.security.CurrentUser;
import com.sonatype.insight.brain.license.LicenseNameProvider;
import com.sonatype.insight.brain.service.BaseUrl;
import com.sonatype.insight.brain.telemetry.PolicyViolationTelemetryBuilder;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.brain.telemetry.TelemetryUtils;
import com.sonatype.insight.client.utils.UrlUtils;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;

import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.insight.telemetry.model.TelemetryPurpose.POLICY_WAIVER_REQUEST;

/**
 * @since 1.164
 */
@Named
@Singleton
public class RequestPolicyWaiverEventService
{
  private static final Logger log = LoggerFactory.getLogger(RequestPolicyWaiverEventService.class);

  private static final String WAIVER_REASON = "waiver_reason";

  private final AsyncEventBus eventBus;

  private final CurrentUser currentUser;

  private final PolicyViolationDAO policyViolationDAO;

  private final PolicyWaiverReasonDAO policyWaiverReasonDAO;

  private final ProxyRepositoryPolicyViolationDAO proxyRepositoryPolicyViolationDAO;

  private final TelemetryUtils telemetryUtils;

  private final BaseUrl baseUrl;

  private final LicenseNameProvider licenseNameProvider;

  @VisibleForTesting
  private TelemetrySender telemetrySender;

  @Inject
  public RequestPolicyWaiverEventService(
      final AsyncEventBus eventBus,
      final CurrentUser currentUser,
      final PolicyViolationDAO policyViolationDAO,
      final PolicyWaiverReasonDAO policyWaiverReasonDAO,
      final ProxyRepositoryPolicyViolationDAO proxyRepositoryPolicyViolationDAO,
      final TelemetrySender telemetrySender,
      final TelemetryUtils telemetryUtils,
      final BaseUrl baseUrl,
      final LicenseNameProvider licenseNameProvider)
  {
    this.eventBus = eventBus;
    this.currentUser = currentUser;
    this.policyViolationDAO = policyViolationDAO;
    this.policyWaiverReasonDAO = policyWaiverReasonDAO;
    this.proxyRepositoryPolicyViolationDAO = proxyRepositoryPolicyViolationDAO;
    this.telemetrySender = telemetrySender;
    this.telemetryUtils = telemetryUtils;
    this.baseUrl = baseUrl;
    this.licenseNameProvider = licenseNameProvider;
  }

  /**
   * @deprecated since 1.192
   *             Kept only for the legacy endpoint "/waiverRequests/{policyViolationId}" which is no longer called by
   *             the UI.
   *             Use {@link #postPolicyWaiverRequestEvent(String, String, String, String, String, String)} instead.
   */
  @Deprecated(since = "1.192")
  public void postRequestPolicyWaiverEvent(
      final String policyViolationId,
      final ApiRequestPolicyWaiverDTO apiRequestWaiverDTO)
  {
    verifyDtoContainsRequiredInformation(apiRequestWaiverDTO);
    verifyMaxCommentLength(apiRequestWaiverDTO);
    WaiverRequestEvent waiverRequestEvent = createWaiverRequestEvent(
        policyViolationId,
        apiRequestWaiverDTO.comment,
        apiRequestWaiverDTO.policyViolationLink,
        apiRequestWaiverDTO.addWaiverLink,
        null,
        apiRequestWaiverDTO.reasonId);
    eventBus.post(waiverRequestEvent);
    sendTelemetryForWaiverRequest(waiverRequestEvent);
  }

  public void postPolicyWaiverRequestEvent(
      final String policyViolationId,
      final String comment,
      final String reasonId,
      final String ownerType,
      final String ownerId,
      final String policyWaiverRequestId)
  {
    String policyViolationLink =
        prependBaseUrl(UserInterfaceLinksHelper.getPolicyViolationDetailsUrl(policyViolationId));
    String addWaiverLink = prependBaseUrl(UserInterfaceLinksHelper.getAddWaiverUrl(policyViolationId, comment,
        reasonId));
    String reviewWaiverRequestLink =
        prependBaseUrl(UserInterfaceLinksHelper.getReviewWaiverRequestUrl(ownerType, ownerId,
            policyWaiverRequestId));
    verifyMaxCommentLength(comment);
    WaiverRequestEvent waiverRequestEvent = createWaiverRequestEvent(
        policyViolationId,
        comment,
        policyViolationLink,
        addWaiverLink, // New event receivers won't need this link, however legacy ones might still expect it.
        reviewWaiverRequestLink,
        reasonId);
    eventBus.post(waiverRequestEvent);
  }

  private WaiverRequestEvent createWaiverRequestEvent(
      final String policyViolationId,
      final String comment,
      final String policyViolationLink,
      final String addWaiverLink,
      final String reviewWaiverRequestLink,
      final String reasonId)
  {
    String ownerId = getEventOwnerIdWithNoAuthChecks(policyViolationId);
    Map<String, PolicyWaiverReason> policyWaiverReasonMap = policyWaiverReasonDAO
        .getPolicyWaiverReasonIdToPolicyWaiverReasonMap();

    WaiverRequestEvent waiverRequestEvent = new WaiverRequestEvent();
    waiverRequestEvent.initiator = currentUser.getUsername();
    waiverRequestEvent.timestamp = LocalDateTime.now();
    waiverRequestEvent.comment = comment;
    waiverRequestEvent.policyViolationId = policyViolationId;
    waiverRequestEvent.policyViolationLink = policyViolationLink;
    waiverRequestEvent.addWaiverLink = addWaiverLink;
    waiverRequestEvent.reviewWaiverRequestLink = reviewWaiverRequestLink;
    waiverRequestEvent.ownerId = ownerId;
    waiverRequestEvent.reasonId = reasonId;
    if (reasonId != null && policyWaiverReasonMap.containsKey(reasonId)) {
      waiverRequestEvent.reasonText = policyWaiverReasonMap.get(reasonId).getReasonText();
    }
    waiverRequestEvent.source = Webhook.CONTEXT_LIFECYCLE;

    return waiverRequestEvent;
  }

  public void postRepositoryWaiverRequestEvent(
      final String policyViolationId,
      final String comment,
      final String reasonId,
      final String ownerType,
      final String ownerId,
      final String policyWaiverRequestId)
  {
    ProxyRepositoryPolicyViolation proxyRepositoryPolicyViolation =
        proxyRepositoryPolicyViolationDAO.getById(policyViolationId);
    if (proxyRepositoryPolicyViolation == null) {
      log.warn("Could not find repository policy violation with ID {} — waiver request webhook event not posted.",
          policyViolationId);
      return;
    }

    String reviewWaiverRequestLink =
        prependBaseUrl(
            UserInterfaceLinksHelper.getFirewallReviewWaiverRequestUrl(ownerType, ownerId, policyWaiverRequestId));
    String policyViolationLink =
        prependBaseUrl(UserInterfaceLinksHelper.getFirewallViolationDetailsUrl(
            proxyRepositoryPolicyViolation.getRepositoryId(), policyViolationId));
    String addWaiverLink =
        prependBaseUrl(UserInterfaceLinksHelper.getFirewallAddWaiverUrl(
            proxyRepositoryPolicyViolation.getRepositoryId(), policyViolationId));
    verifyMaxCommentLength(comment);

    Map<String, PolicyWaiverReason> policyWaiverReasonMap =
        policyWaiverReasonDAO.getPolicyWaiverReasonIdToPolicyWaiverReasonMap();

    WaiverRequestEvent waiverRequestEvent = new WaiverRequestEvent();
    waiverRequestEvent.initiator = currentUser.getUsername();
    waiverRequestEvent.timestamp = LocalDateTime.now();
    waiverRequestEvent.comment = comment;
    waiverRequestEvent.policyViolationId = policyViolationId;
    waiverRequestEvent.policyViolationLink = policyViolationLink;
    waiverRequestEvent.addWaiverLink = addWaiverLink;
    waiverRequestEvent.reviewWaiverRequestLink = reviewWaiverRequestLink;
    waiverRequestEvent.ownerId = proxyRepositoryPolicyViolation.getOwnerId();
    waiverRequestEvent.reasonId = reasonId;
    if (reasonId != null && policyWaiverReasonMap.containsKey(reasonId)) {
      waiverRequestEvent.reasonText = policyWaiverReasonMap.get(reasonId).getReasonText();
    }
    waiverRequestEvent.source = Webhook.CONTEXT_FIREWALL;

    eventBus.post(waiverRequestEvent);
  }

  public void setTelemetrySender(TelemetrySender telemetrySender) {
    this.telemetrySender = telemetrySender;
  }

  private void verifyDtoContainsRequiredInformation(final ApiRequestPolicyWaiverDTO apiRequestWaiverDTO) {
    if (apiRequestWaiverDTO == null || StringUtils.isBlank(apiRequestWaiverDTO.addWaiverLink) ||
        StringUtils.isBlank(apiRequestWaiverDTO.policyViolationLink))
    {
      throw new BadRequestException("both addWaiverLink and policyViolationLink are required");
    }
  }

  private void verifyMaxCommentLength(final ApiRequestPolicyWaiverDTO apiRequestWaiverDTO) {
    verifyMaxCommentLength(apiRequestWaiverDTO.comment);
  }

  private void verifyMaxCommentLength(final String comment) {
    if (StringUtils.isNotBlank(comment) && comment.length() > 1000) {
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

  private void sendTelemetryForWaiverRequest(WaiverRequestEvent waiverRequestEvent) {
    PolicyViolation policyViolation = policyViolationDAO.getById(waiverRequestEvent.policyViolationId);
    policyViolationDAO.loadConstraintFacts(List.of(policyViolation));
    var telemetryData =
        new PolicyViolationTelemetryBuilder(policyViolation, POLICY_WAIVER_REQUEST, telemetryUtils, licenseNameProvider)
            .build()
            .put(WAIVER_REASON, waiverRequestEvent.reasonText);

    telemetrySender.send(telemetryData);
  }

  private String prependBaseUrl(String relativeUrl) {
    return UrlUtils.appendUrlPaths(baseUrl.get(), relativeUrl);
  }
}
