/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import {
  NxTable,
  NxTableBody,
  NxTableCell,
  NxOverflowTooltip,
  NxThreatIndicator,
} from '@sonatype/react-shared-components';
import * as enzymeUtils from '../enzymeUtils';

describe('FirewallQuarantineTable', function () {
  let minimalProps,
    FirewallQuarantineTable,
    getShallowComponent,
    loadQuarantineList,
    setQuarantineGridPage,
    setQuarantineGridSorting,
    setQuarantineGridPolicyFilter;

  beforeEach(function () {
    FirewallQuarantineTable = require('inject-loader!../../../main/frontend/firewall/FirewallQuarantineTable')()
      .default;

    loadQuarantineList = jasmine.createSpy('loadQuarantineList');
    setQuarantineGridPage = jasmine.createSpy('setQuarantineGridPage');
    setQuarantineGridSorting = jasmine.createSpy('setQuarantineGridSorting');
    setQuarantineGridPolicyFilter = jasmine.createSpy('setQuarantineGridPolicyFilter');

    minimalProps = {
      loadQuarantineList: loadQuarantineList,
      setQuarantineGridPage: setQuarantineGridPage,
      setQuarantineGridSorting: setQuarantineGridSorting,
      setQuarantineGridPolicyFilter: setQuarantineGridPolicyFilter,
      loadedQuarantineList: true,
      loadedPolicies: true,
      quarantinePageCount: 1,
      pageSize: 2,
      currentPage: 0,
      quarantineList: [
        {
          displayName: 'test-component',
          repository: 'central',
          quarantineDate: '2018-09-30T03:10:35.754+0000',
          dateCleared: null,
          policyViolations: [
            {
              policyId: 'test-policy-id',
              policyName: 'Security-Medium',
              policyViolationId: 'test-policy-violation-id',
              threatLevel: 5,
              constraintViolations: [],
            },
            {
              policyId: 'test-policy-id2',
              policyName: 'Security-Low',
              policyViolationId: 'test-policy-violation-id2',
              threatLevel: 2,
              constraintViolations: [],
            },
          ],
        },
      ],
      policies: [
        {
          id: 'test-policy-id',
          name: 'Security-Medium',
          ownerId: 'test-owner-id',
          ownerType: 'APPLICATION',
          threatLevel: 5,
          policyType: 'security',
        },
      ],
    };

    getShallowComponent = enzymeUtils.getShallowComponent(FirewallQuarantineTable, minimalProps);
  });

  it('renders a table', () => {
    let component = getShallowComponent(),
      table = component.find(NxTable);

    expect(table).toExist();
  });

  describe('Renders table row', () => {
    it('renders a row within table', () => {
      // when the results table is rendered
      let component = getShallowComponent(),
        table = component.find(NxTable),
        tableBody = table.find(NxTableBody),
        quarantineDate = new Date(minimalProps.quarantineList[0].quarantineDate).toLocaleDateString();

      // then it contains the repository
      expect(
        tableBody.containsAllMatchingElements([
          <NxTableCell key="1" isNumeric>
            <NxThreatIndicator policyThreatLevel={5} />
            <span>{5}</span>
          </NxTableCell>,
          <NxTableCell key="2">
            <NxOverflowTooltip title="test-component">
              <div className="nx-truncate-ellipsis">test-component</div>
            </NxOverflowTooltip>
          </NxTableCell>,
          <NxTableCell key="3">{quarantineDate}</NxTableCell>,
          <NxTableCell key="4">
            <NxOverflowTooltip title="Security-Medium">
              <div className="nx-truncate-ellipsis">Security-Medium</div>
            </NxOverflowTooltip>
          </NxTableCell>,
          <NxTableCell key="5">
            <NxOverflowTooltip title="central">
              <div className="nx-truncate-ellipsis">central</div>
            </NxOverflowTooltip>
          </NxTableCell>,
        ])
      ).toBeTruthy();
    });
  });

  describe('Quarantine grid last updated', () => {
    it('calls the loadQuarantineList when the refresh button is clicked', () => {
      // when the results table is rendered
      let component = getShallowComponent(),
        initialLabel = component.find('.iq-firewall-table__time').text(),
        refreshButton = component.find('#firewall-quarantine-table--refresh-button');

      expect(initialLabel).toEqual('');

      refreshButton.simulate('click');

      expect(loadQuarantineList).toHaveBeenCalled();
    });
  });
});
