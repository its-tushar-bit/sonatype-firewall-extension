/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.naming.NamingException;

import com.sonatype.clm.dto.model.policy.PolicyFact;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.configuration.ldap.LdapManager;
import com.sonatype.insight.brain.configuration.ldap.LdapUser;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.configuration.ldap.LdapServerDAO;
import com.sonatype.insight.brain.dataaccess.security.MembershipMappingDAO;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.configuration.ldap.LdapServer;
import com.sonatype.insight.brain.model.policy.actions.ActionTypes;
import com.sonatype.insight.brain.model.policy.notifications.PolicyNotification;
import com.sonatype.insight.brain.model.policy.notifications.RoleNotification;
import com.sonatype.insight.brain.model.policy.notifications.UserNotification;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.model.security.MemberType;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.security.Member;
import com.sonatype.insight.brain.security.MemberAttributeResolver;
import com.sonatype.insight.brain.security.UserDirectory;
import com.sonatype.insight.brain.service.InsightMail;
import com.sonatype.insight.brain.utils.TemplateUtils;

import freemarker.template.Template;
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

  private static Template policyThreatsTemplate;

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

  protected Map<String, List<PolicyFact>> getPolicyFactsByEmailAddress(Owner owner,
                                                                       final List<PolicyNotification> policyNotifications)
  {
    final Map<String, Set<String>> emailAddressesByRoleId = new HashMap<>();

    final Map<String, List<PolicyFact>> policyFactsByEmailAddress = new HashMap<>();
    for (final PolicyNotification notification : policyNotifications) {
      // Add Role Notifications
      List<RoleNotification> roleNotifications = notification.getNotifications().getRoleNotifications();
      for (RoleNotification roleNotification : roleNotifications) {
        String roleId = roleNotification.getRoleId();
        Set<String> emailAddresses = emailAddressesByRoleId.get(roleId);
        if (emailAddresses == null) {
          emailAddresses = getEmailAddressesForRole(owner, roleId);
          emailAddressesByRoleId.put(roleId, emailAddresses);
        }
        for (String emailAddress : emailAddresses) {
          addPolicyFact(policyFactsByEmailAddress, emailAddress, notification.getPolicyFact());
        }
      }
      // Add Email Notifications
      List<UserNotification> userNotifications = notification.getNotifications().getUserNotifications();
      for (UserNotification userNotification : userNotifications) {
        addPolicyFact(policyFactsByEmailAddress, userNotification.getEmailAddress(), notification.getPolicyFact());
      }
    }

    return policyFactsByEmailAddress;
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

  protected String createPolicyMailSubject(PolicyAlertCounts counts, String ownerName) {
    StringBuilder buffer = new StringBuilder(128);
    buffer.append("Policy Alert for ").append(ownerName).append(": ");
    int total = counts.getRed() + counts.getOrange() + counts.getYellow() + counts.getDarkBlue() + counts.getBlue();
    int highest = 0;
    if (counts.getRed() > 0) {
      buffer.append(highest = counts.getRed()).append(" critical");
    }
    else if (counts.getOrange() > 0) {
      buffer.append(highest = counts.getOrange()).append(" severe");
    }
    else if (counts.getYellow() > 0) {
      buffer.append(highest = counts.getYellow()).append(" moderate");
    }
    else if (counts.getBlue() > 0 || counts.getDarkBlue() > 0) {
      buffer.append(highest = counts.getBlue() + counts.getDarkBlue()).append(" neutral");
    }
    buffer.append(" violation").append(highest != 1 ? "s" : "");
    buffer.append(" out of ").append(total);
    return buffer.toString();
  }

  private Set<String> getEmailAddressesForRole(Owner owner, String roleId) {
    List<Member> members = new ArrayList<>();
    // Get role members from application on up
    for (Owner parentOwner : ownerDAO.walkHierarchy(owner.getId())) {
      for (MembershipMapping membershipMapping : membershipMappingDAO
          .getByContextIdAndRoleId(parentOwner.getId(), roleId)) {
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
            for (LdapUser ldapUser : ldapManager.findUsersByGroup(ldapServer, member.getInternalName(), 0 /* no max results */)) {
              emailAddresses.add(ldapUser.getEmail());
            }
          }
          catch (NamingException e) {
            log.error("Cannot send notifications to members of group {} using ldap server {}", member.getInternalName(), 
                ldapServer.getName(), e);
          }
        }
      }
    }

    return emailAddresses;
  }

  protected InsightMail getMail() {
    return mail;
  }

  protected String createPolicyMailBody(Map<String, Object> model) throws IOException {
    return TemplateUtils.render(getPolicyThreatsTemplate(), model);
  }

  private synchronized static Template getPolicyThreatsTemplate() throws IOException {
    if (policyThreatsTemplate == null) {
      policyThreatsTemplate = TemplateUtils.createFreemarkerConfig().getTemplate("policythreats.ftl");
    }
    return policyThreatsTemplate;
  }

  protected Map<String, Object> createPolicyMailModel(final String cdnUrl,
                                                      final Owner owner,
                                                      final Stage stage,
                                                      final List<PolicyFact> policyFacts)
  {
    PolicyAlertCounts counts = new PolicyAlertCounts(policyFacts);

    final Map<String, Object> model = new HashMap<>();

    model.put("cdnUrl", cdnUrl);
    model.put("policyFacts", policyFacts);
    model.put("policyThreatStage", StageTypes.getById(stage.getStageTypeId()).getName());
    model.put("policyThreatApp", owner.getPublicId());
    model.put("policyThreatTime", new SimpleDateFormat("MMMM dd, yyyy", Locale.ENGLISH).format(new Date()));
    model.put("policyThreatRedCount", counts.getRed());
    model.put("policyThreatOrangeCount", counts.getOrange());
    model.put("policyThreatYellowCount", counts.getYellow());
    model.put("policyThreatDarkBlueCount", counts.getDarkBlue());
    model.put("policyThreatBlueCount", counts.getBlue());
    model.put("actionTypes", ActionTypes.getAll());

    return model;
  }
}
