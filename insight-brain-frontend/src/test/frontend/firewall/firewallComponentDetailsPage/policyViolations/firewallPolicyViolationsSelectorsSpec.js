/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as firewallPolicyViolationsSelectors from 'MainRoot/firewall/firewallComponentDetailsPage/policyViolations/firewallPolicyViolationsSelectors';

describe('firewallPolicyViolationsSelectors', () => {
  const minState = {
    firewall: {
      componentDetailsPage: {
        componentDetails: {
          policyAlerts: [
            {
              trigger: {
                policyId: '332567c02b0b42a4a74c476c612ef5e3',
                policyName: 'Architecture-Quality',
                threatLevel: 1,
                componentFacts: [
                  {
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
                    hash: '7a3c2521ae0c6f53e044',
                    constraintFacts: [
                      {
                        constraintId: 'a1c508bd7ce44ac692c131eeaa966dc1',
                        constraintName: 'Version is unpopular',
                        operatorName: 'OR',
                        conditionFacts: [
                          {
                            conditionTypeId: 'RelativePopularity',
                            conditionIndex: 0,
                            summary: 'Relative Popularity (Percentage) <= 10',
                            reason: 'Relative popularity was <= 10% (relative popularity = 3%)',
                            reference: null,
                            triggerJson: null,
                          },
                        ],
                      },
                    ],
                    pathnames: [],
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
                ],
              },
              actions: [],
            },
          ],
        },
      },
    },
  };

  it('selectPolicyAlerts', () => {
    expect(Object.keys(firewallPolicyViolationsSelectors.selectPolicyAlerts(minState))).toEqual([
      ...Object.keys(minState.firewall.componentDetailsPage.componentDetails.policyAlerts),
    ]);
  });

  it('selectPolicyViolations', () => {
    expect(Object.keys(firewallPolicyViolationsSelectors.selectPolicyViolations(minState))).toEqual([
      ...Object.keys(minState.firewall.componentDetailsPage.componentDetails.policyAlerts.map((trigger) => trigger)),
    ]);
  });
});
