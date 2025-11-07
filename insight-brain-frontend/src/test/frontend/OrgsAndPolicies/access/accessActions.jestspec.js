/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';

import '../../SpecUtil';
import * as actionsSlice from 'MainRoot/OrgsAndPolicies/access/accessSlice';
import * as accessSelectors from 'MainRoot/OrgsAndPolicies/access/accessSelectors';
import {
  getAccessPageRolesUrl,
  getUsersRoleMappingUrl,
  getCreateOrDeleteAccessUrl,
  getRepositoryContainerRoleMappingUrl,
  getUsersRepositoryRoleMappingUrl,
  getCreateOrDeleteAccessRepositoryUrl,
} from 'MainRoot/util/CLMLocation';
import { SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS } from '@sonatype/react-shared-components';
import * as routerSelectors from 'MainRoot/reduxUiRouter/routerSelectors';

const availableRoles = [
  {
    groupSearchEnabled: true,
    membersByRole: [
      {
        roleId: '2cb71b3468d649789163ea2e212b541e',
        roleName: 'Application Evaluator',
        roleDescription: 'Evaluates applications and views policy violation summary results.',
        membersByOwner: [
          {
            ownerId: 'ROOT_ORGANIZATION_ID',
            ownerName: 'Root Organization',
            ownerType: 'organization',
            members: [],
          },
        ],
      },
      {
        roleId: '90c7c98683b4471cb77a916744540bcc',
        roleName: 'Component Evaluator',
        roleDescription:
          'Evaluates individual components and views policy violation results for a specified application.',
        membersByOwner: [
          {
            ownerId: 'ROOT_ORGANIZATION_ID',
            ownerName: 'Root Organization',
            ownerType: 'organization',
            members: [],
          },
        ],
      },
      {
        roleId: '1da70fae1fd54d6cb7999871ebdb9a36',
        roleName: 'Developer',
        roleDescription: 'Views all information for their assigned organization or application.',
        membersByOwner: [
          {
            ownerId: 'ROOT_ORGANIZATION_ID',
            ownerName: 'Root Organization',
            ownerType: 'organization',
            members: [],
          },
        ],
      },
      {
        roleId: '0df46317c031440795007f4ce9c7f002',
        roleName: 'Legal Reviewer',
        roleDescription: 'Reviews legal obligations for component licenses.',
        membersByOwner: [
          {
            ownerId: 'ROOT_ORGANIZATION_ID',
            ownerName: 'Root Organization',
            ownerType: 'organization',
            members: [],
          },
        ],
      },
      {
        roleId: '1cddabf7fdaa47d6833454af10e0a3ef',
        roleName: 'Owner',
        roleDescription: 'Manages assigned organizations, applications, policies, and policy violations.',
        membersByOwner: [
          {
            ownerId: 'ROOT_ORGANIZATION_ID',
            ownerName: 'Root Organization',
            ownerType: 'organization',
            members: [],
          },
        ],
      },
    ],
    roleDescription: 'Reviews legal obligations for component licenses.',
    roleId: '0df46317c031440795007f4ce9c7f002',
    roleName: 'Legal Reviewer',
  },
];

const role = {
  roleDescription: 'Evaluates individual components and views policy violation results for a specified application.',
  roleId: '90c7c98683b4471cb77a916744540bcc',
  roleName: 'Component Evaluator',
};

const formatedMemberList = [
  {
    displayName: 'Admin BuiltIn',
    email: 'admin@localhost',
    internalName: 'admin',
    realm: 'IQ Server',
    type: 'USER',
  },
];

describe('access', () => {
  const mockAxiosCalls = SpecUtil.axiosMockerGenerator(axios);
  const roleMappingUrl = getAccessPageRolesUrl('application', 'application');
  const fetchUrlOrg = getUsersRoleMappingUrl('application', 'application', 'search');
  const fetchUrlRepository = getUsersRepositoryRoleMappingUrl('search');
  const roleRepositoryMappingUrl = getRepositoryContainerRoleMappingUrl();
  const createOrUpdateRoleUrl = getCreateOrDeleteAccessUrl('application', 'application', role.roleId);
  const createOrUpdateRoleUrlRepository = getCreateOrDeleteAccessRepositoryUrl(role.roleId);
  const removeRoleUrl = getCreateOrDeleteAccessUrl('application', 'application', role.roleId);
  const removeRoleUrlRepository = getCreateOrDeleteAccessRepositoryUrl(role.roleId);

  let loadRoles,
    loadFetchUsers,
    loadRolesIfNeeded,
    createOrUpdateRole,
    goToAddAccess,
    removeRole,
    store,
    state,
    orgAdress = 'management.edit.organization.edit-access',
    repositoryAdress = 'management.edit.repository_container.edit-access';

  beforeEach(function () {
    jest.useFakeTimers();
    loadRoles = actionsSlice.loadRoles;
    loadRolesIfNeeded = actionsSlice.loadRolesIfNeeded;
    loadFetchUsers = actionsSlice.loadFetchUsers;
    createOrUpdateRole = actionsSlice.createOrUpdateRole;
    goToAddAccess = actionsSlice.goToAddAccess;
    removeRole = actionsSlice.removeRole;

    state = {
      router: {
        currentParams: {
          applicationPublicId: 'application',
        },
        currentState: {
          name: '',
        },
      },
    };

    store = SpecUtil.mockReduxStore(state);
  });

  describe('loadRoles under organization', () => {
    beforeEach(function () {
      state.router.currentState.name = orgAdress;
      jest.spyOn(routerSelectors, 'selectIsRepositoriesRelated').mockReturnValue(false);
    });

    it('loading all availableRoles loadRoles/fulfilled action success', (done) => {
      mockAxiosCalls({
        get: {
          [roleMappingUrl]: Promise.resolve({ data: { availableRoles } }),
        },
      });
      store.dispatch(loadRoles()).then(() => {
        const actions = store.getActions();
        expect(actions.length).toBe(2);
        expect(actions).toHaveActionType('access/loadRoles/pending');
        expect(axios.get).toHaveBeenCalledTimes(1);
        expect(axios.get).toHaveBeenCalledWith('/api/v2/roleMemberships/application/application/roles');
        expect(actions).toHaveActionTypesInOrder(['access/loadRoles/pending', 'access/loadRoles/fulfilled']);
        done();
      });
    });

    it('dispatches rejected action if loadRoles request fails', (done) => {
      mockAxiosCalls({
        get: {
          [roleMappingUrl]: () => Promise.reject('something went wrong'),
        },
      });
      store.dispatch(loadRoles()).then(() => {
        const actions = store.getActions();
        expect(axios.get).toHaveBeenCalledTimes(1);
        expect(actions.length).toBe(2);
        expect(axios.get).toHaveBeenCalledWith('/api/v2/roleMemberships/application/application/roles');
        expect(actions).toHaveActionTypesInOrder(['access/loadRoles/pending', 'access/loadRoles/rejected']);
        done();
      });
    });
  });

  describe('loadRoles under repository', () => {
    beforeEach(function () {
      state.router.currentState.name = repositoryAdress;
      jest.spyOn(routerSelectors, 'selectIsRepositoriesRelated').mockReturnValue(true);
    });

    it('loading all availableRoles loadRoles/fulfilled action success', (done) => {
      state.router.currentState.name = repositoryAdress;
      mockAxiosCalls({
        get: {
          [roleRepositoryMappingUrl]: Promise.resolve({ data: { availableRoles } }),
        },
      });
      store.dispatch(loadRoles()).then(() => {
        const actions = store.getActions();
        expect(actions.length).toBe(2);
        expect(actions).toHaveActionType('access/loadRoles/pending');
        expect(axios.get).toHaveBeenCalledTimes(1);
        expect(axios.get).toHaveBeenCalledWith('/api/v2/roleMemberships/repository_container/roles');
        expect(actions).toHaveActionTypesInOrder(['access/loadRoles/pending', 'access/loadRoles/fulfilled']);
        done();
      });
    });

    it('dispatches rejected action if loadRoles request fails', (done) => {
      mockAxiosCalls({
        get: {
          [roleRepositoryMappingUrl]: () => Promise.reject('something went wrong'),
        },
      });
      store.dispatch(loadRoles()).then(() => {
        const actions = store.getActions();
        expect(axios.get).toHaveBeenCalledTimes(1);
        expect(actions.length).toBe(2);
        expect(axios.get).toHaveBeenCalledWith('/api/v2/roleMemberships/repository_container/roles');
        expect(actions).toHaveActionTypesInOrder(['access/loadRoles/pending', 'access/loadRoles/rejected']);
        done();
      });
    });
  });

  describe('loadFetchUsers under organization', () => {
    beforeEach(function () {
      state.router.currentState.name = orgAdress;
    });

    it('fetching all users loadFetchUsers/fulfilled action success', (done) => {
      mockAxiosCalls({
        get: {
          [fetchUrlOrg]: Promise.resolve({ data: {} }),
        },
      });
      store.dispatch(loadFetchUsers('search')).then(() => {
        const actions = store.getActions();
        expect(actions.length).toBe(2);
        expect(actions).toHaveActionType('access/loadFetchUsers/pending');
        expect(axios.get).toHaveBeenCalledTimes(1);
        expect(axios.get).toHaveBeenCalledWith('/rest/user/application/application/query?q=search&groups=true');
        expect(actions).toHaveActionTypesInOrder(['access/loadFetchUsers/pending', 'access/loadFetchUsers/fulfilled']);
        done();
      });
    });

    it('dispatches rejected action if loadFetchUsers request fails', (done) => {
      mockAxiosCalls({
        get: {
          [fetchUrlOrg]: () => Promise.reject('something went wrong'),
        },
      });
      store.dispatch(loadFetchUsers('search')).then(() => {
        const actions = store.getActions();
        expect(axios.get).toHaveBeenCalledTimes(1);
        expect(actions.length).toBe(2);
        expect(axios.get).toHaveBeenCalledWith('/rest/user/application/application/query?q=search&groups=true');
        expect(actions).toHaveActionTypesInOrder(['access/loadFetchUsers/pending', 'access/loadFetchUsers/rejected']);
        done();
      });
    });
  });

  describe('loadFetchUsers under repository', () => {
    beforeEach(function () {
      state.router.currentState.name = repositoryAdress;
    });

    it('fetching all users loadFetchUsers/fulfilled action success', (done) => {
      mockAxiosCalls({
        get: {
          [fetchUrlRepository]: Promise.resolve({ data: {} }),
        },
      });
      store.dispatch(loadFetchUsers('search')).then(() => {
        const actions = store.getActions();
        expect(actions.length).toBe(2);
        expect(actions).toHaveActionType('access/loadFetchUsers/pending');
        expect(axios.get).toHaveBeenCalledTimes(1);
        expect(axios.get).toHaveBeenCalledWith('/rest/user/repository_container/query?q=search');
        expect(actions).toHaveActionTypesInOrder(['access/loadFetchUsers/pending', 'access/loadFetchUsers/fulfilled']);
        done();
      });
    });

    it('dispatches rejected action if loadFetchUsers request fails', (done) => {
      mockAxiosCalls({
        get: {
          [fetchUrlRepository]: () => Promise.reject('something went wrong'),
        },
      });
      store.dispatch(loadFetchUsers('search')).then(() => {
        const actions = store.getActions();
        expect(axios.get).toHaveBeenCalledTimes(1);
        expect(actions.length).toBe(2);
        expect(axios.get).toHaveBeenCalledWith('/rest/user/repository_container/query?q=search');
        expect(actions).toHaveActionTypesInOrder(['access/loadFetchUsers/pending', 'access/loadFetchUsers/rejected']);
        done();
      });
    });
  });

  describe('createOrUpdateRole under organization', () => {
    beforeEach(function () {
      state = {
        router: {
          currentParams: {
            applicationPublicId: 'application',
          },
          currentState: {
            name: orgAdress,
          },
        },
        orgsAndPolicies: {
          access: {
            availableRoles,
            role,
          },
        },
      };

      jest.spyOn(accessSelectors, 'selectUnSortedAddedUsers').mockReturnValue(formatedMemberList);
      jest.spyOn(accessSelectors, 'selectRole').mockReturnValue(role);
    });

    it('create new role createOrUpdateRole/fulfilled action success', (done) => {
      mockAxiosCalls({
        put: {
          [createOrUpdateRoleUrl]: Promise.resolve({ data: {} }),
        },
      });
      store.dispatch(createOrUpdateRole('application', 'application', role.roleId)).then(() => {
        jest.advanceTimersByTime(SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
        const actions = store.getActions();
        expect(actions.length).toBe(3);
        expect(actions).toHaveActionType('access/createOrUpdateRole/pending');
        expect(axios.put).toHaveBeenCalledTimes(1);
        expect(axios.put).toHaveBeenCalledWith(
          `/api/v2/roleMemberships/application/application/role/${role.roleId}/members`,
          formatedMemberList
        );
        expect(actions).toHaveActionTypesInOrder([
          'access/createOrUpdateRole/pending',
          'access/createOrUpdateRole/fulfilled',
          'access/saveMaskTimerDone',
        ]);
        done();
      });
    });

    it('dispatches rejected action if createOrUpdateRole request fails', (done) => {
      mockAxiosCalls({
        put: {
          [createOrUpdateRoleUrl]: () => Promise.reject('something went wrong'),
        },
      });
      store.dispatch(createOrUpdateRole('application', 'application', role.roleId)).then(() => {
        const actions = store.getActions();
        expect(axios.put).toHaveBeenCalledTimes(1);
        expect(actions.length).toBe(2);
        expect(axios.put).toHaveBeenCalledWith(
          `/api/v2/roleMemberships/application/application/role/${role.roleId}/members`,
          formatedMemberList
        );
        expect(actions).toHaveActionTypesInOrder([
          'access/createOrUpdateRole/pending',
          'access/createOrUpdateRole/rejected',
        ]);
        done();
      });
    });
  });

  describe('createOrUpdateRole under repository', () => {
    beforeEach(function () {
      state.router.currentState.name = repositoryAdress;
      jest.spyOn(accessSelectors, 'selectUnSortedAddedUsers').mockReturnValue(formatedMemberList);
      jest.spyOn(accessSelectors, 'selectRole').mockReturnValue(role);
    });

    it('create new role createOrUpdateRole/fulfilled action success', (done) => {
      mockAxiosCalls({
        put: {
          [createOrUpdateRoleUrlRepository]: Promise.resolve({ data: {} }),
        },
      });
      store.dispatch(createOrUpdateRole(role.roleId)).then(() => {
        jest.advanceTimersByTime(SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
        const actions = store.getActions();
        expect(actions.length).toBe(3);
        expect(actions).toHaveActionType('access/createOrUpdateRole/pending');
        expect(axios.put).toHaveBeenCalledTimes(1);
        expect(axios.put).toHaveBeenCalledWith(
          `/api/v2/roleMemberships/repository_container/role/${role.roleId}/members`,
          formatedMemberList
        );
        expect(actions).toHaveActionTypesInOrder([
          'access/createOrUpdateRole/pending',
          'access/createOrUpdateRole/fulfilled',
          'access/saveMaskTimerDone',
        ]);
        done();
      });
    });

    it('dispatches rejected action if createOrUpdateRole request fails', (done) => {
      mockAxiosCalls({
        put: {
          [createOrUpdateRoleUrlRepository]: () => Promise.reject('something went wrong'),
        },
      });
      store.dispatch(createOrUpdateRole('application', 'application', role.roleId)).then(() => {
        const actions = store.getActions();
        expect(axios.put).toHaveBeenCalledTimes(1);
        expect(actions.length).toBe(2);
        expect(axios.put).toHaveBeenCalledWith(
          `/api/v2/roleMemberships/repository_container/role/${role.roleId}/members`,
          formatedMemberList
        );
        expect(actions).toHaveActionTypesInOrder([
          'access/createOrUpdateRole/pending',
          'access/createOrUpdateRole/rejected',
        ]);
        done();
      });
    });
  });

  describe('goToAddAccess action success', () => {
    it('goToAddAccess action success', () => {
      store.dispatch(goToAddAccess()).then(() => {
        const actions = store.getActions();
        expect(actions.length).toBe(3);
        expect(actions).toHaveActionTypesInOrder([
          'access/goToAddAccess/pending',
          '@@reduxUiRouter/stateGo',
          'access/goToAddAccess/fulfilled',
        ]);
      });
    });
  });

  describe('loadRolesIfNeeded', () => {
    beforeEach(function () {
      state = {
        roleId: '0df46317c031440795007f4ce9c7f002',
        router: {
          currentParams: {
            '#': null,
            organizationId: 'ROOT_ORGANIZATION_ID',
          },
          currentState: {
            data: {
              isDirty: ['orgsAndPolicies', 'access', 'isDirty'],
              title: 'Organization Access',
            },
            name: orgAdress,
            url: '/access',
          },
        },
        orgsAndPolicies: {
          access: {
            availableRoles,
            role,
          },
        },
      };
      state.router.currentState.name = orgAdress;
    });

    it('if matching role not exists in memory loadRoles/fulfilled action success', (done) => {
      jest.spyOn(accessSelectors, 'selectRoleToEdit').mockReturnValue(null);
      let loadRolesSpy = jest.spyOn(actionsSlice, 'loadRoles');
      let loadRolesIfNeededSpy = jest.spyOn(actionsSlice, 'loadRolesIfNeeded').mockImplementation(() => {
        loadRolesSpy();
        mockAxiosCalls({
          get: {
            [roleMappingUrl]: Promise.resolve({ data: { availableRoles } }),
          },
        });
        store.dispatch(loadRoles()).then(() => {
          const actions = store.getActions();
          expect(actions.length).toBe(2);
          expect(loadRolesSpy).toHaveBeenCalledTimes(1);
          expect(actions).toHaveActionType('access/loadRoles/pending');
          expect(actions).toHaveActionTypesInOrder(['access/loadRoles/pending', 'access/loadRoles/fulfilled']);
        });
      });
      loadRolesIfNeededSpy();
      expect(loadRolesIfNeededSpy).toHaveBeenCalled();
      done();
    });

    it('does not dispatch loadRoles() if matching role exists in memory', () => {
      const store = SpecUtil.mockReduxStore({
        ownerId: 'ROOT_ORGANIZATION_ID',
      });
      jest.spyOn(accessSelectors, 'selectRoleToEdit').mockReturnValue({
        roleId: '1da70fae1fd54d6cb7999871ebdb9a36',
        roleName: 'Developer',
        roleDescription: 'Views all information for their assigned organization or application.',
        membersByOwner: [
          {
            members: [
              {
                displayName: 'Admin BuiltIn',
                email: 'admin@localhost',
                internalName: 'admin',
                realm: 'IQ Server',
                type: 'USER',
              },
            ],
            ownerId: 'ROOT_ORGANIZATION_ID',
            ownerName: 'Root Organization',
            ownerType: 'organization',
          },
        ],
      });

      store.dispatch(loadRolesIfNeeded());
      const actions = store.getActions();
      const payload = [
        {
          displayName: 'Admin BuiltIn',
          email: 'admin@localhost',
          id: 'adminUSER',
          internalName: 'admin',
          realm: 'IQ Server',
          type: 'USER',
        },
      ];
      expect(actions.length).toBe(3);
      expect(actions).toHaveAction({
        type: 'access/clearDeleteError',
        payload: undefined,
      });
      expect(actions).toHaveAction({
        type: 'access/setAddedUsers',
        payload,
      });
      expect(actions).toHaveAction({
        type: 'access/setServerAddedUsers',
        payload,
      });
    });
  });

  describe('removeRole under organisation', () => {
    beforeEach(function () {
      state.router.currentParams.roleId = '90c7c98683b4471cb77a916744540bcc';
    });
    it('deletes role successfully', (done) => {
      mockAxiosCalls({
        put: { [removeRoleUrl]: Promise.resolve({ data: role.roleId }) },
      });
      store.dispatch(removeRole(role.roleId)).then(() => {
        jest.advanceTimersByTime(SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
        const actions = store.getActions();
        expect(actions.length).toBe(3);
        expect(axios.put).toHaveBeenCalledTimes(1);
        expect(axios.put).toHaveBeenCalledWith(
          `/api/v2/roleMemberships/application/application/role/${role.roleId}/members`,
          []
        );
        expect(actions).toHaveActionTypesInOrder([
          'access/removeRole/pending',
          'access/removeRole/fulfilled',
          'access/deleteMaskTimerDone',
        ]);
        done();
      });
    });
    it('dispatches rejected action if remove request fails', (done) => {
      mockAxiosCalls({ put: { [removeRoleUrl]: () => Promise.reject('could not remove role') } });
      store.dispatch(removeRole()).then(() => {
        expect(axios.put).toHaveBeenCalledTimes(1);
        expect(axios.put).toHaveBeenCalledWith(
          '/api/v2/roleMemberships/application/application/role/90c7c98683b4471cb77a916744540bcc/members',
          []
        );
        const actions = store.getActions();
        expect(actions.length).toBe(2);
        expect(actions).toHaveActionTypesInOrder(['access/removeRole/pending', 'access/removeRole/rejected']);
        expect(actions[1].payload).toBe('could not remove role');
        done();
      });
    });
  });

  describe('removeRole under repository', () => {
    beforeEach(function () {
      state.router.currentState.name = repositoryAdress;
      state.router.currentParams.roleId = '90c7c98683b4471cb77a916744540bcc';
      jest.spyOn(routerSelectors, 'selectIsRepositoriesRelated').mockReturnValue(true);
    });

    it('deletes role successfully', (done) => {
      mockAxiosCalls({
        put: { [removeRoleUrlRepository]: Promise.resolve({ data: role.roleId }) },
      });
      store.dispatch(removeRole(role.roleId)).then(() => {
        jest.advanceTimersByTime(SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
        const actions = store.getActions();
        expect(actions.length).toBe(3);
        expect(axios.put).toHaveBeenCalledTimes(1);
        expect(axios.put).toHaveBeenCalledWith(
          `/api/v2/roleMemberships/repository_container/role/${role.roleId}/members`,
          []
        );
        expect(actions).toHaveActionTypesInOrder([
          'access/removeRole/pending',
          'access/removeRole/fulfilled',
          'access/deleteMaskTimerDone',
        ]);
        done();
      });
    });
    it('dispatches rejected action if remove request fails', (done) => {
      mockAxiosCalls({ put: { [removeRoleUrlRepository]: () => Promise.reject('could not remove role') } });
      store.dispatch(removeRole()).then(() => {
        expect(axios.put).toHaveBeenCalledTimes(1);
        expect(axios.put).toHaveBeenCalledWith(
          '/api/v2/roleMemberships/repository_container/role/90c7c98683b4471cb77a916744540bcc/members',
          []
        );
        const actions = store.getActions();
        expect(actions.length).toBe(2);
        expect(actions).toHaveActionTypesInOrder(['access/removeRole/pending', 'access/removeRole/rejected']);
        done();
      });
    });
  });
});
