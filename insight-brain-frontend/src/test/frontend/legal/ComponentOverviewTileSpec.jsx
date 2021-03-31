/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as enzymeUtils from '../enzymeUtils';
import { NxPolicyViolationIndicator } from '@sonatype/react-shared-components';

describe('ComponentOverviewTile', function() {
  let getShallowComponent,
      $state,
      now,
      terseAgoSpy,
      minimalProps,
      ComponentOverviewTile;

  beforeEach(function() {
    $state = {
      get: jasmine.createSpy('$state.get'),
      href: jasmine.createSpy('$state.href')
    };
    now = new Date();
    terseAgoSpy = jasmine.createSpy('terseAgo').and.callFake(time => 'terseAgo ' + time);
    minimalProps = {
      component: {
        licenseLegalData: {
          effectiveLicenses: ['license'],
          componentCopyrightLastUpdatedAt: 1616356809832,
          componentCopyrightLastUpdatedByUsername: 'user1',
          componentNoticesLastUpdatedAt: 1618873200000,
          componentNoticesLastUpdatedByUsername: 'user2',
          componentLicensesLastUpdatedAt: 1618700400000,
          componentLicensesLastUpdatedByUsername: 'user3',
          highestEffectiveLicenseThreatGroup: {
            licenseThreatGroupLevel: 8,
            licenseThreatGroupCategory: 'severe',
            licenseThreatGroupName: 'MyLtg'
          },
          obligations: [],
          attributions: []
        },
        stageScans: [
          { stageName: 'Build', scanId: 'scanId', scanDate: getTimeDaysAgo(1) },
          { stageName: 'Stage Release', scanId: null, scanDate: null },
          { stageName: 'Release', scanId: null, scanDate: null },
          { stageName: 'Operate', scanId: null, scanDate: null }
        ]
      },
      licenseNames: ['License-1.0', 'License-2.0', 'License-1.0-License-2.0'],
      $state
    };
    ComponentOverviewTile =
        require('inject-loader!../../../main/frontend/legal/ComponentOverviewTile')({
          '../util/CommonServices': { terseAgo: terseAgoSpy }
        }).default;
    getShallowComponent = enzymeUtils.getShallowComponent(ComponentOverviewTile, minimalProps);
  });

  it('renders the review status as unreviewed if all are open', function() {
    const wrapper = getShallowComponent({
      component: {
        ...minimalProps.component,
        licenseLegalData: {
          ...minimalProps.component.licenseLegalData,
          obligations: [{ name: 'obligation1', originalStatus: 'OPEN' }]
        }
      }
    });
    expect(wrapper.find('#component-overview-tile-review-status')).toHaveText('Unreviewed');
  });

  it('renders the review status as complete if all are fulfilled', function() {
    const wrapper = getShallowComponent({
      component: {
        ...minimalProps.component,
        licenseLegalData: {
          ...minimalProps.component.licenseLegalData,
          obligations: [{ name: 'obligation1', originalStatus: 'FULFILLED' }]
        }
      }
    });
    expect(wrapper.find('#component-overview-tile-review-status')).toHaveText('Complete');
  });

  it('renders the review status as complete if all are not applicable', function() {
    const wrapper = getShallowComponent({
      component: {
        ...minimalProps.component,
        licenseLegalData: {
          ...minimalProps.component.licenseLegalData,
          obligations: [{ name: 'obligation1', originalStatus: 'IGNORED' }]
        }
      }
    });
    expect(wrapper.find('#component-overview-tile-review-status')).toHaveText('Complete');
  });

  it('renders the review status as complete if all are fulfilled or not applicable', function() {
    const wrapper = getShallowComponent({
      component: {
        ...minimalProps.component,
        licenseLegalData: {
          ...minimalProps.component.licenseLegalData,
          obligations: [
            { name: 'obligation1', originalStatus: 'IGNORED' }, { name: 'obligation2', originalStatus: 'FULFILLED' }
          ]
        }
      }
    });
    expect(wrapper.find('#component-overview-tile-review-status')).toHaveText('Complete');
  });

  it('renders the review status as flagged if any are flagged', function() {
    let wrapper = getShallowComponent({
      component: {
        ...minimalProps.component,
        licenseLegalData: {
          ...minimalProps.component.licenseLegalData,
          obligations: [{ name: 'obligation1', originalStatus: 'FLAGGED' }]
        }
      }
    });
    expect(wrapper.find('#component-overview-tile-review-status')).toHaveText('Flagged');
    wrapper = getShallowComponent({
      component: {
        ...minimalProps.component,
        licenseLegalData: {
          ...minimalProps.component.licenseLegalData,
          obligations: [
            { name: 'obligation1', originalStatus: 'IGNORED' }, { name: 'obligation2', originalStatus: 'FLAGGED' }
          ]
        }
      }
    });
    expect(wrapper.find('#component-overview-tile-review-status')).toHaveText('Flagged');
    wrapper = getShallowComponent({
      component: {
        ...minimalProps.component,
        licenseLegalData: {
          ...minimalProps.component.licenseLegalData,
          obligations: [
            { name: 'obligation1', originalStatus: 'FULFILLED' }, { name: 'obligation2', originalStatus: 'FLAGGED' }
          ]
        }
      }
    });
    expect(wrapper.find('#component-overview-tile-review-status')).toHaveText('Flagged');
    wrapper = getShallowComponent({
      component: {
        ...minimalProps.component,
        licenseLegalData: {
          ...minimalProps.component.licenseLegalData,
          obligations: [
            { name: 'obligation1', originalStatus: 'OPEN' }, { name: 'obligation2', originalStatus: 'FLAGGED' }
          ]
        }
      }
    });
    expect(wrapper.find('#component-overview-tile-review-status')).toHaveText('Flagged');
  });

  it('renders the review status as in progress if some are done and none are flagged', function() {
    let wrapper = getShallowComponent({
      component: {
        ...minimalProps.component,
        licenseLegalData: {
          ...minimalProps.component.licenseLegalData,
          obligations: [
            { name: 'obligation1', originalStatus: 'OPEN' }, { name: 'obligation2', originalStatus: 'FULFILLED' }
          ]
        }
      }
    });
    expect(wrapper.find('#component-overview-tile-review-status')).toHaveText('In Progress');
    wrapper = getShallowComponent({
      component: {
        ...minimalProps.component,
        licenseLegalData: {
          ...minimalProps.component.licenseLegalData,
          obligations: [
            { name: 'obligation1', originalStatus: 'OPEN' }, { name: 'obligation2', originalStatus: 'IGNORED' }
          ]
        }
      }
    });
    expect(wrapper.find('#component-overview-tile-review-status')).toHaveText('In Progress');
  });

  it('renders the review status as unreviewed if there are no obligations and no effective licenses', function() {
    const wrapper = getShallowComponent({
      component: {
        ...minimalProps.component,
        licenseLegalData: {
          ...minimalProps.component.licenseLegalData,
          effectiveLicenses: [],
          obligations: []
        }
      }
    });
    expect(wrapper.find('#component-overview-tile-review-status')).toHaveText('Unreviewed');
  });

  it('renders the review status as complete if there are no obligations and one or more effective licenses',
      function() {
        let wrapper = getShallowComponent({
          component: {
            ...minimalProps.component,
            licenseLegalData: {
              ...minimalProps.component.licenseLegalData,
              obligations: []
            }
          }
        });
        expect(wrapper.find('#component-overview-tile-review-status')).toHaveText('Complete');
        wrapper = getShallowComponent({
          component: {
            ...minimalProps.component,
            licenseLegalData: {
              ...minimalProps.component.licenseLegalData,
              effectiveLicenses: ['license1', 'license2'],
              obligations: []
            }
          }
        });
        expect(wrapper.find('#component-overview-tile-review-status')).toHaveText('Complete');
      });

  it('renders the review status as unreviewed if there are no obligations and effectively unspecified licenses',
      function() {
        let wrapper = getShallowComponent({
          component: {
            ...minimalProps.component,
            licenseLegalData: {
              ...minimalProps.component.licenseLegalData,
              effectiveLicenses: ['Not-Declared'],
              obligations: []
            }
          }
        });
        expect(wrapper.find('#component-overview-tile-review-status')).toHaveText('Unreviewed');

        wrapper = getShallowComponent({
          component: {
            ...minimalProps.component,
            licenseLegalData: {
              ...minimalProps.component.licenseLegalData,
              effectiveLicenses: ['No-Sources'],
              obligations: []
            }
          }
        });
        expect(wrapper.find('#component-overview-tile-review-status')).toHaveText('Unreviewed');

        wrapper = getShallowComponent({
          component: {
            ...minimalProps.component,
            licenseLegalData: {
              ...minimalProps.component.licenseLegalData,
              effectiveLicenses: ['No-Source-License'],
              obligations: []
            }
          }
        });
        expect(wrapper.find('#component-overview-tile-review-status')).toHaveText('Unreviewed');

        wrapper = getShallowComponent({
          component: {
            ...minimalProps.component,
            licenseLegalData: {
              ...minimalProps.component.licenseLegalData,
              effectiveLicenses: ['UNSPECIFIED'],
              obligations: []
            }
          }
        });
        expect(wrapper.find('#component-overview-tile-review-status')).toHaveText('Unreviewed');

        wrapper = getShallowComponent({
          component: {
            ...minimalProps.component,
            licenseLegalData: {
              ...minimalProps.component.licenseLegalData,
              effectiveLicenses: ['Not-Supported'],
              obligations: []
            }
          }
        });
        expect(wrapper.find('#component-overview-tile-review-status')).toHaveText('Unreviewed');

        wrapper = getShallowComponent({
          component: {
            ...minimalProps.component,
            licenseLegalData: {
              ...minimalProps.component.licenseLegalData,
              effectiveLicenses: [
                'Not-Declared', 'No-Sources', 'No-Source-License', 'UNSPECIFIED', 'Not-Supported', 'Not-Supported'
              ],
              obligations: []
            }
          }
        });
        expect(wrapper.find('#component-overview-tile-review-status')).toHaveText('Unreviewed');
      });

  it('renders no last modified and modified by', function() {
    const wrapper = getShallowComponent({
      component: {
        ...minimalProps.component,
        licenseLegalData: {
          ...minimalProps.component.licenseLegalData,
          componentCopyrightLastUpdatedAt: null,
          componentCopyrightLastUpdatedByUsername: null,
          componentNoticesLastUpdatedAt: null,
          componentNoticesLastUpdatedByUsername: null,
          componentLicensesLastUpdatedAt: null,
          componentLicensesLastUpdatedByUsername: null,
          obligations: [{ lastUpdatedAt: null, lastUpdatedByUsername: null }],
          attributions: [{ lastUpdatedAt: null, lastUpdatedByUsername: null }]
        }
      }
    });
    expect(wrapper.find('#component-overview-tile-last-modified')).toHaveText('Never');
    expect(wrapper.find('#component-overview-tile-modified-by')).toHaveText('N/A');
  });

  it('renders the correct last modified and modified by', function() {
    let wrapper = getShallowComponent({
      component: {
        ...minimalProps.component,
        licenseLegalData: {
          ...minimalProps.component.licenseLegalData,
          componentCopyrightLastUpdatedAt: getTimeDaysAgo(1),
          componentCopyrightLastUpdatedByUsername: 'user1',
          componentNoticesLastUpdatedAt: getTimeDaysAgo(2),
          componentNoticesLastUpdatedByUsername: 'user2',
          componentLicensesLastUpdatedAt: getTimeDaysAgo(3),
          componentLicensesLastUpdatedByUsername: 'user3',
          obligations: [
            { lastUpdatedAt: getTimeDaysAgo(4), lastUpdatedByUsername: 'user4' },
            { lastUpdatedAt: getTimeDaysAgo(5), lastUpdatedByUsername: 'user5' }
          ],
          attributions: [
            { lastUpdatedAt: getTimeDaysAgo(6), lastUpdatedByUsername: 'user6' },
            { lastUpdatedAt: getTimeDaysAgo(7), lastUpdatedByUsername: 'user7' }
          ]
        }
      }
    });
    expect(wrapper.find('#component-overview-tile-last-modified')).toHaveText('terseAgo ' + getTimeDaysAgo(1) + ' ago');
    expect(wrapper.find('#component-overview-tile-modified-by')).toHaveText('user1');

    wrapper = getShallowComponent({
      component: {
        ...minimalProps.component,
        licenseLegalData: {
          ...minimalProps.component.licenseLegalData,
          componentCopyrightLastUpdatedAt: getTimeDaysAgo(8),
          componentCopyrightLastUpdatedByUsername: 'user1',
          componentNoticesLastUpdatedAt: getTimeDaysAgo(2),
          componentNoticesLastUpdatedByUsername: 'user2',
          componentLicensesLastUpdatedAt: getTimeDaysAgo(3),
          componentLicensesLastUpdatedByUsername: 'user3',
          obligations: [
            { lastUpdatedAt: getTimeDaysAgo(4), lastUpdatedByUsername: 'user4' },
            { lastUpdatedAt: getTimeDaysAgo(5), lastUpdatedByUsername: 'user5' }
          ],
          attributions: [
            { lastUpdatedAt: getTimeDaysAgo(6), lastUpdatedByUsername: 'user6' },
            { lastUpdatedAt: getTimeDaysAgo(7), lastUpdatedByUsername: 'user7' }
          ]
        }
      }
    });
    expect(wrapper.find('#component-overview-tile-last-modified')).toHaveText('terseAgo ' + getTimeDaysAgo(2) + ' ago');
    expect(wrapper.find('#component-overview-tile-modified-by')).toHaveText('user2');

    wrapper = getShallowComponent({
      component: {
        ...minimalProps.component,
        licenseLegalData: {
          ...minimalProps.component.licenseLegalData,
          componentCopyrightLastUpdatedAt: getTimeDaysAgo(8),
          componentCopyrightLastUpdatedByUsername: 'user1',
          componentNoticesLastUpdatedAt: getTimeDaysAgo(9),
          componentNoticesLastUpdatedByUsername: 'user2',
          componentLicensesLastUpdatedAt: getTimeDaysAgo(3),
          componentLicensesLastUpdatedByUsername: 'user3',
          obligations: [
            { lastUpdatedAt: getTimeDaysAgo(4), lastUpdatedByUsername: 'user4' },
            { lastUpdatedAt: getTimeDaysAgo(5), lastUpdatedByUsername: 'user5' }
          ],
          attributions: [
            { lastUpdatedAt: getTimeDaysAgo(6), lastUpdatedByUsername: 'user6' },
            { lastUpdatedAt: getTimeDaysAgo(7), lastUpdatedByUsername: 'user7' }
          ]
        }
      }
    });
    expect(wrapper.find('#component-overview-tile-last-modified')).toHaveText('terseAgo ' + getTimeDaysAgo(3) + ' ago');
    expect(wrapper.find('#component-overview-tile-modified-by')).toHaveText('user3');

    wrapper = getShallowComponent({
      component: {
        ...minimalProps.component,
        licenseLegalData: {
          ...minimalProps.component.licenseLegalData,
          componentCopyrightLastUpdatedAt: getTimeDaysAgo(8),
          componentCopyrightLastUpdatedByUsername: 'user1',
          componentNoticesLastUpdatedAt: getTimeDaysAgo(9),
          componentNoticesLastUpdatedByUsername: 'user2',
          componentLicensesLastUpdatedAt: getTimeDaysAgo(10),
          componentLicensesLastUpdatedByUsername: 'user3',
          obligations: [
            { lastUpdatedAt: getTimeDaysAgo(4), lastUpdatedByUsername: 'user4' },
            { lastUpdatedAt: getTimeDaysAgo(5), lastUpdatedByUsername: 'user5' }
          ],
          attributions: [
            { lastUpdatedAt: getTimeDaysAgo(6), lastUpdatedByUsername: 'user6' },
            { lastUpdatedAt: getTimeDaysAgo(7), lastUpdatedByUsername: 'user7' }
          ]
        }
      }
    });
    expect(wrapper.find('#component-overview-tile-last-modified')).toHaveText('terseAgo ' + getTimeDaysAgo(4) + ' ago');
    expect(wrapper.find('#component-overview-tile-modified-by')).toHaveText('user4');

    wrapper = getShallowComponent({
      component: {
        ...minimalProps.component,
        licenseLegalData: {
          ...minimalProps.component.licenseLegalData,
          componentCopyrightLastUpdatedAt: getTimeDaysAgo(8),
          componentCopyrightLastUpdatedByUsername: 'user1',
          componentNoticesLastUpdatedAt: getTimeDaysAgo(9),
          componentNoticesLastUpdatedByUsername: 'user2',
          componentLicensesLastUpdatedAt: getTimeDaysAgo(10),
          componentLicensesLastUpdatedByUsername: 'user3',
          obligations: [
            { lastUpdatedAt: getTimeDaysAgo(11), lastUpdatedByUsername: 'user4' },
            { lastUpdatedAt: getTimeDaysAgo(5), lastUpdatedByUsername: 'user5' }
          ],
          attributions: [
            { lastUpdatedAt: getTimeDaysAgo(6), lastUpdatedByUsername: 'user6' },
            { lastUpdatedAt: getTimeDaysAgo(7), lastUpdatedByUsername: 'user7' }
          ]
        }
      }
    });
    expect(wrapper.find('#component-overview-tile-last-modified')).toHaveText('terseAgo ' + getTimeDaysAgo(5) + ' ago');
    expect(wrapper.find('#component-overview-tile-modified-by')).toHaveText('user5');

    wrapper = getShallowComponent({
      component: {
        ...minimalProps.component,
        licenseLegalData: {
          ...minimalProps.component.licenseLegalData,
          componentCopyrightLastUpdatedAt: getTimeDaysAgo(8),
          componentCopyrightLastUpdatedByUsername: 'user1',
          componentNoticesLastUpdatedAt: getTimeDaysAgo(9),
          componentNoticesLastUpdatedByUsername: 'user2',
          componentLicensesLastUpdatedAt: getTimeDaysAgo(10),
          componentLicensesLastUpdatedByUsername: 'user3',
          obligations: [
            { lastUpdatedAt: getTimeDaysAgo(11), lastUpdatedByUsername: 'user4' },
            { lastUpdatedAt: getTimeDaysAgo(12), lastUpdatedByUsername: 'user5' }
          ],
          attributions: [
            { lastUpdatedAt: getTimeDaysAgo(6), lastUpdatedByUsername: 'user6' },
            { lastUpdatedAt: getTimeDaysAgo(7), lastUpdatedByUsername: 'user7' }
          ]
        }
      }
    });
    expect(wrapper.find('#component-overview-tile-last-modified')).toHaveText('terseAgo ' + getTimeDaysAgo(6) + ' ago');
    expect(wrapper.find('#component-overview-tile-modified-by')).toHaveText('user6');

    wrapper = getShallowComponent({
      component: {
        ...minimalProps.component,
        licenseLegalData: {
          ...minimalProps.component.licenseLegalData,
          componentCopyrightLastUpdatedAt: getTimeDaysAgo(8),
          componentCopyrightLastUpdatedByUsername: 'user1',
          componentNoticesLastUpdatedAt: getTimeDaysAgo(9),
          componentNoticesLastUpdatedByUsername: 'user2',
          componentLicensesLastUpdatedAt: getTimeDaysAgo(10),
          componentLicensesLastUpdatedByUsername: 'user3',
          obligations: [
            { lastUpdatedAt: getTimeDaysAgo(11), lastUpdatedByUsername: 'user4' },
            { lastUpdatedAt: getTimeDaysAgo(12), lastUpdatedByUsername: 'user5' }
          ],
          attributions: [
            { lastUpdatedAt: getTimeDaysAgo(13), lastUpdatedByUsername: 'user6' },
            { lastUpdatedAt: getTimeDaysAgo(7), lastUpdatedByUsername: 'user7' }
          ]
        }
      }
    });
    expect(wrapper.find('#component-overview-tile-last-modified')).toHaveText('terseAgo ' + getTimeDaysAgo(7) + ' ago');
    expect(wrapper.find('#component-overview-tile-modified-by')).toHaveText('user7');
  });

  const getTimeDaysAgo = (days) => new Date(new Date(now).setDate(new Date(now).getDate() - days)).getTime();

  it('renders the review progress with open counting as not done', function() {
    const wrapper = getShallowComponent({
      component: {
        ...minimalProps.component,
        licenseLegalData: {
          ...minimalProps.component.licenseLegalData,
          obligations: [{ name: 'obligation1', originalStatus: 'OPEN' }]
        }
      }
    });
    expect(wrapper.find('#component-overview-tile-review-progress')).toHaveText('0/1 complete');
  });

  it('renders the review progress with flagged counting as not done', function() {
    const wrapper = getShallowComponent({
      component: {
        ...minimalProps.component,
        licenseLegalData: {
          ...minimalProps.component.licenseLegalData,
          obligations: [{ name: 'obligation1', originalStatus: 'FLAGGED' }]
        }
      }
    });
    expect(wrapper.find('#component-overview-tile-review-progress')).toHaveText('0/1 complete');
  });

  it('renders the review progress with fulfilled counting as done', function() {
    const wrapper = getShallowComponent({
      component: {
        ...minimalProps.component,
        licenseLegalData: {
          ...minimalProps.component.licenseLegalData,
          obligations: [{ name: 'obligation1', originalStatus: 'FULFILLED' }]
        }
      }
    });
    expect(wrapper.find('#component-overview-tile-review-progress')).toHaveText('1/1 complete');
  });

  it('renders the review progress with ignored counting as done', function() {
    const wrapper = getShallowComponent({
      component: {
        ...minimalProps.component,
        licenseLegalData: {
          ...minimalProps.component.licenseLegalData,
          obligations: [{ name: 'obligation1', originalStatus: 'IGNORED' }]
        }
      }
    });
    expect(wrapper.find('#component-overview-tile-review-progress')).toHaveText('1/1 complete');
  });

  it('renders the fulfilled count', function() {
    let wrapper = getShallowComponent({
      component: {
        ...minimalProps.component,
        licenseLegalData: {
          ...minimalProps.component.licenseLegalData,
          obligations: [{ name: 'obligation1', originalStatus: 'other' }]
        }
      }
    });
    expect(wrapper.find('#component-overview-tile-fulfilled')).toHaveText('0');

    wrapper = getShallowComponent({
      component: {
        ...minimalProps.component,
        licenseLegalData: {
          ...minimalProps.component.licenseLegalData,
          obligations: [{ name: 'obligation1', originalStatus: 'FULFILLED' }]
        }
      }
    });
    expect(wrapper.find('#component-overview-tile-fulfilled')).toHaveText('1');

    wrapper = getShallowComponent({
      component: {
        ...minimalProps.component,
        licenseLegalData: {
          ...minimalProps.component.licenseLegalData,
          obligations: [
            { name: 'obligation1', originalStatus: 'FULFILLED' }, { name: 'obligation2', originalStatus: 'FULFILLED' }
          ]
        }
      }
    });
    expect(wrapper.find('#component-overview-tile-fulfilled')).toHaveText('2');
  });

  it('renders the flagged count', function() {
    let wrapper = getShallowComponent({
      component: {
        ...minimalProps.component,
        licenseLegalData: {
          ...minimalProps.component.licenseLegalData,
          obligations: [{ name: 'obligation1', originalStatus: 'other' }]
        }
      }
    });
    expect(wrapper.find('#component-overview-tile-flagged')).toHaveText('0');

    wrapper = getShallowComponent({
      component: {
        ...minimalProps.component,
        licenseLegalData: {
          ...minimalProps.component.licenseLegalData,
          obligations: [{ name: 'obligation1', originalStatus: 'FLAGGED' }]
        }
      }
    });
    expect(wrapper.find('#component-overview-tile-flagged')).toHaveText('1');

    wrapper = getShallowComponent({
      component: {
        ...minimalProps.component,
        licenseLegalData: {
          ...minimalProps.component.licenseLegalData,
          obligations: [
            { name: 'obligation1', originalStatus: 'FLAGGED' }, { name: 'obligation2', originalStatus: 'FLAGGED' }
          ]
        }
      }
    });
    expect(wrapper.find('#component-overview-tile-flagged')).toHaveText('2');
  });

  it('renders the not applicable count', function() {
    wrapper = getShallowComponent({
      component: {
        ...minimalProps.component,
        licenseLegalData: {
          ...minimalProps.component.licenseLegalData,
          obligations: [{ name: 'obligation1', originalStatus: 'other' }]
        }
      }
    });
    expect(wrapper.find('#component-overview-tile-not-applicable')).toHaveText('0');

    let wrapper = getShallowComponent({
      component: {
        ...minimalProps.component,
        licenseLegalData: {
          ...minimalProps.component.licenseLegalData,
          obligations: [{ name: 'obligation1', originalStatus: 'IGNORED' }]
        }
      }
    });
    expect(wrapper.find('#component-overview-tile-not-applicable')).toHaveText('1');

    wrapper = getShallowComponent({
      component: {
        ...minimalProps.component,
        licenseLegalData: {
          ...minimalProps.component.licenseLegalData,
          obligations: [
            { name: 'obligation1', originalStatus: 'IGNORED' }, { name: 'obligation2', originalStatus: 'IGNORED' }
          ]
        }
      }
    });
    expect(wrapper.find('#component-overview-tile-not-applicable')).toHaveText('2');
  });

  it('renders the licenses', function() {
    const wrapper = getShallowComponent();
    expect(wrapper.find('#component-overview-tile-licenses')).toHaveText(
        'License-1.0, License-2.0, License-1.0-License-2.0');
  });

  it('renders the highest license threat group', function() {
    const wrapper = getShallowComponent();
    const policyViolationIndicator = wrapper.find(NxPolicyViolationIndicator);
    expect(policyViolationIndicator).toHaveProp('policyThreatLevel', 8);
    expect(policyViolationIndicator).toHaveText('MyLtg');
  });

  it('renders the stages', function() {
    const wrapper = getShallowComponent();
    expect(wrapper.find('#component-overview-tile-build')).toHaveText('Build ' + 'terseAgo ' + getTimeDaysAgo(1));
    expect(wrapper.find('#component-overview-tile-stage-release')).toHaveText('Stage');
    expect(wrapper.find('#component-overview-tile-release')).toHaveText('Release');
    expect(wrapper.find('#component-overview-tile-operate')).toHaveText('Operate');
  });
});
