/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import {
  Trash2,
  Plus,
  ArrowLeft,
  ArrowLeftRight,
  Pencil,
  Check,
  ChevronLeft,
  X,
  Search,
  RefreshCw,
  Download,
  ExternalLink,
  Copy,
  Cog,
  Settings,
  Settings2,
  Sun,
  MoonStar,
  Monitor,
  Menu,
  HelpCircle,
  Bell,
  LayoutGrid,
  AlertCircle,
} from 'lucide-react';

/**
 * Semantic action icon mappings.
 *
 * Use these instead of importing Lucide icons directly to ensure
 * consistent icon usage across the application.
 *
 * @example
 * import { ActionIcons } from '@nosc/icons/action-icons';
 * <ActionIcons.Delete size={16} />
 */
export const ActionIcons = {
  Delete: Trash2,
  Add: Plus,
  Back: ArrowLeft,
  /** Two-way swap glyph; used by the Classic↔Preview UI toggle. */
  Swap: ArrowLeftRight,
  Edit: Pencil,
  Save: Check,
  Cancel: X,
  /**
   * Subtle chevron-left used in filter "Clear" links per
   * apps/nexusone-ux-prototype design language.
   */
  ChevronLeft,
  Search,
  Refresh: RefreshCw,
  Download,
  ExternalLink,
  Copy,
  /** Gear / cog — System Preferences in TopNav (matches Classic faCog). */
  Settings: Cog,
  /** Sun / MoonStar / Monitor — dark-mode toggle. */
  Sun,
  Moon: MoonStar,
  System: Monitor,
  /** Hamburger — far-left LeftNav collapse toggle in the TopNav. */
  Menu,
  /** Help (?) icon in the TopNav right cluster. */
  Help: HelpCircle,
  /** Notifications bell. */
  Bell,
  /** 9-dot grid — Solution Switcher dropdown trigger. */
  SolutionSwitcher: LayoutGrid,
  /** Alert circle — used in error Callouts. */
  AlertCircle,
} as const;

export type ActionIconName = keyof typeof ActionIcons;
