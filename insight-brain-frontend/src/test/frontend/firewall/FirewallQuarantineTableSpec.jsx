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
  NxStatefulFilterDropdown,
} from '@sonatype/react-shared-components';
import * as enzymeUtils from '../enzymeUtils';
import * as routerContext from 'MainRoot/react/RouterStateContext';
import { formatDate, STANDARD_DATE_FORMAT, FIREWALL_TIME_DATE_FORMAT } from 'MainRoot/util/dateUtils';

const DATE_TIME = '2018-09-30T03:10:35.754+0000';

describe('FirewallQuarantineTable', function () {
  let minimalProps,
    FirewallQuarantineTable,
    getShallowComponent,
    getMountedComponent,
    loadQuarantineList,
    setQuarantineGridPage,
    setQuarantineGridSorting,
    setQuarantineGridPolicyFilter,
    setQuarantineGridComponentNameFilter,
    setQuarantineGridRepositoryPublicIdFilter,
    goToRepositoryComponentDetailsPageSpy,
    hrefSpy,
    routerContextMock;

  goToRepositoryComponentDetailsPageSpy = jasmine.createSpy('goToRepositoryComponentDetailsPage');

  beforeEach(function () {
    FirewallQuarantineTable = require('inject-loader!../../../main/frontend/firewall/FirewallQuarantineTable')()
      .default;

    loadQuarantineList = jasmine.createSpy('loadQuarantineList');
    setQuarantineGridPage = jasmine.createSpy('setQuarantineGridPage');
    setQuarantineGridSorting = jasmine.createSpy('setQuarantineGridSorting');
    setQuarantineGridPolicyFilter = jasmine.createSpy('setQuarantineGridPolicyFilter');
    setQuarantineGridComponentNameFilter = jasmine.createSpy('setQuarantineGridComponentNameFilter');
    setQuarantineGridRepositoryPublicIdFilter = jasmine.createSpy('setQuarantineGridRepositoryPublicIdFilter');

    hrefSpy = jasmine.createSpy('href').and.callFake(() => 'someHref');
    routerContextMock = { href: hrefSpy };
    spyOn(routerContext, 'useRouterState').and.returnValue(routerContextMock);

    minimalProps = {
      goToRepositoryComponentDetailsPage: goToRepositoryComponentDetailsPageSpy,
      loadQuarantineList: loadQuarantineList,
      setQuarantineGridPage: setQuarantineGridPage,
      setQuarantineGridSorting: setQuarantineGridSorting,
      setQuarantineGridPolicyFilter: setQuarantineGridPolicyFilter,
      setQuarantineGridComponentNameFilter: setQuarantineGridComponentNameFilter,
      setQuarantineGridRepositoryPublicIdFilter: setQuarantineGridRepositoryPublicIdFilter,
      loadedQuarantineList: true,
      loadedPolicies: true,
      quarantinePageCount: 1,
      pageSize: 2,
      currentPage: 0,
      filterPolicies: [],
      filterComponentName: 'initialComponentName',
      filterRepositoryPublicId: 'initialRepositoryPublicId',
      lastUpdated: DATE_TIME,
      quarantineList: [
        {
          threatLevel: 5,
          policyName: 'Security-Medium',
          quarantined: true,
          quarantineDate: DATE_TIME,
          componentIdentifier: {
            format: 'maven',
            coordinates: {
              artifactId: 'test-component',
              classifier: '',
              extension: 'jar',
              groupId: 'test-component',
              version: '1.0.0',
            },
          },
          pathname: 'pathname1',
          componentDisplayText: 'test-component',
          repositoryId: 'test-repository-id',
          repositoryName: 'central',
          hash: 'hash1',
          matchState: 'exact',
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
    getMountedComponent = enzymeUtils.getMountedComponent(FirewallQuarantineTable, minimalProps);
  });

  it('renders a table', () => {
    let component = getShallowComponent(),
      table = component.find(NxTable);

    expect(table).toExist();
  });

  it('renders table header', () => {
    let component = getShallowComponent(),
      header = component.find('.iq-firewall-table-label');

    expect(header).toHaveText(`Components Actively in Quarantine`);
  });

  describe('Renders table row', () => {
    it('renders a row within table', () => {
      // when the results table is rendered
      let component = getShallowComponent(),
        table = component.find(NxTable),
        tableBody = table.find(NxTableBody),
        unFormattedQuarantineDate = minimalProps.quarantineList[0].quarantineDate,
        quarantineDate = formatDate(unFormattedQuarantineDate, STANDARD_DATE_FORMAT);

      // then it contains the repository
      expect(
        tableBody.containsMatchingElement([
          <NxTableRow key="1">
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
              repositoryName: 'central',
              quarantineDate: DATE_TIME,
              threatLevel: 0,
              policyName: null,
            },
          ],
        }),
        table = component.find(NxTable),
        tableBody = table.find(NxTableBody),
        unFormattedQuarantineDate = minimalProps.quarantineList[0].quarantineDate,
        quarantineDate = formatDate(unFormattedQuarantineDate, STANDARD_DATE_FORMAT);

      // then it contains the repository
      expect(
        tableBody.containsMatchingElement([
          <NxTableRow key="1">
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
  });

  describe('Quarantine grid last updated', () => {
    it('has the correct time and date format', () => {
      // when the results table is rendered
      let component = getShallowComponent(),
        initialLabel = component.find('.iq-firewall-table__time').text();

      expect(initialLabel).toEqual('Updated ' + formatDate(minimalProps.lastUpdated, FIREWALL_TIME_DATE_FORMAT));
    });

    it('calls the loadQuarantineList when the refresh button is clicked', () => {
      let component = getShallowComponent(),
        refreshButton = component.find('#firewall-quarantine-table--refresh-button');

      refreshButton.simulate('click');
      expect(loadQuarantineList).toHaveBeenCalled();
    });
  });

  describe('Quarantine grid filter', () => {
    it('NxStatefulFilterDropdown is rendered with the correct props', () => {
      const component = getMountedComponent(),
        filterDropdown = component.find(NxStatefulFilterDropdown);
      expect(filterDropdown).toExist();
      expect(filterDropdown).toHaveProp('options', [{ id: 'test-policy-id', displayName: 'Security-Medium' }]);
    });

    it('calls the setQuarantineGridComponentNameFilter when entering a component name', () => {
      const component = getShallowComponent(),
        input = component.find('#firewall-quarantine-table--component-name');
      expect(input).not.toBeNull();
      expect(input).toHaveValue('initialComponentName');
      input.simulate('change', 'name');
      expect(setQuarantineGridComponentNameFilter).toHaveBeenCalledWith('name');
    });

    it('calls the setQuarantineGridRepositoryPublicIdFilter when entering a repository public id', () => {
      const component = getShallowComponent(),
        input = component.find('#firewall-quarantine-table--repository-public-id');
      expect(input).not.toBeNull();
      expect(input).toHaveValue('initialRepositoryPublicId');
      input.simulate('change', 'name');
      expect(setQuarantineGridRepositoryPublicIdFilter).toHaveBeenCalledWith('name');
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

  describe('renders CDP', () => {
    it('calls the goToRepositoryComponentDetailsPage when Component cell is clicked', () => {
      let component = getShallowComponent(),
        link = component.find('#iq-firewall-quarantine-table--component-details-page');

      link.simulate('click');
      expect(goToRepositoryComponentDetailsPageSpy).toHaveBeenCalled();
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
      expect(link).toHaveProp('truncate', true);
      expect(link).toHaveText(minimalProps.quarantineList[0].repositoryName);
    });
  });
});
