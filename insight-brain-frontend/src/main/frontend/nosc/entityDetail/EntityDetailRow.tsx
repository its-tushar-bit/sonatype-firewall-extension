/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import type { ReactElement, ReactNode } from 'react';
import { Box, Flex, Text } from '@radix-ui/themes';

/**
 * Labelled key/value row used by entity detail Overview and Security Details cards.
 */
export function EntityDetailRow({
  label,
  children,
  testId,
  labelWidth = 160,
}: {
  readonly label: string;
  readonly children: ReactNode;
  readonly testId: string;
  /** Label column width in px. Security Details uses a wider column for long labels. */
  readonly labelWidth?: number;
}): ReactElement {
  return (
    <Flex
      align="start"
      gap="4"
      py="3"
      data-testid={testId}
      style={{ borderTop: '1px solid var(--gray-4)' }}
    >
      <Box style={{ flex: `0 0 ${labelWidth}px` }}>
        <Text size="2" weight="medium">
          {label}
        </Text>
      </Box>
      <Box style={{ flex: '1 1 auto', minWidth: 0 }}>{children}</Box>
    </Flex>
  );
}
