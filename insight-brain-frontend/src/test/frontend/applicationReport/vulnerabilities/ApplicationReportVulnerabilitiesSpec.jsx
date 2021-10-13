/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as enzymeUtils from '../../enzymeUtils';

import ApplicationReportVulnerabilitiesPage from 'MainRoot/applicationReport/vulnerabilities/ApplicationReportVulnerabilitiesPage';
import ApplicationReportVulnerabilitiesTable from 'MainRoot/applicationReport/vulnerabilities/ApplicationReportVulnerabilitiesTable';
import ApplicationReportVulnerabilitiesHeader from 'MainRoot/applicationReport/vulnerabilities/ApplicationReportVulnerabilitiesHeader';
import MenuBarBackButton from 'MainRoot/mainHeader/MenuBar/MenuBarBackButton';
import LoadWrapper from 'MainRoot/react/LoadWrapper';

describe('ApplicationReportVulnerabilities', function () {
  let getShallowComponent, minimalProps;
  const loadReportAllDataSpy = jasmine.createSpy('loadReportAllData');

  beforeEach(function () {
    minimalProps = {
      loadError: 'Error',
      loading: false,
      vulnerabilitiesPageEnabled: true,
      metadata: {
        reportTitle: 'Test Title',
        reportTime: '2021-01-01',
        application: {
          name: 'Sample Application Name',
        },
      },
      vulnerabilities: ['foo-1234', 'bar-qwerty'],
      loadReportAllData: loadReportAllDataSpy,
    };

    getShallowComponent = enzymeUtils.getShallowComponent(ApplicationReportVulnerabilitiesPage, minimalProps);
  });

  it('renders a MenuBarBackButton with correct stateName prop', function () {
    const component = getShallowComponent();
    const menuBarBackButton = component.find(MenuBarBackButton);
    expect(menuBarBackButton).toExist();
    expect(menuBarBackButton).toHaveProp('stateName', 'applicationReport.policy');
  });

  describe('metadata', () => {
    it('shows header and table if metadata exists', () => {
      const component = getShallowComponent();
      const loadWrapperChildren = enzymeUtils.getLoadWrapperChildren(component);
      const tile = loadWrapperChildren.find('.nx-tile');
      expect(tile.childAt(0)).toMatchSelector(ApplicationReportVulnerabilitiesHeader);
      expect(tile.childAt(1)).toMatchSelector(ApplicationReportVulnerabilitiesTable);
    });

    it('does not show header if no metadata exist', () => {
      const component = getShallowComponent({ metadata: null });
      const header = component.find(ApplicationReportVulnerabilitiesHeader);
      const table = component.find(ApplicationReportVulnerabilitiesTable);
      expect(header).not.toExist();
      expect(table).not.toExist();
    });
  });

  describe('LoadWrapper', () => {
    it('shows LoadWrapper when loading', () => {
      const component = getShallowComponent({ loading: true });
      const loadWrapper = component.find(LoadWrapper);
      expect(loadWrapper).toExist();
    });

    it('does not show header or table loading', () => {
      const component = getShallowComponent({ loading: true });
      const header = component.find(ApplicationReportVulnerabilitiesHeader);
      const table = component.find(ApplicationReportVulnerabilitiesTable);
      expect(header).not.toExist();
      expect(table).not.toExist();
    });
  });
});
