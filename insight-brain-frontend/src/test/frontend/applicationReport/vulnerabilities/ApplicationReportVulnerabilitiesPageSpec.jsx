/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as enzymeUtils from '../../enzymeUtils';
import ApplicationReportVulnerabilitiesHeader from 'MainRoot/applicationReport/vulnerabilities/ApplicationReportVulnerabilitiesHeader';
import ApplicationReportVulnerabilitiesTable from 'MainRoot/applicationReport/vulnerabilities/ApplicationReportVulnerabilitiesTable';
import ApplicationReportVulnerabilitiesPage from 'MainRoot/applicationReport/vulnerabilities/ApplicationReportVulnerabilitiesPage';
import * as routerContext from '../../../../main/frontend/react/RouterStateContext';

import LoadWrapper from 'MainRoot/react/LoadWrapper';
import MenuBarBackButton from 'MainRoot/mainHeader/MenuBar/MenuBarBackButton';

describe('ApplicationReportVulnerabilitiesPage', function () {
  let loadReportAllDataSpy, getShallowComponent, getMountedComponent, stateGetSpy, stateHrefSpy, mockState;

  const minimalMetadata = {
    reportTitle: 'fooReport',
    reportTime: 0,
    application: { name: 'foo app' },
  };

  beforeEach(function () {
    loadReportAllDataSpy = jasmine.createSpy('loadReportAllData');

    const minimalProps = {
      loading: false,
      loadReportAllData: loadReportAllDataSpy,
      vulnerabilities: [],
      vulnerabilitiesPageEnabled: true,
    };

    stateGetSpy = jasmine.createSpy('$state.get').and.returnValue({ data: { title: 'some page' } });
    stateHrefSpy = jasmine.createSpy('$state.href').and.returnValue('/noop');

    mockState = {
      get: stateGetSpy,
      href: stateHrefSpy,
    };
    spyOn(routerContext, 'useRouterState').and.returnValue(mockState);

    getShallowComponent = enzymeUtils.getShallowComponent(ApplicationReportVulnerabilitiesPage, minimalProps);
    getMountedComponent = enzymeUtils.getMountedComponent(ApplicationReportVulnerabilitiesPage, minimalProps);
  });

  it('renders a MenuBarBackButton with the correct stateName, ', function () {
    const menuBarBackButton = getShallowComponent().find(MenuBarBackButton);

    expect(menuBarBackButton).toExist();
    expect(menuBarBackButton).toHaveProp('stateName', 'applicationReport.policy');
  });

  it('renders a tile with the header and table, within a LoadWrapper', function () {
    const component = getShallowComponent({ metadata: minimalMetadata });
    const loadWrapperChildren = enzymeUtils.getLoadWrapperChildren(component);
    const tile = loadWrapperChildren.find('.nx-tile');

    expect(component).toExist();
    expect(component).toContainMatchingElement(LoadWrapper);
    expect(component.find(LoadWrapper).prop('children')).toEqual(jasmine.any(Function));
    expect(tile.childAt(0)).toContainMatchingElement(ApplicationReportVulnerabilitiesHeader);
    expect(tile.childAt(1)).toMatchSelector(ApplicationReportVulnerabilitiesTable);
  });

  it("sets the LoadWrapper's loading prop if the metadata prop is falsey or the loading prop is set", function () {
    const metadata = {
      reportTitle: 'fooReport',
      reportTime: 0,
      application: { name: 'foo app' },
    };

    expect(getShallowComponent().find(LoadWrapper)).toHaveProp('loading', true);
    expect(getShallowComponent({ metadata: null }).find(LoadWrapper)).toHaveProp('loading', true);
    expect(getShallowComponent({ metadata: undefined }).find(LoadWrapper)).toHaveProp('loading', true);
    expect(getShallowComponent({ metadata, loading: true }).find(LoadWrapper)).toHaveProp('loading', true);
    expect(getShallowComponent({ metadata }).find(LoadWrapper)).toHaveProp('loading', false);
  });

  it("sets the LoadWrapper's error prop to the loadError", function () {
    expect(getShallowComponent().find(LoadWrapper)).toHaveProp('error', undefined);
    expect(getShallowComponent({ loadError: null }).find(LoadWrapper)).toHaveProp('error', undefined);
    expect(getShallowComponent({ loadError: undefined }).find(LoadWrapper)).toHaveProp('error', undefined);
    expect(getShallowComponent({ loadError: 'error!' }).find(LoadWrapper)).toHaveProp('error', 'error!');
  });

  it("sets the LoadWrapper's error if vulnerabilitiesPageEnabled is not true", function () {
    expect(getShallowComponent({ vulnerabilitiesPageEnabled: false }).find(LoadWrapper)).toHaveProp(
      'error',
      jasmine.any(String)
    );
  });

  it("passes loadReportAllData as the LoadWrapper's retryHandler", function () {
    expect(getShallowComponent().find(LoadWrapper)).toHaveProp('retryHandler', loadReportAllDataSpy);
  });

  it('passes the metadata prop on to the header', function () {
    const component = getShallowComponent({ metadata: minimalMetadata });
    const loadWrapperChildren = enzymeUtils.getLoadWrapperChildren(component);
    expect(loadWrapperChildren.find(ApplicationReportVulnerabilitiesHeader)).toExist();
    expect(loadWrapperChildren.find(ApplicationReportVulnerabilitiesHeader)).toHaveProp('metadata', minimalMetadata);
  });

  it('passes the vulnerabilities to the table', function () {
    const vulnerabilities = [
      {
        displayName: {
          parts: [
            {
              value: 'Foo',
            },
          ],
        },
        securityCode: 'CVE-12345',
        cvssScore: 8.0,
      },
    ];
    const getTable = (additionalProps) =>
      enzymeUtils
        .getLoadWrapperChildren(getShallowComponent(additionalProps))
        .find(ApplicationReportVulnerabilitiesTable);

    expect(getTable({ metadata: minimalMetadata })).toHaveProp('vulnerabilities', []);
    expect(getTable({ metadata: minimalMetadata, vulnerabilities })).toHaveProp('vulnerabilities', vulnerabilities);
  });

  it('calls loadReportAllData on mount', function () {
    const component = getMountedComponent();

    expect(loadReportAllDataSpy).toHaveBeenCalled();
    component.unmount();
  });
});
