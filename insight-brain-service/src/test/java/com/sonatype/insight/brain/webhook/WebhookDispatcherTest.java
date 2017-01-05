/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.webhook;

import java.util.Collections;
import java.util.Date;

import javax.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.dataaccess.configuration.webhook.WebhookDAO;
import com.sonatype.insight.brain.eventbus.AsyncEventBus;
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
import com.sonatype.insight.brain.service.AbstractComponentTest;
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
import com.sonatype.insight.brain.webhook.dto.PolicyManagementType;
import com.sonatype.insight.brain.webhook.dto.PolicyManagementPayload;
import com.sonatype.insight.brain.webhook.dto.SecurityVulnerabilityOverridePayload;
import com.sonatype.insight.brain.webhook.dto.SecurityVulnerabilityOverridePayload.SecurityVulnerabilityOverrideDTO;
import com.sonatype.insight.brain.webhook.dto.WebhookPayload;

import com.google.inject.Binder;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static com.sonatype.insight.brain.dataaccess.TemporaryEntity.WEBHOOK_SECRET_KEY_CLEAR;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

public class WebhookDispatcherTest
    extends AbstractComponentTest
{
  @Inject
  private WebhookDispatcher webhookDispatcher;

  @Inject
  private AsyncEventBus asyncEventBus;

  private WebhookClientUtil webhookClientUtil;

  @Before
  public void before() throws Exception {
    webhookDispatcher.start();
  }

  @After
  public void after() throws Exception {
    webhookDispatcher.stop();
  }

  @Override
  public void configure(Binder binder) {
    webhookClientUtil = mock(WebhookClientUtil.class);
    binder.bind(WebhookClientUtil.class).toInstance(webhookClientUtil);
  }

  @Test
  public void testOn_HandlesApplicationEvaluationEvent() {
    tempEntity.newWebhookWithSecret("http://localhost", Collections.singleton(WebhookEventType.APPLICATION_EVALUATION));

    Date date = new Date();
    ApplicationEvaluationEvent event = new ApplicationEvaluationEvent();
    event.initiator = "initiator";
    event.policyEvaluationId = "policyEvaluationId";
    event.stageTypeId = "stage";
    event.ownerId = "ownerId";
    event.evaluationDate = date;
    event.affectedComponentCount = 1;
    event.criticalComponentCount = 3;
    event.severeComponentCount = 5;
    event.moderateComponentCount = 7;
    event.outcome = "outcome";
    asyncEventBus.post(event);

    ArgumentCaptor<Webhook> webhookArgumentCaptor = ArgumentCaptor.forClass(Webhook.class);
    ArgumentCaptor<WebhookPayload> webhookPayloadArgumentCaptor = ArgumentCaptor.forClass(WebhookPayload.class);
    verify(webhookClientUtil, timeout(500).only())
        .post(webhookArgumentCaptor.capture(), eq(WebhookDispatcher.APPLICATION_EVALUATION_ID),
            webhookPayloadArgumentCaptor.capture());

    Webhook webhook = webhookArgumentCaptor.getValue();
    assertThat(webhook.getUrl(), is("http://localhost"));
    assertThat(webhook.getSecretKey(), is(WEBHOOK_SECRET_KEY_CLEAR));

    ApplicationEvaluationPayload webhookPayload = (ApplicationEvaluationPayload) webhookPayloadArgumentCaptor
        .getValue();
    assertThat(webhookPayload.initiator, is("initiator"));
    assertThat(webhookPayload.id, is("policyEvaluationId"));

    ApplicationEvaluationDTO applicationEvaluationDTO = webhookPayload.applicationEvaluation;
    assertThat(applicationEvaluationDTO.policyEvaluationId, is("policyEvaluationId"));
    assertThat(applicationEvaluationDTO.stage, is("stage"));
    assertThat(applicationEvaluationDTO.ownerId, is("ownerId"));
    assertThat(applicationEvaluationDTO.evaluationDate, is(date));
    assertThat(applicationEvaluationDTO.affectedComponentCount, is(1));
    assertThat(applicationEvaluationDTO.criticalComponentCount, is(3));
    assertThat(applicationEvaluationDTO.severeComponentCount, is(5));
    assertThat(applicationEvaluationDTO.moderateComponentCount, is(7));
    assertThat(applicationEvaluationDTO.outcome, is("outcome"));
  }

  @Test
  public void testOn_HandlesOwnerEvent() {
    tempEntity.newWebhookWithSecret("http://localhost", Collections.singleton(WebhookEventType.POLICY_MANAGEMENT));
    Organization organization = tempEntity.newOrganization();

    OwnerEvent event = new OwnerEvent();
    event.initiator = "initiator";
    event.ownerId = organization.getId();
    event.action = EventAction.CREATED;
    event.owner = organization;
    asyncEventBus.post(event);

    ArgumentCaptor<Webhook> webhookArgumentCaptor = ArgumentCaptor.forClass(Webhook.class);
    ArgumentCaptor<WebhookPayload> webhookPayloadArgumentCaptor = ArgumentCaptor.forClass(WebhookPayload.class);
    verify(webhookClientUtil, timeout(500).only())
        .post(webhookArgumentCaptor.capture(), eq(WebhookDispatcher.POLICY_MANAGEMENT_ID),
            webhookPayloadArgumentCaptor.capture());

    Webhook webhook = webhookArgumentCaptor.getValue();
    assertThat(webhook.getUrl(), is("http://localhost"));
    assertThat(webhook.getSecretKey(), is(WEBHOOK_SECRET_KEY_CLEAR));

    PolicyManagementPayload webhookPayload = (PolicyManagementPayload) webhookPayloadArgumentCaptor.getValue();
    assertThat(webhookPayload.initiator, is("initiator"));
    assertThat(webhookPayload.action, is(EventAction.CREATED));
    assertThat(webhookPayload.id, is(organization.getId()));
    assertThat(webhookPayload.type, is(PolicyManagementType.ORGANIZATION));
    assertThat(webhookPayload.owner.id, is(organization.getId()));
  }

  @Test
  public void testOn_HandlesTagEvent() {
    tempEntity.newWebhookWithSecret("http://localhost", Collections.singleton(WebhookEventType.POLICY_MANAGEMENT));
    Organization organization = tempEntity.newOrganization();
    Tag tag = tempEntity.newTag(organization.getId());

    TagEvent event = new TagEvent();
    event.initiator = "initiator";
    event.ownerId = organization.getId();
    event.action = EventAction.CREATED;
    event.tag = tag;
    asyncEventBus.post(event);

    ArgumentCaptor<Webhook> webhookArgumentCaptor = ArgumentCaptor.forClass(Webhook.class);
    ArgumentCaptor<WebhookPayload> webhookPayloadArgumentCaptor = ArgumentCaptor.forClass(WebhookPayload.class);
    verify(webhookClientUtil, timeout(500).only())
        .post(webhookArgumentCaptor.capture(), eq(WebhookDispatcher.POLICY_MANAGEMENT_ID),
            webhookPayloadArgumentCaptor.capture());

    Webhook webhook = webhookArgumentCaptor.getValue();
    assertThat(webhook.getUrl(), is("http://localhost"));
    assertThat(webhook.getSecretKey(), is(WEBHOOK_SECRET_KEY_CLEAR));

    PolicyManagementPayload webhookPayload = (PolicyManagementPayload) webhookPayloadArgumentCaptor.getValue();
    assertThat(webhookPayload.initiator, is("initiator"));
    assertThat(webhookPayload.action, is(EventAction.CREATED));
    assertThat(webhookPayload.id, is(tag.getId()));
    assertThat(webhookPayload.type, is(PolicyManagementType.APPLICATION_CATEGORY));
    assertThat(webhookPayload.owner.id, is(organization.getId()));
  }

  @Test
  public void testOn_HandlesLabelEvent() {
    tempEntity.newWebhookWithSecret("http://localhost", Collections.singleton(WebhookEventType.POLICY_MANAGEMENT));
    Organization organization = tempEntity.newOrganization();
    Label label = tempEntity.newLabel(organization.getId());

    LabelEvent event = new LabelEvent();
    event.initiator = "initiator";
    event.ownerId = organization.getId();
    event.action = EventAction.CREATED;
    event.label = label;
    asyncEventBus.post(event);

    ArgumentCaptor<Webhook> webhookArgumentCaptor = ArgumentCaptor.forClass(Webhook.class);
    ArgumentCaptor<WebhookPayload> webhookPayloadArgumentCaptor = ArgumentCaptor.forClass(WebhookPayload.class);
    verify(webhookClientUtil, timeout(500).only())
        .post(webhookArgumentCaptor.capture(), eq(WebhookDispatcher.POLICY_MANAGEMENT_ID),
            webhookPayloadArgumentCaptor.capture());

    Webhook webhook = webhookArgumentCaptor.getValue();
    assertThat(webhook.getUrl(), is("http://localhost"));
    assertThat(webhook.getSecretKey(), is(WEBHOOK_SECRET_KEY_CLEAR));

    PolicyManagementPayload webhookPayload = (PolicyManagementPayload) webhookPayloadArgumentCaptor.getValue();
    assertThat(webhookPayload.initiator, is("initiator"));
    assertThat(webhookPayload.action, is(EventAction.CREATED));
    assertThat(webhookPayload.id, is(label.getId()));
    assertThat(webhookPayload.type, is(PolicyManagementType.LABEL));
    assertThat(webhookPayload.owner.id, is(organization.getId()));
  }

  @Test
  public void testOn_HandlesLicenseThreatGroupEvent() {
    tempEntity.newWebhookWithSecret("http://localhost", Collections.singleton(WebhookEventType.POLICY_MANAGEMENT));
    Organization organization = tempEntity.newOrganization();
    LicenseThreatGroup licenseThreatGroup = tempEntity.newLicenseThreatGroup(organization.getId());

    LicenseThreatGroupEvent event = new LicenseThreatGroupEvent();
    event.initiator = "initiator";
    event.ownerId = organization.getId();
    event.action = EventAction.CREATED;
    event.licenseThreatGroup = licenseThreatGroup;
    asyncEventBus.post(event);

    ArgumentCaptor<Webhook> webhookArgumentCaptor = ArgumentCaptor.forClass(Webhook.class);
    ArgumentCaptor<WebhookPayload> webhookPayloadArgumentCaptor = ArgumentCaptor.forClass(WebhookPayload.class);
    verify(webhookClientUtil, timeout(500).only())
        .post(webhookArgumentCaptor.capture(), eq(WebhookDispatcher.POLICY_MANAGEMENT_ID),
            webhookPayloadArgumentCaptor.capture());

    Webhook webhook = webhookArgumentCaptor.getValue();
    assertThat(webhook.getUrl(), is("http://localhost"));
    assertThat(webhook.getSecretKey(), is(WEBHOOK_SECRET_KEY_CLEAR));

    PolicyManagementPayload webhookPayload = (PolicyManagementPayload) webhookPayloadArgumentCaptor.getValue();
    assertThat(webhookPayload.initiator, is("initiator"));
    assertThat(webhookPayload.action, is(EventAction.CREATED));
    assertThat(webhookPayload.id, is(licenseThreatGroup.getId()));
    assertThat(webhookPayload.type, is(PolicyManagementType.LICENSE_THREAT_GROUP));
    assertThat(webhookPayload.owner.id, is(organization.getId()));
  }

  @Test
  public void testOn_HandlesPolicyEvent() {
    tempEntity.newWebhookWithSecret("http://localhost", Collections.singleton(WebhookEventType.POLICY_MANAGEMENT));
    Organization organization = tempEntity.newOrganization();
    Policy policy = tempEntity.newPolicy(organization.getId(), "policy");

    PolicyEvent event = new PolicyEvent();
    event.initiator = "initiator";
    event.ownerId = organization.getId();
    event.action = EventAction.CREATED;
    event.policy = policy;
    asyncEventBus.post(event);

    ArgumentCaptor<Webhook> webhookArgumentCaptor = ArgumentCaptor.forClass(Webhook.class);
    ArgumentCaptor<WebhookPayload> webhookPayloadArgumentCaptor = ArgumentCaptor.forClass(WebhookPayload.class);
    verify(webhookClientUtil, timeout(500).only())
        .post(webhookArgumentCaptor.capture(), eq(WebhookDispatcher.POLICY_MANAGEMENT_ID),
            webhookPayloadArgumentCaptor.capture());

    Webhook webhook = webhookArgumentCaptor.getValue();
    assertThat(webhook.getUrl(), is("http://localhost"));
    assertThat(webhook.getSecretKey(), is(WEBHOOK_SECRET_KEY_CLEAR));

    PolicyManagementPayload webhookPayload = (PolicyManagementPayload) webhookPayloadArgumentCaptor.getValue();
    assertThat(webhookPayload.initiator, is("initiator"));
    assertThat(webhookPayload.action, is(EventAction.CREATED));
    assertThat(webhookPayload.id, is(policy.getId()));
    assertThat(webhookPayload.type, is(PolicyManagementType.POLICY));
    assertThat(webhookPayload.owner.id, is(organization.getId()));
  }

  @Test
  public void testOn_HandlesRoleEvent() {
    tempEntity.newWebhookWithSecret("http://localhost", Collections.singleton(WebhookEventType.POLICY_MANAGEMENT));
    Organization organization = tempEntity.newOrganization();

    RoleEvent event = new RoleEvent();
    event.initiator = "initiator";
    event.ownerId = organization.getId();
    event.action = EventAction.CREATED;
    asyncEventBus.post(event);

    ArgumentCaptor<Webhook> webhookArgumentCaptor = ArgumentCaptor.forClass(Webhook.class);
    ArgumentCaptor<WebhookPayload> webhookPayloadArgumentCaptor = ArgumentCaptor.forClass(WebhookPayload.class);
    verify(webhookClientUtil, timeout(500).only())
        .post(webhookArgumentCaptor.capture(), eq(WebhookDispatcher.POLICY_MANAGEMENT_ID),
            webhookPayloadArgumentCaptor.capture());

    Webhook webhook = webhookArgumentCaptor.getValue();
    assertThat(webhook.getUrl(), is("http://localhost"));
    assertThat(webhook.getSecretKey(), is(WEBHOOK_SECRET_KEY_CLEAR));

    PolicyManagementPayload webhookPayload = (PolicyManagementPayload) webhookPayloadArgumentCaptor.getValue();
    assertThat(webhookPayload.initiator, is("initiator"));
    assertThat(webhookPayload.action, is(EventAction.CREATED));
    assertThat(webhookPayload.id, is(organization.getId()));
    assertThat(webhookPayload.type, is(PolicyManagementType.ACCESS));
    assertThat(webhookPayload.owner.id, is(organization.getId()));
  }

  @Test
  public void testOn_SkipsNonConfiguredWebhooks() {
    WebhookDAO webhookDAO = new WebhookDAO();
    Webhook webhook = tempEntity
        .newWebhookWithSecret("http://localhost", Collections.singleton(WebhookEventType.APPLICATION_EVALUATION));
    asyncEventBus.post(new ManagementEvent());
    asyncEventBus.post(new SecurityVulnerabilityOverrideEvent());

    verify(webhookClientUtil, never()).post(any(Webhook.class), anyString(), any(WebhookPayload.class));
    webhookDAO.delete(webhook);

    webhook = tempEntity
        .newWebhookWithSecret("http://localhost", Collections.singleton(WebhookEventType.POLICY_MANAGEMENT));
    asyncEventBus.post(new ApplicationEvaluationEvent());
    asyncEventBus.post(new SecurityVulnerabilityOverrideEvent());

    verify(webhookClientUtil, never()).post(any(Webhook.class), anyString(), any(WebhookPayload.class));
    webhookDAO.delete(webhook);

    webhook = tempEntity.newWebhookWithSecret("http://localhost",
        Collections.singleton(WebhookEventType.SECURITY_VULNERABILITY_OVERRIDE_MANAGEMENT));
    asyncEventBus.post(new ApplicationEvaluationEvent());
    asyncEventBus.post(new ManagementEvent());

    verify(webhookClientUtil, never()).post(any(Webhook.class), anyString(), any(WebhookPayload.class));
    webhookDAO.delete(webhook);
  }

  @Test
  public void testOn_HandlesSecurityVulnerabilityOverrideEvent() {
    tempEntity.newWebhookWithSecret("http://localhost",
        Collections.singleton(WebhookEventType.SECURITY_VULNERABILITY_OVERRIDE_MANAGEMENT));
    Organization organization = tempEntity.newOrganization();
    SecurityVulnerabilityOverride override = tempEntity.newSecurityVulnerabilityOverride(organization.getId(),
        "some hash", "justin", "foo", SecurityVulnerabilityOverrideStatus.ACKNOWLEDGED, "this is for testing");

    SecurityVulnerabilityOverrideEvent event = new SecurityVulnerabilityOverrideEvent();
    event.initiator = "initiator";
    event.action = EventAction.UPDATED;
    event.override = override;

    asyncEventBus.post(event);

    ArgumentCaptor<Webhook> webhookArgumentCaptor = ArgumentCaptor.forClass(Webhook.class);
    ArgumentCaptor<WebhookPayload> webhookPayloadArgumentCaptor = ArgumentCaptor.forClass(WebhookPayload.class);
    verify(webhookClientUtil, timeout(500).only())
        .post(webhookArgumentCaptor.capture(), eq(WebhookDispatcher.SECURITY_VULNERABILITY_OVERRIDE_MANAGEMENT_ID),
            webhookPayloadArgumentCaptor.capture());

    Webhook webhook = webhookArgumentCaptor.getValue();
    assertThat(webhook.getUrl(), is("http://localhost"));
    assertThat(webhook.getSecretKey(), is(WEBHOOK_SECRET_KEY_CLEAR));

    SecurityVulnerabilityOverridePayload webhookPayload = (SecurityVulnerabilityOverridePayload) webhookPayloadArgumentCaptor
        .getValue();
    assertThat(webhookPayload.initiator, is("initiator"));
    assertThat(webhookPayload.action, is(EventAction.UPDATED));
    assertThat(webhookPayload.id, is(override.getId()));

    SecurityVulnerabilityOverrideDTO securityVulnerabilityOverrideDTO = webhookPayload.securityVulnerabilityOverride;
    assertThat(securityVulnerabilityOverrideDTO.id, is(override.getId()));
  }

  @Test
  public void testOn_HandlesLicenseOverrideEvent() {
    tempEntity
        .newWebhookWithSecret("http://localhost", Collections.singleton(WebhookEventType.LICENSE_OVERRIDE_MANAGEMENT));
    Organization organization = tempEntity.newOrganization();
    ComponentIdentifier mavenCoordinates = ComponentIdentifier.createMavenCoordinates("g", "a", "v");
    LicenseOverride givenOverride = tempEntity.newLicenseOverride(organization.getId(), mavenCoordinates,
        LicenseOverrideStatus.ACKNOWLEDGED, Collections.<String>emptySet());

    LicenseOverrideEvent event = new LicenseOverrideEvent();
    event.initiator = "initiator";
    event.licenseOverride = givenOverride;
    event.action = EventAction.UPDATED;

    asyncEventBus.post(event);

    ArgumentCaptor<Webhook> webhookArgumentCaptor = ArgumentCaptor.forClass(Webhook.class);
    ArgumentCaptor<WebhookPayload> webhookPayloadArgumentCaptor = ArgumentCaptor.forClass(WebhookPayload.class);
    verify(webhookClientUtil, timeout(500).only())
        .post(webhookArgumentCaptor.capture(), eq(WebhookDispatcher.LICENSE_OVERRIDE_MANAGEMENT_ID),
            webhookPayloadArgumentCaptor.capture());

    Webhook webhook = webhookArgumentCaptor.getValue();
    assertThat(webhook.getUrl(), is("http://localhost"));
    assertThat(webhook.getSecretKey(), is(WEBHOOK_SECRET_KEY_CLEAR));
    LicenseOverridePayload webhookPayload = (LicenseOverridePayload) webhookPayloadArgumentCaptor.getValue();
    assertThat(webhookPayload.initiator, is("initiator"));
    assertThat(webhookPayload.action, is(EventAction.UPDATED));
    assertThat(webhookPayload.id, is(givenOverride.getId()));
    LicenseOverrideDTO actualOverride = webhookPayload.licenseOverride;
    assertThat(actualOverride.id, is(givenOverride.getId()));
    assertThat(actualOverride.comment, is("testing"));
    assertThat(actualOverride.componentIdentifier, is(notNullValue()));
    assertThat(actualOverride.componentIdentifier.getFormat(), is(mavenCoordinates.getFormat()));
    assertThat(actualOverride.componentIdentifier.getCoordinates(), is(mavenCoordinates.getCoordinates()));
    assertThat(actualOverride.licenseIds, is(Matchers.<String>empty()));
    assertThat(actualOverride.ownerId, is(organization.getId()));
    assertThat(actualOverride.status, is(LicenseOverrideStatus.ACKNOWLEDGED.name()));
  }
}
