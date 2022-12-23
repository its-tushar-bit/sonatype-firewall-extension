/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen } from 'TestRoot/SpecUtil';
import AccessTile from 'MainRoot/react/accessTile/AccessTile';
import * as routerSelectors from 'MainRoot/reduxUiRouter/routerSelectors';
import * as accessSelectors from 'MainRoot/OrgsAndPolicies/access/accessSelectors';
import * as ownerPolicySelectors from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesSelectors';
import * as RouterStateContext from 'MainRoot/react/RouterStateContext';

describe('AccessTile', () => {
  let renderComponent;
  const extMemberWithLocalRoles = [
    {
      isInherited: false,
      ownerId: 'TEST',
      ownerName: 'Test local',
      owner_type: 'repository_container',
      roles: [
        {
          roleId: 1,
          roleName: 'Test Role1',
          members: [
            {
              type: 'USER',
              displayName: 'user1',
            },
            {
              type: 'GROUP',
              displayName: 'group1',
            },
          ],
        },
      ],
    },
    {
      isInherited: true,
      ownerId: 'TEST inherited',
      ownerName: 'TEST inherited',
      owner_type: 'repository_container',
      roles: [
        {
          roleId: 2,
          roleName: 'Test Role2',
          members: [
            {
              type: 'USER',
              displayName: 'user2',
            },
            {
              type: 'GROUP',
              displayName: 'group2',
            },
          ],
        },
      ],
    },
  ];

  const extMemberWithInheritedRoles = [
    {
      isInherited: false,
      ownerId: 'TEST',
      ownerName: 'Test local',
      owner_type: 'repository_container',
      roles: [
        {
          roleId: 1,
          roleName: 'Test Role1',
          members: [
            {
              type: 'USER',
              displayName: 'user1',
            },
            {
              type: 'GROUP',
              displayName: 'group1',
            },
          ],
        },
      ],
    },
    {
      isInherited: true,
      ownerId: 'ROOT_ORGANIZATION',
      ownerName: 'Root Organization',
      owner_type: 'organization',
      roles: [
        {
          roleId: 2,
          roleName: 'Test Role2',
          members: [
            {
              type: 'USER',
              displayName: 'user2',
            },
          ],
        },
      ],
    },
    {
      isInherited: true,
      ownerId: 'abc123',
      ownerName: 'myTestOrg',
      owner_type: 'organization',
      roles: [
        {
          roleId: 2,
          roleName: 'Test Role3',
          members: [
            {
              type: 'USER',
              displayName: 'user3',
            },
          ],
        },
      ],
    },
  ];

  let extMembers;
  beforeEach(() => {
    extMembers = spyOn(accessSelectors, 'selectExtendedMembersByRole').and.returnValue([]);
    spyOn(ownerPolicySelectors, 'selectSelectedOwnerName').and.returnValue('Test Owner');
    renderComponent = () => render(<AccessTile />);
  });

  describe('when data are being loaded', () => {
    it('renders component with no local access and no rendered inherited access', () => {
      renderComponent();
      expect(screen.queryByText('Add a Role')).toBeInTheDocument();
      expect(screen.queryByText('No local access configured.')).toBeInTheDocument();
      expect(screen.queryByText('Inherited from Root Organization')).not.toBeInTheDocument();
    });
  });

  describe('when rendering add role button', () => {
    it('add role button is rendered with href parameter and enabled', () => {
      spyOn(accessSelectors, 'selectRolesWithoutLocalMembersExist').and.returnValue(true);

      spyOn(RouterStateContext, 'useRouterState').and.returnValue({
        href: jasmine.createSpy('useRouterState.href').and.returnValue('test'),
      });

      const selectRouterStateSpy = spyOn(routerSelectors, 'selectRouterState').and.callFake(() => {
        return { name: 'test/application', data: {} };
      });

      renderComponent();
      const addRoleButton = screen.getByRole('link', { name: 'Add a Role' });
      expect(addRoleButton).not.toHaveClass('disabled');
      expect(addRoleButton).toHaveAttribute('href', 'test');
      expect(selectRouterStateSpy).toHaveBeenCalled();
    });

    it('add role button is disabled', () => {
      renderComponent();
      const addRoleButton = screen.getByText('Add a Role').closest('a');
      expect(addRoleButton).not.toHaveAttribute('href');
      expect(addRoleButton).toHaveClassName('disabled');
    });
  });

  describe('Rendering data', () => {
    it('renders local list and inherited list', () => {
      spyOn(routerSelectors, 'selectRouterState').and.callFake(() => {
        return { name: 'test/application', data: {} };
      });

      extMembers.and.returnValue(extMemberWithLocalRoles);
      renderComponent();

      expect(screen.queryByText('Test Role1')).toBeInTheDocument();
      expect(screen.queryByText('user1')).toBeInTheDocument();
      expect(screen.queryByText('group1')).toBeInTheDocument();

      expect(screen.getByText('Inherited from TEST inherited')).toBeInTheDocument();
      expect(screen.queryByText('Test Role2')).toBeInTheDocument();
      expect(screen.queryByText('user2')).toBeInTheDocument();
      expect(screen.queryByText('group2')).toBeInTheDocument();
    });
  });

  describe('Rendering inherited lists', () => {
    beforeEach(() => {
      extMembers.and.returnValue(extMemberWithInheritedRoles);
      spyOn(routerSelectors, 'selectRouterState').and.callFake(() => {
        return { name: 'test/application', data: { name: 'myAppTest' } };
      });
      renderComponent();
    });

    it('checks if multiple inherited organizations are rendered', () => {
      expect(screen.getByText('Inherited from Root Organization')).toBeInTheDocument();
      expect(screen.getByText('Inherited from myTestOrg')).toBeInTheDocument();
    });
  });
});
