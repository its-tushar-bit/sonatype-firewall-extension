/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import reducer, { TREE_NODE_STATUS } from 'MainRoot/OrgsAndPolicies/ownersTreeSlice';

describe('ownersTreeSlice reducers', () => {
  describe('toogleTreeNode', () => {
    it('toogle a node collapsed/expanded status', () => {
      const state = Object.freeze({ nodesStatus: { sonatype: true } });

      const { nodesStatus } = reducer(state, {
        type: 'ownersTree/toogleTreeNode',
        payload: { ownerId: 'sonatype' },
      });

      expect(nodesStatus.sonatype).toBe(false);
    });
  });

  describe('expandAllTreeNodes', () => {
    it('expand all nodes', () => {
      const state = Object.freeze({ nodesStatus: { nexus: false, sonatype: true } });

      const { nodesStatus, initialStatus } = reducer(state, { type: 'ownersTree/expandAllTreeNodes' });

      expect(nodesStatus).toEqual({});
      expect(initialStatus).toEqual(TREE_NODE_STATUS.expanded);
    });
  });

  describe('collapseAllTreeNodes', () => {
    it('collapse all nodes', () => {
      const state = Object.freeze({ nodesStatus: { nexus: false, sonatype: true } });

      const { nodesStatus, initialStatus } = reducer(state, { type: 'ownersTree/collapseAllTreeNodes' });

      expect(nodesStatus).toEqual({});
      expect(initialStatus).toEqual(TREE_NODE_STATUS.collapsed);
    });
  });
});
