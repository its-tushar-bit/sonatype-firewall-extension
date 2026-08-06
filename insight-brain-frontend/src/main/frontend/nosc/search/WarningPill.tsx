/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { Box, Flex, Text } from '@radix-ui/themes';
import { StatusIcons } from 'MainRoot/nosc/icons';

/**
 * Inline pill surfacing non-fatal query-parser warnings from the search API
 * (e.g. "Unknown filter … ignored", a malformed range, an unclosed quote). The
 * backend treats these as warnings, not errors — the rest of the query still
 * runs — but we surface them so the user can tell their syntax was partially
 * ignored. The hook exposes them as `warnings[]` (also on the X-Search-Warnings
 * header). Rendered above the results / tabs.
 *
 * `role="status"` + `aria-live="polite"` announces the warning to assistive tech
 * without stealing focus. Colors use the Radix orange scale tokens
 * (var(--orange-*)) so they track light/dark appearance; no hardcoded colors.
 */
export function WarningPill({ warnings }: { warnings: readonly string[] }): JSX.Element | null {
  if (warnings.length === 0) return null;
  const label =
    warnings.length === 1 ? warnings[0] : `${warnings.length} warnings: ${warnings.join(' • ')}`;
  return (
    <Box px="3" py="2" data-testid="nosc-search-warning-pill">
      <Flex
        align="center"
        gap="2"
        px="2"
        py="1"
        role="status"
        aria-live="polite"
        style={{
          background: 'var(--orange-3)',
          color: 'var(--orange-12)',
          borderRadius: 'var(--radius-2)',
          border: '1px solid var(--orange-6)',
          width: 'fit-content',
          maxWidth: '100%',
        }}
      >
        <StatusIcons.WarningTriangle size={14} aria-hidden="true" />
        <Text size="1" weight="medium">
          {label}
        </Text>
      </Flex>
    </Box>
  );
}
