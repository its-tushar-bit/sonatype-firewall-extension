/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { NxTableCell } from '@sonatype/react-shared-components';

import DashboardApplicationsTableStageRiskRow from 'MainRoot/dashboard/results/applications/DashboardApplicationsTableStageRiskRow';
import * as routerContext from 'MainRoot/react/RouterStateContext';
import { getShallowComponent } from 'TestRoot/enzymeUtils';

describe('DashboardApplicationsTableStageRiskRow', function () {
  let hrefSpy, getShallow;

  beforeEach(function () {
    hrefSpy = jasmine.createSpy('href').and.returnValue('linkToReport');
    spyOn(routerContext, 'useRouterState').and.returnValue({
      href: hrefSpy,
    });

    const minimalProps = {
      appAutomationId: 'rowId',
      applicationId: 'appId',
      isLastStageRisk: true,
      stageRisk: { stageTypeName: 'build', scanId: 'scan1', risk: {} },
    };
    getShallow = getShallowComponent(DashboardApplicationsTableStageRiskRow, minimalProps);
  });

  it('renders a link to the related stage report in the stage name', function () {
    const row = getShallow(),
      stageNameCell = row.find(NxTableCell).at(0),
      link = stageNameCell.find('a');

    expect(hrefSpy).toHaveBeenCalledWith('applicationReport.policy', {
      publicId: 'appId',
      scanId: 'scan1',
    });
    expect(link).toHaveProp('href', 'linkToReport');
  });
});
