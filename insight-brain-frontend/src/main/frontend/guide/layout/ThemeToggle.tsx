/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { IconButton, Tooltip } from '@radix-ui/themes';
import { Sun, MoonStar } from 'lucide-react';
import { tokens } from '@guide/ui-core/utils';
import { useTheme } from './ThemeProvider';

export function ThemeToggle() {
  const { resolvedTheme, setTheme } = useTheme();

  const isDark = resolvedTheme === 'dark';
  const label = isDark ? 'Switch to light mode' : 'Switch to dark mode';

  return (
    <Tooltip content={label} side="bottom">
      <IconButton
        variant="outline"
        size={tokens.sizes.caption}
        color="gray"
        aria-label={label}
        onClick={() => setTheme(isDark ? 'light' : 'dark')}
      >
        {isDark ? <MoonStar size={16} /> : <Sun size={16} />}
      </IconButton>
    </Tooltip>
  );
}
