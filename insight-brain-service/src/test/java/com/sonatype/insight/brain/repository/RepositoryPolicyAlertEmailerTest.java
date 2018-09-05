/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.ComponentFact;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.clm.dto.model.policy.PolicyFact;
import com.sonatype.insight.brain.component.ComponentDisplayNameUtil;
import com.sonatype.insight.brain.dataaccess.security.UserDAO;
import com.sonatype.insight.brain.landing.UserInterfaceLinksResource;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.component.IdentificationSource;
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
import com.sonatype.insight.brain.model.repository.RepositoryComponent;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.BaseUrl;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.InsightMail;

import org.sonatype.micromailer.Address;

import com.google.inject.Binder;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class RepositoryPolicyAlertEmailerTest
    extends AbstractComponentTest
{
  @Inject
  private RepositoryPolicyAlertEmailer emailer;

  @Inject
  private InsightConfig insightConfig;

  @Inject
  private BaseUrl baseUrl;

  @Mock
  private InsightMail mail;

  @Override
  public void configure(Binder binder) {
    super.configure(binder);

    binder.bind(InsightMail.class).toInstance(mail);
    when(mail.getCdnUrl()).thenReturn("http://cdnUrl");
  }

  @Before
  public void before() {
    insightConfig.setBaseUrl("http://baseUrl");
  }

  @Test
  public void testSendNotifications_validateEmailAddresses() {
    Repository repository = tempEntity.newRepository();
    User user = tempEntity.newUser();
    Policy policy = createPolicy(user);
    PolicyNotification notification = createPolicyNotification(policy,
        tempEntity.newRepositoryComponent(repository.getId()));

    sendNotificationsAndVerify(repository, user, Collections.singletonList(notification));
  }

  @Test
  public void testSendNotifications_observeEmailAddressChanges() {
    Repository repository = tempEntity.newRepository();
    User user = tempEntity.newUser();
    Policy policy = createPolicy(user);
    PolicyNotification notification = createPolicyNotification(policy,
        tempEntity.newRepositoryComponent(repository.getId()));

    sendNotificationsAndVerify(repository, user, Collections.singletonList(notification));

    user.setEmail("newaddress@sonatype.com");
    UserDAO userDAO = new UserDAO();
    userDAO.update(user);

    sendNotificationsAndVerify(repository, user, Collections.singletonList(notification));
  }

  private void sendNotificationsAndVerify(Repository repository, User user, List<PolicyNotification> notifications) {
    emailer.sendNotifications(repository, notifications);

    verify(mail).sendHtml(eq("SONATYPE-IQ-" + repository.getPublicId()),
        argThat(new AddressListEq(Collections.singletonList(new Address(user.getEmail())))), anyString(), anyString());
  }

  @Test
  public void testCreatePolicyMailModel() {
    Repository repository = new Repository("repoManagerId", "repoPublicId");
    repository.setId("repoId");
    List<PolicyFact> policyFacts = new ArrayList<>();

    for (int i = 0; i < 10; i++) {
      Policy policy = new Policy("policyId" + i, "policyName" + i);
      policy.setThreatLevel(i);
      RepositoryComponent component = new RepositoryComponent(repository.getId(), "pathname" + i, new Date(),
          "hash" + i, ComponentIdentifier.createMavenCoordinates("g", "a", "" + i), MatchState.EXACT.getId(),
          IdentificationSource.SONATYPE.getId(), new Date());

      policyFacts.add(createPolicyFact(policy, component));
    }

    Map<String, Object> model = emailer.createPolicyMailModel(repository, policyFacts);
    assertNotNull(model);
    assertEquals(policyFacts, model.get("policyFacts"));
    assertEquals("http://cdnUrl", model.get("cdnUrl"));
    assertEquals(baseUrl.getConfigured() + UserInterfaceLinksResource.getRepositoryReportUrl(repository.getId()),
        model.get("detailedReportUrl"));
    assertEquals(2, model.get("policyThreatRedCount"));
    assertEquals(4, model.get("policyThreatOrangeCount"));
    assertEquals(2, model.get("policyThreatYellowCount"));
    assertEquals(1, model.get("policyThreatBlueCount"));
    assertEquals("Proxy", model.get("policyThreatStage"));
    assertEquals(repository.getPublicId(), model.get("policyThreatApp"));
    assertNotNull(model.get("policyThreatTime"));
    assertEquals("REPO ID", model.get("ownerIdLabel"));
  }

  @Test
  public void testCreatePolicyMailModel_BaseUrlNotConfigured() {
    insightConfig.setBaseUrl(null);

    Repository repository = new Repository("repoManagerId", "repoPublicId");
    repository.setId("repoId");

    Policy policy = new Policy("policyId", "policyName");
    policy.setThreatLevel(5);
    RepositoryComponent component = new RepositoryComponent(repository.getId(), "pathname", new Date(), "hash",
        ComponentIdentifier.createMavenCoordinates("g", "a", "v"), MatchState.EXACT.getId(),
        IdentificationSource.SONATYPE.getId(), new Date());

    List<PolicyFact> policyFacts = new ArrayList<>();
    policyFacts.add(createPolicyFact(policy, component));

    try {
      emailer.createPolicyMailModel(repository, policyFacts);
      fail("Expected exception");
    }
    catch (IllegalStateException expected) {
      assertThat(expected.getMessage(), is(BaseUrl.ERR_MSG_BASE_URL_NOT_CONFIGURED));
    }
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
    policy.getNotifications().add(new RoleNotification(role.getId(), ProxyStageType.ID));
    policy = tempEntity.newPolicy(policy);

    return policy;
  }

  private PolicyNotification createPolicyNotification(Policy policy, RepositoryComponent component) {
    return new PolicyNotification(createPolicyFact(policy, component), policy.getNotifications());
  }

  private PolicyFact createPolicyFact(Policy policy, RepositoryComponent component) {
    ConstraintFact constraintFact = new ConstraintFact("constraintId", "constraintName", "any");
    ComponentFact componentFact = new ComponentFact(component.getComponentIdentifier(), component.getHash());
    componentFact.setDisplayName(ComponentDisplayNameUtil.fromIdentifier(component.getComponentIdentifier()));
    componentFact.addConstraintFact(constraintFact);
    PolicyFact policyFact = new PolicyFact(policy.getId(), policy.getName(), policy.getThreatLevel());
    policyFact.addComponentFact(componentFact);

    return policyFact;
  }

  // This is required as Address doesn't implement equals/hashCode
  private static class AddressListEq
      implements ArgumentMatcher<List<Address>>
  {
    private final List<Address> addresses;

    AddressListEq(List<Address> addresses) {
      this.addresses = addresses;
    }

    @Override
    public boolean matches(List<Address> addressList) {
      if (addressList == null || addressList.isEmpty()) {
        return addresses == null || addresses.isEmpty();
      }

      if (addresses == null || addresses.isEmpty()) {
        return false;
      }

      if (addressList.size() != addresses.size()) {
        return false;
      }

      for (int i = 0; i < addresses.size(); i++) {
        if (!addresses.get(i).getMailAddress().equals(addressList.get(i).getMailAddress())) {
          return false;
        }
      }

      return true;
    }
  }
}
