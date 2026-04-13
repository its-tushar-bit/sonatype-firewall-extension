/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { axiosMockAdapter, fireEvent, render, screen, waitFor, within } from 'TestRoot/SpecUtil';

import {
  getActiveViolationsWithActionFailUrl,
  getAddContainerImagePolicyWaiverUrl,
  getPolicyWaiverReasonsUrl,
} from 'MainRoot/util/CLMLocation';
import AddContainerImageWaiverPage from 'MainRoot/firewall/containerImageWaiver/AddContainerImageWaiverPage';
import { activeViolationsResult as mockPayload, waiverReasons } from './data';
import * as RouterActions from 'MainRoot/reduxUiRouter/routerActions';
import * as productFeatures from 'MainRoot/productFeatures/productFeaturesSelectors';

describe('AddContainerImageWaiverPage', () => {
  let axiosMock;

  const getPreloadState = () =>
    Object.freeze({
      router: { currentParams: { publicId: 'test-public-id', scanId: 'test-scan-id' } },
    });

  const renderComponent = (preloadedState = getPreloadState()) =>
    render(<AddContainerImageWaiverPage />, { preloadedState });

  const validateOptions = (options, expectedValues) => {
    expect(options).toHaveLength(expectedValues.length);

    expectedValues.forEach((value, index) => {
      expect(options[index]).toHaveTextContent(value);
    });
  };

  beforeAll(() => {
    axiosMock = axiosMockAdapter();
  });

  beforeEach(async function () {
    jest.spyOn(productFeatures, 'selectIsContainerImagesEvaluationEnabled').mockReturnValue(true);
    axiosMock.onGet(getActiveViolationsWithActionFailUrl('test-public-id', 'proxy')).reply(200, mockPayload);
    axiosMock.onGet(getPolicyWaiverReasonsUrl()).reply(200, waiverReasons);

    renderComponent();
    await waitFor(() => expect(screen.queryByText('Loading…')).toBeNull());
  });

  it('renders load error when feature flag is disabled', async () => {
    jest.spyOn(productFeatures, 'selectIsContainerImagesEvaluationEnabled').mockReturnValue(false);
    renderComponent();

    expect(screen.getByRole('alert')).toHaveTextContent(
      'An error occurred loading data. This feature is not supported.'
    );
  });

  it('renders correct content', async () => {
    expect(screen.getByRole('heading', { level: 1, name: /Add Waiver/i })).toBeVisible();
    expect(screen.getByRole('heading', { level: 2, name: /Waiver Configuration/i })).toBeVisible();
    expect(screen.getAllByText('1')).toHaveLength(3); // threat indicators
    expect(screen.getByText('3 FAILED VIOLATIONS')).toBeInTheDocument();
    expect(screen.getByText('Affecting 3 components')).toBeInTheDocument();
    expect(screen.getByRole('status')).toBeInTheDocument();
    expect(screen.getByRole('status')).toHaveTextContent(
      'Proceeding to create a waiver will waive all failing policy violations identified in this evaluation. ' +
        'After applying this waiver, you can review waived policy violations per component within the ' +
        'Container Image Report.'
    );
    expect(screen.getByText('docker-all')).toBeInTheDocument();
    expect(screen.getByText('docker-policy-2.7.6-r0')).toBeInTheDocument();
    expect(screen.getByText('docker-policy-2.5.5-r2')).toBeInTheDocument();
    expect(screen.getByText('alpine : 3.6.5 (Container)')).toBeInTheDocument();
    expect(screen.getAllByRole('combobox')).toHaveLength(2);
    const waiverSelect = screen.getAllByRole('combobox')[0];
    expect(waiverSelect).toHaveDisplayValue('Never');
    validateOptions(waiverSelect.options, [
      'Never',
      '7 Days',
      '14 Days',
      '30 Days',
      '60 Days',
      '90 Days',
      '120 Days',
      'Custom',
    ]);
    const reasonSelect = screen.getAllByRole('combobox')[1];
    validateOptions(reasonSelect.options, [
      'Select a reason',
      'Acknowledged violation',
      'Mitigated externally',
      'No upgrade path',
      'Not exploitable',
      'Not reachable',
      'Researching',
      'Other',
    ]);
    expect(screen.getByRole('textbox', { name: 'Comments' })).toBeInTheDocument;
  });

  it('renders tooltip when hovering on the threat level indicators', async () => {
    let tooltip;
    fireEvent.mouseOver(screen.getAllByText('1')[0]);
    tooltip = await screen.findByRole('tooltip', {
      name: 'Critical',
    });
    expect(tooltip).toBeInTheDocument();

    fireEvent.mouseOver(screen.getAllByText('1')[1]);
    tooltip = await screen.findByRole('tooltip', {
      name: 'Severe',
    });
    expect(tooltip).toBeInTheDocument();

    fireEvent.mouseOver(screen.getAllByText('1')[2]);
    tooltip = await screen.findByRole('tooltip', {
      name: 'Moderate',
    });
    expect(tooltip).toBeInTheDocument();
  });

  it('selects the waiver expiry time', async () => {
    const waiverSelect = screen.getAllByRole('combobox')[0];
    const options = within(waiverSelect).getAllByRole('option');
    fireEvent.change(waiverSelect, { target: { value: '30' } });
    expect(options[3].selected).toBeTruthy();
    expect(waiverSelect).toHaveDisplayValue('30 Days');

    fireEvent.change(waiverSelect, { target: { value: 'custom' } });
    expect(options[7].selected).toBeTruthy();
    expect(waiverSelect).toHaveDisplayValue('Custom');

    const dateWrapper = screen.getByTestId('add-container-image-waiver-custom-date');
    expect(dateWrapper).toBeInTheDocument();
    const dateInput = dateWrapper.querySelector('input[type="date"]') || dateWrapper.querySelector('input');
    const date = new Date();
    date.setDate(date.getDate() - 5);
    const formattedDateMinus5Day = date.toISOString().split('T')[0];

    fireEvent.change(dateInput, { target: { value: formattedDateMinus5Day } });
    expect(dateInput.value).toBe(formattedDateMinus5Day);
    const validationErrors = screen.getAllByRole('alert');
    expect(validationErrors[0]).toHaveTextContent('Date must be in the future');
  });

  it('selects the waiver reason', async () => {
    const reasonSelect = screen.getAllByRole('combobox')[1];
    const options = within(reasonSelect).getAllByRole('option');
    fireEvent.change(reasonSelect, { target: { value: '42069f58114f4df8b435a40a415d2835' } });
    expect(options[2].selected).toBeTruthy();
    expect(reasonSelect).toHaveDisplayValue('Mitigated externally');
  });

  it('sets the waiver comments', async () => {
    const commentsInput = screen.getByRole('textbox', { name: 'Comments' });
    fireEvent.change(commentsInput, { target: { value: 'Test waiver comment' } });
    expect(commentsInput.value).toBe('Test waiver comment');
  });

  it('calls submit request with correct data when form is submitted', async () => {
    const payload = {
      expiryTime: null,
      waiverReasonId: null,
      comment: 'Test waiver comment',
    };

    const commentsInput = screen.getByRole('textbox', { name: 'Comments' });
    fireEvent.change(commentsInput, { target: { value: 'Test waiver comment' } });

    const submitButton = screen.getByRole('button', { name: 'Submit' });
    fireEvent.click(submitButton);
    expect(axiosMock.history.post.length).toBe(1);
    expect(axiosMock.history.post[0].url).toBe(getAddContainerImagePolicyWaiverUrl('test-public-id'));
    expect(JSON.parse(axiosMock.history.post[0].data)).toEqual(payload);
  });

  it('calls stateGo when cancel button is clicked', async () => {
    const stateGoSpy = jest.spyOn(RouterActions, 'stateGo');
    const cancelButton = screen.getByRole('button', { name: 'Cancel' });
    fireEvent.click(cancelButton);
    expect(stateGoSpy).toHaveBeenCalledWith('firewall.containerReport', {
      publicId: 'test-public-id',
      scanId: 'test-scan-id',
    });
  });
});
