/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  findSingleLicenseIndex,
  findSimilarLicenseIndex,
  formatLicenseMeta,
  backToComponentOverviewUrl,
  createSubtitle,
  getStatusName,
  createScopeOption,
} from 'MainRoot/legal/legalUtility';

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

  describe('getComponentEffectiveLicenseNames', function () {
    const licenseLegalMetadata = [
      {
        licenseId: 'License-1.0',
        licenseName: 'License-1.0 Name',
        isMulti: false,
        singleLicenseIds: [],
      },
      {
        licenseId: 'License-2.0',
        licenseName: 'License-2.0 Name',
        isMulti: false,
        singleLicenseIds: [],
      },
      {
        licenseId: 'License-1.0-License-2.0',
        licenseName: 'License-1.0 or License-2.0',
        isMulti: true,
        singleLicenseIds: ['License-1.0', 'License-2.0'],
      },
      {
        licenseId: 'LicenseNotOnComponent',
        licenseName: 'LicenseNotOnComponent Name',
        isMulti: false,
        singleLicenseIds: [],
      },
    ];

    const component = {
      licenseLegalData: {
        effectiveLicenses: ['License-1.0', 'License-2.0', 'License-1.0-License-2.0'],
      },
    };
    it('returns license names, isMulti flag and singleLicenseIds', function () {
      expect(formatLicenseMeta('effectiveLicenses', component, licenseLegalMetadata)).toEqual([
        {
          licenseId: 'License-1.0',
          licenseName: 'License-1.0 Name',
          isMulti: false,
          singleLicenseIds: [],
        },
        {
          licenseId: 'License-1.0-License-2.0',
          licenseName: 'License-1.0 or License-2.0',
          isMulti: true,
          singleLicenseIds: ['License-1.0', 'License-2.0'],
        },
        {
          licenseId: 'License-2.0',
          licenseName: 'License-2.0 Name',
          isMulti: false,
          singleLicenseIds: [],
        },
      ]);
    });
  });

  describe('findSingleLicenseIndex', function () {
    const licenseLegalMetadata = [
      {
        licenseId: 'License-1.0',
        licenseName: 'License-1.0 Name',
        isMulti: false,
        singleLicenseIds: [],
      },
      {
        licenseId: 'License-2.0',
        licenseName: 'License-2.0 Name',
        isMulti: false,
        singleLicenseIds: [],
      },
      {
        licenseId: 'License-1.0-License-2.0',
        licenseName: 'License-1.0 Name or License-2.0 Name',
        isMulti: true,
        singleLicenseIds: ['License-1.0', 'License-2.0'],
      },
    ];

    it('return single license index', function () {
      expect(findSingleLicenseIndex('License-1.0 Name', licenseLegalMetadata)).toEqual(0);
      expect(findSingleLicenseIndex('License-2.0 Name', licenseLegalMetadata)).toEqual(1);
    });

    it('return multi license index', function () {
      expect(findSingleLicenseIndex('License-1.0 Name or License-2.0 Name', licenseLegalMetadata)).toEqual(0);
    });
  });

  describe('findSimilarLicenseIndex', function () {
    const licenseLegalMetadata = [
      {
        licenseId: 'License-1.0',
        licenseName: 'License-1.0 Name',
        isMulti: false,
        singleLicenseIds: [],
      },
      {
        licenseId: 'License-2.0',
        licenseName: 'License-2.0 Name',
        isMulti: false,
        singleLicenseIds: [],
      },
      {
        licenseId: 'Apache-2.0',
        licenseName: 'Apache-2.0',
        isMulti: false,
        singleLicenseIds: [],
      },
      {
        licenseId: 'GNU-UNSPECIFIED',
        licenseName: 'GNU-UNSPECIFIED',
        isMulti: false,
        singleLicenseIds: [],
      },
      {
        licenseId: 'GPL-2.0-with-classpath-exception',
        licenseName: 'GPL-2.0-with-classpath-exception',
        isMulti: false,
        singleLicenseIds: [],
      },
      {
        licenseId: 'License-1.0-License-2.0-Apache-2.0+-GNU-2-Apache-3.1-SuperDuperLicense-1.0',
        licenseName:
          'License-1.0 Name or License-2.0 Name or Apache-2.0+, GNU-2 or Apache-3.1 or GPL-2.0-CPE or SuperDuperLicense-1.0',
        isMulti: true,
        singleLicenseIds: ['License-2.0', 'License-1.0, Apache-2.0', 'Apache-3.1', 'GPL-2.0-CPE', 'GNU-2'],
      },
    ];

    it('return multiple license index when the license names matches exactly', function () {
      expect(findSimilarLicenseIndex('License-1.0 Name', licenseLegalMetadata)).toEqual(0);
      expect(findSimilarLicenseIndex('License-2.0 Name', licenseLegalMetadata)).toEqual(1);
    });

    it('return multiple license index when the license names matches by name and major version', function () {
      expect(findSimilarLicenseIndex('Apache-2.0+', licenseLegalMetadata)).toEqual(2);
      expect(findSimilarLicenseIndex('GPL-2.0-CPE', licenseLegalMetadata)).toEqual(4);
    });

    it('return multiple license index when the license names matches just by name', function () {
      expect(findSimilarLicenseIndex('Apache-3.1', licenseLegalMetadata)).toEqual(2);
      expect(findSimilarLicenseIndex('GNU-2', licenseLegalMetadata)).toEqual(3);
    });

    it('return multiple license index as 0 when the license name does not match', function () {
      expect(findSimilarLicenseIndex('SuperDuperLicense-1.0', licenseLegalMetadata)).toEqual(0);
    });
  });

  describe('backToComponentOverviewUrl', function () {
    const state = {
      href: (stateName, params) => {
        return { name: stateName, params };
      },
      get: (state) => state,
    };

    const runBackToComponentOverviewUrlTests = (isSbomManager) => {
      const statePrefix = isSbomManager ? 'sbomManager.legal' : 'legal';

      it('returns a state for org component overview by hash', function () {
        const url = backToComponentOverviewUrl(
          state,
          'organization',
          'org',
          undefined,
          'hash',
          undefined,
          undefined,
          isSbomManager
        );
        expect(url).toEqual({
          name: `${statePrefix}.organizationComponentOverview`,
          params: {
            organizationId: 'org',
            hash: 'hash',
          },
        });
      });

      it('returns a state for org component overview by component identifier', function () {
        const url = backToComponentOverviewUrl(
          state,
          'organization',
          'org',
          undefined,
          undefined,
          'compIdentifier',
          undefined,
          isSbomManager
        );
        expect(url).toEqual({
          name: `${statePrefix}.componentOverviewByComponentIdentifier`,
          params: {
            organizationId: 'org',
            componentIdentifier: 'compIdentifier',
          },
        });
      });

      it('returns a state for application component overview by hash', function () {
        const url = backToComponentOverviewUrl(
          state,
          'application',
          'app',
          undefined,
          'hash',
          undefined,
          undefined,
          isSbomManager
        );
        expect(url).toEqual({
          name: `${statePrefix}.applicationComponentOverview`,
          params: {
            applicationPublicId: 'app',
            hash: 'hash',
          },
        });
      });

      it('returns a state for application component overview by component identifier', function () {
        const url = backToComponentOverviewUrl(
          state,
          'application',
          'app',
          undefined,
          undefined,
          'componentIdentifier',
          undefined,
          isSbomManager
        );
        expect(url).toEqual({
          name: `${statePrefix}.componentOverviewByComponentIdentifier`,
          params: {
            applicationPublicId: 'app',
            componentIdentifier: 'componentIdentifier',
          },
        });
      });

      it('returns a state for application component overview for a given stage by hash', function () {
        const url = backToComponentOverviewUrl(
          state,
          'application',
          'app',
          'build',
          'hash',
          undefined,
          undefined,
          isSbomManager
        );
        expect(url).toEqual({
          name: `${statePrefix}.applicationStageTypeComponentOverview`,
          params: {
            applicationPublicId: 'app',
            hash: 'hash',
            stageTypeId: 'build',
          },
        });
      });

      it('returns a state for application component overview for a given stage by component identifier', function () {
        const url = backToComponentOverviewUrl(
          state,
          'application',
          'app',
          'build',
          undefined,
          'componentIdentifier',
          undefined,
          isSbomManager
        );
        expect(url).toEqual({
          name: `${statePrefix}.applicationStageTypeComponentOverview`,
          params: {
            applicationPublicId: 'app',
            componentIdentifier: 'componentIdentifier',
            stageTypeId: 'build',
          },
        });
      });

      it('returns a state for application component overview for hash and scanId by component identifier', function () {
        const url = backToComponentOverviewUrl(
          state,
          'application',
          'app',
          undefined,
          'hash',
          'componentIdentifier',
          'scanId',
          isSbomManager
        );
        expect(url).toEqual({
          name: `${statePrefix}.applicationComponentOverviewByComponentIdentifier`,
          params: {
            componentIdentifier: 'componentIdentifier',
            applicationPublicId: 'app',
            hash: 'hash',
            scanId: 'scanId',
            tabId: 'legal',
          },
        });
      });
    };

    describe('when not isSbomManager', function () {
      runBackToComponentOverviewUrlTests(false);
    });

    describe('when is isSbomManager', function () {
      runBackToComponentOverviewUrlTests(true);
    });
  });

  describe('createScopeOption', function () {
    it('maps the correct options', () => {
      const option = {
        id: 'id',
        label: 'label',
        name: 'name',
      };
      const createdScopeOption = createScopeOption(option);
      expect(createdScopeOption.type).toEqual('option');
      expect(createdScopeOption.key).toEqual('id');
      expect(createdScopeOption.props.value).toEqual('id');
      expect(createdScopeOption.props.children[0]).toEqual('label');
      expect(createdScopeOption.props.children[1]).toEqual(' - ');
      expect(createdScopeOption.props.children[2]).toEqual('name');
    });
  });

  describe('getStatusName', function () {
    it('maps the correct status names', () => {
      expect(getStatusName('OPEN')).toEqual('Open');
      expect(getStatusName('SELECTED')).toEqual('Selected');
      expect(getStatusName('OVERRIDDEN')).toEqual('Overridden');
      expect(getStatusName('ACKNOWLEDGED')).toEqual('Acknowledged');
      expect(getStatusName('CONFIRMED')).toEqual('Confirmed');
      expect(getStatusName('BlahBlahBlah')).toEqual('BlahBlahBlah');
    });
  });

  describe('createSubtitle', function () {
    const rootScope = {
      id: 'ROOT_ORGANIZATION_ID',
      type: 'organization',
      name: 'root org',
    };

    const availableScopes = {
      error: null,
      values: [
        {
          id: 'appId',
          type: 'application',
          name: 'app',
        },
        {
          id: 'orgId',
          type: 'organization',
          name: 'org',
        },
        rootScope,
      ],
    };

    const availableScopesOnlyRoot = {
      error: null,
      values: [rootScope],
    };

    let component = {
      displayName: 'testComponent',
    };

    it('creates a subtitle from root org + app that does not include `root org`', function () {
      let subtitle = createSubtitle(availableScopes, component);
      let subtitleString = JSON.stringify(subtitle);
      expect(subtitleString).not.toContain('root org');
      expect(subtitle.props.children.length).toEqual(3);
      expect(subtitle.props.children[0].props.children[1].props.children).toEqual('org');
      expect(subtitle.props.children[0].props.children[2].props.children).toEqual('/');
      expect(subtitle.props.children[1].props.children[1].props.children).toEqual('app');
      expect(subtitle.props.children[1].props.children[2].props.children).toEqual('/');
      expect(subtitle.props.children[2].props.children[1].props.children).toEqual('testComponent');
      // Last element should not have separator (condition is false, so children[2] is false)
      expect(subtitle.props.children[2].props.children[2]).toBeFalsy();
    });

    it('creates a subtitle from only root org that includes `root org` + component name and nothing else', function () {
      let subtitle = createSubtitle(availableScopesOnlyRoot, component);
      expect(subtitle.props.children.length).toEqual(2);
      expect(subtitle.props.children[0].props.children[1].props.children).toEqual('root org');
      expect(subtitle.props.children[0].props.children[2].props.children).toEqual('/');
      expect(subtitle.props.children[1].props.children[1].props.children).toEqual('testComponent');
      // Last element should not have separator (condition is false, so children[2] is false)
      expect(subtitle.props.children[1].props.children[2]).toBeFalsy();
    });

    it('creates a subtitle with single element that has no separator', function () {
      let subtitle = createSubtitle(availableScopesOnlyRoot, null);
      expect(subtitle.props.children.length).toEqual(1);
      expect(subtitle.props.children[0].props.children[1].props.children).toEqual('root org');
      // Single element is also last element, so no separator
      expect(subtitle.props.children[0].props.children[2]).toBeFalsy();
    });
  });
});
