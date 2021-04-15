/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as enzymeUtils from '../../enzymeUtils';
import ApplicationReportVulnerabilitiesHeader from '../../../../main/frontend/applicationReport/vulnerabilities/ApplicationReportVulnerabilitiesHeader';
import ApplicationReportVulnerabilitiesTable from '../../../../main/frontend/applicationReport/vulnerabilities/ApplicationReportVulnerabilitiesTable';
import ApplicationReportVulnerabilitiesPage from '../../../../main/frontend/applicationReport/vulnerabilities/ApplicationReportVulnerabilitiesPage';

import LoadWrapper from '../../../../main/frontend/react/LoadWrapper';
import BackButton from '../../../../main/frontend/react/BackButton';

describe('ApplicationReportVulnerabilitiesPage', function () {
  let loadReportAllDataSpy, getShallowComponent, mock$State;

  const minimalMetadata = {
    reportTitle: 'fooReport',
    reportTime: 0,
    application: { name: 'foo app' },
  };

  beforeEach(function () {
    loadReportAllDataSpy = jasmine.createSpy('loadReportAllData');
    mock$State = jasmine.createSpyObj('$state', ['get', 'href']);

    const minimalProps = {
      loading: false,
      loadReportAllData: loadReportAllDataSpy,
      vulnerabilities: [],
      vulnerabilitiesPageEnabled: true,
      $state: mock$State,
    };

    getShallowComponent = enzymeUtils.getShallowComponent(
      ApplicationReportVulnerabilitiesPage,
      minimalProps
    );
  });

  it('renders a BackButton with the applicationReport.policy state name and the provided $state object, ', function () {
    const backButton = getShallowComponent().find(BackButton);

    expect(backButton).toExist();
    expect(backButton).toHaveProp('stateName', 'applicationReport.policy');
    expect(backButton).toHaveProp('$state', mock$State);
  });

  it('renders a tile with the header and table, within a LoadWrapper', function () {
    expect(getShallowComponent()).toContainMatchingElement(LoadWrapper);
    expect(getShallowComponent().find(LoadWrapper).prop('children')).toEqual(
      jasmine.any(Function)
    );

    const loadWrapperChildren = enzymeUtils.getLoadWrapperChildren(
        getShallowComponent({ metadata: minimalMetadata })
      ),
      tile = loadWrapperChildren.find('.nx-tile');

    expect(tile.childAt(0)).toMatchSelector(
      ApplicationReportVulnerabilitiesHeader
    );
    expect(tile.childAt(1)).toMatchSelector(
      ApplicationReportVulnerabilitiesTable
    );
  });

  it("sets the LoadWrapper's loading prop if the metadata prop is falsey or the loading prop is set", function () {
    const metadata = {
      reportTitle: 'fooReport',
      reportTime: 0,
      application: { name: 'foo app' },
    };

    expect(getShallowComponent().find(LoadWrapper)).toHaveProp('loading', true);
    expect(
      getShallowComponent({ metadata: null }).find(LoadWrapper)
    ).toHaveProp('loading', true);
    expect(
      getShallowComponent({ metadata: undefined }).find(LoadWrapper)
    ).toHaveProp('loading', true);
    expect(
      getShallowComponent({ metadata, loading: true }).find(LoadWrapper)
    ).toHaveProp('loading', true);
    expect(getShallowComponent({ metadata }).find(LoadWrapper)).toHaveProp(
      'loading',
      false
    );
  });

  it("sets the LoadWrapper's error prop to the loadError", function () {
    expect(getShallowComponent().find(LoadWrapper)).toHaveProp(
      'error',
      undefined
    );
    expect(
      getShallowComponent({ loadError: null }).find(LoadWrapper)
    ).toHaveProp('error', undefined);
    expect(
      getShallowComponent({ loadError: undefined }).find(LoadWrapper)
    ).toHaveProp('error', undefined);
    expect(
      getShallowComponent({ loadError: 'error!' }).find(LoadWrapper)
    ).toHaveProp('error', 'error!');
  });

  it("sets the LoadWrapper's error if vulnerabilitiesPageEnabled is not true", function () {
    expect(
      getShallowComponent({ vulnerabilitiesPageEnabled: false }).find(
        LoadWrapper
      )
    ).toHaveProp('error', jasmine.any(String));
  });

  it("passes loadReportAllData as the LoadWrapper's retryHandler", function () {
    expect(getShallowComponent().find(LoadWrapper)).toHaveProp(
      'retryHandler',
      loadReportAllDataSpy
    );
  });

  it('passes the metadata prop on to the header', function () {
    const getHeader = (additionalProps) =>
      enzymeUtils
        .getLoadWrapperChildren(getShallowComponent(additionalProps))
        .find(ApplicationReportVulnerabilitiesHeader);

    expect(getHeader({ metadata: minimalMetadata })).toHaveProp(
      'metadata',
      minimalMetadata
    );
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
      ],
      getTable = (additionalProps) =>
        enzymeUtils
          .getLoadWrapperChildren(getShallowComponent(additionalProps))
          .find(ApplicationReportVulnerabilitiesTable);

    expect(getTable({ metadata: minimalMetadata })).toHaveProp(
      'vulnerabilities',
      []
    );
    expect(getTable({ metadata: minimalMetadata, vulnerabilities })).toHaveProp(
      'vulnerabilities',
      vulnerabilities
    );
  });

  it('calls loadReportAllData on mount', function () {
    getShallowComponent();

    expect(loadReportAllDataSpy).toHaveBeenCalled();
  });
});
