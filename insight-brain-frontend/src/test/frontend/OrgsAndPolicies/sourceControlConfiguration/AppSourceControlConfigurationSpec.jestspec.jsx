/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { axiosMockAdapter, render, screen } from 'TestRoot/SpecUtil';
import {
  getCompositeSourceControlUrl,
  getSourceControlMetricsUrl,
  getValidateScmConfigButtonUrl,
} from 'MainRoot/util/CLMLocation';
import { mergeDeepRight } from 'ramda';
import React from 'react';
import AppSourceControlConfiguration from 'MainRoot/OrgsAndPolicies/sourceControlConfiguration/AppSourceControlConfiguration';
import { defaultAppConfigResponse } from 'TestRoot/OrgsAndPolicies/sourceControlConfiguration/data';
import { fireEvent } from '@testing-library/react';

describe('AppSourceControlConfiguration', () => {
  let renderComponent, axiosMock;

  beforeAll(() => {
    axiosMock = axiosMockAdapter();
  });

  let defaultPreloadedState;
  beforeEach(() => {
    const ownerType = 'application';
    const ownerId = '0006b1bf904e45999ee1b4eb05d898fd';

    defaultPreloadedState = {
      router: {
        currentState: {
          name: 'management.edit.application.edit-source-control',
        },
        currentParams: {
          applicationPublicId: 'vulnerable-java-app',
        },
      },
      productFeatures: {
        productFeatures: {
          notifications: true,
          automation: true,
        },
      },
      orgsAndPolicies: {
        root: {
          selectedOwner: {
            id: ownerId,
            publicId: 'vulnerable-java-app',
            name: 'Vulnerable java app',
          },
        },
        sourceControlConfiguration: {
          scmConfigValidation: {
            result: undefined,
            error: null,
            loading: false,
          },
          isDirty: false,
        },
      },
    };

    const validResponse = {
      valid: true,
      message: null,
    };
    const configuredTestResponse = {
      configurationComplete: validResponse,
      repoPrivate: validResponse,
      tokenPermissions: validResponse,
      sshConfiguration: validResponse,
    };

    renderComponent = (preloadedState = {}) =>
      render(
        <>
          <AppSourceControlConfiguration />
        </>,
        { preloadedState: mergeDeepRight(defaultPreloadedState, preloadedState) }
      );

    axiosMock.onGet(getCompositeSourceControlUrl(ownerType, ownerId)).reply(200, defaultAppConfigResponse);
    axiosMock.onGet(getSourceControlMetricsUrl(ownerType, ownerId)).reply(200, { results: [] });
    axiosMock.onGet(getValidateScmConfigButtonUrl(ownerType, ownerId)).reply(200, configuredTestResponse);
  });

  describe('renders a TestConfiguration button', () => {
    it('it is disabled when the test configuration operation is loading', async () => {
      renderComponent();

      const testConfigurationButton = screen.getByRole('button', { name: 'Test Configuration' });
      expect(testConfigurationButton).toBeEnabled();
      fireEvent.click(testConfigurationButton);
      expect(testConfigurationButton).not.toBeEnabled();

      expect(await screen.findByText('Configuration Test Results')).toBeVisible();
      expect(testConfigurationButton).toBeEnabled();
    });

    it('it is disabled when the form is dirty', () => {
      defaultPreloadedState.orgsAndPolicies.sourceControlConfiguration.isDirty = true;
      renderComponent(defaultPreloadedState);

      const testConfigurationButton = screen.getByRole('button', { name: 'Test Configuration' });
      expect(testConfigurationButton).not.toBeEnabled();
    });
  });

  describe('Token field visibility with GitHub App feature', () => {
    beforeEach(() => {
      // Reset axios mocks before each test to ensure clean state
      axiosMock.reset();
      const ownerType = 'application';
      const ownerId = '0006b1bf904e45999ee1b4eb05d898fd';
      const validResponse = {
        valid: true,
        message: null,
      };
      const configuredTestResponse = {
        configurationComplete: validResponse,
        repoPrivate: validResponse,
        tokenPermissions: validResponse,
        sshConfiguration: validResponse,
      };
      axiosMock.onGet(getSourceControlMetricsUrl(ownerType, ownerId)).reply(200, { results: [] });
      axiosMock.onGet(getValidateScmConfigButtonUrl(ownerType, ownerId)).reply(200, configuredTestResponse);
    });

    it('hides token field when feature enabled and no provider selected (neither inherited nor local)', async () => {
      const preloadedState = {
        productFeatures: {
          productFeatures: {
            notifications: true,
            automation: true,
            'github-app-authentication': true,
          },
        },
      };

      const configResponse = {
        ...defaultAppConfigResponse,
        provider: { value: null, parentValue: null, parentName: null },
      };

      axiosMock
        .onGet(getCompositeSourceControlUrl('application', '0006b1bf904e45999ee1b4eb05d898fd'))
        .reply(200, configResponse);

      renderComponent(preloadedState);

      // Wait for form to load by checking for Update button
      await screen.findByRole('button', { name: 'Update' });

      // Token field should be hidden
      expect(screen.queryByTestId('token-input')).not.toBeInTheDocument();
      // GitHub App auth should also be hidden (no provider selected)
      expect(screen.queryByText(/GitHub App/)).not.toBeInTheDocument();
    });
  });
});
