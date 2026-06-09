/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import userEvent from '@testing-library/user-event';
import { axiosMockAdapter, render, screen, waitFor } from 'TestRoot/SpecUtil';
import FirewallProxyConfigurationPage from 'MainRoot/firewall/iqProxy/FirewallProxyConfigurationPage';
import { getAddRepositoryUrl } from 'MainRoot/util/CLMLocation';
import * as routerSelectors from 'MainRoot/reduxUiRouter/routerSelectors';
import { actions as toastActions } from 'MainRoot/toastContainer/toastSlice';

const MANAGER_ID = 'test-manager-id';
const MANAGER_INSTANCE_ID = 'test-instance-id-1234';

describe('FirewallProxyConfigurationPage', () => {
  let axiosMock, addToastSpy;

  const defaultPreloadedState = {
    firewallIqProxy: {
      saving: false,
      saveError: null,
    },
    orgsAndPolicies: {
      root: {
        selectedOwner: {
          id: MANAGER_ID,
          instanceId: MANAGER_INSTANCE_ID,
          name: 'Test Manager',
        },
      },
    },
  };

  beforeEach(() => {
    axiosMock = axiosMockAdapter();
    jest.spyOn(routerSelectors, 'selectRepositoryManagerId').mockReturnValue(MANAGER_ID);
    addToastSpy = jest.spyOn(toastActions, 'addToast');
    axiosMock.onPost(getAddRepositoryUrl(MANAGER_ID)).reply(201, {
      repositoryId: 'generated-repo-id',
      publicId: 'my-repo',
      proxyUrl: `http://localhost/api/v2/proxy/${MANAGER_INSTANCE_ID}/my-repo`,
    });
  });

  afterEach(() => {
    jest.clearAllMocks();
  });

  const renderComponent = (preloadedState) =>
    render(<FirewallProxyConfigurationPage />, {
      preloadedState: preloadedState || defaultPreloadedState,
    });

  describe('page rendering', () => {
    it('renders the page title', () => {
      renderComponent();

      expect(screen.getByText('IQ Proxy')).toBeInTheDocument();
    });

    it('renders the form fields', () => {
      renderComponent();

      expect(screen.getByPlaceholderText('Repository name')).toBeInTheDocument();
      expect(screen.getByText('Select format')).toBeInTheDocument();
      expect(screen.getByPlaceholderText('Upstream repository URL')).toBeInTheDocument();
    });

    it('renders the Save button', () => {
      renderComponent();

      expect(screen.getByRole('button', { name: 'Save' })).toBeInTheDocument();
    });

    it('renders form fields without standalone page title when embedded prop is true', () => {
      render(<FirewallProxyConfigurationPage embedded />, {
        preloadedState: defaultPreloadedState,
      });

      expect(screen.queryByText('IQ Proxy')).not.toBeInTheDocument();
      expect(screen.getByPlaceholderText('Repository name')).toBeInTheDocument();
    });
  });

  describe('form validation', () => {
    it('dispatches error toast when name is empty and Save is clicked', async () => {
      const user = userEvent.setup();
      renderComponent();

      await user.click(screen.getByRole('button', { name: 'Save' }));

      expect(addToastSpy).toHaveBeenCalledWith({ type: 'error', message: 'Repository name is required.' });
    });

    it('dispatches error toast when format is not selected and Save is clicked', async () => {
      const user = userEvent.setup();
      renderComponent();

      await user.type(screen.getByPlaceholderText('Repository name'), 'my-repo');
      await user.click(screen.getByRole('button', { name: 'Save' }));

      expect(addToastSpy).toHaveBeenCalledWith({ type: 'error', message: 'Repository format is required.' });
    });

    it('dispatches error toast when upstream URL is empty and Save is clicked', async () => {
      const user = userEvent.setup();
      renderComponent();

      await user.type(screen.getByPlaceholderText('Repository name'), 'my-repo');
      await user.selectOptions(screen.getByRole('combobox'), 'maven2');
      await user.click(screen.getByRole('button', { name: 'Save' }));

      expect(addToastSpy).toHaveBeenCalledWith({ type: 'error', message: 'Upstream URL is required.' });
    });
  });

  describe('format selection', () => {
    it('shows upstream URL hint when a format is selected', async () => {
      const user = userEvent.setup();
      renderComponent();

      await user.selectOptions(screen.getByRole('combobox'), 'maven2');

      expect(screen.getByText(/https:\/\/repo1\.maven\.org\/maven2\//)).toBeInTheDocument();
    });

    it('does not show upstream URL hint before format is selected', () => {
      renderComponent();

      expect(screen.queryByText(/https:\/\/repo1\.maven\.org\/maven2\//)).not.toBeInTheDocument();
    });
  });

  describe('save button state', () => {
    it('disables Save button while saving', () => {
      renderComponent({
        ...defaultPreloadedState,
        firewallIqProxy: { saving: true, saveError: null },
      });

      expect(screen.getByRole('button', { name: 'Saving…' })).toBeDisabled();
    });
  });

  describe('successful save', () => {
    it('shows success modal after successful save', async () => {
      const user = userEvent.setup();
      renderComponent();

      await user.type(screen.getByPlaceholderText('Repository name'), 'my-repo');
      await user.selectOptions(screen.getByRole('combobox'), 'npm');
      await user.type(screen.getByPlaceholderText('Upstream repository URL'), 'https://registry.npmjs.org');
      await user.click(screen.getByRole('button', { name: 'Save' }));

      await waitFor(() => {
        expect(screen.getByText('Repository Created')).toBeInTheDocument();
      });

      expect(screen.getByText('Repository created successfully.')).toBeInTheDocument();
    });

    it('shows the correct proxy URL in the success modal', async () => {
      const user = userEvent.setup();
      renderComponent();

      await user.type(screen.getByPlaceholderText('Repository name'), 'my-repo');
      await user.selectOptions(screen.getByRole('combobox'), 'npm');
      await user.type(screen.getByPlaceholderText('Upstream repository URL'), 'https://registry.npmjs.org');
      await user.click(screen.getByRole('button', { name: 'Save' }));

      await waitFor(() => {
        expect(screen.getByText('Repository Created')).toBeInTheDocument();
      });

      expect(screen.getByText(`http://localhost/api/v2/proxy/${MANAGER_INSTANCE_ID}/my-repo`)).toBeInTheDocument();
    });

    it('clears form fields after successful save', async () => {
      const user = userEvent.setup();
      renderComponent();

      const nameInput = screen.getByPlaceholderText('Repository name');
      await user.type(nameInput, 'my-repo');
      await user.selectOptions(screen.getByRole('combobox'), 'npm');
      await user.type(screen.getByPlaceholderText('Upstream repository URL'), 'https://registry.npmjs.org');
      await user.click(screen.getByRole('button', { name: 'Save' }));

      await waitFor(() => {
        expect(screen.getByText('Repository Created')).toBeInTheDocument();
      });

      await user.click(screen.getByRole('button', { name: 'Close' }));

      expect(nameInput.value).toBe('');
    });

    it('closes success modal when Close is clicked', async () => {
      const user = userEvent.setup();
      renderComponent();

      await user.type(screen.getByPlaceholderText('Repository name'), 'my-repo');
      await user.selectOptions(screen.getByRole('combobox'), 'npm');
      await user.type(screen.getByPlaceholderText('Upstream repository URL'), 'https://registry.npmjs.org');
      await user.click(screen.getByRole('button', { name: 'Save' }));

      await waitFor(() => {
        expect(screen.getByText('Repository Created')).toBeInTheDocument();
      });

      await user.click(screen.getByRole('button', { name: 'Close' }));

      expect(screen.queryByText('Repository Created')).not.toBeInTheDocument();
    });

    it('posts to the correct URL with the right payload', async () => {
      const user = userEvent.setup();
      renderComponent();

      await user.type(screen.getByPlaceholderText('Repository name'), 'my-repo');
      await user.selectOptions(screen.getByRole('combobox'), 'maven2');
      await user.type(screen.getByPlaceholderText('Upstream repository URL'), 'https://repo1.maven.org/maven2/');
      await user.click(screen.getByRole('button', { name: 'Save' }));

      await waitFor(() => {
        expect(axiosMock.history.post.length).toBe(1);
      });

      const postRequest = axiosMock.history.post[0];
      expect(postRequest.url).toBe(getAddRepositoryUrl(MANAGER_ID));
      expect(JSON.parse(postRequest.data)).toEqual({ publicId: 'my-repo', format: 'maven2', upstreamUrl: 'https://repo1.maven.org/maven2/' });
    });
  });

  describe('copy proxy URL', () => {
    const openSuccessModal = async (user) => {
      await user.type(screen.getByPlaceholderText('Repository name'), 'my-repo');
      await user.selectOptions(screen.getByRole('combobox'), 'npm');
      await user.type(screen.getByPlaceholderText('Upstream repository URL'), 'https://registry.npmjs.org');
      await user.click(screen.getByRole('button', { name: 'Save' }));
      await waitFor(() => expect(screen.getByText('Repository Created')).toBeInTheDocument());
    };

    const stubClipboard = (writeText) => {
      Object.defineProperty(navigator, 'clipboard', {
        value: { writeText },
        configurable: true,
      });
    };

    it('writes the proxy URL to the clipboard and shows "Copied!" on success', async () => {
      const user = userEvent.setup();
      const writeText = jest.fn().mockResolvedValue(undefined);
      stubClipboard(writeText);
      renderComponent();
      await openSuccessModal(user);

      await user.click(screen.getByRole('button', { name: 'Copy URL' }));

      await waitFor(() => {
        expect(writeText).toHaveBeenCalledWith(`http://localhost/api/v2/proxy/${MANAGER_INSTANCE_ID}/my-repo`);
        expect(screen.getByRole('button', { name: 'Copied!' })).toBeInTheDocument();
      });
    });

    it('dispatches an error toast and keeps "Copy URL" label when clipboard write fails', async () => {
      const user = userEvent.setup();
      const writeText = jest.fn().mockRejectedValue(new Error('NotAllowedError'));
      stubClipboard(writeText);
      renderComponent();
      await openSuccessModal(user);

      await user.click(screen.getByRole('button', { name: 'Copy URL' }));

      await waitFor(() => {
        expect(addToastSpy).toHaveBeenCalledWith({ type: 'error', message: 'Unable to copy URL to clipboard.' });
      });
      expect(writeText).toHaveBeenCalled();
      expect(screen.queryByRole('button', { name: 'Copied!' })).not.toBeInTheDocument();
      expect(screen.getByRole('button', { name: 'Copy URL' })).toBeInTheDocument();
    });
  });

  describe('save error handling', () => {
    it('dispatches error toast when saveError is set in state', () => {
      // The error-toast-on-failure flow is: slice sets saveError → useEffect fires → addToast dispatched.
      // We test this by rendering with saveError already set (as if the async thunk already rejected).
      renderComponent({
        ...defaultPreloadedState,
        firewallIqProxy: { saving: false, saveError: 'Repository already exists', saveErrorId: 1 },
      });

      expect(addToastSpy).toHaveBeenCalledWith({ type: 'error', message: 'Repository already exists' });
    });
  });
});
