/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as enzymeUtils from '../../../enzymeUtils';

import { NxTableCell } from '@sonatype/react-shared-components';
import DashboardApplicationsTableStageRiskRow from '../../../../../main/frontend/dashboard/results/applications/DashboardApplicationsTableStageRiskRow';

describe('DashboardApplicationsTableStageRiskRow', function () {
  let getMountedComponent, realUseContext, hrefSpy;

  beforeEach(function () {
    getMountedComponent = enzymeUtils.getMountedComponent(DashboardApplicationsTableStageRiskRow);
    hrefSpy = jasmine.createSpy('href').and.returnValue('linkToReport');

    // Mock the expected hook 'useContext'
    realUseContext = React.useContext;
    React.useContext = jasmine.createSpy('useContextHook').and.returnValue({ href: hrefSpy });
  });

  afterEach(function () {
    React.useContext = realUseContext;
  });

  it('renders a link to the related stage report in the stage name', function () {
    const riskRowProps = {
      applicationId: 'appId',
      stageRisk: { stageTypeName: 'build', scanId: 'scan1', risk: {} },
    };

    const row = getMountedComponent(riskRowProps),
      stageNameCell = row.find(NxTableCell).at(0),
      link = stageNameCell.find('a');

    expect(hrefSpy).toHaveBeenCalledWith('applicationReport.policy', {
      publicId: 'appId',
      scanId: 'scan1',
    });
    expect(link).toHaveProp('href', 'linkToReport');
  });
});
