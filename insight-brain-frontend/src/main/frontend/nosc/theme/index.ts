/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import type { ThemeProps } from '@radix-ui/themes';

export {
  colors,
  spacing,
  radii,
  fontSizes,
  type ColorToken,
  type SpacingToken,
  type RadiusToken,
  type FontSizeToken,
} from './tokens';

/**
 * Radix accent color used across the Nexus One SPA.
 * Single-line edit point — change here and every Radix primitive
 * + every `var(--accent-N)` token re-resolves.
 *
 * Set to 'tomato' per the canonical Sonatype UX standards
 * (see apps/ux-standards/claude.md and ux-standards/system/src/tokens/
 * nexus-one-tokens.css). The tomato scale is overridden by Sonatype's
 * brand orange (`#fe572a` family) when nexus-one-tokens.css is loaded;
 * if the CSS isn't loaded the Radix default tomato shows up — still a
 * sensible Sonatype-orange tone.
 *
 * Brand blue (`#2d36ec`) overrides the Radix `--blue-*` scale via the
 * same tokens CSS, so any component using `<Button color="blue">` gets
 * the real Sonatype blue.
 */
export type AccentColor = NonNullable<ThemeProps['accentColor']>;
export const BRAND_ACCENT: AccentColor = 'tomato';
