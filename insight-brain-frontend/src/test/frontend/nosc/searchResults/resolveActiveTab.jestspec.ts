/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/* eslint-env jest */
import { resolveActiveTab } from 'MainRoot/nosc/searchResults/SearchResultsPage';
import { isTabHiddenForSource } from 'MainRoot/nosc/search/searchDataSource';

/**
 * `?tab=` has to resolve to a tab the tablist actually renders. When it doesn't,
 * no tab matches activeTab, so every tab gets tabIndex={-1} (the whole tablist
 * drops out of the Tab order) and the tabpanel's aria-labelledby points at an
 * element that was never rendered.
 */
describe('resolveActiveTab', () => {
  it('keeps a tab that is rendered for the source', () => {
    expect(resolveActiveTab('APPLICATION', 'local')).toBe('APPLICATION');
    expect(resolveActiveTab('COMPONENT', 'catalog')).toBe('COMPONENT');
  });

  it('passes through the "all" pseudo-tab', () => {
    expect(resolveActiveTab('all', 'local')).toBe('all');
    expect(resolveActiveTab('all', 'catalog')).toBe('all');
  });

  it('falls back to "all" for an unknown tab', () => {
    expect(resolveActiveTab('NOPE', 'local')).toBe('all');
    expect(resolveActiveTab('', 'local')).toBe('all');
  });

  it('accepts a lower-case tab param from a hand-written or shared link', () => {
    expect(resolveActiveTab('violation', 'local')).toBe('VIOLATION');
    expect(resolveActiveTab('Component', 'local')).toBe('COMPONENT');
    expect(resolveActiveTab('ALL', 'local')).toBe('all');
    // Source hiding still applies after normalisation.
    expect(resolveActiveTab('application', 'catalog')).toBe('all');
  });

  it('falls back to "all" for a tab hidden by the active source', () => {
    // catalog holds only component-level data, so the IQ-only tabs are not rendered.
    for (const tab of ['APPLICATION', 'VIOLATION', 'WAIVER']) {
      expect(isTabHiddenForSource(tab as 'APPLICATION', 'catalog')).toBe(true);
      expect(resolveActiveTab(tab, 'catalog')).toBe('all');
      // The same tab is fine against the tenant IQ index.
      expect(resolveActiveTab(tab, 'local')).toBe(tab);
    }
  });
});
