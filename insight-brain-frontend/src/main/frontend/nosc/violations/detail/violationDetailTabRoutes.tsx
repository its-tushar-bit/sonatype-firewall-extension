/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import type { ReactElement } from 'react';
import { OverviewTab } from 'MainRoot/nosc/violations/detail/OverviewTab';
import { VulnerabilityTab } from 'MainRoot/nosc/violations/detail/VulnerabilityTab';
import { WaiversTab } from 'MainRoot/nosc/violations/detail/WaiversTab';

export function ViolationDetailOverviewRoute(): ReactElement {
  return <OverviewTab />;
}

export function ViolationDetailVulnerabilityRoute(): ReactElement {
  return <VulnerabilityTab />;
}

export function ViolationDetailWaiversRoute(): ReactElement {
  return <WaiversTab />;
}
