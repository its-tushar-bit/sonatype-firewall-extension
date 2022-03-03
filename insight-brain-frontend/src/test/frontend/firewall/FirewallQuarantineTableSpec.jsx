/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import {
  NxOverflowTooltip,
  NxTable,
  NxTableBody,
  NxTableCell,
  NxTableRow,
  NxThreatIndicator,
} from '@sonatype/react-shared-components';
import * as enzymeUtils from '../enzymeUtils';
import * as routerContext from 'MainRoot/react/RouterStateContext';

describe('FirewallQuarantineTable', function () {
  let minimalProps,
    FirewallQuarantineTable,
    getShallowComponent,
    loadQuarantineList,
    setQuarantineGridPage,
    setQuarantineGridSorting,
    setQuarantineGridPolicyFilter,
    selectQuarantineComponentSpy,
    hrefSpy,
    routerContextMock;

  selectQuarantineComponentSpy = jasmine.createSpy('selectQuarantineComponent');

  beforeEach(function () {
    FirewallQuarantineTable = require('inject-loader!../../../main/frontend/firewall/FirewallQuarantineTable')()
      .default;

    loadQuarantineList = jasmine.createSpy('loadQuarantineList');
    setQuarantineGridPage = jasmine.createSpy('setQuarantineGridPage');
    setQuarantineGridSorting = jasmine.createSpy('setQuarantineGridSorting');
    setQuarantineGridPolicyFilter = jasmine.createSpy('setQuarantineGridPolicyFilter');

    hrefSpy = jasmine.createSpy('href').and.callFake(() => 'someHref');
    routerContextMock = { href: hrefSpy };
    spyOn(routerContext, 'useRouterState').and.returnValue(routerContextMock);

    minimalProps = {
      selectQuarantineComponent: selectQuarantineComponentSpy,
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
          componentDisplayText: 'test-component',
          repository: 'central',
          quarantineDate: '2018-09-30T03:10:35.754+0000',
          dateCleared: null,
          quarantinePolicyViolations: [
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
          repositoryId: 'test-repository-id',
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
        tableBody.containsMatchingElement([
          <NxTableRow key="1" isClickable="true" onClick={selectQuarantineComponentSpy(1)}>
            <NxTableCell key="1" isNumeric>
              <NxThreatIndicator policyThreatLevel={5} />
              <span>{5}</span>
            </NxTableCell>
            ,
            <NxTableCell key="2">
              <NxOverflowTooltip title="test-component">
                <div className="nx-truncate-ellipsis">test-component</div>
              </NxOverflowTooltip>
            </NxTableCell>
            ,<NxTableCell key="3">{quarantineDate}</NxTableCell>,
            <NxTableCell key="4">
              <NxOverflowTooltip title="Security-Medium">
                <div className="nx-truncate-ellipsis">Security-Medium</div>
              </NxOverflowTooltip>
            </NxTableCell>
            ,
            <NxTableCell key="5">
              <NxOverflowTooltip title="central">
                <div className="nx-truncate-ellipsis">central</div>
              </NxOverflowTooltip>
            </NxTableCell>
            ,
          </NxTableRow>,
        ])
      ).toBeTruthy();
    });

    it('renders a row within table when no violations are present', () => {
      // when the results table is rendered
      let component = getShallowComponent({
          quarantineList: [
            {
              componentDisplayText: 'test-component',
              repository: 'central',
              quarantineDate: '2018-09-30T03:10:35.754+0000',
              dateCleared: null,
              quarantinePolicyViolations: [],
            },
          ],
        }),
        table = component.find(NxTable),
        tableBody = table.find(NxTableBody),
        quarantineDate = new Date(minimalProps.quarantineList[0].quarantineDate).toLocaleDateString();

      // then it contains the repository
      expect(
        tableBody.containsMatchingElement([
          <NxTableRow key="1" isClickable="true" onClick={selectQuarantineComponentSpy(1)}>
            <NxTableCell key="1" isNumeric>
              <NxThreatIndicator policyThreatLevel={0} />
              <span>{0}</span>
            </NxTableCell>
            ,
            <NxTableCell key="2">
              <NxOverflowTooltip title="test-component">
                <div className="nx-truncate-ellipsis">test-component</div>
              </NxOverflowTooltip>
            </NxTableCell>
            ,<NxTableCell key="3">{quarantineDate}</NxTableCell>,
            <NxTableCell key="4">
              <NxOverflowTooltip title="Security-Medium">
                <div className="nx-truncate-ellipsis">None</div>
              </NxOverflowTooltip>
            </NxTableCell>
            ,
            <NxTableCell key="5">
              <NxOverflowTooltip title="central">
                <div className="nx-truncate-ellipsis">central</div>
              </NxOverflowTooltip>
            </NxTableCell>
            ,
          </NxTableRow>,
        ])
      ).toBeTruthy();
    });

    it('dispatches the selectQuarantineComponent action when a row is clicked', () => {
      // when the results table is rendered, find the first row
      let component = getShallowComponent(),
        table = component.find(NxTable),
        tableBody = table.find(NxTableBody),
        row = tableBody.find(NxTableRow);

      // then clicking on the row dispatches selectQuarantineComponentSpy with the correct index
      row.simulate('click');
      expect(selectQuarantineComponentSpy).toHaveBeenCalledWith(1);
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

  describe('row is not clickable', () => {
    it('no action is performed when a row is clicked', () => {
      let component = getShallowComponent(),
        row = component.find(NxTable).find(NxTableBody).find(NxTableRow);

      expect(row).not.toHaveProp('onClick');
      expect(row).not.toHaveProp('isClickable');
    });
  });

  describe('renders CIP', () => {
    it('calls the selectQuarantineComponent when Component cell is clicked', () => {
      let component = getShallowComponent(),
        link = component.find('#iq-firewall-quarantine-table--cip');

      link.simulate('click');
      expect(selectQuarantineComponentSpy).toHaveBeenCalled();
    });
  });

  describe('renders repo results view page link', () => {
    it('renders a NxTextLink for repo results view page', () => {
      let component = getShallowComponent(),
        link = component.find('#iq-firewall-quarantine-table--repo-view-link');

      expect(routerContext.useRouterState).toHaveBeenCalled();
      expect(hrefSpy).toHaveBeenCalledWith('repository-report', {
        repositoryId: minimalProps.quarantineList[0].repositoryId,
      });
      expect(link).toHaveProp('href', 'someHref');
      expect(link).toHaveProp('newTab', true);
      expect(link).toHaveProp('external', true);
      expect(link).toHaveText(minimalProps.quarantineList[0].repository);
    });
  });
});
