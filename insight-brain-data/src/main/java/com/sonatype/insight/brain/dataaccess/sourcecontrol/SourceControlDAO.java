/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.sourcecontrol;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.nexus.scm.GitApiClientFactory;
import com.sonatype.nexus.scm.SourceControlProvider;

import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.StringUtils;

public class SourceControlDAO
    extends AbstractOperationalSqlDAO<SourceControl>
{
  private final ApplicationDAO applicationDAO = new ApplicationDAO();

  private final OrganizationDAO organizationDAO = new OrganizationDAO();

  private final GitApiClientFactory gitApiClientFactory = new GitApiClientFactory();

  @Override
  public SourceControl getById(final TransactionContext tx, final String id) {
    return get(tx, "SELECT entity FROM SourceControl entity WHERE entity.id=?1", id);
  }

  public SourceControl getByIdNotNull(final String id) {
    SourceControl sourceControl = getById(id);
    if (sourceControl == null) {
      throw new NotFoundException("Could not find a SourceControl with ID " + id + ".");
    }
    return sourceControl;
  }

  public List<SourceControl> getByApplication() {
    String query = "SELECT entity FROM SourceControl entity WHERE entity.repositoryUrl IS NOT NULL";

    return getList(query);
  }

  private List<SourceControl> getByOrganization() {
    String query = "SELECT entity FROM SourceControl entity WHERE entity.repositoryUrl IS NULL";

    return getList(query);
  }

  public List<SourceControl> getApplicationsWithPullReqsEnabled() {
    // an application is enabled if it has a valid repository_url and enable_pull_requests is set at the
    // application, parent organization, or root organization level

    SourceControl scRootOrg = getByOwnerId(Organization.ROOT_ORGANIZATION_ID);

    Map<String, Application> applicationsById = applicationDAO.getAll()
        .stream()
        .collect(Collectors.toMap(Application::getId, Function.identity()));

    Map<String, SourceControl> orgSourceControlsByOrgId = getByOrganization()
        .stream()
        .collect(Collectors.toMap(SourceControl::getOwnerId, Function.identity()));

    return getByApplication()
        .stream()
        .filter(application -> isPrEnabled(application, applicationsById, orgSourceControlsByOrgId, scRootOrg))
        .collect(ImmutableList.toImmutableList());
  }

  private boolean isPrEnabled(final SourceControl application,
                              final Map<String, Application> applicationsById,
                              final Map<String, SourceControl> orgSourceControlsByOrgId,
                              final SourceControl scRootOrg)
  {
    if (application.getEnablePullRequests() != null) {
      return application.getEnablePullRequests();
    }

    // application did not define a value, so check organization
    String orgId = applicationsById.get(application.getOwnerId()).getOrganizationId();
    if (orgSourceControlsByOrgId.containsKey(orgId)) {
      SourceControl orgSourcControl = orgSourceControlsByOrgId.get(orgId);
      if (orgSourcControl.getEnablePullRequests() != null) {
        return orgSourcControl.getEnablePullRequests();
      }
    }

    // organization did not define a value, check root org
    if (scRootOrg != null && scRootOrg.getEnablePullRequests() != null) {
      return scRootOrg.getEnablePullRequests();
    }

    // could not find a defined value
    return false;
  }

  public List<SourceControl> getAll() {
    return getList("SELECT entity FROM SourceControl entity");
  }

  @Override
  public void insert(final TransactionContext tx, final SourceControl sourceControl) {
    validate(tx, sourceControl);
    super.insert(tx, sourceControl);
  }

  @Override
  public void update(final TransactionContext tx, final SourceControl sourceControl) {
    validate(tx, sourceControl);
    super.update(tx, sourceControl);
  }

  /**
   * Support cascade delete from Application or Organization.
   */
  public void deleteByOwnerId(final TransactionContext tx, final String ownerId) {
    SourceControl existing = getByOwnerId(tx, ownerId);
    if (existing != null) {
      delete(tx, existing);
    }
  }

  public SourceControl getByOwnerId(final String ownerId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByOwnerId(tx, ownerId);
    }
  }

  private SourceControl getByOwnerId(final TransactionContext tx, final String ownerId) {
    return get(tx, "SELECT entity FROM SourceControl entity WHERE entity.ownerId=?1", ownerId);
  }

  private void validate(final TransactionContext tx, final SourceControl sourceControl) {
    if (StringUtils.isBlank(sourceControl.getOwnerId())) {
      throw new BadRequestException("SourceControl owner id is required");
    }

    if (isForOrganization(tx, sourceControl)) {
      if (sourceControl.getRepositoryUrl() != null) {
        throw new BadRequestException("SourceControl repositoryUrl is not allowed for organization");
      }
      if (StringUtils.isBlank(sourceControl.getToken())) {
        throw new BadRequestException("SourceControl authentication token is required for organization");
      }
      // if the token is provided the provider must be specified as well
      if (StringUtils.isNotEmpty(sourceControl.getToken()) && sourceControl.getProvider() == null) {
        throw new BadRequestException("SourceControl provider is required when a token is provided");
      }
    }
    else if (isForApplication(tx, sourceControl)) {
      // if the token is provided the provider must be specified as well
      if (StringUtils.isNotEmpty(sourceControl.getToken()) && sourceControl.getProvider() == null) {
        throw new BadRequestException("SourceControl provider is required when a token is provided");
      }
      validateRepositoryUrl(tx, sourceControl);
    }
    else {
      throw new BadRequestException(
          "SourceControl ownerId '" + sourceControl.getOwnerId() + "' cannot be found");
    }
  }

  private void validateRepositoryUrl(final TransactionContext tx, final SourceControl sourceControl) {
    if (StringUtils.isBlank(sourceControl.getRepositoryUrl())) {
      throw new BadRequestException("SourceControl repositoryUrl is required for application");
    }
    try {
      SourceControlProvider scmProvider;
      if (sourceControl.getProvider() != null) {
        scmProvider = sourceControl.getProvider();
      }
      else {
        Application application = applicationDAO.getById(tx, sourceControl.getOwnerId());
        scmProvider = getProviderFromOrganization(tx, application.getOrganizationId());
        if (scmProvider == null) {
          throw new BadRequestException("Cannot validate SourceControl repositoryUrl. " +
              "The root organization source control provider is not set. " +
              "Please configure the root organization source control provider");
        }
      }
      gitApiClientFactory.getGitApiClientUtils(scmProvider).createProjectUri(sourceControl.getRepositoryUrl());
    }
    catch (IllegalArgumentException e) {
      throw new BadRequestException("SourceControl repositoryUrl is invalid: " + e.getMessage(), e);
    }
  }

  private boolean isForOrganization(final TransactionContext tx, final SourceControl sourceControl) {
    return organizationDAO.getById(tx, sourceControl.getOwnerId()) != null;
  }

  private boolean isForApplication(final TransactionContext tx, final SourceControl sourceControl) {
    return applicationDAO.getById(tx, sourceControl.getOwnerId()) != null;
  }

  private SourceControlProvider getProviderFromOrganization(final TransactionContext tx,
                                                            final String organizationId)
  {
    SourceControl orgSourceControl = getByOwnerId(tx, organizationId);
    if (orgSourceControl != null && orgSourceControl.getProvider() != null) {
      return orgSourceControl.getProvider();
    }

    Organization organization = organizationDAO.getById(organizationId);
    if (StringUtils.isEmpty(organization.getParentOrganizationId())) {
      return null;
    }

    return getProviderFromOrganization(tx, organization.getParentOrganizationId());
  }
}
