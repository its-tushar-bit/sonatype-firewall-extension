/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import '../../SpecUtil';
import reducer from 'MainRoot/OrgsAndPolicies/access/accessSlice';

const payload = {
  roleId: '90c7c98683b4471cb77a916744540bcc',
  role: {
    roleId: '90c7c98683b4471cb77a916744540bcc',
    roleName: 'Component Evaluator',
    roleDescription: 'Evaluates individual components and views policy violation results for a specified application.',
  },
  data: {
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
            members: [
              {
                type: 'USER',
                internalName: 'admin',
                displayName: 'Admin BuiltIn',
                email: 'admin@localhost',
                realm: 'IQ Server',
              },
            ],
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
    availableRoles: [
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
  },
  addedUsers: [
    {
      displayName: 'Admin BuiltIn',
      email: 'admin@localhost',
      id: 'adminUSER',
      internalName: 'admin',
      realm: 'IQ Server',
      type: 'USER',
    },
  ],
  siblings: [
    {
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
      roleDescription:
        'Evaluates individual components and views policy violation, results for a specified application.',
      roleId: '90c7c98683b4471cb77a916744540bcc',
      roleName: 'Component Evaluator',
    },
  ],
};

describe('access reducer', () => {
  let otherObject;
  beforeEach(() => {
    otherObject = { value: 'Other value' };
  });
  describe('access/loadRoles/pending', () => {
    it('resets loading, loadError properties', () => {
      const state = Object.freeze({
        loading: false,
        loadError: 'error',
      });
      const { loading, loadError } = reducer(state, {
        type: 'access/loadRoles/pending',
      });
      expect(loading).toBe(true);
      expect(loadError).toBeNull();
    });
  });

  describe('access/loadRoles/fulfilled', () => {
    it('sets loading, loadError, policyMonitoringByOwner properties', () => {
      const state = Object.freeze({
        loading: true,
        loadError: 'some error',
        availableRoles: null,
        role: null,
        addedUsers: [],
        serverAddedUsers: [],
        isDirty: false,
        isNew: false,
      });

      const newState = reducer(state, {
        type: 'access/loadRoles/fulfilled',
        payload: payload,
      });

      expect(newState.loading).toBe(false);
      expect(newState.loadError).toBeNull();
      expect(newState.availableRoles).toEqual(payload.data.availableRoles);
      expect(newState.addedUsers).toEqual([
        {
          displayName: 'Admin BuiltIn (admin)',
          email: 'admin@localhost',
          id: 'adminUSER',
          internalName: 'admin',
          realm: 'IQ Server',
          type: 'USER',
        },
      ]);
      expect(newState.isDirty).toBe(false);
      expect(newState.isNew).toBe(false);
      expect(newState.serverData).toEqual(payload.data);
      expect(newState.role).toEqual(payload.role);
      expect(newState.serverAddedUsers).toEqual([
        {
          displayName: 'Admin BuiltIn (admin)',
          email: 'admin@localhost',
          id: 'adminUSER',
          internalName: 'admin',
          realm: 'IQ Server',
          type: 'USER',
        },
      ]);
      expect(newState.siblings).toEqual([
        Object({
          roleId: '90c7c98683b4471cb77a916744540bcc',
          roleName: 'Component Evaluator',
          roleDescription:
            'Evaluates individual components and views policy violation results for a specified application.',
          membersByOwner: [
            Object({
              ownerId: 'ROOT_ORGANIZATION_ID',
              ownerName: 'Root Organization',
              ownerType: 'organization',
              members: [
                Object({
                  type: 'USER',
                  internalName: 'admin',
                  displayName: 'Admin BuiltIn',
                  email: 'admin@localhost',
                  realm: 'IQ Server',
                }),
              ],
            }),
          ],
        }),
      ]);
      expect(newState.inheritedAccessOpen).toEqual({
        ROOT_ORGANIZATION_ID: true,
      });
    });
  });

  describe('access/loadRoles/rejected', () => {
    it('sets loading, loadError properties', () => {
      const state = Object.freeze({
        loading: true,
        loadError: null,
      });

      const { loading, loadError } = reducer(state, {
        type: 'access/loadRoles/rejected',
        payload: 'error',
      });

      expect(loading).toBe(false);
      expect(loadError).toBe('error');
    });
  });

  describe('access/loadFetchUsers/pending action', () => {
    it('sets loading to true', () => {
      const state = Object.freeze({
        other: otherObject,
        fetchUsers: { data: [], loading: false, loadError: 'some error', mostRecentUserQuery: null },
      });

      const {
        fetchUsers: { loading },
        other,
      } = reducer(state, {
        type: 'access/loadFetchUsers/pending',
        meta: {
          arg: 'myQuery',
          requestId: 'requestId',
          requestStatus: 'pending',
        },
      });

      expect(loading).toBe(true);
      expect(other).toBe(otherObject);
    });

    it('sets mostRecentUserQuery to be query provided ', () => {
      const state = Object.freeze({
        other: otherObject,
        fetchUsers: { data: [], loading: false, loadError: 'some error', mostRecentUserQuery: null },
      });

      const {
        fetchUsers: { mostRecentUserQuery },
        other,
      } = reducer(state, {
        type: 'access/loadFetchUsers/pending',
        meta: {
          arg: 'myQuery',
          requestId: 'requestId',
          requestStatus: 'pending',
        },
      });

      expect(mostRecentUserQuery).toBe('myQuery');
      expect(other).toBe(otherObject);
    });
  });

  describe('access/loadFetchUsers/fulfilled action', () => {
    describe('when current query does not match mostRecentUserQuery', () => {
      it('does not make any state changes', () => {
        const state = Object.freeze({
          other: otherObject,
          fetchUsers: {
            data: ['some data'],
            loading: false,
            loadError: null,
            partialError: null,
            mostRecentUserQuery: 'myQuery2',
          },
        });

        const {
          fetchUsers: { loading, loadError, data, partialError },
          other,
        } = reducer(state, {
          type: 'access/loadFetchUsers/fulfilled',
          payload: { members: [{ internalName: 'name', type: 'USER' }], query: 'myQuery1' },
        });

        expect(data).toEqual(state.fetchUsers.data);
        expect(loading).toEqual(state.fetchUsers.loading);
        expect(loadError).toEqual(state.fetchUsers.loadError);
        expect(partialError).toEqual(state.fetchUsers.partialError);
        expect(other).toEqual(state.other);
      });
    });

    describe('when current query matches mostRecentUserQuery', () => {
      describe('makes state changes', () => {
        it('sets loading to false, clears errors and set the data', () => {
          const state = Object.freeze({
            other: otherObject,
            fetchUsers: { data: [], loading: true, loadError: 'some error', mostRecentUserQuery: '*' },
            addedUsers: [
              {
                displayName: 'Admin BuiltIn',
                email: 'admin@localhost',
                id: 'adminUSER',
                internalName: 'admin',
                realm: 'IQ Server',
                type: 'USER',
              },
            ],
          });

          const {
            fetchUsers: { loading, loadError, data },
            other,
          } = reducer(state, {
            type: 'access/loadFetchUsers/fulfilled',
            payload: {
              members: [
                {
                  displayName: 'Bohdan LastName',
                  email: 'amax9111@gmail.com',
                  internalName: 'Bohdan',
                  realm: 'IQ Server',
                  type: 'USER',
                },
              ],
              query: '*',
            },
          });

          expect(loading).toBe(false);
          expect(loadError).toBeNull();
          expect(data).toEqual([
            {
              displayName: 'Bohdan LastName (Bohdan)',
              email: 'amax9111@gmail.com',
              id: 'BohdanUSER',
              internalName: 'Bohdan',
              realm: 'IQ Server',
              type: 'USER',
            },
          ]);
          expect(other).toBe(otherObject);
        });

        it('sets loading to false and set partialError when there is an error', () => {
          const state = Object.freeze({
            other: otherObject,
            fetchUsers: {
              data: [],
              loading: true,
              loadError: 'some error',
              partialError: null,
              mostRecentUserQuery: '*',
            },
            addedUsers: [
              {
                displayName: 'Admin BuiltIn',
                email: 'admin@localhost',
                id: 'adminUSER',
                internalName: 'admin',
                realm: 'IQ Server',
                type: 'USER',
              },
            ],
          });

          const {
            fetchUsers: { loading, loadError, data, partialError },
            other,
          } = reducer(state, {
            type: 'access/loadFetchUsers/fulfilled',
            payload: {
              members: [
                {
                  displayName: 'Bohdan LastName',
                  email: 'amax9111@gmail.com',
                  internalName: 'Bohdan',
                  realm: 'IQ Server',
                  type: 'USER',
                },
              ],
              error: 'there was an error',
              query: '*',
            },
          });

          expect(loading).toBe(false);
          expect(partialError).toBe('there was an error');
          expect(loadError).toBe(null);
          expect(data).toEqual([
            {
              displayName: 'Bohdan LastName (Bohdan)',
              email: 'amax9111@gmail.com',
              internalName: 'Bohdan',
              id: 'BohdanUSER',
              realm: 'IQ Server',
              type: 'USER',
            },
          ]);
          expect(other).toBe(otherObject);
        });
      });
    });
  });

  describe('access/loadFetchUsers/rejected action', () => {
    it('sets loading to false and sets loadError to payload', () => {
      const state = Object.freeze({
        other: otherObject,
        fetchUsers: { data: [], loading: true, loadError: 'some error' },
      });

      const {
        fetchUsers: { loading, loadError },
        other,
      } = reducer(state, {
        type: 'access/loadFetchUsers/rejected',
        payload: 'other error',
      });

      expect(loading).toBe(false);
      expect(loadError).toBe('other error');
      expect(other).toBe(otherObject);
    });
  });

  describe('aceess/createOrUpdateRole/pending', () => {
    it('resets submitMaskState, submitError properties', () => {
      const state = Object.freeze({
        other: otherObject,
        submitMaskState: true,
        submitError: 'error',
      });

      const { submitMaskState, submitError } = reducer(state, {
        type: 'access/createOrUpdateRole/pending',
      });

      expect(submitMaskState).toBe(false);
      expect(submitError).toBeNull();
    });
  });

  describe('access/createOrUpdateRole/fulfilled action', () => {
    it('sets submitMaskState to true', () => {
      const state = Object.freeze({
        other: otherObject,
        isDirty: true,
        submitMaskState: null,
        submitError: 'error',
        availableRoles: [
          {
            roleId: '0df46317c031440795007f4ce9c7f002',
            roleName: 'Legal Reviewer',
            roleDescription: 'Reviews legal obligations for component licenses.',
            membersByOwner: [],
          },
          {
            roleId: '1cddabf7fdaa47d6833454af10e0a3ef',
            roleName: 'Owner',
            roleDescription: 'Manages assigned organizations, applications, policies, and policy violations.',
            membersByOwner: [],
          },
        ],
        siblings: [],
        role: {
          membersByOwner: [
            {
              ownerId: 'REPOSITORY_CONTAINER_ID',
              ownerName: 'Repository Managers',
              ownerType: 'repository_container',
              members: [],
            },
            { ownerId: 'ROOT_ORGANIZATION_ID', ownerName: 'Root Organization', ownerType: 'organization', members: [] },
          ],
          roleId: '90c7c98683b4471cb77a916744540bcc',
          roleName: 'Component Evaluator',
          roleDescription:
            'Evaluates individual components and views policy violation results for a specified application.',
        },
      });

      const { submitMaskState, submitError, isDirty, availableRoles, siblings, other } = reducer(state, {
        type: 'access/createOrUpdateRole/fulfilled',
        payload: {},
      });
      expect(isDirty).toBe(false);
      expect(submitMaskState).toBe(true);
      expect(submitError).toBeNull();
      expect(other).toBe(otherObject);
      expect(siblings).toEqual([
        Object({
          membersByOwner: [
            Object({
              ownerId: 'REPOSITORY_CONTAINER_ID',
              ownerName: 'Repository Managers',
              ownerType: 'repository_container',
              members: [],
            }),
            Object({
              ownerId: 'ROOT_ORGANIZATION_ID',
              ownerName: 'Root Organization',
              ownerType: 'organization',
              members: [],
            }),
          ],
          roleId: '90c7c98683b4471cb77a916744540bcc',
          roleName: 'Component Evaluator',
          roleDescription:
            'Evaluates individual components and views policy violation results for a specified application.',
        }),
      ]);
      expect(availableRoles).toEqual([
        Object({
          roleId: '0df46317c031440795007f4ce9c7f002',
          roleName: 'Legal Reviewer',
          roleDescription: 'Reviews legal obligations for component licenses.',
          membersByOwner: [],
        }),
        Object({
          roleId: '1cddabf7fdaa47d6833454af10e0a3ef',
          roleName: 'Owner',
          roleDescription: 'Manages assigned organizations, applications, policies, and policy violations.',
          membersByOwner: [],
        }),
      ]);
    });
  });

  describe('access/createOrUpdateRole/rejected action', () => {
    it('sets submitMaskState to null and sets submitError to payload', () => {
      const state = Object.freeze({
        other: otherObject,
        submitMaskState: true,
        submitError: null,
      });

      const { submitMaskState, submitError, other } = reducer(state, {
        type: 'access/createOrUpdateRole/rejected',
        payload: 'other error',
      });

      expect(submitMaskState).toBeNull();
      expect(submitError).toBe('other error');
      expect(other).toBe(otherObject);
    });
  });

  describe('access/removeRole/pending', () => {
    it('resets deleteMaskState, deleteError properties', () => {
      const state = Object.freeze({
        deleteMaskState: true,
        deleteError: 'error',
      });

      const { deleteMaskState, deleteError } = reducer(state, {
        type: 'access/removeRole/pending',
      });

      expect(deleteMaskState).toBeNull();
      expect(deleteError).toBeNull();
    });
  });

  describe('access/removeRole/fulfilled', () => {
    it('resets deleteMaskState, deleteError properties', () => {
      const state = Object.freeze({
        deleteMaskState: false,
        deleteError: 'eroror',
        isDirty: true,
        siblings: [
          {
            roleId: '0df46317c031440795007f4ce9c7f002',
            roleName: 'Legal Reviewer',
            roleDescription: 'Reviews legal obligations for component licenses.',
            membersByOwner: [],
          },
          {
            roleId: '1cddabf7fdaa47d6833454af10e0a3ef',
            roleName: 'Owner',
            roleDescription: 'Manages assigned organizations, applications, policies, and policy violations.',
            membersByOwner: [],
          },
        ],
      });

      const { deleteMaskState, deleteError, siblings, isDirty } = reducer(state, {
        type: 'access/removeRole/fulfilled',
        payload: '1cddabf7fdaa47d6833454af10e0a3ef',
      });

      expect(deleteMaskState).toBe(true);
      expect(deleteError).toBeNull();
      expect(isDirty).toBe(false);
      expect(siblings).toEqual([
        Object({
          roleId: '1cddabf7fdaa47d6833454af10e0a3ef',
          roleName: 'Owner',
          roleDescription: 'Manages assigned organizations, applications, policies, and policy violations.',
          membersByOwner: [],
        }),
      ]);
    });
  });

  describe('access/removeRole/rejected', () => {
    it('sets submitMaskState, submitError properties', () => {
      const state = Object.freeze({
        deleteMaskState: null,
        deleteError: null,
      });

      const { deleteMaskState, deleteError } = reducer(state, {
        type: 'access/removeRole/rejected',
        payload: 'error',
      });

      expect(deleteMaskState).toBeNull();
      expect(deleteError).toBe('error');
    });
  });

  describe('access/setRole', () => {
    it('sets role and isDirty property', () => {
      const state = Object.freeze({
        role: undefined,
        isDirty: false,
        availableRoles: [
          {
            roleId: '2cb71b3468d649789163ea2e212b541e',
            roleName: 'Application Evaluator',
            roleDescription: 'Evaluates applications and views policy violation summary results.',
            membersByOwner: [
              Object({
                ownerId: 'ROOT_ORGANIZATION_ID',
                ownerName: 'Root Organization',
                ownerType: 'organization',
                members: [],
              }),
            ],
          },
          {
            roleId: '1da70fae1fd54d6cb7999871ebdb9a36',
            roleName: 'Developer',
            roleDescription: 'Views all information for their assigned organization or application.',
            membersByOwner: [
              Object({
                ownerId: 'ROOT_ORGANIZATION_ID',
                ownerName: 'Root Organization',
                ownerType: 'organization',
                members: [],
              }),
            ],
          },
          {
            roleId: '0df46317c031440795007f4ce9c7f002',
            roleName: 'Legal Reviewer',
            roleDescription: 'Reviews legal obligations for component licenses.',
            membersByOwner: [
              Object({
                ownerId: 'ROOT_ORGANIZATION_ID',
                ownerName: 'Root Organization',
                ownerType: 'organization',
                members: [],
              }),
            ],
          },
          {
            roleId: '1cddabf7fdaa47d6833454af10e0a3ef',
            roleName: 'Owner',
            roleDescription: 'Manages assigned organizations, applications, policies, and policy violations.',
            membersByOwner: [
              Object({
                ownerId: 'ROOT_ORGANIZATION_ID',
                ownerName: 'Root Organization',
                ownerType: 'organization',
                members: [],
              }),
            ],
          },
        ],
        addedUsers: payload.addedUsers,
        serverAddedUsers: [],
      });
      const { isDirty, role } = reducer(state, {
        type: 'access/setRole',
        payload: '1da70fae1fd54d6cb7999871ebdb9a36',
      });
      expect(isDirty).toBe(true);
      expect(role).toEqual({
        roleId: '1da70fae1fd54d6cb7999871ebdb9a36',
        roleName: 'Developer',
        roleDescription: 'Views all information for their assigned organization or application.',
        membersByOwner: [
          Object({
            ownerId: 'ROOT_ORGANIZATION_ID',
            ownerName: 'Root Organization',
            ownerType: 'organization',
            members: [],
          }),
        ],
      });
    });
  });

  describe('access/addSelectedUser', () => {
    it('sets selected user', () => {
      const state = Object.freeze({
        role: payload.role,
        isDirty: false,
        addedUsers: [],
        serverAddedUsers: [],
        fetchUsers: {
          data: [],
        },
      });
      const { isDirty, addedUsers } = reducer(state, {
        type: 'access/addSelectedUser',
        payload: {
          displayName: 'Bohdan LastName',
          email: 'amax9111@gmail.com',
          id: 'BohdanUSER',
          internalName: 'Bohdan',
          realm: 'IQ Server',
          type: 'USER',
        },
      });
      expect(isDirty).toBe(true);
      expect(addedUsers).toEqual([
        {
          displayName: 'Bohdan LastName',
          email: 'amax9111@gmail.com',
          id: 'BohdanUSER',
          internalName: 'Bohdan',
          realm: 'IQ Server',
          type: 'USER',
        },
      ]);
    });
  });

  describe('access/addSelectedUserGroup', () => {
    it('sets selected user group', () => {
      const state = Object.freeze({
        role: payload.role,
        isDirty: false,
        addedUsers: [],
        serverAddedUsers: [],
        groupName: {
          isPristine: true,
          value: 'John Doe',
          trimmedValue: 'John Doe',
          validationErrors: null,
        },
      });
      const { isDirty, addedUsers } = reducer(state, {
        type: 'access/addSelectedUserGroup',
        payload: state.groupName,
      });
      expect(isDirty).toBe(true);
      expect(addedUsers).toEqual([
        {
          displayName: 'John Doe (Group)',
          email: null,
          id: 'John DoeGROUP',
          internalName: 'John Doe',
          realm: null,
          type: 'GROUP',
        },
      ]);
    });
  });

  describe('access/setAddedUsers', () => {
    it('delete selected user', () => {
      const state = Object.freeze({
        role: payload.role,
        isDirty: false,
        addedUsers: [
          {
            displayName: 'Bohdan LastName',
            email: 'amax9111@gmail.com',
            id: 'BohdanUSER',
            internalName: 'Bohdan',
            realm: 'IQ Server',
            type: 'USER',
          },
          {
            displayName: 'Admin BuiltIn',
            email: 'admin@localhost',
            id: 'adminUSER',
            internalName: 'admin',
            realm: 'IQ Server',
            type: 'USER',
          },
        ],
        serverAddedUsers: [],
      });
      const { isDirty, addedUsers } = reducer(state, {
        type: 'access/setAddedUsers',
        payload: [
          {
            displayName: 'Admin BuiltIn',
            email: 'admin@localhost',
            id: 'adminUSER',
            internalName: 'admin',
            realm: 'IQ Server',
            type: 'USER',
          },
        ],
      });
      expect(isDirty).toBe(true);
      expect(addedUsers).toEqual([
        {
          displayName: 'Admin BuiltIn',
          email: 'admin@localhost',
          id: 'adminUSER',
          internalName: 'admin',
          realm: 'IQ Server',
          type: 'USER',
        },
      ]);
    });
  });

  describe('access/setGroupName', () => {
    it('sets groupName', () => {
      const state = Object.freeze({
        role: payload.role,
        groupName: {
          isPristine: true,
          value: '',
          trimmedValue: '',
          validationErrors: null,
        },
        addedUsers: [],
        serverAddedUsers: [],
      });
      const { groupName } = reducer(state, {
        type: 'access/setGroupName',
        payload: 'test123',
      });
      expect(groupName).toEqual({
        isPristine: false,
        value: 'test123',
        trimmedValue: 'test123',
        validationErrors: null,
      });
    });
  });

  describe('access/toggleInheritedAccessOpen', () => {
    it('toggles inheritedAccessOpen to true for the given ownerId', () => {
      const state = Object.freeze({
        inheritedAccessOpen: {
          ownerId: false,
          otherOwnerId1: false,
          otherOwnerId2: true,
        },
      });

      const { inheritedAccessOpen } = reducer(state, {
        type: 'access/toggleInheritedAccessOpen',
        payload: 'ownerId',
      });

      expect(inheritedAccessOpen).toEqual({
        ownerId: true,
        otherOwnerId1: false,
        otherOwnerId2: true,
      });
    });

    it('toggles inheritedAccessOpen to false for the given ownerId', () => {
      const state = Object.freeze({
        inheritedAccessOpen: {
          ownerId: true,
          otherOwnerId1: false,
          otherOwnerId2: true,
        },
      });

      const { inheritedAccessOpen } = reducer(state, {
        type: 'access/toggleInheritedAccessOpen',
        payload: 'ownerId',
      });

      expect(inheritedAccessOpen).toEqual({
        ownerId: false,
        otherOwnerId1: false,
        otherOwnerId2: true,
      });
    });
  });
});
