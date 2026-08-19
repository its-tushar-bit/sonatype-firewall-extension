/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render } from 'TestRoot/SpecUtil';
import ComponentLegalOverviewPage from 'MainRoot/legal/ComponentLegalOverviewPage';
import * as RouterStateContext from 'MainRoot/react/RouterStateContext';

describe('ComponentLegalOverviewPage backHref', () => {
  const applicationPublicId = 'test-app-id';
  const componentHash = 'abc123hash';
  const sbomVersion = 'sha256:deadbeef';

  const baseProps = {
    component: null,
    loading: false,
    error: null,
    organizationId: null,
    applicationPublicId,
    stageTypeId: null,
    hash: componentHash,
    componentIdentifier: null,
    repositoryId: null,
    availableScopes: { loading: false, error: null, scopes: [] },
    obligations: [],
    noticeFiles: null,
    licenseFiles: null,
    sourceLinks: null,
    licenseLegalMetadata: null,
    showEditCopyrightOverrideModal: false,
    setDisplayCopyrightOverrideModal: jest.fn(),
    showNoticesModal: false,
    setShowNoticesModal: jest.fn(),
    showLicenseFilesModal: false,
    setShowLicenseFilesModal: jest.fn(),
    showLicensesModal: false,
    setShowLicensesModal: jest.fn(),
    showOriginalSourcesModal: false,
    setDisplayOriginalSourcesOverrideModal: jest.fn(),
    loadComponent: jest.fn(),
    loadComponentByComponentIdentifier: jest.fn(),
    loadAvailableScopes: jest.fn(),
    ecosystem: null,
    prevState: null,
    prevParams: null,
    tabId: 'legal',
    scanId: sbomVersion,
    isSbomManager: true,
  };

  let mockHref;

  beforeEach(() => {
    mockHref = jest.fn().mockReturnValue('#/mock-href');

    jest.spyOn(RouterStateContext, 'useRouterState').mockReturnValue({
      href: mockHref,
      get: jest.fn().mockReturnValue(null),
      current: { name: 'sbomManager.legal.applicationComponentOverviewByComponentIdentifier' },
      getCurrentPath: jest.fn().mockReturnValue(''),
    });
  });

  afterEach(() => {
    jest.restoreAllMocks();
  });

  it('testBackHref_SbomManagerContext_NavigatesToSbomManagerComponentRoute', () => {
    render(<ComponentLegalOverviewPage {...baseProps} />);

    expect(mockHref).toHaveBeenCalledWith('sbomManager.component', {
      applicationPublicId,
      sbomVersion,
      componentHash,
    });
  });

  it('testBackHref_LifecycleContext_NavigatesToApplicationReportRoute', () => {
    render(<ComponentLegalOverviewPage {...baseProps} isSbomManager={false} />);

    expect(mockHref).toHaveBeenCalledWith('applicationReport.componentDetails.legal', {
      publicId: applicationPublicId,
      scanId: sbomVersion,
      hash: componentHash,
    });
  });

  it('testBackHref_SbomManagerContext_DoesNotNavigateToApplicationReport', () => {
    render(<ComponentLegalOverviewPage {...baseProps} />);

    const applicationReportCalls = mockHref.mock.calls.filter(
      ([stateName]) => stateName === 'applicationReport.componentDetails.legal'
    );
    expect(applicationReportCalls).toHaveLength(0);
  });
});
