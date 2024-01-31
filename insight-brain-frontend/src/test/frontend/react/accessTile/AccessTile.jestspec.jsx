/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { cleanup } from '@testing-library/react';
import { axiosMockAdapter, render, screen, waitFor } from 'TestRoot/SpecUtil';
import AccessTile from 'MainRoot/react/accessTile/AccessTile';
import {
  getAccessPageRolesUrl,
  getRepositoryContainer,
  getRepositoryContainerRoleMappingUrl,
} from 'MainRoot/util/CLMLocation';
import * as accessSelectors from 'MainRoot/OrgsAndPolicies/access/accessSelectors';

describe('AccessTile', () => {
  let mock;

  let preloadedState = {
    router: {
      currentParams: {
        organizationId: '6a9be797916a4c12a6f8d8bc410aba86',
      },
      currentState: {
        name: 'management.view.organization',
        url: '/organization/{organizationId}',
      },
    },
    orgsAndPolicies: {
      access: {
        loading: false,
        serverData: {
          membersByRole: [
            {
              roleId: '2cb71b3468d649789163ea2e212b541e',
              roleName: 'Application Evaluator',
              roleDescription: 'Evaluates applications and views policy violation summary results.',
              membersByOwner: [
                {
                  ownerId: '6a9be797916a4c12a6f8d8bc410aba86',
                  ownerName: 'Sandbox Organization',
                  ownerType: 'organization',
                  members: [],
                },
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
                  ownerId: '6a9be797916a4c12a6f8d8bc410aba86',
                  ownerName: 'Sandbox Organization',
                  ownerType: 'organization',
                  members: [],
                },
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
                  ownerId: '6a9be797916a4c12a6f8d8bc410aba86',
                  ownerName: 'Sandbox Organization',
                  ownerType: 'organization',
                  members: [],
                },
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
                  ownerId: '6a9be797916a4c12a6f8d8bc410aba86',
                  ownerName: 'Sandbox Organization',
                  ownerType: 'organization',
                  members: [],
                },
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
                  ownerId: '6a9be797916a4c12a6f8d8bc410aba86',
                  ownerName: 'Sandbox Organization',
                  ownerType: 'organization',
                  members: [],
                },
                {
                  ownerId: 'ROOT_ORGANIZATION_ID',
                  ownerName: 'Root Organization',
                  ownerType: 'organization',
                  members: [],
                },
              ],
            },
            {
              roleId: 'd52645f5b7d141f58d71fbe1375397dd',
              roleName: 'Test Waivers Administrator',
              roleDescription: 'Test',
              membersByOwner: [
                {
                  ownerId: '6a9be797916a4c12a6f8d8bc410aba86',
                  ownerName: 'Sandbox Organization',
                  ownerType: 'organization',
                  members: [],
                },
                {
                  ownerId: 'ROOT_ORGANIZATION_ID',
                  ownerName: 'Root Organization',
                  ownerType: 'organization',
                  members: [],
                },
              ],
            },
          ],
          groupSearchEnabled: true,
        },
      },
    },
  };

  const rolesMockDataOrganizationInherited = {
    membersByRole: [
      {
        roleId: '2cb71b3468d649789163ea2e212b541e',
        roleName: 'Application Evaluator',
        roleDescription: 'Evaluates applications and views policy violation summary results.',
        membersByOwner: [
          {
            ownerId: '6a9be797916a4c12a6f8d8bc410aba86',
            ownerName: 'Sandbox Organization',
            ownerType: 'organization',
            members: [],
          },
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
            ownerId: '6a9be797916a4c12a6f8d8bc410aba86',
            ownerName: 'Sandbox Organization',
            ownerType: 'organization',
            members: [],
          },
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
            ownerId: '6a9be797916a4c12a6f8d8bc410aba86',
            ownerName: 'Sandbox Organization',
            ownerType: 'organization',
            members: [],
          },
          {
            ownerId: 'ROOT_ORGANIZATION_ID',
            ownerName: 'Root Organization',
            ownerType: 'organization',
            members: [
              {
                type: 'USER',
                internalName: 'testuser3',
                displayName: 'Test User 3',
                email: 'testuser3@test.com',
                realm: 'IQ Server',
              },
            ],
          },
        ],
      },
      {
        roleId: '0df46317c031440795007f4ce9c7f002',
        roleName: 'Legal Reviewer',
        roleDescription: 'Reviews legal obligations for component licenses.',
        membersByOwner: [
          {
            ownerId: '6a9be797916a4c12a6f8d8bc410aba86',
            ownerName: 'Sandbox Organization',
            ownerType: 'organization',
            members: [],
          },
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
            ownerId: '6a9be797916a4c12a6f8d8bc410aba86',
            ownerName: 'Sandbox Organization',
            ownerType: 'organization',
            members: [],
          },
          {
            ownerId: 'ROOT_ORGANIZATION_ID',
            ownerName: 'Root Organization',
            ownerType: 'organization',
            members: [],
          },
        ],
      },
      {
        roleId: 'd52645f5b7d141f58d71fbe1375397dd',
        roleName: 'Test Waivers Administrator',
        roleDescription: 'Test',
        membersByOwner: [
          {
            ownerId: '6a9be797916a4c12a6f8d8bc410aba86',
            ownerName: 'Sandbox Organization',
            ownerType: 'organization',
            members: [],
          },
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

  const rolesMockDataApplication = {
    membersByRole: [
      {
        roleId: '2cb71b3468d649789163ea2e212b541e',
        roleName: 'Application Evaluator',
        roleDescription: 'Evaluates applications and views policy violation summary results.',
        membersByOwner: [
          {
            ownerId: 'sandbox-application',
            ownerName: 'Sandbox Application',
            ownerType: 'application',
            members: [],
          },
          {
            ownerId: '6a9be797916a4c12a6f8d8bc410aba86',
            ownerName: 'Sandbox Organization',
            ownerType: 'organization',
            members: [],
          },
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
            ownerId: 'sandbox-application',
            ownerName: 'Sandbox Application',
            ownerType: 'application',
            members: [],
          },
          {
            ownerId: '6a9be797916a4c12a6f8d8bc410aba86',
            ownerName: 'Sandbox Organization',
            ownerType: 'organization',
            members: [],
          },
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
            ownerId: 'sandbox-application',
            ownerName: 'Sandbox Application',
            ownerType: 'application',
            members: [
              {
                type: 'USER',
                internalName: 'testuser1',
                displayName: 'Test User 1',
                email: 'testuser1@test.com',
                realm: 'IQ Server',
              },
            ],
          },
          {
            ownerId: '6a9be797916a4c12a6f8d8bc410aba86',
            ownerName: 'Sandbox Organization',
            ownerType: 'organization',
            members: [],
          },
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
            ownerId: 'sandbox-application',
            ownerName: 'Sandbox Application',
            ownerType: 'application',
            members: [],
          },
          {
            ownerId: '6a9be797916a4c12a6f8d8bc410aba86',
            ownerName: 'Sandbox Organization',
            ownerType: 'organization',
            members: [],
          },
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
            ownerId: 'sandbox-application',
            ownerName: 'Sandbox Application',
            ownerType: 'application',
            members: [],
          },
          {
            ownerId: '6a9be797916a4c12a6f8d8bc410aba86',
            ownerName: 'Sandbox Organization',
            ownerType: 'organization',
            members: [],
          },
          {
            ownerId: 'ROOT_ORGANIZATION_ID',
            ownerName: 'Root Organization',
            ownerType: 'organization',
            members: [],
          },
        ],
      },
      {
        roleId: 'd52645f5b7d141f58d71fbe1375397dd',
        roleName: 'Test Waivers Administrator',
        roleDescription: 'Test',
        membersByOwner: [
          {
            ownerId: 'sandbox-application',
            ownerName: 'Sandbox Application',
            ownerType: 'application',
            members: [],
          },
          {
            ownerId: '6a9be797916a4c12a6f8d8bc410aba86',
            ownerName: 'Sandbox Organization',
            ownerType: 'organization',
            members: [],
          },
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

  const rolesMockDataOrganizationWithLocallist = {
    membersByRole: [
      {
        roleId: '2cb71b3468d649789163ea2e212b541e',
        roleName: 'Application Evaluator',
        roleDescription: 'Evaluates applications and views policy violation summary results.',
        membersByOwner: [
          {
            ownerId: '6a9be797916a4c12a6f8d8bc410aba86',
            ownerName: 'Sandbox Organization',
            ownerType: 'organization',
            members: [],
          },
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
            ownerId: '6a9be797916a4c12a6f8d8bc410aba86',
            ownerName: 'Sandbox Organization',
            ownerType: 'organization',
            members: [],
          },
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
            ownerId: '6a9be797916a4c12a6f8d8bc410aba86',
            ownerName: 'Sandbox Organization',
            ownerType: 'organization',
            members: [],
          },
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
            ownerId: '6a9be797916a4c12a6f8d8bc410aba86',
            ownerName: 'Sandbox Organization',
            ownerType: 'organization',
            members: [],
          },
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
            ownerId: '6a9be797916a4c12a6f8d8bc410aba86',
            ownerName: 'Sandbox Organization',
            ownerType: 'organization',
            members: [],
          },
          {
            ownerId: 'ROOT_ORGANIZATION_ID',
            ownerName: 'Root Organization',
            ownerType: 'organization',
            members: [],
          },
        ],
      },
      {
        roleId: 'd52645f5b7d141f58d71fbe1375397dd',
        roleName: 'Test Waivers Administrator',
        roleDescription: 'Test',
        membersByOwner: [
          {
            ownerId: '6a9be797916a4c12a6f8d8bc410aba86',
            ownerName: 'Sandbox Organization',
            ownerType: 'organization',
            members: [],
          },
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

  afterEach(() => {
    cleanup();
  });

  beforeEach(() => {
    mock = axiosMockAdapter();
  });

  describe('Rendering inherited data', () => {
    it('renders list', async () => {
      const customPreloadedState = {
        ...preloadedState,
        router: {
          currentParams: {
            organizationId: '6a9be797916a4c12a6f8d8bc410aba86',
          },
          currentState: {
            name: 'management.view.organization',
            url: '/organization/{organizationId}',
          },
        },
        orgsAndPolicies: {
          access: {
            loading: false,
            serverData: {
              membersByRole: [
                {
                  roleId: '2cb71b3468d649789163ea2e212b541e',
                  roleName: 'Application Evaluator',
                  roleDescription: 'Evaluates applications and views policy violation summary results.',
                  membersByOwner: [
                    {
                      ownerId: '6a9be797916a4c12a6f8d8bc410aba86',
                      ownerName: 'Sandbox Organization',
                      ownerType: 'organization',
                      members: [],
                    },
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
                      ownerId: '6a9be797916a4c12a6f8d8bc410aba86',
                      ownerName: 'Sandbox Organization',
                      ownerType: 'organization',
                      members: [],
                    },
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
                      ownerId: '6a9be797916a4c12a6f8d8bc410aba86',
                      ownerName: 'Sandbox Organization',
                      ownerType: 'organization',
                      members: [],
                    },
                    {
                      ownerId: 'ROOT_ORGANIZATION_ID',
                      ownerName: 'Root Organization',
                      ownerType: 'organization',
                      members: [
                        {
                          type: 'USER',
                          internalName: 'testuser3',
                          displayName: 'Test User 3',
                          email: 'testuser3@test.com',
                          realm: 'IQ Server',
                        },
                      ],
                    },
                  ],
                },
                {
                  roleId: '0df46317c031440795007f4ce9c7f002',
                  roleName: 'Legal Reviewer',
                  roleDescription: 'Reviews legal obligations for component licenses.',
                  membersByOwner: [
                    {
                      ownerId: '6a9be797916a4c12a6f8d8bc410aba86',
                      ownerName: 'Sandbox Organization',
                      ownerType: 'organization',
                      members: [],
                    },
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
                      ownerId: '6a9be797916a4c12a6f8d8bc410aba86',
                      ownerName: 'Sandbox Organization',
                      ownerType: 'organization',
                      members: [],
                    },
                    {
                      ownerId: 'ROOT_ORGANIZATION_ID',
                      ownerName: 'Root Organization',
                      ownerType: 'organization',
                      members: [],
                    },
                  ],
                },
                {
                  roleId: 'd52645f5b7d141f58d71fbe1375397dd',
                  roleName: 'Test Waivers Administrator',
                  roleDescription: 'Test',
                  membersByOwner: [
                    {
                      ownerId: '6a9be797916a4c12a6f8d8bc410aba86',
                      ownerName: 'Sandbox Organization',
                      ownerType: 'organization',
                      members: [],
                    },
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
          },
        },
      };

      mock
        .onGet(getAccessPageRolesUrl('organization', '6a9be797916a4c12a6f8d8bc410aba86'))
        .reply(200, rolesMockDataOrganizationInherited);

      render(<AccessTile />, { preloadedState: customPreloadedState });

      await waitFor(() => {
        expect(screen.getByTestId('repositories_access')).toBeVisible();
        expect(screen.getByTestId('add-role-button')).toBeVisible();
        expect(screen.queryByText('Add a Role')).toBeVisible();
        expect(screen.queryByText('Inherited from Root Organization')).toBeInTheDocument();

        const addRoleButton = screen.getByText('Add a Role').closest('a');
        expect(addRoleButton).not.toBeNull();
        expect(addRoleButton).not.toHaveClass('disabled');
      });
    });

    it('renders disabled button', async () => {
      jest.spyOn(accessSelectors, 'selectRolesWithoutLocalMembersExist').mockReturnValue(false);

      mock
        .onGet(getAccessPageRolesUrl('organization', '6a9be797916a4c12a6f8d8bc410aba86'))
        .reply(200, rolesMockDataOrganizationInherited);

      render(<AccessTile />, { preloadedState });

      await waitFor(() => {
        expect(screen.getByTestId('repositories_access')).toBeVisible();
        expect(screen.getByTestId('add-role-button')).toBeVisible();
        expect(screen.queryByText('Add a Role')).toBeVisible();

        const addRoleButton = screen.getByText('Add a Role').closest('a');
        expect(addRoleButton).toHaveClass('disabled');
      });
    });
  });

  describe('Rendering data', () => {
    it('renders empty local list', async () => {
      const customPreloadedState = {
        ...preloadedState,
        router: {
          currentParams: {
            applicationPublicId: 'sandbox-application',
          },
          currentState: {
            name: 'management.view.application',
            url: '/application/{applicationPublicId}',
          },
        },
        orgsAndPolicies: {
          access: {
            loading: false,
            serverData: {
              membersByRole: [
                {
                  roleId: '2cb71b3468d649789163ea2e212b541e',
                  roleName: 'Application Evaluator',
                  roleDescription: 'Evaluates applications and views policy violation summary results.',
                  membersByOwner: [
                    {
                      ownerId: 'sandbox-application',
                      ownerName: 'Sandbox Application',
                      ownerType: 'application',
                      members: [],
                    },
                    {
                      ownerId: '6a9be797916a4c12a6f8d8bc410aba86',
                      ownerName: 'Sandbox Organization',
                      ownerType: 'organization',
                      members: [],
                    },
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
                      ownerId: 'sandbox-application',
                      ownerName: 'Sandbox Application',
                      ownerType: 'application',
                      members: [],
                    },
                    {
                      ownerId: '6a9be797916a4c12a6f8d8bc410aba86',
                      ownerName: 'Sandbox Organization',
                      ownerType: 'organization',
                      members: [],
                    },
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
                      ownerId: 'sandbox-application',
                      ownerName: 'Sandbox Application',
                      ownerType: 'application',
                      members: [
                        {
                          type: 'USER',
                          internalName: 'testuser1',
                          displayName: 'Test User 1',
                          email: 'testuser1@test.com',
                          realm: 'IQ Server',
                        },
                      ],
                    },
                    {
                      ownerId: '6a9be797916a4c12a6f8d8bc410aba86',
                      ownerName: 'Sandbox Organization',
                      ownerType: 'organization',
                      members: [],
                    },
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
                      ownerId: 'sandbox-application',
                      ownerName: 'Sandbox Application',
                      ownerType: 'application',
                      members: [],
                    },
                    {
                      ownerId: '6a9be797916a4c12a6f8d8bc410aba86',
                      ownerName: 'Sandbox Organization',
                      ownerType: 'organization',
                      members: [],
                    },
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
                      ownerId: 'sandbox-application',
                      ownerName: 'Sandbox Application',
                      ownerType: 'application',
                      members: [],
                    },
                    {
                      ownerId: '6a9be797916a4c12a6f8d8bc410aba86',
                      ownerName: 'Sandbox Organization',
                      ownerType: 'organization',
                      members: [],
                    },
                    {
                      ownerId: 'ROOT_ORGANIZATION_ID',
                      ownerName: 'Root Organization',
                      ownerType: 'organization',
                      members: [],
                    },
                  ],
                },
                {
                  roleId: 'd52645f5b7d141f58d71fbe1375397dd',
                  roleName: 'Test Waivers Administrator',
                  roleDescription: 'Test',
                  membersByOwner: [
                    {
                      ownerId: 'sandbox-application',
                      ownerName: 'Sandbox Application',
                      ownerType: 'application',
                      members: [],
                    },
                    {
                      ownerId: '6a9be797916a4c12a6f8d8bc410aba86',
                      ownerName: 'Sandbox Organization',
                      ownerType: 'organization',
                      members: [],
                    },
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
          },
        },
      };

      mock.onGet(getAccessPageRolesUrl('application', 'sandbox-application')).reply(200, rolesMockDataApplication);

      render(<AccessTile />, { preloadedState: customPreloadedState });

      await waitFor(() => {
        expect(screen.getByTestId('repositories_access')).toBeVisible();
        expect(screen.getByTestId('add-role-button')).toBeVisible();
        expect(screen.queryByText('Add a Role')).toBeVisible();
        expect(screen.queryByText('No local access configured.')).not.toBeInTheDocument();

        const addRoleButton = screen.getByText('Add a Role').closest('a');
        expect(addRoleButton).not.toBeNull();
        expect(addRoleButton).not.toHaveClass('disabled');
      });
    });
  });

  describe('Rendering inherited lists', () => {
    it('renders component with no local access and no rendered inherited access', async () => {
      mock
        .onGet(getAccessPageRolesUrl('organization', '6a9be797916a4c12a6f8d8bc410aba86'))
        .reply(200, rolesMockDataOrganizationWithLocallist);

      render(<AccessTile />, { preloadedState });

      await waitFor(() => {
        expect(screen.getByTestId('repositories_access')).toBeVisible();
        expect(screen.getByTestId('add-role-button')).toBeVisible();
        expect(screen.queryByText('Add a Role')).toBeVisible();
        expect(screen.queryByText('No local access configured.')).toBeInTheDocument();
        expect(screen.queryByText('Inherited from Root Organization')).not.toBeInTheDocument();

        const addRoleButton = screen.getByText('Add a Role').closest('a');
        expect(addRoleButton).not.toBeNull();
        expect(addRoleButton).not.toHaveClass('disabled');
      });
    });
  });

  const rolesMockDataRepositoryContainerInheritedAndLocal = {
    membersByRole: [
      {
        roleId: '2cb71b3468d649789163ea2e212b541e',
        roleName: 'Application Evaluator',
        roleDescription: 'Evaluates applications and views policy violation summary results.',
        membersByOwner: [
          {
            ownerId: 'REPOSITORY_CONTAINER_ID',
            ownerName: 'Repository Managers',
            ownerType: 'repository_container',
            members: [
              {
                type: 'USER',
                internalName: 'testuser1',
                displayName: 'Test User 1',
                email: 'testuser1@test.com',
                realm: 'IQ Server',
              },
            ],
          },
          {
            ownerId: 'ROOT_ORGANIZATION_ID',
            ownerName: 'Root Organization',
            ownerType: 'organization',
            members: [
              {
                type: 'USER',
                internalName: 'testuser1',
                displayName: 'Test User 1',
                email: 'testuser1@test.com',
                realm: 'IQ Server',
              },
            ],
          },
        ],
      },
    ],
  };

  let repositoryContainerPreloadedState = {
    router: {
      currentParams: {
        repositoryContainerId: 'REPOSITORY_CONTAINER_ID',
      },
      currentState: {
        name: 'management.view.repository_container',
        url: '/respository_container/{repositoryContainerId}',
      },
    },
    orgsAndPolicies: {
      access: {
        loading: false,
        serverData: {
          membersByRole: [
            {
              roleId: '2cb71b3468d649789163ea2e212b541e',
              roleName: 'Application Evaluator',
              roleDescription: 'Evaluates applications and views policy violation summary results.',
              membersByOwner: [
                {
                  ownerId: 'REPOSITORY_CONTAINER_ID',
                  ownerName: 'Repository Managers',
                  ownerType: 'repository_container',
                  members: [
                    {
                      type: 'USER',
                      internalName: 'testuser1',
                      displayName: 'Test User 1',
                      email: 'testuser1@test.com',
                      realm: 'IQ Server',
                    },
                  ],
                },
                {
                  ownerId: 'ROOT_ORGANIZATION_ID',
                  ownerName: 'Root Organization',
                  ownerType: 'organization',
                  members: [
                    {
                      type: 'USER',
                      internalName: 'testuser1',
                      displayName: 'Test User 1',
                      email: 'testuser1@test.com',
                      realm: 'IQ Server',
                    },
                  ],
                },
              ],
            },
          ],
          groupSearchEnabled: true,
        },
      },
    },
  };

  describe('Rendering inherited and local data', () => {
    it('renders component with local and inherited access', async () => {
      mock.onGet(getRepositoryContainerRoleMappingUrl()).reply(200, rolesMockDataRepositoryContainerInheritedAndLocal);

      mock.onGet(getRepositoryContainer()).reply(200, {
        id: 'REPOSITORY_CONTAINER_ID',
        name: 'Repository Managers',
      });

      render(<AccessTile />, { preloadedState: repositoryContainerPreloadedState });

      await waitFor(() => {
        expect(screen.getByTestId('repositories_access')).toBeVisible();
        expect(screen.queryByText('Local to Repository Managers')).toBeVisible();
        expect(screen.queryByText('Inherited from Root Organization')).toBeVisible();
      });
    });
  });
});
