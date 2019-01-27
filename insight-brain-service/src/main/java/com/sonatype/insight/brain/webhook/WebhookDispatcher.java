/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.webhook;

import java.util.Date;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.api.v2.dto.ApiComponentIdentifierDTOV2;
import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.AuditRecorder;
import com.sonatype.insight.brain.audit.AuditSession;
import com.sonatype.insight.brain.configuration.webhook.WebhookService;
import com.sonatype.insight.brain.eventbus.AsyncEventBus;
import com.sonatype.insight.brain.features.Feature;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.configuration.webhook.Webhook;
import com.sonatype.insight.brain.model.configuration.webhook.WebhookEventType;
import com.sonatype.insight.brain.product.license.CLMLicenseManager;
import com.sonatype.insight.brain.product.license.LicenseListener;
import com.sonatype.insight.brain.webhook.ManagementEvent.LabelEvent;
import com.sonatype.insight.brain.webhook.ManagementEvent.LicenseThreatGroupEvent;
import com.sonatype.insight.brain.webhook.ManagementEvent.OwnerEvent;
import com.sonatype.insight.brain.webhook.ManagementEvent.PolicyEvent;
import com.sonatype.insight.brain.webhook.ManagementEvent.RoleEvent;
import com.sonatype.insight.brain.webhook.ManagementEvent.TagEvent;
import com.sonatype.insight.brain.webhook.dto.ApplicationEvaluationPayload;
import com.sonatype.insight.brain.webhook.dto.ApplicationEvaluationPayload.ApplicationEvaluationDTO;
import com.sonatype.insight.brain.webhook.dto.LicenseOverridePayload;
import com.sonatype.insight.brain.webhook.dto.LicenseOverridePayload.LicenseOverrideDTO;
import com.sonatype.insight.brain.webhook.dto.PolicyManagementPayload;
import com.sonatype.insight.brain.webhook.dto.PolicyManagementType;
import com.sonatype.insight.brain.webhook.dto.SecurityVulnerabilityOverridePayload;
import com.sonatype.insight.brain.webhook.dto.SecurityVulnerabilityOverridePayload.SecurityVulnerabilityOverrideDTO;

import com.google.common.eventbus.Subscribe;
import io.dropwizard.lifecycle.Managed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since 1.25.0
 */
@Named
@Singleton
public class WebhookDispatcher
    implements Managed, LicenseListener
{
  public static final String APPLICATION_EVALUATION_ID = "iq:applicationEvaluation";

  public static final String LICENSE_OVERRIDE_MANAGEMENT_ID = "iq:licenseOverrideManagement";

  public static final String SECURITY_VULNERABILITY_OVERRIDE_MANAGEMENT_ID = "iq:securityVulnerabilityOverrideManagement";

  public static final String POLICY_MANAGEMENT_ID = "iq:policyManagement";

  private static final Logger log = LoggerFactory.getLogger(WebhookDispatcher.class);

  private final WebhookService webhookService;

  private final WebhookClientUtil webhookClientUtil;

  private final AsyncEventBus asyncEventBus;

  private final OwnerDTOUtil ownerDTOUtil;

  private final AuditRecorder auditRecorder;

  private final CLMLicenseManager clmLicenseManager;

  @Inject
  public WebhookDispatcher(final AsyncEventBus asyncEventBus,
                           final WebhookService webhookService,
                           final WebhookClientUtil webhookClientUtil,
                           final OwnerDTOUtil ownerDTOUtil,
                           final AuditRecorder auditRecorder,
                           final CLMLicenseManager clmLicenseManager)
  {
    this.webhookService = webhookService;
    this.webhookClientUtil = webhookClientUtil;
    this.asyncEventBus = asyncEventBus;
    this.ownerDTOUtil = ownerDTOUtil;
    this.auditRecorder = auditRecorder;
    this.clmLicenseManager = clmLicenseManager;
    clmLicenseManager.addListener(this);
  }

  @Subscribe
  public void on(final ApplicationEvaluationEvent applicationEvaluationEvent) {
    for (Webhook webhook : getWebhooksOfEventType(WebhookEventType.APPLICATION_EVALUATION)) {
      invokeWithAudit(webhook, WebhookEventType.APPLICATION_EVALUATION,
          () -> sendApplicationEvaluationPayload(webhookService.getDecrypted(webhook.getId()),
              applicationEvaluationEvent));
    }
  }

  @Subscribe
  public void on(final OwnerEvent ownerEvent) {
    for (Webhook webhook : getWebhooksOfEventType(WebhookEventType.POLICY_MANAGEMENT)) {
      PolicyManagementType type = ownerEvent.owner.getType() ==
          OwnerType.ORGANIZATION ? PolicyManagementType.ORGANIZATION : PolicyManagementType.APPLICATION;
      invokeWithAudit(webhook, WebhookEventType.POLICY_MANAGEMENT,
          () -> sendPolicyManagementPayload(webhookService.getDecrypted(webhook.getId()), type,
              ownerEvent.owner.getId(),
              ownerEvent));
    }
  }

  @Subscribe
  public void on(final TagEvent tagEvent) {
    for (Webhook webhook : getWebhooksOfEventType(WebhookEventType.POLICY_MANAGEMENT)) {
      invokeWithAudit(webhook, WebhookEventType.POLICY_MANAGEMENT,
          () -> sendPolicyManagementPayload(webhookService.getDecrypted(webhook.getId()),
              PolicyManagementType.APPLICATION_CATEGORY, tagEvent.tag.getId(), tagEvent));
    }
  }

  @Subscribe
  public void on(final LabelEvent labelEvent) {
    for (Webhook webhook : getWebhooksOfEventType(WebhookEventType.POLICY_MANAGEMENT)) {
      invokeWithAudit(webhook, WebhookEventType.POLICY_MANAGEMENT,
          () -> sendPolicyManagementPayload(webhookService.getDecrypted(webhook.getId()), PolicyManagementType.LABEL,
              labelEvent.label.getId(), labelEvent));
    }
  }

  @Subscribe
  public void on(final LicenseThreatGroupEvent licenseThreatGroupEvent) {
    for (Webhook webhook : getWebhooksOfEventType(WebhookEventType.POLICY_MANAGEMENT)) {
      invokeWithAudit(webhook, WebhookEventType.POLICY_MANAGEMENT,
          () -> sendPolicyManagementPayload(webhookService.getDecrypted(webhook.getId()),
              PolicyManagementType.LICENSE_THREAT_GROUP, licenseThreatGroupEvent.licenseThreatGroup.getId(),
              licenseThreatGroupEvent));
    }
  }

  @Subscribe
  public void on(final PolicyEvent policyEvent) {
    for (Webhook webhook : getWebhooksOfEventType(WebhookEventType.POLICY_MANAGEMENT)) {
      invokeWithAudit(webhook, WebhookEventType.POLICY_MANAGEMENT,
          () -> sendPolicyManagementPayload(webhookService.getDecrypted(webhook.getId()), PolicyManagementType.POLICY,
              policyEvent.policy.getId(), policyEvent));
    }
  }

  @Subscribe
  public void on(final RoleEvent roleEvent) {
    for (Webhook webhook : getWebhooksOfEventType(WebhookEventType.POLICY_MANAGEMENT)) {
      invokeWithAudit(webhook, WebhookEventType.POLICY_MANAGEMENT,
          () -> sendPolicyManagementPayload(webhookService.getDecrypted(webhook.getId()), PolicyManagementType.ACCESS,
              roleEvent.ownerId, roleEvent));
    }
  }

  @Subscribe
  public void on(final SecurityVulnerabilityOverrideEvent securityVulnerabilityOverrideEvent) {
    for (Webhook webhook : getWebhooksOfEventType(WebhookEventType.SECURITY_VULNERABILITY_OVERRIDE_MANAGEMENT)) {
      invokeWithAudit(webhook, WebhookEventType.SECURITY_VULNERABILITY_OVERRIDE_MANAGEMENT,
          () -> sendSecurityVulnerabilityOverridePayload(webhookService.getDecrypted(webhook.getId()),
              securityVulnerabilityOverrideEvent));
    }
  }

  @Subscribe
  public void on(final LicenseOverrideEvent licenseOverrideEvent) {
    for (Webhook webhook : getWebhooksOfEventType(WebhookEventType.LICENSE_OVERRIDE_MANAGEMENT)) {
      invokeWithAudit(webhook, WebhookEventType.LICENSE_OVERRIDE_MANAGEMENT,
          () -> sendLicenseOverridePayload(webhookService.getDecrypted(webhook.getId()), licenseOverrideEvent));
    }
  }

  private void invokeWithAudit(Webhook webhook, WebhookEventType webhookEventType, Runnable invocation) {
    try (AuditSession auditSession = auditRecorder.recordSystemEvent(AuditEvent.INVOKE_WEBHOOK)) {
      try {
        AuditData.get().setData("webhookdId", webhook.getId())
            .setData("webhookUrl", webhook.getUrl())
            .setEnum("webhookTriggerEvent", webhookEventType);
        invocation.run();
      }
      catch (RuntimeException e) {
        AuditData.get().setException(e);
        throw e;
      }
    }
  }

  private Iterable<Webhook> getWebhooksOfEventType(final WebhookEventType webhookEventType) {
    return webhookService.getAll_Unauthorized().stream()
        .filter(webhook -> webhook.getEventTypes().contains(webhookEventType)).collect(Collectors.toList());
  }

  private void sendLicenseOverridePayload(final Webhook webhook, final LicenseOverrideEvent event) {
    LicenseOverrideDTO licenseOverrideDTO = new LicenseOverrideDTO();
    licenseOverrideDTO.id = event.licenseOverride.getId();
    licenseOverrideDTO.ownerId = event.licenseOverride.getOwnerId();
    licenseOverrideDTO.status = event.licenseOverride.getStatus().name();
    licenseOverrideDTO.comment = event.licenseOverride.getComment();
    licenseOverrideDTO.licenseIds = event.licenseOverride.getLicenseIds();
    licenseOverrideDTO.componentIdentifier = ApiComponentIdentifierDTOV2
        .fromComponentIdentifier(event.licenseOverride.getComponentIdentifier());

    LicenseOverridePayload payload = new LicenseOverridePayload();
    payload.action = event.action;
    payload.id = event.licenseOverride.getId();
    payload.timestamp = new Date();
    payload.initiator = event.initiator;
    payload.licenseOverride = licenseOverrideDTO;

    webhookClientUtil.post(webhook, LICENSE_OVERRIDE_MANAGEMENT_ID, payload);
  }

  private void sendApplicationEvaluationPayload(final Webhook webhook, final ApplicationEvaluationEvent event) {
    ApplicationEvaluationDTO applicationEvaluationDTO = new ApplicationEvaluationDTO();
    applicationEvaluationDTO.policyEvaluationId = event.policyEvaluationId;
    applicationEvaluationDTO.stage = event.stageTypeId;
    applicationEvaluationDTO.ownerId = event.ownerId;
    applicationEvaluationDTO.evaluationDate = event.evaluationDate;
    applicationEvaluationDTO.affectedComponentCount = event.affectedComponentCount;
    applicationEvaluationDTO.criticalComponentCount = event.criticalComponentCount;
    applicationEvaluationDTO.severeComponentCount = event.severeComponentCount;
    applicationEvaluationDTO.moderateComponentCount = event.moderateComponentCount;
    applicationEvaluationDTO.outcome = event.outcome;

    ApplicationEvaluationPayload payload = new ApplicationEvaluationPayload();
    payload.timestamp = new Date();
    payload.initiator = event.initiator;
    payload.id = event.policyEvaluationId;
    payload.applicationEvaluation = applicationEvaluationDTO;

    webhookClientUtil.post(webhook, APPLICATION_EVALUATION_ID, payload);
  }

  private void sendSecurityVulnerabilityOverridePayload(final Webhook webhook,
                                                        final SecurityVulnerabilityOverrideEvent event)
  {
    SecurityVulnerabilityOverrideDTO securityVulnerabilityOverrideDTO = new SecurityVulnerabilityOverrideDTO();
    securityVulnerabilityOverrideDTO.id = event.override.getId();
    securityVulnerabilityOverrideDTO.ownerId = event.override.getOwnerId();
    securityVulnerabilityOverrideDTO.hash = event.override.getHash();
    securityVulnerabilityOverrideDTO.source = event.override.getSource();
    securityVulnerabilityOverrideDTO.referenceId = event.override.getReferenceId();
    securityVulnerabilityOverrideDTO.status = event.override.getStatus().name();
    securityVulnerabilityOverrideDTO.comment = event.override.getComment();

    SecurityVulnerabilityOverridePayload payload = new SecurityVulnerabilityOverridePayload();
    payload.action = event.action;
    payload.id = event.override.getId();
    payload.initiator = event.initiator;
    payload.timestamp = new Date();
    payload.securityVulnerabilityOverride = securityVulnerabilityOverrideDTO;

    webhookClientUtil.post(webhook, SECURITY_VULNERABILITY_OVERRIDE_MANAGEMENT_ID, payload);
  }

  private void sendPolicyManagementPayload(final Webhook webhook,
                                           final PolicyManagementType type,
                                           final String id,
                                           final ManagementEvent event)
  {
    PolicyManagementPayload payload = new PolicyManagementPayload();
    payload.timestamp = new Date();
    payload.initiator = event.initiator;
    payload.type = type;
    payload.id = id;
    payload.action = event.action;

    payload.owner = ownerDTOUtil.buildOwnerDTO(event);

    webhookClientUtil.post(webhook, POLICY_MANAGEMENT_ID, payload);
  }

  @Override
  public void start() {
    if (!clmLicenseManager.hasFeature(Feature.WEBHOOKS)) {
      log.debug("Webhooks dispatcher not supported by license.");
      return;
    }
    asyncEventBus.register(this);
  }

  @Override
  public void stop() {
    asyncEventBus.unregister(this);
  }

  @Override
  public void licenseChanged() {
    if (clmLicenseManager.hasFeature(Feature.WEBHOOKS)) {
      log.debug("Webhooks dispatcher supported by license.");
      start();
    } else {
      log.debug("Webhooks dispatcher not supported by license.");
      stop();
    }
  }
}
