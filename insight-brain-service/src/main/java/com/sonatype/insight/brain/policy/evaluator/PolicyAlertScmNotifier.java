/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.policy.evaluator;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.git.PullRequestCommentingRemediationService;
import com.sonatype.insight.brain.git.RemediationVersionDTO;
import com.sonatype.insight.brain.git.pullrequestcreationservice.AutomatedPullRequestCreationService;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.policy.notifications.PolicyNotification;
import com.sonatype.insight.brain.shutdown.ShutdownHandler;
import com.sonatype.insight.brain.shutdown.ShutdownPriority;
import com.sonatype.insight.brain.tenancy.TenantAwareOneTimeRunnable;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Suppliers;
import org.apache.commons.collections.CollectionUtils;
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

  /** Prefix of the async SCM-notifier thread name (per scan); shared with tests that join on it. */
  public static final String THREAD_NAME_PREFIX = "PolicyAlertScmNotifierForScan-";

  private final AutomatedPullRequestCreationService automatedPullRequestCreationService;

  private final PullRequestCommentingRemediationService pullRequestCommentingRemediationService;

  private final PolicyAlertSourceCodeOrganizer policyAlertSourceCodeOrganizer;

  private final ShutdownHandler shutdownHandler;

  private final ReportComponentService reportComponentService;

  @VisibleForTesting
  PullRequestInvoker pullRequestInvoker = new PullRequestInvoker();

  /**
   * notifier for sending to hosted git source control manager service
   *
   * @param automatedPullRequestCreationService service for creating remediation pull request
   * @param policyAlertSourceCodeOrganizer service to aggregate policy alerts
   * @param shutdownHandler shutdown handler
   * @param reportComponentService service to fetch components info from a report
   */
  @Inject
  public PolicyAlertScmNotifier(
      final AutomatedPullRequestCreationService automatedPullRequestCreationService,
      final PullRequestCommentingRemediationService pullRequestCommentingRemediationService,
      final PolicyAlertSourceCodeOrganizer policyAlertSourceCodeOrganizer,
      final ShutdownHandler shutdownHandler,
      final ReportComponentService reportComponentService)
  {
    this.automatedPullRequestCreationService = automatedPullRequestCreationService;
    this.pullRequestCommentingRemediationService = pullRequestCommentingRemediationService;
    this.policyAlertSourceCodeOrganizer = policyAlertSourceCodeOrganizer;
    this.shutdownHandler = shutdownHandler;
    this.reportComponentService = reportComponentService;
  }

  /**
   * send a notification to git hosting service
   *
   * @param app application with policy notifications
   * @param policyNotifications policy notifications
   */
  public void sendNotifications(
      final Application app,
      final String scanId,
      final Stage stage,
      final List<PolicyNotification> policyNotifications,
      final String scannedBranchName)
  {
    pullRequestInvoker.execute(scanId, () -> {
      try {
        internalSendNotification(app, scanId, stage, policyNotifications, scannedBranchName);
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
      final List<PolicyNotification> policyNotifications,
      final String scannedBranchName)
  {
    Map<ComponentIdentifier, Boolean> directDependencies =
        fetchDirectDependenciesMap(app, scanId, stage.getStageTypeId());

    Map<ComponentIdentifier, List<PolicyNotification>> sortedComponentAlerts =
        policyAlertSourceCodeOrganizer.getNotificationsForScm(policyNotifications);
    sortedComponentAlerts.forEach((componentIdentifier, notifications) -> {
      // If dependency type cannot be specified, default to true (direct dependency)
      boolean isDirectDependency = directDependencies.getOrDefault(componentIdentifier, true);

      Supplier<Optional<RemediationVersionDTO>> remediationVersionDTOSupplier = Suppliers.memoize(
          () -> pullRequestCommentingRemediationService.getRemediationVersion(componentIdentifier, app.getId()));
      try {
        automatedPullRequestCreationService.createAutomatedRemediationPullRequest(
            app,
            scanId,
            stage,
            componentIdentifier,
            remediationVersionDTOSupplier,
            notifications,
            isDirectDependency,
            scannedBranchName);
      }
      catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    });
  }

  private Map<ComponentIdentifier, Boolean> fetchDirectDependenciesMap(
      final Application app,
      final String scanId,
      final String stageTypeId)
  {
    try {
      ReportComponentData data = reportComponentService.fetchReportAndComponents(app, scanId, stageTypeId);
      if (data == null || CollectionUtils.isEmpty(data.components)) {
        log.warn("Report for application '{}' and scanId '{}' is null or empty.", app.getPublicId(), scanId);
        return Collections.emptyMap();
      }

      Map<ComponentIdentifier, Boolean> dependencyTypes = new HashMap<>(data.components.size());
      for (Component component : data.components) {
        dependencyTypes.put(component.getComponentIdentifier(), !Boolean.FALSE.equals(component.getDirectDependency()));
      }
      return dependencyTypes;
    }
    catch (Exception e) {
      log.warn("Failed to fetch report for application '{}' and scanId '{}'.", app.getPublicId(), scanId, e);
      return Collections.emptyMap();
    }
  }

  /**
   * Invoke the PR runnable in a named thread. Package-private to allow for mocking in tests.
   */
  class PullRequestInvoker
  {
    public void execute(final String scanId, Runnable runnable) {
      Thread scmNotificationThread =
          new Thread(new TenantAwareOneTimeRunnable(runnable), THREAD_NAME_PREFIX + scanId);
      shutdownHandler.add(scmNotificationThread, ShutdownPriority.NOTIFICATIONS);
      scmNotificationThread.start();
    }
  }
}
