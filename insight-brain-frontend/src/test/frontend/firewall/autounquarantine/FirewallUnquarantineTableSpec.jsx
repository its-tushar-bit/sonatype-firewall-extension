/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import { NxTable, NxTableBody, NxTableCell, NxOverflowTooltip, NxTableRow } from '@sonatype/react-shared-components';
import * as enzymeUtils from '../../enzymeUtils';

describe('FirewallUnquarantineTable', function () {
  let minimalProps, FirewallUnquarantineTable, getShallowComponent;

  beforeEach(function () {
    FirewallUnquarantineTable = require('inject-loader!../../../../main/frontend/firewall/autounquarantine/FirewallUnquarantineTable')()
      .default;

    minimalProps = {
      loadedReleaseQuarantineList: true,
      loadedPolicies: true,
      releaseQuarantinePageCount: 1,
      pageSize: 2,
      currentPage: 0,
      releaseQuarantineList: [
        {
          displayName: 'test-component',
          repository: 'central',
          quarantineDate: '2018-09-30T03:10:35.754+0000',
          dateCleared: '2018-10-16T18:45:59.967+0000',
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
        {
          displayName: 'test-component2',
          repository: 'central',
          quarantineDate: '2018-09-30T03:10:35.754+0000',
          dateCleared: '2018-10-16T18:45:59.967+0000',
          policyViolations: [],
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

    getShallowComponent = enzymeUtils.getShallowComponent(FirewallUnquarantineTable, minimalProps);
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
        quarantineDate = new Date(minimalProps.releaseQuarantineList[0].quarantineDate).toLocaleDateString(),
        dateCleared = new Date(minimalProps.releaseQuarantineList[0].dateCleared).toLocaleDateString();

      // contains a row of normal data
      expect(
        tableBody.containsMatchingElement([
          <NxTableRow key="1">
            <NxTableCell key="1" className="iq-firewall-grid-component">
              <NxOverflowTooltip title="test-component">
                <div className="nx-truncate-ellipsis">test-component</div>
              </NxOverflowTooltip>
            </NxTableCell>
            <NxTableCell key="2">{quarantineDate}</NxTableCell>,
            <NxTableCell key="3">
              <NxOverflowTooltip title="Security-Medium">
                <div className="nx-truncate-ellipsis">Security-Medium</div>
              </NxOverflowTooltip>
            </NxTableCell>
            <NxTableCell key="4">
              <NxOverflowTooltip title="central">
                <div className="nx-truncate-ellipsis">central</div>
              </NxOverflowTooltip>
            </NxTableCell>
            <NxTableCell key="5">{dateCleared}</NxTableCell>
          </NxTableRow>,
        ])
      ).toBeTruthy();

      // contains a row of data with no policy violations
      expect(
        tableBody.containsMatchingElement([
          <NxTableRow key="1">
            <NxTableCell key="1" className="iq-firewall-grid-component">
              <NxOverflowTooltip title="test-component">
                <div className="nx-truncate-ellipsis">test-component2</div>
              </NxOverflowTooltip>
            </NxTableCell>
            <NxTableCell key="2">{quarantineDate}</NxTableCell>,
            <NxTableCell key="3">
              <NxOverflowTooltip title="">
                <div className="nx-truncate-ellipsis"></div>
              </NxOverflowTooltip>
            </NxTableCell>
            <NxTableCell key="4">
              <NxOverflowTooltip title="central">
                <div className="nx-truncate-ellipsis">central</div>
              </NxOverflowTooltip>
            </NxTableCell>
            <NxTableCell key="5">{dateCleared}</NxTableCell>
          </NxTableRow>,
        ])
      ).toBeTruthy();
    });
  });
});
