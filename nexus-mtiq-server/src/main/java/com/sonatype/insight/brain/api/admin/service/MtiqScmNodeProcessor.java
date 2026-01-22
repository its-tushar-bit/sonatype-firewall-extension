/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.admin.service;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.service.ScmNodeProcessor;
import com.sonatype.insight.brain.tenancy.TenantUtil;

/**
 * Only run SCM event and pull request polling processes in MTIQ batch sever mode
 */
@Singleton
public class MtiqScmNodeProcessor
    extends ScmNodeProcessor
{
  private final TenantUtil tenantUtil;

  @Inject
  public MtiqScmNodeProcessor(TenantUtil tenantUtil) {
    this.tenantUtil = tenantUtil;
  }

  @Override
  public boolean shouldRun() {
    return tenantUtil.isMtiqBatchMode();
  }
}
