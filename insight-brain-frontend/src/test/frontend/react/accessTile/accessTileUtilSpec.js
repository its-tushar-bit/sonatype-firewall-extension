/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import {
  findOutIfRolesWithoutLocalMembersExists,
  reformatRouteStateParams,
} from 'MainRoot/react/accessTile/accessTileUtil';

describe('accessTileUtil', () => {
  describe('reformatRouteStateParams', () => {
    it('validates a application without params', () => {
      const routerState = {
        name: 'test.application',
        data: {},
      };
      const result = reformatRouteStateParams(routerState, null);
      expect(result.to).toBe('management.edit.application.add-access');
      expect(result.params).toEqual({});
    });

    it('validates repositories with params', () => {
      const customParams = { repositoryContainerId: 'REPOSITORY_CONTAINER_ID' };
      const routerState = {
        name: 'test.repository_container',
        data: {
          repositoryContainerId: 'REPOSITORY_CONTAINER_ID',
        },
      };
      const result = reformatRouteStateParams(routerState, customParams);
      expect(result.to).toBe('management.edit.repository_container.add-access');
      expect(result.params).toEqual(customParams);
    });

    it('validates organization with params passed as argument', () => {
      const customParams = { name: 'test' };
      const routerState = {
        name: 'test.abc',
        data: {
          organizationId: customParams,
        },
      };
      const result = reformatRouteStateParams(routerState, customParams);
      expect(result.to).toBe('management.edit.organization.add-access');
      expect(result.params).toEqual(customParams);
    });
  });

  describe('findOutIfRolesWithoutLocalMembersExists', () => {
    it('returns true when no members exist in role', () => {
      const membersByRoles = [
        {
          membersByOwner: [{ id: 2, members: [] }],
        },
      ];
      const result = findOutIfRolesWithoutLocalMembersExists(membersByRoles);
      expect(result).toBeTrue();
    });

    it('returns false when at least one members exist in  role', () => {
      const membersByRoles = [
        {
          membersByOwner: [{ id: 2, members: ['test'] }],
        },
      ];
      const result = findOutIfRolesWithoutLocalMembersExists(membersByRoles);
      expect(result).toBeFalse();
    });
  });
});
