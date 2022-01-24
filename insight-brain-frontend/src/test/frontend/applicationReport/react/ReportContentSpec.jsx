/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';

import * as enzymeUtils from '../../enzymeUtils';
import { render, screen, fireEvent } from '../../SpecUtil';
import ReportContent from '../../../../main/frontend/applicationReport/react/ReportContent';
import {
  NxTable,
  NxTableBody,
  NxTableCell,
  NxTableHead,
  NxTableRow,
  NxFilterInput,
} from '@sonatype/react-shared-components';

describe('ReportContent component', function () {
  let minimalProps, getShallowComponent, renderComponent;

  beforeEach(function () {
    minimalProps = {
      toggleAggregateReportEntries: jasmine.createSpy('toggleAggregateReportEntries'),
      selectedReport: {
        displayedEntries: [],
      },
    };

    renderComponent = (additionalProps = {}) => render(<ReportContent {...minimalProps} {...additionalProps} />);
    getShallowComponent = enzymeUtils.getShallowComponent(ReportContent, minimalProps);
  });

  it('renders a tile', function () {
    const shallowComponent = getShallowComponent();
    expect(shallowComponent).toMatchSelector('.nx-tile');
  });

  it('renders aggregate by component toggle', function () {
    renderComponent();
    const aggregateByComponentToggle = screen.getByLabelText('Aggregate by component');

    expect(aggregateByComponentToggle).toBeVisible();
  });

  it('dispatches correct action when toggling aggregate by component toggle', function () {
    renderComponent();
    const aggregateByComponentToggle = screen.getByLabelText('Aggregate by component');

    fireEvent.click(aggregateByComponentToggle);
    expect(minimalProps.toggleAggregateReportEntries).toHaveBeenCalled();
  });

  it('renders table header and body', function () {
    const shallowComponent = getShallowComponent();
    const table = shallowComponent.find(NxTable);
    const header = shallowComponent.find(NxTableHead);
    const body = shallowComponent.find(NxTableBody);
    expect(table).toExist();
    expect(header).toExist();
    expect(body).toExist();
  });

  it('sets the emptyMessage prop on the NxTableBody', function () {
    const tableBody = getShallowComponent().find(NxTableBody);
    expect(tableBody).toHaveProp('emptyMessage', 'No Results');
  });

  it('render the table header with sordir desc', function () {
    const props = {
      selectedReport: {
        displayedEntries: [
          {
            derivedComponentName: 'Component A',
            policyName: 'None',
            policyThreatLevel: 0,
          },
          {
            derivedComponentName: 'Component B',
            policyName: 'Security-High',
            policyThreatLevel: 9,
          },
        ],
      },
      sortConfiguration: {
        key: 'policyThreatLevel',
        sortFields: ['-policyThreatLevel', 'policyName', 'derivedComponentName'],
        dir: 'desc',
      },
    };

    const shallowComponent = getShallowComponent(props),
      head = shallowComponent.find(NxTableHead),
      rows = head.find(NxTableRow),
      firstRowTds = rows.at(0).find(NxTableCell);

    expect(firstRowTds.at(0)).toHaveProp('isSortable', true);
    expect(firstRowTds.at(0)).toHaveProp('sortDir', 'desc');
    expect(firstRowTds.at(1)).toHaveProp('isSortable', true);
    expect(firstRowTds.at(2)).toHaveProp('isSortable', true);
  });

  it('render the table header with sordir asc', function () {
    const props = {
      selectedReport: {
        displayedEntries: [
          {
            derivedComponentName: 'Component A',
            policyName: 'None',
            policyThreatLevel: 0,
          },
          {
            derivedComponentName: 'Component B',
            policyName: 'Security-High',
            policyThreatLevel: 9,
          },
        ],
      },
      sortConfiguration: {
        key: 'policyName',
        sortFields: ['policyName', '-policyThreatLevel', 'derivedComponentName'],
        dir: 'asc',
      },
    };

    const shallowComponent = getShallowComponent(props),
      head = shallowComponent.find(NxTableHead),
      rows = head.find(NxTableRow),
      firstRowTds = rows.at(0).find(NxTableCell);

    expect(firstRowTds.at(0)).toHaveProp('isSortable', true);
    expect(firstRowTds.at(1)).toHaveProp('isSortable', true);
    expect(firstRowTds.at(1)).toHaveProp('sortDir', 'asc');
    expect(firstRowTds.at(2)).toHaveProp('isSortable', true);
  });

  it('render the table header with filters', function () {
    const setStringFieldFilterSpy = jasmine.createSpy('setStringFieldFilter');

    const props = {
      setStringFieldFilter: setStringFieldFilterSpy,
      selectedReport: {
        displayedEntries: [
          {
            derivedComponentName: 'Component A',
            policyName: 'None',
            policyThreatLevel: 0,
          },
          {
            derivedComponentName: 'Component B',
            policyName: 'Security-High',
            policyThreatLevel: 9,
          },
        ],
      },
      sortConfiguration: {
        key: 'policyThreatLevel',
        sortFields: ['-policyThreatLevel', 'policyName', 'derivedComponentName'],
        dir: 'desc',
      },
      substringFilters: {
        policyName: 'policyName',
        derivedComponentName: 'derivedComponentName',
      },
    };

    const shallowComponent = getShallowComponent(props),
      head = shallowComponent.find(NxTableHead),
      rows = head.find(NxTableRow),
      secondRowTds = rows.at(1).find(NxTableCell),
      policyNameFilter = secondRowTds.at(0).find(NxFilterInput),
      derivedComponentNameFilter = secondRowTds.at(1).find(NxFilterInput);

    expect(policyNameFilter).toHaveProp('placeholder', 'policy name');
    expect(policyNameFilter).toHaveProp('value', 'policyName');
    expect(derivedComponentNameFilter).toHaveProp('placeholder', 'component name');
    expect(derivedComponentNameFilter).toHaveProp('value', 'derivedComponentName');
    policyNameFilter.simulate('change', 'High');
    derivedComponentNameFilter.simulate('change', 'A');
    expect(setStringFieldFilterSpy).toHaveBeenCalledWith('policyName', 'High');
    expect(setStringFieldFilterSpy).toHaveBeenCalledWith('derivedComponentName', 'A');
  });

  it('renders a ReportTableRow for each entry', function () {
    const props = {
        selectedReport: {
          displayedEntries: [
            {
              derivedComponentName: 'Component B',
              policyName: 'Security-Critical',
              policyThreatLevel: 9,
            },
            {
              derivedComponentName: 'Component A',
              policyName: 'None',
              policyThreatLevel: 0,
            },
          ],
        },
        sortConfiguration: {
          key: 'policyThreatLevel',
          sortFields: ['-policyThreatLevel', 'policyName', 'derivedComponentName'],
          dir: 'desc',
        },
      },
      shallowComponent = getShallowComponent(props),
      body = shallowComponent.find(NxTableBody),
      tableRow = body.find('ReportTableRow');

    expect(body).toExist();
    expect(tableRow).toExist();
    expect(tableRow.at(0)).toHaveProp('component', props.selectedReport.displayedEntries[0]);
    expect(tableRow.at(0)).toHaveProp('index', 0);
    expect(tableRow.at(1)).toHaveProp('component', props.selectedReport.displayedEntries[1]);
    expect(tableRow.at(1)).toHaveProp('index', 1);
  });

  it('dispatches action on filter`s click', function () {
    const toggleShowFilterPopoverSpy = jasmine.createSpy('toggleShowFilterPopover');
    const props = {
      toggleShowFilterPopover: toggleShowFilterPopoverSpy,
    };
    const shallowComponent = getShallowComponent(props);
    const button = shallowComponent.find('#filters-toggle-button');

    expect(button).toExist();
    button.simulate('click');
    expect(toggleShowFilterPopoverSpy).toHaveBeenCalledTimes(1);
  });

  it('when there is a DT dispatches action on DT button click and is enabled', function () {
    const goToDependencyTreePageSpy = jasmine.createSpy('goToDependencyTreePage');
    const props = {
      goToDependencyTreePage: goToDependencyTreePageSpy,
      dependencyTreeIsAvailable: true,
      dependencyTreeUnavailableMessage: '',
    };
    const shallowComponent = getShallowComponent(props);
    const button = shallowComponent.find('#dependency-tree-button');

    expect(button).toExist();
    expect(button).not.toHaveClassName('disabled');
    button.simulate('click');
    expect(goToDependencyTreePageSpy).toHaveBeenCalledTimes(1);
  });

  it('when there is not a DT does not dispatches action on DT button click and is disabled ', function () {
    const goToDependencyTreePageSpy = jasmine.createSpy('goToDependencyTreePage');
    const props = {
      goToDependencyTreePage: goToDependencyTreePageSpy,
      dependencyTreeIsAvailable: false,
      dependencyTreeUnavailableMessage: 'some text',
    };
    const shallowComponent = getShallowComponent(props);
    const button = shallowComponent.find('#dependency-tree-button');

    expect(button).toExist();
    expect(button).toHaveClassName('disabled');
    button.simulate('click');
    expect(goToDependencyTreePageSpy).toHaveBeenCalledTimes(0);
    expect(button).toHaveProp('title', 'some text');
  });
});
