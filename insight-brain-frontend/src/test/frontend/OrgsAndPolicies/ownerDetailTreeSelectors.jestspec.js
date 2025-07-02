/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  selectOwnerDetailTreeSlice,
  selectLoading,
  selectLoadError,
  selectOwnerDetails,
  selectRolesWithoutLocalMembersExist,
} from 'MainRoot/OrgsAndPolicies/ownerDetailTreeSelectors';
import { selectOrgsAndPoliciesSlice } from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesSelectors';

describe('ownerDetailTreeSelectors', () => {
  describe('selectOwnerDetailTreeSlice', () => {
    it('is composed from the following selector', () => {
      expect(selectOwnerDetailTreeSlice.dependencies).toEqual([selectOrgsAndPoliciesSlice]);
    });

    it('selects OwnerDetailTree', () => {
      const orgsAndPoliciesSlice = {
        ownerDetailTree: null,
      };

      const actualSelection = selectOwnerDetailTreeSlice.resultFunc(orgsAndPoliciesSlice);

      expect(actualSelection).toBe(null);
    });
  });

  describe('selectLoading', () => {
    it('is composed from the following selector', () => {
      expect(selectLoading.dependencies).toEqual([selectOwnerDetailTreeSlice]);
    });

    it('selects loading', () => {
      const OwnerDetailTreeSlice = {
        loading: true,
      };

      const actualSelection = selectLoading.resultFunc(OwnerDetailTreeSlice);

      expect(actualSelection).toBe(true);
    });
  });

  describe('selectLoadError', () => {
    it('is composed from the following selector', () => {
      expect(selectLoadError.dependencies).toEqual([selectOwnerDetailTreeSlice]);
    });

    it('selects loadError', () => {
      const OwnerDetailTreeSlice = {
        loadError: 'error',
      };

      const actualSelection = selectLoadError.resultFunc(OwnerDetailTreeSlice);

      expect(actualSelection).toBe('error');
    });
  });

  describe('selectOwnerDetails', () => {
    it('is composed from the following selector', () => {
      expect(selectOwnerDetails.dependencies).toEqual([selectOwnerDetailTreeSlice]);
    });

    it('selects details', () => {
      const ownerDetails = {
        otherKeys: 'other',
        roles: {
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
              roleId: '16995b4e0ee549d1a3678610adafae9d',
              roleName: 'Custom Role',
              roleDescription: 'Custom Role',
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
          groupSearchEnabled: true,
        },
      };
      const OwnerDetailTreeSlice = {
        ownerDetails,
      };

      const actualSelection = selectOwnerDetails.resultFunc(OwnerDetailTreeSlice);
      expect(actualSelection).toEqual({ ...ownerDetails, roles: [] });
    });
  });

  describe('selectOwnerDetails', () => {
    it('is composed from the following selector', () => {
      expect(selectRolesWithoutLocalMembersExist.dependencies).toEqual([selectOwnerDetailTreeSlice]);
    });

    it('selects true if there are roles without local members', () => {
      const ownerDetails = {
        otherKeys: 'other',
        roles: {
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
              roleId: '16995b4e0ee549d1a3678610adafae9d',
              roleName: 'Custom Role',
              roleDescription: 'Custom Role',
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
          groupSearchEnabled: true,
        },
      };
      const OwnerDetailTreeSlice = {
        ownerDetails,
      };

      const actualSelection = selectRolesWithoutLocalMembersExist.resultFunc(OwnerDetailTreeSlice);
      expect(actualSelection).toBe(true);
    });

    it('selects false if there arenot  roles without local members', () => {
      const ownerDetails = {
        otherKeys: 'other',
        roles: {
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
                  members: ['a member'],
                },
              ],
            },
          ],
          groupSearchEnabled: true,
        },
      };
      const OwnerDetailTreeSlice = {
        ownerDetails,
      };

      const actualSelection = selectRolesWithoutLocalMembersExist.resultFunc(OwnerDetailTreeSlice);
      expect(actualSelection).toBe(false);
    });
  });
});
