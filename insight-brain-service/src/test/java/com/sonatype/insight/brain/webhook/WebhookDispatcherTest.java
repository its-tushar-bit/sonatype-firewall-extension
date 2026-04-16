/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.webhook;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import jakarta.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.ComponentFact;
import com.sonatype.clm.dto.model.policy.PolicyFact;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentIdentifierDTOV2;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.configuration.webhook.WebhookDAO;
import com.sonatype.insight.brain.eventbus.AsyncEventBus;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.configuration.webhook.Webhook;
import com.sonatype.insight.brain.model.configuration.webhook.WebhookEventType;
import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.brain.model.license.LicenseOverride;
import com.sonatype.insight.brain.model.license.LicenseOverrideStatus;
import com.sonatype.insight.brain.model.license.LicenseThreatGroup;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.brain.model.vulnerability.SecurityVulnerabilityOverride;
import com.sonatype.insight.brain.model.vulnerability.SecurityVulnerabilityOverrideStatus;
import com.sonatype.insight.brain.product.license.TestProductLicense;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.brain.webhook.ManagementEvent.LabelEvent;
import com.sonatype.insight.brain.webhook.ManagementEvent.LicenseThreatGroupEvent;
import com.sonatype.insight.brain.webhook.ManagementEvent.OwnerEvent;
import com.sonatype.insight.brain.webhook.ManagementEvent.PolicyEvent;
import com.sonatype.insight.brain.webhook.ManagementEvent.RoleEvent;
import com.sonatype.insight.brain.webhook.ManagementEvent.TagEvent;
import com.sonatype.insight.brain.webhook.dto.ApplicationEvaluationPayload;
import com.sonatype.insight.brain.webhook.dto.ApplicationEvaluationPayload.ApplicationEvaluationDTO;
import com.sonatype.insight.brain.webhook.dto.ApplicationSummary;
import com.sonatype.insight.brain.webhook.dto.ContainerEvaluationPayload;
import com.sonatype.insight.brain.webhook.dto.ContainerEvaluationPayload.ContainerEvaluationDTO;
import com.sonatype.insight.brain.webhook.dto.LicenseOverridePayload;
import com.sonatype.insight.brain.webhook.dto.LicenseOverridePayload.LicenseOverrideDTO;
import com.sonatype.insight.brain.webhook.dto.OrganizationApplicationSummaryPayload;
import com.sonatype.insight.brain.webhook.dto.OrganizationSummary;
import com.sonatype.insight.brain.webhook.dto.PolicyAlertPayload;
import com.sonatype.insight.brain.webhook.dto.PolicyAlertPayload.PolicyAlertDTO;
import com.sonatype.insight.brain.webhook.dto.PolicyManagementPayload;
import com.sonatype.insight.brain.webhook.dto.PolicyManagementType;
import com.sonatype.insight.brain.webhook.dto.SecurityVulnerabilityOverridePayload;
import com.sonatype.insight.brain.webhook.dto.SecurityVulnerabilityOverridePayload.SecurityVulnerabilityOverrideDTO;
import com.sonatype.insight.brain.webhook.dto.WaiverRequestPayload;
import com.sonatype.insight.brain.webhook.dto.WebhookPayload;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

import com.google.inject.Binder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import static com.sonatype.clm.dto.model.component.ComponentIdentifier.createMavenCoordinates;
import static com.sonatype.insight.brain.dataaccess.TemporaryEntity.WEBHOOK_SECRET_KEY_CLEAR;
import static com.sonatype.insight.brain.webhook.WebhookDispatcher.JIRA_CLOUD_PLUGIN_TELEMETRY_URL_IDENTIFIER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class WebhookDispatcherTest
    extends AbstractComponentTest
{
  private static final int EVENT_TIMEOUT_MS = 5000;

  @Inject
  private WebhookDAO webhookDAO;

  @Inject
  private OrganizationDAO organizationDAO;

  @Inject
  private WebhookDispatcher webhookDispatcher;

  @Inject
  private AsyncEventBus asyncEventBus;

  @Inject
  private TestProductLicense testProductLicense;

  @Mock
  private WebhookClientUtil webhookClientUtil;

  @Mock
  private TelemetrySender telemetrySender;

  @Before
  public void before() {
    webhookDispatcher.start();
  }

  @After
  public void after() {
    webhookDispatcher.stop();
  }

  @Override
  public void configure(Binder binder) {
    binder.bind(WebhookClientUtil.class).toInstance(webhookClientUtil);
    binder.bind(TelemetrySender.class).toInstance(telemetrySender);
    super.configure(binder);
  }

  @Test
  public void testOn_HandlesApplicationEvaluationEvent() {
    Webhook createdWebhook = tempEntity.newWebhookWithSecret("http://localhost",
        Collections.singleton(WebhookEventType.APPLICATION_EVALUATION));
    createdWebhook.setContext(Webhook.CONTEXT_LIFECYCLE);
    webhookDAO.update(createdWebhook);

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
    event.reportId = "reportId";
    event.isForLatestScan = true;

    event.application.id = "ownerId";
    event.application.publicId = "app-public-id";
    event.application.name = "app-name";
    event.application.organizationId = "org-id";

    asyncEventBus.post(event);

    ArgumentCaptor<Webhook> webhookArgumentCaptor = ArgumentCaptor.forClass(Webhook.class);
    ArgumentCaptor<WebhookPayload> webhookPayloadArgumentCaptor = ArgumentCaptor.forClass(WebhookPayload.class);
    verify(webhookClientUtil, timeout(EVENT_TIMEOUT_MS).only())
        .post(webhookArgumentCaptor.capture(), eq(WebhookEventType.APPLICATION_EVALUATION.getId()),
            webhookPayloadArgumentCaptor.capture());

    Webhook webhook = webhookArgumentCaptor.getValue();
    assertThat(webhook.getUrl()).isEqualTo("http://localhost");
    assertThat(webhook.getSecretKey()).isEqualTo(WEBHOOK_SECRET_KEY_CLEAR);

    ApplicationEvaluationPayload webhookPayload = (ApplicationEvaluationPayload) webhookPayloadArgumentCaptor
        .getValue();
    assertThat(webhookPayload.initiator).isEqualTo("initiator");
    assertThat(webhookPayload.id).isEqualTo("policyEvaluationId");

    ApplicationEvaluationDTO applicationEvaluationDTO = webhookPayload.applicationEvaluation;
    assertThat(applicationEvaluationDTO.policyEvaluationId).isEqualTo("policyEvaluationId");
    assertThat(applicationEvaluationDTO.stage).isEqualTo("stage");
    assertThat(applicationEvaluationDTO.ownerId).isEqualTo("ownerId");
    assertThat(applicationEvaluationDTO.evaluationDate).isEqualTo(date);
    assertThat(applicationEvaluationDTO.affectedComponentCount).isEqualTo(1);
    assertThat(applicationEvaluationDTO.criticalComponentCount).isEqualTo(3);
    assertThat(applicationEvaluationDTO.severeComponentCount).isEqualTo(5);
    assertThat(applicationEvaluationDTO.moderateComponentCount).isEqualTo(7);
    assertThat(applicationEvaluationDTO.outcome).isEqualTo("outcome");
    assertThat(applicationEvaluationDTO.reportId).isEqualTo("reportId");
    assertThat(applicationEvaluationDTO.isForLatestScan).isEqualTo(true);

    assertThat(applicationEvaluationDTO.application.id).isEqualTo("ownerId");
    assertThat(applicationEvaluationDTO.application.publicId).isEqualTo("app-public-id");
    assertThat(applicationEvaluationDTO.application.name).isEqualTo("app-name");
    assertThat(applicationEvaluationDTO.application.organizationId).isEqualTo("org-id");
  }

  @Test
  public void testOn_HandlesApplicationEvaluationEvent_FirewallContext() {
    tempEntity.newWebhookWithSecret("http://localhost", Collections.singleton(WebhookEventType.APPLICATION_EVALUATION));

    Date date = new Date();
    ApplicationEvaluationEvent event = new ApplicationEvaluationEvent();
    event.initiator = "admin";
    event.policyEvaluationId = "policyEvaluationId";
    event.stageTypeId = "proxy"; // Firewall proxy stage
    event.ownerId = "repositoryId";
    event.evaluationDate = date;
    event.affectedComponentCount = 10;
    event.criticalComponentCount = 2;
    event.severeComponentCount = 5;
    event.moderateComponentCount = 3;
    event.outcome = "fail";
    event.reportId = "reportId";
    event.isForLatestScan = true;

    event.application.id = "repositoryId";
    event.application.publicId = "docker-proxy";
    event.application.name = "host.docker.internal_8013-docker-proxy";
    event.application.organizationId = "org-id";

    asyncEventBus.post(event);

    ArgumentCaptor<WebhookPayload> webhookPayloadArgumentCaptor = ArgumentCaptor.forClass(WebhookPayload.class);
    verify(webhookClientUtil, timeout(EVENT_TIMEOUT_MS).only())
        .post(any(Webhook.class), eq(WebhookEventType.APPLICATION_EVALUATION.getId()),
            webhookPayloadArgumentCaptor.capture());

    // Verify Firewall context sends ContainerEvaluationPayload with container-specific fields
    ContainerEvaluationPayload webhookPayload = (ContainerEvaluationPayload) webhookPayloadArgumentCaptor
        .getValue();

    ContainerEvaluationDTO containerEvaluationDTO = webhookPayload.containerEvaluation;
    assertThat(containerEvaluationDTO.stage).isEqualTo("proxy");
    assertThat(containerEvaluationDTO.repository.name).isEqualTo("host.docker.internal_8013-docker-proxy");
  }

  @Test
  public void test_WebhooksAppAndRepoLicensed_SendsAllEvents() {
    testProductLicense.setFeatures(LicensedFeature.WEBHOOKS_FOR_APPLICATIONS,
        LicensedFeature.WEBHOOKS_FOR_REPOSITORIES);

    Organization organization = tempEntity.newOrganization();
    Organization rootOrg = organizationDAO.getById(Organization.ROOT_ORGANIZATION_ID);
    Application application = tempEntity.newApplication(organization.getId());
    Repository repository = tempEntity.newRepository();

    tempEntity.newWebhookWithSecret("http://localhost", EnumSet.allOf(WebhookEventType.class));

    testEventTypesWithOwner(rootOrg);
    verifyEventTypesSent();

    testEventTypesWithOwner(organization);
    verifyEventTypesSent();

    testEventTypesWithOwner(application);
    verifyEventTypesSent();

    testEventTypesWithOwner(repository);
    verifyEventTypesSent();
  }

  @Test
  public void test_WebhooksNotLicensed_DoesNotSendEvents() {
    testProductLicense.setMissingFeatures(LicensedFeature.WEBHOOKS_FOR_APPLICATIONS,
        LicensedFeature.WEBHOOKS_FOR_REPOSITORIES);

    Organization organization = tempEntity.newOrganization();
    Organization rootOrg = organizationDAO.getById(Organization.ROOT_ORGANIZATION_ID);
    Application application = tempEntity.newApplication(organization.getId());
    Repository repository = tempEntity.newRepository();

    tempEntity.newWebhookWithSecret("http://localhost", EnumSet.allOf(WebhookEventType.class));

    testEventTypesWithOwner(rootOrg);
    testEventTypesWithOwner(organization);
    testEventTypesWithOwner(application);
    testEventTypesWithOwner(repository);

    verifyNoInteractions(webhookClientUtil);
  }

  @Test
  public void test_WebhooksRepositoryLicensed_SendsOnlyRepositoryEvents() {
    testProductLicense.setFeatures(LicensedFeature.WEBHOOKS_FOR_REPOSITORIES);

    Organization organization = tempEntity.newOrganization();
    Organization rootOrg = organizationDAO.getById(Organization.ROOT_ORGANIZATION_ID);
    Application application = tempEntity.newApplication(organization.getId());
    Repository repository = tempEntity.newRepository();

    Webhook webhook = tempEntity.newWebhookWithSecret("http://localhost", EnumSet.allOf(WebhookEventType.class));
    webhook.setContext(Webhook.CONTEXT_FIREWALL);
    webhookDAO.update(webhook);

    testEventTypesWithOwner(rootOrg);
    verifyEventTypesSent();

    testEventTypesWithOwner(repository);
    verifyEventTypesSent();

    testEventTypesWithOwner(organization);
    verifyNoMoreInteractions(webhookClientUtil);

    testEventTypesWithOwner(application);
    verifyNoMoreInteractions(webhookClientUtil);
  }

  @Test
  public void test_ApplicationWebhooksLicensed_SendsOnlyApplicationEvents() {
    testProductLicense.setFeatures(LicensedFeature.WEBHOOKS_FOR_APPLICATIONS);

    Organization rootOrg = organizationDAO.getById(Organization.ROOT_ORGANIZATION_ID);
    Organization organization = tempEntity.newOrganization();
    Application application = tempEntity.newApplication(organization.getId());
    Repository repository = tempEntity.newRepository();

    Webhook webhook = tempEntity.newWebhookWithSecret("http://localhost", EnumSet.allOf(WebhookEventType.class));
    webhook.setContext(Webhook.CONTEXT_LIFECYCLE);
    webhookDAO.update(webhook);

    testEventTypesWithOwner(rootOrg);
    verifyEventTypesSent();

    testEventTypesWithOwner(organization);
    verifyEventTypesSent();

    testEventTypesWithOwner(application);
    verifyEventTypesSent();

    testEventTypesWithOwner(repository);
    verifyNoMoreInteractions(webhookClientUtil);
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
    verify(webhookClientUtil, timeout(EVENT_TIMEOUT_MS).only())
        .post(webhookArgumentCaptor.capture(), eq(WebhookEventType.POLICY_MANAGEMENT.getId()),
            webhookPayloadArgumentCaptor.capture());

    Webhook webhook = webhookArgumentCaptor.getValue();
    assertThat(webhook.getUrl()).isEqualTo("http://localhost");
    assertThat(webhook.getSecretKey()).isEqualTo(WEBHOOK_SECRET_KEY_CLEAR);

    PolicyManagementPayload webhookPayload = (PolicyManagementPayload) webhookPayloadArgumentCaptor.getValue();
    assertThat(webhookPayload.initiator).isEqualTo("initiator");
    assertThat(webhookPayload.action).isEqualTo(EventAction.CREATED);
    assertThat(webhookPayload.id).isEqualTo(organization.getId());
    assertThat(webhookPayload.type).isEqualTo(PolicyManagementType.ORGANIZATION);
    assertThat(webhookPayload.owner.id).isEqualTo(organization.getId());
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
    verify(webhookClientUtil, timeout(EVENT_TIMEOUT_MS).only())
        .post(webhookArgumentCaptor.capture(), eq(WebhookEventType.POLICY_MANAGEMENT.getId()),
            webhookPayloadArgumentCaptor.capture());

    Webhook webhook = webhookArgumentCaptor.getValue();
    assertThat(webhook.getUrl()).isEqualTo("http://localhost");
    assertThat(webhook.getSecretKey()).isEqualTo(WEBHOOK_SECRET_KEY_CLEAR);

    PolicyManagementPayload webhookPayload = (PolicyManagementPayload) webhookPayloadArgumentCaptor.getValue();
    assertThat(webhookPayload.initiator).isEqualTo("initiator");
    assertThat(webhookPayload.action).isEqualTo(EventAction.CREATED);
    assertThat(webhookPayload.id).isEqualTo(tag.getId());
    assertThat(webhookPayload.type).isEqualTo(PolicyManagementType.APPLICATION_CATEGORY);
    assertThat(webhookPayload.owner.id).isEqualTo(organization.getId());
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
    verify(webhookClientUtil, timeout(EVENT_TIMEOUT_MS).only())
        .post(webhookArgumentCaptor.capture(), eq(WebhookEventType.POLICY_MANAGEMENT.getId()),
            webhookPayloadArgumentCaptor.capture());

    Webhook webhook = webhookArgumentCaptor.getValue();
    assertThat(webhook.getUrl()).isEqualTo("http://localhost");
    assertThat(webhook.getSecretKey()).isEqualTo(WEBHOOK_SECRET_KEY_CLEAR);

    PolicyManagementPayload webhookPayload = (PolicyManagementPayload) webhookPayloadArgumentCaptor.getValue();
    assertThat(webhookPayload.initiator).isEqualTo("initiator");
    assertThat(webhookPayload.action).isEqualTo(EventAction.CREATED);
    assertThat(webhookPayload.id).isEqualTo(label.getId());
    assertThat(webhookPayload.type).isEqualTo(PolicyManagementType.LABEL);
    assertThat(webhookPayload.owner.id).isEqualTo(organization.getId());
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
    verify(webhookClientUtil, timeout(EVENT_TIMEOUT_MS).only())
        .post(webhookArgumentCaptor.capture(), eq(WebhookEventType.POLICY_MANAGEMENT.getId()),
            webhookPayloadArgumentCaptor.capture());

    Webhook webhook = webhookArgumentCaptor.getValue();
    assertThat(webhook.getUrl()).isEqualTo("http://localhost");
    assertThat(webhook.getSecretKey()).isEqualTo(WEBHOOK_SECRET_KEY_CLEAR);

    PolicyManagementPayload webhookPayload = (PolicyManagementPayload) webhookPayloadArgumentCaptor.getValue();
    assertThat(webhookPayload.initiator).isEqualTo("initiator");
    assertThat(webhookPayload.action).isEqualTo(EventAction.CREATED);
    assertThat(webhookPayload.id).isEqualTo(licenseThreatGroup.getId());
    assertThat(webhookPayload.type).isEqualTo(PolicyManagementType.LICENSE_THREAT_GROUP);
    assertThat(webhookPayload.owner.id).isEqualTo(organization.getId());
  }

  @Test
  public void testOn_HandlesPolicyEvent() {
    tempEntity.newWebhookWithSecret("http://localhost", Collections.singleton(WebhookEventType.POLICY_MANAGEMENT));
    Organization organization = tempEntity.newOrganization();
    Policy policy = tempEntity.newPolicy(organization);

    PolicyEvent event = new PolicyEvent();
    event.initiator = "initiator";
    event.ownerId = organization.getId();
    event.action = EventAction.CREATED;
    event.policy = policy;
    asyncEventBus.post(event);

    ArgumentCaptor<Webhook> webhookArgumentCaptor = ArgumentCaptor.forClass(Webhook.class);
    ArgumentCaptor<WebhookPayload> webhookPayloadArgumentCaptor = ArgumentCaptor.forClass(WebhookPayload.class);
    verify(webhookClientUtil, timeout(EVENT_TIMEOUT_MS).only())
        .post(webhookArgumentCaptor.capture(), eq(WebhookEventType.POLICY_MANAGEMENT.getId()),
            webhookPayloadArgumentCaptor.capture());

    Webhook webhook = webhookArgumentCaptor.getValue();
    assertThat(webhook.getUrl()).isEqualTo("http://localhost");
    assertThat(webhook.getSecretKey()).isEqualTo(WEBHOOK_SECRET_KEY_CLEAR);

    PolicyManagementPayload webhookPayload = (PolicyManagementPayload) webhookPayloadArgumentCaptor.getValue();
    assertThat(webhookPayload.initiator).isEqualTo("initiator");
    assertThat(webhookPayload.action).isEqualTo(EventAction.CREATED);
    assertThat(webhookPayload.id).isEqualTo(policy.getId());
    assertThat(webhookPayload.type).isEqualTo(PolicyManagementType.POLICY);
    assertThat(webhookPayload.owner.id).isEqualTo(organization.getId());
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
    verify(webhookClientUtil, timeout(EVENT_TIMEOUT_MS).only())
        .post(webhookArgumentCaptor.capture(), eq(WebhookEventType.POLICY_MANAGEMENT.getId()),
            webhookPayloadArgumentCaptor.capture());

    Webhook webhook = webhookArgumentCaptor.getValue();
    assertThat(webhook.getUrl()).isEqualTo("http://localhost");
    assertThat(webhook.getSecretKey()).isEqualTo(WEBHOOK_SECRET_KEY_CLEAR);

    PolicyManagementPayload webhookPayload = (PolicyManagementPayload) webhookPayloadArgumentCaptor.getValue();
    assertThat(webhookPayload.initiator).isEqualTo("initiator");
    assertThat(webhookPayload.action).isEqualTo(EventAction.CREATED);
    assertThat(webhookPayload.id).isEqualTo(organization.getId());
    assertThat(webhookPayload.type).isEqualTo(PolicyManagementType.ACCESS);
    assertThat(webhookPayload.owner.id).isEqualTo(organization.getId());
  }

  @Test
  public void testOn_HandlesPolicyAlertEvent() {
    Webhook target =
        tempEntity.newWebhookWithSecret("http://localhost", Collections.singleton(WebhookEventType.POLICY_ALERT));
    Organization organization = tempEntity.newOrganization();
    Application application = tempEntity.newApplication(organization.getId());

    Date date = new Date();
    ApplicationEvaluationEvent evaluationEvent = new ApplicationEvaluationEvent();
    evaluationEvent.initiator = "initiator";
    evaluationEvent.policyEvaluationId = "policyEvaluationId";
    evaluationEvent.stageTypeId = "stage";
    evaluationEvent.ownerId = "ownerId";
    evaluationEvent.evaluationDate = date;
    evaluationEvent.affectedComponentCount = 1;
    evaluationEvent.criticalComponentCount = 3;
    evaluationEvent.severeComponentCount = 5;
    evaluationEvent.moderateComponentCount = 7;
    evaluationEvent.outcome = "outcome";
    evaluationEvent.reportId = "reportId";
    evaluationEvent.isForLatestScan = true;
    PolicyAlertEvent event = new PolicyAlertEvent(target.getId());
    event.initiator = "initiator";
    event.targetId = target.getId();
    event.application = new ApplicationSummary();
    event.application.id = application.getId();
    event.application.publicId = application.getPublicId();
    event.application.name = application.getName();
    event.application.organizationId = organization.getId();
    event.applicationEvaluation = evaluationEvent;
    PolicyFact policyFact = new PolicyFact("policyId", "name", 5, "policyViolationId");
    ComponentIdentifier mavenCoordinates = createMavenCoordinates("com.group", "artifact", "1.0", "test", "jar");
    policyFact.addComponentFact(new ComponentFact(mavenCoordinates, "123"));
    event.policyFacts.add(policyFact);
    asyncEventBus.post(event);

    ArgumentCaptor<Webhook> webhookArgumentCaptor = ArgumentCaptor.forClass(Webhook.class);
    ArgumentCaptor<WebhookPayload> webhookPayloadArgumentCaptor = ArgumentCaptor.forClass(WebhookPayload.class);
    verify(webhookClientUtil, timeout(EVENT_TIMEOUT_MS).only())
        .post(webhookArgumentCaptor.capture(), eq(WebhookEventType.POLICY_ALERT.getId()),
            webhookPayloadArgumentCaptor.capture());

    Webhook webhook = webhookArgumentCaptor.getValue();
    assertThat(webhook.getUrl()).isEqualTo("http://localhost");
    assertThat(webhook.getSecretKey()).isEqualTo(WEBHOOK_SECRET_KEY_CLEAR);

    PolicyAlertPayload webhookPayload = (PolicyAlertPayload) webhookPayloadArgumentCaptor.getValue();
    assertThat(webhookPayload.initiator).isEqualTo("initiator");
    assertThat(webhookPayload.application.organizationId).isEqualTo(organization.getId());
    assertThat(webhookPayload.application.id).isEqualTo(application.getId());
    assertThat(webhookPayload.application.publicId).isEqualTo(application.getPublicId());
    assertThat(webhookPayload.application.name).isEqualTo(application.getName());

    assertThat(webhookPayload.policyAlerts).isNotEmpty();
    PolicyAlertDTO policyAlertDTO = webhookPayload.policyAlerts.get(0);
    assertThat(policyAlertDTO.policyId).isEqualTo("policyId");
    assertThat(policyAlertDTO.policyName).isEqualTo("name");
    assertThat(policyAlertDTO.threatLevel).isEqualTo(5);
    assertThat(policyAlertDTO.policyViolationId).isEqualTo("policyViolationId");
    assertThat(policyAlertDTO.componentFacts).isNotEmpty();
    assertThat(webhookPayload.applicationEvaluation.reportId).isEqualTo("reportId");
    assertThat(webhookPayload.applicationEvaluation.stage).isEqualTo("stage");
    assertThat(webhookPayload.applicationEvaluation.ownerId).isEqualTo("ownerId");
    assertThat(webhookPayload.applicationEvaluation.isForLatestScan).isEqualTo(true);
    ApiComponentIdentifierDTOV2 componentIdentifier = policyAlertDTO.componentFacts.get(0).componentIdentifier;
    assertThat(ApiComponentIdentifierDTOV2.toComponentIdentifier(componentIdentifier)).isEqualTo(mavenCoordinates);
  }

  @Test
  public void testOn_SkipsNonConfiguredWebhooks() {
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
    verify(webhookClientUtil, timeout(EVENT_TIMEOUT_MS).only())
        .post(webhookArgumentCaptor.capture(), eq(WebhookEventType.SECURITY_VULNERABILITY_OVERRIDE_MANAGEMENT.getId()),
            webhookPayloadArgumentCaptor.capture());

    Webhook webhook = webhookArgumentCaptor.getValue();
    assertThat(webhook.getUrl()).isEqualTo("http://localhost");
    assertThat(webhook.getSecretKey()).isEqualTo(WEBHOOK_SECRET_KEY_CLEAR);

    SecurityVulnerabilityOverridePayload webhookPayload =
        (SecurityVulnerabilityOverridePayload) webhookPayloadArgumentCaptor.getValue();
    assertThat(webhookPayload.initiator).isEqualTo("initiator");
    assertThat(webhookPayload.action).isEqualTo(EventAction.UPDATED);
    assertThat(webhookPayload.id).isEqualTo(override.getId());

    SecurityVulnerabilityOverrideDTO securityVulnerabilityOverrideDTO = webhookPayload.securityVulnerabilityOverride;
    assertThat(securityVulnerabilityOverrideDTO.id).isEqualTo(override.getId());
  }

  @Test
  public void testOn_HandlesLicenseOverrideEvent() {
    tempEntity
        .newWebhookWithSecret("http://localhost", Collections.singleton(WebhookEventType.LICENSE_OVERRIDE_MANAGEMENT));
    Organization organization = tempEntity.newOrganization();
    ComponentIdentifier mavenCoordinates = createMavenCoordinates("com.group", "artifact", "1.0", "test", "jar");
    LicenseOverride givenOverride = tempEntity.newLicenseOverride(organization.getId(), mavenCoordinates,
        LicenseOverrideStatus.ACKNOWLEDGED, Collections.emptySet());

    LicenseOverrideEvent event = new LicenseOverrideEvent();
    event.initiator = "initiator";
    event.licenseOverride = givenOverride;
    event.action = EventAction.UPDATED;

    asyncEventBus.post(event);

    ArgumentCaptor<Webhook> webhookArgumentCaptor = ArgumentCaptor.forClass(Webhook.class);
    ArgumentCaptor<WebhookPayload> webhookPayloadArgumentCaptor = ArgumentCaptor.forClass(WebhookPayload.class);
    verify(webhookClientUtil, timeout(EVENT_TIMEOUT_MS).only())
        .post(webhookArgumentCaptor.capture(), eq(WebhookEventType.LICENSE_OVERRIDE_MANAGEMENT.getId()),
            webhookPayloadArgumentCaptor.capture());

    Webhook webhook = webhookArgumentCaptor.getValue();
    assertThat(webhook.getUrl()).isEqualTo("http://localhost");
    assertThat(webhook.getSecretKey()).isEqualTo(WEBHOOK_SECRET_KEY_CLEAR);
    LicenseOverridePayload webhookPayload = (LicenseOverridePayload) webhookPayloadArgumentCaptor.getValue();
    assertThat(webhookPayload.initiator).isEqualTo("initiator");
    assertThat(webhookPayload.action).isEqualTo(EventAction.UPDATED);
    assertThat(webhookPayload.id).isEqualTo(givenOverride.getId());
    LicenseOverrideDTO actualOverride = webhookPayload.licenseOverride;
    assertThat(actualOverride.id).isEqualTo(givenOverride.getId());
    assertThat(actualOverride.comment).isEqualTo("testing");
    assertThat(actualOverride.componentIdentifier).isNotNull();
    assertThat(ApiComponentIdentifierDTOV2.toComponentIdentifier(actualOverride.componentIdentifier))
        .isEqualTo(mavenCoordinates);
    assertThat(actualOverride.licenseIds).isEmpty();
    assertThat(actualOverride.ownerId).isEqualTo(organization.getId());
    assertThat(actualOverride.status).isEqualTo(LicenseOverrideStatus.ACKNOWLEDGED.name());
  }

  @Test
  public void testOn_HandlesWaiverRequestEvent() {
    tempEntity.newWebhookWithSecret("http://localhost", Collections.singleton(WebhookEventType.WAIVER_REQUEST));

    WaiverRequestEvent event = new WaiverRequestEvent();
    event.initiator = "initiator";
    event.timestamp = LocalDateTime.now();
    event.comment = "Important waiver";
    event.policyViolationId = "policyViolationId";
    event.policyViolationLink = "https://encoded.policy.violation.link:8182?additionalParameters&anotherOne=yeah";
    event.reviewWaiverRequestLink = "https://encoded.review.waiver.link:8182?additionalParameters&anotherOne=yeah";
    asyncEventBus.post(event);

    ArgumentCaptor<Webhook> webhookArgumentCaptor = ArgumentCaptor.forClass(Webhook.class);
    ArgumentCaptor<WebhookPayload> webhookPayloadArgumentCaptor = ArgumentCaptor.forClass(WebhookPayload.class);
    verify(webhookClientUtil, timeout(EVENT_TIMEOUT_MS).only())
        .post(webhookArgumentCaptor.capture(), eq(WebhookEventType.WAIVER_REQUEST.getId()),
            webhookPayloadArgumentCaptor.capture());

    Webhook webhook = webhookArgumentCaptor.getValue();
    assertThat(webhook.getUrl()).isEqualTo("http://localhost");
    assertThat(webhook.getSecretKey()).isEqualTo(WEBHOOK_SECRET_KEY_CLEAR);

    WaiverRequestPayload webhookPayload = (WaiverRequestPayload) webhookPayloadArgumentCaptor.getValue();
    assertThat(webhookPayload.initiator).isEqualTo("initiator");
    assertThat(webhookPayload.timestamp).isEqualTo(
        Date.from(event.timestamp.atZone(ZoneId.systemDefault()).toInstant()));
    assertThat(webhookPayload.comment).isEqualTo("Important waiver");
    assertThat(webhookPayload.policyViolationId).isEqualTo("policyViolationId");
    assertThat(webhookPayload.policyViolationLink).isEqualTo(
        "https://encoded.policy.violation.link:8182?additionalParameters&anotherOne=yeah");
    assertThat(webhookPayload.reviewWaiverRequestLink).isEqualTo(
        "https://encoded.review.waiver.link:8182?additionalParameters&anotherOne=yeah");
  }

  @Test
  public void testOn_HandlesOrganizationApplicationSummaryEvent() {
    testProductLicense.setFeatures(LicensedFeature.WEBHOOKS_FOR_APPLICATIONS);

    Webhook webhook =
        tempEntity.newWebhookWithSecret("http://localhost", Collections.singleton(WebhookEventType.ORG_APP_MANAGEMENT));
    webhook.setContext(Webhook.CONTEXT_LIFECYCLE);
    webhookDAO.update(webhook);

    final Organization organization = tempEntity.newOrganization();
    final OrganizationSummary organizationSummary = new OrganizationSummary(organization);
    final List<OrganizationSummary> organizationSummaries = Collections.singletonList(organizationSummary);

    final Application application = tempEntity.newApplicationWithParent();
    final ApplicationSummary applicationSummary = new ApplicationSummary(application);
    final List<ApplicationSummary> applicationSummaries = Collections.singletonList(applicationSummary);

    final OrganizationApplicationManagementEvent event =
        new OrganizationApplicationManagementEvent(
            organizationSummaries, applicationSummaries, Collections.emptyList(), Collections.emptyList());
    event.initiator = "initiator";
    asyncEventBus.post(event);

    final ArgumentCaptor<Webhook> webhookArgumentCaptor = ArgumentCaptor.forClass(Webhook.class);
    final ArgumentCaptor<WebhookPayload> webhookPayloadArgumentCaptor = ArgumentCaptor.forClass(WebhookPayload.class);
    verify(webhookClientUtil, timeout(EVENT_TIMEOUT_MS).only())
        .post(webhookArgumentCaptor.capture(), eq(WebhookEventType.ORG_APP_MANAGEMENT.getId()),
            webhookPayloadArgumentCaptor.capture());

    final Webhook capturedWebhook = webhookArgumentCaptor.getValue();
    assertThat(capturedWebhook.getUrl()).isEqualTo("http://localhost");
    assertThat(capturedWebhook.getSecretKey()).isEqualTo(WEBHOOK_SECRET_KEY_CLEAR);

    final OrganizationApplicationSummaryPayload webhookPayload =
        (OrganizationApplicationSummaryPayload) webhookPayloadArgumentCaptor.getValue();
    assertThat(webhookPayload.initiator).isEqualTo("initiator");
    assertThat(webhookPayload.timestamp).isNotNull();
    assertThat(webhookPayload.organizations).hasSameElementsAs(organizationSummaries);
    assertThat(webhookPayload.applications).hasSameElementsAs(applicationSummaries);
  }

  @Test
  public void testOn_HandlesPolicyAlertEvent_SendTelemetry_WhenWebhookUrlMatchesCriteria() {
    final String webhookUrl = String.format("https://12345.%s/x1/67890", JIRA_CLOUD_PLUGIN_TELEMETRY_URL_IDENTIFIER);
    final Webhook target =
        tempEntity.newWebhookWithSecret(webhookUrl, Collections.singleton(WebhookEventType.POLICY_ALERT));

    final ApplicationEvaluationEvent evaluationEvent = new ApplicationEvaluationEvent();
    final PolicyAlertEvent event = new PolicyAlertEvent(target.getId());
    event.application = new ApplicationSummary();
    event.applicationEvaluation = evaluationEvent;
    asyncEventBus.post(event);

    ArgumentCaptor<Webhook> webhookArgumentCaptor = ArgumentCaptor.forClass(Webhook.class);
    ArgumentCaptor<WebhookPayload> webhookPayloadArgumentCaptor = ArgumentCaptor.forClass(WebhookPayload.class);
    verify(webhookClientUtil, timeout(EVENT_TIMEOUT_MS).only())
        .post(webhookArgumentCaptor.capture(), eq(WebhookEventType.POLICY_ALERT.getId()),
            webhookPayloadArgumentCaptor.capture());

    final ArgumentCaptor<TelemetryData> telemetryDataCaptor = ArgumentCaptor.forClass(TelemetryData.class);
    verify(telemetrySender).send(telemetryDataCaptor.capture());
    final TelemetryData telemetryData = telemetryDataCaptor.getValue();
    assertThat(telemetryData.getPurpose()).isEqualTo(TelemetryPurpose.JIRA_CLOUD_PLUGIN_USAGE_METRICS);
    assertThat(telemetryData.getTimestamp()).isPositive();
  }

  @Test
  public void testOn_HandlesPolicyAlertEvent_DoNotSendTelemetry_WhenWebhookUrlDoesNotMatchCriteria() {
    final Webhook target =
        tempEntity.newWebhookWithSecret("http://locahost", Collections.singleton(WebhookEventType.POLICY_ALERT));

    final ApplicationEvaluationEvent evaluationEvent = new ApplicationEvaluationEvent();
    final PolicyAlertEvent event = new PolicyAlertEvent(target.getId());
    event.application = new ApplicationSummary();
    event.applicationEvaluation = evaluationEvent;
    asyncEventBus.post(event);

    ArgumentCaptor<Webhook> webhookArgumentCaptor = ArgumentCaptor.forClass(Webhook.class);
    ArgumentCaptor<WebhookPayload> webhookPayloadArgumentCaptor = ArgumentCaptor.forClass(WebhookPayload.class);
    verify(webhookClientUtil, timeout(EVENT_TIMEOUT_MS).only())
        .post(webhookArgumentCaptor.capture(), eq(WebhookEventType.POLICY_ALERT.getId()),
            webhookPayloadArgumentCaptor.capture());

    verifyNoInteractions(telemetrySender);
  }

  private void testEventTypesWithOwner(Owner owner) {
    String ownerId = owner.getId();

    ApplicationEvaluationEvent applicationEvaluationEvent = new ApplicationEvaluationEvent();
    applicationEvaluationEvent.ownerId = ownerId;
    webhookDispatcher.on(applicationEvaluationEvent);

    // policy management events
    OwnerEvent ownerEvent = new OwnerEvent();
    ownerEvent.ownerId = ownerId;
    ownerEvent.owner = owner;
    webhookDispatcher.on(ownerEvent);

    TagEvent tagEvent = new TagEvent();
    tagEvent.tag = new Tag();
    tagEvent.ownerId = ownerId;
    webhookDispatcher.on(tagEvent);

    LabelEvent labelEvent = new LabelEvent();
    labelEvent.ownerId = ownerId;
    labelEvent.label = new Label();
    webhookDispatcher.on(labelEvent);

    LicenseThreatGroupEvent licenseThreatGroupEvent = new LicenseThreatGroupEvent();
    licenseThreatGroupEvent.ownerId = ownerId;
    licenseThreatGroupEvent.licenseThreatGroup = new LicenseThreatGroup();
    webhookDispatcher.on(licenseThreatGroupEvent);

    PolicyEvent policyEvent = new PolicyEvent();
    policyEvent.ownerId = ownerId;
    policyEvent.policy = new Policy();
    webhookDispatcher.on(policyEvent);

    RoleEvent roleEvent = new RoleEvent();
    roleEvent.ownerId = ownerId;
    webhookDispatcher.on(roleEvent);
    // end policy management events

    SecurityVulnerabilityOverrideEvent securityVulnerabilityOverrideEvent = new SecurityVulnerabilityOverrideEvent();
    securityVulnerabilityOverrideEvent.override = new SecurityVulnerabilityOverride();
    securityVulnerabilityOverrideEvent.override.setStatus(SecurityVulnerabilityOverrideStatus.NOT_APPLICABLE);
    securityVulnerabilityOverrideEvent.override.setOwnerId(ownerId);
    webhookDispatcher.on(securityVulnerabilityOverrideEvent);

    LicenseOverrideEvent licenseOverrideEvent = new LicenseOverrideEvent();
    licenseOverrideEvent.licenseOverride = new LicenseOverride();
    licenseOverrideEvent.licenseOverride.setStatus(LicenseOverrideStatus.ACKNOWLEDGED);
    licenseOverrideEvent.licenseOverride.setOwnerId(ownerId);
    webhookDispatcher.on(licenseOverrideEvent);

    WaiverRequestEvent waiverRequestEvent = new WaiverRequestEvent();
    waiverRequestEvent.policyViolationId = "policyViolationId";
    waiverRequestEvent.ownerId = ownerId;
    webhookDispatcher.on(waiverRequestEvent);
  }

  private void verifyEventTypesSent() {
    // we have 6 different events for policy management
    verify(webhookClientUtil, times(6))
        .post(any(Webhook.class), eq(WebhookEventType.POLICY_MANAGEMENT.getId()), any(WebhookPayload.class));

    verify(webhookClientUtil)
        .post(any(Webhook.class), eq(WebhookEventType.APPLICATION_EVALUATION.getId()), any(WebhookPayload.class));

    verify(webhookClientUtil)
        .post(any(Webhook.class), eq(WebhookEventType.SECURITY_VULNERABILITY_OVERRIDE_MANAGEMENT.getId()),
            any(WebhookPayload.class));

    verify(webhookClientUtil)
        .post(any(Webhook.class), eq(WebhookEventType.LICENSE_OVERRIDE_MANAGEMENT.getId()), any(WebhookPayload.class));

    verify(webhookClientUtil)
        .post(any(Webhook.class), eq(WebhookEventType.WAIVER_REQUEST.getId()), any(WebhookPayload.class));

    reset(webhookClientUtil);
  }
}
