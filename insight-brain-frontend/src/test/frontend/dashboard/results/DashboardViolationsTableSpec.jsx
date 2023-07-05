/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { lensPath, set } from 'ramda';
import { NxPagination, NxTable, NxTableBody, NxTableHead, NxTableRow } from '@sonatype/react-shared-components';
import * as enzymeUtils from '../../enzymeUtils';

describe('DashboardViolationsTable', function () {
  let minimalProps,
    getShallowComponent,
    reloadSpy,
    sortViolationsSpy,
    setViolationsPageSpy,
    DashboardViolationsTableRowMock,
    DashboardViolationsTable;

  beforeEach(() => {
    reloadSpy = jasmine.createSpy('reload');
    sortViolationsSpy = jasmine.createSpy('sortViolations');
    setViolationsPageSpy = jasmine.createSpy('setViolationsPage');
    DashboardViolationsTableRowMock = jasmine
      .createSpy('DashboardViolationsTableRow')
      .and.returnValue(<div>DashboardViolationsTableRow</div>);

    DashboardViolationsTable = require('inject-loader!../../../../main/frontend/dashboard/results/violations/DashboardViolationsTable')(
      {
        './DashboardViolationsTableRow': DashboardViolationsTableRowMock,
      }
    ).default;

    minimalProps = {
      reload: reloadSpy,
      sortViolations: sortViolationsSpy,
      stateGo: () => {},
      maxDaysOld: 0,
      needsAcknowledgement: false,
      violations: {
        results: [
          {
            policyViolationId: 'policyViolationId1',
            threatLevel: 7,
            policyName: 'policyName1',
            applicationName: 'App1',
            firstOccurrenceTime: Date.now(),
          },
          {
            policyViolationId: 'policyViolationId2',
            threatLevel: 9,
            policyName: 'policyName2',
            applicationName: 'App1',
            firstOccurrenceTime: Date.now(),
          },
          {
            policyViolationId: 'policyViolationId3',
            threatLevel: 5,
            policyName: 'policyName3',
            applicationName: 'App1',
            firstOccurrenceTime: Date.now(),
          },
        ],
        numResults: 3,
        sortFields: ['-threatLevel', '-firstOccurrenceTime'],
        pageCount: 1,
        page: 0,
      },
      setViolationsPage: setViolationsPageSpy,
    };

    getShallowComponent = enzymeUtils.getShallowComponent(DashboardViolationsTable, minimalProps);
  });

  it('renders a container for the table', () => {
    const component = getShallowComponent(),
      table = component.find(NxTable);

    expect(component).toMatchSelector('.nx-scrollable');
    expect(component).toMatchSelector('.nx-table-container');
    expect(component).toMatchSelector('.nx-viewport-sized__scrollable');
    expect(table).toMatchSelector('.nx-table--fixed-layout');
  });

  it('renders a table header with the appropriate headers', () => {
    const component = getShallowComponent(),
      table = component.find(NxTable),
      headerRow = table.find(NxTableHead).find(NxTableRow),
      threatHeaderCell = headerRow.childAt(0),
      policyHeaderCell = headerRow.childAt(1),
      applicationHeaderCell = headerRow.childAt(2),
      componentHeaderCell = headerRow.childAt(3),
      ageHeaderCell = headerRow.childAt(4);

    expect(threatHeaderCell).toExist();
    expect(policyHeaderCell).toExist();
    expect(applicationHeaderCell).toExist();
    expect(componentHeaderCell).toExist();
    expect(ageHeaderCell).toExist();
  });

  describe('NxTableBody', () => {
    it('renders a needs acknowledgement message if needsAcknowledgement is true', () => {
      const component = getShallowComponent({ needsAcknowledgement: true }),
        tBody = component.find(NxTableBody),
        tRow = tBody.childAt(0).dive(),
        infoBox = tRow.find('#needs-acknowledgement');

      expect(tBody.children().length).toEqual(1);
      expect(infoBox).toHaveText("Select your filter criteria and click 'apply' to see results.");
    });

    it('renders an empty message if the table is empty', () => {
      const props = {
        ...minimalProps,
        violations: {
          ...minimalProps.violations,
          results: [],
        },
      };
      let component, tableBody, expectedEmptyMessage;

      component = getShallowComponent(props);
      tableBody = component.find(NxTableBody);
      expectedEmptyMessage = 'No data available given the applied filters and permissions.';

      expect(tableBody).toHaveProp('emptyMessage', expectedEmptyMessage);

      component = getShallowComponent({ ...props, maxDaysOld: 7 });
      tableBody = component.find(NxTableBody);
      expectedEmptyMessage = 'No data available in the last 7 days given the applied filters and permissions.';

      expect(tableBody).toHaveProp('emptyMessage', expectedEmptyMessage);
    });

    it('renders an error and a retry handler button if there is an error in the state', () => {
      const props = {
        ...minimalProps,
        violations: {
          ...minimalProps.violations,
          error: 'Something went wrong',
        },
      };
      const component = getShallowComponent(props),
        tableBody = component.find(NxTableBody),
        retry = tableBody.prop('retryHandler');

      expect(tableBody).toHaveProp('error', 'Something went wrong');

      retry();
      expect(reloadSpy).toHaveBeenCalled();
    });
  });

  describe('Cell sorting', () => {
    it('calls the sortViolations function with the threat column fields if clicked: asc to desc', () => {
      const props = {
        ...minimalProps,
        violations: {
          ...minimalProps.violations,
          sortFields: ['threatLevel', '-firstOccurrenceTime'],
        },
      };
      let component = getShallowComponent(props),
        table = component.find(NxTable),
        headerRow = table.find(NxTableHead).find(NxTableRow),
        threatHeaderCell = headerRow.childAt(0);

      expect(threatHeaderCell).toHaveProp('isSortable');
      expect(threatHeaderCell).toHaveProp('sortDir', 'asc');

      threatHeaderCell.simulate('click');
      expect(sortViolationsSpy).toHaveBeenCalledWith(['-threatLevel', '-firstOccurrenceTime']);
    });

    it('calls the sortViolations function with the threat column fields if clicked: asc to desc', () => {
      const props = {
        ...minimalProps,
        violations: {
          ...minimalProps.violations,
          sortFields: ['-threatLevel', '-firstOccurrenceTime'],
        },
      };
      let component = getShallowComponent(props),
        table = component.find(NxTable),
        headerRow = table.find(NxTableHead).find(NxTableRow),
        threatHeaderCell = headerRow.childAt(0);

      expect(threatHeaderCell).toHaveProp('isSortable');
      expect(threatHeaderCell).toHaveProp('sortDir', 'desc');

      threatHeaderCell.simulate('click');
      expect(sortViolationsSpy).toHaveBeenCalledWith(['threatLevel', '-firstOccurrenceTime']);
    });

    it('calls the sortViolations function with the policy column fields if clicked: asc to desc', () => {
      const props = {
        ...minimalProps,
        violations: {
          ...minimalProps.violations,
          sortFields: ['policyName', '-firstOccurrenceTime'],
        },
      };
      const component = getShallowComponent(props),
        table = component.find(NxTable),
        headerRow = table.find(NxTableHead).find(NxTableRow),
        policyHeaderCell = headerRow.childAt(1);

      expect(policyHeaderCell).toHaveProp('isSortable');
      expect(policyHeaderCell).toHaveProp('sortDir', 'asc');

      policyHeaderCell.simulate('click');
      expect(sortViolationsSpy).toHaveBeenCalledWith(['-policyName', '-firstOccurrenceTime']);
    });

    it('calls the sortViolations function with the policy column fields if clicked: desc to asc', () => {
      const props = {
        ...minimalProps,
        violations: {
          ...minimalProps.violations,
          sortFields: ['-policyName', '-firstOccurrenceTime'],
        },
      };
      const component = getShallowComponent(props),
        table = component.find(NxTable),
        headerRow = table.find(NxTableHead).find(NxTableRow),
        policyHeaderCell = headerRow.childAt(1);

      expect(policyHeaderCell).toHaveProp('isSortable');
      expect(policyHeaderCell).toHaveProp('sortDir', 'desc');

      policyHeaderCell.simulate('click');
      expect(sortViolationsSpy).toHaveBeenCalledWith(['policyName', '-firstOccurrenceTime']);
    });

    it('calls the sortViolations function with the application column fields if clicked: asc to desc', () => {
      const props = {
        ...minimalProps,
        violations: {
          ...minimalProps.violations,
          sortFields: ['applicationName', '-threatLevel'],
        },
      };
      const component = getShallowComponent(props),
        table = component.find(NxTable),
        headerRow = table.find(NxTableHead).find(NxTableRow),
        applicationHeaderCell = headerRow.childAt(2);

      expect(applicationHeaderCell).toHaveProp('isSortable');
      expect(applicationHeaderCell).toHaveProp('sortDir', 'asc');

      applicationHeaderCell.simulate('click');
      expect(sortViolationsSpy).toHaveBeenCalledWith(['-applicationName', '-threatLevel']);
    });

    it('calls the sortViolations function with the application column fields if clicked: desc to asc', () => {
      const props = {
        ...minimalProps,
        violations: {
          ...minimalProps.violations,
          sortFields: ['-applicationName', '-threatLevel'],
        },
      };
      const component = getShallowComponent(props),
        table = component.find(NxTable),
        headerRow = table.find(NxTableHead).find(NxTableRow),
        applicationHeaderCell = headerRow.childAt(2);

      expect(applicationHeaderCell).toHaveProp('isSortable');
      expect(applicationHeaderCell).toHaveProp('sortDir', 'desc');

      applicationHeaderCell.simulate('click');
      expect(sortViolationsSpy).toHaveBeenCalledWith(['applicationName', '-threatLevel']);
    });

    it('calls the sortViolations function with the component column fields if clicked: asc to desc', () => {
      const props = {
        ...minimalProps,
        violations: {
          ...minimalProps.violations,
          sortFields: ['derivedComponentName', '-threatLevel'],
        },
      };
      const component = getShallowComponent(props),
        table = component.find(NxTable),
        headerRow = table.find(NxTableHead).find(NxTableRow),
        componentHeaderCell = headerRow.childAt(3);

      expect(componentHeaderCell).toHaveProp('isSortable');
      expect(componentHeaderCell).toHaveProp('sortDir', 'asc');

      componentHeaderCell.simulate('click');
      expect(sortViolationsSpy).toHaveBeenCalledWith(['-derivedComponentName', '-threatLevel']);
    });

    it('calls the sortViolations function with the component column fields if clicked: desc to asc', () => {
      const props = {
        ...minimalProps,
        violations: {
          ...minimalProps.violations,
          sortFields: ['-derivedComponentName', '-threatLevel'],
        },
      };
      const component = getShallowComponent(props),
        table = component.find(NxTable),
        headerRow = table.find(NxTableHead).find(NxTableRow),
        componentHeaderCell = headerRow.childAt(3);

      expect(componentHeaderCell).toHaveProp('isSortable');
      expect(componentHeaderCell).toHaveProp('sortDir', 'desc');

      componentHeaderCell.simulate('click');
      expect(sortViolationsSpy).toHaveBeenCalledWith(['derivedComponentName', '-threatLevel']);
    });

    it('calls the sortViolations function with the age column fields if clicked: desc to asc', () => {
      const props = {
        ...minimalProps,
        violations: {
          ...minimalProps.violations,
          sortFields: ['firstOccurrenceTime', '-threatLevel'],
        },
      };
      const component = getShallowComponent(props),
        table = component.find(NxTable),
        headerRow = table.find(NxTableHead).find(NxTableRow),
        ageHeaderCell = headerRow.childAt(4);

      expect(ageHeaderCell).toHaveProp('isSortable');
      expect(ageHeaderCell).toHaveProp('sortDir', 'desc'); // age column uses inverted sort hence the flipped order.

      ageHeaderCell.simulate('click');
      expect(sortViolationsSpy).toHaveBeenCalledWith(['-firstOccurrenceTime', '-threatLevel']);
    });

    it('calls the sortViolations function with the age column fields if clicked: asc to desc', () => {
      const props = {
        ...minimalProps,
        violations: {
          ...minimalProps.violations,
          sortFields: ['-firstOccurrenceTime', '-threatLevel'],
        },
      };
      const component = getShallowComponent(props),
        table = component.find(NxTable),
        headerRow = table.find(NxTableHead).find(NxTableRow),
        ageHeaderCell = headerRow.childAt(4);

      expect(ageHeaderCell).toHaveProp('isSortable');
      expect(ageHeaderCell).toHaveProp('sortDir', 'asc'); // age column uses inverted sort hence the flipped order.

      ageHeaderCell.simulate('click');
      expect(sortViolationsSpy).toHaveBeenCalledWith(['firstOccurrenceTime', '-threatLevel']);
    });
  });

  describe('pagination', () => {
    it('renders a NxPagination component', () => {
      const pagination = getShallowComponent().find(NxPagination);
      expect(pagination).toExist();
      expect(pagination).toHaveProp('pageCount', 1);
      expect(pagination).toHaveProp('currentPage', 0);
      expect(pagination).toHaveProp('onChange', setViolationsPageSpy);
    });

    it('sets currentPage to null when there are no results', () => {
      const props = set(lensPath(['violations', 'pageCount']), 0, minimalProps);
      const pagination = getShallowComponent(props).find(NxPagination);
      expect(pagination).toHaveProp('currentPage', null);
    });
  });
});
