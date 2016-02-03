/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.naming.NamingException;

import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.clm.dto.model.policy.PolicyAlert;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.security.MembershipMappingDAO;
import com.sonatype.insight.brain.landing.UserInterfaceLinksResource;
import com.sonatype.insight.brain.ldap.LdapManager;
import com.sonatype.insight.brain.ldap.LdapUser;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.policy.actions.ActionTypes;
import com.sonatype.insight.brain.model.policy.actions.NotifyActionType;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.model.security.MemberType;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.organization.ApplicationAdapter;
import com.sonatype.insight.brain.organization.ContactDTO;
import com.sonatype.insight.brain.security.Member;
import com.sonatype.insight.brain.security.MemberAttributeResolver;
import com.sonatype.insight.brain.security.UserDirectory;
import com.sonatype.insight.brain.service.BaseUrl;
import com.sonatype.insight.brain.service.InsightMail;
import com.sonatype.insight.brain.utils.TemplateUtils;

import org.sonatype.micromailer.Address;

import freemarker.template.Template;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class used to send (email) notifications for policy alerts.
 *
 * @since 1.8
 */
@Named
public class PolicyAlertEmailer
{
  private static final Logger log = LoggerFactory.getLogger(PolicyAlertEmailer.class);

  private static Template policyThreatsTemplate;

  private final InsightMail mail;

  private final BaseUrl baseUrl;

  private final ApplicationDAO applicationDAO;

  private final MembershipMappingDAO membershipMappingDAO;

  private final ApplicationAdapter applicationAdapter;

  private final MemberAttributeResolver memberAttributeResolver;

  private final LdapManager ldapManager;

  private final OwnerDAO ownerDAO;

  @Inject
  public PolicyAlertEmailer(final InsightMail mail,
                            final BaseUrl baseUrl,
                            final ApplicationAdapter applicationAdapter,
                            final UserDirectory userDirectory,
                            final LdapManager ldapManager,
                            final OwnerDAO ownerDAO,
                            final ApplicationDAO applicationDAO,
                            final MembershipMappingDAO membershipMappingDAO)
  {
    this.mail = mail;
    this.baseUrl = baseUrl;
    this.applicationAdapter = applicationAdapter;
    memberAttributeResolver = new MemberAttributeResolver(userDirectory);
    this.ldapManager = ldapManager;
    this.ownerDAO = ownerDAO;
    this.applicationDAO = applicationDAO;
    this.membershipMappingDAO = membershipMappingDAO;
  }

  public void sendNotifications(final Application app,
                                final String scanId,
                                final Stage stage,
                                final List<PolicyAlert> policyAlerts)
  {
    // baseUrl uses ThreadContext to get the base URL. We need to get it before switching threads.
    final String stringBaseUrl = baseUrl.get();
    new Thread("PolicyAlertNotifierForScan-" + scanId)
    {
      @Override
      public void run() {
        String applicationPublicId = app.getPublicId();
        String mailServer = mail.getServer();
        Map<String, List<PolicyAlert>> alertsByRecipients = getPolicyAlertsByEmailAddresses(app, policyAlerts);
        if (alertsByRecipients.isEmpty()) {
          log.debug("Not sending notification emails for application {} and scan {} in stage {}"
              + ", no recipients configured for any violated policy", applicationPublicId, scanId, stage);
        }
        for (final Entry<String, List<PolicyAlert>> details : alertsByRecipients.entrySet()) {
          try {
            log.debug("Sending notification email via {} to {} for application {} and scan {} in stage {}", mailServer,
                details.getKey(), applicationPublicId, scanId, stage);
            final String mailId = "SONATYPE-CLM-" + applicationPublicId + '-' + scanId;
            final List<Address> addresses = Arrays.asList(new Address(details.getKey()));
            final String subject = createPolicyMailSubject(new MailPolicyAlertCounts(details.getValue()));
            final String body = summarizeThreats(stringBaseUrl, applicationPublicId, scanId, stage, details.getValue());
            mail.sendHtml(mailId, addresses, subject, body);
          }
          catch (final Exception e) {
            log.error("Unable to send notification email to {} for application {} and scan {} in stage {}",
                details.getKey(), applicationPublicId, scanId, stage, e);
          }
        }
      }
    }.start();
  }

  private Map<String, List<PolicyAlert>> getPolicyAlertsByEmailAddresses(Application app, final List<PolicyAlert> alerts)
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
              emailAddresses = getEmailAddressesForRole(app, roleId);
              emailAddressesByRoleId.put(roleId, emailAddresses);
            }
            for (String emailAddress : emailAddresses) {
              addPolicyAlert(policyAlertsByEmailAddress, emailAddress, alert);
            }
          }
          else {
            String emailAddress = action.getTarget();
            addPolicyAlert(policyAlertsByEmailAddress, emailAddress, alert);
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

  private Set<String> getEmailAddressesForRole(Application app, String roleId) {
    List<Member> members = new ArrayList<>();
    // Get role members from application on up
    for (Owner owner : ownerDAO.walkHierarchy(app.getId())) {
      for (MembershipMapping membershipMapping : membershipMappingDAO.getByContextIdAndRoleId(owner.getId(), roleId)) {
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

  static String createPolicyMailSubject(MailPolicyAlertCounts counts) {
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

  private String summarizeThreats(String baseUrl,
                                  final String applicationPublicId,
                                  final String scanId,
                                  final Stage stage,
                                  final List<PolicyAlert> policyAlerts) throws IOException
  {
    final Map<String, Object> model = createPolicyMailModel(baseUrl, mail.getCdnUrl(), applicationPublicId, scanId,
        stage, getContact(applicationPublicId), policyAlerts);
    return TemplateUtils.render(getPolicyThreatsTemplate(), model);
  }

  private ContactDTO getContact(String applicationPublicId) {
    Application application = applicationDAO.getByPublicIdNotNull(applicationPublicId);
    return applicationAdapter.getContact(application.getContactInternalName());
  }

  private synchronized static Template getPolicyThreatsTemplate() throws IOException {
    if (policyThreatsTemplate == null) {
      policyThreatsTemplate = TemplateUtils.createFreemarkerConfig().getTemplate("policythreats.ftl");
    }
    return policyThreatsTemplate;
  }

  static Map<String, Object> createPolicyMailModel(final String serverUrl,
                                                   final String cdnUrl,
                                                   final String applicationPublicId,
                                                   final String scanId,
                                                   final Stage stage,
                                                   ContactDTO contact,
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
    model.put("detailedReportUrl", serverUrl + UserInterfaceLinksResource.getReportUrl(applicationPublicId, scanId));
    model.put("policyAlerts", policyAlerts);
    model.put("policyThreatStage", StageTypes.getById(stage.getStageTypeId()).getName());
    model.put("policyThreatApp", applicationPublicId);
    model.put("policyThreatTime", new SimpleDateFormat("MMMM dd, yyyy", Locale.ENGLISH).format(new Date()));
    model.put("policyThreatRedCount", counts.red);
    model.put("policyThreatOrangeCount", counts.orange);
    model.put("policyThreatYellowCount", counts.yellow);
    model.put("policyThreatDarkBlueCount", counts.darkBlue);
    model.put("policyThreatBlueCount", counts.blue);
    model.put("actionTypes", ActionTypes.getAll());
    if (contact != null) {
      model.put("applicationContactEmail", contact.getEmail());
      model.put("applicationContactName", contact.getDisplayName());
    }

    return model;
  }

  static class MailPolicyAlertCounts
  {
    public int red, orange, yellow, darkBlue, blue;

    public MailPolicyAlertCounts(final int red, final int orange, final int yellow, final int darkBlue, final int blue)
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
