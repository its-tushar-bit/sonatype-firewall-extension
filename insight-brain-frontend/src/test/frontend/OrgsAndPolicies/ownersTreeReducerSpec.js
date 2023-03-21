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

  describe('setOwnersTreeSearchTerm', () => {
    const ownersMap = {
      '6thlevelapp': {
        type: 'application',
        id: '6thlevelapp',
        name: '6th level app',
        publicId: '6thlevelapp',
        organizationId: '6thlevelchild',
      },
      '5thlevelapp': {
        type: 'application',
        id: '5thlevelapp',
        name: '4th level app',
        publicId: '5thlevelapp',
        organizationId: '4thlevelchild',
      },
      '5thlevelchild': {
        type: 'organization',
        id: '5thlevelchild',
        name: '5th-level child',
        synthetic: true,
        parentOrganizationId: '4thlevelchild',
        applicationIds: [],
        subOrgs: 1,
        totalApps: 1,
        organizationIds: ['6thlevelchild'],
      },
      '6thlevelchild': {
        type: 'organization',
        id: '6thlevelchild',
        name: '6th level child',
        synthetic: true,
        parentOrganizationId: '5thlevelchild',
        applicationIds: ['6thlevelapp'],
        subOrgs: 0,
        totalApps: 1,
        organizationIds: [],
      },
      '4thlevelchild': {
        type: 'organization',
        id: '4thlevelchild',
        name: '4th-level child',
        synthetic: true,
        parentOrganizationId: 'AboveLevelId',
        applicationIds: ['5thlevelapp'],
        subOrgs: 2,
        totalApps: 2,
        organizationIds: ['5thlevelchild'],
      },
    };

    it('there are no matches', () => {
      const state = Object.freeze({
        searchTerm: '',
        filteredOwners: ['owner1', 'owner2'],
        filteredNodesStatus: { some: true },
        initialFilteredStatus: false,
      });
      const { searchTerm, filteredOwners, filteredNodesStatus, initialFilteredStatus } = reducer(state, {
        type: 'ownersTree/setOwnersTreeSearchTerm',
        payload: { ownersMap, topParentOrganizationId: '4thlevelchild', searchTerm: 'asddsa' },
      });
      expect(searchTerm).toBe('asddsa');
      expect(filteredOwners).toEqual([]);
      expect(filteredNodesStatus).toEqual({});
      expect(initialFilteredStatus).toBe(true);
    });

    it('there are matches', () => {
      const state = Object.freeze({
        searchTerm: '',
        filteredOwners: ['owner1', 'owner2'],
        filteredNodesStatus: { some: true },
        initialFilteredStatus: false,
      });
      const { searchTerm, filteredOwners, filteredNodesStatus, initialFilteredStatus } = reducer(state, {
        type: 'ownersTree/setOwnersTreeSearchTerm',
        payload: { ownersMap, topParentOrganizationId: '4thlevelchild', searchTerm: '5th' },
      });
      expect(searchTerm).toBe('5th');
      expect(filteredOwners).toEqual(['6thlevelapp', '6thlevelchild', '5thlevelchild', '4thlevelchild']);
      expect(filteredNodesStatus).toEqual({});
      expect(initialFilteredStatus).toBe(true);
    });
  });
});
