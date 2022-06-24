/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as enzymeUtils from '../../../enzymeUtils';
import { NxButton, NxFontAwesomeIcon, NxTableBody, NxTableCell, NxTableRow } from '@sonatype/react-shared-components';

import ComponentWaiversPopoverTable, {
  ComponentWaiversTableRow,
} from 'MainRoot/componentDetails/ViolationsTableTile/componentWaivers/ComponentWaiversPopoverTable';
import { faTrashAlt } from '@fortawesome/pro-solid-svg-icons';
import { waiverMatcherStrategy } from 'MainRoot/util/waiverUtils';

describe('ComponentWaiversPopover', function () {
  let minimalProps, getShallowComponent;
  const waiverCreateTime = new Date(1627942284167);
  const waiverCreateDate =
    (waiverCreateTime.getMonth() + 1).toString().padStart(2, '0') +
    '/' +
    waiverCreateTime.getDate().toString().padStart(2, '0') +
    '/' +
    waiverCreateTime.getFullYear();

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
        createTime: waiverCreateTime.getTime(),
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
        createTime: waiverCreateTime.getTime(),
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
        createTime: waiverCreateTime.getTime(),
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

  it('renders a ComponentWaiversTableRow per each waiver', () => {
    const component = getShallowComponent().dive();
    const tableBody = component.find(NxTableBody);
    const rows = tableBody.find(ComponentWaiversTableRow);
    expect(rows.length).toBe(3);

    const row1 = rows.at(0).dive().find(NxTableRow);
    const cellsRow1 = row1.find(NxTableCell);
    expect(cellsRow1.length).toBe(7);
    expect(cellsRow1.at(0).dive()).toHaveText('policyName1constraint-1');
    expect(cellsRow1.at(1).dive()).toHaveText(waiverCreateDate);
    expect(cellsRow1.at(2).dive()).toHaveText('Application - owner1');
    expect(cellsRow1.at(3).dive()).toHaveText('A component name : 1.0');
    expect(cellsRow1.at(4).dive()).toHaveText('- -');
    expect(cellsRow1.at(5).dive()).toHaveText('- -');
    assertDeleteWaiverBtn(cellsRow1.at(6), minimalProps.waivers[0]);

    const row2 = rows.at(1).dive().find(NxTableRow);
    const cellsRow2 = row2.find(NxTableCell);
    expect(cellsRow2.length).toBe(7);
    expect(cellsRow2.at(0).dive()).toHaveText('policyName2constraint-2');
    expect(cellsRow2.at(1).dive()).toHaveText(waiverCreateDate);
    expect(cellsRow2.at(2).dive()).toHaveText('Organization - owner1');
    expect(cellsRow2.at(3).dive()).toHaveText('All');
    expect(cellsRow1.at(4).dive()).toHaveText('- -');
    expect(cellsRow2.at(5).dive()).toHaveText('Some comment');
    assertDeleteWaiverBtn(cellsRow2.at(6), minimalProps.waivers[1]);

    const row3 = rows.at(2).dive().find(NxTableRow);
    const cellsRow3 = row3.find(NxTableCell);
    expect(cellsRow3.length).toBe(7);
    expect(cellsRow3.at(0).dive()).toHaveText('policyName3constraint-3');
    expect(cellsRow3.at(1).dive()).toHaveText(waiverCreateDate);
    expect(cellsRow3.at(2).dive()).toHaveText('Application - owner1');
    expect(cellsRow3.at(3).dive()).toHaveText('A component name (all versions)');
    expect(cellsRow3.at(4).dive()).toHaveText('creator name');
    expect(cellsRow3.at(5).dive()).toHaveText('- -');
    assertDeleteWaiverBtn(cellsRow3.at(6), minimalProps.waivers[2]);
  });
});
