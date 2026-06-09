/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import type { ThemeProps } from '@radix-ui/themes';

/**
 * Radix accent color used across the Nexus One SPA.
 * Single-line edit point — change here and every Radix primitive
 * + every `var(--accent-N)` token re-resolves.
 *
 * Set to 'tomato' per the canonical Sonatype UX standards. The lib
 * (`@sonatype/nexus-one-components`) ships brand-anchored `--blue-*`
 * and `--tomato-*` Radix scales so `<Button color="blue">` and any
 * component using the accent scale render in Sonatype brand colors.
 * Those CSS scales are activated by the lib's side-effect import in
 * the bundle entry points (see `nexus-one/App.tsx` for the Nexus One
 * SPA bundle and `nosc/shell/ClassicToggleButton.tsx` for the Classic
 * bundle).
 */
export type AccentColor = NonNullable<ThemeProps['accentColor']>;
export const BRAND_ACCENT: AccentColor = 'tomato';
