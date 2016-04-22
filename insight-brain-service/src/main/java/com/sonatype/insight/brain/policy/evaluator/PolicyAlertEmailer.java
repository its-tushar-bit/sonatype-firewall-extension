/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.clm.dto.model.policy.PolicyAlert;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.security.MembershipMappingDAO;
import com.sonatype.insight.brain.landing.UserInterfaceLinksResource;
import com.sonatype.insight.brain.ldap.LdapManager;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.organization.ApplicationAdapter;
import com.sonatype.insight.brain.organization.ContactDTO;
import com.sonatype.insight.brain.security.UserDirectory;
import com.sonatype.insight.brain.service.BaseUrl;
import com.sonatype.insight.brain.service.InsightMail;

import org.sonatype.micromailer.Address;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class used to send (email) notifications for policy alerts.
 *
 * @since 1.8
 */
@Named
public class PolicyAlertEmailer
    extends AbstractPolicyAlertEmailer
{
  private static final Logger log = LoggerFactory.getLogger(PolicyAlertEmailer.class);

  private final BaseUrl baseUrl;

  private final ApplicationAdapter applicationAdapter;

  @Inject
  public PolicyAlertEmailer(final InsightMail mail,
                            final BaseUrl baseUrl,
                            final ApplicationAdapter applicationAdapter,
                            final UserDirectory userDirectory,
                            final LdapManager ldapManager,
                            final OwnerDAO ownerDAO,
                            final MembershipMappingDAO membershipMappingDAO)
  {
    super(mail, userDirectory, ldapManager, ownerDAO, membershipMappingDAO);
    this.baseUrl = baseUrl;
    this.applicationAdapter = applicationAdapter;
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
        String mailServer = getMail().getServer();
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
            final String body = processTemplate(
                createPolicyMailModel(stringBaseUrl, app, scanId, stage, details.getValue()));
            getMail().sendHtml(mailId, addresses, subject, body);
          }
          catch (final Exception e) {
            log.error("Unable to send notification email to {} for application {} and scan {} in stage {}",
                details.getKey(), applicationPublicId, scanId, stage, e);
          }
        }
      }
    }.start();
  }

  protected Map<String, Object> createPolicyMailModel(String serverBaseUrl,
                                                      Application app,
                                                      String scanId,
                                                      Stage stage,
                                                      List<PolicyAlert> policyAlerts)
  {
    Map<String, Object> model = createPolicyMailModel(getMail().getCdnUrl(), app, stage, policyAlerts);
    ContactDTO contact = applicationAdapter.getContact(app.getContactInternalName());
    if (contact != null) {
      model.put("applicationContactEmail", contact.getEmail());
      model.put("applicationContactName", contact.getDisplayName());
    }
    model.put("detailedReportUrl", serverBaseUrl + UserInterfaceLinksResource.getReportUrl(app.getPublicId(), scanId));
    model.put("ownerIdLabel", "APP ID");

    return model;
  }
}
