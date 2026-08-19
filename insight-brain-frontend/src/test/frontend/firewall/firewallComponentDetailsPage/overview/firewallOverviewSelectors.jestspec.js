/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as firewallOverviewSelectors from 'MainRoot/firewall/firewallComponentDetailsPage/overview/firewallOverviewSelectors';

describe('firewallOverviewSelectors', () => {
  const minState = {
    firewall: {
      componentDetailsPage: {
        isLoadingComponentDetails: false,
        componentDetails: {
          hash: '7a3c2521ae0c6f53e044',
          matchState: 'exact',
          identificationSource: 'Sonatype',
          componentIdentifier: {
            format: 'maven',
            coordinates: {
              artifactId: 'ant',
              classifier: '',
              extension: 'jar',
              groupId: 'ant',
              version: '1.6',
            },
          },
          displayName: {
            parts: [
              {
                field: 'Group',
                value: 'ant',
              },
              {
                value: ' : ',
              },
              {
                field: 'Artifact',
                value: 'ant',
              },
              {
                value: ' : ',
              },
              {
                field: 'Version',
                value: '1.6',
              },
            ],
            name: 'ant',
          },
          groupId: 'ant',
          artifactId: 'ant',
          version: '1.6',
        },
        componentDetailsError: null,
      },
    },
    componentDetailsOverview: {
      versionExplorerData: {
        loading: false,
        loadError: null,
        versions: [
          {
            matchState: 'exact',
            identificationSource: 'Sonatype',
            displayName: {
              parts: [
                {
                  field: 'Group',
                  value: 'ant',
                },
                {
                  value: ' : ',
                },
                {
                  field: 'Artifact',
                  value: 'ant',
                },
                {
                  value: ' : ',
                },
                {
                  field: 'Version',
                  value: '1.6',
                },
              ],
              name: 'ant',
            },
            componentIdentifier: {
              format: 'maven',
              coordinates: {
                artifactId: 'ant',
                classifier: '',
                extension: 'jar',
                groupId: 'ant',
                version: '1.6',
              },
            },
          },
          {
            matchState: 'exact',
            identificationSource: 'Sonatype',
            displayName: {
              parts: [
                {
                  field: 'Group',
                  value: 'ant',
                },
                {
                  value: ' : ',
                },
                {
                  field: 'Artifact',
                  value: 'ant',
                },
                {
                  value: ' : ',
                },
                {
                  field: 'Version',
                  value: '1.6.1',
                },
              ],
              name: 'ant',
            },
            componentIdentifier: {
              format: 'maven',
              coordinates: {
                artifactId: 'ant',
                classifier: '',
                extension: 'jar',
                groupId: 'ant',
                version: '1.6.1',
              },
            },
          },
        ],
        remediation: {
          versionChanges: [
            {
              type: 'next-non-failing',
              data: {
                component: {
                  packageUrl: 'pkg:maven/ant/ant@1.6.1?type=jar',
                  hash: null,
                  componentIdentifier: {
                    format: 'maven',
                    coordinates: {
                      artifactId: 'ant',
                      classifier: '',
                      extension: 'jar',
                      groupId: 'ant',
                      version: '1.6.1',
                    },
                  },
                  displayName: 'ant : ant : 1.6.1',
                },
              },
            },
            {
              type: 'next-non-failing-with-dependencies',
              data: {
                component: {
                  packageUrl: 'pkg:maven/ant/ant@1.6.1?type=jar',
                  hash: null,
                  componentIdentifier: {
                    format: 'maven',
                    coordinates: {
                      artifactId: 'ant',
                      classifier: '',
                      extension: 'jar',
                      groupId: 'ant',
                      version: '1.6.1',
                    },
                  },
                  displayName: 'ant : ant : 1.6.1',
                },
              },
            },
          ],
        },
        currentVersionDetails: {
          hash: '7a3c2521ae0c6f53e044',
          matchState: 'exact',
          website: null,
          identificationSource: 'Sonatype',
          componentIdentifier: {
            format: 'maven',
            coordinates: {
              artifactId: 'ant',
              classifier: '',
              extension: 'jar',
              groupId: 'ant',
              version: '1.6',
            },
          },
          componentCategories: [
            {
              componentCategoryId: 10,
              path: 'Build Tools',
            },
          ],
          displayName: {
            parts: [
              {
                field: 'Group',
                value: 'ant',
              },
              {
                value: ' : ',
              },
              {
                field: 'Artifact',
                value: 'ant',
              },
              {
                value: ' : ',
              },
              {
                field: 'Version',
                value: '1.6',
              },
            ],
            name: 'ant',
          },
          groupId: 'ant',
          artifactId: 'ant',
          version: '1.6',
          observedLicenseIds: ['Apache-1.1'],
          declaredLicenseIds: ['Apache-1.1'],
        },
      },
    },
    router: {
      currentParams: {
        '#': null,
        repositoryId: '603ac500381f48cba8433df1bc916991',
        componentIdentifier:
          '{"format":"maven","coordinates":{"artifactId":"ant","classifier":"","extension":"jar","groupId":"ant","version":"1.6"}}',
        componentHash: '7a3c2521ae0c6f53e044',
        matchState: 'exact',
        proprietary: 'false',
        tabId: 'overview',
      },
    },
  };

  it('selectSelectedComponent', () => {
    expect(Object.keys(firewallOverviewSelectors.selectSelectedComponent(minState))).toEqual([
      ...Object.keys(minState.firewall.componentDetailsPage.componentDetails),
      'routeParams',
      'scanId',
      'ownerId',
    ]);
  });

  it('selectComponentDetailsRequestData', () => {
    const selectedComponentDetailsRequestData = firewallOverviewSelectors.selectComponentDetailsRequestData(minState);
    expect(Object.keys(selectedComponentDetailsRequestData)).toEqual([
      'clientType',
      'ownerType',
      'ownerId',
      'matchState',
      'proprietary',
      'componentIdentifier',
      'hash',
      'scanId',
    ]);
    expect(selectedComponentDetailsRequestData.clientType).toEqual('ci');
    expect(selectedComponentDetailsRequestData.ownerType).toEqual('repository');
  });

  it('selectComponentDetailsSelectedRequestData', () => {
    const customState = {
      ...minState,
      componentDetailsOverview: {
        ...minState.componentDetailsOverview,
        selectedVersionData: {
          ...minState.selectedVersionData,
          selectedVersion:
            minState.componentDetailsOverview.versionExplorerData.versions[1].componentIdentifier.coordinates.version,
        },
      },
    };
    const selectedComponentDetailsRequestData = firewallOverviewSelectors.selectComponentDetailsSelectedRequestData(
      customState
    );
    expect(Object.keys(selectedComponentDetailsRequestData)).toEqual([
      'clientType',
      'ownerType',
      'ownerId',
      'matchState',
      'componentIdentifier',
      'hash',
      'scanId',
    ]);
    expect(selectedComponentDetailsRequestData.clientType).toEqual('ci');
    expect(selectedComponentDetailsRequestData.ownerType).toEqual('repository');
    expect(selectedComponentDetailsRequestData.hash).toBeUndefined();
  });

  it('selectVersionExplorerRequestData', () => {
    const selectedComponentsDetailsRequestData = firewallOverviewSelectors.selectComponentDetailsRequestData(minState);
    const selectedVersionExplorerRequestData = firewallOverviewSelectors.selectVersionExplorerRequestData(minState);
    expect(Object.keys(selectedVersionExplorerRequestData)).toEqual([
      ...Object.keys(selectedComponentsDetailsRequestData),
      'stageId',
    ]);
    expect(selectedVersionExplorerRequestData.stageId).toEqual('proxy');
  });
});
