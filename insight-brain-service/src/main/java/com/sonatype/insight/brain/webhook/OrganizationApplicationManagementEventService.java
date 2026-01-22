/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.webhook;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.eventbus.AsyncEventBus;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.security.CurrentUser;
import com.sonatype.insight.brain.webhook.dto.ApplicationSummary;
import com.sonatype.insight.brain.webhook.dto.OrganizationSummary;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
public class OrganizationApplicationManagementEventService
{
  private static final Logger log = LoggerFactory.getLogger(OrganizationApplicationManagementEventService.class);

  private final AsyncEventBus asyncEventBus;

  private final ApplicationDAO applicationDAO;

  private final OrganizationDAO organizationDAO;

  private final CurrentUser currentUser;

  @Inject
  public OrganizationApplicationManagementEventService(
      final AsyncEventBus asyncEventBus,
      final OrganizationDAO organizationDAO,
      final ApplicationDAO applicationDAO,
      final CurrentUser currentUser)
  {
    this.asyncEventBus = asyncEventBus;
    this.organizationDAO = organizationDAO;
    this.applicationDAO = applicationDAO;
    this.currentUser = currentUser;
  }

  public void postEvent() {
    try {
      final List<OrganizationSummary> organizationSummaries = createOrganizationSummaries();
      final List<ApplicationSummary> applicationSummaries = createApplicationSummaries();
      final OrganizationApplicationManagementEvent orgAppManagementEvent =
          new OrganizationApplicationManagementEvent(organizationSummaries, applicationSummaries);
      orgAppManagementEvent.initiator = currentUser.getUsernameOrSystem();

      asyncEventBus.post(orgAppManagementEvent);
    }
    catch (final RuntimeException e) {
      log.error("Webhook not posted due to exception.", e);
    }
  }

  private List<OrganizationSummary> createOrganizationSummaries() {
    return organizationDAO.getAll()
        // We only want to send customer organizations, so remove the root org built-in if it exists
        .stream().filter(org -> !org.getId().equals(Organization.ROOT_ORGANIZATION_ID))
        .sorted(Comparator.comparing(orgSummary -> orgSummary.getName().toLowerCase(Locale.ROOT)))
        .map(OrganizationSummary::new)
        .collect(Collectors.toList());
  }

  private List<ApplicationSummary> createApplicationSummaries() {
    return applicationDAO.getAll()
        .stream()
        .sorted(Comparator.comparing(application -> application.getName().toLowerCase(Locale.ROOT)))
        .map(ApplicationSummary::new)
        .collect(Collectors.toList());
  }
}
