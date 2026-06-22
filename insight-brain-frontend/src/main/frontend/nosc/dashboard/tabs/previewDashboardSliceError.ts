/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

/** Normalize Classic dashboard slice `error` (string or Error object) for display. */
export function previewDashboardSliceError(
  error: unknown,
  fallbackMessage: string,
): string | null {
  if (error == null) return null;
  if (typeof error === 'string') return error;
  if (
    typeof error === 'object' &&
    'message' in error &&
    typeof (error as { message: unknown }).message === 'string'
  ) {
    return (error as { message: string }).message;
  }
  return fallbackMessage;
}
