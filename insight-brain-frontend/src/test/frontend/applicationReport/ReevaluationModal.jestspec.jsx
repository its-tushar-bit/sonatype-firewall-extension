/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen, fireEvent, axiosMockAdapter, within } from 'TestRoot/SpecUtil';
import * as productFeaturesSelectors from 'MainRoot/productFeatures/productFeaturesSelectors';
import * as routerSelectors from 'MainRoot/reduxUiRouter/routerSelectors';
import * as applicationReportSelectors from 'MainRoot/applicationReport/applicationReportSelectors';
import * as latestReportSelectors from 'MainRoot/applicationReport/latestReportForStageSelectors';
import ReevaluationModal from 'MainRoot/applicationReport/ReevaluationModal';

describe('ReevaluationModal', () => {
  let axiosMock;
  const givenScanIdForReport = 'scan-id';
  const givenPublicId = 'publicId';

  beforeEach(() => {
    jest.spyOn(applicationReportSelectors, 'selectReportParameters').mockReturnValue({
      appId: 'appId',
      scanId: givenScanIdForReport,
    });
    jest.spyOn(productFeaturesSelectors, 'selectIsAutoWaiversEnabled').mockReturnValue(false);
    jest.spyOn(applicationReportSelectors, 'selectApplicationReportSlice').mockReturnValue({ reevaluating: false });
    jest.spyOn(routerSelectors, 'selectRouterCurrentParams').mockReturnValue({
      publicId: givenPublicId,
      scanId: givenScanIdForReport,
    });
    jest.spyOn(latestReportSelectors, 'selectIsLatestReportForStageRequestPending').mockReturnValue(false);
    jest.spyOn(latestReportSelectors, 'selectLatestReportForStageId').mockReturnValue(givenScanIdForReport);
    axiosMock = axiosMockAdapter();
  });

  it('renders re-evaluate button correctly', () => {
    render(<ReevaluationModal />);
    expect(screen.getByRole('button', { name: 'Re-Evaluate Report' })).toBeInTheDocument();
  });

  it('renders re-evaluate button correctly when image container', () => {
    jest
      .spyOn(applicationReportSelectors, 'selectIsContainerImagesEvaluationEnabledAndProxyStage')
      .mockReturnValue(true);
    jest.spyOn(applicationReportSelectors, 'selectReportStageId').mockReturnValue('proxy');
    render(<ReevaluationModal />);
    expect(screen.getByRole('button', { name: 'Re-Evaluate Container' })).toBeInTheDocument();
  });

  it('disables re-evaluate button when not latest scan', () => {
    jest.spyOn(latestReportSelectors, 'selectLatestReportForStageId').mockReturnValue('scan-id-2');
    render(<ReevaluationModal />);

    const button = screen.getByRole('button', { name: 'Re-Evaluate Report' });
    expect(button).toBeDisabled();
  });

  it('shows tooltip when button is disabled', async () => {
    jest.spyOn(latestReportSelectors, 'selectLatestReportForStageId').mockReturnValue('different-scan-id');
    render(<ReevaluationModal />);

    fireEvent.mouseOver(screen.getByRole('button', { name: 'Re-Evaluate Report' }));

    expect(
      await screen.findByRole('tooltip', {
        name: 'Re-Evaluation is only allowed on the latest scan of a given stage.',
      })
    ).toBeInTheDocument();
  });

  describe('with auto waivers disabled', () => {
    it('triggers direct reevaluation on button click', () => {
      render(<ReevaluationModal />);

      fireEvent.click(screen.getByRole('button', { name: 'Re-Evaluate Report' }));

      expect(axiosMock.history.post).toHaveLength(1);
      expect(axiosMock.history.post[0].url).toEqual('/rest/report/appId/scan-id/reevaluatePolicy?async=true');
    });
  });

  describe('with auto waivers enabled', () => {
    beforeEach(() => {
      jest.spyOn(productFeaturesSelectors, 'selectIsAutoWaiversEnabled').mockReturnValue(true);
    });

    it('opens modal on button click', () => {
      render(<ReevaluationModal />);

      fireEvent.click(screen.getByRole('button', { name: 'Re-Evaluate Report' }));

      const dialog = screen.getByRole('dialog');
      expect(within(dialog).getByRole('heading', { name: 'Re-Evaluate Report' })).toBeInTheDocument();
    });

    it('handles quick re-evaluate', () => {
      render(<ReevaluationModal />);

      fireEvent.click(screen.getByRole('button', { name: 'Re-Evaluate Report' }));
      fireEvent.click(screen.getByRole('button', { name: 'Quick Re-Evaluate' }));

      expect(axiosMock.history.post).toHaveLength(1);
      expect(axiosMock.history.post[0].url).toEqual(
        '/rest/report/appId/scan-id/reevaluatePolicy?async=true&skipAutoWaivers=true'
      );
    });

    it('handles full re-evaluate', () => {
      render(<ReevaluationModal />);

      fireEvent.click(screen.getByRole('button', { name: 'Re-Evaluate Report' }));
      fireEvent.click(screen.getByRole('button', { name: 'Re-Evaluate' }));

      expect(axiosMock.history.post).toHaveLength(1);
      expect(axiosMock.history.post[0].url).toEqual('/rest/report/appId/scan-id/reevaluatePolicy?async=true');
    });

    it('handles modal cancel', () => {
      render(<ReevaluationModal />);

      fireEvent.click(screen.getByRole('button', { name: 'Re-Evaluate Report' }));
      fireEvent.click(screen.getByRole('button', { name: 'Cancel' }));

      expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    });

    it('disables modal buttons while reevaluating', () => {
      applicationReportSelectors.selectApplicationReportSlice.mockReturnValue({ reevaluating: true });
      render(<ReevaluationModal />);

      const reEvaluateButton = screen.getByRole('button', { name: 'Re-Evaluate Report' });
      expect(reEvaluateButton).toBeDisabled();
    });
  });
});
