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
import jakarta.inject.Inject;
import jakarta.inject.Named;

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
import com.sonatype.insight.brain.security.CrowdClient;
import com.sonatype.insight.brain.security.CrowdClientFactory;
import com.sonatype.insight.brain.security.CrowdRealm;
import com.sonatype.insight.brain.security.Member;
import com.sonatype.insight.brain.security.MemberAttributeResolver;
import com.sonatype.insight.brain.security.SsoUser;
import com.sonatype.insight.brain.security.SsoUserService;
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

  private final SsoUserService ssoUserService;

  private final CrowdClientFactory crowdClientFactory;

  private final LdapServerDAO ldapServerDAO;

  @Inject
  public PolicyAlertEmailResolver(
      final UserDirectory userDirectory,
      final LdapService ldapService,
      final OwnerDAO ownerDAO,
      final SsoUserService ssoUserService,
      final MembershipMappingDAO membershipMappingDAO,
      final LdapServerDAO ldapServerDAO,
      final CrowdClientFactory crowdClientFactory)
  {
    this.userDirectory = userDirectory;
    this.ldapService = ldapService;
    this.ownerDAO = ownerDAO;
    this.ssoUserService = ssoUserService;
    this.membershipMappingDAO = membershipMappingDAO;
    this.ldapServerDAO = ldapServerDAO;
    this.crowdClientFactory = crowdClientFactory;
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

  private Set<String> getEmailAddressesForRole(
      Owner owner,
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
        if (CrowdRealm.ID.equals(member.getRealm())) {
          addEmailAddressesForCrowdGroupMembers(emailAddresses, member);
        }
        else if (ssoUserService.isSsoRealm(member.getRealm())) {
          addEmailAddressesForSsoGroupMembers(emailAddresses, member);
        }
        else {
          addEmailAddressesForLDAPGroupMembers(emailAddresses, member);
        }
      }
    }

    return emailAddresses;
  }

  private void addEmailAddressesForCrowdGroupMembers(Set<String> emailAddresses, Member group) {
    CrowdClient crowdClient = crowdClientFactory.createCrowdClient();
    if (crowdClient != null) {
      try {
        for (Member crowdMember : crowdClient.getUsersByGroupName(group.getInternalName())) {
          if (StringUtils.isNotBlank(crowdMember.getEmail())) {
            emailAddresses.add(crowdMember.getEmail());
          }
        }
      }
      catch (Exception e) {
        log.error("Cannot send notifications to members of group {} using Crowd server.", group.getInternalName(), e);
      }
    }
  }

  private void addEmailAddressesForSsoGroupMembers(Set<String> emailAddresses, Member group) {
    if (ssoUserService.isSsoConfigured()) {
      for (SsoUser ssoUser : ssoUserService.getSsoUsersByGroupName(group.getInternalName())) {
        if (StringUtils.isNotBlank(ssoUser.getEmail())) {
          emailAddresses.add(ssoUser.getEmail());
        }
      }
    }
  }

  private void addEmailAddressesForLDAPGroupMembers(Set<String> emailAddresses, Member group) {
    for (LdapServer ldapServer : ldapServerDAO.getAll()) {
      try {
        for (LdapUser ldapUser : ldapService.getUsersByGroup(ldapServer, group.getDn())) {
          String email = ldapUser.getEmail();
          if (email != null) {
            emailAddresses.add(email);
          }
        }
      }
      catch (Exception e) {
        log.error("Cannot send notifications to members of group {} using ldap server {}", group.getInternalName(),
            ldapServer.getName(), e);
      }
    }
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
