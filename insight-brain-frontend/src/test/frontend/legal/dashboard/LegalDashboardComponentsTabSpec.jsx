/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as enzymeUtils from '../../enzymeUtils';
import { NxTable } from '@sonatype/react-shared-components';
import LegalDashboardComponentsTab from '../../../../main/frontend/legal/dashboard/LegalDashboardComponentsTab';
import LegalDashboardComponentRow from '../../../../main/frontend/legal/dashboard/LegalDashboardComponentRow';
import { DASHBOARD } from '../../../../main/frontend/legal/advancedLegalConstants';

describe('LegalDashboardComponentsTab component', function () {
  let getShallowComponent;

  const minimalProps = {
    components: {
      results: [
        {
          applicationOccurrences: 1,
          displayName: 'com.amazonaws : aws-java-sdk-sqs : 1.11.415',
          hash: '8c5c838e0c6d2f6cdf30',
          licenseNames: ['Apache-2.0', 'GPL 1'],
          reviewCompletedCount: 0,
          reviewTotalCount: 0,
        },
        {
          applicationOccurrences: 1,
          displayName: 'com.amazonaws : aws-java-sdk-sqs : 2.11.415',
          hash: '8c5c838e0c6d2f6cdf31',
          licenseNames: ['Apache-2.0'],
          reviewCompletedCount: 2,
          reviewTotalCount: 4,
        },
        {
          applicationOccurrences: 1,
          displayName: 'com.amazonaws : aws-java-sdk-sqs : 3.11.415',
          hash: '8c5c838e0c6d2f6cdf32',
          licenseNames: ['Apache-2.0'],
          reviewCompletedCount: 0,
          reviewTotalCount: 0,
        },
        {
          applicationOccurrences: 1,
          displayName: 'com.amazonaws : aws-java-sdk-sqs : 4.11.415',
          hash: '8c5c838e0c6d2f6cdf33',
          licenseNames: ['Apache-2.0'],
          reviewCompletedCount: 0,
          reviewTotalCount: 0,
        },
        {
          applicationOccurrences: 1,
          displayName: 'com.amazonaws : aws-java-sdk-sqs : 5.11.415',
          hash: '8c5c838e0c6d2f6cdf34',
          licenseNames: ['Apache-2.0'],
          reviewCompletedCount: 0,
          reviewTotalCount: 0,
        },
        {
          applicationOccurrences: 1,
          displayName: 'com.amazonaws : aws-java-sdk-sqs : 6.11.415',
          hash: '8c5c838e0c6d2f6cdf35',
          licenseNames: ['Apache-2.0'],
          reviewCompletedCount: 0,
          reviewTotalCount: 0,
        },
        {
          applicationOccurrences: 1,
          displayName: 'com.amazonaws : aws-java-sdk-sqs : 7.11.415',
          hash: '8c5c838e0c6d2f6cdf36',
          licenseNames: ['Apache-2.0'],
          reviewCompletedCount: 0,
          reviewTotalCount: 0,
        },
        {
          applicationOccurrences: 1,
          displayName: 'com.amazonaws : aws-java-sdk-sqs : 8.11.415',
          hash: '8c5c838e0c6d2f6cdf37',
          licenseNames: ['Apache-2.0'],
          reviewCompletedCount: 0,
          reviewTotalCount: 0,
        },
        {
          applicationOccurrences: 1,
          displayName: 'com.amazonaws : aws-java-sdk-sqs : 9.11.415',
          hash: '8c5c838e0c6d2f6cdf38',
          licenseNames: ['Apache-2.0'],
          reviewCompletedCount: 0,
          reviewTotalCount: 0,
        },
        {
          applicationOccurrences: 1,
          displayName: 'com.amazonaws : aws-java-sdk-sqs : 10.11.415',
          hash: '8c5c838e0c6d2f6cdf39',
          licenseNames: ['Apache-2.0'],
          reviewCompletedCount: 0,
          reviewTotalCount: 0,
        },
        {
          applicationOccurrences: 1,
          displayName: 'com.amazonaws : aws-java-sdk-sqs : 10.11.415',
          hash: '8c5c838e0c6d2f6cdf40',
          licenseNames: ['Apache-2.0'],
          reviewCompletedCount: 0,
          reviewTotalCount: 0,
        },
      ],
      totalResultsCount: 11,
      backendPage: 1,
    },
    fetchBackendPage: () => {},
    stateGo: () => {},
  };

  beforeEach(function () {
    getShallowComponent = enzymeUtils.getShallowComponent(LegalDashboardComponentsTab, minimalProps);
  });

  it('renders a table', function () {
    const wrapper = getShallowComponent();
    let table = wrapper.find(NxTable);
    expect(table).toExist();
    expect(table).toHaveClassName('legal-dashboard-table');
  });

  it('renders LegalDashboardComponentRow components for each application passed in', function () {
    const wrapper = getShallowComponent();
    let table = wrapper.find(NxTable);
    let rows = table.find(LegalDashboardComponentRow);
    expect(rows).toExist();
    expect(rows.length).toEqual(DASHBOARD.components.itemsPerPage);
    expect(rows.at(0)).toHaveProp('row', minimalProps.components.results[0]);
    expect(rows.at(1)).toHaveProp('row', minimalProps.components.results[1]);
    expect(rows.length === DASHBOARD.components.itemsPerPage);
  });

  it('renders a no result legend when no components are available', function () {
    const wrapper = enzymeUtils.getMountedComponent(LegalDashboardComponentsTab, {
      components: {
        results: [],
      },
    })();
    let table = wrapper.find(NxTable);
    let rows = table.find(LegalDashboardComponentRow);
    expect(rows).not.toExist();
    expect(rows.length).toEqual(0);
    expect(wrapper.find('.nx-cell--meta-info').text()).toEqual(
      'No components found given the applied filters and permissions.'
    );
  });
});
