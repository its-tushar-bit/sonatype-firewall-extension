/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/* eslint-env jest */
import {
  ALL_TAB_ID,
  ROWS_VISIBLE_PER_TAB,
  buildPanelTabs,
  derivePanelState,
  flattenSuggestRows,
  itemTypeTokens,
  resultsTabForPanelTab,
  selectTabRows,
} from 'MainRoot/nosc/search/searchPanelModel';
import { SearchEntityType, SearchRow, SuggestGroupRows } from 'MainRoot/nosc/search/searchTypes';
import { FILTER_TREE } from 'MainRoot/nosc/search/searchFilterTree';

function row(type: SearchEntityType, id: string, source: 'local' | 'catalog' = 'local'): SearchRow {
  return { id, type, source, title: `${type}-${id}`, subtitle: '', href: null, fields: {} };
}

function group(type: SearchEntityType, rows: SearchRow[]): SuggestGroupRows {
  return { type, source: 'local', rows };
}

describe('derivePanelState', () => {
  const base = { panelOpen: true, trimmedQuery: 'log4j', loading: false, rowCount: 3 };

  it('is closed whenever the panel is not open, regardless of query or results', () => {
    expect(derivePanelState({ ...base, panelOpen: false })).toBe('closed');
  });

  it('is focused-empty on an open panel with no query', () => {
    expect(derivePanelState({ ...base, trimmedQuery: '' })).toBe('focused-empty');
  });

  it('is focused-short for a query below the minimum length', () => {
    expect(derivePanelState({ ...base, trimmedQuery: 'l' })).toBe('focused-short');
  });

  it('keeps a short query on recent searches even while a stale fetch is in flight', () => {
    expect(derivePanelState({ ...base, trimmedQuery: 'l', loading: true })).toBe('focused-short');
  });

  it('is loading while a request for a long-enough query is in flight', () => {
    expect(derivePanelState({ ...base, loading: true, rowCount: 0 })).toBe('loading');
  });

  it('is loaded-empty when the request finished with no rows', () => {
    expect(derivePanelState({ ...base, rowCount: 0 })).toBe('loaded-empty');
  });

  it('is loaded when rows are available', () => {
    expect(derivePanelState(base)).toBe('loaded');
  });
});

describe('buildPanelTabs', () => {
  it('puts All first and one tab per entity type for the local source', () => {
    const tabs = buildPanelTabs('local', {}, 12);
    expect(tabs[0]).toEqual({ id: ALL_TAB_ID, label: 'All', count: 12 });
    expect(tabs.map((tab) => tab.id)).toEqual([
      ALL_TAB_ID,
      'APPLICATION',
      'COMPONENT',
      'VULNERABILITY',
      'VIOLATION',
      'WAIVER',
    ]);
  });

  it('drops tabs the catalog source cannot serve', () => {
    const tabs = buildPanelTabs('catalog', {}, 4);
    expect(tabs.map((tab) => tab.id)).toEqual([ALL_TAB_ID, 'COMPONENT', 'VULNERABILITY']);
  });

  it('carries per-type counts onto their tabs', () => {
    const tabs = buildPanelTabs('local', { COMPONENT: 7, APPLICATION: 0 }, 7);
    expect(tabs.find((tab) => tab.id === 'COMPONENT')?.count).toBe(7);
    expect(tabs.find((tab) => tab.id === 'APPLICATION')?.count).toBe(0);
    // An absent count stays undefined rather than defaulting to zero, so the tab
    // is not wrongly disabled while counts are unknown.
    expect(tabs.find((tab) => tab.id === 'WAIVER')?.count).toBeUndefined();
  });
});

describe('flattenSuggestRows', () => {
  it('promotes the best match to the front', () => {
    const best = row('VULNERABILITY', 'cve-1');
    const rows = flattenSuggestRows(best, [group('COMPONENT', [row('COMPONENT', 'c1')])], 'local');
    expect(rows[0]).toBe(best);
    expect(rows).toHaveLength(2);
  });

  it('does not repeat a best match that also appears in its group', () => {
    const best = row('VULNERABILITY', 'cve-1');
    const rows = flattenSuggestRows(best, [group('VULNERABILITY', [row('VULNERABILITY', 'cve-1')])], 'local');
    expect(rows).toHaveLength(1);
  });

  it('drops rows whose type the active source cannot serve', () => {
    const rows = flattenSuggestRows(
      null,
      [group('APPLICATION', [row('APPLICATION', 'a1')]), group('COMPONENT', [row('COMPONENT', 'c1')])],
      'catalog'
    );
    expect(rows.map((r) => r.type)).toEqual(['COMPONENT']);
  });

  it('drops a best match the active source cannot serve', () => {
    const rows = flattenSuggestRows(row('APPLICATION', 'a1'), [], 'catalog');
    expect(rows).toHaveLength(0);
  });
});

describe('selectTabRows', () => {
  const tabs = buildPanelTabs('local', {}, 0);

  it('mixes one row per entity type on the All tab', () => {
    const rows = [
      row('COMPONENT', 'c1'),
      row('COMPONENT', 'c2'),
      row('APPLICATION', 'a1'),
      row('VIOLATION', 'v1'),
    ];
    const selected = selectTabRows(rows, ALL_TAB_ID, tabs);
    expect(selected.map((r) => r.type)).toEqual(['COMPONENT', 'APPLICATION', 'VIOLATION']);
  });

  it('keeps the leading best match first on the All tab even when its type sorts later', () => {
    const best = row('WAIVER', 'w1');
    const selected = selectTabRows([best, row('COMPONENT', 'c1')], ALL_TAB_ID, tabs);
    expect(selected[0]).toBe(best);
  });

  it('filters to a single type on a type tab', () => {
    const rows = [row('COMPONENT', 'c1'), row('APPLICATION', 'a1'), row('COMPONENT', 'c2')];
    const selected = selectTabRows(rows, 'COMPONENT', tabs);
    expect(selected.map((r) => r.id)).toEqual(['c1', 'c2']);
  });

  it('caps the rows at the per-tab limit', () => {
    const rows = Array.from({ length: 12 }, (_, i) => row('COMPONENT', `c${i}`));
    expect(selectTabRows(rows, 'COMPONENT', tabs)).toHaveLength(ROWS_VISIBLE_PER_TAB);
  });

  it('returns no rows for a non-All tab id the tab list does not offer', () => {
    // The catalog tab list has no APPLICATION tab, so an APPLICATION selection has
    // nothing to filter by. Falling through to the mixed All list would render rows
    // contradicting the requested narrowing.
    const catalogTabs = buildPanelTabs('catalog', {}, 0);
    const rows = [row('COMPONENT', 'c1', 'catalog'), row('VULNERABILITY', 'v1', 'catalog')];
    expect(selectTabRows(rows, 'APPLICATION', catalogTabs)).toEqual([]);
  });
});

describe('itemTypeTokens', () => {
  it('finds no tokens in a plain query', () => {
    expect(itemTypeTokens('log4j')).toEqual([]);
  });

  it('extracts a single token', () => {
    expect(itemTypeTokens('itemType:APPLICATION log4j')).toEqual(['APPLICATION']);
  });

  it('extracts multiple tokens', () => {
    expect(itemTypeTokens('itemType:APPLICATION itemType:COMPONENT')).toEqual(['APPLICATION', 'COMPONENT']);
  });

  it('maps both violation item types onto the merged VIOLATION tab', () => {
    expect(itemTypeTokens('itemType:POLICY_VIOLATION')).toEqual(['VIOLATION']);
    expect(itemTypeTokens('itemType:LEGAL_VIOLATION')).toEqual(['VIOLATION']);
  });

  it('maps the SECURITY_VULNERABILITY item type onto the Vulnerabilities tab', () => {
    // The Type filter leaf inserts itemType:SECURITY_VULNERABILITY, which must
    // narrow to the Vulnerabilities tab just like the violation types narrow.
    expect(itemTypeTokens('itemType:SECURITY_VULNERABILITY')).toEqual(['VULNERABILITY']);
  });

  it('narrows to exactly one tab for every itemType leaf the Type filter offers', () => {
    const itemTypeLeaves = FILTER_TREE.flatMap((node) => [
      ...(node.leaves ?? []),
      ...(node.groups ?? []).flatMap((group) => group.leaves),
    ])
      .map((leaf) => leaf.syntax)
      .filter((syntax) => syntax.startsWith('itemType:'));

    expect(itemTypeLeaves).toHaveLength(5);
    for (const syntax of itemTypeLeaves) {
      expect(itemTypeTokens(syntax)).toHaveLength(1);
    }
  });

  it('ignores a token with no matching tab', () => {
    expect(itemTypeTokens('itemType:NOT_A_TYPE')).toEqual([]);
  });
});

describe('resultsTabForPanelTab', () => {
  it('maps the All tab onto the results ALL tab', () => {
    expect(resultsTabForPanelTab(ALL_TAB_ID)).toBe('ALL');
  });

  it('passes an entity-type tab through unchanged', () => {
    expect(resultsTabForPanelTab('COMPONENT')).toBe('COMPONENT');
  });
});
