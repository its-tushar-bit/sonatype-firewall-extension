/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.sourcecontrol;

import java.util.List;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.nexus.scm.GitApiClientFactory;
import com.sonatype.nexus.scm.SourceControlProvider;

import org.apache.commons.lang3.StringUtils;

public class SourceControlDAO
    extends AbstractOperationalSqlDAO<SourceControl>
{
  private final ApplicationDAO applicationDAO = new ApplicationDAO();

  private final OrganizationDAO organizationDAO = new OrganizationDAO();

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
      if (sourceControl.getProvider() == null) {
        throw new BadRequestException("SourceControl provider is required for organization");
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
        scmProvider = SourceControlProvider.fromString(sourceControl.getProvider().toString());
      }
      else {
        scmProvider = getProviderFromOrganization(tx, sourceControl);
        if (scmProvider == null) {
          throw new BadRequestException("Cannot validate SourceControl repositoryUrl due to undetermined provider");
        }
      }
      GitApiClientFactory.getGitApiClientUtils(scmProvider).createProjectUri(sourceControl.getRepositoryUrl());
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
                                                            final SourceControl sourceControl)
  {
    Application application = applicationDAO.getById(tx, sourceControl.getOwnerId());
    SourceControl orgSourceControl  = getByOwnerId(tx, application.getOrganizationId());
    if (orgSourceControl == null || orgSourceControl.getProvider() == null) {
      return null;
    }
    return SourceControlProvider.fromString(orgSourceControl.getProvider().toString());
  }
}
