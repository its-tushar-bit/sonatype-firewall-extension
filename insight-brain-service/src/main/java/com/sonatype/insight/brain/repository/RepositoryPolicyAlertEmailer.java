/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor.AbortPolicy;
import java.util.concurrent.TimeUnit;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sonatype.clm.dto.model.policy.PolicyFact;
import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.AuditRecorder;
import com.sonatype.insight.brain.audit.AuditSession;
import com.sonatype.insight.brain.landing.UserInterfaceLinksHelper;
import com.sonatype.insight.brain.model.policy.notifications.PolicyNotification;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.policy.evaluator.AbstractPolicyAlertEmailer;
import com.sonatype.insight.brain.policy.evaluator.PolicyAlertCounts;
import com.sonatype.insight.brain.policy.evaluator.PolicyAlertEmailResolver;
import com.sonatype.insight.brain.service.BaseUrl;
import com.sonatype.insight.brain.service.InsightMail;
import com.sonatype.insight.brain.shutdown.ShutdownHandler;
import com.sonatype.insight.brain.shutdown.ShutdownPriority;
import com.sonatype.insight.brain.tenancy.TenantThreadPoolExecutor;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
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

  private final AuditRecorder auditRecorder;

  private final ThreadPoolExecutor executor;

  @Inject
  public RepositoryPolicyAlertEmailer(
      final InsightMail mail,
      final PolicyAlertEmailResolver policyAlertEmailResolver,
      final BaseUrl baseUrl,
      final AuditRecorder auditRecorder,
      final ShutdownHandler shutdownHandler)
  {
    super(mail, policyAlertEmailResolver);
    this.baseUrl = baseUrl;
    this.auditRecorder = auditRecorder;

    executor = new TenantThreadPoolExecutor(1000, 1000, 1L, TimeUnit.SECONDS, new LinkedBlockingQueue<>(),
        new ThreadFactoryBuilder().setNameFormat("RepositoryPolicyAlertEmailNotifier-%d").build(), new AbortPolicy(),
        "repository_policy_alert_emailer", "RepositoryPolicyAlertEmailer");
    executor.allowCoreThreadTimeOut(true);
    shutdownHandler.add(executor, ShutdownPriority.NOTIFICATIONS);
  }

  // Visible for testing
  ThreadPoolExecutor getExecutor() {
    return executor;
  }

  public void sendNotifications(Repository repository, List<PolicyNotification> notifications) {
    executor.execute(() -> {
      try {
        Map<String, List<PolicyFact>> policyFactsByEmailAddress =
            getPolicyFactsByEmailAddress(repository, notifications);
        for (final Entry<String, List<PolicyFact>> details : policyFactsByEmailAddress.entrySet()) {
          try (AuditSession auditSession = auditRecorder.recordSystemEvent(AuditEvent.SEND_MAIL)) {
            try {
              log.debug("Sending notification email via {} to {} for repository {} ({})", getMail().getServer(),
                  details.getKey(), repository.getPublicId(), repository.getId());
              AuditData.get().setRepository(repository).setData("emailAddress", details.getKey());
              PolicyAlertCounts policyAlertCounts = new PolicyAlertCounts(details.getValue());
              AuditData.get().setData("totalPolicyViolationCount", policyAlertCounts.getTotal());
              final String subject = createPolicyMailSubject(policyAlertCounts, repository.getName(), null);
              final String body = createPolicyMailBody(createPolicyMailModel(repository, details.getValue()));
              getMail().sendHtml(details.getKey(), subject, body);
            }
            catch (final Exception e) {
              log.error("Unable to send notification email to {} for repository {} ({})", details.getKey(),
                  repository.getPublicId(), repository.getId(), e);
              AuditData.get().setException(e);
            }
          }
        }
      }
      catch (RuntimeException e) {
        log.error("Error while sending repository policy email notifications: {}", e.getMessage(), e);
      }
      catch (Throwable t) {
        // Try to log to stderr before trying the standard logging because the standard logging may not be operational
        // at this point.
        t.printStackTrace();
        log.error(t.getMessage(), t);
        System.exit(1);
      }
    });
  }

  protected Map<String, Object> createPolicyMailModel(Repository repository, List<PolicyFact> policyFacts) {
    Map<String, Object> model = createPolicyMailModel(getMail().getCdnUrl(), repository, StageTypes.PROXY, policyFacts);

    model.put("detailedReportUrl",
        baseUrl.getConfigured() + UserInterfaceLinksHelper.getRepositoryReportUrl(repository.getId()));
    model.put("ownerIdLabel", "REPO ID");

    return model;
  }
}
