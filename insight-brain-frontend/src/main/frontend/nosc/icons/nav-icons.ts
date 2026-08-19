/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import {
  ChevronRight,
  ChevronLeft,
  ChevronDown,
  ChevronUp,
  ArrowLeft,
} from 'lucide-react';

/**
 * Semantic navigation icon mappings.
 *
 * @example
 * import { NavIcons } from '@nosc/icons/nav-icons';
 * <NavIcons.Expand size={16} />
 */
export const NavIcons = {
  Forward: ChevronRight,
  Back: ChevronLeft,
  Expand: ChevronDown,
  Collapse: ChevronUp,
  Return: ArrowLeft,
} as const;

export type NavIconName = keyof typeof NavIcons;
