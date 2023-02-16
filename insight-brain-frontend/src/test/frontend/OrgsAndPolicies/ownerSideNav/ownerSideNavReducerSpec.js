/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import reducer from 'MainRoot/OrgsAndPolicies/ownerSideNav/ownerSideNavSlice';

describe('ownerSideNavSlice reducer', () => {
  describe('ownerSideNav/toggleOrganizationsCollapse', () => {
    it('sets toggleOrganizationsCheck, to false if it was set to true', () => {
      const state = Object.freeze({
        toggleOrganizationsCheck: true,
      });

      const { toggleOrganizationsCheck } = reducer(state, {
        type: 'ownerSideNav/toggleOrganizationsCollapse',
      });

      expect(toggleOrganizationsCheck).toBeFalse();
    });

    it('sets toggleOrganizationsCheck, to true if it was set to false', () => {
      const state = Object.freeze({
        toggleOrganizationsCheck: false,
      });

      const { toggleOrganizationsCheck } = reducer(state, {
        type: 'ownerSideNav/toggleOrganizationsCollapse',
      });

      expect(toggleOrganizationsCheck).toBeTrue();
    });
  });

  describe('ownerSideNav/toggleApplicationsCollapse', () => {
    it('sets toggleApplicationsCollapse, to false if it was set to true', () => {
      const state = Object.freeze({
        toggleApplicationsCheck: true,
      });

      const { toggleApplicationsCheck } = reducer(state, {
        type: 'ownerSideNav/toggleApplicationsCollapse',
      });

      expect(toggleApplicationsCheck).toBeFalse();
    });

    it('sets toggleApplicationsCollapse, to true if it was set to false', () => {
      const state = Object.freeze({
        toggleApplicationsCheck: false,
      });

      const { toggleApplicationsCheck } = reducer(state, {
        type: 'ownerSideNav/toggleApplicationsCollapse',
      });

      expect(toggleApplicationsCheck).toBeTrue();
    });
  });

  describe('CRUD actions', () => {
    let stateMock;
    beforeEach(() => {
      // ownersMap fixture:
      //
      //                 root
      //              /        \
      //          childOrg1    childOrg2
      //            /    \          \
      // grandChildOrg1  app1    grandChildOrg2
      //            |              /      \
      // grandGrandChildOrg1     app4    app6
      //       /    /   \
      //     app2 app3  app5

      const root = {
        id: 'root',
        name: 'root org',
        applicationIds: [],
        organizationIds: ['childOrg1', 'childOrg2'],
        totalApps: 6,
        subOrgs: 5,
      };
      const app1 = { id: 'app1', publicId: 'app1', name: 'app1', organizationId: 'childOrg1' };
      const app2 = { id: 'app2', publicId: 'app2', name: 'app2', organizationId: 'grandGrandChildOrg1' };
      const app3 = { id: 'app3', publicId: 'app3', name: 'app3', organizationId: 'grandGrandChildOrg1' };
      const app4 = { id: 'app4', publicId: 'app4', name: 'z app', organizationId: 'grandChildOrg2' };
      const app5 = { id: 'app5', publicId: 'app5', name: 'm app', organizationId: 'grandGrandChildOrg1' };
      const app6 = { id: 'app6', publicId: 'app6', name: 'a app', organizationId: 'grandChildOrg2' };
      const childOrg1 = {
        id: 'childOrg1',
        name: 'childOrg1',
        parentOrganizationId: 'root',
        organizationIds: ['grandChildOrg1'],
        applicationIds: ['app1'],
        totalApps: 4,
        subOrgs: 2,
      };
      const grandChildOrg1 = {
        id: 'grandChildOrg1',
        name: 'grandChildOrg1',
        parentOrganizationId: 'childOrg1',
        organizationIds: ['grandGrandChildOrg1'],
        applicationIds: [],
        totalApps: 3,
        subOrgs: 1,
      };
      const grandGrandChildOrg1 = {
        id: 'grandGrandChildOrg1',
        name: 'grandGrandChildOrg1',
        parentOrganizationId: 'grandChildOrg1',
        applicationIds: ['app2', 'app3', 'app5'],
        organizationIds: [],
        totalApps: 3,
        subOrgs: 0,
      };
      const childOrg2 = {
        id: 'childOrg2',
        name: 'childOrg2',
        parentOrganizationId: 'root',
        organizationIds: ['grandChildOrg2'],
        applicationIds: [],
        totalApps: 2,
        subOrgs: 1,
      };
      const grandChildOrg2 = {
        id: 'grandChildOrg2',
        name: 'New parent',
        parentOrganizationId: 'childOrg2',
        applicationIds: ['app4', 'app6'],
        totalApps: 2,
        subOrgs: 0,
      };
      const ownersMap = {
        root,
        app1,
        app2,
        app3,
        app4,
        app5,
        app6,
        childOrg1,
        childOrg2,
        grandChildOrg1,
        grandChildOrg2,
        grandGrandChildOrg1,
      };

      stateMock = Object.freeze({
        ownersMap,
        topParentOrganizationId: 'root',
        displayedOrganization: grandGrandChildOrg1,
        flattenEntries: {
          organizations: [root, childOrg1, childOrg2, grandChildOrg1, grandChildOrg2, grandGrandChildOrg1],
          applications: [app1, app2, app3, app4, app5, app6],
        },
        filteredEntries: {
          organizations: [],
          applications: [],
        },
      });
    });

    describe('ownerSideNav/removeOrganizationFromOwnerHierarchy', () => {
      it('removes organization and its children from owners map', () => {
        const { ownersMap } = reducer(stateMock, {
          type: 'ownerSideNav/removeOrganizationFromOwnerHierarchy',
          payload: 'grandChildOrg1',
        });

        expect(ownersMap.grandChildOrg1).not.toBeDefined();
        expect(ownersMap.grandGrandChildOrg1).not.toBeDefined();
        expect(ownersMap.app2).not.toBeDefined();
        expect(ownersMap.app3).not.toBeDefined();
        expect(ownersMap.childOrg1.organizationIds.length).toBe(0);
      });

      it('updates flattenEntries and counters', () => {
        const { ownersMap, flattenEntries } = reducer(stateMock, {
          type: 'ownerSideNav/removeOrganizationFromOwnerHierarchy',
          payload: 'grandChildOrg1',
        });

        expect(flattenEntries.organizations.length).toBe(4);
        expect(flattenEntries.organizations).toEqual(
          jasmine.arrayContaining([
            jasmine.objectContaining({ id: 'root' }),
            jasmine.objectContaining({ id: 'childOrg1' }),
            jasmine.objectContaining({ id: 'childOrg2' }),
            jasmine.objectContaining({ id: 'grandChildOrg2' }),
          ])
        );
        expect(flattenEntries.applications.length).toBe(3);
        expect(flattenEntries.applications).toEqual([
          jasmine.objectContaining({ id: 'app1' }),
          jasmine.objectContaining({ id: 'app4' }),
          jasmine.objectContaining({ id: 'app6' }),
        ]);

        expect(ownersMap.childOrg1.totalApps).toBe(1);
        expect(ownersMap.childOrg1.subOrgs).toBe(0);

        expect(ownersMap.grandChildOrg2.totalApps).toBe(2);
        expect(ownersMap.grandChildOrg2.subOrgs).toBe(0);

        expect(ownersMap.childOrg2.totalApps).toBe(2);
        expect(ownersMap.childOrg2.subOrgs).toBe(1);

        expect(ownersMap.root.totalApps).toBe(3);
        expect(ownersMap.root.subOrgs).toBe(3);
      });
    });

    describe('ownerSideNav/removeApplicationFromOwnerHierarchy', () => {
      it('removes application from owners map', () => {
        const state = Object.freeze({
          ownersMap: {
            root: { id: 'root', name: 'root org', applicationIds: ['nexus'] },
            nexus: { id: 'nexus', name: 'nexus', organizationId: 'root' },
          },
          flattenEntries: {
            organizations: [],
            applications: [],
          },
        });

        const { ownersMap } = reducer(state, {
          type: 'ownerSideNav/removeApplicationFromOwnerHierarchy',
          payload: 'nexus',
        });

        expect(ownersMap.nexus).not.toBeDefined();
        expect(ownersMap.root.applicationIds.length).toBe(0);
      });

      it('updates flattenEntries and counters', () => {
        const { ownersMap, flattenEntries } = reducer(stateMock, {
          type: 'ownerSideNav/removeApplicationFromOwnerHierarchy',
          payload: 'app2',
        });

        expect(flattenEntries.organizations.length).toBe(6);
        expect(flattenEntries.applications.length).toBe(5);
        expect(flattenEntries.applications).toEqual([
          jasmine.objectContaining({ id: 'app1' }),
          jasmine.objectContaining({ id: 'app3' }),
          jasmine.objectContaining({ id: 'app4' }),
          jasmine.objectContaining({ id: 'app5' }),
          jasmine.objectContaining({ id: 'app6' }),
        ]);

        expect(ownersMap.grandGrandChildOrg1.applicationIds.length).toBe(2);
        expect(ownersMap.grandGrandChildOrg1.totalApps).toBe(2);

        expect(ownersMap.grandChildOrg1.applicationIds.length).toBe(0);
        expect(ownersMap.grandChildOrg1.totalApps).toBe(2);

        expect(ownersMap.childOrg1.applicationIds.length).toBe(1);
        expect(ownersMap.childOrg1.totalApps).toBe(3);

        expect(ownersMap.grandChildOrg2.applicationIds.length).toBe(2);
        expect(ownersMap.grandChildOrg2.totalApps).toBe(2);

        expect(ownersMap.childOrg2.applicationIds.length).toBe(0);
        expect(ownersMap.childOrg2.totalApps).toBe(2);

        expect(ownersMap.root.applicationIds.length).toBe(0);
        expect(ownersMap.root.totalApps).toBe(5);
      });
    });

    describe('ownerSideNav/moveApplication', () => {
      it('updates app counters in the whole hierarchy', () => {
        const { ownersMap } = reducer(stateMock, {
          type: 'ownerSideNav/moveApplication',
          payload: {
            currentOwner: {
              id: 'app5',
              publicId: 'app5',
              name: 'm app',
              organizationId: 'grandGrandChildOrg1',
              organizationName: 'grandGrandChildOrg1',
              contact: null,
            },
            newParentId: 'grandChildOrg2',
          },
        });

        expect(ownersMap.grandGrandChildOrg1.applicationIds.length).toBe(2);
        expect(ownersMap.grandGrandChildOrg1.totalApps).toBe(2);

        expect(ownersMap.grandChildOrg1.applicationIds.length).toBe(0);
        expect(ownersMap.grandChildOrg1.totalApps).toBe(2);

        expect(ownersMap.childOrg1.applicationIds.length).toBe(1);
        expect(ownersMap.childOrg1.totalApps).toBe(3);

        expect(ownersMap.grandChildOrg2.applicationIds.length).toBe(3);
        expect(ownersMap.grandChildOrg2.totalApps).toBe(3);

        expect(ownersMap.childOrg2.applicationIds.length).toBe(0);
        expect(ownersMap.childOrg2.totalApps).toBe(3);

        expect(ownersMap.root.applicationIds.length).toBe(0);
        expect(ownersMap.root.totalApps).toBe(6);

        expect(ownersMap.grandChildOrg2.applicationIds).toEqual(['app6', 'app5', 'app4']);
      });
    });

    describe('ownerSideNav/updateOwnersMapWithNewEntry', () => {
      it('updates app flattenEntries and app counters', () => {
        const { ownersMap, flattenEntries } = reducer(stateMock, {
          type: 'ownerSideNav/updateOwnersMapWithNewEntry',
          payload: {
            entry: {
              id: 'newApp',
              publicId: 'newApp',
              organizationId: 'grandGrandChildOrg1',
              name: '1 New First App',
            },
            isApp: true,
          },
        });

        expect(flattenEntries.applications.length).toBe(7);
        expect(flattenEntries.applications).toEqual([
          jasmine.objectContaining({ id: 'app1' }),
          jasmine.objectContaining({ id: 'app2' }),
          jasmine.objectContaining({ id: 'app3' }),
          jasmine.objectContaining({ id: 'app4' }),
          jasmine.objectContaining({ id: 'app5' }),
          jasmine.objectContaining({ id: 'app6' }),
          jasmine.objectContaining({ id: 'newApp' }),
        ]);

        expect(ownersMap.grandGrandChildOrg1.applicationIds.length).toBe(4);
        expect(ownersMap.grandGrandChildOrg1.totalApps).toBe(4);
        expect(ownersMap.grandGrandChildOrg1.applicationIds).toEqual(['newApp', 'app2', 'app3', 'app5']);

        const createdApp = ownersMap['newApp'];
        expect(createdApp).not.toBeNull();
        expect(createdApp.id).toBe('newApp');
        expect(createdApp.publicId).toBe('newApp');
        expect(createdApp.organizationId).toBe('grandGrandChildOrg1');
        expect(createdApp.name).toBe('1 New First App');
        expect(createdApp.type).toBe('application');

        expect(ownersMap.grandChildOrg1.applicationIds.length).toBe(0);
        expect(ownersMap.grandChildOrg1.totalApps).toBe(4);

        expect(ownersMap.childOrg1.applicationIds.length).toBe(1);
        expect(ownersMap.childOrg1.totalApps).toBe(5);

        expect(ownersMap.grandChildOrg2.applicationIds.length).toBe(2);
        expect(ownersMap.grandChildOrg2.totalApps).toBe(2);

        expect(ownersMap.childOrg2.applicationIds.length).toBe(0);
        expect(ownersMap.childOrg2.totalApps).toBe(2);

        expect(ownersMap.root.applicationIds.length).toBe(0);
        expect(ownersMap.root.totalApps).toBe(7);
      });

      it('updates org flattenEntries and org counters', () => {
        const state = { ...stateMock, displayedOrganization: stateMock.ownersMap.root };
        const { ownersMap, flattenEntries } = reducer(state, {
          type: 'ownerSideNav/updateOwnersMapWithNewEntry',
          payload: {
            entry: {
              id: 'newOrg',
              name: '1 First New Org',
              parentOrganizationId: 'root',
            },
            isApp: false,
          },
        });

        expect(flattenEntries.organizations.length).toBe(7);
        expect(flattenEntries.organizations).toEqual(
          jasmine.arrayContaining([jasmine.objectContaining({ id: 'newOrg' })])
        );

        const createdOrg = ownersMap['newOrg'];
        expect(createdOrg).not.toBeNull();
        expect(createdOrg.id).toBe('newOrg');
        expect(createdOrg.name).toBe('1 First New Org');
        expect(createdOrg.parentOrganizationId).toBe('root');
        expect(createdOrg.type).toBe('organization');

        expect(ownersMap.grandGrandChildOrg1.organizationIds.length).toBe(0);
        expect(ownersMap.grandGrandChildOrg1.totalApps).toBe(3);
        expect(ownersMap.grandGrandChildOrg1.subOrgs).toBe(0);

        expect(ownersMap.grandChildOrg1.applicationIds.length).toBe(0);
        expect(ownersMap.grandChildOrg1.totalApps).toBe(3);
        expect(ownersMap.grandChildOrg1.subOrgs).toBe(1);

        expect(ownersMap.childOrg1.totalApps).toBe(4);
        expect(ownersMap.childOrg1.subOrgs).toBe(2);

        expect(ownersMap.grandChildOrg2.totalApps).toBe(2);
        expect(ownersMap.grandChildOrg2.subOrgs).toBe(0);

        expect(ownersMap.childOrg2.totalApps).toBe(2);
        expect(ownersMap.childOrg2.subOrgs).toBe(1);

        expect(ownersMap.root.applicationIds.length).toBe(0);
        expect(ownersMap.root.totalApps).toBe(6);
        expect(ownersMap.root.subOrgs).toBe(6);
        expect(ownersMap.root.organizationIds).toEqual(['newOrg', 'childOrg1', 'childOrg2']);
      });
    });
  });
});
