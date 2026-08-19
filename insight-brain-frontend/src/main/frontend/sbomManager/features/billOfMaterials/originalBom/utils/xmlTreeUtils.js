/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
// MAX_TITLE_LENGTH is not needed anymore since we don't show attributes in element names

export const isXmlContent = (content) => {
  if (typeof content !== 'string') return false;
  const trimmed = content.trim();
  return trimmed.startsWith('<');
};

/**
 * Extracts an intelligent display name from an XML node by looking for identifying attributes or child elements.
 * Priority order: name@version → purl → name → bom-ref → SPDXID → spdxId → id → type → ref
 * Then checks child elements with same priority
 * Fallback: uses element name
 */
const getXmlDisplayName = (xmlNode, fallbackName) => {
  if (!xmlNode || xmlNode.nodeType !== Node.ELEMENT_NODE) {
    return fallbackName;
  }

  // Check for name@version combination in child elements
  // CycloneDX uses <name> and <version>, SPDX uses <name> and <versionInfo>
  // Use childNodes iteration instead of querySelector to handle namespaced XML
  let nameValue = null;
  let versionValue = null;

  if (xmlNode.childNodes) {
    for (let i = 0; i < xmlNode.childNodes.length; i++) {
      const child = xmlNode.childNodes[i];
      if (child.nodeType !== Node.ELEMENT_NODE) continue;

      const childName = child.localName || child.nodeName;
      if (childName === 'name') {
        nameValue = child.textContent?.trim();
      } else if (childName === 'version' || childName === 'versionInfo') {
        versionValue = child.textContent?.trim();
      }
      // Break early if both found
      if (nameValue && versionValue) {
        break;
      }
    }
  }

  // Return name@version if both are present
  if (nameValue && versionValue) {
    return `${nameValue}@${versionValue}`;
  }

  // Priority order for identifying fields
  const identifyingFields = ['purl', 'name', 'bom-ref', 'SPDXID', 'spdxId', 'id', 'type', 'ref'];

  // First check attributes
  if (xmlNode.attributes) {
    for (const field of identifyingFields) {
      const attr = xmlNode.getAttribute(field);
      if (attr && attr.trim()) {
        return attr.trim();
      }
    }
  }

  // Then check immediate child elements (not deep descendants)
  if (xmlNode.childNodes) {
    for (const field of identifyingFields) {
      for (let i = 0; i < xmlNode.childNodes.length; i++) {
        const child = xmlNode.childNodes[i];
        if (child.nodeType !== Node.ELEMENT_NODE) continue;

        const childName = child.localName || child.nodeName;
        if (childName === field) {
          const textContent = child.textContent?.trim();
          if (textContent) {
            return textContent;
          }
        }
      }
    }
  }

  // Fallback: just use element name without attributes
  // Attributes are shown as child nodes in the tree, so no need to show them in the name
  return fallbackName;
};

/**
 * Creates a node for an XML element.
 * Handles both initial tree creation and lazy expansion with intelligent naming.
 *
 * @param {Element} node - The XML element node
 * @param {string} path - Path of parent node (e.g., "bom.components")
 * @param {number} [elementIndex] - Optional index for array-like elements (e.g., 0, 1, 2...)
 * @returns {Object} Tree node object
 */
const createXmlElementNode = (node, path, elementIndex) => {
  // Use localName for namespace-agnostic element names, fallback to nodeName
  const elementName = node.localName || node.nodeName;
  // Build path with optional array index
  let nodePath;
  if (elementIndex !== undefined) {
    nodePath = `${path}.${elementName}[${elementIndex}]`;
  } else {
    nodePath = path ? `${path}.${elementName}` : elementName;
  }

  const hasAttributes = node.attributes && node.attributes.length > 0;
  const isSingleTextNode =
    !hasAttributes && node.childNodes.length === 1 && node.childNodes[0].nodeType === Node.TEXT_NODE;

  if (isSingleTextNode) {
    return {
      id: nodePath,
      name: elementName,
      value: node.childNodes[0].textContent?.trim(),
    };
  }

  // For root elements (path is empty), just use element name without intelligent naming
  // This prevents SPDX <Document> from being named "Test" based on its <name> child
  const isRootElement = !path || path === '';
  const displayName = isRootElement ? elementName : getXmlDisplayName(node, elementName);

  // Check if node has expandable content (attributes or child elements/text)
  let hasExpandableContent = hasAttributes;
  if (!hasExpandableContent) {
    for (let i = 0; i < node.childNodes.length; i++) {
      const child = node.childNodes[i];
      if (child.nodeType === Node.ELEMENT_NODE || (child.nodeType === Node.TEXT_NODE && child.textContent?.trim())) {
        hasExpandableContent = true;
        break;
      }
    }
  }

  // Generate preview only if node has expandable content
  const preview = hasExpandableContent ? generateXmlPreview(node) : null;

  return {
    id: nodePath,
    name: displayName,
    value: null,
    xmlNode: node, // Store DOM node reference for lazy expansion (performance optimization)
    preview, // Show preview of collapsed content (e.g., "@name: 'express', @version: '4.18.2', …")
  };
};

const createXmlTextNode = (path, text, index) => ({
  id: `${path}_text_${index}`,
  name: text,
  value: null,
});

export const parseXmlToTree = (xmlString) => {
  try {
    const parser = new DOMParser();
    const xmlDoc = parser.parseFromString(xmlString, 'text/xml');

    // Check for parser errors
    const parserError = xmlDoc.getElementsByTagName('parsererror');
    if (parserError.length > 0) {
      return [];
    }

    // documentElement is always an ELEMENT_NODE, so we can call createXmlElementNode directly
    const root = createXmlElementNode(xmlDoc.documentElement, '');
    return root ? [root] : [];
  } catch (e) {
    return [];
  }
};

// Find and extract a specific component by PURL from SBOM XML
// Supports both CycloneDX (components) and SPDX (packages) formats
export const findComponentInXml = (xmlDoc, componentPurl) => {
  console.log('[findComponentInXml] Searching for purl:', componentPurl);
  if (!componentPurl || !xmlDoc) return null;

  try {
    const purlElements = xmlDoc.getElementsByTagName('purl');
    console.log('[findComponentInXml] Found purl elements:', purlElements.length);
    for (let i = 0; i < purlElements.length; i++) {
      const purl = purlElements[i].textContent?.trim();
      const parentName = purlElements[i].parentNode?.localName || purlElements[i].parentNode?.nodeName;
      console.log('[findComponentInXml] Checking purl:', purl, 'parent:', parentName);
      if (purl === componentPurl && parentName === 'component') {
        console.log('[findComponentInXml] Found matching component');
        const node = createXmlElementNode(purlElements[i].parentNode, 'components');
        console.log('[findComponentInXml] Created node:', node);
        return node;
      }
    }

    const refLocators = xmlDoc.getElementsByTagName('referenceLocator');
    console.log('[findComponentInXml] Checking SPDX referenceLocator elements:', refLocators.length);
    for (let i = 0; i < refLocators.length; i++) {
      const locator = refLocators[i].textContent?.trim();
      if (locator === componentPurl) {
        const externalRef = refLocators[i].parentNode;
        if (externalRef) {
          // Use getElementsByTagName instead of querySelector for namespace compatibility
          const refTypeElements = externalRef.getElementsByTagName('referenceType');
          const refTypeEl = refTypeElements.length > 0 ? refTypeElements[0] : null;
          if (refTypeEl?.textContent?.trim() === 'purl') {
            let node = externalRef.parentNode;
            while (node && (node.localName || node.nodeName) !== 'packages') {
              node = node.parentNode;
            }
            if (node) {
              console.log('[findComponentInXml] Found matching SPDX package');
              const result = createXmlElementNode(node, 'Document');
              console.log('[findComponentInXml] Created node:', result);
              return result;
            }
          }
        }
      }
    }

    console.log('[findComponentInXml] Component not found');
    return null;
  } catch (e) {
    console.error('[findComponentInXml] Error:', e);
    return null;
  }
};

const createAttributeNode = (attr, parentPath) => ({
  id: `${parentPath}@${attr.name}`,
  name: `@${attr.name}`,
  value: attr.value,
});

export const expandXmlChildren = (xmlNode, parentPath) => {
  const children = [];
  // Track element indices for generating unique node IDs like component[0], component[1]
  const elementNameCounts = {};
  let textIndex = 0;

  if (xmlNode.attributes) {
    for (let i = 0; i < xmlNode.attributes.length; i++) {
      children.push(createAttributeNode(xmlNode.attributes[i], parentPath));
    }
  }

  // Create child nodes with intelligent naming
  for (let i = 0; i < xmlNode.childNodes.length; i++) {
    const child = xmlNode.childNodes[i];

    if (child.nodeType === Node.TEXT_NODE) {
      const text = child.textContent?.trim();
      if (text) {
        children.push(createXmlTextNode(parentPath, text, textIndex++));
      }
    } else if (child.nodeType === Node.ELEMENT_NODE) {
      const elementName = child.localName || child.nodeName;
      elementNameCounts[elementName] = (elementNameCounts[elementName] || 0) + 1;
      const elementIndex = elementNameCounts[elementName] - 1;
      const node = createXmlElementNode(child, parentPath, elementIndex);
      children.push(node);
    }
  }

  // Sort children by priority order
  const priorityOrder = ['version', 'purl', 'name', 'bom-ref', 'SPDXID', 'spdxId', 'id', 'type', 'ref'];
  children.sort((a, b) => {
    const aName = a.name.replace(/^@/, ''); // Remove @ prefix for attributes
    const bName = b.name.replace(/^@/, '');
    const aIndex = priorityOrder.indexOf(aName);
    const bIndex = priorityOrder.indexOf(bName);

    // If both are in priority list, sort by priority
    if (aIndex !== -1 && bIndex !== -1) {
      return aIndex - bIndex;
    }
    // If only a is in priority list, it comes first
    if (aIndex !== -1) return -1;
    // If only b is in priority list, it comes first
    if (bIndex !== -1) return 1;
    // Neither in priority list, maintain original order
    return 0;
  });

  return children;
};

export const getXmlTotalDescendants = (node) => {
  if (!node || !node.xmlNode) {
    return 0;
  }
  // Count all descendant elements using getElementsByTagName('*')
  const descendants = node.xmlNode.getElementsByTagName('*');
  return descendants.length;
};

export const generateXmlPreview = (xmlNode) => {
  if (!xmlNode || xmlNode.nodeType !== 1) {
    return xmlNode?.textContent?.trim() || '';
  }

  try {
    const MAX_ITEMS = 3;
    const previewParts = [];
    let count = 0;

    if (xmlNode.attributes) {
      for (let i = 0; i < xmlNode.attributes.length && count < MAX_ITEMS; i++) {
        previewParts.push(`@${xmlNode.attributes[i].name}: ${JSON.stringify(xmlNode.attributes[i].value)}`);
        count++;
      }
    }

    // Use childNodes and filter for ELEMENT_NODE since children might not be available in JSDOM for XML
    if (xmlNode.childNodes && count < MAX_ITEMS) {
      for (let i = 0; i < xmlNode.childNodes.length && count < MAX_ITEMS; i++) {
        const child = xmlNode.childNodes[i];
        if (child.nodeType === Node.ELEMENT_NODE) {
          const childName = child.localName || child.nodeName;
          previewParts.push(`${childName}: {…}`);
          count++;
        }
      }
    }

    // Calculate hasMore based on total items, not collected count
    const totalItems = (xmlNode.attributes?.length || 0) + (xmlNode.children?.length || 0);
    const hasMore = totalItems > MAX_ITEMS;
    const displayParts = previewParts.slice(0, MAX_ITEMS);
    return hasMore ? `${displayParts.join(', ')}, …` : displayParts.join(', ');
  } catch (e) {
    console.error('Error generating XML preview:', e);
    return xmlNode.nodeName || '';
  }
};
