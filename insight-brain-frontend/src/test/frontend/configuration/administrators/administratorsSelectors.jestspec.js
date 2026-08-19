/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  selectIsLoading,
  selectLoadError,
  selectServerData,
  selectMembersByRole,
  selectRoleToEdit,
  selectFetchUsers,
  selectSubmitMaskState,
  selectSubmitError,
  selectAddedUsers,
  selectIsGroupSearchEnabled,
  selectFetchUsersLoadingError,
  selectFetchUsersLoading,
  selectFetchUsersData,
  selectUsersNotAdded,
} from 'MainRoot/configuration/administrators/administratorsSelectors';
import { selectRouterCurrentParams } from 'MainRoot/reduxUiRouter/routerSelectors';

describe('administratorsSelectors', () => {
  let mockState;

  beforeEach(() => {
    mockState = {
      administratorsConfig: {
        loading: false,
        loadError: null,
        rolesForCurrentOwner: { data: [], loading: false, loadError: null },
        serverData: {
          membersByRole: [
            {
              roleId: '1',
              roleName: 'Policy Administrator',
              roleDescription: 'Manages all organizations, applications, policies, and policy violations.',
              membersByOwner: [
                {
                  ownerId: 'global',
                  ownerName: 'Global',
                  ownerType: 'global',
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
              roleId: '2',
              roleName: 'System Administrator',
              roleDescription: 'Manages system configuration and users.',
              membersByOwner: [
                {
                  ownerId: 'global',
                  ownerName: 'Global',
                  ownerType: 'global',
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
          ],
          groupSearchEnabled: true,
        },
        fetchUsers: {
          data: [
            {
              type: 'GROUP',
              internalName: 'fetchgroup',
              displayName: 'fetch group',
              email: 'null',
              realm: 'IQ Server',
            },
            {
              type: 'USER',
              internalName: 'fetchadmin',
              displayName: 'fetch Admin BuiltIn',
              email: 'fetch@localhost',
              realm: 'IQ Server',
            },
            {
              type: 'GROUP',
              internalName: 'addedgroup',
              displayName: 'added group',
              email: 'null',
              realm: 'IQ Server',
            },
          ],
          loading: true,
          loadError: 'load fetch error',
        },
        addedUsers: [
          {
            type: 'GROUP',
            internalName: 'addedgroup',
            displayName: 'added group',
            email: 'null',
            realm: 'IQ Server',
          },
          {
            type: 'USER',
            internalName: 'addedadmin',
            displayName: 'added Admin',
            email: 'added@localhost',
            realm: 'IQ Server',
          },
        ],
        submitMaskState: null,
        submitError: 'error submitting',
        isDirty: false,
      },
    };
  });

  describe('selectIsLoading', () => {
    it('selects loading value', () => {
      expect(selectIsLoading(mockState)).toBe(false);
    });
  });

  describe('selectLoadError', () => {
    it('selects loadError value', () => {
      expect(selectLoadError(mockState)).toBeNull();
    });
  });

  describe('selectServerData', () => {
    it('selects serverData', () => {
      const actual = selectServerData(mockState);

      expect(actual).toEqual(mockState.administratorsConfig.serverData);
    });
  });

  describe('selectMembersByRole', () => {
    it('selects membersByRole if available', () => {
      const actual = selectMembersByRole(mockState);

      expect(actual).toEqual(mockState.administratorsConfig.serverData.membersByRole);
    });

    it('selects membersByRole if not available and returns empty array', () => {
      mockState.administratorsConfig.serverData = null;
      const actual = selectMembersByRole(mockState);

      expect(actual).toEqual([]);
    });
  });

  describe('selectRoleToEdit', () => {
    it('is composed from the following selectors', () => {
      expect(selectRoleToEdit.dependencies).toEqual([selectMembersByRole, selectRouterCurrentParams]);
    });

    it('selects the role by roleId', () => {
      const mockRouterCurrentParams = { roleId: '2' };
      const actualSelection = selectRoleToEdit.resultFunc(
        mockState.administratorsConfig.serverData.membersByRole,
        mockRouterCurrentParams
      );

      expect(actualSelection).toEqual(mockState.administratorsConfig.serverData.membersByRole[1]);
    });
  });

  describe('selectFetchUsers', () => {
    it('selects selectFetchUsers', () => {
      const actualSelection = selectFetchUsers(mockState);
      expect(actualSelection).toEqual(mockState.administratorsConfig.fetchUsers);
    });
  });

  describe('selectSubmitMaskState', () => {
    it('selects submitMaskState', () => {
      const actualSelection = selectSubmitMaskState(mockState);
      expect(actualSelection).toBeNull();
    });
  });

  describe('selectSubmitError', () => {
    it('selects submitError', () => {
      const actualSelection = selectSubmitError(mockState);
      expect(actualSelection).toBe('error submitting');
    });
  });

  describe('selectAddedUsers', () => {
    it('selects and formats addedUsers', () => {
      const actualSelection = selectAddedUsers(mockState);
      expect(actualSelection).toEqual([
        {
          type: 'USER',
          internalName: 'addedadmin',
          displayName: 'added Admin (addedadmin)',
          email: 'added@localhost',
          realm: 'IQ Server',
        },
        {
          type: 'GROUP',
          internalName: 'addedgroup',
          displayName: 'added group (Group)',
          email: 'null',
          realm: 'IQ Server',
        },
      ]);
    });
  });

  describe('selectIsGroupSearchEnabled', () => {
    it('selects groupSearchEnabled', () => {
      const actual = selectIsGroupSearchEnabled(mockState);
      expect(actual).toBe(true);
    });
  });

  describe('selectFetchUsersLoading', () => {
    it('selects fetchUsers.loading', () => {
      const actual = selectFetchUsersLoading(mockState);
      expect(actual).toBe(true);
    });
  });

  describe('selectFetchUsersLoadingError', () => {
    it('selects fetchUsers.loadError', () => {
      const actual = selectFetchUsersLoadingError(mockState);
      expect(actual).toBe('load fetch error');
    });
  });

  describe('selectFetchUsersData', () => {
    it('selects fetchUsers.data', () => {
      const actual = selectFetchUsersData(mockState);
      expect(actual).toEqual([
        {
          type: 'GROUP',
          internalName: 'fetchgroup',
          displayName: 'fetch group',
          email: 'null',
          realm: 'IQ Server',
        },
        {
          type: 'USER',
          internalName: 'fetchadmin',
          displayName: 'fetch Admin BuiltIn',
          email: 'fetch@localhost',
          realm: 'IQ Server',
        },
        {
          type: 'GROUP',
          internalName: 'addedgroup',
          displayName: 'added group',
          email: 'null',
          realm: 'IQ Server',
        },
      ]);
    });
  });

  describe('selectUsersNotAdded', () => {
    it('selects formats and filters fetchUsers.data', () => {
      const actual = selectUsersNotAdded(mockState);
      expect(actual).toEqual([
        {
          type: 'GROUP',
          internalName: 'fetchgroup',
          displayName: 'fetch group (Group)',
          email: 'null',
          realm: 'IQ Server',
        },
        {
          type: 'USER',
          internalName: 'fetchadmin',
          displayName: 'fetch Admin BuiltIn (fetchadmin)',
          email: 'fetch@localhost',
          realm: 'IQ Server',
        },
      ]);
    });
  });
});
