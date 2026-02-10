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
} from 'MainRoot/sbomManager/features/billOfMaterials/originalBom/utils/xmlTreeUtils';

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

    it('handles invalid XML by returning parsererror node', () => {
      const tree = parseXmlToTree('invalid xml');
      // DOMParser creates a parsererror element for invalid XML
      expect(tree).toHaveLength(1);
      expect(tree[0].name).toBe('parsererror');
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
      expect(node.name).toBe('component');
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
      expect(node.name).toBe('packages');
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
