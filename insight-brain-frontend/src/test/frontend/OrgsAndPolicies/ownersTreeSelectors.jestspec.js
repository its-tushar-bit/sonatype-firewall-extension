/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  selectOwnersTreeSlice,
  selectOwnersTreeNodesStatus,
  selectOwnersTreeNodesInitialStatus,
  selectIsOwnerNodeExpanded,
  selectShouldDisplayRepositories,
} from 'MainRoot/OrgsAndPolicies/ownersTreeSelectors';
import { selectOrgsAndPoliciesSlice } from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesSelectors';
import { TREE_NODE_STATUS as STATUS } from 'MainRoot/OrgsAndPolicies/ownersTreeSlice';
import {
  selectIsOrganizationTopOfHierarchyForUser,
  selectShowRepositories,
} from 'MainRoot/OrgsAndPolicies/ownerSideNav/ownerSideNavSelectors';
import { selectIsRepositoriesRelated } from 'MainRoot/reduxUiRouter/routerSelectors';

describe('ownersTreeSelectors', () => {
  describe('selectOwnersTreeSlice', () => {
    it('is composed from the following selector', () => {
      expect(selectOwnersTreeSlice.dependencies).toEqual([selectOrgsAndPoliciesSlice]);
    });

    it('selects ownerSummary', () => {
      const orgsAndPoliciesSlice = { ownersTree: null };

      const actualSelection = selectOwnersTreeSlice.resultFunc(orgsAndPoliciesSlice);

      expect(actualSelection).toBe(null);
    });
  });

  describe('selectOwnersTreeNodesStatus', () => {
    it('is composed from the following selector', () => {
      expect(selectOwnersTreeNodesStatus.dependencies).toEqual([selectOwnersTreeSlice]);
    });

    it('selects ownersTreeNodesStatus without searchTerm', () => {
      const slice = { nodesStatus: { sonatype: false }, filteredNodesStatus: { sonatype: true }, searchTerm: '' };

      const actualSelection = selectOwnersTreeNodesStatus.resultFunc(slice);

      expect(actualSelection).toEqual({ sonatype: false });
    });

    it('selects ownersTreeNodesStatus without searchTerm', () => {
      const slice = { nodesStatus: { sonatype: false }, filteredNodesStatus: { sonatype: true }, searchTerm: 'some' };

      const actualSelection = selectOwnersTreeNodesStatus.resultFunc(slice);

      expect(actualSelection).toEqual({ sonatype: true });
    });
  });

  describe('selectOwnersTreeNodesInitialStatus', () => {
    it('is composed from the following selector', () => {
      expect(selectOwnersTreeNodesInitialStatus.dependencies).toEqual([selectOwnersTreeSlice]);
    });

    it('selects initialStatus without searchTerm', () => {
      const slice = { initialStatus: STATUS.collapsed, initialFilteredStatus: STATUS.expanded, searchTerm: '' };

      const actualSelection = selectOwnersTreeNodesInitialStatus.resultFunc(slice);

      expect(actualSelection).toEqual(STATUS.collapsed);
    });

    it('selects initialStatus with searchTerm', () => {
      const slice = {
        initialStatus: STATUS.collapsed,
        initialFilteredStatus: STATUS.expanded,
        searchTerm: 'some term',
      };

      const actualSelection = selectOwnersTreeNodesInitialStatus.resultFunc(slice);

      expect(actualSelection).toEqual(STATUS.expanded);
    });
  });

  describe('selectIsOwnerNodeExpanded', () => {
    it('is composed from the following selector', () => {
      expect(selectIsOwnerNodeExpanded.dependencies).toEqual([
        selectOwnersTreeNodesStatus,
        selectOwnersTreeNodesInitialStatus,
        expect.any(Function),
      ]);
    });

    it('selects node expanded/collapsed status', () => {
      const status = { sonatype: false, lifecycle: true, nexus: undefined };

      const isSonatypeExpanded = selectIsOwnerNodeExpanded.resultFunc(status, STATUS.expanded, 'sonatype');
      const isLifecycleExpanded = selectIsOwnerNodeExpanded.resultFunc(status, STATUS.expanded, 'lifecycle');
      const isNexusExpanded = selectIsOwnerNodeExpanded.resultFunc(status, STATUS.expanded, 'nexus');

      expect(isSonatypeExpanded).toBe(false);
      expect(isLifecycleExpanded).toBe(true);
      expect(isNexusExpanded).toBe(true);
    });

    it('returns initial status when expanded/collapsed status is not defined', () => {
      const status = { nexus: undefined, sonatype: null };

      expect(selectIsOwnerNodeExpanded.resultFunc(status, STATUS.expanded, 'nexus')).toBe(STATUS.expanded);
      expect(selectIsOwnerNodeExpanded.resultFunc(status, STATUS.collapsed, 'sonatype')).toBe(STATUS.collapsed);
    });
  });

  describe('selectShouldDisplayRepositories', () => {
    it('is composed from the following selector', () => {
      expect(selectShouldDisplayRepositories.dependencies).toEqual([
        selectShowRepositories,
        selectIsOrganizationTopOfHierarchyForUser,
        selectIsRepositoriesRelated,
      ]);
    });

    it('select the correct value depending on the inputs', () => {
      const doNotShowRepos = selectShouldDisplayRepositories.resultFunc(false, true, true);
      const isNotTopOrgAndIsRepoRelated = selectShouldDisplayRepositories.resultFunc(true, false, true);
      const isTopOrgAndIsNotRepoRelated = selectShouldDisplayRepositories.resultFunc(true, true, false);
      const isNotTopOrgAndIsNotRepoRelated = selectShouldDisplayRepositories.resultFunc(true, false, false);

      expect(doNotShowRepos).toBe(false);
      expect(isNotTopOrgAndIsRepoRelated).toBe(true);
      expect(isTopOrgAndIsNotRepoRelated).toBe(true);
      expect(isNotTopOrgAndIsNotRepoRelated).toBe(false);
    });
  });
});
