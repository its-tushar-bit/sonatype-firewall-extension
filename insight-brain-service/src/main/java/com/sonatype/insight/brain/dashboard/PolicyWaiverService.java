/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard;

public interface PolicyWaiverService
{
  DashboardResultsDTO<DashboardPolicyWaiverDTO> getDashboardPolicyWaivers(final RisksFilterDTO risksFilterDTO);

  DashboardResultsDTO<DashboardPolicyWaiverDTO> getDashboardPolicyWaivers(
      final RisksFilterDTO risksFilterDTO,
      boolean includeAutoWaivers);

  DashboardResultsDTO<DashboardPolicyWaiverDTO> getDashboardPolicyWaiversForExport(final RisksFilterDTO risksFilterDTO);

  DashboardResultsDTO<DashboardPolicyWaiverDTO> getDashboardPolicyWaiversForExport(
      RisksFilterDTO risksFilterDTO,
      boolean includeAutoWaivers);
}
