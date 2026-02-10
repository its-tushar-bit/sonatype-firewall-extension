/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  getChildCount,
  createJsonNode,
  expandJsonChildren,
  generateJsonPreview,
  findComponentInJson,
} from 'MainRoot/sbomManager/features/billOfMaterials/originalBom/utils/jsonTreeUtils';

describe('jsonTreeUtils', () => {
  describe('getChildCount', () => {
    it('returns array length for arrays', () => {
      expect(getChildCount([1, 2, 3])).toBe(3);
      expect(getChildCount([])).toBe(0);
    });

    it('returns number of keys for objects', () => {
      expect(getChildCount({ a: 1, b: 2 })).toBe(2);
      expect(getChildCount({})).toBe(0);
    });

    it('returns 0 for primitives', () => {
      expect(getChildCount(null)).toBe(0);
      expect(getChildCount('string')).toBe(0);
      expect(getChildCount(42)).toBe(0);
    });
  });

  describe('createJsonNode', () => {
    it('creates node for primitive values', () => {
      const node = createJsonNode('key', 'value', 'path.key');
      expect(node).toEqual({
        id: 'path.key',
        name: 'key',
        value: 'value',
        rawData: null,
        preview: null,
      });
    });

    it('creates node for object values', () => {
      const obj = { nested: 'data' };
      const node = createJsonNode('key', obj, 'path.key');
      expect(node).toEqual({
        id: 'path.key',
        name: 'key',
        value: null,
        rawData: obj,
        preview: 'nested: "data"',
      });
    });
  });

  describe('expandJsonChildren', () => {
    it('expands array items', () => {
      const arr = ['item1', 'item2'];
      const children = expandJsonChildren(arr, 'parent');
      expect(children).toHaveLength(2);
      expect(children[0].name).toBe('0');
      expect(children[0].id).toBe('parent[0]');
      expect(children[1].name).toBe('1');
      expect(children[1].id).toBe('parent[1]');
    });

    it('expands object properties', () => {
      const obj = { key1: 'value1', key2: 'value2' };
      const children = expandJsonChildren(obj, 'parent');
      expect(children).toHaveLength(2);
      expect(children[0].name).toBe('key1');
      expect(children[0].id).toBe('parent.key1');
    });

    it('returns empty array for primitives', () => {
      expect(expandJsonChildren('string', 'parent')).toEqual([]);
      expect(expandJsonChildren(null, 'parent')).toEqual([]);
    });
  });

  describe('generateJsonPreview', () => {
    describe('empty and null inputs', () => {
      it('returns empty string for null', () => {
        expect(generateJsonPreview(null)).toBe('');
      });

      it('returns empty string for undefined', () => {
        expect(generateJsonPreview(undefined)).toBe('');
      });

      it('returns empty string for non-objects', () => {
        expect(generateJsonPreview('string')).toBe('');
        expect(generateJsonPreview(42)).toBe('');
        expect(generateJsonPreview(true)).toBe('');
      });

      it('returns empty string for empty objects', () => {
        expect(generateJsonPreview({})).toBe('');
      });

      it('returns empty string for empty arrays', () => {
        expect(generateJsonPreview([])).toBe('');
      });
    });

    describe('objects with different property counts', () => {
      it('previews object with 1 property', () => {
        const obj = { key1: 'value1' };
        expect(generateJsonPreview(obj)).toBe('key1: "value1"');
      });

      it('previews object with 2 properties', () => {
        const obj = { key1: 'value1', key2: 'value2' };
        expect(generateJsonPreview(obj)).toBe('key1: "value1", key2: "value2"');
      });

      it('previews object with 3 properties without ellipsis', () => {
        const obj = { key1: 'value1', key2: 'value2', key3: 'value3' };
        expect(generateJsonPreview(obj)).toBe('key1: "value1", key2: "value2", key3: "value3"');
      });

      it('previews object with 4+ properties with ellipsis', () => {
        const obj = { key1: 'value1', key2: 'value2', key3: 'value3', key4: 'value4' };
        const preview = generateJsonPreview(obj);
        expect(preview).toContain('…');
        expect(preview.split(',').length).toBe(4); // 3 items + ellipsis
      });
    });

    describe('arrays with different item counts', () => {
      it('previews array with <3 items', () => {
        const arr = ['item1', 'item2'];
        expect(generateJsonPreview(arr)).toBe('"item1", "item2"');
      });

      it('previews array with exactly 3 items without ellipsis', () => {
        const arr = ['item1', 'item2', 'item3'];
        expect(generateJsonPreview(arr)).toBe('"item1", "item2", "item3"');
      });

      it('previews array with >3 items with ellipsis', () => {
        const arr = ['item1', 'item2', 'item3', 'item4', 'item5'];
        const preview = generateJsonPreview(arr);
        expect(preview).toBe('"item1", "item2", "item3", …');
      });
    });

    describe('nested objects and arrays', () => {
      it('shows nested objects as {…}', () => {
        const obj = { key1: { nested: 'value' }, key2: 'value2' };
        expect(generateJsonPreview(obj)).toBe('key1: {…}, key2: "value2"');
      });

      it('shows nested arrays as […]', () => {
        const obj = { key1: [1, 2, 3], key2: 'value2' };
        expect(generateJsonPreview(obj)).toBe('key1: […], key2: "value2"');
      });

      it('handles deeply nested structures', () => {
        const obj = {
          level1: {
            level2: {
              level3: 'deep',
            },
          },
          arr: [
            [1, 2],
            [3, 4],
          ],
        };
        expect(generateJsonPreview(obj)).toBe('level1: {…}, arr: […]');
      });
    });

    describe('different value types', () => {
      it('handles string values with quotes', () => {
        const obj = { str: 'text' };
        expect(generateJsonPreview(obj)).toBe('str: "text"');
      });

      it('handles number values without quotes', () => {
        const obj = { num: 42 };
        expect(generateJsonPreview(obj)).toBe('num: 42');
      });

      it('handles boolean values', () => {
        const obj = { bool: true };
        expect(generateJsonPreview(obj)).toBe('bool: true');
      });

      it('handles null values', () => {
        const obj = { nullVal: null };
        expect(generateJsonPreview(obj)).toBe('nullVal: null');
      });

      it('handles undefined values', () => {
        const obj = { undefinedVal: undefined };
        expect(generateJsonPreview(obj)).toBe('undefinedVal: undefined');
      });

      it('handles long string values', () => {
        const longString = 'a'.repeat(10000);
        const obj = { description: longString, version: '1.0.0' };
        const preview = generateJsonPreview(obj);

        // Preview should be generated without errors
        expect(preview).toContain('description:');
        expect(preview).toContain('version:');
        // Long string is included in preview (CSS will truncate it)
        expect(preview.length).toBeGreaterThan(100);
      });

      it('handles multiple long strings', () => {
        const obj = {
          description: 'x'.repeat(5000),
          license: 'y'.repeat(5000),
          copyright: 'z'.repeat(5000),
        };

        const preview = generateJsonPreview(obj);

        // Should generate preview without errors
        expect(preview).toContain('description:');
        expect(preview).toContain('license:');
        expect(preview).toContain('copyright:');
      });
    });

    describe('performance with large objects', () => {
      it('handles large objects efficiently (50K+ keys)', () => {
        const largeObj = {};
        for (let i = 0; i < 50000; i++) {
          largeObj[`key${i}`] = `value${i}`;
        }

        const startTime = performance.now();
        const preview = generateJsonPreview(largeObj);
        const endTime = performance.now();

        // Should complete in reasonable time (<300ms for CI environments)
        expect(endTime - startTime).toBeLessThan(300);

        // Should still show first 3 items with ellipsis
        expect(preview).toContain('…');
        expect(preview.split(',').length).toBe(4); // 3 items + ellipsis
      });

      it('handles large arrays efficiently', () => {
        const largeArr = new Array(50000).fill('item');

        const startTime = performance.now();
        const preview = generateJsonPreview(largeArr);
        const endTime = performance.now();

        // Should complete in reasonable time (<300ms for CI environments)
        expect(endTime - startTime).toBeLessThan(300);

        // Should show first 3 items with ellipsis
        expect(preview).toBe('"item", "item", "item", …');
      });

      it('handles objects with long string values efficiently', () => {
        const largeObj = {
          description: 'a'.repeat(50000),
          license: 'b'.repeat(50000),
          copyright: 'c'.repeat(50000),
          version: '1.0.0',
        };

        const startTime = performance.now();
        const preview = generateJsonPreview(largeObj);
        const endTime = performance.now();

        // Should complete in reasonable time (<100ms) despite long strings
        expect(endTime - startTime).toBeLessThan(100);

        // Should show first 3 items with ellipsis
        expect(preview).toContain('…');
        expect(preview.split(',').length).toBe(4); // 3 items + ellipsis
      });
    });

    describe('edge cases', () => {
      it('handles objects with symbol keys (ignores them)', () => {
        const sym = Symbol('test');
        const obj = { normalKey: 'value', [sym]: 'symbol value' };
        // Object.keys ignores symbol keys
        expect(generateJsonPreview(obj)).toBe('normalKey: "value"');
      });

      it('handles objects with function values', () => {
        const obj = { func: () => {} };
        const preview = generateJsonPreview(obj);
        expect(preview).toContain('func:');
      });

      it('handles circular references gracefully', () => {
        const obj = { name: 'test' };
        obj.self = obj; // circular reference

        // Should not throw, but may show [object Object] or similar
        expect(() => generateJsonPreview(obj)).not.toThrow();
      });

      it('returns fallback on error', () => {
        // Create an object that throws on property access
        const errorObj = {};
        Object.defineProperty(errorObj, 'badKey', {
          get() {
            throw new Error('Access denied');
          },
          enumerable: true,
        });

        // Should return fallback value instead of throwing
        const preview = generateJsonPreview(errorObj);
        expect(preview).toBe('{…}');
      });
    });
  });

  describe('findComponentInJson', () => {
    const cycloneDxSbom = {
      components: [
        { name: 'component1', purl: 'pkg:npm/component1@1.0.0' },
        { name: 'component2', purl: 'pkg:npm/component2@2.0.0' },
      ],
    };

    const spdxSbom = {
      packages: [
        {
          name: 'package1',
          externalRefs: [{ referenceType: 'purl', referenceLocator: 'pkg:npm/package1@1.0.0' }],
        },
      ],
    };

    it('finds component by purl in CycloneDX format', () => {
      const node = findComponentInJson(cycloneDxSbom, 'pkg:npm/component1@1.0.0');
      expect(node).not.toBeNull();
      expect(node.name).toBe('component');
      expect(node.rawData.name).toBe('component1');
    });

    it('finds package by purl in SPDX format', () => {
      const node = findComponentInJson(spdxSbom, 'pkg:npm/package1@1.0.0');
      expect(node).not.toBeNull();
      expect(node.name).toBe('package');
      expect(node.rawData.name).toBe('package1');
    });

    it('returns null when component not found', () => {
      const node = findComponentInJson(cycloneDxSbom, 'pkg:npm/nonexistent@1.0.0');
      expect(node).toBeNull();
    });

    it('returns null for null inputs', () => {
      expect(findComponentInJson(null, 'pkg:npm/test@1.0.0')).toBeNull();
      expect(findComponentInJson(cycloneDxSbom, null)).toBeNull();
    });

    it('handles JSON string input', () => {
      const jsonString = JSON.stringify(cycloneDxSbom);
      const node = findComponentInJson(jsonString, 'pkg:npm/component1@1.0.0');
      expect(node).not.toBeNull();
      expect(node.rawData.name).toBe('component1');
    });
  });
});
