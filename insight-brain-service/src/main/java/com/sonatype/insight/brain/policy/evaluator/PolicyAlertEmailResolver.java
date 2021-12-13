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

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.clm.dto.model.policy.PolicyFact;
import com.sonatype.insight.brain.configuration.ldap.LdapService;
import com.sonatype.insight.brain.configuration.ldap.LdapUser;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.configuration.ldap.LdapServerDAO;
import com.sonatype.insight.brain.dataaccess.security.MembershipMappingDAO;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.configuration.ldap.LdapServer;
import com.sonatype.insight.brain.model.policy.notifications.PolicyNotification;
import com.sonatype.insight.brain.model.policy.notifications.RoleNotification;
import com.sonatype.insight.brain.model.policy.notifications.UserNotification;
import com.sonatype.insight.brain.model.security.MemberType;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.security.Member;
import com.sonatype.insight.brain.security.MemberAttributeResolver;
import com.sonatype.insight.brain.security.UserDirectory;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
public class PolicyAlertEmailResolver
{
  private static final Logger log = LoggerFactory.getLogger(PolicyAlertEmailResolver.class);

  private final MembershipMappingDAO membershipMappingDAO;

  private final UserDirectory userDirectory;

  private final LdapService ldapService;

  private final OwnerDAO ownerDAO;

  @Inject
  public PolicyAlertEmailResolver(final UserDirectory userDirectory,
                                  final LdapService ldapService,
                                  final OwnerDAO ownerDAO,
                                  final MembershipMappingDAO membershipMappingDAO)
  {
    this.userDirectory = userDirectory;
    this.ldapService = ldapService;
    this.ownerDAO = ownerDAO;
    this.membershipMappingDAO = membershipMappingDAO;
  }

  public Map<String, List<PolicyFact>> getPolicyFactsByEmailAddress(Owner owner,
                                                                    List<PolicyNotification> policyNotifications)
  {
    Map<String, List<PolicyFact>> policyFactsByEmailAddress = new HashMap<>();
    for (PolicyNotification notification : policyNotifications) {
      addRoleNotifications(policyFactsByEmailAddress, notification, owner);
      addUserNotifications(policyFactsByEmailAddress, notification);
    }
    return policyFactsByEmailAddress;
  }

  private void addRoleNotifications(Map<String, List<PolicyFact>> policyFactsByEmailAddress,
                                    PolicyNotification notification,
                                    Owner owner)
  {
    Map<String, Set<String>> emailAddressesByRoleId = new HashMap<>();
    MemberAttributeResolver memberAttributeResolver = new MemberAttributeResolver(userDirectory);
    for (RoleNotification roleNotification : notification.getNotifications().getRoleNotifications()) {
      String roleId = roleNotification.getRoleId();
      Set<String> emailAddresses = emailAddressesByRoleId.get(roleId);
      if (emailAddresses == null) {
        emailAddresses = getEmailAddressesForRole(owner, roleId, memberAttributeResolver);
        emailAddressesByRoleId.put(roleId, emailAddresses);
      }
      for (String emailAddress : emailAddresses) {
        addPolicyFact(policyFactsByEmailAddress, emailAddress, notification.getPolicyFact());
      }
    }
  }

  private Set<String> getEmailAddressesForRole(Owner owner,
                                               String roleId,
                                               MemberAttributeResolver memberAttributeResolver)
  {
    List<Member> members = new ArrayList<>();
    // Get role members from owner on up
    for (Owner parentOwner : ownerDAO.walkHierarchy(owner)) {
      for (MembershipMapping membershipMapping : membershipMappingDAO.getByContextIdAndRoleId(parentOwner.getId(),
          roleId)) {
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
        for (LdapServer ldapServer : new LdapServerDAO().getAll()) {
          try {
            for (LdapUser ldapUser : ldapService.getUsersByGroup(ldapServer, member.getInternalName())) {
              String email = ldapUser.getEmail();
              if (email != null) {
                emailAddresses.add(email);
              }
            }
          }
          catch (Exception e) {
            log.error("Cannot send notifications to members of group {} using ldap server {}", member.getInternalName(),
                ldapServer.getName(), e);
          }
        }
      }
    }

    return emailAddresses;
  }

  private void addUserNotifications(Map<String, List<PolicyFact>> policyFactsByEmailAddress,
                                    PolicyNotification notification)
  {
    for (UserNotification userNotification : notification.getNotifications().getUserNotifications()) {
      addPolicyFact(policyFactsByEmailAddress, userNotification.getEmailAddress(), notification.getPolicyFact());
    }
  }

  private void addPolicyFact(Map<String, List<PolicyFact>> policyFactsByEmailAddress,
                             String emailAddress,
                             PolicyFact policyFact)
  {
    List<PolicyFact> policyFacts = policyFactsByEmailAddress.get(emailAddress);
    if (policyFacts == null) {
      policyFactsByEmailAddress.put(emailAddress, policyFacts = new ArrayList<>());
    }
    if (!policyFacts.contains(policyFact)) {
      policyFacts.add(policyFact);
    }
  }
}
