/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

export const isXmlContent = (content) => {
  if (typeof content !== 'string') return false;
  const trimmed = content.trim();
  return trimmed.startsWith('<');
};

const createXmlElementNode = (node, path, elementIndex = null) => {
  const nodePath =
    elementIndex !== null
      ? `${path}.${node.nodeName}[${elementIndex}]`
      : path
      ? `${path}.${node.nodeName}`
      : node.nodeName;

  // Check if this is a simple text node (no attributes, single text child)
  const hasAttributes = node.attributes && node.attributes.length > 0;
  const isSingleTextNode =
    !hasAttributes && node.childNodes.length === 1 && node.childNodes[0].nodeType === Node.TEXT_NODE;

  if (isSingleTextNode) {
    return {
      id: nodePath,
      name: node.nodeName,
      value: node.childNodes[0].textContent?.trim(),
      xmlNode: null,
      preview: null,
    };
  }

  return {
    id: nodePath,
    name: node.nodeName,
    value: null,
    xmlNode: node,
    preview: generateXmlPreview(node),
  };
};

const createXmlTextNode = (path, text, index) => ({
  id: `${path}_text_${index}`,
  name: text,
  value: null,
  xmlNode: null,
});

export const parseXmlToTree = (xmlString) => {
  try {
    const parser = new DOMParser();
    const xmlDoc = parser.parseFromString(xmlString, 'text/xml');
    const root = createXmlElementNode(xmlDoc.documentElement, '');
    return root ? [root] : [];
  } catch (e) {
    console.error('Error parsing XML:', e);
    return [];
  }
};

// Find and extract a specific component by PURL from SBOM XML
// Supports both CycloneDX (component tags) and SPDX (packages tags)
export const findComponentInXml = (xmlDoc, componentPurl) => {
  if (!componentPurl || !xmlDoc) return null;

  try {
    // Search for component elements (CycloneDX format) - check purl child element
    const purlElements = xmlDoc.getElementsByTagName('purl');
    for (let i = 0; i < purlElements.length; i++) {
      const purl = purlElements[i].textContent?.trim();
      if (purl === componentPurl && purlElements[i].parentNode?.nodeName === 'component') {
        return createXmlElementNode(purlElements[i].parentNode, '');
      }
    }

    // Search for packages elements (SPDX format) - check externalRefs/referenceLocator
    const refLocators = xmlDoc.getElementsByTagName('referenceLocator');
    for (let i = 0; i < refLocators.length; i++) {
      const locator = refLocators[i].textContent?.trim();
      if (locator === componentPurl) {
        // Verify this is a purl type reference
        const externalRef = refLocators[i].parentNode;
        if (externalRef) {
          const refTypeEl = externalRef.querySelector('referenceType');
          if (refTypeEl?.textContent?.trim() === 'purl') {
            // Navigate up to find the package element
            let node = externalRef.parentNode;
            while (node && node.nodeName !== 'packages') {
              node = node.parentNode;
            }
            if (node) {
              return createXmlElementNode(node, '');
            }
          }
        }
      }
    }

    return null;
  } catch (e) {
    console.error('Error finding component in XML:', e);
    return null;
  }
};

const createAttributeNode = (attr, parentPath) => ({
  id: `${parentPath}@${attr.name}`,
  name: `@${attr.name}`,
  value: attr.value,
  xmlNode: null,
});

export const expandXmlChildren = (xmlNode, parentPath) => {
  const children = [];
  const elementNameCounts = {};
  let textIndex = 0;

  if (xmlNode.attributes) {
    for (let i = 0; i < xmlNode.attributes.length; i++) {
      children.push(createAttributeNode(xmlNode.attributes[i], parentPath));
    }
  }

  for (let i = 0; i < xmlNode.childNodes.length; i++) {
    const child = xmlNode.childNodes[i];

    if (child.nodeType === Node.TEXT_NODE) {
      const text = child.textContent?.trim();
      if (text) {
        children.push(createXmlTextNode(parentPath, text, textIndex++));
      }
    } else if (child.nodeType === Node.ELEMENT_NODE) {
      const elementName = child.nodeName;
      elementNameCounts[elementName] = (elementNameCounts[elementName] || 0) + 1;
      const elementIndex = elementNameCounts[elementName] - 1;
      children.push(createXmlElementNode(child, parentPath, elementIndex));
    }
  }

  return children;
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

    if (xmlNode.children && count < MAX_ITEMS) {
      for (let i = 0; i < xmlNode.children.length && count < MAX_ITEMS; i++) {
        previewParts.push(`${xmlNode.children[i].nodeName}: {…}`);
        count++;
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
