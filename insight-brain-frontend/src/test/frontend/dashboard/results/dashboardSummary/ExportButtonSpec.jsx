/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as enzymeUtils from '../../../enzymeUtils';
import ExportButton from '../../../../../main/frontend/dashboard/results/dashboardSummary/ExportButton/ExportButton';

describe('ExportButton', () => {
  let getMountedComponent;

  beforeEach(() => {
    getMountedComponent = enzymeUtils.getMountedComponent(ExportButton);
  });

  it('renders a component', () => {
    expect(getMountedComponent()).toExist();
  });

  it('renders a form with a hidden input and a button as children', () => {
    const component = getMountedComponent();
    expect(component.find('form input')).toExist();
    expect(component.find('form button')).toExist();
  });

  it('renders the form input with the action attribute equal to the exportUrl prop', () => {
    const props = { exportUrl: 'https://example.com/post' };
    const component = getMountedComponent(props);
    expect(component.find('form').prop('action')).toEqual(props.exportUrl);
  });

  it('disables the button when the exportUrl prop is falsey or an empty string', () => {
    const props = { exportUrl: '' };
    const component = getMountedComponent(props);
    expect(component.find('button').prop('disabled')).toEqual(true);
  });

  it('renders the hidden input with the JSON.stringified value of the exportRequestData prop', () => {
    const exportRequestData = {
      orderBy: '-TOTAL_RISK',
      organizationIds: [],
      applicationIds: [],
      stageIds: [],
      tagIds: [],
      policyViolationStates: ['OPEN'],
      maxDaysOld: 30,
      policyThreatLevelRange: '2,10'
    };
    const component = getMountedComponent({ exportRequestData });
    expect(component.find('form input').prop('value')).toEqual(
        JSON.stringify(exportRequestData)
    );
  });
});
