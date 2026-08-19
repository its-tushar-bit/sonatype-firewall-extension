/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import { axiosMockAdapter } from 'TestRoot/SpecUtil';
import WaiversConfiguration from 'MainRoot/OrgsAndPolicies/autoWaiversConfiguration/AutoWaiversConfiguration';
import {
  getAutoWaiversConfigurationURL,
  getAutoWaiversConfigurationURLWaiver,
  getAutoWaiversConfigurationURLnoStatus,
} from 'MainRoot/util/CLMLocation';
import React from 'react';

describe('WaiversConfiguration URL Mock Test', () => {
  let mock, renderComponent;
  renderComponent = () => render(<WaiversConfiguration />);

  beforeAll(() => {
    mock = axiosMockAdapter();
  });

  afterEach(() => {
    mock.reset();
  });

  it('returns the expected data from the mock waiversConfigurationUrl', async () => {
    const waiversConfigurationUrl = getAutoWaiversConfigurationURL('organization', 'ROOT_ORGANIZATION_ID');
    const mockResponse = {
      isAutoWaiverEnabled: false,
      isInherited: false,
      autoPolicyWaiverId: 'waiverId',
      autoPolicyWaiverOwnerId: 'ownerId',
      autoPolicyWaiverOwnerName: 'ownerName',
    };

    mock.onGet(waiversConfigurationUrl).reply(200, mockResponse);

    const response = await axios.get(waiversConfigurationUrl);

    expect(response.status).toBe(200);
    expect(response.data).toEqual(mockResponse);
    renderComponent();
    expect(screen.getByRole('heading', { name: 'Automated Waivers' })).toBeVisible();
    expect(
      screen.getByText(
        'Limit disruptions by deprioritizing low-threat violations until a remediation path is available.'
      )
    ).toBeVisible();
    expect(await screen.findByText('Max. Threat Level')).toBeVisible();
    expect(await screen.findByRole('button', { name: 'Update' })).toBeVisible();
    const notReachableCheckbox = await screen.findByLabelText('Security vulnerability is Not Reachable');
    const noPathForwardCheckbox = await screen.findByLabelText(
      'No newer, non-violating component version is available'
    );

    expect(notReachableCheckbox).toBeInTheDocument();
    expect(noPathForwardCheckbox).toBeInTheDocument();
  });

  it('returns the expected data from the mock waiversConfigurationUrlWaiver', async () => {
    const waiversConfigurationUrl = getAutoWaiversConfigurationURLWaiver(
      'organization',
      'ROOT_ORGANIZATION_ID',
      'waiversId'
    );

    const mockResponse = {
      reachable: false,
      pathForward: false,
      threatLevel: 7,
    };

    mock.onGet(waiversConfigurationUrl).reply(200, mockResponse);

    const response = await axios.get(waiversConfigurationUrl);

    expect(response.status).toBe(200);
    expect(response.data).toEqual(mockResponse);
    renderComponent();

    expect(screen.getByText('7')).toBeVisible();
    expect(screen.getByLabelText('Security vulnerability is Not Reachable')).not.toBeChecked();
    expect(screen.getByLabelText('No newer, non-violating component version is available')).not.toBeChecked();
  });

  it('handles a 404 error from the mock waiversConfigurationUrl', async () => {
    const waiversConfigurationUrl = getAutoWaiversConfigurationURL('organization', 'ROOT_ORGANIZATION_ID');

    mock.onGet(waiversConfigurationUrl).reply(404, { message: 'Not Found' });
    renderComponent();
    const alert = screen.getByRole('alert');
    expect(alert).toBeInTheDocument();
    expect(alert).toHaveTextContent(
      'An error occurred loading data. Organization with ID ROOT_ORGANIZATION_ID does not exist.'
    );
  });

  it('handles a 500 error from the mock waiversConfigurationUrl', async () => {
    const waiversConfigurationUrl = getAutoWaiversConfigurationURL('organization', 'ROOT_ORGANIZATION_ID');

    mock.onGet(waiversConfigurationUrl).reply(500, { message: 'Internal Server Error' });
    renderComponent();
    const alert = screen.getByRole('alert');
    expect(alert).toBeInTheDocument();
  });

  it('handles a successful PUT request to the mock waiversConfigurationUrlWaiver', async () => {
    const waiversConfigurationUrl = getAutoWaiversConfigurationURLWaiver(
      'organization',
      'ROOT_ORGANIZATION_ID',
      'waiversId'
    );

    const putData = {
      reachable: true,
      pathForward: true,
      threatLevel: 5,
    };

    mock.onPut(waiversConfigurationUrl, putData).reply(200, putData);

    renderComponent();

    const threatLevelIndicator = await screen.findByText('5');
    expect(threatLevelIndicator).toBeVisible();

    const reachableCheckbox = screen.getByLabelText('Security vulnerability is Not Reachable');
    const pathForwardCheckbox = screen.getByLabelText('No newer, non-violating component version is available');

    fireEvent.click(reachableCheckbox);
    fireEvent.click(pathForwardCheckbox);

    const submitButton = screen.getByRole('button', { name: 'Update' });
    fireEvent.click(submitButton);

    expect(mock.history.put.length).toBe(1);
    expect(mock.history.put[0].url).toBe(waiversConfigurationUrl);
    expect(JSON.parse(mock.history.put[0].data)).toEqual(putData);
  });

  it('handles a failed PUT request with 500 error to the mock waiversConfigurationUrlWaiver', async () => {
    const waiversConfigurationUrl = getAutoWaiversConfigurationURLWaiver(
      'organization',
      'ROOT_ORGANIZATION_ID',
      'waiversId'
    );

    const putData = {
      reachable: true,
      pathForward: true,
      threatLevel: 5,
    };

    mock.onPut(waiversConfigurationUrl, putData).reply(500, { message: 'Internal Server Error' });

    renderComponent();

    const reachableCheckbox = screen.getByLabelText('Security vulnerability is Not Reachable');
    const pathForwardCheckbox = screen.getByLabelText('No newer, non-violating component version is available');

    fireEvent.click(reachableCheckbox);
    fireEvent.click(pathForwardCheckbox);

    const submitButton = screen.getByRole('button', { name: 'Update' });
    fireEvent.click(submitButton);

    expect(mock.history.put.length).toBe(1);
    expect(mock.history.put[0].url).toBe(waiversConfigurationUrl);
    expect(JSON.parse(mock.history.put[0].data)).toEqual(putData);

    const alert = screen.getByRole('alert');
    expect(alert).toBeInTheDocument();
  });

  it('handles a successful POST request to the mock waiversConfigurationUrlnoStatus', async () => {
    const waiversConfigurationUrl = getAutoWaiversConfigurationURLnoStatus('organization', 'ROOT_ORGANIZATION_ID');

    const postData = {
      reachable: true,
      pathForward: false,
      threatLevel: 7,
    };

    const postResponse = {
      reachable: true,
      pathForward: false,
      threatLevel: 7,
    };

    mock.onPost(waiversConfigurationUrl, postData).reply(200, postResponse);

    const response = await axios.post(waiversConfigurationUrl, postData);

    expect(response.status).toBe(200);
    expect(response.data).toEqual(postResponse);

    renderComponent();
    expect(screen.getByText('7')).toBeVisible();
    expect(screen.getByLabelText('Security vulnerability is Not Reachable')).toBeChecked();
    expect(screen.getByLabelText('No newer, non-violating component version is available')).not.toBeChecked();
  });

  it('handles a failed POST request with 500 error to the mock waiversConfigurationUrlnoStatus', async () => {
    const waiversConfigurationUrl = getAutoWaiversConfigurationURLnoStatus('organization', 'ROOT_ORGANIZATION_ID');

    const postData = {
      reachable: true,
      pathForward: false,
      threatLevel: 7,
    };

    mock.onPost(waiversConfigurationUrl, postData).reply(500, { message: 'Internal Server Error' });

    try {
      await axios.post(waiversConfigurationUrl, postData);
    } catch (error) {
      expect(error.response.status).toBe(500);
      expect(error.response.data).toEqual({ message: 'Internal Server Error' });
    }
    renderComponent();
    const alert = screen.getByRole('alert');
    expect(alert).toBeInTheDocument();
  });

  it('renders the expected data from the mock waiversConfigurationUrl in the component', async () => {
    const waiversConfigurationUrl = getAutoWaiversConfigurationURL('organization', 'ROOT_ORGANIZATION_ID');
    const mockResponse = {
      isAutoWaiverEnabled: true,
      isInherited: false,
      autoPolicyWaiverId: 'waiverId',
      autoPolicyWaiverOwnerId: 'ownerId',
      autoPolicyWaiverOwnerName: 'ownerName',
      reachable: true,
      pathForward: true,
      threatLevel: 7,
    };

    mock.onGet(waiversConfigurationUrl).reply(200, mockResponse);

    renderComponent();

    expect(screen.getByText('7')).toBeVisible();
    expect(screen.getByLabelText('Security vulnerability is Not Reachable')).toBeChecked();
    expect(screen.getByLabelText('No newer, non-violating component version is available')).toBeChecked();
  });

  it('renders an error message on a 500 error from the mock waiversConfigurationUrl in the component', async () => {
    const waiversConfigurationUrl = getAutoWaiversConfigurationURL('organization', 'ROOT_ORGANIZATION_ID');

    mock.onGet(waiversConfigurationUrl).reply(500, { message: 'Internal Server Error' });

    renderComponent();

    await waitFor(() => {
      expect(screen.getByText('Error loading waivers configuration')).toBeVisible();
    });
  });

  it('renders the correct state when the data indicates auto-waiver is inherited in the component', async () => {
    const waiversConfigurationUrl = getAutoWaiversConfigurationURL('organization', 'ROOT_ORGANIZATION_ID');
    const mockResponse = {
      isAutoWaiverEnabled: true,
      isInherited: true,
      autoPolicyWaiverId: 'waiverId',
      autoPolicyWaiverOwnerId: 'ownerId',
      autoPolicyWaiverOwnerName: 'ownerName',
      reachable: false,
      pathForward: false,
      threatLevel: 7,
    };

    mock.onGet(waiversConfigurationUrl).reply(200, mockResponse);

    renderComponent();

    expect(screen.getByText('7')).toBeVisible();
    expect(screen.getByLabelText('Security vulnerability is Not Reachable')).not.toBeChecked();
    expect(screen.getByLabelText('No newer, non-violating component version is available')).not.toBeChecked();
  });

  it('shows an alert when no changes are made and waivers are enabled', async () => {
    const waiversConfigurationUrl = getAutoWaiversConfigurationURL('organization', 'ROOT_ORGANIZATION_ID');
    const mockResponse = {
      isAutoWaiverEnabled: true,
      isInherited: false,
      autoPolicyWaiverId: 'waiverId',
      autoPolicyWaiverOwnerId: 'ownerId',
      autoPolicyWaiverOwnerName: 'ownerName',
      reachable: false,
      pathForward: false,
      threatLevel: 7,
    };

    mock.onGet(waiversConfigurationUrl).reply(200, mockResponse);

    renderComponent();

    const submitButton = await screen.findByRole('button', { name: 'Update' });
    fireEvent.click(submitButton);

    const alert = screen.getByRole('alert');
    expect(alert).toHaveTextContent('There were validation errors. There are no changes to save.');
  });

  it('creates a waiver when waivers are disabled', async () => {
    const waiversConfigurationUrl = getAutoWaiversConfigurationURL('organization', 'ROOT_ORGANIZATION_ID');
    const waiversConfigurationUrlNoStatus = getAutoWaiversConfigurationURLnoStatus(
      'organization',
      'ROOT_ORGANIZATION_ID'
    );

    const mockResponse = {
      isAutoWaiverEnabled: false,
      isInherited: false,
      autoPolicyWaiverId: 'waiverId',
      autoPolicyWaiverOwnerId: 'ownerId',
      autoPolicyWaiverOwnerName: 'ownerName',
      reachable: false,
      pathForward: false,
      threatLevel: 7,
    };

    mock.onGet(waiversConfigurationUrl).reply(200, mockResponse);
    mock.onPost(waiversConfigurationUrlNoStatus).reply(200);

    renderComponent();

    const submitButton = await screen.findByRole('button', { name: 'Update' });
    fireEvent.click(submitButton);

    expect(mock.history.post.length).toBe(1);
    expect(mock.history.post[0].url).toBe(waiversConfigurationUrlNoStatus);

    const postResponse = await axios.post(waiversConfigurationUrlNoStatus);
    expect(postResponse.status).toBe(200);
  });

  it('creates a waiver when waivers are enabled and inherited is true', async () => {
    const waiversConfigurationUrl = getAutoWaiversConfigurationURL('organization', 'ROOT_ORGANIZATION_ID');
    const waiversConfigurationUrlNoStatus = getAutoWaiversConfigurationURLnoStatus(
      'organization',
      'ROOT_ORGANIZATION_ID'
    );

    const mockResponse = {
      isAutoWaiverEnabled: true,
      isInherited: true,
      autoPolicyWaiverId: 'waiverId',
      autoPolicyWaiverOwnerId: 'ownerId',
      autoPolicyWaiverOwnerName: 'ownerName',
      reachable: false,
      pathForward: false,
      threatLevel: 7,
    };

    mock.onGet(waiversConfigurationUrl).reply(200, mockResponse);
    mock.onPost(waiversConfigurationUrlNoStatus).reply(200);

    renderComponent();

    const submitButton = await screen.findByRole('button', { name: 'Update' });
    fireEvent.click(submitButton);

    expect(mock.history.post.length).toBe(1);
    expect(mock.history.post[0].url).toBe(waiversConfigurationUrlNoStatus);

    const postResponse = await axios.post(waiversConfigurationUrlNoStatus);
    expect(postResponse.status).toBe(200);
  });
});
