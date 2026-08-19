/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { Theme } from '@radix-ui/themes';
import { screen, waitFor } from '@testing-library/dom';
import { axiosMockAdapter, render, userEvent } from 'TestRoot/SpecUtil';
import PreviewUiSettingsPage from 'MainRoot/nosc/settings/PreviewUiSettingsPage';
import { authErrorMessage } from 'MainRoot/util/authorizationUtil';
import { getProductFeaturesUrl, getConfigFeatureUrl } from 'MainRoot/util/CLMLocation';

describe('PreviewUiSettingsPage', () => {
  let axiosMock;

  beforeAll(() => {
    axiosMock = axiosMockAdapter();
  });

  afterEach(() => {
    jest.restoreAllMocks();
    axiosMock.reset();
  });

  // The page's <Switch> components need a Radix <Theme> ancestor; Classic IQ
  // doesn't wrap admin pages in <Theme>, so the page renders inside its own
  // (real production behavior is that the route container provides it).
  const renderPage = (preloadedState = {}, { isAuthorized = true } = {}) =>
    render(
      <Theme appearance="light" accentColor="blue" grayColor="slate" radius="medium" scaling="100%">
        <PreviewUiSettingsPage isAuthorized={isAuthorized} />
      </Theme>,
      { preloadedState },
    );

  // Helper: build a Redux state with the given list of enabled feature
  // kebab-case keys already loaded.
  const stateWithFeatures = (enabledKebabFeatures) => ({
    productFeatures: {
      loading: false,
      loadError: null,
      productFeatures: enabledKebabFeatures.reduce((acc, key) => {
        acc[key] = true;
        return acc;
      }, {}),
    },
  });

  it('shows authorization message when isAuthorized is false', () => {
    renderPage({}, { isAuthorized: false });
    const unauthorized = screen.getByTestId('preview-ui-settings-unauthorized');
    expect(unauthorized.textContent?.replace(/\s+/g, ' ').trim()).toBe(
      authErrorMessage.replace(/\s+/g, ' ').trim(),
    );
    expect(screen.queryByTestId('preview-ui-settings-page')).not.toBeInTheDocument();
    expect(axiosMock.history.get).toHaveLength(0);
  });

  it('renders the page heading and both Card sections', async () => {
    axiosMock.onGet(getProductFeaturesUrl()).reply(200, []);
    renderPage(stateWithFeatures([]));

    expect(await screen.findByRole('heading', { name: /Preview — Nexus One UI Settings/i })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: /Access Control/i })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: /Rollout/i })).toBeInTheDocument();
  });

  it('renders all 4 toggles in OFF state when no features are enabled', async () => {
    axiosMock.onGet(getProductFeaturesUrl()).reply(200, []);
    renderPage(stateWithFeatures([]));

    await screen.findByRole('heading', { name: /Preview — Nexus One UI Settings/i });

    const toggleIds = [
      'preview-ui-toggle-preview-nexus-one-ui-anonymous-enabled',
      'preview-ui-toggle-preview-nexus-one-ui-loggedin-enabled',
      'preview-ui-toggle-preview-nexus-one-ui-default-to-preview',
      'preview-ui-toggle-preview-nexus-one-ui-disable-switch-feedback',
    ];

    for (const testId of toggleIds) {
      const toggle = screen.getByTestId(testId);
      expect(toggle).toBeInTheDocument();
      // Radix Switch uses aria-checked or data-state for state
      expect(toggle.getAttribute('data-state')).toBe('unchecked');
    }
  });

  it('renders a toggle in ON state when its feature is in productFeatures', async () => {
    axiosMock.onGet(getProductFeaturesUrl()).reply(200, ['preview-nexus-one-ui-loggedin-enabled']);
    renderPage(stateWithFeatures(['preview-nexus-one-ui-loggedin-enabled']));

    await screen.findByRole('heading', { name: /Preview — Nexus One UI Settings/i });

    const onToggle = screen.getByTestId('preview-ui-toggle-preview-nexus-one-ui-loggedin-enabled');
    expect(onToggle.getAttribute('data-state')).toBe('checked');

    const offToggle = screen.getByTestId('preview-ui-toggle-preview-nexus-one-ui-anonymous-enabled');
    expect(offToggle.getAttribute('data-state')).toBe('unchecked');
  });

  it('POSTs to /api/v2/config/features/{ENUM_NAME} when a toggle is flipped ON', async () => {
    let featuresFetchCount = 0;
    axiosMock.onGet(getProductFeaturesUrl()).reply(() => {
      featuresFetchCount += 1;
      return [
        200,
        featuresFetchCount === 1 ? [] : ['preview-nexus-one-ui-loggedin-enabled'],
      ];
    });
    const expectedUrl = getConfigFeatureUrl('PREVIEW_NEXUS_ONE_UI_LOGGEDIN_ENABLED');
    axiosMock.onPost(expectedUrl).reply(204);

    renderPage(stateWithFeatures([]));

    await screen.findByRole('heading', { name: /Preview — Nexus One UI Settings/i });
    const toggle = screen.getByTestId('preview-ui-toggle-preview-nexus-one-ui-loggedin-enabled');
    expect(toggle.getAttribute('data-state')).toBe('unchecked');
    await userEvent.click(toggle);

    await waitFor(() => {
      const postRequests = axiosMock.history.post.filter((req) => req.url === expectedUrl);
      expect(postRequests).toHaveLength(1);
      expect(toggle.getAttribute('data-state')).toBe('checked');
    });
  });

  it('DELETEs to /api/v2/config/features/{ENUM_NAME} when a toggle is flipped OFF', async () => {
    axiosMock.onGet(getProductFeaturesUrl()).reply(200, ['preview-nexus-one-ui-loggedin-enabled']);
    const expectedUrl = getConfigFeatureUrl('PREVIEW_NEXUS_ONE_UI_LOGGEDIN_ENABLED');
    axiosMock.onDelete(expectedUrl).reply(204);

    renderPage(stateWithFeatures(['preview-nexus-one-ui-loggedin-enabled']));

    await screen.findByRole('heading', { name: /Preview — Nexus One UI Settings/i });
    await userEvent.click(screen.getByTestId('preview-ui-toggle-preview-nexus-one-ui-loggedin-enabled'));

    await waitFor(() => {
      const deleteRequests = axiosMock.history.delete.filter((req) => req.url === expectedUrl);
      expect(deleteRequests).toHaveLength(1);
    });
  });

  it('shows a save error inline when the toggle write fails', async () => {
    axiosMock.onGet(getProductFeaturesUrl()).reply(200, []);
    const expectedUrl = getConfigFeatureUrl('PREVIEW_NEXUS_ONE_UI_LOGGEDIN_ENABLED');
    axiosMock.onPost(expectedUrl).reply(500, { message: 'Boom' });

    renderPage(stateWithFeatures([]));

    await screen.findByRole('heading', { name: /Preview — Nexus One UI Settings/i });
    await userEvent.click(screen.getByTestId('preview-ui-toggle-preview-nexus-one-ui-loggedin-enabled'));

    await waitFor(() => {
      expect(screen.getByTestId('preview-ui-settings-save-error')).toBeInTheDocument();
    });
  });

  // Real backend (JaxRsExceptionMapper) returns plain-text error bodies, not
  // JSON. Exercise BOTH shapes to be defensive against future endpoints that
  // do emit JSON envelopes.
  it.each([
    ['plain-text (real JaxRsExceptionMapper)', 'Feature is already enabled.'],
    ['JSON envelope (defensive)', { message: 'Feature is already enabled.' }],
  ])(
    'silently re-syncs (no error banner) when server says already-enabled — %s',
    async (_label, body) => {
      const expectedUrl = getConfigFeatureUrl('PREVIEW_NEXUS_ONE_UI_LOGGEDIN_ENABLED');
      axiosMock.onGet(getProductFeaturesUrl()).reply(200, []);
      axiosMock.onPost(expectedUrl).reply(400, body);

      renderPage(stateWithFeatures([]));
      await screen.findByRole('heading', { name: /Preview — Nexus One UI Settings/i });
      await userEvent.click(screen.getByTestId('preview-ui-toggle-preview-nexus-one-ui-loggedin-enabled'));

      await waitFor(() => {
        expect(axiosMock.history.post.filter((r) => r.url === expectedUrl)).toHaveLength(1);
      });

      expect(screen.queryByTestId('preview-ui-settings-save-error')).not.toBeInTheDocument();
    },
  );

  it('keeps settings visible when background refresh fails after a successful load', async () => {
    let featuresFetchCount = 0;
    axiosMock.onGet(getProductFeaturesUrl()).reply(() => {
      featuresFetchCount += 1;
      return featuresFetchCount === 1 ? [200, []] : [500, 'Refresh failed'];
    });
    const expectedUrl = getConfigFeatureUrl('PREVIEW_NEXUS_ONE_UI_LOGGEDIN_ENABLED');
    axiosMock.onPost(expectedUrl).reply(204);

    renderPage(stateWithFeatures([]));
    await screen.findByRole('heading', { name: /Preview — Nexus One UI Settings/i });
    await userEvent.click(screen.getByTestId('preview-ui-toggle-preview-nexus-one-ui-loggedin-enabled'));

    await waitFor(() => {
      expect(screen.getByTestId('preview-ui-settings-page')).toBeInTheDocument();
      expect(screen.queryByTestId('preview-ui-settings-error')).not.toBeInTheDocument();
      expect(screen.getByTestId('preview-ui-settings-refresh-error')).toBeInTheDocument();
    });
  });

  it('shows the load-error state when the features fetch fails', async () => {
    // Empty productFeatures triggers the fetch on mount; mocked 500 rejects
    // the thunk; reducer sets loadError; page renders the error box.
    axiosMock.onGet(getProductFeaturesUrl()).reply(500);
    renderPage(stateWithFeatures([]));

    const errorBox = await screen.findByTestId('preview-ui-settings-error');
    expect(errorBox).toBeInTheDocument();
    // Don't assert on the exact error text \u2014 it's whatever
    // Messages.getHttpErrorMessage produces for an unmocked 500. The presence
    // of the error box (data-testid) is sufficient.
  });
});
