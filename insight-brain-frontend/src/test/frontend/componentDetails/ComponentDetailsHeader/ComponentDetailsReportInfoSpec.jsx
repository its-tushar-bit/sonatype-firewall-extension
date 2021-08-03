/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as enzymeUtils from '../../enzymeUtils';
import * as dateUtils from '../../../../main/frontend/util/dateUtils';

import { ComponentDetailsReportInfo } from '../../../../main/frontend/componentDetails/ComponentDetailsHeader';

describe('ComponentDetailsReportInfo', () => {
  let minimalProps;
  let getShallowComponent;
  let getShallowComponentNoProps;
  let getMountedComponent;

  beforeEach(() => {
    minimalProps = {
      applicationName: 'My Application',
    };

    getShallowComponent = enzymeUtils.getShallowComponent(ComponentDetailsReportInfo, minimalProps);
    getShallowComponentNoProps = enzymeUtils.getShallowComponent(ComponentDetailsReportInfo);
    getMountedComponent = enzymeUtils.getMountedComponent(ComponentDetailsReportInfo, minimalProps);
  });

  it('merges the className attribute to the root element if className is passed as props', () => {
    const component = getShallowComponent();
    const currentClassNames = component.first().prop('className');
    component.setProps({ className: 'my-class' });
    const el = component.first();
    expect(el).toHaveClassName(currentClassNames);
    expect(el).toHaveClassName('my-class');
  });

  it('forwards all extra props to the root element', () => {
    const component = getShallowComponent({ id: 'thisone', tabIndex: 0, 'data-testid': 'bar' });
    const el = component.first();
    expect(el).toHaveProp('id', 'thisone');
    expect(el).toHaveProp('tabIndex', 0);
    expect(el).toHaveProp('data-testid', 'bar');
  });

  it('does not render if there is no applicationName, organizationName or reportTime and reportTitle props passed', () => {
    const component = getShallowComponentNoProps();
    expect(component).toBeEmptyRender();
  });

  it('only renders Organization Name part if `organizationName` prop is passed', () => {
    const organizationName = 'My Organization';
    const component = getMountedComponent({ applicationName: null, organizationName });
    expect(component).toIncludeText(organizationName);
    const componentWithout = getMountedComponent({ applicationName: 'My Application' });
    expect(componentWithout).not.toIncludeText(organizationName);
  });

  it('only renders Application Name part if `applicationName` prop is passed', () => {
    const applicationName = 'My Application';
    const component = getMountedComponent({ applicationName, organizationName: null });
    expect(component).toIncludeText(applicationName);

    const componentWithout = getMountedComponent({ applicationName: null, organizationName: 'My Organization' });
    expect(componentWithout).not.toIncludeText(applicationName);
  });

  it('only renders Report Evaluation timestamp part if both `reportTitle` and `reportTime` props are passed', () => {
    const formattedTimestamp = 'Report Build 22/22/22';
    spyOn(dateUtils, 'formatDate').and.returnValue(formattedTimestamp);
    const reportTitle = 'Report Build';
    const reportTime = 1234562;
    const component = getMountedComponent({ reportTitle, reportTime });
    expect(component).toIncludeText(formattedTimestamp);

    const componentWithoutTitle = getMountedComponent({ reportTitle: null, reportTime });
    expect(componentWithoutTitle).not.toIncludeText(formattedTimestamp);

    const componentWithoutTime = getMountedComponent({ reportTime: null, reportTitle });
    expect(componentWithoutTime).not.toIncludeText(formattedTimestamp);
  });
});
