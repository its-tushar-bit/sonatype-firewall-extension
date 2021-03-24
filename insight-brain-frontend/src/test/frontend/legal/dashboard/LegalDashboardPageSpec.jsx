/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as enzymeUtils from '../../enzymeUtils';
import React from 'react';
import LegalDashboardApplicationsTab from '../../../../main/frontend/legal/dashboard/LegalDashboardApplicationsTab';
import LoadWrapper from '../../../../main/frontend/react/LoadWrapper';

describe('LegalDashboardPage', function() {
  let minimalProps,
      LegalDashboardPage,
      LegalDashboardFilterContainerMock,
      loadResultsSpy,
      getShallowComponent;
  const mockApplications = {
    results: []
  };

  beforeEach(function() {
    LegalDashboardFilterContainerMock = jasmine.createSpy('MaximizedContainerMock')
        .and.returnValue(<div>LegalDashboardFilterContainer</div>);

    LegalDashboardPage =
        require('inject-loader!../../../../main/frontend/legal/dashboard/LegalDashboardPage')({
          './filter/LegalDashboardFilterContainer': LegalDashboardFilterContainerMock
        }).default;

    loadResultsSpy = jasmine.createSpy('loadResults');
    minimalProps = {
      applications: mockApplications,
      components: [],
      loadResults: loadResultsSpy,
      isAuthorized: true,
      loading: 'loading',
      loadError: 'loadError',
      filtersAreDirty: 'filtersAreDirty'
    };

    getShallowComponent = enzymeUtils.getShallowComponent(LegalDashboardPage, minimalProps);
  });

  it('is wrapped by a LoadWrapper with appropriate parameters', function() {
    let loadWrapper = getShallowComponent().find(LoadWrapper);
    expect(loadWrapper).toExist();
    expect(loadWrapper).toHaveProp('loading', 'loading');
    expect(loadWrapper).toHaveProp('error', 'loadError');
    expect(loadWrapper).toHaveProp('retryHandler', loadResultsSpy);
  });

  it('renders an aside and a main', function() {
    let sidebar = getShallowComponent().find('aside.nx-page-sidebar');
    expect(sidebar).toExist();
    expect(sidebar.find(LegalDashboardFilterContainerMock)).toExist();
    expect(getShallowComponent().find('main.nx-page-main')).toExist();
  });

  it('renders an LegalDashboardApplicationsTab', function() {
    const wrapper = getShallowComponent();
    let applicationsTab = wrapper.find(LegalDashboardApplicationsTab);
    expect(applicationsTab).toExist();
    expect(applicationsTab).toHaveProp('applications', mockApplications);
    expect(applicationsTab).toHaveProp('filtersAreDirty', 'filtersAreDirty');
  });

});
