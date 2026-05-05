/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { clone } from 'ramda';
import { render, screen, axiosMockAdapter, waitFor } from 'TestRoot/SpecUtil';
import userEvent from '@testing-library/user-event';
import AutoWaiverModal from 'MainRoot/OrgsAndPolicies/autoWaiversConfiguration/AutoWaiverModal';
import { initialState } from 'MainRoot/OrgsAndPolicies/autoWaiversConfiguration/autoWaiverModalSlice';
import { getAutoWaiversConfigurationURL, getAutoWaiversConfigurationURLWaiver } from 'MainRoot/util/CLMLocation';

describe('Auto Waiver Modal Component', () => {
  let axiosMock, renderComponent, user;

  const defaultPreloadedState = {
    orgsAndPolicies: {
      root: {
        selectedOwner: {
          id: 'app',
          publicId: 'publicId',
          name: 'App',
        },
      },
      autoWaivers: {
        autoWaiverModal: {
          ...initialState,
          isModalOpen: true,
        },
      },
    },
    productFeatures: {
      productFeatures: {
        'developer-dashboard': true,
        'auto-waivers': true,
        'auto-waiver-management': true,
      },
    },
  };

  beforeAll(() => {
    axiosMock = axiosMockAdapter();
  });

  beforeEach(() => {
    renderComponent = (preloadedState) => {
      user = userEvent.setup();
      return render(<AutoWaiverModal />, { preloadedState: preloadedState || defaultPreloadedState });
    };
  });

  it('does not render a dialog when model is not open', () => {
    const state = clone(defaultPreloadedState);
    state.orgsAndPolicies.autoWaivers.autoWaiverModal.isModalOpen = false;
    renderComponent(state);

    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });

  it('renders reachability info in tooltip', async () => {
    renderComponent();

    const reachabilityInfoIcon = screen.getByTestId('auto-waiver-modal-reachability-icon');
    expect(reachabilityInfoIcon).toBeInTheDocument();

    user.hover(reachabilityInfoIcon);

    const tooltip = await screen.findByRole('tooltip');
    expect(tooltip).toBeInTheDocument();
    expect(tooltip).toHaveTextContent('Reachability Analysis must be enabled (via Sonatype CLI or CI/CD Integration).');
  });

  describe('create new waiver dialog', () => {
    it('should display a dialog element', () => {
      renderComponent();

      expect(screen.getByRole('dialog')).toBeInTheDocument();
    });

    it('should display with correct text', () => {
      renderComponent();

      expect(screen.getByRole('heading', { level: 2 })).toHaveTextContent('New Auto-Waiver');
      expect(
        screen.getByText('Automatically waive policy violations when the following conditions are met:')
      ).toBeVisible();
    });

    it('should display with correct action buttons', () => {
      renderComponent();

      expect(screen.getByRole('button', { name: 'Cancel' })).toBeVisible();
      expect(screen.getByRole('button', { name: 'Create' })).toBeVisible();
    });

    it('should display content for Threat Level', async () => {
      renderComponent();

      expect(await screen.findByText('Threat Level is equal to or less than')).toBeVisible();
      expect(await screen.findByRole('button', { name: /7 - Severe/i })).toBeVisible();
    });

    describe('scope dropdown', () => {
      it('should be disabled with tooltip and text "any/all" when both checkboxes are not checked', async () => {
        renderComponent();

        expect(await screen.findByText(/And, when /)).toBeVisible();
        expect(await screen.findByText(/ of the following are true:/)).toBeVisible();

        const dropdown = await screen.findByRole('button', { name: 'any/all' });
        expect(dropdown).toBeInTheDocument();
        expect(dropdown).toHaveClass('disabled');

        await user.hover(dropdown);

        const tooltip = await screen.findByRole('tooltip');
        expect(tooltip).toBeInTheDocument();
        expect(tooltip).toHaveTextContent('Select both conditions below to enable this option');
      });

      it('should be enabled with text "any" when both checkboxes are checked', async () => {
        renderComponent();

        let dropdown = await screen.findByRole('button', { name: 'any/all' });
        expect(dropdown).toBeInTheDocument();
        expect(dropdown).toHaveClass('disabled');

        const upgradePathNotAvailableCheckbox = await screen.findByLabelText(
          'No newer, non-violating component version is available'
        );
        const vulnerabilityNotReachableCheckbox = await screen.findByLabelText(
          'Application does not execute any calls to the vulnerable method'
        );

        await user.click(upgradePathNotAvailableCheckbox);
        await user.click(vulnerabilityNotReachableCheckbox);

        expect(screen.queryByRole('button', { name: 'any/all' })).not.toBeInTheDocument();

        dropdown = await screen.findByRole('button', { name: 'any' });
        expect(dropdown).toBeInTheDocument();
        expect(dropdown).not.toHaveClass('disabled');
      });
    });

    it('should display content for Upgrade Path is not available', async () => {
      renderComponent();

      const upgradePathNotAvailableCheckbox = await screen.findByLabelText(
        'No newer, non-violating component version is available'
      );
      expect(upgradePathNotAvailableCheckbox).toBeInTheDocument();
      expect(upgradePathNotAvailableCheckbox).not.toBeChecked();
    });

    it('should display content for Vulnerability is not reachable', async () => {
      renderComponent();

      const vulnerabilityNotReachableCheckbox = await screen.findByLabelText(
        'Application does not execute any calls to the vulnerable method'
      );
      expect(vulnerabilityNotReachableCheckbox).toBeInTheDocument();
      expect(vulnerabilityNotReachableCheckbox).not.toBeChecked();
    });

    it('should display a validation error when save is clicked with no changes', async () => {
      renderComponent();

      user.click(screen.getByRole('button', { name: 'Create' }));

      expect(await screen.findByText(/There were validation errors./)).toBeVisible();
      expect(
        await screen.findByText(
          /Either 'Upgrade Path is not available' or 'Vulnerability is not reachable' is required to be selected./
        )
      ).toBeVisible();
    });

    it('should save when Upgrade Path is checked', async () => {
      renderComponent();

      const upgradePathNotAvailableCheckbox = await screen.findByLabelText(
        'No newer, non-violating component version is available'
      );

      await user.click(upgradePathNotAvailableCheckbox);
      expect(upgradePathNotAvailableCheckbox).toBeChecked();

      await user.click(screen.getByRole('button', { name: 'Create' }));

      // Wait for the API call to complete instead of checking for Submitting text
      await waitFor(() => {
        expect(axiosMock.history.post.length).toBe(1);
        expect(axiosMock.history.post[0].url).toBe('/api/v2/autoPolicyWaivers/application/app');
        expect(axiosMock.history.post[0].data).toBe(
          JSON.stringify({
            reachability: false,
            pathForward: true,
            threatLevel: 7,
            scopesOperatorAny: true,
          })
        );
      });
    });
  });

  describe('edit existing waiver dialog', () => {
    let editModeState;

    beforeEach(() => {
      const waiverData = {
        pathForward: true,
        reachability: true,
        threatLevel: 8,
        scope: 'all',
        isInherited: false,
        autoPolicyWaiverId: 'abc123',
      };

      editModeState = {
        orgsAndPolicies: {
          root: {
            selectedOwner: {
              id: 'app',
              publicId: 'publicId',
              name: 'App',
            },
          },
          autoWaivers: {
            autoWaiverModal: {
              ...initialState,
              isModalOpen: true,
              isEditMode: true,
              data: waiverData,
              serverData: waiverData,
            },
          },
        },
        productFeatures: {
          productFeatures: {
            'developer-dashboard': true,
            'auto-waivers': true,
            'auto-waiver-management': true,
          },
        },
      };
    });

    it('should display a dialog element', () => {
      renderComponent(editModeState);

      expect(screen.getByRole('dialog')).toBeInTheDocument();
    });

    it('should display with correct text', () => {
      renderComponent(editModeState);

      expect(screen.getByRole('heading', { level: 2 })).toHaveTextContent('Edit Auto-Waiver');
      expect(
        screen.getByText('Automatically waive policy violations when the following conditions are met:')
      ).toBeVisible();
    });

    it('should display with correct action buttons', () => {
      renderComponent(editModeState);

      expect(screen.getByRole('button', { name: 'Cancel' })).toBeVisible();
      expect(screen.getByRole('button', { name: 'Update' })).toBeVisible();
    });

    it('should display content for Threat Level', async () => {
      renderComponent(editModeState);

      expect(await screen.findByText('Threat Level is equal to or less than')).toBeVisible();
      expect(await screen.findByRole('button', { name: /8 - Critical/i })).toBeVisible();
    });

    describe('scope dropdown', () => {
      it('should be enabled with text "all" when both checkboxes are checked', async () => {
        renderComponent(editModeState);

        expect(await screen.findByText(/And, when /)).toBeVisible();
        expect(await screen.findByText(/ of the following are true:/)).toBeVisible();
        expect(await screen.findByRole('button', { name: 'all' })).toBeVisible();
      });

      it('should be disabled with tooltip and text "any/all" when both checkboxes are not checked', async () => {
        renderComponent(editModeState);

        expect(await screen.findByRole('button', { name: 'all' })).not.toHaveClass('disabled');

        const upgradePathNotAvailableCheckbox = await screen.findByLabelText(
          'No newer, non-violating component version is available'
        );

        await user.click(upgradePathNotAvailableCheckbox);

        expect(screen.queryByRole('button', { name: 'all' })).not.toBeInTheDocument();

        const dropdown = await screen.findByRole('button', { name: 'any/all' });
        expect(dropdown).toBeInTheDocument();
        expect(dropdown).toHaveClass('disabled');

        await user.hover(dropdown);

        const tooltip = await screen.findByRole('tooltip');
        expect(tooltip).toBeInTheDocument();
        expect(tooltip).toHaveTextContent('Select both conditions below to enable this option');
      });
    });

    it('should display content for Upgrade Path is not available', async () => {
      renderComponent(editModeState);

      const upgradePathNotAvailableCheckbox = await screen.findByLabelText(
        'No newer, non-violating component version is available'
      );
      expect(upgradePathNotAvailableCheckbox).toBeInTheDocument();
      expect(upgradePathNotAvailableCheckbox).toBeChecked();
    });

    it('should display content for Vulnerability is not reachable', async () => {
      renderComponent(editModeState);

      const vulnerabilityNotReachableCheckbox = await screen.findByLabelText(
        'Application does not execute any calls to the vulnerable method'
      );
      expect(vulnerabilityNotReachableCheckbox).toBeInTheDocument();
      expect(vulnerabilityNotReachableCheckbox).toBeChecked();
    });

    it('should display a validation error when save is clicked with no changes', async () => {
      renderComponent(editModeState);

      await user.click(screen.getByRole('button', { name: 'Update' }));

      await waitFor(() => {
        expect(screen.getByText(/There were validation errors./)).toBeVisible();
        expect(screen.getByText(/There are no changes to save./)).toBeVisible();
      });
    });

    it('should save when Upgrade Path is unchecked', async () => {
      const autoWaiverModal = editModeState.orgsAndPolicies.autoWaivers.autoWaiverModal;
      const appId = editModeState.orgsAndPolicies.root.selectedOwner.id;

      axiosMock.onGet(getAutoWaiversConfigurationURL('application', appId)).reply(200, autoWaiverModal.data);

      renderComponent(editModeState);

      const upgradePathNotAvailableCheckbox = await screen.findByLabelText(
        'No newer, non-violating component version is available'
      );

      await user.click(upgradePathNotAvailableCheckbox);
      expect(upgradePathNotAvailableCheckbox).not.toBeChecked();

      await user.click(screen.getByRole('button', { name: 'Update' }));

      // Wait for the API calls to complete instead of checking for Submitting text
      await waitFor(() => {
        expect(axiosMock.history.get.length).toBe(1);
        expect(axiosMock.history.get[0].url).toBe(getAutoWaiversConfigurationURL('application', appId));
      });

      await waitFor(() => {
        expect(axiosMock.history.put.length).toBe(1);
        expect(axiosMock.history.put[0].url).toBe(
          getAutoWaiversConfigurationURLWaiver('application', appId, autoWaiverModal.data.autoPolicyWaiverId)
        );
        expect(axiosMock.history.put[0].data).toBe(
          JSON.stringify({
            threatLevel: 8,
            autoPolicyWaiverId: 'abc123',
            reachability: true,
            pathForward: false,
            scopesOperatorAny: true,
          })
        );
      });
    });

    it('should display a validation error when save is clicked with unchecked Upgrade Path or Vulnerability is not reachable', async () => {
      renderComponent(editModeState);

      const upgradePathNotAvailableCheckbox = await screen.findByLabelText(
        'No newer, non-violating component version is available'
      );

      await user.click(upgradePathNotAvailableCheckbox);
      expect(upgradePathNotAvailableCheckbox).not.toBeChecked();

      const vulnerabilityNotReachableCheckbox = await screen.findByLabelText(
        'Application does not execute any calls to the vulnerable method'
      );
      await user.click(vulnerabilityNotReachableCheckbox);
      expect(vulnerabilityNotReachableCheckbox).not.toBeChecked();

      await user.click(screen.getByRole('button', { name: 'Update' }));

      expect(await screen.findByText(/There were validation errors./)).toBeVisible();
      expect(
        await screen.findByText(
          /Either 'Upgrade Path is not available' or 'Vulnerability is not reachable' is required to be selected./
        )
      ).toBeVisible();
    });
  });
});
