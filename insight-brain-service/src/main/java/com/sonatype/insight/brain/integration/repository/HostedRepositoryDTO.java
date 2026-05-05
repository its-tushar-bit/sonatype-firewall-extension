/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.integration.repository;

import com.sonatype.clm.dto.model.repository.RepositoryType;

public class HostedRepositoryDTO
{
  public String id;

  public String publicId;

  public String name;

  public String format;

  public RepositoryType type;

  public boolean auditEnabled;

  public boolean quarantineEnabled;

  public boolean policyCompliantComponentSelectionEnabled;

  public boolean namespaceConfusionProtectionEnabled;

  public Long lastScannedTime;

  public boolean hasQueuedScans;
}
