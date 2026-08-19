/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { axiosMockAdapter, render, screen, userEvent } from 'TestRoot/SpecUtil';
import { mergeDeepRight } from 'ramda';
import {
  getCompositeSourceControlUrl,
  getOrganizationUrl,
  getRelayWebhookSecret,
  getRelayWebhookUrl,
  getSourceControlMetricsUrl,
} from 'MainRoot/util/CLMLocation';
import SourceControlConfiguration from 'MainRoot/OrgsAndPolicies/sourceControlConfiguration/SourceControlConfiguration';
import {
  ORGANIZATION_ID,
  ROOT_ORGANIZATION_ID,
  ROOT_ORGANIZATION_NAME,
  defaultAppConfigResponse,
  defaultOrgConfigResponse,
  defaultRootOrgConfigResponse,
  rootOrganizationResponse,
} from './data';

const RELAY_URL = 'https://relay.example.com/webhook/abc-123/github';
const RELAY_SECRET = 'whsec_abc123';
const APP_OWNER_ID = '0006b1bf904e45999ee1b4eb05d898fd';

describe('Relay Webhook URL field', () => {
  let axiosMock;

  beforeAll(() => {
    axiosMock = axiosMockAdapter();
  });

  describe('Root organization', () => {
    const ownerType = 'organization';
    const ownerId = ROOT_ORGANIZATION_ID;

    const baseState = {
      router: {
        currentParams: { organizationId: ROOT_ORGANIZATION_ID },
        currentState: { name: 'organization' },
      },
      productFeatures: {
        productFeatures: { notifications: true, automation: true, 'saas-lifecycle-scm-prs-enabled': true },
      },
      orgsAndPolicies: {
        root: { selectedOwner: { id: ROOT_ORGANIZATION_ID, name: ROOT_ORGANIZATION_NAME } },
      },
    };

    beforeEach(() => {
      axiosMock.onGet(getOrganizationUrl(ROOT_ORGANIZATION_ID)).reply(200, rootOrganizationResponse);
      axiosMock.onGet(getCompositeSourceControlUrl(ownerType, ownerId)).reply(200, defaultRootOrgConfigResponse);
      axiosMock.onGet(getSourceControlMetricsUrl(ownerType, ownerId)).reply(200, { results: [] });
      // Default: relay URL not available; tests that need it override below.
      axiosMock.onGet(getRelayWebhookUrl()).reply(404);
      axiosMock.onGet(getRelayWebhookSecret()).reply(404);
    });

    const renderRoot = (extra = {}) =>
      render(<SourceControlConfiguration />, { preloadedState: mergeDeepRight(baseState, extra) });

    it('renders the read-only field and copy button when the API returns a URL', async () => {
      axiosMock.onGet(getCompositeSourceControlUrl(ownerType, ownerId)).reply(200, {
        ...defaultRootOrgConfigResponse,
        provider: { value: 'github', parentValue: null, parentName: null },
      });
      axiosMock.onGet(getRelayWebhookUrl()).reply(200, { webhookUrl: RELAY_URL });

      renderRoot();

      await screen.findByText('Relay Webhook URL');
      const wrapper = document.getElementById('source-control-relay-webhook-url');
      expect(wrapper).toBeVisible();
      const textarea = wrapper.querySelector('textarea');
      expect(textarea).toHaveValue(RELAY_URL);
      expect(textarea).toHaveAttribute('readonly');
      expect(screen.getByRole('button', { name: 'Copy to Clipboard' })).toBeVisible();
    });

    it('renders the field for non-GitHub providers (gitlab, bitbucket, azure)', async () => {
      // The relay supports all four SCM providers; the URL field must show for any of them
      // when relay is configured and the auth method is not GitHub App.
      for (const provider of ['gitlab', 'bitbucket', 'azure']) {
        axiosMock.reset();
        axiosMock.onGet(getOrganizationUrl(ROOT_ORGANIZATION_ID)).reply(200, rootOrganizationResponse);
        axiosMock.onGet(getSourceControlMetricsUrl(ownerType, ownerId)).reply(200, { results: [] });
        axiosMock.onGet(getCompositeSourceControlUrl(ownerType, ownerId)).reply(200, {
          ...defaultRootOrgConfigResponse,
          provider: { value: provider, parentValue: null, parentName: null },
        });
        axiosMock.onGet(getRelayWebhookUrl()).reply(200, { webhookUrl: RELAY_URL });

        const { unmount } = renderRoot();
        await screen.findByText('Relay Webhook URL');
        expect(document.getElementById('source-control-relay-webhook-url')).toBeVisible();
        unmount();
      }
    });

    it('hides the field when the API returns 404 (IQ not registered)', async () => {
      axiosMock.onGet(getRelayWebhookUrl()).reply(404);

      renderRoot();

      await screen.findByRole('button', { name: 'Create' });
      expect(screen.queryByText('Relay Webhook URL')).not.toBeInTheDocument();
      expect(document.getElementById('source-control-relay-webhook-url')).toBeNull();
    });

    it('hides the field when the API returns 412 (feature flag off)', async () => {
      axiosMock.onGet(getRelayWebhookUrl()).reply(412);

      renderRoot();

      await screen.findByRole('button', { name: 'Create' });
      expect(document.getElementById('source-control-relay-webhook-url')).toBeNull();
    });

    it('hides the field when no SCM provider has been selected yet', async () => {
      // defaultRootOrgConfigResponse has provider.value = null — the URL field must not
      // float on an otherwise-empty form before configuration starts.
      axiosMock.onGet(getRelayWebhookUrl()).reply(200, { webhookUrl: RELAY_URL });

      renderRoot();

      await screen.findByRole('button', { name: 'Create' });
      expect(document.getElementById('source-control-relay-webhook-url')).toBeNull();
    });

    it('copies the URL to the clipboard when the copy button is clicked', async () => {
      const writeText = jest.fn().mockResolvedValue(undefined);
      Object.assign(navigator, { clipboard: { writeText } });

      axiosMock.onGet(getCompositeSourceControlUrl(ownerType, ownerId)).reply(200, {
        ...defaultRootOrgConfigResponse,
        provider: { value: 'github', parentValue: null, parentName: null },
      });
      axiosMock.onGet(getRelayWebhookUrl()).reply(200, { webhookUrl: RELAY_URL });

      renderRoot();

      await screen.findByText('Relay Webhook URL');
      await userEvent.click(screen.getByRole('button', { name: 'Copy to Clipboard' }));
      expect(writeText).toHaveBeenCalledWith(RELAY_URL);
    });

    it('hides the field when GitHub App authentication is selected', async () => {
      axiosMock.onGet(getCompositeSourceControlUrl(ownerType, ownerId)).reply(200, {
        ...defaultRootOrgConfigResponse,
        provider: { value: 'github', parentValue: null, parentName: null },
        authenticationType: { value: 'GITHUB_APP', parentValue: null, parentName: null },
      });
      axiosMock.onGet(getRelayWebhookUrl()).reply(200, { webhookUrl: RELAY_URL });

      renderRoot();

      await screen.findByRole('button', { name: 'Create' });
      expect(screen.queryByText('Relay Webhook URL')).not.toBeInTheDocument();
      expect(document.getElementById('source-control-relay-webhook-url')).toBeNull();
    });

    it('hides the field when GitHub App authentication is INHERITED from a parent', async () => {
      // Mirror of the local-GITHUB_APP test for the inherited case. effectiveAuthenticationType
      // returns parentValue when isInherited=true; this test guards against a regression where
      // a refactor mishandles parent-side auth lookup and a child page incorrectly displays
      // the relay URL field for an App-mode parent.
      axiosMock.onGet(getCompositeSourceControlUrl(ownerType, ownerId)).reply(200, {
        ...defaultRootOrgConfigResponse,
        provider: { value: null, parentValue: 'github', parentName: ROOT_ORGANIZATION_NAME },
        authenticationType: { value: null, parentValue: 'GITHUB_APP', parentName: ROOT_ORGANIZATION_NAME },
      });
      axiosMock.onGet(getRelayWebhookUrl()).reply(200, { webhookUrl: RELAY_URL });

      renderRoot();

      await screen.findByRole('button', { name: 'Create' });
      expect(screen.queryByText('Relay Webhook URL')).not.toBeInTheDocument();
      expect(document.getElementById('source-control-relay-webhook-url')).toBeNull();
    });

    describe('Webhook Signing Secret field', () => {
      const renderWithProvider = () => {
        axiosMock.onGet(getCompositeSourceControlUrl(ownerType, ownerId)).reply(200, {
          ...defaultRootOrgConfigResponse,
          provider: { value: 'github', parentValue: null, parentName: null },
        });
        axiosMock.onGet(getRelayWebhookUrl()).reply(200, { webhookUrl: RELAY_URL });
        return renderRoot();
      };

      it('renders masked by default and toggles to plaintext on click', async () => {
        axiosMock.onGet(getRelayWebhookSecret()).reply(200, { webhookSecret: RELAY_SECRET });
        renderWithProvider();

        await screen.findByText('Webhook Signing Secret');
        const input = document.getElementById('source-control-relay-webhook-secret-input');
        expect(input).toHaveAttribute('type', 'password');
        expect(input).toHaveValue(RELAY_SECRET);
        expect(input).toHaveAttribute('readonly');

        const toggle = screen.getByRole('button', { name: 'Show webhook signing secret' });
        await userEvent.click(toggle);
        expect(input).toHaveAttribute('type', 'text');
        // After reveal the toggle's accessible name flips so screen readers announce the new state.
        expect(screen.getByRole('button', { name: 'Hide webhook signing secret' })).toBeVisible();

        await userEvent.click(screen.getByRole('button', { name: 'Hide webhook signing secret' }));
        expect(input).toHaveAttribute('type', 'password');
      });

      it('copies the secret to the clipboard while masked (no reveal needed)', async () => {
        const writeText = jest.fn().mockResolvedValue(undefined);
        Object.assign(navigator, { clipboard: { writeText } });
        axiosMock.onGet(getRelayWebhookSecret()).reply(200, { webhookSecret: RELAY_SECRET });

        renderWithProvider();

        await screen.findByText('Webhook Signing Secret');
        const input = document.getElementById('source-control-relay-webhook-secret-input');
        expect(input).toHaveAttribute('type', 'password');

        await userEvent.click(screen.getByRole('button', { name: 'Copy webhook signing secret to clipboard' }));
        expect(writeText).toHaveBeenCalledWith(RELAY_SECRET);
      });

      it('copies the secret to the clipboard while revealed', async () => {
        const writeText = jest.fn().mockResolvedValue(undefined);
        Object.assign(navigator, { clipboard: { writeText } });
        axiosMock.onGet(getRelayWebhookSecret()).reply(200, { webhookSecret: RELAY_SECRET });

        renderWithProvider();

        await screen.findByText('Webhook Signing Secret');
        await userEvent.click(screen.getByRole('button', { name: 'Show webhook signing secret' }));
        await userEvent.click(screen.getByRole('button', { name: 'Copy webhook signing secret to clipboard' }));
        expect(writeText).toHaveBeenCalledWith(RELAY_SECRET);
      });

      it('hides the field when GitHub App authentication is selected', async () => {
        axiosMock.onGet(getCompositeSourceControlUrl(ownerType, ownerId)).reply(200, {
          ...defaultRootOrgConfigResponse,
          provider: { value: 'github', parentValue: null, parentName: null },
          authenticationType: { value: 'GITHUB_APP', parentValue: null, parentName: null },
        });
        axiosMock.onGet(getRelayWebhookUrl()).reply(404);
        // Even if the API improbably returned a secret, GitHub App auth must not show the field.
        axiosMock.onGet(getRelayWebhookSecret()).reply(200, { webhookSecret: RELAY_SECRET });

        renderRoot();

        await screen.findByRole('button', { name: 'Create' });
        expect(screen.queryByText('Webhook Signing Secret')).not.toBeInTheDocument();
        expect(document.getElementById('source-control-relay-webhook-secret')).toBeNull();
      });

      it('hides the field when the secret API returns 404', async () => {
        axiosMock.onGet(getRelayWebhookSecret()).reply(404);
        renderWithProvider();

        await screen.findByText('Relay Webhook URL');
        expect(screen.queryByText('Webhook Signing Secret')).not.toBeInTheDocument();
      });

      it('hides the field when the secret API returns 412', async () => {
        axiosMock.onGet(getRelayWebhookSecret()).reply(412);
        renderWithProvider();

        await screen.findByText('Relay Webhook URL');
        expect(screen.queryByText('Webhook Signing Secret')).not.toBeInTheDocument();
      });
    });
  });

  describe('Organization (non-root)', () => {
    const ownerType = 'organization';
    const ownerId = ORGANIZATION_ID;

    const baseState = {
      router: {
        currentParams: { organizationId: ORGANIZATION_ID },
        currentState: { name: 'organization' },
      },
      productFeatures: {
        productFeatures: { notifications: true, automation: true, 'saas-lifecycle-scm-prs-enabled': true },
      },
      orgsAndPolicies: {
        root: { selectedOwner: { id: ORGANIZATION_ID, name: 'Some Organization' } },
      },
    };

    beforeEach(() => {
      axiosMock.onGet(getCompositeSourceControlUrl(ownerType, ownerId)).reply(200, defaultOrgConfigResponse);
      axiosMock.onGet(getSourceControlMetricsUrl(ownerType, ownerId)).reply(200, { results: [] });
      axiosMock.onGet(getRelayWebhookUrl()).reply(404);
      axiosMock.onGet(getRelayWebhookSecret()).reply(404);
    });

    const renderOrg = (extra = {}) =>
      render(<SourceControlConfiguration />, { preloadedState: mergeDeepRight(baseState, extra) });

    it('renders the read-only field when the API returns a URL', async () => {
      axiosMock.onGet(getCompositeSourceControlUrl(ownerType, ownerId)).reply(200, {
        ...defaultOrgConfigResponse,
        provider: { value: 'github', parentValue: null, parentName: null },
      });
      axiosMock.onGet(getRelayWebhookUrl()).reply(200, { webhookUrl: RELAY_URL });

      renderOrg();

      await screen.findByText('Relay Webhook URL');
      const textarea = document.getElementById('source-control-relay-webhook-url').querySelector('textarea');
      expect(textarea).toHaveValue(RELAY_URL);
    });

    it('hides the field when no SCM provider has been selected yet', async () => {
      axiosMock.onGet(getRelayWebhookUrl()).reply(200, { webhookUrl: RELAY_URL });

      renderOrg();

      await screen.findByRole('button', { name: 'Update' });
      expect(document.getElementById('source-control-relay-webhook-url')).toBeNull();
    });

    it('hides the field when the API returns 404', async () => {
      axiosMock.onGet(getRelayWebhookUrl()).reply(404);

      renderOrg();

      await screen.findByRole('button', { name: 'Update' });
      expect(document.getElementById('source-control-relay-webhook-url')).toBeNull();
      expect(screen.queryByText('Relay Webhook URL')).not.toBeInTheDocument();
    });

    it('hides the field when GitHub App authentication is selected (local)', async () => {
      axiosMock.onGet(getCompositeSourceControlUrl(ownerType, ownerId)).reply(200, {
        ...defaultOrgConfigResponse,
        provider: { value: 'github', parentValue: null, parentName: null },
        authenticationType: { value: 'GITHUB_APP', parentValue: null, parentName: null },
      });
      axiosMock.onGet(getRelayWebhookUrl()).reply(200, { webhookUrl: RELAY_URL });

      renderOrg();

      await screen.findByRole('button', { name: 'Update' });
      expect(document.getElementById('source-control-relay-webhook-url')).toBeNull();
    });

    it('hides the field when GitHub App authentication is INHERITED from a parent', async () => {
      axiosMock.onGet(getCompositeSourceControlUrl(ownerType, ownerId)).reply(200, {
        ...defaultOrgConfigResponse,
        provider: { value: null, parentValue: 'github', parentName: ROOT_ORGANIZATION_NAME },
        authenticationType: { value: null, parentValue: 'GITHUB_APP', parentName: ROOT_ORGANIZATION_NAME },
      });
      axiosMock.onGet(getRelayWebhookUrl()).reply(200, { webhookUrl: RELAY_URL });

      renderOrg();

      await screen.findByRole('button', { name: 'Update' });
      expect(document.getElementById('source-control-relay-webhook-url')).toBeNull();
    });

    describe('Webhook Signing Secret field (smoke)', () => {
      // Smoke coverage so an accidental removal of RelayWebhookSecretField from the Org page
      // is caught here. Full reveal/copy/hide-on-app coverage lives in the Root section.
      it('renders the secret field when PAT is selected and the API returns a secret', async () => {
        axiosMock.onGet(getCompositeSourceControlUrl(ownerType, ownerId)).reply(200, {
          ...defaultOrgConfigResponse,
          provider: { value: 'github', parentValue: null, parentName: null },
        });
        axiosMock.onGet(getRelayWebhookUrl()).reply(200, { webhookUrl: RELAY_URL });
        axiosMock.onGet(getRelayWebhookSecret()).reply(200, { webhookSecret: RELAY_SECRET });

        renderOrg();

        await screen.findByText('Webhook Signing Secret');
        const input = document.getElementById('source-control-relay-webhook-secret-input');
        expect(input).toHaveValue(RELAY_SECRET);
      });

      it('hides the secret field when the API returns 404', async () => {
        axiosMock.onGet(getCompositeSourceControlUrl(ownerType, ownerId)).reply(200, {
          ...defaultOrgConfigResponse,
          provider: { value: 'github', parentValue: null, parentName: null },
        });
        axiosMock.onGet(getRelayWebhookUrl()).reply(200, { webhookUrl: RELAY_URL });
        axiosMock.onGet(getRelayWebhookSecret()).reply(404);

        renderOrg();

        await screen.findByText('Relay Webhook URL');
        expect(screen.queryByText('Webhook Signing Secret')).not.toBeInTheDocument();
      });
    });
  });

  describe('Application', () => {
    const ownerType = 'application';
    const ownerId = APP_OWNER_ID;

    const baseState = {
      router: {
        currentState: { name: 'management.edit.application.edit-source-control' },
        currentParams: { applicationPublicId: 'vulnerable-java-app' },
      },
      productFeatures: { productFeatures: { notifications: true, automation: true } },
      orgsAndPolicies: {
        root: {
          selectedOwner: { id: ownerId, publicId: 'vulnerable-java-app', name: 'Vulnerable java app' },
        },
      },
    };

    beforeEach(() => {
      axiosMock.onGet(getCompositeSourceControlUrl(ownerType, ownerId)).reply(200, defaultAppConfigResponse);
      axiosMock.onGet(getSourceControlMetricsUrl(ownerType, ownerId)).reply(200, { results: [] });
      axiosMock.onGet(getRelayWebhookUrl()).reply(404);
      axiosMock.onGet(getRelayWebhookSecret()).reply(404);
    });

    const renderApp = (extra = {}) =>
      render(<SourceControlConfiguration />, { preloadedState: mergeDeepRight(baseState, extra) });

    it('renders the read-only field when the API returns a URL', async () => {
      axiosMock.onGet(getCompositeSourceControlUrl(ownerType, ownerId)).reply(200, {
        ...defaultAppConfigResponse,
        provider: { value: 'github', parentValue: null, parentName: null },
      });
      axiosMock.onGet(getRelayWebhookUrl()).reply(200, { webhookUrl: RELAY_URL });

      renderApp();

      await screen.findByText('Relay Webhook URL');
      const textarea = document.getElementById('source-control-relay-webhook-url').querySelector('textarea');
      expect(textarea).toHaveValue(RELAY_URL);
    });

    it('hides the field when no SCM provider has been selected yet', async () => {
      // defaultAppConfigResponse has provider.value = null — the URL field must not
      // float on an otherwise-empty form before configuration starts.
      axiosMock.onGet(getRelayWebhookUrl()).reply(200, { webhookUrl: RELAY_URL });

      renderApp();

      await screen.findByRole('button', { name: 'Update' });
      expect(document.getElementById('source-control-relay-webhook-url')).toBeNull();
    });

    it('hides the field when the API returns 404', async () => {
      axiosMock.onGet(getRelayWebhookUrl()).reply(404);

      renderApp();

      await screen.findByRole('button', { name: 'Update' });
      expect(document.getElementById('source-control-relay-webhook-url')).toBeNull();
      expect(screen.queryByText('Relay Webhook URL')).not.toBeInTheDocument();
    });

    it('hides the field when the API returns 412 (feature flag off)', async () => {
      axiosMock.onGet(getRelayWebhookUrl()).reply(412);

      renderApp();

      await screen.findByRole('button', { name: 'Update' });
      expect(document.getElementById('source-control-relay-webhook-url')).toBeNull();
    });

    it('hides the field when GitHub App authentication is selected', async () => {
      axiosMock.onGet(getCompositeSourceControlUrl(ownerType, ownerId)).reply(200, {
        ...defaultAppConfigResponse,
        provider: { value: 'github', parentValue: null, parentName: null },
        authenticationType: { value: 'GITHUB_APP', parentValue: null, parentName: null },
      });
      axiosMock.onGet(getRelayWebhookUrl()).reply(200, { webhookUrl: RELAY_URL });

      renderApp();

      await screen.findByRole('button', { name: 'Update' });
      expect(screen.queryByText('Relay Webhook URL')).not.toBeInTheDocument();
      expect(document.getElementById('source-control-relay-webhook-url')).toBeNull();
    });

    it('hides the field when GitHub App authentication is INHERITED from a parent', async () => {
      // Mirror of Root and Org inherited-GITHUB_APP tests for the Application page.
      // Guards against a regression where parent-side auth lookup is mishandled and a child
      // App page incorrectly shows the relay URL field for an App-mode parent.
      axiosMock.onGet(getCompositeSourceControlUrl(ownerType, ownerId)).reply(200, {
        ...defaultAppConfigResponse,
        provider: { value: null, parentValue: 'github', parentName: ROOT_ORGANIZATION_NAME },
        authenticationType: { value: null, parentValue: 'GITHUB_APP', parentName: ROOT_ORGANIZATION_NAME },
      });
      axiosMock.onGet(getRelayWebhookUrl()).reply(200, { webhookUrl: RELAY_URL });

      renderApp();

      await screen.findByRole('button', { name: 'Update' });
      expect(document.getElementById('source-control-relay-webhook-url')).toBeNull();
    });

    describe('Webhook Signing Secret field (smoke)', () => {
      // Smoke coverage so an accidental removal of RelayWebhookSecretField from the App page
      // is caught here. Full reveal/copy/hide-on-app coverage lives in the Root section.
      it('renders the secret field when PAT is selected and the API returns a secret', async () => {
        axiosMock.onGet(getCompositeSourceControlUrl(ownerType, ownerId)).reply(200, {
          ...defaultAppConfigResponse,
          provider: { value: 'github', parentValue: null, parentName: null },
        });
        axiosMock.onGet(getRelayWebhookUrl()).reply(200, { webhookUrl: RELAY_URL });
        axiosMock.onGet(getRelayWebhookSecret()).reply(200, { webhookSecret: RELAY_SECRET });

        renderApp();

        await screen.findByText('Webhook Signing Secret');
        const input = document.getElementById('source-control-relay-webhook-secret-input');
        expect(input).toHaveValue(RELAY_SECRET);
      });

      it('hides the secret field when the API returns 404', async () => {
        axiosMock.onGet(getCompositeSourceControlUrl(ownerType, ownerId)).reply(200, {
          ...defaultAppConfigResponse,
          provider: { value: 'github', parentValue: null, parentName: null },
        });
        axiosMock.onGet(getRelayWebhookUrl()).reply(200, { webhookUrl: RELAY_URL });
        axiosMock.onGet(getRelayWebhookSecret()).reply(404);

        renderApp();

        await screen.findByText('Relay Webhook URL');
        expect(screen.queryByText('Webhook Signing Secret')).not.toBeInTheDocument();
      });
    });

  });
});
