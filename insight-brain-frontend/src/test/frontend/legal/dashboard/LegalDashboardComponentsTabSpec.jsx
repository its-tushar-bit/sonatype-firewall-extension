/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as enzymeUtils from '../../enzymeUtils';
import {
  NxPagination,
  NxTable,
  NxTableCell,
  NxTableHead,
  NxTextInput,
  NxButton,
} from '@sonatype/react-shared-components';
import LegalDashboardComponentsTab from '../../../../main/frontend/legal/dashboard/LegalDashboardComponentsTab';
import LegalDashboardComponentRow from '../../../../main/frontend/legal/dashboard/LegalDashboardComponentRow';
import { DASHBOARD } from '../../../../main/frontend/legal/advancedLegalConstants';

describe('LegalDashboardComponentsTab component', function () {
  let getShallowComponent;

  const componentSearchInput = { isPristine: true, value: '', trimmedValue: '', validationErrors: null };
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
      componentSearchInput,
    },
    fetchBackendPage: () => {},
    changeSortField: () => {},
    stateGo: () => {},
    changeComponentNameToSearch: () => {},
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

  it('renders a filter input with send button', function () {
    const wrapper = getShallowComponent();
    let filterInput = wrapper.find(NxTextInput);
    let searchButton = wrapper.find(NxButton);
    expect(filterInput).toExist();
    expect(searchButton).toExist();
    expect(searchButton).toHaveProp('variant', 'primary');
  });

  it('renders the component search box text using the state passed through', function () {
    const customMinimalProps = {
      ...minimalProps,
      components: {
        ...minimalProps.components,
        componentSearchInput: {
          isPristine: false,
          value: 'componentSearchInput',
          trimmedValue: 'componentSearchInput',
          validationErrors: null,
        },
      },
    };
    const wrapper = enzymeUtils.getShallowComponent(LegalDashboardComponentsTab, customMinimalProps)();
    let filterInput = wrapper.find(NxTextInput);
    expect(filterInput).toHaveProp('value', customMinimalProps.components.componentSearchInput.value);
  });

  it('renders the component search box validation error', function () {
    const customMinimalProps = {
      ...minimalProps,
      components: {
        ...minimalProps.components,
        componentSearchInput: {
          isPristine: false,
          value: '12',
          trimmedValue: '12',
          validationErrors: 'validation error',
        },
      },
    };
    const wrapper = enzymeUtils.getShallowComponent(LegalDashboardComponentsTab, customMinimalProps)();
    let filterInput = wrapper.find(NxTextInput);
    expect(filterInput).toHaveProp(
      'validationErrors',
      customMinimalProps.components.componentSearchInput.validationErrors
    );
  });

  it('renders LegalDashboardComponentRow components for each component passed in', function () {
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
        componentSearchInput,
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

  it('paginates locally without calling backend until reaching end of pages loaded', function () {
    const { itemsPerPage, pagesToFill } = DASHBOARD.components;
    const items = [];
    for (let index = 0; index < itemsPerPage * pagesToFill; index++) {
      items.push({
        applicationId: `app${index}`,
        applicationName: `app${index}`,
        lastScanTime: 1000,
        applicationTagNames: ['tag'],
        componentsReviewedCount: 1,
        componentsTotalCount: 2,
      });
    }

    const appProps = {
      components: {
        results: items,
        totalResultsCount: items.length * 3,
        backendPage: 1,
        componentSearchInput,
      },
      fetchBackendPage: () => {},
    };

    spyOn(appProps, 'fetchBackendPage');

    const wrapper = enzymeUtils.getShallowComponent(LegalDashboardComponentsTab, appProps)();
    let pagination = wrapper.find(NxPagination);
    expect(pagination).toExist();

    const onChangePage = pagination.prop('onChange');
    for (let index = 0; index < pagesToFill; index++) {
      onChangePage(index);
    }

    expect(appProps.fetchBackendPage).not.toHaveBeenCalled();

    onChangePage(pagesToFill);
    expect(appProps.fetchBackendPage).toHaveBeenCalledWith('components', 2);

    onChangePage(pagesToFill * 2);
    expect(appProps.fetchBackendPage).toHaveBeenCalledWith('components', 3);
  });

  it('changes the sortField properly', function () {
    spyOn(minimalProps, 'changeSortField');
    const wrapper = getShallowComponent();
    const table = wrapper.find(NxTable);
    const tableHeadCells = table.find(NxTableHead).find(NxTableCell);

    expect(tableHeadCells).toExist();
    expect(tableHeadCells.length).toBe(5);

    const expectedResults = ['COMPONENT_NAME', 'LICENSE_NAME', 'APPLICATION_COUNT'];

    for (let index = 0; index < 3; index++) {
      const onClickSort = tableHeadCells.at(index).prop('onClick');

      onClickSort();
      let expectedResult = `${expectedResults[index]}_ASC`;
      expect(minimalProps.changeSortField).toHaveBeenCalledWith('components', expectedResult);
      minimalProps.components.sortField = expectedResult;

      onClickSort();
      expectedResult = `${expectedResults[index]}_DESC`;
      expect(minimalProps.changeSortField).toHaveBeenCalledWith('components', expectedResult);
      minimalProps.components.sortField = expectedResult;

      onClickSort();
      expectedResult = null;
      expect(minimalProps.changeSortField).toHaveBeenCalledWith('components', expectedResult);
    }
  });
});
