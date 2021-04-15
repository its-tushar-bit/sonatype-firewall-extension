/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
const {
  isScopeOverride,
} = require('../../../main/frontend/legal/legalUtility');
describe('legalUtility', function () {
  const availableScopeValues = [
    { id: 'appId' },
    { id: 'orgId' },
    { id: 'ROOT_ORGANIZATION_ID' },
  ];

  it('returns true if the originalOwnerId index is greater than the ownerId index', function () {
    expect(
      isScopeOverride('ROOT_ORGANIZATION_ID', 'orgId', availableScopeValues)
    ).toBeTruthy();
    expect(
      isScopeOverride('ROOT_ORGANIZATION_ID', 'appId', availableScopeValues)
    ).toBeTruthy();
    expect(
      isScopeOverride('orgId', 'appId', availableScopeValues)
    ).toBeTruthy();
  });

  it('returns false if the originalOwnerId index is less than or equal to the ownerId index', function () {
    expect(
      isScopeOverride(
        'ROOT_ORGANIZATION_ID',
        'ROOT_ORGANIZATION_ID',
        availableScopeValues
      )
    ).toBeFalsy();
    expect(isScopeOverride('orgId', 'orgId', availableScopeValues)).toBeFalsy();
    expect(
      isScopeOverride('orgId', 'ROOT_ORGANIZATION_ID', availableScopeValues)
    ).toBeFalsy();
    expect(isScopeOverride('appId', 'appId', availableScopeValues)).toBeFalsy();
    expect(isScopeOverride('appId', 'orgId', availableScopeValues)).toBeFalsy();
    expect(
      isScopeOverride('appId', 'ROOT_ORGANIZATION_ID', availableScopeValues)
    ).toBeFalsy();
  });
});
