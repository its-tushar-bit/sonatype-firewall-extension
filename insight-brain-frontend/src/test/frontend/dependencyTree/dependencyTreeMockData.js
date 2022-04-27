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
        isOpen: true,
        treePath: [0, 'children', 0],
        originalTreePath: [0, 'children', 0],
        hash: 'qwert32145',
        policyThreatLevel: 10,
      },
    ],
    isOpen: false,
    treePath: [0],
    originalTreePath: [0],
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
        originalTreePath: [1, 'children', 0],
        hash: 'qwert321432',
        policyThreatLevel: 6,
      },
    ],
    isOpen: false,
    treePath: [1],
    originalTreePath: [1],
    hash: 'qwert32143',
    policyThreatLevel: 3,
  },
  {
    displayName: 'axis : axis : 1.2',
    children: null,
    isOpen: true,
    treePath: [2],
    originalTreePath: [2],
    hash: 'qwert98',
    policyThreatLevel: 2,
  },
  {
    displayName: 'taglibs : standard : 1.1.2',
    children: null,
    isOpen: true,
    treePath: [3],
    originalTreePath: [3],
    hash: 'qwert56',
    policyThreatLevel: 10,
  },
];

export const flatDependencyTreeData = [
  {
    displayName: 'org.apache.commons : commons-lang3 : 3.3.2',
    children: [],
    isOpen: false,
    treePath: [0],
    originalTreePath: [0],
    hash: 'qwert3214',
    policyThreatLevel: 1,
  },
  {
    displayName: 'net.sourceforge.jtds : jtds : 1.2.2',
    children: null,
    isOpen: false,
    treePath: [1],
    originalTreePath: [1],
    hash: 'qwert32143',
    policyThreatLevel: 3,
  },
  {
    displayName: 'axis : axis : 1.2',
    children: null,
    isOpen: true,
    treePath: [2],
    originalTreePath: [2],
    hash: 'qwert98',
    policyThreatLevel: 2,
  },
  {
    displayName: 'taglibs : standard : 1.1.2',
    children: null,
    isOpen: true,
    treePath: [3],
    originalTreePath: [3],
    hash: 'qwert56',
    policyThreatLevel: 10,
  },
];

export const unextendedDependencyTreeData = {
  children: [
    {
      componentIdentifier: {
        format: 'maven',
        coordinates: {
          artifactId: 'wsdl4j',
          classifier: '',
          extension: 'jar',
          groupId: 'wsdl4j',
          version: '1.5.1',
        },
      },
    },
    {
      componentIdentifier: {
        format: 'maven',
        coordinates: {
          artifactId: 'standard',
          classifier: '',
          extension: 'jar',
          groupId: 'apache-taglibs',
          version: '1.1.2',
        },
      },
    },
    {
      componentIdentifier: {
        format: 'maven',
        coordinates: {
          artifactId: 'commons-discovery',
          classifier: '',
          extension: 'jar',
          groupId: 'commons-discovery',
          version: '0.2',
        },
      },
      children: [
        {
          componentIdentifier: {
            format: 'maven',
            coordinates: {
              artifactId: 'commons-logging',
              classifier: '',
              extension: 'jar',
              groupId: 'commons-logging',
              version: '1.1.3',
            },
          },
        },
      ],
    },
  ],
};

export const indexedEntries = {
  'maven:artifactId\u001fstandard\u001eclassifier\u001f\u001eextension\u001fjar\u001egroupId\u001fapache-taglibs\u001eversion\u001f1.1.2': {
    hash: 'a17e8a4d9a1f7fcc5eed',
    policyThreatLevel: 9,
    derivedComponentName: 'apache-taglibs : standard : 1.1.2',
    innerSource: false,
    directDependency: true,
  },
  'maven:artifactId\u001fcommons-discovery\u001eclassifier\u001f\u001eextension\u001fjar\u001egroupId\u001fcommons-discovery\u001eversion\u001f0.2': {
    hash: '7773ac7a7248f08ed2b8',
    policyThreatLevel: 1,
    derivedComponentName: 'commons-discovery : commons-discovery : 0.2',
    innerSource: false,
    directDependency: true,
  },
  'maven:artifactId\u001fcommons-logging\u001eclassifier\u001f\u001eextension\u001fjar\u001egroupId\u001fcommons-logging\u001eversion\u001f1.1.3': {
    hash: 'f6f66e966c70a83ffbdb',
    policyThreatLevel: 1,
    derivedComponentName: 'commons-logging : commons-logging : 1.1.3',
    innerSource: false,
    directDependency: true,
  },
  'maven:artifactId\u001fwsdl4j\u001eclassifier\u001f\u001eextension\u001fjar\u001egroupId\u001fwsdl4j\u001eversion\u001f1.5.1': {
    hash: 'bd804633b9c2cf062586',
    policyThreatLevel: 2,
    derivedComponentName: 'wsdl4j : wsdl4j : 1.5.1',
    innerSource: false,
    directDependency: true,
  },
};
