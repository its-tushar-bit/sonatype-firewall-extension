/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { previewDashboardSliceError } from 'MainRoot/nosc/dashboard/tabs/previewDashboardSliceError';

describe('previewDashboardSliceError', () => {
  it('returns null for null/undefined', () => {
    expect(previewDashboardSliceError(null, 'fallback')).toBeNull();
    expect(previewDashboardSliceError(undefined, 'fallback')).toBeNull();
  });

  it('returns string errors verbatim', () => {
    expect(previewDashboardSliceError('network down', 'fallback')).toBe('network down');
  });

  it('extracts message from Error objects', () => {
    expect(previewDashboardSliceError(new Error('boom'), 'fallback')).toBe('boom');
  });

  it('falls back for non-string unknown values', () => {
    expect(previewDashboardSliceError({ code: 500 }, 'Failed to load')).toBe('Failed to load');
  });
});
