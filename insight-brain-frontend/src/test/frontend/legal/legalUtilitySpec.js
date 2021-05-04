/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { backToComponentOverviewUrl } from '../../../main/frontend/legal/legalUtility';

const { isScopeOverride, getLicenseThreatGroupsFromLicense } = require('../../../main/frontend/legal/legalUtility');
describe('legalUtility', function () {
  const availableScopeValues = [{ id: 'appId' }, { id: 'orgId' }, { id: 'ROOT_ORGANIZATION_ID' }];

  describe('isScopeOverride', function () {
    it('returns true if the originalOwnerId index is greater than the ownerId index', function () {
      expect(isScopeOverride('ROOT_ORGANIZATION_ID', 'orgId', availableScopeValues)).toBeTruthy();
      expect(isScopeOverride('ROOT_ORGANIZATION_ID', 'appId', availableScopeValues)).toBeTruthy();
      expect(isScopeOverride('orgId', 'appId', availableScopeValues)).toBeTruthy();
    });

    it('returns false if the originalOwnerId index is less than or equal to the ownerId index', function () {
      expect(isScopeOverride('ROOT_ORGANIZATION_ID', 'ROOT_ORGANIZATION_ID', availableScopeValues)).toBeFalsy();
      expect(isScopeOverride('orgId', 'orgId', availableScopeValues)).toBeFalsy();
      expect(isScopeOverride('orgId', 'ROOT_ORGANIZATION_ID', availableScopeValues)).toBeFalsy();
      expect(isScopeOverride('appId', 'appId', availableScopeValues)).toBeFalsy();
      expect(isScopeOverride('appId', 'orgId', availableScopeValues)).toBeFalsy();
      expect(isScopeOverride('appId', 'ROOT_ORGANIZATION_ID', availableScopeValues)).toBeFalsy();
    });

    it('returns true if the originalOwnerId index is greater than the ownerId index', function () {
      expect(isScopeOverride('ROOT_ORGANIZATION_ID', 'orgId', availableScopeValues)).toBeTruthy();
      expect(isScopeOverride('ROOT_ORGANIZATION_ID', 'appId', availableScopeValues)).toBeTruthy();
      expect(isScopeOverride('orgId', 'appId', availableScopeValues)).toBeTruthy();
    });
  });

  describe('getLicenseThreatGroupsFromLicense', function () {
    it('returns the license threat groups in a license object', function () {
      expect(
        getLicenseThreatGroupsFromLicense({
          licenseThreatGroups: [{ licenseThreatGroupName: 'group1' }, { licenseThreatGroupName: 'group2' }],
        })
      ).toEqual([{ licenseThreatGroupName: 'group1' }, { licenseThreatGroupName: 'group2' }]);
    });

    it('returns a default "No License Threat Group (LTG)" when no groups are present', function () {
      expect(getLicenseThreatGroupsFromLicense({ licenseThreatGroups: [] })).toEqual([
        { licenseThreatGroupName: 'No LTG Assigned' },
      ]);
      expect(getLicenseThreatGroupsFromLicense({})).toEqual([{ licenseThreatGroupName: 'No LTG Assigned' }]);
    });
  });

  describe('backToComponentOverviewUrl', function () {
    const state = {
      href: (stateName, params) => {
        return { name: stateName, params };
      },
      get: (state) => state,
    };

    it('returns a state for org component overview', function () {
      const url = backToComponentOverviewUrl(state, 'organization', 'org', undefined, 'hash');
      expect(url).toEqual({
        name: 'legal.organizationComponentOverview',
        params: {
          organizationId: 'org',
          hash: 'hash',
        },
      });
    });

    it('returns a state for application component overview', function () {
      const url = backToComponentOverviewUrl(state, 'application', 'app', undefined, 'hash');
      expect(url).toEqual({
        name: 'legal.applicationComponentOverview',
        params: {
          applicationPublicId: 'app',
          hash: 'hash',
        },
      });
    });

    it('returns a state for application component overview for a given stage', function () {
      const url = backToComponentOverviewUrl(state, 'application', 'app', 'build', 'hash');
      expect(url).toEqual({
        name: 'legal.applicationStageTypeComponentOverview',
        params: {
          applicationPublicId: 'app',
          hash: 'hash',
          stageTypeId: 'build',
        },
      });
    });
  });
});
