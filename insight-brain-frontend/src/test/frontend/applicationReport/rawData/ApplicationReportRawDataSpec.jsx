/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as enzymeUtils from '../../enzymeUtils';
import MenuBarBackButton from 'MainRoot/mainHeader/MenuBar/MenuBarBackButton';
import { NxInfoAlert } from '@sonatype/react-shared-components';

import ApplicationReportRawData from 'MainRoot/applicationReport/rawData/ApplicationReportRawData';
import ApplicationReportRawDataTable from 'MainRoot/applicationReport/rawData/ApplicationReportRawDataTable';
import ApplicationReportRawDataHeader from 'MainRoot/applicationReport/rawData/ApplicationReportRawDataHeader';

describe('ApplicationReportRawData', () => {
  let minimalProps, getShallowComponent;
  const loadReportRawDataSpy = jasmine.createSpy('loadReportRawData');

  beforeEach(() => {
    minimalProps = {
      loadReportRawData: loadReportRawDataSpy,
      loading: false,
      metadata: {
        reportTitle: 'Build Report',
        reportTime: 0,
        application: { name: 'random name' },
      },
      displayedEntries: [],
    };

    getShallowComponent = enzymeUtils.getShallowComponent(ApplicationReportRawData, minimalProps);
  });

  it('renders a MenuBarBackButton with correct stateName prop', function () {
    const component = getShallowComponent();
    const menuBarBackButton = component.find(MenuBarBackButton);

    expect(menuBarBackButton).toExist();
    expect(menuBarBackButton).toHaveProp('stateName', 'applicationReport.policy');
  });

  describe('header', () => {
    it('shows header if metadata exists', () => {
      const header = getShallowComponent().find(ApplicationReportRawDataHeader);
      expect(header).toExist();
    });

    it("doesn't show header if no metadata exist", () => {
      const header = getShallowComponent({ metadata: null }).find(ApplicationReportRawDataHeader);
      expect(header).not.toExist();
    });
  });

  it('renders alert', () => {
    const alert = getShallowComponent().find(NxInfoAlert);

    expect(alert).toExist();
    expect(alert.text()).toBe(
      'Please note that the data appearing on this page is the raw data and not the result of policy evaluation'
    );
  });

  it('renders table', () => {
    const table = getShallowComponent().find(ApplicationReportRawDataTable);
    expect(table).toExist();
  });
});
