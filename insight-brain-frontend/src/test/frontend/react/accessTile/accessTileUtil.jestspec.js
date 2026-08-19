/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { findOutIfRolesWithoutLocalMembersExists } from 'MainRoot/react/accessTile/accessTileUtil';

describe('accessTileUtil', () => {
  describe('findOutIfRolesWithoutLocalMembersExists', () => {
    it('returns true when no members exist in role', () => {
      const membersByRoles = [
        {
          membersByOwner: [{ id: 2, members: [] }],
        },
      ];
      const result = findOutIfRolesWithoutLocalMembersExists(membersByRoles);
      expect(result).toBe(true);
    });

    it('returns false when at least one members exist in  role', () => {
      const membersByRoles = [
        {
          membersByOwner: [{ id: 2, members: ['test'] }],
        },
      ];
      const result = findOutIfRolesWithoutLocalMembersExists(membersByRoles);
      expect(result).toBe(false);
    });
  });
});
