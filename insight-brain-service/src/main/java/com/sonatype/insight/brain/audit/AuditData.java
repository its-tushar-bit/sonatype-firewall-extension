/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.audit;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.function.Function;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.api.v2.dto.ApiSbomVulnerabilityAnalysisRequestDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiSbomVulnerabilityAnalysisRequestDTO.ComponentLocator;
import com.sonatype.insight.brain.git.dto.ImportScmOrganizationRequest;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.brain.model.license.LicenseThreatGroup;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.notifications.Notifications;
import com.sonatype.insight.brain.model.repository.HostedRepositoryComponent;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.model.successmetrics.SuccessMetricsReport;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadata;
import com.sonatype.insight.brain.policy.ActionDTO;
import com.sonatype.insight.brain.policy.ConstraintDTO;
import com.sonatype.insight.brain.policy.NotificationDTO;
import com.sonatype.insight.brain.tenancy.TenantAwareOneTimeRunnable;
import com.sonatype.insight.brain.thirdparty.SbomAction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The data for one audit record. Code populates audit data for the current operation/event using
 * {@link AuditData#get()}.
 */
public abstract class AuditData
{
  private static final Logger log = LoggerFactory.getLogger(AuditData.class);

  /**
   * Gets the data for the current audit event. Note that this method can safely be called from anywhere in the code and
   * from any thread: It never returns {@code null}.
   */
  public static AuditData get() {
    return AuditSession.getCurrent();
  }

  /**
   * Starts a nested audit event that inherits the user and request information from the current event.
   * <p>
   * If the event is declared independent, it will be committed to the audit log once the returned audit session is
   * closed. Logging of a dependent event on the other hand will be deferred until either the parent event is committed
   * or {@link #commitSubEvents()} gets invoked on the parent event.
   */
  public final AuditSession recordSubEvent(AuditEvent event, boolean independent) {
    return new AuditSession(forSubEvent(event, independent, false));
  }

  /**
   * Starts a nested audit event that does not inherit the user or request information from the current event. Instead,
   * the audited operation is attributed to the system/server itself.
   * <p>
   * If the event is declared independent, it will be committed to the audit log once the returned audit session is
   * closed. Logging of a dependent event on the other hand will be deferred until either the parent event is committed
   * or {@link #commitSubEvents()} gets invoked on the parent event.
   */
  public final AuditSession recordSystemEvent(AuditEvent event, boolean independent) {
    return new AuditSession(forSubEvent(event, independent, true));
  }

  protected abstract AuditData forSubEvent(AuditEvent event, boolean independent, boolean system);

  /**
   * Continue audit logging of asynchronous task scheduled using
   * {@link Executor#execute(Runnable)}.
   */
  public final void continueAsync(Executor executor, Runnable task) {
    continueAsync(task, runnable -> {
      executor.execute(runnable);
      return null;
    });
  }

  /**
   * Continue audit logging of asynchronous task scheduled using
   * {@link java.util.concurrent.ExecutorService#submit(Runnable)} or similar.
   */
  public final <F> F continueAsync(Runnable task, Function<Runnable, F> taskSubmitter) {
    return continueAsync(auditData -> {
      Runnable auditedTask = new TenantAwareOneTimeRunnable(() -> {
        try (AuditSession auditSession = new AuditSession(auditData)) {
          try {
            task.run();
          }
          catch (Exception e) {
            auditData.setException(e);
          }
          catch (Throwable t) {
            // Try to log to stderr before trying the standard logging because the standard logging may not be
            // operational at this point.
            t.printStackTrace();
            log.error(t.getMessage(), t);
            System.exit(1);
          }
        }
      });
      return taskSubmitter.apply(auditedTask);
    });
  }

  protected abstract <F> F continueAsync(Function<AuditData, F> taskSubmitter);

  protected abstract void commit();

  /**
   * Commits all dependent sub events of this event to the audit log. This is typically done after a database
   * transaction is successfully committed, thereby flushing the sub events for changes audited during that database
   * transaction.
   */
  public abstract void commitSubEvents();

  public abstract void setUsername(String username);

  public abstract AuditEvent getEvent();

  public abstract void setEvent(AuditEvent event);

  public abstract void setError(String error);

  public abstract void setException(Throwable error);

  public abstract void setHttpStatus(int httpStatus);

  public abstract AuditData setData(String key, Object value);

  public AuditData setEnum(String key, Enum<?> enumValue) {
    setData(key, enumValue != null ? enumValue.name().toLowerCase(Locale.ROOT).replace('_', '-') : null);
    return this;
  }

  public AuditData setApplicationWithDetails(Application application) {
    if (application != null) {
      setApplication(application);
      setData("contactUsername", application.getContactInternalName());
    }
    return this;
  }

  public AuditData setApplication(Application application) {
    if (application != null) {
      setApplicationId(application.getId());
      setApplicationPublicId(application.getPublicId());
      setApplicationName(application.getName());
    }
    return this;
  }

  AuditData setApplicationId(String applicationId) {
    setData("applicationId", applicationId);
    return this;
  }

  AuditData setApplicationPublicId(String applicationPublicId) {
    setData("applicationPublicId", applicationPublicId);
    return this;
  }

  AuditData setApplicationName(String applicationName) {
    setData("applicationName", applicationName);
    return this;
  }

  public AuditData setOrganization(Organization organization) {
    if (organization != null) {
      setOrganizationId(organization.getId());
      setOrganizationName(organization.getName());
    }
    return this;
  }

  AuditData setOrganizationId(String organizationId) {
    setData("organizationId", organizationId);
    return this;
  }

  AuditData setOrganizationName(String organizationName) {
    setData("organizationName", organizationName);
    return this;
  }

  public AuditData setRepositoryManagerInstanceId(String repositoryManagerInstanceId) {
    setData("repositoryManagerInstanceId", repositoryManagerInstanceId);
    return this;
  }

  public AuditData setRepositoryManagerId(String repositoryManagerId) {
    setData("repositoryManagerId", repositoryManagerId);
    return this;
  }

  public AuditData setRepositoryManagerName(String repositoryManagerName) {
    setData("repositoryManagerName", repositoryManagerName);
    return this;
  }

  public AuditData setRepository(Repository repository) {
    if (repository != null) {
      setRepositoryId(repository.getId());
      setRepositoryPublicId(repository.getPublicId());
      setData("format", repository.getFormat());
      setData("type", repository.getRepositoryType() == null ? null : repository.getRepositoryType().name());
      setData("auditEnabled", repository.isAuditEnabled());
      setData("quarantineEnabled", repository.isQuarantineEnabled());
      setData("policyCompliantComponentSelectionEnabled", repository.isPolicyCompliantComponentSelectionEnabled());
      setData("namespaceConfusionProtectionEnabled", repository.isNamespaceConfusionProtectionEnabled());
    }
    return this;
  }

  AuditData setRepositoryId(String repositoryId) {
    setData("repositoryId", repositoryId);
    return this;
  }

  public AuditData setRepositoryPublicId(String repositoryPublicId) {
    setData("repositoryPublicId", repositoryPublicId);
    return this;
  }

  AuditData setRepositoryContainer() {
    setData("scope", "all-repositories");
    return this;
  }

  public AuditData setRepositoryManager(RepositoryManager repositoryManager) {
    if (repositoryManager != null) {
      setRepositoryManagerId(repositoryManager.getId());
      setRepositoryManagerInstanceId(repositoryManager.getInstanceId());
      setRepositoryManagerName(repositoryManager.getName());
    }
    return this;
  }

  AuditData setGlobal() {
    setData("scope", "global");
    return this;
  }

  public AuditData setStageId(String stageId) {
    setData("stageId", stageId);
    return this;
  }

  public AuditData setScanId(String scanId) {
    setData("scanId", scanId);
    return this;
  }

  public AuditData setIsReevaluation(boolean isReevaluation) {
    setData("isReevaluation", isReevaluation);
    return this;
  }

  public AuditData setComponentHash(String componentHash) {
    setData("componentHash", componentHash);
    return this;
  }

  public AuditData setComment(String comment) {
    setData("comment", comment);
    return this;
  }

  public AuditData setComponentIdentifier(ComponentIdentifier componentIdentifier) {
    setData("componentIdentifier", componentIdentifier);
    return this;
  }

  public AuditData setLabel(Label label) {
    if (label != null) {
      setLabelId(label.getId());
      setLabelName(label.getLabel());
    }
    return this;
  }

  AuditData setLabelId(String labelId) {
    setData("labelId", labelId);
    return this;
  }

  AuditData setLabelName(String labelName) {
    setData("labelName", labelName);
    return this;
  }

  public AuditData setPolicyWithDetails(Policy policy) {
    if (policy != null) {
      setPolicy(policy);
      setPolicyThreatLevel(policy.getThreatLevel());
      setLegacyViolationMode(policy.isLegacyViolationAllowed());
      setPolicyActionsOverrideMode(policy.isPolicyActionsOverrideAllowed());
      setPolicyNotificationsOverrideMode(policy.isPolicyNotificationsOverrideAllowed());
      setPolicyConstraints(policy.getConstraints());
      setPolicyActions(policy.getActions());
      setPolicyNotifications(policy.getNotifications());
    }
    return this;
  }

  AuditData setPolicyNotifications(final Notifications notifications) {
    setData("notifications", NotificationDTO.transcribe(notifications));
    return this;
  }

  AuditData setPolicyActions(final Map<String, String> actions) {
    setData("actions", ActionDTO.transcribe(actions));
    return this;
  }

  AuditData setPolicyConstraints(final List<Constraint> policyConstraints) {
    setData("policyConstraints", ConstraintDTO.transcribe(policyConstraints));
    return this;
  }

  AuditData setLegacyViolationMode(final boolean legacyViolationAllowed) {
    setData("legacyViolationMode", legacyViolationAllowed ? "allow" : "disallow");
    return this;
  }

  AuditData setPolicyActionsOverrideMode(final boolean policyActionsOverrideAllowed) {
    setData("policyActionsOverrideMode", policyActionsOverrideAllowed ? "allow" : "disallow");
    return this;
  }

  AuditData setPolicyNotificationsOverrideMode(final boolean policyNotificationsOverrideAllowed) {
    setData("setPolicyNotificationsOverrideMode", policyNotificationsOverrideAllowed ? "allow" : "disallow");
    return this;
  }

  AuditData setPolicyThreatLevel(final int threatLevel) {
    setData("policyThreatLevel", threatLevel);
    return this;
  }

  public AuditData setPolicy(Policy policy) {
    if (policy != null) {
      setPolicyId(policy.getId());
      setPolicyName(policy.getName());
    }
    return this;
  }

  AuditData setPolicyId(String policyId) {
    setData("policyId", policyId);
    return this;
  }

  AuditData setPolicyName(String policyName) {
    setData("policyName", policyName);
    return this;
  }

  public AuditData setLicenseThreatGroup(LicenseThreatGroup licenseThreatGroup) {
    if (licenseThreatGroup != null) {
      setLicenseThreatGroupId(licenseThreatGroup.getId());
      setLicenseThreatGroupName(licenseThreatGroup.getName());
    }
    return this;
  }

  AuditData setLicenseThreatGroupId(String ltgId) {
    setData("licenseThreatGroupId", ltgId);
    return this;
  }

  AuditData setLicenseThreatGroupName(String ltgName) {
    setData("licenseThreatGroupName", ltgName);
    return this;
  }

  public AuditData setInheritanceScope(final List<ApplicationCategoryAuditDTO> applicationCategories) {
    if (applicationCategories.isEmpty()) {
      setData("inheritanceScope", "all-children");
    }
    else {
      setData("inheritanceScope", "matching-application-category");
      setApplicationCategories(applicationCategories);
    }
    return this;
  }

  public AuditData setApplicationCategories(final List<ApplicationCategoryAuditDTO> applicationCategories) {
    setData("applicationCategories", applicationCategories);
    return this;
  }

  public AuditData setSelectedApplicationCategories(List<ApplicationCategoryAuditDTO> applicationCategories) {
    setData("selectedApplicationCategories", applicationCategories);
    return this;
  }

  public AuditData setOwner(Owner owner) {
    if (owner != null) {
      switch (owner.getType()) {
        case APPLICATION:
          return setApplication((Application) owner);
        case ORGANIZATION:
          return setOrganization((Organization) owner);
        case REPOSITORY:
          return setRepository((Repository) owner);
        case REPOSITORY_MANAGER:
          return setRepositoryManager((RepositoryManager) owner);
        case REPOSITORY_CONTAINER:
          return setRepositoryContainer();
        case HOSTED_REPOSITORY_COMPONENT:
          return setHostedRepositoryComponent((HostedRepositoryComponent) owner);
        default:
          throw new IllegalArgumentException("unsupported owner type " + owner.getType());
      }
    }
    return this;
  }

  public AuditData setHostedRepositoryComponent(HostedRepositoryComponent hrc) {
    if (hrc != null) {
      setData("hostedRepositoryComponentId", hrc.getId());
      setData("repositoryId", hrc.getRepositoryId());
      setData("pathname", hrc.getPathname());
      setData("hash", hrc.getHash());
    }
    return this;
  }

  public AuditData setParentOrganization(final Organization parentOrganization) {
    if (parentOrganization != null) {
      setData("parentOrganizationId", parentOrganization.getId());
      setData("parentOrganizationName", parentOrganization.getName());
    }
    return this;
  }

  public AuditData setReportId(final String reportId) {
    setData("reportId", reportId);
    return this;
  }

  public AuditData setSuccessMetricsReport(final SuccessMetricsReport successMetricsReport) {
    if (successMetricsReport != null) {
      setData("reportId", successMetricsReport.getId());
      setData("reportName", successMetricsReport.getName());
    }
    return this;
  }

  public AuditData setScmImportEvent(final ImportScmOrganizationRequest importRequest) {
    if (importRequest != null) {
      setData("scmHostUrl", importRequest.scmHostUrl);
      setData("desiredSubOrganizationCount", importRequest.desiredSubOrganizationCount);
      setData("importLimit", importRequest.importLimit);
    }
    return this;
  }

  public AuditData setLookerDashboard(final String dashboardId) {
    if (dashboardId != null) {
      setData("dashboard", dashboardId);
    }
    return this;
  }

  public AuditData setSystemConfigurationPropertyFeature(
      final SystemConfigurationPropertyFeature systemConfigurationPropertyFeature)
  {
    if (systemConfigurationPropertyFeature.isStored()) {
      AuditData.get()
          .setData(systemConfigurationPropertyFeature.getPropertyName(),
              systemConfigurationPropertyFeature.getPropertyValue());
    }
    else {
      AuditData.get().setData(systemConfigurationPropertyFeature.getPropertyName(), "null");
    }
    return this;
  }

  public AuditData setSbomVersion(final ThirdPartySbomMetadata sbomMetadata, final SbomAction action) {
    setData("applicationId", sbomMetadata.getApplicationId());
    setData("sbomVersion", sbomMetadata.getSbomVersion());
    if (action != null) {
      setData("status", sbomMetadata.getStatus());
      setData("operation", action);
    }
    return this;
  }

  public void setVulnerability(
      final ApiSbomVulnerabilityAnalysisRequestDTO analysisRequestDto,
      final String refId)
  {
    if (analysisRequestDto != null && analysisRequestDto.getComponentLocator() != null) {
      ComponentLocator componentLocator = analysisRequestDto.getComponentLocator();
      setData("packageUrl", componentLocator.getPackageUrl());
      setData("componentHash", componentLocator.getHash());
    }
    setData("vulnerabilityReference", refId);
  }

  public void setVulnerability(
      final ComponentLocator componentLocator,
      final String refId)
  {
    if (componentLocator != null) {
      setData("packageUrl", componentLocator.getPackageUrl());
      setData("componentHash", componentLocator.getHash());
    }
    setData("vulnerabilityReference", refId);
  }

  public void setVersionEvaluationWindow(
      final String contextId,
      final Integer maxVersions,
      final Integer maxAgeInDays)
  {
    setData("contextId", contextId);
    setData("maxVersions", maxVersions);
    setData("maxAgeInDays", maxAgeInDays);
  }
}
