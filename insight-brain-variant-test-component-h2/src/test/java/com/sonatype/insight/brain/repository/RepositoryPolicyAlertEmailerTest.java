/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.ComponentFact;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.clm.dto.model.policy.PolicyFact;
import com.sonatype.insight.IdentificationSource;
import com.sonatype.insight.brain.component.ComponentDisplayNameUtil;
import com.sonatype.insight.brain.dataaccess.security.UserDAO;
import com.sonatype.insight.brain.landing.UserInterfaceLinksHelper;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.LogicalOperator;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.conditions.CoordinatesConditionType;
import com.sonatype.insight.brain.model.policy.notifications.PolicyNotification;
import com.sonatype.insight.brain.model.policy.notifications.RoleNotification;
import com.sonatype.insight.brain.model.policy.notifications.UserNotification;
import com.sonatype.insight.brain.model.policy.stages.ProxyStageType;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.ProxyRepositoryComponent;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.variant.AbstractComponentH2Test;
import com.sonatype.insight.brain.service.BaseUrl;
import com.sonatype.insight.brain.service.InsightMail;
import com.sonatype.insight.brain.shutdown.ShutdownHandler;
import com.sonatype.insight.brain.shutdown.ShutdownPriority;
import com.sonatype.insight.brain.variant.ComponentH2Test;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

@ComponentH2Test
public class RepositoryPolicyAlertEmailerTest
    extends AbstractComponentH2Test
{
  @Inject
  private RepositoryPolicyAlertEmailer emailer;

  @Inject
  private BaseUrl baseUrl;

  @Inject
  private UserDAO userDAO;

  @Mock
  private InsightMail mail;

  @Mock
  private ShutdownHandler mockShutdownHandler;

  @BeforeEach
  public void before() {
    setBaseUrl("http://baseUrl");
  }

  @Test
  public void testRepositoryPolicyAlertEmailer_AddsExecutorToShutdownHandler() {
    RepositoryPolicyAlertEmailer localEmailer = new RepositoryPolicyAlertEmailer(
        mail,
        lookup(com.sonatype.insight.brain.policy.evaluator.PolicyAlertEmailResolver.class),
        baseUrl,
        lookup(com.sonatype.insight.brain.audit.AuditRecorder.class),
        mockShutdownHandler);

    try {
      verify(mockShutdownHandler).add(localEmailer.getExecutor(), ShutdownPriority.NOTIFICATIONS);
    }
    finally {
      localEmailer.getExecutor().shutdownNow();
    }
  }

  @Test
  public void testSendNotifications_validateEmailAddresses() {
    when(mail.getCdnUrl()).thenReturn("http://cdnUrl");
    Repository repository = tempEntity.newRepository();
    User user = tempEntity.newUser();
    Policy policy = createPolicy(user);
    PolicyNotification notification = createPolicyNotification(policy,
        tempEntity.newRepositoryComponent(repository.getId()));

    sendNotificationsAndVerify(repository, user, Collections.singletonList(notification));
  }

  @Test
  public void testSendNotifications_observeEmailAddressChanges() {
    when(mail.getCdnUrl()).thenReturn("http://cdnUrl");
    Repository repository = tempEntity.newRepository();
    User user = tempEntity.newUser();
    Policy policy = createPolicy(user);
    PolicyNotification notification = createPolicyNotification(policy,
        tempEntity.newRepositoryComponent(repository.getId()));

    sendNotificationsAndVerify(repository, user, Collections.singletonList(notification));

    user.setEmail("newaddress@sonatype.com");
    userDAO.update(user);

    sendNotificationsAndVerify(repository, user, Collections.singletonList(notification));
  }

  private void sendNotificationsAndVerify(Repository repository, User user, List<PolicyNotification> notifications) {
    emailer.sendNotifications(repository, notifications);

    verify(mail, Mockito.timeout(5000)).sendHtml(eq(user.getEmail()), anyString(), anyString());
  }

  @Test
  public void testCreatePolicyMailModel() {
    when(mail.getCdnUrl()).thenReturn("http://cdnUrl");
    Repository repository = new Repository("repoManagerId", "repoPublicId");
    repository.setId("repoId");
    List<PolicyFact> policyFacts = new ArrayList<>();

    for (int i = 0; i < 10; i++) {
      Policy policy = new Policy("policyId" + i, "policyName" + i);
      policy.setThreatLevel(i);
      ProxyRepositoryComponent component = new ProxyRepositoryComponent(repository.getId(), "pathname" + i, new Date(),
          "hash" + i, ComponentIdentifier.createMavenCoordinates("g", "a", "" + i), MatchState.EXACT.getId(),
          IdentificationSource.SONATYPE.getId(), new Date());

      policyFacts.add(createPolicyFact(policy, component));
    }

    Map<String, Object> model = emailer.createPolicyMailModel(repository, policyFacts);
    assertThat(model).isNotNull();
    assertThat(model.get("policyFacts")).isEqualTo(policyFacts);
    assertThat(model.get("cdnUrl")).isEqualTo("http://cdnUrl");
    assertThat(model.get("detailedReportUrl"))
        .isEqualTo(baseUrl.getConfigured() + UserInterfaceLinksHelper.getRepositoryReportUrl(repository.getId()));
    assertThat(model.get("policyThreatRedCount")).isEqualTo(2);
    assertThat(model.get("policyThreatOrangeCount")).isEqualTo(4);
    assertThat(model.get("policyThreatYellowCount")).isEqualTo(2);
    assertThat(model.get("policyThreatBlueCount")).isEqualTo(1);
    assertThat(model.get("policyThreatStage")).isEqualTo("Proxy");
    assertThat(model.get("policyThreatApp")).isEqualTo(repository.getPublicId());
    assertThat(model.get("policyThreatTime")).isNotNull();
    assertThat(model.get("ownerIdLabel")).isEqualTo("REPO ID");
  }

  @Test
  public void testCreatePolicyMailModel_BaseUrlNotConfigured() {
    when(mail.getCdnUrl()).thenReturn("http://cdnUrl");
    setBaseUrl(null);

    Repository repository = new Repository("repoManagerId", "repoPublicId");
    repository.setId("repoId");

    Policy policy = new Policy("policyId", "policyName");
    policy.setThreatLevel(5);
    ProxyRepositoryComponent component =
        new ProxyRepositoryComponent(repository.getId(), "pathname", new Date(), "hash",
            ComponentIdentifier.createMavenCoordinates("g", "a", "v"), MatchState.EXACT.getId(),
            IdentificationSource.SONATYPE.getId(), new Date());

    List<PolicyFact> policyFacts = new ArrayList<>();
    policyFacts.add(createPolicyFact(policy, component));

    assertThatExceptionOfType(IllegalStateException.class)
        .isThrownBy(() -> emailer.createPolicyMailModel(repository, policyFacts))
        .withMessage(BaseUrl.ERR_MSG_BASE_URL_NOT_CONFIGURED);
  }

  private Policy createPolicy(User user) {
    Role role = tempEntity.newRole(false, Permission.READ);

    tempEntity.newMembershipMapping(Organization.ROOT_ORGANIZATION_ID, role.getId(), user.getUsername());

    Policy policy = new Policy(null, "policyName");
    policy.setThreatLevel(5);
    policy.setOwnerId(Organization.ROOT_ORGANIZATION_ID);
    Constraint constraint = new Constraint(null, "Constraint", LogicalOperator.AND);
    constraint.addCondition(new Condition(CoordinatesConditionType.ID, "match", "maven:foobar"));
    policy.addConstraint(constraint);
    policy.getNotifications().add(new UserNotification("email@sonatype.com", ProxyStageType.ID));
    policy.getNotifications().add(new RoleNotification(role.getId(), role.getName(), ProxyStageType.ID));
    policy = tempEntity.newPolicy(policy);

    return policy;
  }

  private PolicyNotification createPolicyNotification(Policy policy, ProxyRepositoryComponent component) {
    return new PolicyNotification(createPolicyFact(policy, component), policy.getNotifications());
  }

  private PolicyFact createPolicyFact(Policy policy, ProxyRepositoryComponent component) {
    ConstraintFact constraintFact = new ConstraintFact("constraintId", "constraintName", "any");
    ComponentFact componentFact = new ComponentFact(component.getComponentIdentifier(), component.getHash());
    componentFact.setDisplayName(ComponentDisplayNameUtil.fromIdentifier(component.getComponentIdentifier()));
    componentFact.addConstraintFact(constraintFact);
    PolicyFact policyFact = new PolicyFact(policy.getId(), policy.getName(), policy.getThreatLevel());
    policyFact.addComponentFact(componentFact);

    return policyFact;
  }
}
