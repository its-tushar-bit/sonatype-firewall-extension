/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render } from 'TestRoot/SpecUtil';
import ComponentOverviewTile from 'MainRoot/legal/ComponentOverviewTile';
import { NEXUS_ONE_APPLICATION_REPORT_STATE } from 'MainRoot/nexus-one/nexusOneApplicationReportStates';
import * as RouterStateContext from 'MainRoot/react/RouterStateContext';
import * as urlUtil from 'MainRoot/util/urlUtil';

describe('ComponentOverviewTile stage scan links', () => {
  const applicationPublicId = 'test-app-id';
  const scanId = 'test-scan-id';

  const component = {
    displayName: 'org.package : component1 : 1.0',
    licenseLegalData: {
      obligations: [],
      attributions: [],
      effectiveLicenses: [],
      highestEffectiveLicenseThreatGroup: null,
    },
    stageScans: [{ stageName: 'Release', scanId, scanDate: Date.now() }],
  };

  let mockHref;

  beforeEach(() => {
    mockHref = jest.fn().mockReturnValue('#/mock-href');
    jest.spyOn(RouterStateContext, 'useRouterState').mockReturnValue({ href: mockHref });
  });

  afterEach(() => {
    jest.restoreAllMocks();
  });

  it('links a stage scan to applicationReport.policy in the Classic bundle', () => {
    jest.spyOn(urlUtil, 'isNexusOneBundle').mockReturnValue(false);

    render(<ComponentOverviewTile applicationPublicId={applicationPublicId} component={component} />);

    expect(mockHref).toHaveBeenCalledWith('applicationReport.policy', { publicId: applicationPublicId, scanId });
  });

  it('links a stage scan to the Nexus One embed state in the Nexus One bundle', () => {
    // Regression guard (CLM-42162): applicationReport.policy is never registered in the Nexus One
    // bundle's own router (it embeds the same report under nexusOneApplicationReportStates.ts's
    // NEXUS_ONE_APPLICATION_REPORT_STATE instead) — without this branch, $state.href would return
    // null there and the stage link would render with no href at all.
    jest.spyOn(urlUtil, 'isNexusOneBundle').mockReturnValue(true);

    render(<ComponentOverviewTile applicationPublicId={applicationPublicId} component={component} />);

    expect(mockHref).toHaveBeenCalledWith(NEXUS_ONE_APPLICATION_REPORT_STATE, {
      publicId: applicationPublicId,
      scanId,
    });
  });
});
