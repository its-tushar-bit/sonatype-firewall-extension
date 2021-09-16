/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { mount } from 'enzyme';

import { NxTableCell } from '@sonatype/react-shared-components';
import RouterStateContext from '../../../../../main/frontend/react/RouterStateContext';
import DashboardApplicationsTableStageRiskRow from '../../../../../main/frontend/dashboard/results/applications/DashboardApplicationsTableStageRiskRow';

describe('DashboardApplicationsTableStageRiskRow', function () {
  let hrefSpy;

  beforeEach(function () {
    hrefSpy = jasmine.createSpy('href').and.returnValue('linkToReport');
  });

  it('renders a link to the related stage report in the stage name', function () {
    const riskRowProps = {
      applicationId: 'appId',
      stageRisk: { stageTypeName: 'build', scanId: 'scan1', risk: {} },
    };

    const row = mount(
        <RouterStateContext.Provider value={{ href: hrefSpy }}>
          <DashboardApplicationsTableStageRiskRow {...riskRowProps} />
        </RouterStateContext.Provider>
      ),
      stageNameCell = row.find(NxTableCell).at(0),
      link = stageNameCell.find('a');

    expect(hrefSpy).toHaveBeenCalledWith('applicationReport.policy', {
      publicId: 'appId',
      scanId: 'scan1',
    });
    expect(link).toHaveProp('href', 'linkToReport');
  });
});
