/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen } from 'TestRoot/SpecUtil';
import FirewallPolicyViolationsTile from 'MainRoot/firewall/firewallComponentDetailsPage/policyViolations/policyViolationsTile/FirewallPolicyViolationsTile';
import * as ViewAllPoliciesWaiversButton from 'MainRoot/firewall/firewallComponentDetailsPage/policyViolations/policyViolationsTile/ViewAllPoliciesWaiversButton';
import * as FirewallPolicyViolationsTable from 'MainRoot/firewall/firewallComponentDetailsPage/policyViolations/policyViolationsTile/FirewallPolicyViolationsTable';
import * as firewallPolicyViolationsSelectors from 'MainRoot/firewall/firewallComponentDetailsPage/policyViolations/firewallPolicyViolationsSelectors.js';

describe('FirewallPolicyViolationsTile component', () => {
  let minState,
    originalSelectComponentName = firewallPolicyViolationsSelectors.selectComponentName,
    originalSelectComponentNameWithoutVersion = firewallPolicyViolationsSelectors.selectComponentNameWithoutVersion,
    originalSelectWaivers = firewallPolicyViolationsSelectors.selectWaivers,
    originalSelectWaiversByOwner = firewallPolicyViolationsSelectors.selectWaiversByOwner,
    originalselectPolicyViolations = firewallPolicyViolationsSelectors.selectPolicyViolations,
    spyViewAllPoliciesWaiversButton,
    spyFirewallPolicyViolationsTable;

  beforeEach(() => {
    minState = {
      firewall: {
        componentDetailsPage: {
          policyExistingWaivers: {
            waiversByOwner: [
              {
                ownerId: '8098fb28cfc84ff99b3c34e66d2b9ccf',
                ownerName: 'maven-central',
                ownerType: 'repository',
                waivers: [
                  {
                    id: '468e1552699445d48e448bf22740ad8b',
                    hash: '7a3c2521ae0c6f53e044',
                    policyId: '6f085a73545f443ab92ce7a109c83935',
                    ownerId: '8098fb28cfc84ff99b3c34e66d2b9ccf',
                    comment: '',
                    createTime: 1661928739954,
                    expiryTime: null,
                    creatorId: 'admin',
                    creatorName: 'Admin BuiltIn',
                    constraintFactsJson:
                      '[{"constraintId":"d17bd2a78ada49d6b40df2dd596d8e19","constraintName":"older than one day","operatorName":"AND","conditionFacts":[{"conditionTypeId":"License","conditionIndex":0,"summary":"License is \'Apache-1.1\'","reason":"Found \'Apache-1.1\' license","reference":null,"triggerJson":"{\\"conditionIndex\\":0,\\"trigger\\":{\\"id\\":\\"Apache-1.1\\"}}"}]}]',
                    constraintFacts: [
                      {
                        constraintId: 'd17bd2a78ada49d6b40df2dd596d8e19',
                        constraintName: 'older than one day',
                        operatorName: 'AND',
                        conditionFacts: [
                          {
                            conditionTypeId: 'License',
                            conditionIndex: 0,
                            summary: "License is 'Apache-1.1'",
                            reason: "Found 'Apache-1.1' license",
                            reference: null,
                            triggerJson: '{"conditionIndex":0,"trigger":{"id":"Apache-1.1"}}',
                          },
                        ],
                      },
                    ],
                    associatedPackageUrl: null,
                    componentMatchStrategy: 'EXACT_COMPONENT',
                    componentIdentifier: null,
                    policyName: 'test-policy',
                  },
                ],
              },
            ],
          },
          waivers: [
            {
              id: '468e1552699445d48e448bf22740ad8b',
              hash: '7a3c2521ae0c6f53e044',
              policyId: '6f085a73545f443ab92ce7a109c83935',
              ownerId: '8098fb28cfc84ff99b3c34e66d2b9ccf',
              comment: '',
              createTime: 1661928739954,
              expiryTime: null,
              creatorId: 'admin',
              creatorName: 'Admin BuiltIn',
              constraintFactsJson:
                '[{"constraintId":"d17bd2a78ada49d6b40df2dd596d8e19","constraintName":"older than one day","operatorName":"AND","conditionFacts":[{"conditionTypeId":"License","conditionIndex":0,"summary":"License is \'Apache-1.1\'","reason":"Found \'Apache-1.1\' license","reference":null,"triggerJson":"{\\"conditionIndex\\":0,\\"trigger\\":{\\"id\\":\\"Apache-1.1\\"}}"}]}]',
              constraintFacts: [
                {
                  constraintId: 'd17bd2a78ada49d6b40df2dd596d8e19',
                  constraintName: 'older than one day',
                  operatorName: 'AND',
                  conditionFacts: [
                    {
                      conditionTypeId: 'License',
                      conditionIndex: 0,
                      summary: "License is 'Apache-1.1'",
                      reason: "Found 'Apache-1.1' license",
                      reference: null,
                      triggerJson: '{"conditionIndex":0,"trigger":{"id":"Apache-1.1"}}',
                    },
                  ],
                },
              ],
              associatedPackageUrl: null,
              componentMatchStrategy: 'EXACT_COMPONENT',
              componentIdentifier: null,
              policyName: 'test-policy',
              policyWaiverId: '468e1552699445d48e448bf22740ad8b',
              scopeOwnerId: '8098fb28cfc84ff99b3c34e66d2b9ccf',
              scopeOwnerType: 'repository',
              scopeOwnerName: 'maven-central',
            },
          ],
          componentDetails: {
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
          },
          componentName: 'ant : ant : 1.6',
          name: 'ant',
        },
      },
      spyViewAllPoliciesWaiversButton,
      spyFirewallPolicyViolationsTable,
    };

    spyViewAllPoliciesWaiversButton = spyOn(ViewAllPoliciesWaiversButton, 'default').and.callFake(() => (
      <div>ViewAllPoliciesWaiversButton</div>
    ));

    spyFirewallPolicyViolationsTable = spyOn(FirewallPolicyViolationsTable, 'default').and.callFake(() => (
      <div>FirewallPolicyViolationsTable</div>
    ));

    spyOn(firewallPolicyViolationsSelectors, 'selectComponentName').and.callFake(() => {
      return originalSelectComponentName(minState);
    });

    spyOn(firewallPolicyViolationsSelectors, 'selectComponentNameWithoutVersion').and.callFake(() => {
      return originalSelectComponentNameWithoutVersion(minState);
    });

    spyOn(firewallPolicyViolationsSelectors, 'selectWaivers').and.callFake(() => {
      return originalSelectWaivers(minState);
    });

    spyOn(firewallPolicyViolationsSelectors, 'selectWaiversByOwner').and.callFake(() => {
      return originalSelectWaiversByOwner(minState);
    });

    spyOn(firewallPolicyViolationsSelectors, 'selectPolicyViolations').and.callFake(() => {
      return originalselectPolicyViolations(minState);
    });

    SpecUtil.mockReduxStore(minState);
  });

  it('render Tile component with FirewallPolicyViolationsTile and FirewallPolicyViolationsTable with props', () => {
    render(<FirewallPolicyViolationsTile />);
    expect(screen.queryByText(/ViewAllPoliciesWaiversButton/)).toBeVisible();
    expect(spyViewAllPoliciesWaiversButton.prototype.constructor).toHaveBeenCalledWith(
      {
        setShowComponentWaiversPopover: jasmine.any(Function),
      }, // params
      {} // state
    );

    expect(screen.queryByText(/FirewallPolicyViolationsTable/)).toBeVisible();
    expect(spyFirewallPolicyViolationsTable.prototype.constructor).toHaveBeenCalledWith(
      {
        setShowComponentWaiversPopover: jasmine.any(Function),
        showPolicyWaiversPopover: false,
        violations: originalselectPolicyViolations(minState),
        showProxyState: true,
        componentName: originalSelectComponentName(minState),
        componentNameWithoutVersion: originalSelectComponentNameWithoutVersion(minState),
        waivers: originalSelectWaivers(minState),
        waiverToDelete: null,
        setWaiverToDelete: jasmine.any(Function),
        componentHash: undefined,
        tabId: undefined,
        repositoryId: undefined,
      }, // params
      {} // state
    );
  });
});
