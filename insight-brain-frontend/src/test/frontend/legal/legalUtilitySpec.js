/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
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
});
