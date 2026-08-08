/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  DEFAULT_UPSTREAM_URLS,
  FORMAT_OPTIONS,
  getFormatIcon,
  getFormatLabel,
  isPackageHostUrlRequired,
  isPccsEligible,
} from 'MainRoot/firewall/iqProxy/proxyRepositoryFormats';

describe('proxyRepositoryFormats (FIRE-665)', () => {
  it('FORMAT_OPTIONS exposes the four allow-listed formats in order', () => {
    expect(FORMAT_OPTIONS.map((o) => o.value)).toEqual(['maven2', 'npm', 'pypi', 'nuget']);
    expect(FORMAT_OPTIONS.map((o) => o.label)).toEqual(['Maven', 'npm', 'PyPI', 'NuGet']);
  });

  it('DEFAULT_UPSTREAM_URLS has an entry for every format option', () => {
    FORMAT_OPTIONS.forEach((opt) => {
      expect(typeof DEFAULT_UPSTREAM_URLS[opt.value]).toBe('string');
      expect(DEFAULT_UPSTREAM_URLS[opt.value]).toMatch(/^https?:\/\//);
    });
  });

  it('getFormatLabel maps values to human-facing labels', () => {
    expect(getFormatLabel('maven2')).toBe('Maven');
    expect(getFormatLabel('npm')).toBe('npm');
    expect(getFormatLabel('pypi')).toBe('PyPI');
    expect(getFormatLabel('nuget')).toBe('NuGet');
  });

  it('getFormatLabel falls back to the raw value when unknown', () => {
    expect(getFormatLabel('docker')).toBe('docker');
  });

  it('getFormatIcon returns a truthy icon reference for known formats', () => {
    expect(getFormatIcon('maven2')).toBeTruthy();
    expect(getFormatIcon('npm')).toBeTruthy();
    expect(getFormatIcon('pypi')).toBeTruthy();
    expect(getFormatIcon('nuget')).toBeTruthy();
  });

  it('getFormatIcon returns null for unknown formats', () => {
    expect(getFormatIcon('docker')).toBeNull();
  });

  it('isPccsEligible is true for npm and pypi only', () => {
    expect(isPccsEligible('maven2')).toBe(false);
    expect(isPccsEligible('nuget')).toBe(false);
    expect(isPccsEligible('npm')).toBe(true);
    expect(isPccsEligible('pypi')).toBe(true);
  });

  it('isPackageHostUrlRequired is true only for pypi', () => {
    expect(isPackageHostUrlRequired('maven2')).toBe(false);
    expect(isPackageHostUrlRequired('nuget')).toBe(false);
    expect(isPackageHostUrlRequired('npm')).toBe(false);
    expect(isPackageHostUrlRequired('pypi')).toBe(true);
  });
});
