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
import { pathSet } from 'MainRoot/util/jsUtil';
import { NxModal } from '@sonatype/react-shared-components';

describe('LegalDashboardPage', function () {
  let minimalProps,
    LegalDashboardPage,
    LegalDashboardFilterContainerMock,
    loadResultsSpy,
    loadDashboardUISpy,
    loadFilterSpy,
    getShallowComponent,
    getMountedComponent,
    stateGoSpy;
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
    loadDashboardUISpy = jasmine.createSpy('loadDashboardUISpy');
    stateGoSpy = jasmine.createSpy('stateGoSpy');
    minimalProps = {
      applications: mockApplications,
      components: mockComponents,
      loadResults: loadResultsSpy,
      loadFilter: loadFilterSpy,
      loadDashboardUI: loadDashboardUISpy,
      isAuthorized: true,
      loading: true,
      loadError: 'loadError',
      filtersAreDirty: true,
      router: {
        currentState: {
          data: {
            activeTab: 'applications',
            isMultiApp: false,
            disableCreateAttributionReportBtn: false,
          },
        },
      },
      changeComponentNameToSearch: () => {},
      legalDashboardSetPage: () => {},
      toggleFilterSidebar: () => {},
      fetchBackendPage: () => {},
      changeSortField: () => {},
      stateGo: stateGoSpy,
      searchByComponentName: () => {},
      setComponentSearchInputValue: () => {},
    };

    getShallowComponent = enzymeUtils.getShallowComponent(LegalDashboardPage, minimalProps);
    getMountedComponent = enzymeUtils.getMountedComponent(LegalDashboardPage, minimalProps);
  });

  it('calls loadDashboardUI', function () {
    expect(loadDashboardUISpy).not.toHaveBeenCalled();
    getMountedComponent();
    expect(loadDashboardUISpy).toHaveBeenCalled();
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

    let createAttribReportButton = wrapper.find('#create-attribution-report-btn');
    expect(createAttribReportButton).toHaveSize(1);
    expect(createAttribReportButton).toHaveProp('title', '');
    expect(applicationsTab).toHaveProp('filtersAreDirty', true);
  });

  it('renders an LegalDashboardComponentsTab', function () {
    const wrapper = getShallowComponent();
    let componentsTab = wrapper.find(LegalDashboardComponentsTab);
    expect(componentsTab).toExist();
    expect(componentsTab).toHaveProp('components', mockComponents);
  });

  it('renders a title on the Create Attribution Report when not in the Applications tab', function () {
    const propsComponentsTab = pathSet(['router', 'currentState', 'data', 'activeTab'], 'components', minimalProps);
    const wrapper = enzymeUtils.getShallowComponent(LegalDashboardPage, propsComponentsTab)();

    const createAttribReportButton = wrapper.find('#create-attribution-report-btn');
    expect(createAttribReportButton).toHaveProp(
      'title',
      'Only available for applications. Switch to Applications to use.'
    );
  });

  it('omits modal and navigation to the Attribution Report page when the button is disabled', function () {
    const propsDisableAttributionReportBtn = pathSet(
      ['router', 'currentState', 'data', 'disableCreateAttributionReportBtn'],
      true,
      minimalProps
    );

    const wrapper = enzymeUtils.getShallowComponent(LegalDashboardPage, propsDisableAttributionReportBtn)();
    const createAttribReportButton = wrapper.find('#create-attribution-report-btn');

    expect(createAttribReportButton).toHaveClassName('disabled');

    createAttribReportButton.simulate('click');
    expect(wrapper.find(NxModal)).not.toExist();
    expect(stateGoSpy).not.toHaveBeenCalled();
  });

  it('omits modal and navigation to the Attribution Report page when there is no application', function () {
    const propsNoApplication = pathSet(['applications', 'totalResultsCount'], 0, minimalProps);
    const wrapper = enzymeUtils.getShallowComponent(LegalDashboardPage, propsNoApplication)();

    const createAttribReportButton = wrapper.find('#create-attribution-report-btn');
    expect(createAttribReportButton).toHaveClassName('disabled');

    createAttribReportButton.simulate('click');
    expect(wrapper.find(NxModal)).not.toExist();
    expect(stateGoSpy).not.toHaveBeenCalled();
  });

  it('navigates to the Attribution Report page when there is only 1 application', function () {
    const propsOneApplication = pathSet(['applications', 'totalResultsCount'], 1, minimalProps);
    const wrapper = enzymeUtils.getShallowComponent(LegalDashboardPage, propsOneApplication)();

    const createAttribReportButton = wrapper.find('#create-attribution-report-btn');
    expect(createAttribReportButton).not.toHaveClassName('disabled');

    createAttribReportButton.simulate('click');
    expect(wrapper.find(NxModal)).not.toExist();
    expect(stateGoSpy).toHaveBeenCalledWith('legal.attributionReportMultiApp');
  });

  it('prompts users with dialog for generating report with more than 1 application', function () {
    const mockApplications = {
      results: [
        {
          applicationId: 'test1',
          applicationPublicId: 'test1',
          applicationName: 'test1',
          lastScanTime: 123,
          stageTypeId: 'test',
          applicationTagNames: ['tag'],
          componentsReviewedCount: 1,
          componentsTotalCount: 2,
        },
        {
          applicationId: 'test2',
          applicationPublicId: 'test2',
          applicationName: 'test2',
          lastScanTime: 123,
          stageTypeId: 'test',
          applicationTagNames: ['tag'],
          componentsReviewedCount: 1,
          componentsTotalCount: 2,
        },
      ],
      totalResultsCount: 2,
      backendPage: 1,
    };

    const minimalPropsDialogTest = {
      ...minimalProps,
      applications: mockApplications,
    };

    getShallowComponent = enzymeUtils.getShallowComponent(LegalDashboardPage, minimalPropsDialogTest);

    const wrapper = getShallowComponent();
    const applicationsTab = wrapper.find(LegalDashboardApplicationsTab);
    expect(applicationsTab).toExist();

    const createAttribReportButton = wrapper.find('#create-attribution-report-btn');
    expect(createAttribReportButton).toExist();
    expect(createAttribReportButton).not.toHaveClassName('disabled');
    createAttribReportButton.simulate('click');
    expect(wrapper.find(NxModal)).toExist();

    const generateAttribReportButton = wrapper.find('#create-report-generate-report-button');
    expect(generateAttribReportButton).toExist();
    generateAttribReportButton.simulate('click');

    createAttribReportButton.simulate('click');
    const cancelAttribReportButton = wrapper.find('#create-report-cancel-button');
    expect(cancelAttribReportButton).toExist();
    cancelAttribReportButton.simulate('click');

    expect(wrapper.find(NxModal)).not.toExist();
  });
});
