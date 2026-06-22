/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { Badge } from '@radix-ui/themes';
import { Check, X } from 'lucide-react';
import { tokens } from '@guide/ui-core/utils';
import type { GuidePolicyComplianceLevel } from './policyComplianceTypes';

export interface PolicyBadgeV2Props {
  complianceLevel: GuidePolicyComplianceLevel;
  /** Render only the status icon, omitting the text label. */
  onlyIcon?: boolean;
}

type BadgeColor = 'green' | 'amber' | 'red';

const LEVEL_CONFIG: Record<
  GuidePolicyComplianceLevel,
  { color: BadgeColor; label: string; compliant: boolean }
> = {
  // compliant === (complianceLevel !== 'FAIL'); WARN is the new amber "still compliant" state.
  PASS: { color: 'green', label: 'Compliant', compliant: true },
  WARN: { color: 'amber', label: 'Compliant', compliant: true },
  FAIL: { color: 'red', label: 'Non-Compliant', compliant: false },
};

/**
 * Level-aware compliance badge for self-hosted Guide. Unlike `@guide/ui-core`'s boolean-only
 * `PolicyBadge`, this renders the tri-state PASS (green) / WARN (amber) / FAIL (red).
 */
export function PolicyBadgeV2({ complianceLevel, onlyIcon = false }: PolicyBadgeV2Props) {
  // Fall back to the FAIL config for any value outside the union — a null/unknown level (e.g. a
  // newer backend level during a rolling deploy) degrades to red "Non-Compliant" rather than
  // crashing the card on a destructure of undefined.
  const { color, label, compliant } = LEVEL_CONFIG[complianceLevel] ?? LEVEL_CONFIG.FAIL;
  return (
    <Badge color={color} radius="full" variant="soft" size={tokens.badge.medium}>
      {compliant ? <Check size={tokens.icon.theme} /> : <X size={tokens.icon.theme} />}
      {!onlyIcon && label}
    </Badge>
  );
}
