/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as enzymeUtils from '../../enzymeUtils';
import React from 'react';
import LegalDashboardApplicationsTab from '../../../../main/frontend/legal/dashboard/LegalDashboardApplicationsTab';
import LegalDashboardComponentsTab from '../../../../main/frontend/legal/dashboard/LegalDashboardComponentsTab';
import LoadWrapper from '../../../../main/frontend/react/LoadWrapper';

describe('LegalDashboardPage', function () {
  let minimalProps,
    LegalDashboardPage,
    LegalDashboardFilterContainerMock,
    loadResultsSpy,
    loadFilterSpy,
    getShallowComponent,
    getMountedComponent;
  const mockApplications = {
    results: [],
    totalResultsCount: 0,
    backendPage: 1,
  };
  const mockComponents = {
    results: [],
  };

  beforeEach(function () {
    LegalDashboardFilterContainerMock = jasmine
      .createSpy('MaximizedContainerMock')
      .and.returnValue(<div>LegalDashboardFilterContainer</div>);

    LegalDashboardPage = require('inject-loader!../../../../main/frontend/legal/dashboard/LegalDashboardPage')({
      './filter/LegalDashboardFilterContainer': LegalDashboardFilterContainerMock,
    }).default;

    loadResultsSpy = jasmine.createSpy('loadResults');
    loadFilterSpy = jasmine.createSpy('loadFilter');
    minimalProps = {
      applications: mockApplications,
      components: mockComponents,
      loadResults: loadResultsSpy,
      loadFilter: loadFilterSpy,
      isAuthorized: true,
      loading: true,
      loadError: 'loadError',
      filtersAreDirty: true,
      router: {
        currentParams: {
          legalComponentsTabEnabled: 'true',
        },
        currentState: {
          data: {
            activeTab: 'applications',
          },
        },
      },
      changeComponentNameToSearch: () => {},
      legalDashboardSetPage: () => {},
      toggleFilterSidebar: () => {},
      fetchBackendPage: () => {},
      changeSortField: () => {},
      stateGo: () => {},
    };

    getShallowComponent = enzymeUtils.getShallowComponent(LegalDashboardPage, minimalProps);
    getMountedComponent = enzymeUtils.getMountedComponent(LegalDashboardPage, minimalProps);
  });

  it('calls loadFilter', function () {
    expect(loadFilterSpy).not.toHaveBeenCalled();
    getMountedComponent();
    expect(loadFilterSpy).toHaveBeenCalled();
  });

  it('calls loadResults if filterLoading is false', function () {
    expect(loadResultsSpy).not.toHaveBeenCalled();
    getMountedComponent();
    expect(loadResultsSpy).toHaveBeenCalledWith('applications');
  });

  it('does not call loadResults if filterLoading is true', function () {
    getMountedComponent({ filterLoading: true });
    expect(loadResultsSpy).not.toHaveBeenCalled();
  });

  it('is wrapped by a LoadWrapper with appropriate parameters', function () {
    let loadWrapper = getShallowComponent().find(LoadWrapper);
    expect(loadWrapper).toExist();
    expect(loadWrapper).toHaveProp('loading', true);
    expect(loadWrapper).toHaveProp('error', 'loadError');
    expect(loadWrapper).toHaveProp('retryHandler', loadResultsSpy);
  });

  it('renders a main', function () {
    expect(getShallowComponent().find('main.nx-page-main')).toExist();
  });

  it('renders an LegalDashboardApplicationsTab', function () {
    const wrapper = getShallowComponent();
    let applicationsTab = wrapper.find(LegalDashboardApplicationsTab);
    expect(applicationsTab).toExist();
    expect(applicationsTab).toHaveProp('applications', mockApplications);
    expect(applicationsTab).toHaveProp('filtersAreDirty', true);
  });

  it('renders an LegalDashboardComponentsTab', function () {
    const wrapper = getShallowComponent();
    let componentsTab = wrapper.find(LegalDashboardComponentsTab);
    expect(componentsTab).toExist();
    expect(componentsTab).toHaveProp('components', mockComponents);
  });
});
