/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import {
  Loader2,
  AlertCircle,
  AlertTriangle,
  TriangleAlert,
  CheckCircle2,
  Info,
} from 'lucide-react';

/**
 * Semantic status icon mappings for feedback states.
 *
 * @example
 * import { StatusIcons } from '@nosc/icons/status-icons';
 * <StatusIcons.Loading size={16} className="animate-spin" />
 */
export const StatusIcons = {
  Loading: Loader2,
  Error: AlertCircle,
  Warning: AlertTriangle,
  /** Filled triangle-alert — inline parser-warning pill. */
  WarningTriangle: TriangleAlert,
  Success: CheckCircle2,
  Info,
} as const;

export type StatusIconName = keyof typeof StatusIcons;
