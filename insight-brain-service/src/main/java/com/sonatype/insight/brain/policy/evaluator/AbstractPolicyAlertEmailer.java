/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.naming.NamingException;

import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.clm.dto.model.policy.PolicyAlert;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.security.MembershipMappingDAO;
import com.sonatype.insight.brain.ldap.LdapManager;
import com.sonatype.insight.brain.ldap.LdapUser;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.policy.actions.ActionTypes;
import com.sonatype.insight.brain.model.policy.actions.NotifyActionType;
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

  protected String createPolicyMailSubject(MailPolicyAlertCounts counts) {
    StringBuilder buffer = new StringBuilder(128);
    buffer.append("Policy Alert: ");
    int total = counts.red + counts.orange + counts.yellow + counts.darkBlue + counts.blue;
    int highest = 0;
    if (counts.red > 0) {
      buffer.append(highest = counts.red).append(" critical");
    }
    else if (counts.orange > 0) {
      buffer.append(highest = counts.orange).append(" severe");
    }
    else if (counts.yellow > 0) {
      buffer.append(highest = counts.yellow).append(" moderate");
    }
    else if (counts.blue > 0 || counts.darkBlue > 0) {
      buffer.append(highest = counts.blue + counts.darkBlue).append(" neutral");
    }
    buffer.append(" violation").append(highest != 1 ? "s" : "");
    buffer.append(" out of ").append(total);
    return buffer.toString();
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

  protected String processTemplate(Map<String, Object> model) throws IOException {
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
                                                      final List<PolicyAlert> policyAlerts)
  {
    MailPolicyAlertCounts counts = new MailPolicyAlertCounts(policyAlerts);

    Collections.sort(policyAlerts, new Comparator<PolicyAlert>()
    {
      @Override
      public int compare(PolicyAlert o1, PolicyAlert o2) {
        int t1 = o1.getTrigger().getThreatLevel();
        int t2 = o2.getTrigger().getThreatLevel();
        int r = t2 - t1;
        if (r == 0) {
          r = String.CASE_INSENSITIVE_ORDER.compare(o1.getTrigger().getPolicyName(), o2.getTrigger().getPolicyName());
        }
        return r;
      }
    });

    final Map<String, Object> model = new HashMap<>();

    model.put("cdnUrl", cdnUrl);
    model.put("policyAlerts", policyAlerts);
    model.put("policyThreatStage", StageTypes.getById(stage.getStageTypeId()).getName());
    model.put("policyThreatApp", owner.getPublicId());
    model.put("policyThreatTime", new SimpleDateFormat("MMMM dd, yyyy", Locale.ENGLISH).format(new Date()));
    model.put("policyThreatRedCount", counts.red);
    model.put("policyThreatOrangeCount", counts.orange);
    model.put("policyThreatYellowCount", counts.yellow);
    model.put("policyThreatDarkBlueCount", counts.darkBlue);
    model.put("policyThreatBlueCount", counts.blue);
    model.put("actionTypes", ActionTypes.getAll());

    return model;
  }

  protected static class MailPolicyAlertCounts
  {
    int red, orange, yellow, darkBlue, blue;

    MailPolicyAlertCounts(final int red, final int orange, final int yellow, final int darkBlue, final int blue)
    {
      this.red = red;
      this.orange = orange;
      this.yellow = yellow;
      this.darkBlue = darkBlue;
      this.blue = blue;
    }

    public MailPolicyAlertCounts(final List<PolicyAlert> alerts) {
      for (PolicyAlert alert : alerts) {
        int level = alert.getTrigger().getThreatLevel();
        int components = alert.getTrigger().getComponentFacts().size();

        if (level > 7) {
          red += components;
        }
        else if (level > 3) {
          orange += components;
        }
        else if (level > 1) {
          yellow += components;
        }
        else if (level == 1) {
          darkBlue += components;
        }
        else {
          blue += components;
        }
      }
    }
  }
}
