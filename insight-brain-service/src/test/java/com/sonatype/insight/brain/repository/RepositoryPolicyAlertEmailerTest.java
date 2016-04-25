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
import com.sonatype.clm.dto.model.policy.PolicyAlert;
import com.sonatype.clm.dto.model.policy.PolicyFact;
import com.sonatype.insight.brain.component.ComponentDisplayNameUtil;
import com.sonatype.insight.brain.landing.UserInterfaceLinksResource;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.component.IdentificationSource;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.LogicalOperator;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.conditions.CoordinatesConditionType;
import com.sonatype.insight.brain.model.policy.notifications.RoleNotification;
import com.sonatype.insight.brain.model.policy.notifications.UserNotification;
import com.sonatype.insight.brain.model.policy.stages.ProxyStageType;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryComponent;
import com.sonatype.insight.brain.model.security.MemberType;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.security.Member;
import com.sonatype.insight.brain.security.UserDirectory;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.BaseUrl;
import com.sonatype.insight.brain.service.InsightMail;

import org.sonatype.micromailer.Address;

import com.google.inject.Binder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class RepositoryPolicyAlertEmailerTest
    extends AbstractComponentTest
{
  @Inject
  RepositoryPolicyAlertEmailer emailer;

  @Mock
  private InsightMail mail;

  @Mock
  private UserDirectory userDirectory;

  @Mock
  private BaseUrl baseUrl;

  @Override
  public void configure(Binder binder) {
    super.configure(binder);
    binder.bind(InsightMail.class).toInstance(mail);
    binder.bind(BaseUrl.class).toInstance(baseUrl);

    when(baseUrl.get()).thenReturn("http://baseUrl");
    when(mail.getCdnUrl()).thenReturn("http://cdnUrl");
  }

  @Test
  public void testSendNotifications_validateEmailAddresses() {
    Repository repository = tempEntity.newRepository();

    User user = tempEntity.newUser();

    Member member = new Member(MemberType.USER, user.getUsername(), user.calculateDisplayName(), user.getEmail(),
        "CLMRealm");

    when(userDirectory.getUsersByName(Collections.singleton(user.getUsername())))
        .thenReturn(new UserDirectory.QueryResult(Collections.singletonList(member)));

    Policy policy = createPolicy(user);

    PolicyAlert alert = createPolicyAlert(policy, tempEntity.newRepositoryComponent(repository.getId()));

    emailer.sendNotifications(repository, Collections.singletonList(alert));

    verify(mail).sendHtml(eq("SONATYPE-IQ-" + repository.getPublicId()),
        argThat(new AddressListEq(Collections.singletonList(new Address("email@sonatype.com")))), anyString(),
        anyString());

    verify(mail).sendHtml(eq("SONATYPE-IQ-" + repository.getPublicId()),
        argThat(new AddressListEq(Collections.singletonList(new Address(user.getEmail())))), anyString(), anyString());
  }

  @Test
  public void testCreatePolicyMailModel() {
    Repository repository = new Repository("repoManagerId", "repoPublicId");
    repository.setId("repoId");
    List<PolicyAlert> alerts = new ArrayList<>();

    for (int i = 0; i < 10; i++) {
      Policy policy = new Policy("policyId" + i, "policyName" + i);
      policy.setThreatLevel(i);
      RepositoryComponent component = new RepositoryComponent(repository.getId(), "pathname" + i, new Date(),
          "hash" + i, ComponentIdentifier.createMavenCoordinates("g", "a", "" + i), MatchState.EXACT.getId(),
          IdentificationSource.SONATYPE.getId(), new Date(), true);

      alerts.add(createPolicyAlert(policy, component));
    }

    Map<String, Object> model = emailer.createPolicyMailModel(repository, alerts);
    assertNotNull(model);
    assertEquals(alerts, model.get("policyAlerts"));
    assertEquals("http://cdnUrl", model.get("cdnUrl"));
    assertEquals(baseUrl.get() + UserInterfaceLinksResource.getRepositoryReportUrl(repository.getId()),
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

  private Policy createPolicy(User user) {
    Role role = tempEntity.newRole(false, Permission.READ);

    tempEntity.newMembershipMapping(Organization.ROOT_ORGANIZATION_ID, role.getId(), user.getUsername());

    Policy policy = new Policy(null, "policyName");
    policy.setThreatLevel(5);
    policy.setOwnerId(Organization.ROOT_ORGANIZATION_ID);
    Constraint constraint = new Constraint(null, "Constraint", LogicalOperator.AND);
    constraint.addCondition(new Condition(CoordinatesConditionType.ID, "match", "foobar"));
    policy.addConstraint(constraint);
    policy.getNotifications().add(new UserNotification("email@sonatype.com", ProxyStageType.ID));
    policy.getNotifications().add(new RoleNotification(role.getId(), ProxyStageType.ID));
    policy = tempEntity.newPolicy(policy);

    return policy;
  }

  private PolicyAlert createPolicyAlert(Policy policy, RepositoryComponent component) {
    ConstraintFact constraintFact = new ConstraintFact("constraintId", "constraintName", "any");
    ComponentFact componentFact = new ComponentFact(component.getComponentIdentifier(), component.getHash());
    componentFact.setDisplayName(ComponentDisplayNameUtil.fromIdentifier(component.getComponentIdentifier()));
    componentFact.addConstraintFact(constraintFact);
    PolicyFact policyFact = new PolicyFact(policy.getId(), policy.getName(), policy.getThreatLevel());
    policyFact.addComponentFact(componentFact);
    return new PolicyAlert(policyFact, policy.toActions(ProxyStageType.ID, false));
  }

  //This is required as Address doesn't implement equals/hashCode
  private static class AddressListEq
      extends ArgumentMatcher<List<Address>>
  {
    private final List<Address> addresses;

    AddressListEq(List<Address> addresses) {
      this.addresses = addresses;
    }

    @Override
    public boolean matches(Object list) {
      @SuppressWarnings("unchecked")
      List<Address> addressList = (List<Address>) list;
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
