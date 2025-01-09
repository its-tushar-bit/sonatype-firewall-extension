/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as enzymeUtils from '../../../enzymeUtils';
import { NxButton, NxFontAwesomeIcon, NxTableBody, NxTableCell, NxTableRow } from '@sonatype/react-shared-components';

import ComponentWaiversPopoverTable from 'MainRoot/componentDetails/ViolationsTableTile/componentWaivers/ComponentWaiversPopoverTable';
import { faTrashAlt } from '@fortawesome/pro-solid-svg-icons';
import { waiverMatcherStrategy } from 'MainRoot/util/waiverUtils';
import { formatDate, STANDARD_DATE_FORMAT } from 'MainRoot/util/dateUtils';
import WaiverRow from 'MainRoot/waivers/WaiverRow';
import ComponentDisplay from 'MainRoot/ComponentDisplay/ReactComponentDisplay';

describe('ComponentWaiversPopover', function () {
  let minimalProps, getShallowComponent, getMountedComponent;
  const waiverCreateTime = new Date(1627942284167);
  const waiverCreateDate = formatDate(waiverCreateTime, STANDARD_DATE_FORMAT);

  beforeEach(function () {
    const waivers = [
      {
        policyId: 'policyId1',
        policyName: 'policyName1',
        policyWaiverId: 'policyWaiverId1',
        scopeOwnerId: 'scopeOwnerId1',
        scopeOwnerName: 'owner1',
        scopeOwnerType: 'application',
        componentMatchStrategy: waiverMatcherStrategy.EXACT_COMPONENT,
        hash: 'hash-1',
        constraintFacts: [{ constraintName: 'constraint-1' }],
        createTime: waiverCreateTime,
      },
      {
        policyId: 'policyId2',
        policyName: 'policyName2',
        policyWaiverId: 'policyWaiverId2',
        scopeOwnerId: 'scopeOwnerId1',
        scopeOwnerName: 'owner1',
        scopeOwnerType: 'organization',
        componentMatchStrategy: waiverMatcherStrategy.ALL_COMPONENTS,
        hash: null,
        constraintFacts: [{ constraintName: 'constraint-2' }],
        createTime: waiverCreateTime,
        comment: 'Some comment',
      },
      {
        policyId: 'policyId3',
        policyName: 'policyName3',
        policyWaiverId: 'policyWaiverId3',
        scopeOwnerId: 'scopeOwnerId3',
        scopeOwnerName: 'owner1',
        scopeOwnerType: 'application',
        componentMatchStrategy: waiverMatcherStrategy.ALL_VERSIONS,
        hash: 'hash-2',
        constraintFacts: [{ constraintName: 'constraint-3' }],
        createTime: waiverCreateTime,
        componentName: 'component name',
        creatorName: 'creator name',
      },
    ];
    minimalProps = {
      waivers,
      setWaiverToDelete: jasmine.createSpy('setWaiverToDelete'),
      componentName: 'A component name : 1.0',
      componentNameWithoutVersion: 'A component name',
    };

    getShallowComponent = enzymeUtils.getShallowComponent(ComponentWaiversPopoverTable, minimalProps);
    getMountedComponent = enzymeUtils.getMountedComponent(ComponentWaiversPopoverTable, minimalProps);
  });

  const assertDeleteWaiverBtn = (cell, waiver) => {
    const btn = cell.find(NxButton);
    expect(btn).toHaveProp('variant', 'icon-only');
    expect(btn).toHaveProp('title', 'Delete');
    expect(btn.find(NxFontAwesomeIcon)).toHaveProp('icon', faTrashAlt);
    btn.simulate('click');
    expect(minimalProps.setWaiverToDelete).toHaveBeenCalledWith(waiver);
  };

  it('renders an NxTable with empty message if no waivers are passed', () => {
    const component = getShallowComponent({ waivers: [] }).dive();
    const tableBody = component.find(NxTableBody);
    expect(tableBody).toExist();
    expect(tableBody).toHaveProp('emptyMessage', 'No existing component waivers');
    expect(tableBody.children.length).toBe(1);
  });

  it('renders a WaiverRow per each waiver', () => {
    const component = getMountedComponent();
    const tableBody = component.find(NxTableBody);
    const rows = tableBody.find(WaiverRow);
    expect(rows.length).toBe(3);

    const row1 = rows.at(0).find(NxTableRow);
    const cellsRow1 = row1.find(NxTableCell);
    expect(cellsRow1.length).toBe(3);
    expect(cellsRow1.at(0)).toIncludeText(waiverCreateDate);
    expect(cellsRow1.at(1)).toIncludeText('Application - owner1');
    expect(cellsRow1.at(1).find(ComponentDisplay)).toIncludeText('A component name : 1.0');
    expect(cellsRow1.at(1)).toIncludeText('—');
    assertDeleteWaiverBtn(cellsRow1.at(2), minimalProps.waivers[0]);

    const row2 = rows.at(1).find(NxTableRow);
    const cellsRow2 = row2.find(NxTableCell);
    expect(cellsRow2.length).toBe(3);
    expect(cellsRow2.at(0)).toIncludeText(waiverCreateDate);
    expect(cellsRow2.at(1)).toIncludeText('Organization - owner1');
    expect(cellsRow2.at(1)).toIncludeText('All');
    expect(cellsRow1.at(1)).toIncludeText('—');
    expect(cellsRow2.at(1)).toIncludeText('Some comment');
    assertDeleteWaiverBtn(cellsRow2.at(2), minimalProps.waivers[1]);

    const row3 = rows.at(2).find(NxTableRow);
    const cellsRow3 = row3.find(NxTableCell);
    expect(cellsRow3.length).toBe(3);
    expect(cellsRow3.at(0)).toIncludeText(waiverCreateDate);
    expect(cellsRow3.at(1)).toIncludeText('Application - owner1');
    expect(cellsRow3.at(1).find(ComponentDisplay)).toIncludeText('A component name (all versions)');
    expect(cellsRow3.at(1)).toIncludeText('creator name');
    expect(cellsRow3.at(1)).toIncludeText('—');
    assertDeleteWaiverBtn(cellsRow3.at(2), minimalProps.waivers[2]);
  });
});
