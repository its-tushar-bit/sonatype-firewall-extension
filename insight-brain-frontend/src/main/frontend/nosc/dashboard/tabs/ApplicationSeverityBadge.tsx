/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { Badge } from '@radix-ui/themes';
import {
  ApplicationSeverity,
  applicationSeverityBadgeColor,
} from 'MainRoot/nosc/dashboard/tabs/previewDashboardSeverity';

const SEVERITY_ACCESSIBLE_LABEL: Record<ApplicationSeverity, string> = {
  total: 'total risk',
  critical: 'critical',
  severe: 'severe',
  moderate: 'moderate',
  low: 'low',
};

export function ApplicationSeverityBadge({
  value,
  severity,
  size = '1',
}: {
  readonly value: number;
  readonly severity: ApplicationSeverity;
  readonly size?: '1' | '2';
}): JSX.Element {
  return (
    <Badge
      color={applicationSeverityBadgeColor(value, severity)}
      variant="soft"
      radius="full"
      size={size}
      aria-label={`${value} ${SEVERITY_ACCESSIBLE_LABEL[severity]} violations`}
    >
      {value}
    </Badge>
  );
}
