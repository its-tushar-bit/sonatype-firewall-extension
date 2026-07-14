/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { Badge } from '@radix-ui/themes';
import { threatColorFor } from 'MainRoot/nosc/applications/applicationDetailUtils';

/**
 * Numeric policy-threat pill (0–10) colored via the canonical {@link threatColorFor} palette, so a
 * threat level renders the same hue on violation cards as on the application-detail and waivers
 * views. An absent threat renders a neutral em-dash.
 */
export function ViolationThreatBadge({
  threat,
  size = '1',
}: {
  readonly threat?: number;
  readonly size?: '1' | '2';
}): JSX.Element {
  const hasThreat = typeof threat === 'number';
  const minWidth = size === '1' ? 22 : 28;
  return (
    <Badge
      color={hasThreat ? threatColorFor(threat as number) : 'gray'}
      variant="solid"
      radius="full"
      size={size}
      aria-label={hasThreat ? `Threat level ${threat}` : 'Threat level unknown'}
      data-testid="violation-threat-badge"
      style={{
        minWidth,
        justifyContent: 'center',
        fontVariantNumeric: 'tabular-nums',
        flexShrink: 0,
      }}
    >
      {hasThreat ? threat : '—'}
    </Badge>
  );
}
