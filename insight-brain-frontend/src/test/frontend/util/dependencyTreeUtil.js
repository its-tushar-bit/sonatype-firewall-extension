/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
export const dependencyTreeData = [
  {
    displayName: 'org.apache.commons : commons-lang3 : 3.3.2',
    children: [
      {
        displayName: 'taglibs : standard : 1.1.2.hh',
        children: null,
        isOpen: false,
        treePath: [0, 'children', 0],
        hash: 'qwert32145',
        policyThreatLevel: 10,
      },
    ],
    isOpen: true,
    treePath: [0],
    hash: 'qwert3214',
    policyThreatLevel: 1,
  },
  {
    displayName: 'net.sourceforge.jtds : jtds : 1.2.2',
    children: [
      {
        displayName: 'taglibs : standard : 1.1.2.FF',
        children: null,
        isOpen: true,
        treePath: [1, 'children', 0],
        hash: 'qwert321432',
        policyThreatLevel: 6,
      },
    ],
    isOpen: false,
    treePath: [1],
    hash: 'qwert32143',
    policyThreatLevel: 3,
  },
  {
    displayName: 'axis : axis : 1.2',
    children: null,
    isOpen: true,
    treePath: [2],
    hash: 'qwert98',
    policyThreatLevel: 2,
  },
  {
    displayName: 'taglibs : standard : 1.1.2',
    children: null,
    isOpen: true,
    treePath: [3],
    hash: 'qwert56',
    policyThreatLevel: 10,
  },
];
