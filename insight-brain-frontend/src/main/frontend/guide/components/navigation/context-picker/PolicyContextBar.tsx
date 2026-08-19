/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { Box, Flex, Separator } from '@radix-ui/themes';
import { tokens } from '@guide/ui-core/utils';
import { PolicyContextPicker } from './PolicyContextPicker';

/**
 * Row that hosts the {@link PolicyContextPicker} at the top of a content page, above the page
 * header/filters. The picker is left-aligned with the page content, and a full-width divider
 * separates this row from the content below. Component content pages render this as the first
 * child inside their PageLayout.
 */
export function PolicyContextBar() {
  return (
    <Box mb={tokens.space.header}>
      <Flex align="center" mb={tokens.space.header}>
        <PolicyContextPicker />
      </Flex>
      {/* size="4" makes the separator span 100% of the page content width. */}
      <Separator size="4" />
    </Box>
  );
}
