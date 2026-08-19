/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { createSelector } from '@reduxjs/toolkit';
import {
  selectServerData,
  selectMembersByRole,
  selectRoleToEdit,
  selectFetchUsers,
  selectUnSortedAddedUsers,
  selectIsGroupSearchEnabled,
  selectAvailableRoles,
  selectRole,
  selectRolesSiblings,
  selectOwnerType,
  selectGroupName,
  selectValidationError,
  selectNoRolesAvailableError,
  selectAccessSlice,
  selectExtendedMembersByRole,
  selectRolesWithoutLocalMembersExist,
  selectInheritedAccessOpen,
} from 'MainRoot/OrgsAndPolicies/access/accessSelectors';
import { selectRouterCurrentParams } from 'MainRoot/reduxUiRouter/routerSelectors';
import { GLOBAL_FORM_VALIDATION_ERROR } from 'MainRoot/util/validationUtil';
import { lensPath, prop, set } from 'ramda';

describe('accessSelectors', () => {
  let mockState;

  beforeEach(() => {
    mockState = {
      router: {
        currentParams: {
          applicationPublicId: 'application',
        },
        currentState: {
          name: 'application',
        },
      },
      orgsAndPolicies: {
        access: {
          loading: false,
          loadError: null,
          isNew: false,
          deleteError: null,
          role: {
            roleDescription:
              'Evaluates individual components and views policy violation results for a specified application.',
            roleId: '90c7c98683b4471cb77a916744540bcc',
            roleName: 'Component Evaluator',
          },
          availableRoles: {
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
          },
          serverData: {
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
          },
          siblings: [
            {
              roleId: '2cb71b3468d649789163ea2e212b541e',
              roleName: 'Application Evaluator',
              roleDescription: 'Evaluates applications and views policy violation summary results.',
              membersByOwner: [
                {
                  ownerId: 'ROOT_ORGANIZATION_ID',
                  ownerName: 'Root Organization',
                  ownerType: 'organization',
                  members: [
                    {
                      type: 'GROUP',
                      internalName: 'fetchgroup',
                      displayName: 'fetch group',
                      email: 'null',
                      realm: 'IQ Server',
                    },
                  ],
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
                      type: 'GROUP',
                      internalName: 'fetchgroup',
                      displayName: 'fetch group',
                      email: 'null',
                      realm: 'IQ Server',
                    },
                  ],
                },
              ],
            },
          ],
          fetchUsers: {
            data: [
              {
                type: 'GROUP',
                internalName: 'fetchgroup',
                displayName: 'fetch group',
                id: 'fetchgroupGROUP',
                email: 'null',
                realm: 'IQ Server',
              },
              {
                type: 'USER',
                internalName: 'fetchadmin',
                displayName: 'fetch Admin BuiltIn',
                id: 'fetchadminUSER',
                email: 'fetch@localhost',
                realm: 'IQ Server',
              },
              {
                type: 'GROUP',
                internalName: 'addedgroup',
                displayName: 'added group',
                id: 'addedgroupGROUP',
                email: 'null',
                realm: 'IQ Server',
              },
            ],
            loading: true,
            loadError: 'load fetch error',
            partialError: 'error message',
          },
          addedUsers: [
            {
              type: 'GROUP',
              internalName: 'addedgroup',
              displayName: 'added group (Group)',
              id: 'addedgroupGROUP',
              email: 'null',
              realm: 'IQ Server',
            },
            {
              type: 'USER',
              internalName: 'addedadmin',
              displayName: 'added Admin',
              id: 'addedadminUSER',
              email: 'added@localhost',
              realm: 'IQ Server',
            },
          ],
          submitMaskState: null,
          deleteMaskState: null,
          submitError: 'error submitting',
          isDirty: false,
          groupName: {
            isPristine: true,
            value: 'groupName',
            trimmedValue: 'groupName',
            validationErrors: null,
          },
          inheritedAccessOpen: {
            '6b365e8a8000449aa924f194a7ed0d27': false,
          },
        },
      },
    };
  });

  describe('selectServerData', () => {
    it('selects serverData', () => {
      const actual = selectServerData(mockState);
      expect(actual).toEqual(mockState.orgsAndPolicies.access.serverData);
    });
  });

  describe('selectMembersByRole', () => {
    it('selects membersByRole if available', () => {
      const actual = selectMembersByRole(mockState);
      expect(actual).toEqual(mockState.orgsAndPolicies.access.serverData.membersByRole);
    });

    it('selects membersByRole if not available and returns empty array', () => {
      mockState.orgsAndPolicies.access.serverData = null;
      const actual = selectMembersByRole(mockState);
      expect(actual).toEqual([]);
    });
  });

  describe('selectRoleToEdit', () => {
    it('is composed from the following selectors', () => {
      expect(selectRoleToEdit.dependencies).toEqual([selectMembersByRole, selectRouterCurrentParams]);
    });
  });

  describe('selectFetchUsers', () => {
    it('selects selectFetchUsers', () => {
      const actualSelection = selectFetchUsers(mockState);
      expect(actualSelection).toEqual(mockState.orgsAndPolicies.access.fetchUsers);
    });
  });

  describe('selectUnSortedAddedUsers', () => {
    it('selects addedUsers', () => {
      const actualSelection = selectUnSortedAddedUsers(mockState);
      expect(actualSelection).toEqual([
        {
          type: 'GROUP',
          internalName: 'addedgroup',
          displayName: 'added group (Group)',
          id: 'addedgroupGROUP',
          email: 'null',
          realm: 'IQ Server',
        },
        {
          type: 'USER',
          internalName: 'addedadmin',
          displayName: 'added Admin',
          id: 'addedadminUSER',
          email: 'added@localhost',
          realm: 'IQ Server',
        },
      ]);
    });
  });

  describe('selectIsGroupSearchEnabled', () => {
    it('selects groupSearchEnabled', () => {
      const actual = selectIsGroupSearchEnabled(mockState);
      expect(actual).toBeTruthy();
    });
  });

  describe('selectAvailableRoles', () => {
    it('available roles to choose', () => {
      const actual = selectAvailableRoles(mockState);
      const expectAvailableRoles = {
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
      };
      expect(actual).toEqual(expectAvailableRoles);
    });
  });

  describe('selectRole', () => {
    it('selects role from dropdown', () => {
      const role = {
        roleDescription:
          'Evaluates individual components and views policy violation results for a specified application.',
        roleId: '90c7c98683b4471cb77a916744540bcc',
        roleName: 'Component Evaluator',
      };
      const actual = selectRole(mockState);
      expect(actual).toEqual(role);
    });
  });

  describe('selectRolesSiblings', () => {
    it('select array of roles which was added', () => {
      const actual = selectRolesSiblings(mockState);
      const expectSiblings = [
        {
          roleId: '2cb71b3468d649789163ea2e212b541e',
          roleName: 'Application Evaluator',
          roleDescription: 'Evaluates applications and views policy violation summary results.',
          membersByOwner: [
            {
              ownerId: 'ROOT_ORGANIZATION_ID',
              ownerName: 'Root Organization',
              ownerType: 'organization',
              members: [
                {
                  type: 'GROUP',
                  internalName: 'fetchgroup',
                  displayName: 'fetch group',
                  email: 'null',
                  realm: 'IQ Server',
                },
              ],
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
                  type: 'GROUP',
                  internalName: 'fetchgroup',
                  displayName: 'fetch group',
                  email: 'null',
                  realm: 'IQ Server',
                },
              ],
            },
          ],
        },
      ];
      expect(actual).toEqual(expectSiblings);
    });
  });

  describe('selectAvailableRoles', () => {
    it('select available roles with select option', () => {
      const actual = selectAvailableRoles(mockState);
      const expectAvailableRoles = {
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
      };
      expect(actual).toEqual(expectAvailableRoles);
    });
  });

  describe('selectOwnerType', () => {
    it('returns "application" when at the app level', () => {
      const actual = selectOwnerType(mockState);
      expect(actual).toEqual('application');
    });

    it('returns "organization" when at the org level', () => {
      const routerState = {
        currentParams: {
          applicationPublicId: 'organizationId',
        },
        currentState: {
          name: 'organization',
        },
      };
      const actual = selectOwnerType({ ...mockState, router: routerState });
      expect(actual).toEqual('organization');
    });

    it('returns "all repository managers" when at the repository container level', () => {
      const routerState = {
        currentParams: {
          repositoryContainerId: 'repositoryContainerId',
        },
        currentState: {
          name: 'repository_container',
        },
      };
      const actual = selectOwnerType({ ...mockState, router: routerState });
      expect(actual).toEqual('all repository managers');
    });

    it('returns "repository manager" when at the repository manager level', () => {
      const routerState = {
        currentParams: {
          repositoryManagerId: 'repositoryManagerId',
        },
        currentState: {
          name: 'repository_manager',
        },
      };
      const actual = selectOwnerType({ ...mockState, router: routerState });
      expect(actual).toEqual('repository manager');
    });

    it('returns "repository" when at the repository level', () => {
      const routerState = {
        currentParams: {
          repositoryId: 'repositoryId',
        },
        currentState: {
          name: 'repository',
        },
      };
      const actual = selectOwnerType({ ...mockState, router: routerState });
      expect(actual).toEqual('repository');
    });
  });

  describe('selectGroupName', () => {
    it('select correct groupName', () => {
      const actual = selectGroupName(mockState);
      const expected = {
        isPristine: true,
        value: 'groupName',
        trimmedValue: 'groupName',
        validationErrors: null,
      };
      expect(actual).toEqual(expected);
    });
  });

  describe('selectValidationError', () => {
    describe('when in create mode', () => {
      it('returns validation error if no role is selected and added members exist', () => {
        const state = {
          orgsAndPolicies: {
            access: {
              role: null,
              isNew: true,
              addedUsers: mockState.orgsAndPolicies.access.addedUsers,
            },
          },
        };
        const actual = selectValidationError(state);
        expect(actual).toEqual(GLOBAL_FORM_VALIDATION_ERROR);
      });

      it('returns validation error if role is selected and no added members exist', () => {
        const state = {
          orgsAndPolicies: {
            access: {
              role: mockState.orgsAndPolicies.access.role,
              isNew: true,
              addedUsers: [],
            },
          },
        };
        const actual = selectValidationError(state);
        expect(actual).toEqual(GLOBAL_FORM_VALIDATION_ERROR);
      });

      it('returns no validation error if role is selected and added members exist', () => {
        const state = {
          orgsAndPolicies: {
            access: {
              role: mockState.orgsAndPolicies.access.role,
              isNew: true,
              addedUsers: mockState.orgsAndPolicies.access.addedUsers,
            },
          },
        };
        const actual = selectValidationError(state);
        expect(actual).toEqual(null);
      });
    });

    describe('when in edit mode', () => {
      it('returns no validation error', () => {
        const actual = selectValidationError(mockState);
        expect(actual).toEqual(null);
      });
    });
  });

  describe('selectNoRolesAvailableError', () => {
    it('show error message', () => {
      expect(JSON.stringify(selectNoRolesAvailableError.dependencies)).toEqual(
        JSON.stringify([createSelector(selectAccessSlice, prop('isNew')), selectAvailableRoles])
      );
    });
  });

  describe('selectExtendedMembersByRole', () => {
    it('formats the members by role object', () => {
      const actual = selectExtendedMembersByRole(mockState);
      const expected = [
        {
          ownerId: 'ROOT_ORGANIZATION_ID',
          ownerName: 'Root Organization',
          ownerType: 'organization',
          roles: [
            {
              roleId: '90c7c98683b4471cb77a916744540bcc',
              roleName: 'Component Evaluator',
              roleDescription:
                'Evaluates individual components and views policy violation results for a specified application.',
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
          isInherited: false,
        },
      ];
      expect(actual).toEqual(expected);
    });
  });

  describe('selectRolesWithoutLocalMembersExist', () => {
    it('returns true when there are roles without local members', () => {
      const actual = selectRolesWithoutLocalMembersExist(mockState);
      expect(actual).toBeTruthy();
    });

    it('returns false when there are no roles without local members', () => {
      const membersByRoleLens = lensPath(['orgsAndPolicies', 'access', 'serverData', 'membersByRole']);
      const newMockState = set(
        membersByRoleLens,
        [
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
        ],
        mockState
      );
      const actual = selectRolesWithoutLocalMembersExist(newMockState);
      expect(actual).toBeFalsy();
    });
  });

  describe('selectInheritedAccessOpen', () => {
    it('returns inheritedAccessOpen map', () => {
      const expected = {
        '6b365e8a8000449aa924f194a7ed0d27': false,
      };
      expect(selectInheritedAccessOpen(mockState)).toEqual(expected);
    });
  });
});
