/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.webhook;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Locale;

import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.configuration.webhook.Webhook;
import com.sonatype.insight.brain.model.configuration.webhook.WebhookEventType;
import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.brain.model.license.LicenseOverride;
import com.sonatype.insight.brain.model.license.LicenseOverrideStatus;
import com.sonatype.insight.brain.model.license.LicenseThreatGroup;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.brain.model.vulnerability.SecurityVulnerabilityOverride;
import com.sonatype.insight.brain.model.vulnerability.SecurityVulnerabilityOverrideStatus;
import com.sonatype.insight.brain.service.AbstractComponentAuditTest;
import com.sonatype.insight.brain.webhook.ManagementEvent.LabelEvent;
import com.sonatype.insight.brain.webhook.ManagementEvent.LicenseThreatGroupEvent;
import com.sonatype.insight.brain.webhook.ManagementEvent.OwnerEvent;
import com.sonatype.insight.brain.webhook.ManagementEvent.PolicyEvent;
import com.sonatype.insight.brain.webhook.ManagementEvent.RoleEvent;
import com.sonatype.insight.brain.webhook.ManagementEvent.TagEvent;

import org.eclipse.jetty.ee11.servlet.ServletContextHandler;
import org.eclipse.jetty.ee11.servlet.ServletHolder;
import org.eclipse.jetty.server.Server;
import jakarta.servlet.http.HttpServlet;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class WebhookDispatcherAuditTest
    extends AbstractComponentAuditTest
{
  @Inject
  private WebhookDispatcher webhookDispatcher;

  private Server server;

  private volatile Integer webhookStatusCode = 200;

  @Before
  public void before() throws Exception {
    webhookDispatcher.start();
    server = new Server(0);
    ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
    context.setContextPath("/");
    context.addServlet(new ServletHolder(new HttpServlet()
    {
      @Override
      protected void service(HttpServletRequest request, HttpServletResponse response) {
        response.setStatus(webhookStatusCode);
      }
    }), "/*");
    server.setHandler(context);
    server.start();
  }

  @After
  public void after() throws Exception {
    if (server != null) {
      server.stop();
    }
    webhookDispatcher.stop();
  }

  @Test
  public void testOn_ApplicationEvaluationEvent() {
    Webhook webhook = tempEntity.newWebhookWithSecret(server.getURI().toString(),
        Collections.singleton(WebhookEventType.APPLICATION_EVALUATION));

    ApplicationEvaluationEvent event = new ApplicationEvaluationEvent();
    webhookDispatcher.on(event);

    AuditDTO auditDTO = assertAuditLog(AuditEvent.INVOKE_WEBHOOK, null, SYSTEM_USER);
    assertWebhookData(auditDTO, webhook, WebhookEventType.APPLICATION_EVALUATION, true);
  }

  @Test
  public void testOn_OwnerEvent() {
    Webhook webhook = tempEntity.newWebhookWithSecret(server.getURI().toString(),
        Collections.singleton(WebhookEventType.POLICY_MANAGEMENT));
    Organization organization = tempEntity.newOrganization();

    OwnerEvent event = new OwnerEvent();
    event.ownerId = organization.getId();
    event.owner = organization;
    webhookDispatcher.on(event);

    AuditDTO auditDTO = assertAuditLog(AuditEvent.INVOKE_WEBHOOK, null, SYSTEM_USER);
    assertWebhookData(auditDTO, webhook, WebhookEventType.POLICY_MANAGEMENT, true);
  }

  @Test
  public void testOn_TagEvent() {
    Webhook webhook = tempEntity.newWebhookWithSecret(server.getURI().toString(),
        Collections.singleton(WebhookEventType.POLICY_MANAGEMENT));
    Organization organization = tempEntity.newOrganization();
    Tag tag = tempEntity.newTag(organization.getId());

    TagEvent event = new TagEvent();
    event.ownerId = organization.getId();
    event.tag = tag;
    webhookDispatcher.on(event);

    AuditDTO auditDTO = assertAuditLog(AuditEvent.INVOKE_WEBHOOK, null, SYSTEM_USER);
    assertWebhookData(auditDTO, webhook, WebhookEventType.POLICY_MANAGEMENT, true);
  }

  @Test
  public void testOn_LabelEvent() {
    Webhook webhook = tempEntity.newWebhookWithSecret(server.getURI().toString(),
        Collections.singleton(WebhookEventType.POLICY_MANAGEMENT));
    Organization organization = tempEntity.newOrganization();
    Label label = tempEntity.newLabel(organization.getId());

    LabelEvent event = new LabelEvent();
    event.ownerId = organization.getId();
    event.label = label;
    webhookDispatcher.on(event);

    AuditDTO auditDTO = assertAuditLog(AuditEvent.INVOKE_WEBHOOK, null, SYSTEM_USER);
    assertWebhookData(auditDTO, webhook, WebhookEventType.POLICY_MANAGEMENT, true);
  }

  @Test
  public void testOn_LicenseThreatGroupEvent() {
    Webhook webhook = tempEntity.newWebhookWithSecret(server.getURI().toString(),
        Collections.singleton(WebhookEventType.POLICY_MANAGEMENT));
    Organization organization = tempEntity.newOrganization();
    LicenseThreatGroup licenseThreatGroup = tempEntity.newLicenseThreatGroup(organization.getId());

    LicenseThreatGroupEvent event = new LicenseThreatGroupEvent();
    event.ownerId = organization.getId();
    event.licenseThreatGroup = licenseThreatGroup;
    webhookDispatcher.on(event);

    AuditDTO auditDTO = assertAuditLog(AuditEvent.INVOKE_WEBHOOK, null, SYSTEM_USER);
    assertWebhookData(auditDTO, webhook, WebhookEventType.POLICY_MANAGEMENT, true);
  }

  @Test
  public void testOn_PolicyEvent() {
    Webhook webhook = tempEntity.newWebhookWithSecret(server.getURI().toString(),
        Collections.singleton(WebhookEventType.POLICY_MANAGEMENT));
    Organization organization = tempEntity.newOrganization();
    Policy policy = tempEntity.newPolicy(organization.getId());

    PolicyEvent event = new PolicyEvent();
    event.ownerId = organization.getId();
    event.policy = policy;
    webhookDispatcher.on(event);

    AuditDTO auditDTO = assertAuditLog(AuditEvent.INVOKE_WEBHOOK, null, SYSTEM_USER);
    assertWebhookData(auditDTO, webhook, WebhookEventType.POLICY_MANAGEMENT, true);
  }

  @Test
  public void testOn_RoleEvent() {
    Webhook webhook = tempEntity.newWebhookWithSecret(server.getURI().toString(),
        Collections.singleton(WebhookEventType.POLICY_MANAGEMENT));
    Organization organization = tempEntity.newOrganization();

    RoleEvent event = new RoleEvent();
    event.ownerId = organization.getId();
    webhookDispatcher.on(event);

    AuditDTO auditDTO = assertAuditLog(AuditEvent.INVOKE_WEBHOOK, null, SYSTEM_USER);
    assertWebhookData(auditDTO, webhook, WebhookEventType.POLICY_MANAGEMENT, true);
  }

  @Test
  public void testOn_SecurityVulnerabilityOverrideEvent() {
    Webhook webhook = tempEntity.newWebhookWithSecret(server.getURI().toString(),
        Collections.singleton(WebhookEventType.SECURITY_VULNERABILITY_OVERRIDE_MANAGEMENT));
    Organization organization = tempEntity.newOrganization();
    SecurityVulnerabilityOverride override = tempEntity.newSecurityVulnerabilityOverride(organization.getId(),
        "hash", "source", "refId", SecurityVulnerabilityOverrideStatus.ACKNOWLEDGED, "testing");

    SecurityVulnerabilityOverrideEvent event = new SecurityVulnerabilityOverrideEvent();
    event.override = override;
    webhookDispatcher.on(event);

    AuditDTO auditDTO = assertAuditLog(AuditEvent.INVOKE_WEBHOOK, null, SYSTEM_USER);
    assertWebhookData(auditDTO, webhook, WebhookEventType.SECURITY_VULNERABILITY_OVERRIDE_MANAGEMENT, true);
  }

  @Test
  public void testOn_WaiverRequestEvent() {
    Webhook webhook = tempEntity.newWebhookWithSecret(server.getURI().toString(),
        Collections.singleton(WebhookEventType.WAIVER_REQUEST));
    WaiverRequestEvent event = new WaiverRequestEvent();
    event.policyViolationId = "policyViolationId";
    event.timestamp = LocalDateTime.now();
    webhookDispatcher.on(event);

    AuditDTO auditDTO = assertAuditLog(AuditEvent.INVOKE_WEBHOOK, null, SYSTEM_USER);
    assertWebhookData(auditDTO, webhook, WebhookEventType.WAIVER_REQUEST, true);
  }

  @Test
  public void testOn_LicenseOverrideEvent() {
    Webhook webhook = tempEntity.newWebhookWithSecret(server.getURI().toString(),
        Collections.singleton(WebhookEventType.LICENSE_OVERRIDE_MANAGEMENT));
    Organization organization = tempEntity.newOrganization();
    ComponentIdentifier mavenCoordinates = ComponentIdentifier.createMavenCoordinates("g", "a", "v");
    LicenseOverride givenOverride = tempEntity.newLicenseOverride(organization.getId(), mavenCoordinates,
        LicenseOverrideStatus.ACKNOWLEDGED, Collections.emptySet());

    LicenseOverrideEvent event = new LicenseOverrideEvent();
    event.licenseOverride = givenOverride;
    webhookDispatcher.on(event);

    AuditDTO auditDTO = assertAuditLog(AuditEvent.INVOKE_WEBHOOK, null, SYSTEM_USER);
    assertWebhookData(auditDTO, webhook, WebhookEventType.LICENSE_OVERRIDE_MANAGEMENT, true);
  }

  @Test
  public void testOn_ServerError() {
    Webhook webhook = tempEntity.newWebhookWithSecret(server.getURI().toString(),
        Collections.singleton(WebhookEventType.POLICY_MANAGEMENT));

    RoleEvent event = new RoleEvent();
    // missing owner id generates an error
    assertThatExceptionOfType(RuntimeException.class).isThrownBy(() -> webhookDispatcher.on(event));

    AuditDTO auditDTO = assertAuditLog(AuditEvent.INVOKE_WEBHOOK, "server-error", SYSTEM_USER);
    assertWebhookData(auditDTO, webhook, WebhookEventType.POLICY_MANAGEMENT, false);
  }

  @Test
  public void testOn_WebhookClientError() {
    webhookStatusCode = 400;
    Webhook webhook = tempEntity.newWebhookWithSecret(server.getURI().toString(),
        Collections.singleton(WebhookEventType.APPLICATION_EVALUATION));

    ApplicationEvaluationEvent event = new ApplicationEvaluationEvent();
    webhookDispatcher.on(event);

    AuditDTO auditDTO = assertAuditLog(AuditEvent.INVOKE_WEBHOOK, "bad-request", SYSTEM_USER);
    assertWebhookData(auditDTO, webhook, WebhookEventType.APPLICATION_EVALUATION, true);
  }

  @Test
  public void testOn_WebhookClientException() throws Exception {
    String uri = server.getURI().toString();
    server.stop();
    Webhook webhook = tempEntity.newWebhookWithSecret(uri,
        Collections.singleton(WebhookEventType.APPLICATION_EVALUATION));

    ApplicationEvaluationEvent event = new ApplicationEvaluationEvent();
    webhookDispatcher.on(event);

    AuditDTO auditDTO = assertAuditLog(AuditEvent.INVOKE_WEBHOOK, "server-error", SYSTEM_USER);
    assertWebhookData(auditDTO, webhook, WebhookEventType.APPLICATION_EVALUATION, true);
  }

  private void assertWebhookData(
      final AuditDTO auditDTO,
      final Webhook webhook,
      WebhookEventType webhookEventType,
      boolean withDelivery)
  {
    assertCustomData(auditDTO, "webhookdId", webhook.getId());
    assertCustomData(auditDTO, "webhookUrl", webhook.getUrl());
    assertCustomData(auditDTO, "webhookTriggerEvent",
        webhookEventType.name().toLowerCase(Locale.ROOT).replace('_', '-'));
    if (withDelivery) {
      assertThat((String) auditDTO.data.get("webhookDeliveryId"))
          .matches("[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}");
    }
  }
}
