/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { extendDependencyTreeData } from 'MainRoot/DependencyTree/dependencyTreeUtil';
import { unextendedDependencyTreeData, indexedEntries } from './dependencyTreeMockData';

describe('dependencyTreeUtil', () => {
  it('extends and sort dependency tree raw data', () => {
    const output = extendDependencyTreeData(unextendedDependencyTreeData, indexedEntries);
    expect(output).toEqual([
      jasmine.objectContaining({ hash: 'a17e8a4d9a1f7fcc5eed', displayName: 'apache-taglibs : standard : 1.1.2' }),
      jasmine.objectContaining({ hash: 'bd804633b9c2cf062586', displayName: 'wsdl4j : wsdl4j : 1.5.1' }),
      jasmine.objectContaining({
        hash: '7773ac7a7248f08ed2b8',
        displayName: 'commons-discovery : commons-discovery : 0.2',
        children: [
          jasmine.objectContaining({
            hash: 'f6f66e966c70a83ffbdb',
            displayName: 'commons-logging : commons-logging : 1.1.3',
          }),
        ],
      }),
    ]);
  });
});
