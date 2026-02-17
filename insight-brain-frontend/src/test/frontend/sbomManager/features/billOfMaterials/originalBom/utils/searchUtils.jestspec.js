/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  getHighlightParts,
  nodeMatchesSearch,
  filterTreeNodes,
  countMatchingNodes,
} from 'MainRoot/sbomManager/features/billOfMaterials/originalBom/utils/searchUtils';

describe('searchUtils', () => {
  describe('getHighlightParts', () => {
    it('returns empty string for null', () => {
      expect(getHighlightParts(null, 'test')).toBe('');
    });

    it('returns empty string for undefined', () => {
      expect(getHighlightParts(undefined, 'test')).toBe('');
    });

    it('returns original text when no search term', () => {
      expect(getHighlightParts('hello', '')).toBe('hello');
      expect(getHighlightParts('hello', null)).toBe('hello');
    });

    it('returns text string when no match found', () => {
      expect(getHighlightParts('hello', 'xyz')).toBe('hello');
    });

    it('returns highlight parts for matching text (case-insensitive)', () => {
      const result = getHighlightParts('Hello World', 'world');
      expect(result).toEqual({
        before: 'Hello ',
        match: 'World',
        after: '',
      });
    });

    it('handles number values', () => {
      expect(getHighlightParts(123, 'test')).toBe('123');
      const result = getHighlightParts(0, '0');
      expect(result).toEqual({
        before: '',
        match: '0',
        after: '',
      });
    });

    it('handles boolean values', () => {
      const result = getHighlightParts(true, 'true');
      expect(result).toEqual({
        before: '',
        match: 'true',
        after: '',
      });
    });

    it('handles empty strings', () => {
      expect(getHighlightParts('', 'test')).toBe('');
    });

    it('highlights first occurrence only', () => {
      const result = getHighlightParts('test test test', 'test');
      expect(result).toEqual({
        before: '',
        match: 'test',
        after: ' test test',
      });
    });

    it('is case-insensitive', () => {
      const result1 = getHighlightParts('Component', 'comp');
      const result2 = getHighlightParts('component', 'COMP');
      expect(result1).toEqual({
        before: '',
        match: 'Comp',
        after: 'onent',
      });
      expect(result2).toEqual({
        before: '',
        match: 'comp',
        after: 'onent',
      });
    });
  });

  describe('nodeMatchesSearch', () => {
    it('returns true when no search term', () => {
      const node = { name: 'test', value: 'value' };
      expect(nodeMatchesSearch(node, '')).toBe(true);
    });

    it('matches node name (case-insensitive)', () => {
      const node = { name: 'Component', value: null };
      expect(nodeMatchesSearch(node, 'component')).toBe(true);
      expect(nodeMatchesSearch(node, 'comp')).toBe(true);
    });

    it('matches node value (case-insensitive)', () => {
      const node = { name: 'key', value: 'TestValue' };
      expect(nodeMatchesSearch(node, 'testvalue')).toBe(true);
      expect(nodeMatchesSearch(node, 'value')).toBe(true);
    });

    it('returns false when no match', () => {
      const node = { name: 'Component', value: 'value' };
      expect(nodeMatchesSearch(node, 'xyz')).toBe(false);
    });

    it('handles null values', () => {
      const node = { name: 'key', value: null };
      expect(nodeMatchesSearch(node, 'key')).toBe(true);
      expect(nodeMatchesSearch(node, 'xyz')).toBe(false);
    });

    it('handles number values', () => {
      const node = { name: 'count', value: 123 };
      expect(nodeMatchesSearch(node, '123')).toBe(true);
      expect(nodeMatchesSearch(node, '12')).toBe(true);
    });
  });

  describe('filterTreeNodes', () => {
    it('returns original nodes when no search term', () => {
      const nodes = [{ id: '1', name: 'test', value: 'value' }];
      const result = filterTreeNodes(nodes, '');
      expect(result).toEqual(nodes);
    });

    it('filters nodes with single character search', () => {
      const nodes = [
        { id: '1', name: 'apple', value: 'red' },
        { id: '2', name: 'orange', value: 'yellow' },
      ];
      const result = filterTreeNodes(nodes, 'x');
      expect(result).toHaveLength(0);

      const result2 = filterTreeNodes(nodes, 'p');
      expect(result2).toHaveLength(1);
      expect(result2[0].name).toBe('apple');
      expect(result2[0].matchesSearch).toBe(true);
    });

    it('returns empty array when no matches', () => {
      const nodes = [
        { id: '1', name: 'component', value: 'value1' },
        { id: '2', name: 'package', value: 'value2' },
      ];
      const result = filterTreeNodes(nodes, 'xyz');
      expect(result).toEqual([]);
    });

    it('filters nodes by name match', () => {
      const nodes = [
        { id: '1', name: 'component', value: 'value1' },
        { id: '2', name: 'package', value: 'value2' },
      ];
      const result = filterTreeNodes(nodes, 'comp');
      expect(result).toHaveLength(1);
      expect(result[0].name).toBe('component');
      expect(result[0].matchesSearch).toBe(true);
    });

    it('filters nodes by value match', () => {
      const nodes = [
        { id: '1', name: 'key1', value: 'component' },
        { id: '2', name: 'key2', value: 'package' },
      ];
      const result = filterTreeNodes(nodes, 'comp');
      expect(result).toHaveLength(1);
      expect(result[0].value).toBe('component');
      expect(result[0].matchesSearch).toBe(true);
    });

    it('includes parent nodes when children match', () => {
      const nodes = [
        {
          id: 'parent',
          name: 'parent',
          value: null,
          children: [
            { id: 'child1', name: 'child1', value: 'component' },
            { id: 'child2', name: 'child2', value: 'package' },
          ],
        },
      ];

      const result = filterTreeNodes(nodes, 'comp');
      expect(result).toHaveLength(1);
      expect(result[0].name).toBe('parent');
      expect(result[0].hasMatchingDescendants).toBe(true);
      expect(result[0].children).toBeDefined();
      expect(result[0].children.length).toBe(1);
      expect(result[0].children[0].value).toBe('component');
    });

    it('expands collapsed nodes to search within them', () => {
      const nodes = [
        {
          id: '1',
          name: 'data',
          value: null,
          rawData: { nested: 'component' },
        },
      ];

      const result = filterTreeNodes(nodes, 'comp');
      expect(result).toHaveLength(1);
      expect(result[0].hasMatchingDescendants).toBe(true);
      expect(result[0].children).toBeDefined();
      expect(result[0].children.length).toBeGreaterThan(0);
    });

    it('does not add isOpen property to filtered nodes', () => {
      const nodes = [
        {
          id: 'parent',
          name: 'parent',
          value: null,
          children: [{ id: 'child', name: 'child', value: 'component' }],
        },
      ];

      const result = filterTreeNodes(nodes, 'comp');
      expect(result[0].isOpen).toBeUndefined();
    });

    it('adds matchesSearch and hasMatchingDescendants properties', () => {
      const nodes = [
        {
          id: 'parent',
          name: 'parent',
          value: null,
          children: [{ id: 'child', name: 'child', value: 'component' }],
        },
      ];

      const result = filterTreeNodes(nodes, 'comp');
      expect(result[0]).toHaveProperty('matchesSearch');
      expect(result[0]).toHaveProperty('hasMatchingDescendants');
    });

    it('handles nested filtering correctly', () => {
      const nodes = [
        {
          id: 'root',
          name: 'root',
          value: null,
          children: [
            {
              id: 'level1',
              name: 'level1',
              value: null,
              children: [
                {
                  id: 'level2',
                  name: 'level2',
                  value: null,
                  children: [{ id: 'match', name: 'match', value: 'component' }],
                },
              ],
            },
          ],
        },
      ];

      const result = filterTreeNodes(nodes, 'comp');
      expect(result).toHaveLength(1);
      expect(result[0].hasMatchingDescendants).toBe(true);
    });

    it('handles empty nodes array', () => {
      const result = filterTreeNodes([], 'test');
      expect(result).toEqual([]);
    });

    it('handles null nodes', () => {
      const result = filterTreeNodes(null, 'test');
      expect(result).toBeNull();
    });

    it('is case-insensitive', () => {
      const nodes = [
        { id: '1', name: 'Component', value: 'Value' },
        { id: '2', name: 'Package', value: 'Data' },
      ];

      const result1 = filterTreeNodes(nodes, 'COMPONENT');
      const result2 = filterTreeNodes(nodes, 'component');
      const result3 = filterTreeNodes(nodes, 'CoMpOnEnT');

      expect(result1).toHaveLength(1);
      expect(result2).toHaveLength(1);
      expect(result3).toHaveLength(1);
    });

    it('filters multiple matching nodes', () => {
      const nodes = [
        { id: '1', name: 'component-1', value: 'value1' },
        { id: '2', name: 'component-2', value: 'value2' },
        { id: '3', name: 'package', value: 'value3' },
      ];

      const result = filterTreeNodes(nodes, 'comp');
      expect(result).toHaveLength(2);
    });

    it('respects MAX_SEARCH_DEPTH and prevents stack overflow on deeply nested structures', () => {
      // Create a deeply nested structure (15 levels > MAX_SEARCH_DEPTH of 10)
      const createDeeplyNestedRawData = (depth, maxDepth) => {
        if (depth >= maxDepth) {
          return { match: 'deepmatch' };
        }
        return {
          [`level${depth}`]: createDeeplyNestedRawData(depth + 1, maxDepth),
        };
      };

      const nodes = [
        {
          id: 'root',
          name: 'root',
          value: null,
          rawData: createDeeplyNestedRawData(0, 15),
        },
      ];

      // Should not cause stack overflow and should still work
      expect(() => {
        const result = filterTreeNodes(nodes, 'deepmatch');
        // The result should be empty or have limited depth because MAX_SEARCH_DEPTH stops expansion
        // at depth 10, and the match is at depth 15
        expect(result).toBeDefined();
      }).not.toThrow();
    });

    it('finds matches within MAX_SEARCH_DEPTH limit', () => {
      // Create a nested structure within MAX_SEARCH_DEPTH (8 levels < MAX_SEARCH_DEPTH of 10)
      const createDeeplyNestedRawData = (depth, maxDepth) => {
        if (depth >= maxDepth) {
          return { match: 'shallowmatch' };
        }
        return {
          [`level${depth}`]: createDeeplyNestedRawData(depth + 1, maxDepth),
        };
      };

      const nodes = [
        {
          id: 'root',
          name: 'root',
          value: null,
          rawData: createDeeplyNestedRawData(0, 8),
        },
      ];

      // Should find the match since it's within MAX_SEARCH_DEPTH
      const result = filterTreeNodes(nodes, 'shallowmatch');
      expect(result).toHaveLength(1);
      expect(result[0].hasMatchingDescendants).toBe(true);
    });

    it('prevents stack overflow with absolute depth limit on pathologically deep structures', () => {
      // Create a structure deeper than MAX_SEARCH_DEPTH * 2 (25 levels > 20)
      const createDeeplyNestedRawData = (depth, maxDepth) => {
        if (depth >= maxDepth) {
          return { match: 'verydeepmatch' };
        }
        return {
          [`level${depth}`]: createDeeplyNestedRawData(depth + 1, maxDepth),
        };
      };

      const nodes = [
        {
          id: 'root',
          name: 'root',
          value: null,
          rawData: createDeeplyNestedRawData(0, 25),
        },
      ];

      // Should not cause stack overflow due to absolute depth limit
      expect(() => {
        const result = filterTreeNodes(nodes, 'verydeepmatch');
        expect(result).toBeDefined();
        // Result should be empty because match is beyond absolute depth limit
        expect(result).toHaveLength(0);
      }).not.toThrow();
    });
  });

  describe('countMatchingNodes', () => {
    it('returns 0 for empty array', () => {
      expect(countMatchingNodes([])).toBe(0);
    });

    it('returns 0 for null or undefined', () => {
      expect(countMatchingNodes(null)).toBe(0);
      expect(countMatchingNodes(undefined)).toBe(0);
    });

    it('counts nodes with matchesSearch=true', () => {
      const nodes = [
        { id: '1', name: 'test', matchesSearch: true },
        { id: '2', name: 'other', matchesSearch: false },
        { id: '3', name: 'test2', matchesSearch: true },
      ];
      expect(countMatchingNodes(nodes)).toBe(2);
    });

    it('counts matching nodes recursively in children', () => {
      const nodes = [
        {
          id: '1',
          name: 'parent',
          matchesSearch: false,
          children: [
            { id: '1.1', name: 'child1', matchesSearch: true },
            { id: '1.2', name: 'child2', matchesSearch: true },
          ],
        },
      ];
      expect(countMatchingNodes(nodes)).toBe(2);
    });

    it('counts matching nodes at all levels', () => {
      const nodes = [
        {
          id: '1',
          name: 'root',
          matchesSearch: true,
          children: [
            {
              id: '1.1',
              name: 'level1',
              matchesSearch: true,
              children: [
                { id: '1.1.1', name: 'level2', matchesSearch: true },
                { id: '1.1.2', name: 'level2b', matchesSearch: false },
              ],
            },
            { id: '1.2', name: 'level1b', matchesSearch: true },
          ],
        },
        { id: '2', name: 'root2', matchesSearch: true },
      ];
      expect(countMatchingNodes(nodes)).toBe(5);
    });

    it('handles nodes without matchesSearch property', () => {
      const nodes = [
        { id: '1', name: 'test' },
        { id: '2', name: 'other', matchesSearch: true },
      ];
      expect(countMatchingNodes(nodes)).toBe(1);
    });

    it('handles nodes with empty children array', () => {
      const nodes = [{ id: '1', name: 'test', matchesSearch: true, children: [] }];
      expect(countMatchingNodes(nodes)).toBe(1);
    });
  });
});
