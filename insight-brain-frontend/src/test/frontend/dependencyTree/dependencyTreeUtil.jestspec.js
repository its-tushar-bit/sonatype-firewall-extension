/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import {
  deepReduce,
  extendDependencyTreeData,
  filterDependencyTreeBySearchTerm,
  flattenModuleDirectDependencies,
  getDependencyTreeSubset,
  isFlatDependencyTree,
  renderDisplayName,
} from 'MainRoot/DependencyTree/dependencyTreeUtil';
import {
  dependencyTreeData,
  unextendedDependencyTreeData,
  indexedEntries,
  flatDependencyTreeData,
} from './dependencyTreeMockData';

describe('dependencyTreeUtil', () => {
  it('extends and sort dependency tree raw data', () => {
    const output = extendDependencyTreeData(unextendedDependencyTreeData, indexedEntries);
    expect(output).toEqual([
      expect.objectContaining({ hash: 'a17e8a4d9a1f7fcc5eed', displayName: 'apache-taglibs : standard : 1.1.2' }),
      expect.objectContaining({ hash: 'bd804633b9c2cf062586', displayName: 'wsdl4j : wsdl4j : 1.5.1' }),
      expect.objectContaining({
        hash: '7773ac7a7248f08ed2b8',
        displayName: 'commons-discovery : commons-discovery : 0.2',
        children: [
          expect.objectContaining({
            hash: 'f6f66e966c70a83ffbdb',
            displayName: 'commons-logging : commons-logging : 1.1.3',
          }),
        ],
      }),
    ]);
  });

  describe('getDependencyTreeSubset', () => {
    it('returns empty subset if no arguments were provided', () => {
      const subset = getDependencyTreeSubset();

      expect(subset.length).toBe(0);
    });

    it('returns empty subset if the dependency could not be found', () => {
      const subset = getDependencyTreeSubset(dependencyTreeData, 'ramdomHash');

      expect(subset.length).toBe(0);
    });

    it('returns subset for a matching dependency', () => {
      const subset = getDependencyTreeSubset(dependencyTreeData, 'qwert32145');

      const [firstDependency] = subset;

      expect(firstDependency.hash).toBe('qwert3214');
      expect(firstDependency.treePath).toEqual([0]);
      expect(firstDependency.children.length).toBe(1);
      expect(firstDependency.isOpen).toBe(true);

      const [firstChildDependency] = firstDependency.children;

      expect(firstChildDependency.hash).toBe('qwert32145');
      expect(firstChildDependency.treePath).toEqual([0, 'children', 0]);
      expect(firstChildDependency.children).toBeNull();
      expect(firstChildDependency.isOpen).toBe(false);
    });

    it('returns subset for a matching direct dependency', () => {
      const subset = getDependencyTreeSubset(dependencyTreeData, 'qwert3214');

      const [firstDependency] = subset;

      expect(firstDependency.hash).toBe('qwert3214');
      expect(firstDependency.treePath).toEqual([0]);
      expect(firstDependency.children.length).toBe(1);
      expect(firstDependency.isOpen).toBe(true);

      const [firstChildDependency] = firstDependency.children;

      expect(firstChildDependency.hash).toBe('qwert32145');
      expect(firstChildDependency.treePath).toEqual([0, 'children', 0]);
      expect(firstChildDependency.children).toBeNull();
      expect(firstChildDependency.isOpen).toBe(true);
    });
  });

  describe('filterDependencyTreeBySearchTerm', () => {
    it('returns an empty list if no arguments were provided', () => {
      const subset = filterDependencyTreeBySearchTerm();

      expect(subset.length).toBe(0);
    });

    it('returns an empty list if there are no results matching the search term', () => {
      const result = filterDependencyTreeBySearchTerm(dependencyTreeData, 'unknown component');

      expect(result.length).toBe(0);
    });

    it('returns list with nodes matching search term', () => {
      const result = filterDependencyTreeBySearchTerm(dependencyTreeData, 'jtds');

      expect(result.length).toBe(1);

      const [firstDependency] = result;

      expect(firstDependency.displayName).toBe('net.sourceforge.jtds : jtds : 1.2.2');
      expect(firstDependency.treePath).toEqual([0]);
      expect(firstDependency.originalTreePath).toEqual([1]);
      expect(firstDependency.children.length).toBe(1);

      const [firstChildDependency] = firstDependency.children;

      expect(firstChildDependency.displayName).toBe('taglibs : standard : 1.1.2.FF');
      expect(firstChildDependency.treePath).toEqual([0, 'children', 0]);
      expect(firstChildDependency.originalTreePath).toEqual([1, 'children', 0]);
      expect(firstChildDependency.children).toBeNull();
    });

    it('performs case insensitive search', () => {
      expect(filterDependencyTreeBySearchTerm(dependencyTreeData, 'jtds')).toEqual(
        filterDependencyTreeBySearchTerm(dependencyTreeData, 'JTDS')
      );
    });
  });

  describe('deepReduce', () => {
    it('reduce a tree by applying a reducer function to each node in a tree', () => {
      const callback = jest.fn().mockReturnValue(0);
      deepReduce(callback, 0, dependencyTreeData);

      expect(callback).toHaveBeenCalledTimes(6);
      dependencyTreeData.forEach((node) => {
        if (node.children) node.children.forEach((node) => expect(callback).toHaveBeenCalledWith(0, node));
        expect(callback).toHaveBeenCalledWith(0, node);
      });
    });
  });

  describe('flattenModuleDirectDependencies', () => {
    it('flattens direct module dependencies in the tree', () => {
      const dependencyTree = {
        packageUrl: 'a',
        children: [
          { packageUrl: 'a1', module: true },
          { packageUrl: 'a2', module: true, children: [] },
          { packageUrl: 'a3', module: true, children: [{ packageUrl: 'a31' }, { packageUrl: 'a32' }] },
          { packageUrl: 'a4' },
        ],
      };
      const result = flattenModuleDirectDependencies(dependencyTree);
      expect(result).toEqual({
        packageUrl: 'a',
        children: [{ packageUrl: 'a31' }, { packageUrl: 'a32' }, { packageUrl: 'a4' }],
      });
    });

    it('handles undefined children', () => {
      const dependencyTree = {
        packageUrl: 'a',
      };
      const result = flattenModuleDirectDependencies(dependencyTree);
      expect(result).toEqual({
        packageUrl: 'a',
        children: [],
      });
    });

    it('handles empty children', () => {
      const dependencyTree = {
        packageUrl: 'a',
        children: [],
      };
      const result = flattenModuleDirectDependencies(dependencyTree);
      expect(result).toEqual({
        packageUrl: 'a',
        children: [],
      });
    });
  });

  describe('isFlatDependencyTree', () => {
    it('returns true if its a tree with only one level', () => {
      expect(isFlatDependencyTree(flatDependencyTreeData)).toBe(true);
    });

    it('returns true if its an empty tree', () => {
      expect(isFlatDependencyTree([])).toBe(true);
      expect(isFlatDependencyTree(null)).toBe(true);
      expect(isFlatDependencyTree(undefined)).toBe(true);
    });

    it('returns false if its a tree with more than one level of depth', () => {
      expect(isFlatDependencyTree(dependencyTreeData)).toBe(false);
    });
  });

  describe('renderDisplayName', () => {
    it('returns displayName when there is no searchTerm', () => {
      expect(renderDisplayName('some name')).toBe('some name');
    });

    it('returns displayName when there is no match in the searchTerm', () => {
      expect(renderDisplayName('some name', 'no match')).toEqual(['some name']);
    });

    it('returns formatted displayName when there is a match in the searchTerm', () => {
      expect(renderDisplayName('some name', 'me na', 'classn')).toEqual([
        'so',
        // eslint-disable-next-line react/jsx-key
        <mark key="renderDisplayNameme na0" className="classn">
          me na
        </mark>,
        'me',
      ]);
    });
  });
});
