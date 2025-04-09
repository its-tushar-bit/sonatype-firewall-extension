/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.policy.evaluator;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.git.PullRequestCommentingRemediationService;
import com.sonatype.insight.brain.git.RemediationPullRequestEligibilityService;
import com.sonatype.insight.brain.git.RemediationVersionDTO;
import com.sonatype.insight.brain.git.pullrequestcreationservice.AutomatedPullRequestCreationService;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.notifications.PolicyNotification;
import com.sonatype.insight.brain.shutdown.ShutdownHandler;
import com.sonatype.insight.brain.shutdown.ShutdownPriority;
import com.sonatype.insight.brain.tenancy.TenantAwareOneTimeRunnable;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Suppliers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class to send notifications of policy alerts to Source Code Management
 * systems like github
 */
@Named
@Singleton
public class PolicyAlertScmNotifier
{
  private static final Logger log = LoggerFactory.getLogger(PolicyAlertScmNotifier.class);

  private final AutomatedPullRequestCreationService automatedPullRequestCreationService;

  private final RemediationPullRequestEligibilityService remediationPullRequestEligibilityService;

  private final PullRequestCommentingRemediationService pullRequestCommentingRemediationService;

  private final PolicyAlertSourceCodeOrganizer policyAlertSourceCodeOrganizer;

  private final ShutdownHandler shutdownHandler;

  @VisibleForTesting
  PullRequestInvoker pullRequestInvoker = new PullRequestInvoker();

  /**
   * notifier for sending to hosted git source control manager service
   *
   * @param automatedPullRequestCreationService service for creating remediation pull request
   * @param policyAlertSourceCodeOrganizer     service to aggregate policy alerts
   * @param shutdownHandler shutdown handler
   */
  @Inject
  public PolicyAlertScmNotifier(
      final AutomatedPullRequestCreationService automatedPullRequestCreationService,
      final RemediationPullRequestEligibilityService remediationPullRequestEligibilityService,
      final PullRequestCommentingRemediationService pullRequestCommentingRemediationService,
      final PolicyAlertSourceCodeOrganizer policyAlertSourceCodeOrganizer,
      final ShutdownHandler shutdownHandler
  )
  {
    this.automatedPullRequestCreationService = automatedPullRequestCreationService;
    this.remediationPullRequestEligibilityService = remediationPullRequestEligibilityService;
    this.pullRequestCommentingRemediationService = pullRequestCommentingRemediationService;
    this.policyAlertSourceCodeOrganizer = policyAlertSourceCodeOrganizer;
    this.shutdownHandler = shutdownHandler;
  }

  /**
   * send a notification to git hosting service
   *
   * @param app                 application with policy notifications
   * @param policyNotifications policy notifications
   */
  public void sendNotifications(
      final Application app,
      final String scanId,
      final Stage stage,
      final List<PolicyNotification> policyNotifications)
  {
    pullRequestInvoker.execute(scanId, () -> {
      try {
        internalSendNotification(app, scanId, stage, policyNotifications);
      }
      catch (final Exception e) {
        log.error("Unable to send PullRequest notification for application {} and scan {} in stage {}",
            app.getPublicId(), scanId, stage, e);
      }
    });
  }

  private void internalSendNotification(
      final Application app,
      final String scanId,
      final Stage stage,
      final List<PolicyNotification> policyNotifications)
  {
    // aggregate by component and loop each one
    Map<ComponentIdentifier, List<PolicyNotification>> sortedComponentAlerts =
        policyAlertSourceCodeOrganizer.getNotificationsForScm(policyNotifications);
    sortedComponentAlerts.forEach((componentIdentifier, notifications) -> {
      Supplier<Optional<RemediationVersionDTO>> remediationVersionDTOSupplier = Suppliers.memoize(
          () -> pullRequestCommentingRemediationService.getRemediationVersion(componentIdentifier, app.getId()));
      try {
        automatedPullRequestCreationService.createAutomatedRemediationPullRequest(
            app,
            scanId,
            stage,
            componentIdentifier,
            remediationVersionDTOSupplier,
            notifications);
      }
      catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    });
  }

  /**
   * Invoke the PR runnable in a named thread. Package-private to allow for mocking in tests.
   */
  class PullRequestInvoker
  {
    public void execute(final String scanId, Runnable runnable) {
      Thread scmNotificationThread =
          new Thread(new TenantAwareOneTimeRunnable(runnable), "PolicyAlertScmNotifierForScan-" + scanId);
      shutdownHandler.add(scmNotificationThread, ShutdownPriority.NOTIFICATIONS);
      scmNotificationThread.start();
    }
  }
}
