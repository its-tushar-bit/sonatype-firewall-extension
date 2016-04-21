/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.naming.NamingException;

import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.clm.dto.model.policy.PolicyAlert;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.security.MembershipMappingDAO;
import com.sonatype.insight.brain.ldap.LdapManager;
import com.sonatype.insight.brain.ldap.LdapUser;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.policy.actions.NotifyActionType;
import com.sonatype.insight.brain.model.security.MemberType;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.security.Member;
import com.sonatype.insight.brain.security.MemberAttributeResolver;
import com.sonatype.insight.brain.security.UserDirectory;
import com.sonatype.insight.brain.service.InsightMail;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since 1.21
 */
public abstract class AbstractPolicyAlertEmailer
{
  private static final Logger log = LoggerFactory.getLogger(AbstractPolicyAlertEmailer.class);

  private final InsightMail mail;

  private final MembershipMappingDAO membershipMappingDAO;

  private final MemberAttributeResolver memberAttributeResolver;

  private final LdapManager ldapManager;

  private final OwnerDAO ownerDAO;

  public AbstractPolicyAlertEmailer(final InsightMail mail,
                                    final UserDirectory userDirectory,
                                    final LdapManager ldapManager,
                                    final OwnerDAO ownerDAO,
                                    final MembershipMappingDAO membershipMappingDAO)
  {
    this.mail = mail;
    memberAttributeResolver = new MemberAttributeResolver(userDirectory);
    this.ldapManager = ldapManager;
    this.ownerDAO = ownerDAO;
    this.membershipMappingDAO = membershipMappingDAO;
  }

  protected Map<String, List<PolicyAlert>> getPolicyAlertsByEmailAddresses(Owner owner,
                                                                           final List<PolicyAlert> alerts)
  {
    final Map<String, Set<String>> emailAddressesByRoleId = new HashMap<>();

    final Map<String, List<PolicyAlert>> policyAlertsByEmailAddress = new HashMap<>();
    for (final PolicyAlert alert : alerts) {
      for (final Action action : alert.getActions()) {
        if (NotifyActionType.ID.equals(action.getActionTypeId())) {
          if (NotifyActionType.TARGET_TYPE_ROLE.equals(action.getTargetType())) {
            String roleId = action.getTarget();
            Set<String> emailAddresses = emailAddressesByRoleId.get(roleId);
            if (emailAddresses == null) {
              emailAddresses = getEmailAddressesForRole(owner, roleId);
              emailAddressesByRoleId.put(roleId, emailAddresses);
            }
            for (String emailAddress : emailAddresses) {
              addPolicyAlert(policyAlertsByEmailAddress, emailAddress, alert);
            }
          }
          else {
            addPolicyAlert(policyAlertsByEmailAddress, action.getTarget(), alert);
          }
        }
      }
    }
    return policyAlertsByEmailAddress;
  }

  private void addPolicyAlert(Map<String, List<PolicyAlert>> policyAlertsByEmailAddress,
                              String emailAddress,
                              PolicyAlert policyAlert)
  {
    List<PolicyAlert> policyAlerts = policyAlertsByEmailAddress.get(emailAddress);
    if (policyAlerts == null) {
      policyAlertsByEmailAddress.put(emailAddress, policyAlerts = new ArrayList<>());
    }
    if (!policyAlerts.contains(policyAlert)) {
      policyAlerts.add(policyAlert);
    }
  }

  private Set<String> getEmailAddressesForRole(Owner owner, String roleId) {
    List<Member> members = new ArrayList<>();
    // Get role members from application on up
    for (Owner parentOwner : ownerDAO.walkHierarchy(owner.getId())) {
      for (MembershipMapping membershipMapping : membershipMappingDAO.getByContextIdAndRoleId(parentOwner.getId(), roleId)) {
        Member member = new Member(membershipMapping.getMemberType(), membershipMapping.getMemberName(),
            membershipMapping.getMemberName());
        members.add(member);
      }
    }

    // Fill in email addresses
    memberAttributeResolver.resolve(members);

    Set<String> emailAddresses = new HashSet<>();
    for (Member member : members) {
      if (!StringUtils.isBlank(member.getEmail())) {
        emailAddresses.add(member.getEmail());
      }
      if (MemberType.GROUP == member.getType()) {
        try {
          List<LdapUser> ldapUsers = ldapManager.findUsersByGroup(member.getInternalName(), 0 /* no max results */);
          for (LdapUser ldapUser : ldapUsers) {
            emailAddresses.add(ldapUser.getEmail());
          }
        }
        catch (NamingException e) {
          log.error("Cannot send notifications to members of group {}", member.getInternalName(), e);
        }
      }
    }

    return emailAddresses;
  }

  protected InsightMail getMail() {
    return mail;
  }
}
