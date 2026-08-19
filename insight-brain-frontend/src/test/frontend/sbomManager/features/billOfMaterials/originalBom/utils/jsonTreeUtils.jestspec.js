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
  getJsonTotalDescendants,
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

    it('creates node with name@version for array items', () => {
      const component = { name: 'express', version: '4.18.2', purl: 'pkg:npm/express@4.18.2' };
      const node = createJsonNode(0, component, 'components[0]');
      expect(node.name).toBe('express@4.18.2');
      expect(node.id).toBe('components[0]');
    });

    it('uses purl when version is missing (purl has priority over name)', () => {
      const component = { name: 'express', purl: 'pkg:npm/express@4.18.2' };
      const node = createJsonNode(0, component, 'components[0]');
      expect(node.name).toBe('pkg:npm/express@4.18.2');
    });

    it('uses numeric index for non-object array items', () => {
      const node = createJsonNode(0, 'simple string', 'array[0]');
      expect(node.name).toBe('0');
    });

    it('falls back to purl when name is missing but version exists', () => {
      const component = { version: '4.18.2', purl: 'pkg:npm/express@4.18.2' };
      const node = createJsonNode(0, component, 'components[0]');
      // Should use purl since name is missing
      expect(node.name).toBe('pkg:npm/express@4.18.2');
    });

    it('handles empty name and version strings', () => {
      const component = { name: '', version: '', purl: 'pkg:npm/express@4.18.2' };
      const node = createJsonNode(0, component, 'components[0]');
      // Empty strings should be ignored, fall back to purl
      expect(node.name).toBe('pkg:npm/express@4.18.2');
    });

    it('handles whitespace-only name and version', () => {
      const component = { name: '   ', version: '  ', id: 'my-id' };
      const node = createJsonNode(0, component, 'components[0]');
      // Whitespace-only should be ignored, fall back to id
      expect(node.name).toBe('my-id');
    });

    it('does not truncate long name@version combinations', () => {
      // name@version should not be truncated by createJsonNode
      // Truncation happens only in the UI display layer
      const longName = 'a'.repeat(100);
      const longVersion = 'b'.repeat(100);
      const component = { name: longName, version: longVersion };
      const node = createJsonNode(0, component, 'components[0]');
      expect(node.name).toBe(`${longName}@${longVersion}`);
      expect(node.name.length).toBe(201); // 100 + '@' + 100
    });

    it('handles scoped package names with @ symbol', () => {
      const component = { name: '@scope/package', version: '1.0.0' };
      const node = createJsonNode(0, component, 'components[0]');
      expect(node.name).toBe('@scope/package@1.0.0');
    });

    it('handles special characters in name and version', () => {
      const component = { name: '@org/pkg-name_v2', version: '1.0.0-beta.1+build.123' };
      const node = createJsonNode(0, component, 'components[0]');
      expect(node.name).toBe('@org/pkg-name_v2@1.0.0-beta.1+build.123');
    });

    it('respects field priority order when computing display name', () => {
      // Start with an object containing all possible name fields
      // Priority order (highest to lowest): name@version, purl, name, bom-ref, SPDXID, spdxId, id, type, ref
      let component = {
        name: 'test-name',
        version: '1.0.0',
        purl: 'pkg:npm/test@1.0.0',
        id: 'test-id',
        ref: 'test-ref',
        'bom-ref': 'test-bom-ref',
        SPDXID: 'test-spdxid',
        spdxId: 'test-spdxid-lower',
        type: 'library',
      };

      // With all fields, should use name@version (highest priority)
      let node = createJsonNode(0, component, 'components[0]');
      expect(node.name).toBe('test-name@1.0.0');

      // Remove version, should use purl (second priority)
      delete component.version;
      node = createJsonNode(0, component, 'components[0]');
      expect(node.name).toBe('pkg:npm/test@1.0.0');

      // Remove purl, should fall back to name
      delete component.purl;
      node = createJsonNode(0, component, 'components[0]');
      expect(node.name).toBe('test-name');

      // Remove name, should fall back to bom-ref
      delete component.name;
      node = createJsonNode(0, component, 'components[0]');
      expect(node.name).toBe('test-bom-ref');

      // Remove bom-ref, should fall back to SPDXID
      delete component['bom-ref'];
      node = createJsonNode(0, component, 'components[0]');
      expect(node.name).toBe('test-spdxid');

      // Remove SPDXID, should fall back to spdxId
      delete component.SPDXID;
      node = createJsonNode(0, component, 'components[0]');
      expect(node.name).toBe('test-spdxid-lower');

      // Remove spdxId, should fall back to id
      delete component.spdxId;
      node = createJsonNode(0, component, 'components[0]');
      expect(node.name).toBe('test-id');

      // Remove id, should fall back to type
      delete component.id;
      node = createJsonNode(0, component, 'components[0]');
      expect(node.name).toBe('library');

      // Remove type, should fall back to ref
      delete component.type;
      node = createJsonNode(0, component, 'components[0]');
      expect(node.name).toBe('test-ref');

      // Remove ref, should fall back to numeric index
      delete component.ref;
      node = createJsonNode(0, component, 'components[0]');
      expect(node.name).toBe('0');
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

  describe('getJsonTotalDescendants', () => {
    it('returns 0 for nodes without children', () => {
      const node = { id: 'test', name: 'test' };
      expect(getJsonTotalDescendants(node)).toBe(0);
    });

    it('returns 0 for null node', () => {
      expect(getJsonTotalDescendants(null)).toBe(0);
    });

    it('returns 0 for node without rawData', () => {
      const node = { id: 'test', name: 'test', rawData: null };
      expect(getJsonTotalDescendants(node)).toBe(0);
    });

    it('counts direct children in object', () => {
      const rawData = { key1: 'value1', key2: 'value2', key3: 'value3' };
      const node = { id: 'test', rawData };
      expect(getJsonTotalDescendants(node)).toBe(3);
    });

    it('counts direct children in array', () => {
      const rawData = ['item1', 'item2', 'item3'];
      const node = { id: 'test', rawData };
      expect(getJsonTotalDescendants(node)).toBe(3);
    });

    it('counts nested descendants recursively', () => {
      const rawData = {
        level1: {
          level2: {
            level3: 'value',
          },
        },
      };
      const node = { id: 'test', rawData };
      // level1 (1) + level2 (1) + level3 (1) = 3
      expect(getJsonTotalDescendants(node)).toBe(3);
    });

    it('counts array items and nested objects', () => {
      const rawData = {
        components: [
          { name: 'comp1', version: '1.0' },
          { name: 'comp2', version: '2.0' },
        ],
        metadata: { name: 'test' },
      };
      const node = { id: 'test', rawData };
      // components (1) + 2 array items (2) + comp1 props (2) + comp2 props (2) + metadata (1) + metadata.name (1) = 9
      expect(getJsonTotalDescendants(node)).toBe(9);
    });

    it('respects max recursion depth', () => {
      // Create deeply nested structure beyond MAX_RECURSION_DEPTH (32)
      let deepObj = { value: 'deep' };
      for (let i = 0; i < 50; i++) {
        deepObj = { nested: deepObj };
      }
      const node = { id: 'test', rawData: deepObj };
      const count = getJsonTotalDescendants(node);
      // Should stop at exactly depth 32
      expect(count).toBe(32);
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
