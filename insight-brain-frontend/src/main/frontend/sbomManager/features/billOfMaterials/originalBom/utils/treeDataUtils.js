/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { expandJsonChildren } from './jsonTreeUtils';
import { isXmlContent, parseXmlToTree, expandXmlChildren } from './xmlTreeUtils';

export const processRawDataToTree = (data) => {
  if (typeof data === 'string' && isXmlContent(data)) {
    const tree = parseXmlToTree(data);
    if (tree.length > 0 && tree[0].xmlNode) {
      tree[0].isOpen = true;
      tree[0].children = expandXmlChildren(tree[0].xmlNode, tree[0].id);
    }
    return { treeData: tree, isXml: true, rawData: data };
  }

  const tree = expandJsonChildren(data, '');
  if (tree.length > 0 && tree[0].rawData) {
    tree[0].isOpen = true;
    tree[0].children = expandJsonChildren(tree[0].rawData, tree[0].id);
  }
  return { treeData: tree, isXml: false, rawData: data };
};
