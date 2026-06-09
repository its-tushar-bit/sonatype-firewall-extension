/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { Flex, Text } from '@radix-ui/themes';
import { DomainIcons } from 'MainRoot/nosc/icons';

type TrendDirection = 'up' | 'down' | 'flat';

interface TrendVariant {
  readonly testId: string;
  readonly Icon: typeof DomainIcons.TrendUp;
  readonly iconColor: string;
  readonly textColor: 'red' | 'green' | 'gray';
  readonly ariaLabel: string;
  readonly displayPct: (trendPct: number) => number;
}

const TREND_VARIANTS: Record<TrendDirection, TrendVariant> = {
  up: {
    testId: 'legal-tile-trend-up',
    Icon: DomainIcons.TrendUp,
    iconColor: 'var(--red-9)',
    textColor: 'red',
    ariaLabel: 'license violations trending up',
    displayPct: (trendPct) => trendPct,
  },
  down: {
    testId: 'legal-tile-trend-down',
    Icon: DomainIcons.TrendDown,
    iconColor: 'var(--green-9)',
    textColor: 'green',
    ariaLabel: 'license violations trending down',
    displayPct: (trendPct) => trendPct,
  },
  flat: {
    testId: 'legal-tile-trend-flat',
    Icon: DomainIcons.TrendFlat,
    iconColor: 'var(--gray-9)',
    textColor: 'gray',
    ariaLabel: 'license violations flat',
    displayPct: () => 0,
  },
};

function trendDirection(trendPct: number): TrendDirection {
  if (trendPct > 0) {
    return 'up';
  }
  if (trendPct < 0) {
    return 'down';
  }
  return 'flat';
}

export interface LegalObligationsTrendArrowProps {
  trendPct: number;
}

export function LegalObligationsTrendArrow({ trendPct }: LegalObligationsTrendArrowProps) {
  const direction = trendDirection(trendPct);
  const variant = TREND_VARIANTS[direction];
  const Icon = variant.Icon;

  return (
    <Flex align="center" gap="1" data-testid={variant.testId}>
      <Icon size={14} color={variant.iconColor} aria-label={variant.ariaLabel} />
      <Text size="1" color={variant.textColor}>
        {variant.displayPct(trendPct)}%
      </Text>
    </Flex>
  );
}
