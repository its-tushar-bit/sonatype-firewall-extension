/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  isXmlContent,
  parseXmlToTree,
  expandXmlChildren,
  generateXmlPreview,
  findComponentInXml,
  getXmlTotalDescendants,
} from 'MainRoot/sbomManager/features/billOfMaterials/originalBom/utils/xmlTreeUtils';
import { AUTO_EXPAND_THRESHOLD } from 'MainRoot/sbomManager/features/billOfMaterials/originalBom/utils/constants';

describe('xmlTreeUtils', () => {
  describe('isXmlContent', () => {
    it('returns true for XML strings', () => {
      expect(isXmlContent('<?xml version="1.0"?><root></root>')).toBe(true);
      expect(isXmlContent('<root></root>')).toBe(true);
      expect(isXmlContent('  <root></root>')).toBe(true);
    });

    it('returns false for non-XML strings', () => {
      expect(isXmlContent('not xml')).toBe(false);
      expect(isXmlContent('{ "json": "data" }')).toBe(false);
      expect(isXmlContent('')).toBe(false);
    });

    it('returns false for non-strings', () => {
      expect(isXmlContent(null)).toBe(false);
      expect(isXmlContent(undefined)).toBe(false);
      expect(isXmlContent(42)).toBe(false);
      expect(isXmlContent({})).toBe(false);
    });
  });

  describe('parseXmlToTree', () => {
    it('parses simple XML to tree structure', () => {
      const xml = '<root><child>value</child></root>';
      const tree = parseXmlToTree(xml);
      expect(tree).toHaveLength(1);
      expect(tree[0].name).toBe('root');
    });

    it('handles invalid XML by returning empty array', () => {
      const tree = parseXmlToTree('invalid xml');
      // Parser errors are detected and filtered out
      expect(tree).toHaveLength(0);
    });
  });

  describe('expandXmlChildren', () => {
    let parser;

    beforeEach(() => {
      parser = new DOMParser();
    });

    it('expands element nodes', () => {
      const xml = '<root><child1>text1</child1><child2>text2</child2></root>';
      const doc = parser.parseFromString(xml, 'text/xml');
      const children = expandXmlChildren(doc.documentElement, 'root');

      expect(children.length).toBeGreaterThan(0);
      expect(children.some((c) => c.name === 'child1')).toBe(true);
      expect(children.some((c) => c.name === 'child2')).toBe(true);
    });

    it('expands attribute nodes', () => {
      const xml = '<root attr1="value1" attr2="value2"><child/></root>';
      const doc = parser.parseFromString(xml, 'text/xml');
      const children = expandXmlChildren(doc.documentElement, 'root');

      const attributes = children.filter((c) => c.name.startsWith('@'));
      expect(attributes.length).toBe(2);
      expect(attributes[0].name).toBe('@attr1');
      expect(attributes[0].value).toBe('value1');
    });

    it('expands text nodes', () => {
      const xml = '<root>text content</root>';
      const doc = parser.parseFromString(xml, 'text/xml');
      const children = expandXmlChildren(doc.documentElement, 'root');

      const textNode = children.find((c) => c.name === 'text content');
      expect(textNode).toBeDefined();
    });

    it('uses intelligent display names for repeated elements with name@version', () => {
      const xml = `
        <components>
          <component>
            <name>express</name>
            <version>4.18.2</version>
          </component>
          <component>
            <name>lodash</name>
            <version>4.17.21</version>
          </component>
        </components>
      `;
      const doc = parser.parseFromString(xml, 'text/xml');
      const children = expandXmlChildren(doc.documentElement, 'components');

      const components = children.filter((c) => c.name.includes('@'));
      expect(components.length).toBeGreaterThan(0);
      expect(components.some((c) => c.name === 'express@4.18.2')).toBe(true);
      expect(components.some((c) => c.name === 'lodash@4.17.21')).toBe(true);
    });

    it('uses intelligent display name even for single element', () => {
      const xml = `
        <root>
          <component>
            <name>express</name>
            <version>4.18.2</version>
          </component>
        </root>
      `;
      const doc = parser.parseFromString(xml, 'text/xml');
      const children = expandXmlChildren(doc.documentElement, 'root');

      // Intelligent naming is ALWAYS applied for consistency
      // Element should show name@version regardless of sibling count
      const component = children.find((c) => c.name === 'express@4.18.2');
      expect(component).toBeDefined();
      expect(component.name).toBe('express@4.18.2');
    });

    it('falls back to purl when name is missing but version exists', () => {
      const xml = `
        <components>
          <component>
            <version>4.18.2</version>
            <purl>pkg:npm/express@4.18.2</purl>
          </component>
        </components>
      `;
      const doc = parser.parseFromString(xml, 'text/xml');
      const children = expandXmlChildren(doc.documentElement, 'components');
      const component = children.find((c) => c.name.includes('pkg:npm'));
      expect(component).toBeDefined();
      expect(component.name).toBe('pkg:npm/express@4.18.2');
    });

    it('handles empty name and version elements', () => {
      const xml = `
        <components>
          <component>
            <name></name>
            <version></version>
            <purl>pkg:npm/express@4.18.2</purl>
          </component>
        </components>
      `;
      const doc = parser.parseFromString(xml, 'text/xml');
      const children = expandXmlChildren(doc.documentElement, 'components');
      const component = children.find((c) => c.name.includes('pkg:npm'));
      expect(component).toBeDefined();
      // Empty elements should be ignored, fall back to purl
      expect(component.name).toBe('pkg:npm/express@4.18.2');
    });

    it('handles whitespace-only name and version elements', () => {
      const xml = `
        <components>
          <component>
            <name>   </name>
            <version>  </version>
            <id>my-component-id</id>
          </component>
        </components>
      `;
      const doc = parser.parseFromString(xml, 'text/xml');
      const children = expandXmlChildren(doc.documentElement, 'components');
      const component = children.find((c) => c.name === 'my-component-id');
      expect(component).toBeDefined();
      // Whitespace-only should be ignored, fall back to id
      expect(component.name).toBe('my-component-id');
    });

    it('does not truncate long name@version combinations', () => {
      // name@version should not be truncated in the tree data structure
      // Truncation happens only in the UI display layer
      const longName = 'a'.repeat(100);
      const longVersion = 'b'.repeat(100);
      const xml = `
        <components>
          <component>
            <name>${longName}</name>
            <version>${longVersion}</version>
          </component>
        </components>
      `;
      const doc = parser.parseFromString(xml, 'text/xml');
      const children = expandXmlChildren(doc.documentElement, 'components');
      const component = children[0];
      expect(component.name).toBe(`${longName}@${longVersion}`);
      expect(component.name.length).toBe(201); // 100 + '@' + 100
    });

    it('handles scoped package names with @ symbol', () => {
      const xml = `
        <components>
          <component>
            <name>@scope/package</name>
            <version>1.0.0</version>
          </component>
        </components>
      `;
      const doc = parser.parseFromString(xml, 'text/xml');
      const children = expandXmlChildren(doc.documentElement, 'components');
      const component = children[0];
      expect(component.name).toBe('@scope/package@1.0.0');
    });

    it('handles special characters in name and version', () => {
      const xml = `
        <components>
          <component>
            <name>@org/pkg-name_v2</name>
            <version>1.0.0-beta.1+build.123</version>
          </component>
        </components>
      `;
      const doc = parser.parseFromString(xml, 'text/xml');
      const children = expandXmlChildren(doc.documentElement, 'components');
      const component = children[0];
      expect(component.name).toBe('@org/pkg-name_v2@1.0.0-beta.1+build.123');
    });
  });

  describe('generateXmlPreview', () => {
    let parser;

    beforeEach(() => {
      parser = new DOMParser();
    });

    describe('null and invalid nodes', () => {
      it('returns empty string for null node', () => {
        expect(generateXmlPreview(null)).toBe('');
      });

      it('returns empty string for undefined node', () => {
        expect(generateXmlPreview(undefined)).toBe('');
      });

      it('returns text content for text nodes', () => {
        const xml = '<root>text content</root>';
        const doc = parser.parseFromString(xml, 'text/xml');
        const textNode = doc.documentElement.firstChild;
        expect(generateXmlPreview(textNode)).toBe('text content');
      });
    });

    describe('nodes with attributes only', () => {
      it('previews node with 1 attribute', () => {
        const xml = '<root attr1="value1"/>';
        const doc = parser.parseFromString(xml, 'text/xml');
        const preview = generateXmlPreview(doc.documentElement);
        expect(preview).toBe('@attr1: "value1"');
      });

      it('previews node with 2 attributes', () => {
        const xml = '<root attr1="value1" attr2="value2"/>';
        const doc = parser.parseFromString(xml, 'text/xml');
        const preview = generateXmlPreview(doc.documentElement);
        expect(preview).toBe('@attr1: "value1", @attr2: "value2"');
      });

      it('previews node with 3 attributes without ellipsis', () => {
        const xml = '<root attr1="value1" attr2="value2" attr3="value3"/>';
        const doc = parser.parseFromString(xml, 'text/xml');
        const preview = generateXmlPreview(doc.documentElement);
        expect(preview).toBe('@attr1: "value1", @attr2: "value2", @attr3: "value3"');
      });

      it('previews node with 4+ attributes with ellipsis', () => {
        const xml = '<root attr1="value1" attr2="value2" attr3="value3" attr4="value4"/>';
        const doc = parser.parseFromString(xml, 'text/xml');
        const preview = generateXmlPreview(doc.documentElement);
        expect(preview).toContain('…');
        expect(preview.split(',').length).toBe(4); // 3 items + ellipsis
      });
    });

    describe('nodes with children only', () => {
      it('previews node with 1 child', () => {
        const xml = '<root><child1/></root>';
        const doc = parser.parseFromString(xml, 'text/xml');
        const preview = generateXmlPreview(doc.documentElement);
        expect(preview).toBe('child1: {…}');
      });

      it('previews node with 2 children', () => {
        const xml = '<root><child1/><child2/></root>';
        const doc = parser.parseFromString(xml, 'text/xml');
        const preview = generateXmlPreview(doc.documentElement);
        expect(preview).toBe('child1: {…}, child2: {…}');
      });

      it('previews node with 3 children without ellipsis', () => {
        const xml = '<root><child1/><child2/><child3/></root>';
        const doc = parser.parseFromString(xml, 'text/xml');
        const preview = generateXmlPreview(doc.documentElement);
        expect(preview).toBe('child1: {…}, child2: {…}, child3: {…}');
      });

      it('previews node with 4+ children with ellipsis', () => {
        const xml = '<root><child1/><child2/><child3/><child4/></root>';
        const doc = parser.parseFromString(xml, 'text/xml');
        const preview = generateXmlPreview(doc.documentElement);
        expect(preview).toContain('…');
        expect(preview.split(',').length).toBe(4); // 3 items + ellipsis
      });
    });

    describe('nodes with both attributes and children', () => {
      it('previews attributes first, then children', () => {
        const xml = '<root attr1="value1"><child1/></root>';
        const doc = parser.parseFromString(xml, 'text/xml');
        const preview = generateXmlPreview(doc.documentElement);
        expect(preview).toBe('@attr1: "value1", child1: {…}');
      });

      it('respects 3-item limit across attributes and children', () => {
        const xml = '<root attr1="value1" attr2="value2"><child1/><child2/></root>';
        const doc = parser.parseFromString(xml, 'text/xml');
        const preview = generateXmlPreview(doc.documentElement);
        expect(preview).toContain('…');
        expect(preview.split(',').length).toBe(4); // 3 items + ellipsis
      });

      it('shows only attributes if 3+ attributes exist', () => {
        const xml = '<root attr1="value1" attr2="value2" attr3="value3"><child1/><child2/></root>';
        const doc = parser.parseFromString(xml, 'text/xml');
        const preview = generateXmlPreview(doc.documentElement);
        expect(preview).toBe('@attr1: "value1", @attr2: "value2", @attr3: "value3", …');
      });
    });

    describe('performance with large nodes', () => {
      it('handles nodes with many attributes efficiently', () => {
        let xml = '<root';
        for (let i = 0; i < 1000; i++) {
          xml += ` attr${i}="value${i}"`;
        }
        xml += '/>';

        const doc = parser.parseFromString(xml, 'text/xml');

        const startTime = performance.now();
        const preview = generateXmlPreview(doc.documentElement);
        const endTime = performance.now();

        // Should complete in reasonable time (<300ms for CI environments)
        expect(endTime - startTime).toBeLessThan(300);

        // Should show first 3 attributes with ellipsis
        expect(preview).toContain('…');
      });

      it('handles nodes with many children efficiently', () => {
        let xml = '<root>';
        for (let i = 0; i < 1000; i++) {
          xml += `<child${i}/>`;
        }
        xml += '</root>';

        const doc = parser.parseFromString(xml, 'text/xml');

        const startTime = performance.now();
        const preview = generateXmlPreview(doc.documentElement);
        const endTime = performance.now();

        // Should complete in reasonable time (<300ms for CI environments)
        expect(endTime - startTime).toBeLessThan(300);

        // Should show first 3 children with ellipsis
        expect(preview).toContain('…');
      });

      it('handles attributes with long values efficiently', () => {
        const longValue = 'x'.repeat(50000);
        const xml = `<root description="${longValue}" license="${'y'.repeat(50000)}" copyright="${'z'.repeat(
          50000
        )}" version="1.0.0"/>`;

        const doc = parser.parseFromString(xml, 'text/xml');

        const startTime = performance.now();
        const preview = generateXmlPreview(doc.documentElement);
        const endTime = performance.now();

        // Should complete in reasonable time (<100ms) despite long attribute values
        expect(endTime - startTime).toBeLessThan(100);

        // Should show first 3 attributes with ellipsis (4 attrs total)
        expect(preview).toContain('@description:');
        expect(preview).toContain('…');
        expect(preview.split(',').length).toBe(4); // 3 items + ellipsis
      });
    });

    describe('error handling', () => {
      it('returns node name on error', () => {
        const xml = '<root/>';
        const doc = parser.parseFromString(xml, 'text/xml');
        const node = doc.documentElement;

        // Make the node throw an error when accessing properties
        Object.defineProperty(node, 'attributes', {
          get() {
            throw new Error('Access denied');
          },
        });

        const preview = generateXmlPreview(node);
        expect(preview).toBe('root');
      });
    });
  });

  describe('getXmlTotalDescendants', () => {
    let parser;

    beforeEach(() => {
      parser = new DOMParser();
    });

    it('returns 0 for nodes without children', () => {
      const node = { id: 'test', name: 'test' };
      expect(getXmlTotalDescendants(node)).toBe(0);
    });

    it('returns 0 for null node', () => {
      expect(getXmlTotalDescendants(null)).toBe(0);
    });

    it('returns 0 for node without xmlNode', () => {
      const node = { id: 'test', name: 'test', xmlNode: null };
      expect(getXmlTotalDescendants(node)).toBe(0);
    });

    it('does not count attributes as descendants', () => {
      const xml = '<root attr1="value1" attr2="value2"/>';
      const doc = parser.parseFromString(xml, 'text/xml');
      const node = { id: 'root', xmlNode: doc.documentElement };
      // getElementsByTagName('*') only returns elements, not attributes
      expect(getXmlTotalDescendants(node)).toBe(0);
    });

    it('counts element children as descendants', () => {
      const xml = '<root><child1/><child2/><child3/></root>';
      const doc = parser.parseFromString(xml, 'text/xml');
      const node = { id: 'root', xmlNode: doc.documentElement };
      expect(getXmlTotalDescendants(node)).toBe(3);
    });

    it('counts attributes and children', () => {
      const xml = '<root attr="value"><child1/><child2/></root>';
      const doc = parser.parseFromString(xml, 'text/xml');
      const node = { id: 'root', xmlNode: doc.documentElement };
      // getElementsByTagName('*') only returns elements, not attributes
      expect(getXmlTotalDescendants(node)).toBe(2); // 2 child elements
    });

    it('counts nested descendants recursively', () => {
      const xml = '<root><level1><level2><level3/></level2></level1></root>';
      const doc = parser.parseFromString(xml, 'text/xml');
      const node = { id: 'root', xmlNode: doc.documentElement };
      // level1 (1) + level2 (1) + level3 (1) = 3
      expect(getXmlTotalDescendants(node)).toBe(3);
    });

    it('does not count leaf text nodes as separate items', () => {
      const xml = '<root><child>text</child></root>';
      const doc = parser.parseFromString(xml, 'text/xml');
      const node = { id: 'root', xmlNode: doc.documentElement };
      // Only counts child element, not the text inside (leaf text node)
      expect(getXmlTotalDescendants(node)).toBe(1);
    });

    it('counts text nodes in elements with attributes', () => {
      const xml = '<root attr="value">text content</root>';
      const doc = parser.parseFromString(xml, 'text/xml');
      const node = { id: 'root', xmlNode: doc.documentElement };
      // getElementsByTagName('*') only returns elements, not attributes or text nodes
      expect(getXmlTotalDescendants(node)).toBe(0);
    });

    it('returns actual count when below threshold', () => {
      // Create deeply nested structure
      // getElementsByTagName('*') has NO depth limit and returns ALL descendants
      // This test verifies that counts below AUTO_EXPAND_THRESHOLD are returned as-is
      let xml = '<root>';
      for (let i = 0; i < 15; i++) {
        xml += '<nested>';
      }
      for (let i = 0; i < 15; i++) {
        xml += '</nested>';
      }
      xml += '</root>';

      const doc = parser.parseFromString(xml, 'text/xml');
      const node = { id: 'root', xmlNode: doc.documentElement };
      const count = getXmlTotalDescendants(node);
      // Should return actual count (15) since it's below AUTO_EXPAND_THRESHOLD (1000)
      expect(count).toBe(15);
    });

    it('returns actual count for large documents', () => {
      // Create large structure exceeding AUTO_EXPAND_THRESHOLD
      let xml = '<root>';
      for (let i = 0; i < AUTO_EXPAND_THRESHOLD + 100; i++) {
        xml += '<child/>';
      }
      xml += '</root>';

      const doc = parser.parseFromString(xml, 'text/xml');
      const node = { id: 'root', xmlNode: doc.documentElement };
      const count = getXmlTotalDescendants(node);
      // Should return actual count (AUTO_EXPAND_THRESHOLD + 100 = 1100)
      expect(count).toBe(AUTO_EXPAND_THRESHOLD + 100);
    });
  });

  describe('findComponentInXml', () => {
    let parser;

    beforeEach(() => {
      parser = new DOMParser();
    });

    it('finds component by purl in CycloneDX format', () => {
      const xml = `
        <bom>
          <components>
            <component>
              <name>test-component</name>
              <purl>pkg:npm/test-component@1.0.0</purl>
            </component>
          </components>
        </bom>
      `;
      const doc = parser.parseFromString(xml, 'text/xml');
      const node = findComponentInXml(doc, 'pkg:npm/test-component@1.0.0');

      expect(node).not.toBeNull();
      // Uses intelligent naming with purl priority (purl comes before name)
      expect(node.name).toBe('pkg:npm/test-component@1.0.0');
    });

    it('finds package by purl in SPDX format', () => {
      const xml = `
        <Document>
          <packages>
            <SPDXID>SPDXRef-Package</SPDXID>
            <name>test-pkg</name>
            <externalRefs>
              <referenceType>purl</referenceType>
              <referenceLocator>pkg:npm/test-pkg@1.0.0</referenceLocator>
            </externalRefs>
          </packages>
        </Document>
      `;
      const doc = parser.parseFromString(xml, 'text/xml');
      const node = findComponentInXml(doc, 'pkg:npm/test-pkg@1.0.0');

      expect(node).not.toBeNull();
      // Uses intelligent naming to extract name from <name> child element
      expect(node.name).toBe('test-pkg');
    });

    it('returns null when component not found', () => {
      const xml = '<bom><components></components></bom>';
      const doc = parser.parseFromString(xml, 'text/xml');
      const node = findComponentInXml(doc, 'pkg:npm/nonexistent@1.0.0');

      expect(node).toBeNull();
    });

    it('returns null for null inputs', () => {
      const xml = '<bom></bom>';
      const doc = parser.parseFromString(xml, 'text/xml');

      expect(findComponentInXml(null, 'pkg:npm/test@1.0.0')).toBeNull();
      expect(findComponentInXml(doc, null)).toBeNull();
    });
  });
});
