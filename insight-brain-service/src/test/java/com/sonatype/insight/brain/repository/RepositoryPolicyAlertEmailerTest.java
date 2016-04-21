/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import com.sonatype.clm.dto.model.policy.ComponentFact;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.clm.dto.model.policy.NotifyAction;
import com.sonatype.clm.dto.model.policy.PolicyAlert;
import com.sonatype.clm.dto.model.policy.PolicyFact;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.LogicalOperator;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.actions.NotifyActionType;
import com.sonatype.insight.brain.model.policy.conditions.CoordinatesConditionType;
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
import com.sonatype.insight.brain.service.InsightMail;

import org.sonatype.micromailer.Address;

import com.google.inject.Binder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

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

  @Override
  public void configure(Binder binder) {
    super.configure(binder);
    binder.bind(InsightMail.class).toInstance(mail);
  }

  @Test
  public void testSendNotifications() {
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
        argThat(new AddressListEq(Collections.singletonList(new Address("email@sonatype.com")))), eq("subject"),
        eq("body"));

    verify(mail).sendHtml(eq("SONATYPE-IQ-" + repository.getPublicId()),
        argThat(new AddressListEq(Collections.singletonList(new Address(user.getEmail())))), eq("subject"),
        eq("body"));
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
    policy.addAction(ProxyStageType.ID, new NotifyAction("email@sonatype.com", null));
    policy.addAction(ProxyStageType.ID, new NotifyAction(role.getId(), NotifyActionType.TARGET_TYPE_ROLE));
    policy = tempEntity.newPolicy(policy);

    return policy;
  }

  private PolicyAlert createPolicyAlert(Policy policy, RepositoryComponent component) {
    ConstraintFact constraintFact = new ConstraintFact("constraintId", "constraintName", "any");
    ComponentFact componentFact = new ComponentFact(component.getComponentIdentifier(), component.getHash());
    componentFact.addConstraintFact(constraintFact);
    PolicyFact policyFact = new PolicyFact(policy.getId(), policy.getName(), policy.getThreatLevel());
    policyFact.addComponentFact(componentFact);
    return new PolicyAlert(policyFact, policy.getActions(ProxyStageType.ID));
  }

  //This is required as Address doesn't implement equals/hashCode
  private static class AddressListEq
      extends ArgumentMatcher<List>
  {
    private final List<Address> addresses;

    AddressListEq(List<Address> addresses) {
      this.addresses = addresses;
    }

    public boolean matches(Object list) {
      List<Address> addressList = (List<Address>) list;
      if (addressList == null || addressList.isEmpty()) {
        return addresses == null || ((List) addresses).isEmpty();
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
