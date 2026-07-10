/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { Callout } from '@radix-ui/themes';
import { StatusIcons } from 'MainRoot/nosc/icons';
import { LARGE_SCAN_WARNING_THRESHOLD } from './applicationDetailUtils';

interface LargeScanBannerProps {
  readonly itemCount: number;
  readonly itemLabel: string;
  readonly guidance: string;
  readonly testId: string;
}

/**
 * Warns when a tab's client-side filter/pagination may be slow on very large
 * single-app scans (Components + Violations tabs share this).
 */
export function LargeScanBanner({
  itemCount,
  itemLabel,
  guidance,
  testId,
}: LargeScanBannerProps): JSX.Element | null {
  if (itemCount < LARGE_SCAN_WARNING_THRESHOLD) return null;

  return (
    <Callout.Root color="amber" mb="3" data-testid={testId}>
      <Callout.Icon>
        <StatusIcons.Info size={16} />
      </Callout.Icon>
      <Callout.Text>
        This scan has {itemCount.toLocaleString()} {itemLabel}. {guidance}
      </Callout.Text>
    </Callout.Root>
  );
}
