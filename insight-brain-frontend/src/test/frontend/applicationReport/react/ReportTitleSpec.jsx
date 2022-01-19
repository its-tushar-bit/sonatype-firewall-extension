/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import moment from 'moment-timezone';
import { NxButton, NxStatefulDropdown, NxTooltip } from '@sonatype/react-shared-components';
import * as enzymeUtils from 'TestRoot/enzymeUtils';
import ReportTitle from 'MainRoot/applicationReport/react/ReportTitle';

describe('ReportTitle component', function () {
  let getShallowComponent, mockedReevaluateReport;

  beforeAll(function () {
    moment.tz.setDefault('America/New_York');
  });

  afterAll(function () {
    moment.tz.setDefault();
  });

  beforeEach(function () {
    mockedReevaluateReport = jasmine.createSpy('reevaluateReport');

    const minimalProps = {
      metadataDetails: {
        reportTitle: 'Title',
        reportTime: moment('2018-11-11 15:13:11').toDate().getTime(),
        application: {
          id: 'metadataApplicationId',
          name: 'App Name',
        },
      },
      publicId: 'publicId',
      scanId: 'scanId',
      selectedReport: {
        reportVersion: 3,
      },
      reevaluateReport: mockedReevaluateReport,
    };

    getShallowComponent = enzymeUtils.getShallowComponent(ReportTitle, minimalProps);
  });

  it('renders a div a dropdown and a button', function () {
    const shallowComponent = getShallowComponent();
    const div = shallowComponent.find('div');
    const button = shallowComponent.find(NxButton);
    const dropdown = shallowComponent.find(NxStatefulDropdown);
    expect(div).toExist();
    expect(dropdown).toExist();
    expect(button).toExist();
  });

  it('renders a disabled link if report version is less than 5', function () {
    const shallowComponent = getShallowComponent();
    const tooltip = shallowComponent.find(NxTooltip);
    const link = tooltip.find('a');
    expect(tooltip).toHaveProp('title', 'Reevaluate the report in order to enable Vulnerabilities view');
    expect(link).toHaveClassName('disabled', true);
  });

  it('renders an enabled link if report version is greater than 5', function () {
    const props = {
      metadataDetails: {
        reportTitle: 'Title',
        application: {
          name: 'App Name',
        },
      },
      publicId: 'publicId',
      scanId: 'scanId',
      selectedReport: {
        reportVersion: 7,
      },
    };
    const shallowComponent = getShallowComponent(props);
    const button = shallowComponent.find(NxTooltip).find('a');
    expect(button).toHaveClassName('nx-dropdown-link');
    expect(button).not.toHaveClassName('disabled');
  });

  it('calls reevaluateReport when the reevaluateReport button is pressed', function () {
    const shallowComponent = getShallowComponent();
    const button = shallowComponent.find(NxButton);
    button.simulate('click');
    expect(mockedReevaluateReport).toHaveBeenCalled();
  });

  it('renders a page title value', function () {
    const component = getShallowComponent(),
      title = component.find('.nx-page-title').find('.nx-h1');
    expect(title).toHaveText('App Name Title');
  });

  it('renders a description with time value', function () {
    const component = getShallowComponent(),
      content = component.find('.nx-page-title__description');
    expect(content).toHaveText('2018-11-11 15:13:11 UTC-05:00');
  });

  it('renders dropdown with Generate PDF button', () => {
    const component = getShallowComponent();
    const pdfButton = component.find('.iq-report-actions').find('.nx-dropdown-button').get(0);

    expect(pdfButton.props.href).toBe('/rest/report/publicId/scanId/printReport');
  });

  it('renders dropdown with View SBOM button', () => {
    const component = getShallowComponent();
    const sbomButton = component.find('.iq-report-actions').find('.nx-dropdown-button').get(1);

    expect(sbomButton.props.href).toBe('/ui/links/cycloneDx/metadataApplicationId/reports/scanId');
  });
});
