/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';

import { render, waitFor, fireEvent, screen, axiosMockAdapter, within } from '../../SpecUtil';
import SuccessMetricsConfiguration from 'MainRoot/configuration/successMetricsConfiguration/SuccessMetricsConfiguration';
import { getSuccessMetricsConfigUrl } from 'MainRoot/util/CLMLocation';
import { getGlobalPermissionTestUrl } from 'MainRoot/util/CLMContextLocation';

describe('SuccessMetricsConfigurationSpec', () => {
  const selectValidationErrorVisibility = 'form.nx-form--show-validation-errors';

  const successMetricsConfigurationUrl = getSuccessMetricsConfigUrl();
  const globalPermissionTestUrl = getGlobalPermissionTestUrl();

  let axiosMock;

  beforeEach(() => {
    axiosMock = axiosMockAdapter();

    // given user has permissions to configure the system
    axiosMock.onPut(globalPermissionTestUrl).reply(200, ['CONFIGURE_SYSTEM']);
  });

  it('renders enabled toggle', async () => {
    axiosMock.onGet(successMetricsConfigurationUrl).reply(200, { enabled: true });

    const { container } = renderComponent();

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
    axiosMock.onGet(successMetricsConfigurationUrl).reply(200, { enabled: false });

    renderComponent();
    await waitFor(() => screen.getByRole('heading', { name: /configure success metrics/i }));

    expect(screen.getByLabelText('Enable Success Metrics')).not.toBeChecked();
  });

  it('handles load error fetching success metrics configuration', async () => {
    axiosMock.onGet(successMetricsConfigurationUrl).reply(403);

    renderComponent();
    await waitFor(() => screen.getByText(/An error occurred loading data/));

    expect(screen.getByRole('button', { name: 'Retry' })).toBeVisible();
  });

  it('handles load error fetching permissions', async () => {
    axiosMock.onGet(successMetricsConfigurationUrl).reply(200, { enabled: false });

    // given permission check experienced an error
    axiosMock.onPut(globalPermissionTestUrl).reply(403);

    renderComponent();

    screen.debug(null, Infinity);
    await waitFor(() => screen.getByText(/An error occurred loading data/));

    expect(screen.getByRole('button', { name: 'Retry' })).toBeVisible();
  });

  it('handles instance where user does not have permissions', async () => {
    axiosMock.onGet(successMetricsConfigurationUrl).reply(200, { enabled: false });

    // given permission check succeeded but the user does not have permission
    axiosMock.onPut(globalPermissionTestUrl).reply(200, []);

    renderComponent();
    await waitFor(() => screen.getByText(/An error occurred loading data/));

    expect(screen.getByRole('button', { name: 'Retry' })).toBeVisible();
  });

  it('toggles and cancels', async () => {
    axiosMock.onGet(successMetricsConfigurationUrl).reply(200, { enabled: false });

    renderComponent();
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
    axiosMock.onGet(successMetricsConfigurationUrl).reply(200, { enabled: false });
    axiosMock.onPut(successMetricsConfigurationUrl).reply(200, {});

    renderComponent();
    await waitFor(() => screen.getByRole('heading', { name: /configure success metrics/i }));
    fireEvent.click(screen.getByLabelText('Enable Success Metrics'));

    expect(screen.queryByRole('alert')).not.toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: 'Update' }));

    await waitFor(() => expect(axiosMock.history.put.length).toEqual(2));
    expect(axiosMock.history.put.length).toEqual(2);
    expect(axiosMock.history.put[1].url).toEqual(successMetricsConfigurationUrl);
    expect(axiosMock.history.put[1].data).toEqual('{"enabled":true}');

    // check that a success mask is shown for the update event
    expect(await screen.queryByText('Success!')).toBeVisible();

    await assertAlertNotVisible();

    expect(screen.getByRole('button', { name: 'Cancel' })).toBeDisabled();
  });

  it('submits on pristine document shows validation error', async () => {
    axiosMock.onGet(successMetricsConfigurationUrl).reply(200, { enabled: false });
    axiosMock.onPut(successMetricsConfigurationUrl).reply(200, {});

    renderComponent();
    await waitFor(() =>
      expect(screen.getByRole('heading', { name: /configure success metrics/i })).toBeInTheDocument()
    );

    await assertAlertNotVisible();

    fireEvent.click(screen.getByRole('button', { name: 'Update' }));

    await assertAlertToBeVisibleVisible('There were validation errors. There are no changes to update.');
  });

  it('handles submit error', async () => {
    axiosMock.onGet(successMetricsConfigurationUrl).reply(200, { enabled: false });
    axiosMock.onPut(successMetricsConfigurationUrl).reply(403);

    renderComponent();
    await waitFor(() => screen.getByLabelText('Enable Success Metrics'));
    fireEvent.click(screen.getByLabelText('Enable Success Metrics'));
    fireEvent.click(screen.getByRole('button', { name: 'Update' }));

    await waitFor(() => screen.getByText(/An error occurred saving data/));

    expect(screen.getByRole('button', { name: 'Retry' })).toBeEnabled();

    // retry button
    expect(axiosMock.history.put.length).toBe(2);
    fireEvent.click(screen.getByRole('button', { name: 'Retry' }));
    expect(axiosMock.history.put.length).toBe(3);
  });

  it('displays warning alert if the feature is not supported by the product license', async () => {
    axiosMock.onGet(successMetricsConfigurationUrl).reply(200, { enabled: true });

    renderComponent({
      productFeatures: {
        productFeatures: {
          'orgs-and-apps': false, // isOrgsAndAppsEnabled is false
        },
        loading: false, // isProductFeaturesLoading is false
      },
    });

    expect(await screen.queryByRole('heading', { name: /success metrics/i })).toBeVisible();

    await waitFor(() => expect(screen.queryByText('Loading…')).toBeNull());

    const alert = await screen.getByRole('alert');
    expect(within(alert).getByText('This feature is not supported by your product license.')).toBeVisible();
  });

  function renderComponent(preloadStateOverrides = {}) {
    const preloadedState = {
      ...givenOrgsAndAppsEnabled(),
      ...preloadStateOverrides,
    };

    return render(<SuccessMetricsConfiguration />, { preloadedState });
  }

  function givenOrgsAndAppsEnabled() {
    return {
      productFeatures: {
        productFeatures: {
          'orgs-and-apps': true,
        },
        loading: false,
      },
    };
  }

  // The validation alerts are in the dom, regardless but are hidden by css rules.
  // The closest we can come to testing if validation errors are shown is to check for the presence
  // of the nx-form--show-validation-errors on the form
  async function assertAlertNotVisible() {
    await waitFor(() => expect(screen.getByRole('alert')).toBeInTheDocument());
    screen.debug();
    expect(screen.getByRole('main').querySelector(selectValidationErrorVisibility)).not.toBeInTheDocument();
  }

  async function assertAlertToBeVisibleVisible(alertText) {
    await waitFor(() => expect(screen.getByRole('alert')).toBeInTheDocument());
    const alert = screen.getByRole('alert');

    expect(screen.getByRole('main').querySelector(selectValidationErrorVisibility)).toBeInTheDocument();

    expect(within(alert).queryByText(alertText)).toBeInTheDocument();
  }
});
