/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.webhook.dto;

import com.sonatype.clm.dto.model.repository.RepositoryType;
import com.sonatype.insight.brain.model.repository.Repository;

public class RepositorySummary
{
  public String id;

  public String publicId;

  public String repositoryManagerId;

  public String format;

  public RepositoryType repositoryType;

  public boolean auditEnabled;

  public boolean quarantineEnabled;

  public RepositorySummary() {
  }

  public RepositorySummary(final Repository repository) {
    if (repository == null) {
      throw new IllegalArgumentException("Repository cannot be null");
    }
    this.id = repository.getId();
    this.publicId = repository.getPublicId();
    this.repositoryManagerId = repository.getRepositoryManagerId();
    this.format = repository.getFormat();
    this.repositoryType = repository.getRepositoryType();
    this.auditEnabled = repository.isAuditEnabled();
    this.quarantineEnabled = repository.isQuarantineEnabled();
  }
}
