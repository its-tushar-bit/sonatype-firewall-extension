/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.sourcecontrol;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;

import org.apache.commons.lang3.StringUtils;

public class SourceControlDAO
    extends AbstractOperationalSqlDAO<SourceControl>
{
  private final ApplicationDAO applicationDAO = new ApplicationDAO();

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
   * Support cascade delete from Application.
   */
  public void deleteByApplicationId(final TransactionContext tx, final String applicationId) {
    SourceControl existing = getByApplicationId(tx, applicationId);
    if (existing != null) {
      delete(tx, existing);
    }
  }

  public SourceControl getByApplicationId(final String applicationId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByApplicationId(tx, applicationId);
    }
  }

  private SourceControl getByApplicationId(final TransactionContext tx, final String applicationId) {
    return get(tx, "SELECT entity FROM SourceControl entity WHERE entity.applicationId=?1", applicationId);
  }

  private void validate(final TransactionContext tx, final SourceControl sourceControl) {
    if (StringUtils.isBlank(sourceControl.getApplicationId())) {
      throw new BadRequestException("SourceControl application id is required");
    }
    if (applicationDAO.getById(tx, sourceControl.getApplicationId()) == null) {
      throw new BadRequestException(
          "SourceControl applicationId '" + sourceControl.getApplicationId() + "' cannot be found");
    }
    SourceControl existing = getByApplicationId(tx, sourceControl.getApplicationId());
    if (existing != null && !existing.getId().equals(sourceControl.getId())) {
      throw new BadRequestException(
          "SourceControl already configured for application with id: '" + sourceControl.getApplicationId() + "'");
    }
    if (StringUtils.isBlank(sourceControl.getToken())) {
      throw new BadRequestException("SourceControl authentication token is required");
    }
    if (sourceControl.getRepositoryUrl() == null || !sourceControl.getRepositoryUrl().startsWith("https://")) {
      throw new BadRequestException("SourceControl URL must start with https://");
    }

    try {
      new URI(sourceControl.getRepositoryUrl());
    }
    catch (URISyntaxException e) {
      throw new BadRequestException("SourceControl URL is invalid: " + e.getMessage(), e);
    }
  }
}
