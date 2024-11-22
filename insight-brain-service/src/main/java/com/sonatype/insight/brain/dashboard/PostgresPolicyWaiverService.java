/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard;

import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
public class PostgresPolicyWaiverService
    implements PolicyWaiverService
{
  private static final Logger log = LoggerFactory.getLogger(PostgresPolicyWaiverService.class);

  @Inject
  public PostgresPolicyWaiverService() {
    // TODO - inject dependencies
    log.info("todo");
  }

  @Override
  public DashboardResultsDTO<DashboardPolicyWaiverDTO> getDashboardPolicyWaivers(final RisksFilterDTO risksFilterDTO) {
    // TODO - CLM-32521
    return null;
  }

  @Override
  public DashboardResultsDTO<DashboardPolicyWaiverDTO> getDashboardPolicyWaiversForExport(
      final RisksFilterDTO risksFilterDTO)
  {
    // TODO - CLM-32521
    return null;
  }

  @Override
  public DashboardResultsDTO<DashboardPolicyWaiverDTO> getDashboardPolicyWaiversForExport(
      final RisksFilterDTO risksFilterDTO,
      boolean includeAutoWaivers)
  {
    // TODO - CLM-32521
    return null;
  }

  @Override
  public DashboardResultsDTO<DashboardPolicyWaiverDTO> getDashboardPolicyWaivers(
      final RisksFilterDTO risksFilterDTO,
      boolean includeAutoWaivers)
  {
    // TODO - CLM-32521
    return null;
  }
}
