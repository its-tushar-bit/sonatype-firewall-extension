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

import com.sonatype.clm.dto.model.policy.ComponentFact;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.clm.dto.model.policy.PolicyFact;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentIdentifierDTOV2;
import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.AuditRecorder;
import com.sonatype.insight.brain.audit.AuditSession;
import com.sonatype.insight.brain.configuration.webhook.WebhookService;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.eventbus.AsyncEventBus;
import com.sonatype.insight.brain.features.LicensedFeature;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.configuration.webhook.Webhook;
import com.sonatype.insight.brain.model.configuration.webhook.WebhookEventType;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.brain.policy.ConstraintFactDTO;
import com.sonatype.insight.brain.product.license.ProductLicense;
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
import com.sonatype.insight.brain.webhook.dto.PolicyAlertPayload;
import com.sonatype.insight.brain.webhook.dto.PolicyAlertPayload.ComponentFactDTO;
import com.sonatype.insight.brain.webhook.dto.PolicyAlertPayload.PolicyAlertDTO;
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
    implements Managed
{
  public static final String APPLICATION_EVALUATION_ID = "iq:applicationEvaluation";

  public static final String POLICY_ALERT_ID = "iq:policyAlert";

  public static final String LICENSE_OVERRIDE_MANAGEMENT_ID = "iq:licenseOverrideManagement";

  public static final String SECURITY_VULNERABILITY_OVERRIDE_MANAGEMENT_ID =
      "iq:securityVulnerabilityOverrideManagement";

  public static final String POLICY_MANAGEMENT_ID = "iq:policyManagement";

  private static final Logger log = LoggerFactory.getLogger(WebhookDispatcher.class);

  private final WebhookService webhookService;

  private final WebhookClientUtil webhookClientUtil;

  private final AsyncEventBus asyncEventBus;

  private final OwnerDTOUtil ownerDTOUtil;

  private final AuditRecorder auditRecorder;

  private final ProductLicense productLicense;

  private final RepositoryDAO repositoryDAO;

  @Inject
  public WebhookDispatcher(final AsyncEventBus asyncEventBus,
                           final WebhookService webhookService,
                           final WebhookClientUtil webhookClientUtil,
                           final OwnerDTOUtil ownerDTOUtil,
                           final AuditRecorder auditRecorder,
                           final ProductLicense productLicense,
                           final RepositoryDAO repositoryDAO)
  {
    this.webhookService = webhookService;
    this.webhookClientUtil = webhookClientUtil;
    this.asyncEventBus = asyncEventBus;
    this.ownerDTOUtil = ownerDTOUtil;
    this.auditRecorder = auditRecorder;
    this.productLicense = productLicense;
    this.repositoryDAO = repositoryDAO;
  }

  @Subscribe
  public void on(final ApplicationEvaluationEvent applicationEvaluationEvent) {
    WebhookEventType webhookEventType = WebhookEventType.APPLICATION_EVALUATION;
    if (!checkEventIsLicensed(applicationEvaluationEvent.ownerId, webhookEventType)) {
      return;
    }
    for (Webhook webhook : getWebhooksOfEventType(webhookEventType)) {
      invokeWithAudit(webhook, webhookEventType,
          () -> sendApplicationEvaluationPayload(webhookService.getDecrypted(webhook.getId()),
              applicationEvaluationEvent));
    }
  }

  @Subscribe
  public void on(final OwnerEvent ownerEvent) {
    WebhookEventType webhookEventType = WebhookEventType.POLICY_MANAGEMENT;
    if (!checkEventIsLicensed(ownerEvent.ownerId, webhookEventType)) {
      return;
    }
    for (Webhook webhook : getWebhooksOfEventType(webhookEventType)) {
      PolicyManagementType type = ownerEvent.owner.getType() ==
          OwnerType.ORGANIZATION ? PolicyManagementType.ORGANIZATION : PolicyManagementType.APPLICATION;
      invokeWithAudit(webhook, webhookEventType,
          () -> sendPolicyManagementPayload(webhookService.getDecrypted(webhook.getId()), type,
              ownerEvent.owner.getId(),
              ownerEvent));
    }
  }

  @Subscribe
  public void on(final TagEvent tagEvent) {
    WebhookEventType webhookEventType = WebhookEventType.POLICY_MANAGEMENT;
    if (!checkEventIsLicensed(tagEvent.ownerId, webhookEventType)) {
      return;
    }
    for (Webhook webhook : getWebhooksOfEventType(webhookEventType)) {
      invokeWithAudit(webhook, webhookEventType,
          () -> sendPolicyManagementPayload(webhookService.getDecrypted(webhook.getId()),
              PolicyManagementType.APPLICATION_CATEGORY, tagEvent.tag.getId(), tagEvent));
    }
  }

  @Subscribe
  public void on(final LabelEvent labelEvent) {
    WebhookEventType webhookEventType = WebhookEventType.POLICY_MANAGEMENT;
    if (!checkEventIsLicensed(labelEvent.ownerId, webhookEventType)) {
      return;
    }
    for (Webhook webhook : getWebhooksOfEventType(webhookEventType)) {
      invokeWithAudit(webhook, webhookEventType,
          () -> sendPolicyManagementPayload(webhookService.getDecrypted(webhook.getId()), PolicyManagementType.LABEL,
              labelEvent.label.getId(), labelEvent));
    }
  }

  @Subscribe
  public void on(final LicenseThreatGroupEvent licenseThreatGroupEvent) {
    WebhookEventType webhookEventType = WebhookEventType.POLICY_MANAGEMENT;
    if (!checkEventIsLicensed(licenseThreatGroupEvent.ownerId, webhookEventType)) {
      return;
    }
    for (Webhook webhook : getWebhooksOfEventType(webhookEventType)) {
      invokeWithAudit(webhook, webhookEventType,
          () -> sendPolicyManagementPayload(webhookService.getDecrypted(webhook.getId()),
              PolicyManagementType.LICENSE_THREAT_GROUP, licenseThreatGroupEvent.licenseThreatGroup.getId(),
              licenseThreatGroupEvent));
    }
  }

  @Subscribe
  public void on(final PolicyEvent policyEvent) {
    WebhookEventType webhookEventType = WebhookEventType.POLICY_MANAGEMENT;
    if (!checkEventIsLicensed(policyEvent.ownerId, webhookEventType)) {
      return;
    }
    for (Webhook webhook : getWebhooksOfEventType(webhookEventType)) {
      invokeWithAudit(webhook, webhookEventType,
          () -> sendPolicyManagementPayload(webhookService.getDecrypted(webhook.getId()), PolicyManagementType.POLICY,
              policyEvent.policy.getId(), policyEvent));
    }
  }

  @Subscribe
  public void on(final RoleEvent roleEvent) {
    WebhookEventType webhookEventType = WebhookEventType.POLICY_MANAGEMENT;
    if (!checkEventIsLicensed(roleEvent.ownerId, webhookEventType)) {
      return;
    }
    for (Webhook webhook : getWebhooksOfEventType(webhookEventType)) {
      invokeWithAudit(webhook, webhookEventType,
          () -> sendPolicyManagementPayload(webhookService.getDecrypted(webhook.getId()), PolicyManagementType.ACCESS,
              roleEvent.ownerId, roleEvent));
    }
  }

  @Subscribe
  public void on(final SecurityVulnerabilityOverrideEvent securityVulnerabilityOverrideEvent) {
    WebhookEventType webhookEventType = WebhookEventType.SECURITY_VULNERABILITY_OVERRIDE_MANAGEMENT;
    if (!checkEventIsLicensed(securityVulnerabilityOverrideEvent.override.getOwnerId(), webhookEventType)) {
      return;
    }
    for (Webhook webhook : getWebhooksOfEventType(webhookEventType)) {
      invokeWithAudit(webhook, webhookEventType,
          () -> sendSecurityVulnerabilityOverridePayload(webhookService.getDecrypted(webhook.getId()),
              securityVulnerabilityOverrideEvent));
    }
  }

  @Subscribe
  public void on(final LicenseOverrideEvent licenseOverrideEvent) {
    WebhookEventType webhookEventType = WebhookEventType.LICENSE_OVERRIDE_MANAGEMENT;
    if (!checkEventIsLicensed(licenseOverrideEvent.licenseOverride.getOwnerId(), webhookEventType)) {
      return;
    }
    for (Webhook webhook : getWebhooksOfEventType(webhookEventType)) {
      invokeWithAudit(webhook, webhookEventType,
          () -> sendLicenseOverridePayload(webhookService.getDecrypted(webhook.getId()), licenseOverrideEvent));
    }
  }

  @Subscribe
  public void on(final PolicyAlertEvent policyAlertEvent) {
    WebhookEventType webhookEventType = WebhookEventType.POLICY_ALERT;
    if (!checkEventIsLicensed(policyAlertEvent.application.id, webhookEventType)) {
      return;
    }

    // PolicyAlertEvents must be configured both as Webhook configuration and Policy Notification
    for (Webhook webhook : getWebhooksOfEventType(WebhookEventType.POLICY_ALERT)) {
      if (webhook.getId().equals(policyAlertEvent.targetId)) {
        invokeWithAudit(webhook, webhookEventType,
            () -> sendPolicyAlertPayload(webhookService.getDecrypted(webhook.getId()), policyAlertEvent));
      }
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

  private void sendPolicyAlertPayload(final Webhook webhook, final PolicyAlertEvent event) {

    final PolicyAlertPayload payload = new PolicyAlertPayload();
    payload.applicationEvaluation.policyEvaluationId = event.applicationEvaluation.policyEvaluationId;
    payload.applicationEvaluation.evaluationDate = event.applicationEvaluation.evaluationDate;
    payload.applicationEvaluation.affectedComponentCount = event.applicationEvaluation.affectedComponentCount;
    payload.applicationEvaluation.criticalComponentCount = event.applicationEvaluation.criticalComponentCount;
    payload.applicationEvaluation.severeComponentCount = event.applicationEvaluation.severeComponentCount;
    payload.applicationEvaluation.moderateComponentCount = event.applicationEvaluation.moderateComponentCount;
    payload.applicationEvaluation.outcome = event.applicationEvaluation.outcome;
    payload.applicationEvaluation.reportId = event.applicationEvaluation.reportId;

    payload.application.id = event.application.id;
    payload.application.name = event.application.name;
    payload.application.organizationId = event.application.organizationId;

    payload.initiator = event.initiator;

    for (final PolicyFact policyFact : event.policyFacts) {
      final PolicyAlertDTO policyAlertDTO = new PolicyAlertDTO();
      policyAlertDTO.policyId = policyFact.getPolicyId();
      policyAlertDTO.policyName = policyFact.getPolicyName();
      policyAlertDTO.threatLevel = policyFact.getThreatLevel();
      policyAlertDTO.policyViolationId = policyFact.getPolicyViolationId();

      for (final ComponentFact componentFact : policyFact.getComponentFacts()) {
        final ComponentFactDTO componentFactDTO = new ComponentFactDTO();
        componentFactDTO.hash = componentFact.getHash();
        if (componentFact.getDisplayName() != null) {
          componentFactDTO.displayName = componentFact.getDisplayName().toString();
        }
        else {
          componentFactDTO.displayName = "Unknown Component";
        }
        if (componentFact.getComponentIdentifier() != null) {
          componentFactDTO.componentIdentifier = ApiComponentIdentifierDTOV2
              .fromComponentIdentifier(componentFact.getComponentIdentifier());
        }
        componentFactDTO.pathNames = componentFact.getPathnames();

        for (final ConstraintFact constraintFact : componentFact.getConstraintFacts()) {
          componentFactDTO.constraintFacts.add(new ConstraintFactDTO(constraintFact));
        }

        policyAlertDTO.componentFacts.add(componentFactDTO);
      }
      payload.policyAlerts.add(policyAlertDTO);
    }

    webhookClientUtil.post(webhook, POLICY_ALERT_ID, payload);
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
    asyncEventBus.register(this);
  }

  @Override
  public void stop() {
    asyncEventBus.unregister(this);
  }

  private boolean checkEventIsLicensed(final String ownerId, final WebhookEventType webhookEventType) {
    boolean eventApplicableToRepos = Organization.ROOT_ORGANIZATION_ID.equals(ownerId) ||
        RepositoryContainer.REPOSITORY_CONTAINER_ID.equals(ownerId) || repositoryDAO.getById(ownerId) != null;
    boolean eventApplicableToApps = Organization.ROOT_ORGANIZATION_ID.equals(ownerId) || !eventApplicableToRepos;

    if (eventApplicableToRepos && productLicense.hasFeature(LicensedFeature.WEBHOOKS_FOR_REPOSITORIES)) {
      return true;
    }
    if (eventApplicableToApps && productLicense.hasFeature(LicensedFeature.WEBHOOKS_FOR_APPLICATIONS)) {
      return true;
    }

    log.debug("Webhooks feature for event {} is not supported by the current license.", webhookEventType);
    return false;
  }
}
