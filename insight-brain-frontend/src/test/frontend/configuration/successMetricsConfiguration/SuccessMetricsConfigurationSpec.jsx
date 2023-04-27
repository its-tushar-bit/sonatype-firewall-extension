/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import axios from 'axios';

import { render, waitFor, fireEvent, screen } from '../../SpecUtil';
import SuccessMetricsConfigurationContainer from 'MainRoot/configuration/successMetricsConfiguration/SuccessMetricsConfigurationContainer';
import { getSuccessMetricsConfigUrl } from 'MainRoot/util/CLMLocation';
import { getGlobalPermissionTestUrl } from 'MainRoot/utilAngular/CLMContextLocation';

describe('SuccessMetricsConfigurationSpec', () => {
  const mockAxiosCalls = SpecUtil.axiosMockerGenerator(axios);
  const successMetricsConfigurationUrl = getSuccessMetricsConfigUrl();
  const globalPermissionTestUrl = getGlobalPermissionTestUrl();

  it('renders enabled toggle', async () => {
    mockAxiosCalls({
      get: {
        [successMetricsConfigurationUrl]: Promise.resolve({
          data: { enabled: true },
        }),
      },
      put: {
        [globalPermissionTestUrl]: Promise.resolve({ data: ['CONFIGURE_SYSTEM'] }),
      },
    });

    const { container } = render(<SuccessMetricsConfigurationContainer />);
    expect(screen.getByRole('heading', { name: /success metrics/i })).toBeVisible();
    expect(screen.queryByRole('checkbox', { name: /enable success metrics/i })).toBeNull();
    expect(screen.queryByText('Loading…')).toBeVisible();

    await waitFor(() => screen.getByRole('heading', { name: /configure success metrics/i }));

    expect(screen.queryByText('Loading…')).toBeNull();
    expect(screen.getByLabelText('Enable Success Metrics')).toBeChecked();

    // query whole container
    expect(container.querySelector('.nx-toggle__input')).toBeChecked();
  });

  it('renders disabled toggle', async () => {
    mockAxiosCalls({
      get: {
        [successMetricsConfigurationUrl]: Promise.resolve({
          data: { enabled: false },
        }),
      },
      put: {
        [globalPermissionTestUrl]: Promise.resolve({ data: ['CONFIGURE_SYSTEM'] }),
      },
    });

    render(<SuccessMetricsConfigurationContainer />);
    await waitFor(() => screen.getByRole('heading', { name: /configure success metrics/i }));

    expect(screen.getByLabelText('Enable Success Metrics')).not.toBeChecked();
  });

  it('handles load error', async () => {
    mockAxiosCalls({
      get: {
        [successMetricsConfigurationUrl]: () => Promise.reject({ status: 403 }),
      },
      put: {
        [globalPermissionTestUrl]: Promise.resolve({ data: ['CONFIGURE_SYSTEM'] }),
      },
    });

    render(<SuccessMetricsConfigurationContainer />);
    await waitFor(() => screen.getByText(/An error occurred loading data/));

    expect(screen.getByRole('button', { name: 'Retry' })).toBeVisible();
  });

  it('toggles and cancels', async () => {
    mockAxiosCalls({
      get: {
        [successMetricsConfigurationUrl]: Promise.resolve({
          data: { enabled: false },
        }),
      },
      put: {
        [globalPermissionTestUrl]: Promise.resolve({ data: ['CONFIGURE_SYSTEM'] }),
      },
    });

    render(<SuccessMetricsConfigurationContainer />);
    await waitFor(() => screen.getByRole('heading', { name: /configure success metrics/i }));
    const toggle = screen.getByLabelText('Enable Success Metrics');

    expect(toggle).not.toBeChecked();
    expect(screen.getByRole('button', { name: 'Cancel' })).toBeDisabled();
    expect(screen.getByRole('alert')).toHaveTextContent(
      'There were validation errors. There are no changes to update.'
    );
    fireEvent.click(toggle);

    expect(toggle).toBeChecked();
    expect(screen.getByRole('button', { name: 'Cancel' })).toBeEnabled();
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: 'Cancel' }));

    expect(toggle).not.toBeChecked();
    expect(screen.getByRole('button', { name: 'Cancel' })).toBeDisabled();
    expect(screen.getByRole('alert')).toHaveTextContent(
      'There were validation errors. There are no changes to update.'
    );
  });

  it('submits updated setting', async () => {
    mockAxiosCalls({
      get: {
        [successMetricsConfigurationUrl]: Promise.resolve({
          data: { enabled: false },
        }),
      },
      put: {
        [globalPermissionTestUrl]: Promise.resolve({ data: ['CONFIGURE_SYSTEM'] }),
        [successMetricsConfigurationUrl]: Promise.resolve({}),
      },
    });

    render(<SuccessMetricsConfigurationContainer />);
    await waitFor(() => screen.getByRole('heading', { name: /configure success metrics/i }));
    fireEvent.click(screen.getByLabelText('Enable Success Metrics'));

    expect(screen.queryByRole('alert')).not.toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: 'Update' }));

    await waitFor(() => screen.getByRole('alert'));

    expect(screen.getByRole('button', { name: 'Cancel' })).toBeDisabled();
    expect(screen.getByRole('alert')).toHaveTextContent(
      'There were validation errors. There are no changes to update.'
    );
    expect(screen.getByLabelText('Enable Success Metrics')).toBeChecked();
  });

  it('handles submit error', async () => {
    mockAxiosCalls({
      get: {
        [successMetricsConfigurationUrl]: Promise.resolve({
          data: { enabled: false },
        }),
      },
      put: {
        [globalPermissionTestUrl]: Promise.resolve({ data: ['CONFIGURE_SYSTEM'] }),
        [successMetricsConfigurationUrl]: () => Promise.reject({ status: 403 }),
      },
    });

    render(<SuccessMetricsConfigurationContainer />);
    await waitFor(() => screen.getByLabelText('Enable Success Metrics'));
    fireEvent.click(screen.getByLabelText('Enable Success Metrics'));
    fireEvent.click(screen.getByRole('button', { name: 'Update' }));

    await waitFor(() => screen.getByText(/An error occurred saving data/));

    expect(screen.getByRole('button', { name: 'Retry' })).toBeEnabled();

    // retry button
    expect(axios.put.calls.count()).toBe(2);
    fireEvent.click(screen.getByRole('button', { name: 'Retry' }));
    expect(axios.put.calls.count()).toBe(3);
  });
});
