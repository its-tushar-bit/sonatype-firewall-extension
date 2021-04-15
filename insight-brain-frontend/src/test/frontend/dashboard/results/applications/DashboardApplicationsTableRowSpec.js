/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as enzymeUtils from '../../../enzymeUtils';

import { NxTableRow } from '@sonatype/react-shared-components';

import DashboardApplicationsTableRow from '../../../../../main/frontend/dashboard/results/applications/DashboardApplicationsTableRow';
import DashboardApplicationsTableStageRiskRow from '../../../../../main/frontend/dashboard/results/applications/DashboardApplicationsTableStageRiskRow';

describe('DashboardApplicationsTableRow', function () {
  let getShallowComponent;

  beforeEach(function () {
    getShallowComponent = enzymeUtils.getShallowComponent(DashboardApplicationsTableRow);
  });

  it('renders an NxTableRow for the app information and an NxTableRow for each stage risk', function () {
    const rowProps = {
      application: {
        applicationId: 'appId',
        applicationName: 'appName',
        totalApplicationRisk: {},
        stageRisks: [
          { stageTypeName: 'build', scanId: 'scan1' },
          { stageTypeName: 'release', scanId: 'scan2' },
        ],
      },
    };

    const row = getShallowComponent(rowProps),
      mainRow = row.find(NxTableRow),
      stageRiskRows = row.find(DashboardApplicationsTableStageRiskRow);

    expect(mainRow).toExist();
    expect(stageRiskRows.length).toBe(2);

    expect(stageRiskRows.at(0)).toHaveProp('applicationId', 'appId');
    expect(stageRiskRows.at(0)).toHaveProp('stageRisk', {
      stageTypeName: 'build',
      scanId: 'scan1',
    });
    expect(stageRiskRows.at(0)).toHaveProp('isLastStageRisk', false);
    expect(stageRiskRows.at(0).key()).toBe('scan1');

    expect(stageRiskRows.at(1)).toHaveProp('applicationId', 'appId');
    expect(stageRiskRows.at(1)).toHaveProp('stageRisk', {
      stageTypeName: 'release',
      scanId: 'scan2',
    });
    expect(stageRiskRows.at(1)).toHaveProp('isLastStageRisk', true);
    expect(stageRiskRows.at(1).key()).toBe('scan2');
  });
});
