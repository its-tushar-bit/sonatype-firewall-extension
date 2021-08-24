/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import SuccessMetricsReportListItem from '../../../../main/frontend/labs/successMetrics/SuccessMetricsReportListItem';
import { getShallowComponent } from '../../enzymeUtils';
import * as routerContext from '../../../../main/frontend/react/RouterStateContext';

describe('successMetricsReportListItem', () => {
  let getShallow, initialProps, hrefSpy;

  beforeEach(() => {
    hrefSpy = jasmine.createSpy('href');

    spyOn(routerContext, 'useRouterState').and.returnValue({
      href: hrefSpy,
    });

    initialProps = {
      reportId: '101',
      reportName: 'test 101',
    };

    getShallow = getShallowComponent(SuccessMetricsReportListItem, initialProps);
  });

  it('renders a list item with the report name', () => {
    const component = getShallow();
    expect(component.find('.nx-list__item--link')).toIncludeText(initialProps.reportName);
  });
  it('renders a link to go to report with proper report id', () => {
    getShallow();
    expect(hrefSpy).toHaveBeenCalledWith('labs.successMetricsReport', {
      successMetricsReportId: initialProps.reportId,
    });
  });
});
