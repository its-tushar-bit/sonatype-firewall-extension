/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen } from 'TestRoot/SpecUtil';
import * as routerStateContext from 'MainRoot/react/RouterStateContext';
import FirewallViolationPage from 'MainRoot/firewall/firewallComponentDetailsPage/policyViolations/policyViolationsTile/FirewallViolationPage';

describe('FirewallViolationPage', () => {
  beforeEach(() => {
    jest.spyOn(routerStateContext, 'useRouterState').mockReturnValue({
      href: jest.fn(() => '#/management/view/organization'),
      get: jest.fn((state) => state),
    });
  });

  afterEach(() => {
    jest.restoreAllMocks();
  });

  it('loads firewall violation and vulnerability details using firewall-owned props', () => {
    const loadFirewallViolationDetails = jest.fn();
    const loadFirewallPolicyVulnerabilityDetails = jest.fn();

    render(
      <FirewallViolationPage
        selectPolicyId="policy-violation-id"
        policyDetail={{
          policyViolationId: 'policy-violation-id',
          policyName: 'Security-Medium',
          policyOwner: {
            ownerId: 'ROOT_ORGANIZATION_ID',
            ownerName: 'Root Organization',
            ownerType: 'organization',
          },
          policyThreatLevel: 7,
          policyThreatCategory: 'SECURITY',
          componentIdentifier: {
            format: 'maven',
            coordinates: {
              artifactId: 'commons-collections',
              groupId: 'commons-collections',
              version: '3.2.1',
            },
          },
          constraints: [
            {
              constraintName: 'Medium risk CVSS score',
              conditions: [
                {
                  conditionReason: 'Found security vulnerability CVE-2012-2098 with severity >= 4',
                  conditionTriggerReference: {
                    value: 'CVE-2012-2098',
                    type: 'SECURITY_VULNERABILITY_REFID',
                  },
                },
              ],
            },
          ],
        }}
        violationDetails={{
          policyName: 'Security-Medium',
          componentIdentifier: {
            format: 'maven',
            coordinates: {
              artifactId: 'commons-collections',
              groupId: 'commons-collections',
              version: '3.2.1',
            },
          },
        }}
        violationDetailsError={null}
        firewallIsLoading={false}
        activeWaivers={[]}
        vulnerabilityDetailsLoading={false}
        vulnerabilityDetails={null}
        vulnerabilityDetailsError={null}
        isVulnerabilityDetailsOutdated={false}
        loadFirewallViolationDetails={loadFirewallViolationDetails}
        loadFirewallPolicyVulnerabilityDetails={loadFirewallPolicyVulnerabilityDetails}
        setSelectPolicyViolation={jest.fn()}
        componentIdentifier={{
          format: 'maven',
          coordinates: {
            artifactId: 'commons-collections',
            groupId: 'commons-collections',
            version: '3.2.1',
          },
        }}
        componentHash="component-hash"
        tabId="violations"
        repositoryId="repository-id"
        matchState="exact"
        pathname="commons-collections/commons-collections/3.2.1"
        componentDisplayName="commons-collections:commons-collections:3.2.1"
        hasEditIqPermission
        similarWaiversFilterSelectedIds={{}}
        setFilterIdsSimilarWaivers={jest.fn()}
        isFirewall
        isSbomManager={false}
        isFromPolicyViolations
      />
    );

    expect(loadFirewallViolationDetails).toHaveBeenCalledWith('policy-violation-id');
    expect(loadFirewallPolicyVulnerabilityDetails).toHaveBeenCalledWith('CVE-2012-2098', {
      format: 'maven',
      coordinates: {
        artifactId: 'commons-collections',
        groupId: 'commons-collections',
        version: '3.2.1',
      },
    });
  });

  it('renders policy metadata from violationDetails when the selected row lacks those fields', () => {
    render(
      <FirewallViolationPage
        selectPolicyId="policy-violation-id"
        policyDetail={{
          policyViolationId: 'policy-violation-id',
          policyName: 'numpy-root',
          policyThreatLevel: 10,
          constraints: [
            {
              constraintName: 'Coordinates were numpy',
              conditions: [
                {
                  conditionReason: 'Coordinates were numpy',
                },
              ],
            },
          ],
        }}
        violationDetails={{
          policyName: 'numpy-root',
          policyThreatCategory: 'OTHER',
          policyOwner: {
            ownerId: 'ROOT_ORGANIZATION_ID',
            ownerName: 'Root Organization',
            ownerType: 'organization',
          },
          openTime: '2022-08-10T13:35:40.641+03:00',
          componentIdentifier: {
            format: 'pypi',
            coordinates: {
              name: 'numpy',
              extension: 'whl',
              version: '2.4.4',
            },
          },
        }}
        violationDetailsError={null}
        firewallIsLoading={false}
        activeWaivers={[]}
        vulnerabilityDetailsLoading={false}
        vulnerabilityDetails={null}
        vulnerabilityDetailsError={null}
        isVulnerabilityDetailsOutdated={false}
        loadFirewallViolationDetails={jest.fn()}
        loadFirewallPolicyVulnerabilityDetails={jest.fn()}
        setSelectPolicyViolation={jest.fn()}
        componentIdentifier={{
          format: 'pypi',
          coordinates: {
            name: 'numpy',
            extension: 'whl',
            version: '2.4.4',
          },
        }}
        componentHash="component-hash"
        tabId="violations"
        repositoryId="repository-id"
        matchState="exact"
        pathname="numpy/2.4.4"
        componentDisplayName="numpy"
        hasEditIqPermission
        similarWaiversFilterSelectedIds={{}}
        setFilterIdsSimilarWaivers={jest.fn()}
        isFirewall
        isSbomManager={false}
        isFromPolicyViolations
      />
    );

    expect(screen.getByRole('definition', { name: 'Policy Type' })).toHaveTextContent('Other');
    expect(screen.getByRole('definition', { name: 'Policy Owner' })).toHaveTextContent('Root Organization');
    expect(screen.getByRole('definition', { name: 'Last Reported' })).not.toHaveTextContent('--');
  });
});
