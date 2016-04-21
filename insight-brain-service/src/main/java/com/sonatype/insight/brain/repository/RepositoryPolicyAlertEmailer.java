/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.clm.dto.model.policy.PolicyAlert;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.security.MembershipMappingDAO;
import com.sonatype.insight.brain.ldap.LdapManager;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.policy.evaluator.AbstractPolicyAlertEmailer;
import com.sonatype.insight.brain.security.UserDirectory;
import com.sonatype.insight.brain.service.InsightMail;

import org.sonatype.micromailer.Address;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since 1.21
 */
@Named
public class RepositoryPolicyAlertEmailer
    extends AbstractPolicyAlertEmailer
{
  private static final Logger log = LoggerFactory.getLogger(RepositoryPolicyAlertEmailer.class);

  @Inject
  public RepositoryPolicyAlertEmailer(final InsightMail mail,
                                      final UserDirectory userDirectory,
                                      final LdapManager ldapManager,
                                      final OwnerDAO ownerDAO,
                                      final MembershipMappingDAO membershipMappingDAO)
  {
    super(mail, userDirectory, ldapManager, ownerDAO, membershipMappingDAO);
  }

  public void sendNotifications(Repository repository, List<PolicyAlert> alerts) {
    Map<String, List<PolicyAlert>> alertsByRecipients = getPolicyAlertsByEmailAddresses(repository, alerts);
    for (final Entry<String, List<PolicyAlert>> details : alertsByRecipients.entrySet()) {
      try {
        log.debug("Sending notification email via {} to {} for repository {}", getMail().getServer(), details.getKey(),
            repository.getId());
        final String mailId = "SONATYPE-IQ-" + repository.getPublicId();
        final List<Address> addresses = Collections.singletonList(new Address(details.getKey()));
        //TODO: subject/body and send should be handled by CLM-6416
        //final String subject = createPolicyMailSubject(new MailPolicyAlertCounts(details.getValue()));
        //final String body = summarizeThreats(stringBaseUrl, applicationPublicId, scanId, stage, details.getValue());
        getMail().sendHtml(mailId, addresses, "subject", "body");
      }
      catch (final Exception e) {
        log.error("Unable to send notification email to {} for repository {}", details.getKey(), repository.getId(), e);
      }
    }
  }
}
