/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.integration;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sonatype.clm.dto.model.application.ApplicationSummaryList;
import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.AuditSession;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.configuration.AutomaticApplicationsConfigurationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.organization.ApplicationHelper;
import com.sonatype.insight.brain.product.license.InvalidLicenseException;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.security.AuthzFilter;
import com.sonatype.insight.brain.telemetry.OwnerMaintenanceTelemetry;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.brain.telemetry.TelemetryUtils;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.authz.UnauthorizedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
public class ApplicationSummaryService
{
  private static final Logger log = LoggerFactory.getLogger(ApplicationSummaryService.class);

  static final String APP_CREATED_AUTOMATICALLY_TELEMETRY_ATTR = "application_created_automatically";

  private static final Comparator<Application> APP_COMPARATOR = new Comparator<>()
  {
    @Override
    public int compare(Application a1, Application a2) {
      return a1.getName().compareToIgnoreCase(a2.getName());
    }
  };

  private final ApplicationDAO applicationDAO;

  private final PolicyEvaluationDAO policyEvaluationDAO;

  private final AutomaticApplicationsConfigurationDAO automaticApplicationsConfigurationDAO;

  private final TelemetrySender telemetrySender;

  private final ApplicationHelper applicationHelper;

  private final OrganizationDAO organizationDAO;

  private final ProductLicense productLicense;

  private final TelemetryUtils telemetryUtils;

  @Inject
  public ApplicationSummaryService(
      final ApplicationDAO applicationDAO,
      PolicyEvaluationDAO policyEvaluationDAO,
      final AutomaticApplicationsConfigurationDAO automaticApplicationsConfigurationDAO,
      TelemetrySender telemetrySender,
      final ApplicationHelper applicationHelper,
      final OrganizationDAO organizationDAO,
      final ProductLicense productLicense,
      TelemetryUtils telemetryUtils)
  {
    this.applicationDAO = applicationDAO;
    this.policyEvaluationDAO = policyEvaluationDAO;
    this.automaticApplicationsConfigurationDAO = automaticApplicationsConfigurationDAO;
    this.telemetrySender = telemetrySender;
    this.applicationHelper = applicationHelper;
    this.organizationDAO = organizationDAO;
    this.productLicense = productLicense;
    this.telemetryUtils = telemetryUtils;
  }

  public ApplicationSummaryList getApplications(Goal goal, String organizationId, Set<String> applicationPublicIds) {
    if (!productLicense.hasFeature(LicensedFeature.ENFORCEMENT) && Goal.EVALUATE_COMPONENT.equals(goal)) {
      log.debug("License does not support IDE plugins.");
      throw new InvalidLicenseException();
    }
    return toApplicationSummaryList(getApplicationsForGoal(goal, organizationId, applicationPublicIds));
  }

  private List<Application> getApplicationsForGoal(Goal goal, String organizationId, Set<String> applicationPublicIds) {
    if (goal == null) {
      // For back compatibility only
      return getApplicationsForRead(organizationId, applicationPublicIds);
    }
    switch (goal) {
      case EVALUATE_APPLICATION:
        return getApplicationsForEvaluateApplication(organizationId, applicationPublicIds);
      case EVALUATE_COMPONENT:
      case VIEW_CIP:
        return getApplicationsForEvaluateComponent(organizationId, applicationPublicIds);
      default:
        return getApplicationsForRead(organizationId, applicationPublicIds);
    }
  }

  /**
   * Returns applications the caller has {@link Permission#READ} on. When {@code applicationPublicIds}
   * is non-empty, only those apps are fetched before {@code @AuthzFilter} runs (Martha facet search).
   */
  @AuthzFilter(permission = Permission.READ, context = AuthzFilter.Context.APPLICATION)
  public List<Application> getApplicationsForRead(String organizationId, Set<String> applicationPublicIds) {
    return getApplications(organizationId, applicationPublicIds);
  }

  /**
   * @since 1.14.0
   */
  @AuthzFilter(permission = Permission.EVALUATE_APPLICATION, context = AuthzFilter.Context.APPLICATION)
  protected List<Application> getApplicationsForEvaluateApplication(
      String organizationId,
      Set<String> applicationPublicIds)
  {
    return getApplications(organizationId, applicationPublicIds);
  }

  /**
   * Returns applications the caller has EVALUATE_COMPONENT permission on.
   * <p>
   * <strong>License bypass warning:</strong> This method does NOT check the ENFORCEMENT license
   * feature that {@link #getApplications(Goal, String, Set)} enforces for EVALUATE_COMPONENT.
   * Callers MUST gate this method with their own license check (e.g.,
   * {@code @ProductLicenseEnforcementPoint}) or restrict usage to license-independent contexts.
   * <p>
   * <strong>Approved callers:</strong> ApiPolicyContextOwnersResource (gated via GUIDE_SEARCH license).
   *
   * @since 1.14.0
   */
  @AuthzFilter(permission = Permission.EVALUATE_COMPONENT, context = AuthzFilter.Context.APPLICATION)
  public List<Application> getApplicationsForEvaluateComponent(
      String organizationId,
      Set<String> applicationPublicIds)
  {
    return getApplications(organizationId, applicationPublicIds);
  }

  private List<Application> getApplications(String organizationId, Set<String> applicationPublicIds) {
    if (StringUtils.isNotEmpty(organizationId)) {
      return applicationDAO.getByOrganizationId(organizationId);
    }
    if (applicationPublicIds != null && !applicationPublicIds.isEmpty()) {
      return applicationDAO.getByPublicIds(applicationPublicIds);
    }
    else {
      return applicationDAO.getAllWithoutRelatedRepositories();
    }
  }

  private ApplicationSummaryList toApplicationSummaryList(List<Application> apps) {
    // The input list may be immutable
    apps = new ArrayList<>(apps);
    apps.sort(APP_COMPARATOR);
    return ApplicationSummaryAdapter.convert(apps);
  }

  /**
   * Verifies if the user can access the application identified by applicationPublicId for the specified goal.
   * If an application with the specified applicationPublicId already exists, then the method checks access for the
   * current user and the specified goal to that application.
   * If such an application does not exist and automatic application creation is enabled, then the method creates the
   * new application and returns true to indicate the application will now be available.
   * If the optional organizationId is provided, all checks and application creation are done under the organization
   * referenced by the given ID; otherwise the default organization for automatic application creation is used.
   *
   * This method does not return the reason when the verification fails. This is by design.
   * If the method returned the verification failure reason, then an attacker could use that info to
   * find more about the system.
   * This is similar to login failure messages:
   * The system is supposed to return a generic message for all causes the login fails.
   * If the system tells back if the username or the password is incorrect,
   * that can be use by an attacker to further its attack.
   *
   * @since 1.45
   */
  boolean verifyOrCreateApplication(
      String applicationPublicId,
      String organizationId,
      Goal goal,
      String clientUserAgent)
  {
    if (goal == null) {
      throw new BadRequestException("A goal must be specified");
    }

    Application application = applicationDAO.getByPublicId(applicationPublicId);

    if (application != null) {
      if (StringUtils.isNotBlank(organizationId) && !application.getOrganizationId().equals(organizationId)) {
        log.debug("Application '{}' exists, but it doesn't belong to organization '{}'.", applicationPublicId,
            organizationId);
        return false;
      }
    }

    // If the application does not exist and automatic application creation is enabled, then create a new
    // application with the given public ID.
    if (automaticApplicationsConfigurationDAO.isEnabled()) {
      String targetOrganizationId = StringUtils.isNotBlank(
          organizationId) ? organizationId : automaticApplicationsConfigurationDAO.getOrganizationId();
      Organization targetOrganization;
      if (application == null) {
        try {
          targetOrganization = organizationDAO.getByIdNotNull(targetOrganizationId);
          checkEvaluateApplicationPermissionForOrganization(targetOrganizationId);
        }
        catch (NotFoundException e) { // the provided organizationId is invalid
          log.debug(e.getMessage());
          return false;
        }
        catch (UnauthorizedException e) {
          log.debug("Insufficient permissions to automatically create an application.");
          return false;
        }
        log.info("Automatic application creation is enabled. Creating an application with name and public id: {}.",
            applicationPublicId);
        application = new Application(applicationPublicId, applicationPublicId, targetOrganizationId);
        applicationHelper.validateNewApplication(application);
        applicationDAO.insert(application);
        applicationHelper.postAddApplicationEvent();
        auditCreateApplication(application, targetOrganization);
        sendApplicationCreatedTelemetryData(application, true, clientUserAgent);
      }
      else {
        if (policyEvaluationDAO.getCountByApplicationId(application.getId()) == 0) {
          sendApplicationCreatedTelemetryData(application, false, clientUserAgent);
        }
      }
    }

    // Verify the application public ID after (possibly) automatically creating the application in the case
    // the user does not actually have the permissions to access it
    if (application != null) {
      try {
        switch (goal) {
          case EVALUATE_APPLICATION:
            checkEvaluateApplicationPermission(application);
            break;
          case EVALUATE_COMPONENT:
          case VIEW_CIP:
            checkEvaluateComponentPermission(application);
            break;
          default:
            checkReadPermission(application);
        }
        return true;
      }
      catch (UnauthorizedException e) {
        return false;
      }
    }

    return false;
  }

  private void auditCreateApplication(final Application application, final Organization parentOrganization) {
    try (AuditSession auditSession = AuditData.get().recordSubEvent(AuditEvent.AUTO_CREATE_APPLICATION, true)) {
      AuditData.get().setApplicationWithDetails(application).setParentOrganization(parentOrganization);
    }
  }

  @Authorize(permission = Permission.EVALUATE_APPLICATION)
  void checkEvaluateApplicationPermissionForOrganization(
      @SuppressWarnings("unused") @AuthzContext(AuthzContext.Key.ORGANIZATION_ID) String organizationId)
  {
    // actual work done by AOP interceptor
  }

  @Authorize(permission = Permission.READ)
  void checkReadPermission(
      @SuppressWarnings("unused") @AuthzContext(AuthzContext.Key.APPLICATION) Application application)
  {
    // actual work done by AOP interceptor
  }

  @Authorize(permission = Permission.EVALUATE_APPLICATION)
  void checkEvaluateApplicationPermission(
      @SuppressWarnings("unused") @AuthzContext(AuthzContext.Key.APPLICATION) Application application)
  {
    // actual work done by AOP interceptor
  }

  @Authorize(permission = Permission.EVALUATE_COMPONENT)
  void checkEvaluateComponentPermission(
      @SuppressWarnings("unused") @AuthzContext(AuthzContext.Key.APPLICATION) Application application)
  {
    // actual work done by AOP interceptor
  }

  private void sendApplicationCreatedTelemetryData(
      Application application,
      boolean appCreatedAutomatically,
      String clientUserAgent)
  {
    // TODO CLM-29876 maybe this should use the OwnerMaintenanceTelemetryCreator class with some refactoring
    TelemetryData telemetryData = new TelemetryData(TelemetryPurpose.AUTOMATIC_APPLICATION_CREATION);
    telemetryData.getAttributes()
        .put(APP_CREATED_AUTOMATICALLY_TELEMETRY_ATTR,
            String.valueOf(appCreatedAutomatically));

    String applicationId = telemetryUtils.obfuscateIfAdvancedReportingDisabled(application.getId());
    String applicationName = telemetryUtils.obfuscateIfAdvancedReportingDisabled(application.getName());
    String parentOwnerId = telemetryUtils.obfuscateIfAdvancedReportingDisabled(application.getParentOwnerId());
    String ownerType = application.getType().toString();

    final OwnerMaintenanceTelemetry ownerMaintenanceTelemetry =
        new OwnerMaintenanceTelemetry(
            applicationId,
            applicationName,
            parentOwnerId,
            ownerType,
            OwnerMaintenanceTelemetry.TYPE_ADD);
    telemetryData.getAttributes().put(OwnerMaintenanceTelemetry.OWNER_MAINTENANCE_TELEMETRY, ownerMaintenanceTelemetry);

    telemetrySender.send(telemetryData, clientUserAgent);
  }
}
