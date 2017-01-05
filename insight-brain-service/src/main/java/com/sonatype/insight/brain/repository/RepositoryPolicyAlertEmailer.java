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

import com.sonatype.clm.dto.model.policy.PolicyFact;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.configuration.ldap.LdapManager;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.security.MembershipMappingDAO;
import com.sonatype.insight.brain.landing.UserInterfaceLinksResource;
import com.sonatype.insight.brain.model.policy.notifications.PolicyNotification;
import com.sonatype.insight.brain.model.policy.stages.ProxyStageType;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.policy.evaluator.AbstractPolicyAlertEmailer;
import com.sonatype.insight.brain.policy.evaluator.PolicyAlertCounts;
import com.sonatype.insight.brain.security.UserDirectory;
import com.sonatype.insight.brain.service.BaseUrl;
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

  private final BaseUrl baseUrl;

  @Inject
  public RepositoryPolicyAlertEmailer(final InsightMail mail,
                                      final UserDirectory userDirectory,
                                      final LdapManager ldapManager,
                                      final OwnerDAO ownerDAO,
                                      final MembershipMappingDAO membershipMappingDAO,
                                      final BaseUrl baseUrl)
  {
    super(mail, userDirectory, ldapManager, ownerDAO, membershipMappingDAO);
    this.baseUrl = baseUrl;
  }

  public void sendNotifications(Repository repository, List<PolicyNotification> notifications) {
    Map<String, List<PolicyFact>> policyFactsByEmailAddress = getPolicyFactsByEmailAddress(repository, notifications);
    for (final Entry<String, List<PolicyFact>> details : policyFactsByEmailAddress.entrySet()) {
      try {
        log.debug("Sending notification email via {} to {} for repository {}", getMail().getServer(), details.getKey(),
            repository.getId());
        final String mailId = "SONATYPE-IQ-" + repository.getPublicId();
        final List<Address> addresses = Collections.singletonList(new Address(details.getKey()));
        final String subject = createPolicyMailSubject(new PolicyAlertCounts(details.getValue()),
            repository.getName());
        final String body = createPolicyMailBody(createPolicyMailModel(repository, details.getValue()));
        getMail().sendHtml(mailId, addresses, subject, body);
      }
      catch (final Exception e) {
        log.error("Unable to send notification email to {} for repository {}", details.getKey(), repository.getId(), e);
      }
    }
  }

  protected Map<String, Object> createPolicyMailModel(Repository repository, List<PolicyFact> policyFacts)
  {
    Map<String, Object> model = createPolicyMailModel(getMail().getCdnUrl(), repository, new Stage(ProxyStageType.ID),
        policyFacts);

    model.put("detailedReportUrl",
        baseUrl.get() + UserInterfaceLinksResource.getRepositoryReportUrl(repository.getId()));
    model.put("ownerIdLabel", "REPO ID");

    return model;
  }
}
