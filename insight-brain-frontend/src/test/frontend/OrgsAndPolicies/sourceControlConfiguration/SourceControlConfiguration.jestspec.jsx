/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import SourceControlConfiguration from 'MainRoot/OrgsAndPolicies/sourceControlConfiguration/SourceControlConfiguration';
import { render, screen, axiosMockAdapter, within } from 'TestRoot/SpecUtil';
import {
  getApplicationSummaryUrl,
  getCompositeSourceControlUrl,
  getOrganizationUrl,
  getSourceControlMetricsUrl,
  getSourceControlUrl,
} from 'MainRoot/util/CLMLocation';
import { fireEvent, waitFor } from '@testing-library/react';
import { testSourceControlContainers } from './helpers';
import {
  ROOT_ORGANIZATION_ID,
  ROOT_ORGANIZATION_NAME,
  ORGANIZATION_ID,
  ORGANIZATION_NAME,
  defaultRootOrgConfigResponse,
  existingRootOrgConfigResponse,
  defaultOrgConfigResponse,
  assertionsForOrgDefaultState,
  inheritedOrgConfigResponse,
  assertionsForOrgInheritedState,
  existingOrgConfigResponse,
  assertionsForOrgExistingState,
  APPLICATION_ID,
  APPLICATION_NAME,
  defaultAppConfigResponse,
  assertionsForAppDefaultState,
  inheritedAppConfigResponse,
  existingAppConfigResponse,
  assertionsForAppInheritedState,
  assertionsForAppNoTokenInheritedState,
  inheritedAppNoTokenConfigResponse,
  assertionsForAppExistingState,
  applicationsResponse,
  rootOrganizationResponse,
  organizationResponse,
} from './data';
import { clone } from 'ramda';
import {
  AUTHENTICATION_TYPES,
  SOURCE_CONTROL_UNSUPPORTED_MESSAGE,
  compositeSourceControlToModel,
  getScmFormStateStorageKey,
} from 'MainRoot/OrgsAndPolicies/sourceControlConfiguration/utils';

import 'TestRoot/SpecUtil';

let ownerType;
let ownerId;

describe('sourceControlConfiguration', () => {
  let axiosMock;

  beforeAll(() => {
    axiosMock = axiosMockAdapter();
  });

  let defaultPreloadedState;

  let renderComponent = (preloadedState) =>
    render(<SourceControlConfiguration />, { preloadedState: preloadedState || defaultPreloadedState });

  describe('Root Organization', () => {
    beforeEach(() => {
      ownerType = 'organization';
      ownerId = ROOT_ORGANIZATION_ID;
      defaultPreloadedState = {
        router: {
          currentParams: {
            organizationId: ROOT_ORGANIZATION_ID,
          },
          currentState: {
            name: 'organization',
          },
        },
        productFeatures: {
          productFeatures: {
            notifications: true,
            automation: true,
            'manual-pull-requests': true,
            'saas-lifecycle-scm-prs-enabled': true,
          },
        },
        orgsAndPolicies: {
          root: {
            selectedOwner: {
              id: ROOT_ORGANIZATION_ID,
              name: ROOT_ORGANIZATION_NAME,
            },
          },
        },
      };

      axiosMock.onGet(getOrganizationUrl(ROOT_ORGANIZATION_ID)).reply(200, rootOrganizationResponse);
      axiosMock.onGet(getCompositeSourceControlUrl(ownerType, ownerId)).reply(200, defaultRootOrgConfigResponse);
      axiosMock.onGet(getSourceControlMetricsUrl(ownerType, ownerId)).reply(200, { results: [] });
    });

    describe('initial source control configuration render', () => {
      it('shows page with the correct title and subtitle', async () => {
        renderComponent();
        expect(screen.getByText('Loading…')).toBeVisible();

        const sourcePageTitle = await screen.findByText('Source Control Configuration');
        const sourceSubtitle = screen.getByText(
          `Configures the integration with an external SCM for ${ROOT_ORGANIZATION_NAME}`
        );
        expect(sourcePageTitle).toBeVisible();
        expect(sourceSubtitle).toBeVisible();
      });

      it('fetches sourceControl configuration, metrics and shows loading spinner, while loading page', async () => {
        renderComponent();
        expect(screen.getByText('Loading…')).toBeVisible();
        await screen.findByText('Source Control Configuration');
        expect(axiosMock.history.get.length).toBe(2);
        expect(axiosMock.history.get[0].url).toBe(getCompositeSourceControlUrl(ownerType, ownerId));
        expect(axiosMock.history.get[1].url).toBe(getSourceControlMetricsUrl(ownerType, ownerId));
      });

      it('shows all form elements on successful initial render with correct default configuration values', async () => {
        renderComponent();
        const submitButton = await screen.findByRole('button', { name: 'Create' });
        const resetButton = screen.getByRole('button', { name: 'Reset' });
        const providerSelector = screen.getByRole('combobox');
        const options = screen.getAllByRole('option');
        expect(options.length).toBe(5);
        expect(options[0].value).toBe('');
        expect(options[1].value).toBe('azure');
        expect(providerSelector.value).toBe('');
        const tokenInput = screen.getByLabelText('Access Token');
        const branchNameInput = screen.getByRole('textbox', { name: 'Default Branch' });
        expect(tokenInput).toBeVisible();
        expect(tokenInput).toBeDisabled();
        expect(tokenInput.value).toBe('');
        expect(branchNameInput).toBeVisible();
        expect(branchNameInput).toBeDisabled();
        expect(branchNameInput.value).toBe('main');
        const [
          sshToggle,
          remediationPullRequests,
          innerSourceAutomatedUpdates,
          pullRequestCommenting,
          sourceControlEvaluations,
          automaticCommitFeedback,
          manualPullRequests,
        ] = screen.getAllByRole('switch');
        const userNameInput = screen.queryByRole('textbox', { name: 'Username' });
        expect(userNameInput).not.toBeInTheDocument();
        expect(sshToggle).toBeVisible();
        expect(sshToggle).toBeDisabled();
        expect(sshToggle.checked).toBe(false);
        expect(remediationPullRequests).toBeVisible();
        expect(remediationPullRequests).toBeDisabled();
        expect(remediationPullRequests.checked).toBe(false);
        expect(pullRequestCommenting).toBeVisible();
        expect(pullRequestCommenting).toBeDisabled();
        expect(pullRequestCommenting.checked).toBe(true);
        expect(sourceControlEvaluations).toBeVisible();
        expect(sourceControlEvaluations).toBeDisabled();
        expect(sourceControlEvaluations.checked).toBe(true);
        expect(automaticCommitFeedback).toBeVisible();
        expect(automaticCommitFeedback).toBeDisabled();
        expect(automaticCommitFeedback.checked).toBe(true);
        expect(manualPullRequests).toBeVisible();
        expect(manualPullRequests).toBeDisabled();
        expect(manualPullRequests.checked).toBe(true);
        expect(innerSourceAutomatedUpdates).toBeVisible();
        expect(innerSourceAutomatedUpdates).toBeDisabled();
        expect(innerSourceAutomatedUpdates.checked).toBe(true);
        expect(resetButton).toBeVisible();
        expect(submitButton).toBeVisible();
        fireEvent.click(submitButton);
        expect(axiosMock.history.put.length).toBe(0);
      });

      it('renders error message and retry button when loading source control configuration fails', async () => {
        axiosMock.onGet(getCompositeSourceControlUrl(ownerType, ownerId)).reply(500, 'Some Error');
        renderComponent();
        expect(await screen.findByRole('alert')).toBeVisible();
        expect(screen.getByRole('button', { name: 'Retry' })).toBeVisible();
        expect(screen.getByText('An error occurred loading data. Some Error')).toBeVisible();
      });

      it('shows No license error, when user does not have license', async () => {
        renderComponent({
          router: {
            currentParams: {
              organizationId: ROOT_ORGANIZATION_ID,
            },
            currentState: {
              name: 'organization',
            },
          },
          productFeatures: {
            productFeatures: {
              notifications: false,
              automation: false,
            },
          },
          orgsAndPolicies: {
            root: {
              selectedOwner: {
                id: ROOT_ORGANIZATION_ID,
                name: ROOT_ORGANIZATION_NAME,
              },
            },
          },
        });
        expect(await screen.findByRole('alert')).toBeVisible();
        const errorAlert = screen.getByText(SOURCE_CONTROL_UNSUPPORTED_MESSAGE);
        expect(errorAlert).toBeVisible();
      });
    });

    describe('Reset modal', () => {
      beforeEach(() => {
        axiosMock.onDelete(getSourceControlUrl(ownerType, ownerId)).reply(200);
      });

      it('shows disabled Reset button until form submitting(rely on sourceControl.id)', async () => {
        renderComponent();
        const resetBtn = await screen.findByRole('button', { name: 'Reset' });
        fireEvent.click(resetBtn);
        const resetModalText = screen.queryByText(
          `You are about to reset the Source Control configuration for ${ROOT_ORGANIZATION_NAME}. This action cannot be undone.`
        );
        expect(resetModalText).not.toBeInTheDocument();
      });

      it('enables reset button and shows reset modal if user has source control configuration(sourceControl.id)', async () => {
        axiosMock.onGet(getCompositeSourceControlUrl(ownerType, ownerId)).reply(200, existingRootOrgConfigResponse);
        renderComponent();
        const resetBtn = await screen.findByRole('button', { name: 'Reset' });
        fireEvent.click(resetBtn);
        const resetModalText = screen.getByText(
          `You are about to reset the Source Control configuration for ${ROOT_ORGANIZATION_NAME}. This action cannot be undone.`
        );
        const submitBtn = screen.getByRole('button', { name: 'Continue' });
        const cancelBtn = screen.getByRole('button', { name: 'Cancel' });
        expect(screen.getByRole('alert')).toBeVisible();
        expect(resetModalText).toBeVisible();
        expect(submitBtn).toBeVisible();
        expect(cancelBtn).toBeVisible();
      });

      it('resets configuration while clicking on Continue in Reset modal', async () => {
        axiosMock.onGet(getCompositeSourceControlUrl(ownerType, ownerId)).reply(200, existingRootOrgConfigResponse);
        renderComponent();
        const resetBtn = await screen.findByRole('button', { name: 'Reset' });
        fireEvent.click(resetBtn);
        const submitModalBtn = screen.getByRole('button', { name: 'Continue' });
        fireEvent.click(submitModalBtn);
        expect(axiosMock.history.delete.length).toBe(1);
        expect(axiosMock.history.delete[0].url).toBe(getSourceControlUrl(ownerType, ownerId));
      });

      it('renders error message and retry button when resetting configuration fails', async () => {
        axiosMock.onGet(getCompositeSourceControlUrl(ownerType, ownerId)).reply(200, existingRootOrgConfigResponse);
        axiosMock.onDelete(getSourceControlUrl(ownerType, ownerId)).reply(500, 'Some Error');
        renderComponent();
        const resetBtn = await screen.findByRole('button', { name: 'Reset' });
        fireEvent.click(resetBtn);
        const submitModalBtn = screen.getByRole('button', { name: 'Continue' });
        fireEvent.click(submitModalBtn);
        expect(await screen.findByRole('alert')).toBeVisible();
        expect(screen.getByRole('button', { name: 'Retry' })).toBeVisible();
        expect(screen.getByText('An error occurred saving data. Some Error')).toBeVisible();
      });
    });

    describe('submitting and changing the form', () => {
      it('shows userName input field only if Azure DevOps or Bitbucket providers are chosen', async () => {
        renderComponent();
        const providerSelector = await screen.findByRole('combobox');
        let userNameInput = screen.queryByRole('textbox', { name: 'Username' });
        expect(userNameInput).not.toBeInTheDocument();
        fireEvent.change(providerSelector, { target: { value: 'azure' } });
        userNameInput = screen.getByRole('textbox', { name: 'Username' });
        expect(userNameInput).toBeVisible();
        fireEvent.change(providerSelector, { target: { value: 'github' } });
        userNameInput = screen.queryByRole('textbox', { name: 'Username' });
        expect(userNameInput).not.toBeInTheDocument();
        fireEvent.change(providerSelector, { target: { value: 'bitbucket' } });
        userNameInput = screen.getByRole('textbox', { name: 'Username' });
        expect(userNameInput).toBeVisible();
        fireEvent.change(providerSelector, { target: { value: 'gitlab' } });
        userNameInput = screen.queryByRole('textbox', { name: 'Username' });
        expect(userNameInput).not.toBeInTheDocument();
      });

      it('submits new configuration (Post request) if there is no entity before', async () => {
        const submitData = {
          authenticationType: null,
          provider: 'gitlab',
          username: null,
          token: 'admin123',
          baseBranch: 'main',
          remediationPullRequestsEnabled: false,
          statusChecksEnabled: true,
          pullRequestCommentingEnabled: true,
          commitStatusEnabled: true,
          sourceControlEvaluationsEnabled: true,
          sshEnabled: null,
          manualPullRequestsEnabled: true,
          innerSourceAutomatedUpdatesEnabled: true,
          closePrOnFailedChecksEnabled: true,
          closePrAfterDaysOpenEnabled: false,
          closePrAfterDays: null,
        };
        axiosMock.onPost(getSourceControlUrl(ownerType, ownerId), submitData).reply(200);
        renderComponent();
        const providerSelector = await screen.findByRole('combobox');
        fireEvent.change(providerSelector, { target: { value: 'gitlab' } });
        expect(providerSelector.value).toBe('gitlab');
        const tokenInput = screen.getByLabelText('Access Token');
        fireEvent.change(tokenInput, { target: { value: 'admin123' } });
        const submitButton = screen.getByRole('button', { name: 'Create' });
        fireEvent.click(submitButton);
        expect(axiosMock.history.post.length).toBe(1);
        expect(axiosMock.history.post[0].url).toBe(getSourceControlUrl(ownerType, ownerId));
        expect(JSON.parse(axiosMock.history.post[0].data)).toEqual(submitData);
      });

      it('updates configuration(Put request) if there is an entity before', async () => {
        const submitData = {
          authenticationType: null,
          provider: 'azure',
          username: 'admin',
          token: 'admin123',
          baseBranch: 'main',
          remediationPullRequestsEnabled: false,
          statusChecksEnabled: true,
          pullRequestCommentingEnabled: true,
          commitStatusEnabled: true,
          sourceControlEvaluationsEnabled: true,
          sshEnabled: null,
          manualPullRequestsEnabled: false,
          innerSourceAutomatedUpdatesEnabled: false,
          closePrOnFailedChecksEnabled: null,
          closePrAfterDaysOpenEnabled: false,
          closePrAfterDays: null,
        };
        axiosMock.onGet(getCompositeSourceControlUrl(ownerType, ownerId)).reply(200, existingRootOrgConfigResponse);
        axiosMock.onPut(getSourceControlUrl(ownerType, ownerId), submitData).reply(200);
        renderComponent();
        const tokenInput = await screen.findByLabelText('Access Token');
        fireEvent.change(tokenInput, { target: { value: 'admin123' } });
        const submitButton = screen.getByRole('button', { name: 'Update' });
        fireEvent.click(submitButton);
        expect(axiosMock.history.put.length).toBe(1);
        expect(axiosMock.history.put[0].url).toBe(getSourceControlUrl(ownerType, ownerId));
        expect(JSON.parse(axiosMock.history.put[0].data)).toEqual(submitData);
      });

      it('shows validation error and prevent from submitting if field is not valid', async () => {
        axiosMock.onGet(getCompositeSourceControlUrl(ownerType, ownerId)).reply(200, existingRootOrgConfigResponse);
        axiosMock.onPut(getSourceControlUrl(ownerType, ownerId), {}).reply(200);
        renderComponent();
        const tokenInput = await screen.findByLabelText('Access Token');
        fireEvent.change(tokenInput, { target: { value: '' } });
        const submitButton = screen.getByRole('button', { name: 'Update' });
        fireEvent.click(submitButton);
        expect(axiosMock.history.put.length).toBe(0);
        const alerts = await screen.findAllByRole('alert');
        expect(alerts.length).toBe(2);
        const inputValidationMsg = screen.getByText('Must be non-empty');
        const footerErrorAlertMsg = screen.getByText(
          'There were validation errors. Unable to save: fields with invalid or missing data'
        );
        expect(inputValidationMsg).toBeVisible();
        expect(footerErrorAlertMsg).toBeVisible();
      });

      it('shows validation error and prevent from submitting if form is not dirty', async () => {
        axiosMock.onGet(getCompositeSourceControlUrl(ownerType, ownerId)).reply(200, existingRootOrgConfigResponse);
        axiosMock.onPut(getSourceControlUrl(ownerType, ownerId), {}).reply(200);
        renderComponent();
        const userNameInput = await screen.findByRole('textbox', { name: 'Username' });
        expect(userNameInput.value).toBe('admin');
        fireEvent.change(userNameInput, { target: { value: 'some Other Name' } });
        fireEvent.change(userNameInput, { target: { value: 'admin' } });
        const submitButton = screen.getByRole('button', { name: 'Update' });
        fireEvent.click(submitButton);
        const alert = screen.getByRole('alert');
        expect(alert).toBeVisible();
        const footerErrorAlertMsg = screen.getByText('There were validation errors. There are no changes to save.');
        expect(footerErrorAlertMsg).toBeVisible();
      });

      it('renders error message and retry button when submitting configuration fails and clears error if provider is changed', async () => {
        const submitData = {
          authenticationType: null,
          provider: 'azure',
          username: 'some Other Name',
          token: '#~FAKE~SECRET~KEY~#',
          baseBranch: 'main',
          remediationPullRequestsEnabled: false,
          statusChecksEnabled: true,
          pullRequestCommentingEnabled: true,
          commitStatusEnabled: true,
          sourceControlEvaluationsEnabled: true,
          sshEnabled: null,
          manualPullRequestsEnabled: false,
          innerSourceAutomatedUpdatesEnabled: false,
          closePrOnFailedChecksEnabled: null,
          closePrAfterDaysOpenEnabled: false,
          closePrAfterDays: null,
        };
        axiosMock.onGet(getCompositeSourceControlUrl(ownerType, ownerId)).reply(200, existingRootOrgConfigResponse);
        axiosMock.onPut(getSourceControlUrl(ownerType, ownerId), submitData).reply(500, 'Saving Error');
        renderComponent();
        const userNameInput = await screen.findByRole('textbox', { name: 'Username' });
        expect(userNameInput.value).toBe('admin');
        fireEvent.change(userNameInput, { target: { value: 'some Other Name' } });
        const submitButton = screen.getByRole('button', { name: 'Update' });
        fireEvent.click(submitButton);
        expect(axiosMock.history.put.length).toBe(1);
        expect(axiosMock.history.put[0].url).toBe(getSourceControlUrl(ownerType, ownerId));
        expect(await screen.findByRole('alert')).toBeVisible();
        expect(screen.getByRole('button', { name: 'Retry' })).toBeVisible();
        expect(screen.getByText('An error occurred saving data. Saving Error')).toBeVisible();
        const providerSelector = await screen.findByRole('combobox');
        fireEvent.change(providerSelector, { target: { value: 'github' } });
        expect(screen.queryByText('An error occurred saving data. Saving Error')).not.toBeInTheDocument();
      });

      it('shows the manual pull requests toggle when saas-lifecycle-scm-prs-enabled is true', async () => {
        renderComponent();
        await screen.findByRole('button', { name: 'Create' });
        const switchesWithFlag = screen.getAllByRole('switch');
        expect(switchesWithFlag.length).toBe(7);
        expect(screen.getByText('Manual Pull Requests')).toBeInTheDocument();
      });

      it('hides the manual pull requests toggle when saas-lifecycle-scm-prs-enabled is false', async () => {
        const preloadedState = clone(defaultPreloadedState);
        preloadedState.productFeatures.productFeatures['saas-lifecycle-scm-prs-enabled'] = false;
        renderComponent(preloadedState);
        await screen.findByRole('button', { name: 'Create' });
        const switchesWithFlag = screen.getAllByRole('switch');
        expect(switchesWithFlag.length).toBe(3);
        expect(screen.queryByText('Manual Pull Requests')).not.toBeInTheDocument();
      });
    });

    describe('fields validation', () => {
      it('shows an error message if provider --Not Configured-- option was selected', async () => {
        renderComponent();
        const providerSelector = await screen.findByRole('combobox');
        fireEvent.change(providerSelector, { target: { value: 'gitlab' } });
        fireEvent.change(providerSelector, { target: { value: '' } });
        expect(screen.getByText('Must be non-empty')).toBeVisible();
      });

      it("doesn't show an error message if provider doesn't require username, and token field has no value", async () => {
        renderComponent();
        const providerSelector = await screen.findByRole('combobox');
        fireEvent.change(providerSelector, { target: { value: 'gitlab' } });
        const tokenInput = screen.getByLabelText('Access Token');
        fireEvent.change(tokenInput, { target: { value: '' } });
        expect(screen.queryByText('Must be non-empty')).toBeInTheDocument();
      });

      it("doesn't show an error message if provider requires username, but username and token fields have no values", async () => {
        renderComponent();
        const providerSelector = await screen.findByRole('combobox');
        fireEvent.change(providerSelector, { target: { value: 'azure' } });
        const userNameInput = screen.getByRole('textbox', { name: 'Username' });
        const tokenInput = screen.getByLabelText('Access Token');
        expect(userNameInput.value).toBe('');
        expect(tokenInput.value).toBe('');
        expect(screen.getAllByText('Must be non-empty').length).toBe(2);
      });

      it('shows an error message if provider requires username, username has value, token has no value and dirty(isPristine: false)', async () => {
        renderComponent();
        const providerSelector = await screen.findByRole('combobox');
        fireEvent.change(providerSelector, { target: { value: 'azure' } });
        const userNameInput = screen.getByRole('textbox', { name: 'Username' });
        const tokenInput = screen.getByLabelText('Access Token');
        fireEvent.change(userNameInput, { target: { value: 'admin' } });
        expect(userNameInput.value).toBe('admin');
        expect(tokenInput.value).toBe('');
        expect(screen.getByText('Must be non-empty')).toBeVisible();
        fireEvent.change(tokenInput, { target: { value: 'token' } });
        expect(screen.queryByText('Must be non-empty')).not.toBeInTheDocument();
        fireEvent.change(tokenInput, { target: { value: '' } });
        fireEvent.change(userNameInput, { target: { value: '' } });
        expect(screen.getAllByText('Must be non-empty').length).toBe(2);
        fireEvent.change(userNameInput, { target: { value: 'admin' } });
        expect(screen.getByText('Must be non-empty')).toBeVisible();
      });

      it('shows an error message if provider requires username, token field has a value, username has no value and dirty(isPristine=false)', async () => {
        renderComponent();
        const providerSelector = await screen.findByRole('combobox');
        fireEvent.change(providerSelector, { target: { value: 'azure' } });
        const userNameInput = screen.getByRole('textbox', { name: 'Username' });
        const tokenInput = screen.getByLabelText('Access Token');
        fireEvent.change(tokenInput, { target: { value: 'token123' } });
        expect(userNameInput.value).toBe('');
        expect(tokenInput.value).toBe('token123');
        expect(screen.getByText('Must be non-empty')).toBeVisible();
        fireEvent.change(userNameInput, { target: { value: 'admin' } });
        expect(screen.queryByText('Must be non-empty')).not.toBeInTheDocument();
        fireEvent.change(tokenInput, { target: { value: '' } });
        fireEvent.change(userNameInput, { target: { value: '' } });
        expect(screen.getAllByText('Must be non-empty').length).toBe(2);
        fireEvent.change(tokenInput, { target: { value: 'token123' } });
        expect(screen.getByText('Must be non-empty')).toBeVisible();
      });

      it('check required inputs', async () => {
        renderComponent();
        const providerSelector = await screen.findByRole('combobox');
        fireEvent.change(providerSelector, { target: { value: 'azure' } });
        const userNameInput = screen.getByRole('textbox', { name: 'Username' });
        const tokenInput = screen.getByLabelText('Access Token');
        fireEvent.change(tokenInput, { target: { value: 'token123' } });

        expect(userNameInput).toBeRequired();
        expect(tokenInput).toBeRequired();
      });
    });

    describe('Advanced options visibility', () => {
      const selectProvider = async (provider) => {
        const providerSelector = await screen.findByRole('combobox');
        fireEvent.change(providerSelector, { target: { value: provider } });
      };

      it('shows advanced options for GitHub', async () => {
        renderComponent();
        await selectProvider('github');

        const advancedOptionsSection = screen.queryByText(/Advanced Github Options/);
        const failedChecksCheckbox = screen.queryByText('Close AutoPRs when one or more required checks fail');
        const afterDaysCheckbox = screen.queryByText(/Close AutoPRs that have not been merged or closed after:/);
        const daysInput = screen.queryByPlaceholderText('Ex. 7');

        expect(advancedOptionsSection).toBeInTheDocument();
        expect(failedChecksCheckbox).toBeInTheDocument();
        expect(afterDaysCheckbox).toBeInTheDocument();
        expect(daysInput).toBeInTheDocument();
      });

      it('shows advanced options for GitLab', async () => {
        renderComponent();
        await selectProvider('gitlab');

        const advancedOptionsSection = screen.queryByText(/Advanced Gitlab Options/);
        const failedChecksCheckbox = screen.queryByText('Close AutoPRs when one or more required checks fail');
        const afterDaysCheckbox = screen.queryByText(/Close AutoPRs that have not been merged or closed after:/);
        const daysInput = screen.queryByPlaceholderText('Ex. 7');

        expect(advancedOptionsSection).toBeInTheDocument();
        expect(failedChecksCheckbox).toBeInTheDocument();
        expect(afterDaysCheckbox).toBeInTheDocument();
        expect(daysInput).toBeInTheDocument();
      });

      it('shows advanced options for Azure', async () => {
        renderComponent();
        await selectProvider('azure');

        const advancedOptionsSection = screen.queryByText(/Advanced .* Options/);
        const failedChecksCheckbox = screen.queryByText('Close AutoPRs when one or more required checks fail');
        const afterDaysCheckbox = screen.queryByText(/Close AutoPRs that have not been merged or closed after:/);
        const daysInput = screen.queryByPlaceholderText('Ex. 7');

        expect(advancedOptionsSection).toBeInTheDocument();
        expect(failedChecksCheckbox).not.toBeInTheDocument();
        expect(afterDaysCheckbox).toBeInTheDocument();
        expect(daysInput).toBeInTheDocument();
      });

      it('shows advanced options for Bitbucket', async () => {
        renderComponent();
        await selectProvider('bitbucket');

        const advancedOptionsSection = screen.queryByText(/Advanced .* Options/);
        const failedChecksCheckbox = screen.queryByText('Close AutoPRs when one or more required checks fail');
        const afterDaysCheckbox = screen.queryByText(/Close AutoPRs that have not been merged or closed after:/);
        const daysInput = screen.queryByPlaceholderText('Ex. 7');

        expect(advancedOptionsSection).toBeInTheDocument();
        expect(afterDaysCheckbox).toBeInTheDocument();
        expect(daysInput).toBeInTheDocument();
        expect(failedChecksCheckbox).not.toBeInTheDocument();
      });

      it('hides advanced options for GitHub when saas-lifecycle-scm-prs-enabled is false', async () => {
        const preloadedState = clone(defaultPreloadedState);
        preloadedState.productFeatures.productFeatures['saas-lifecycle-scm-prs-enabled'] = false;
        renderComponent(preloadedState);
        await selectProvider('github');

        const advancedOptionsSection = screen.queryByText(/Advanced Github Options/);
        const failedChecksCheckbox = screen.queryByText('Close AutoPRs when one or more required checks fail');
        const afterDaysCheckbox = screen.queryByText(/Close AutoPRs that have not been merged or closed after:/);
        const daysInput = screen.queryByPlaceholderText('Ex. 7');

        expect(advancedOptionsSection).not.toBeInTheDocument();
        expect(failedChecksCheckbox).not.toBeInTheDocument();
        expect(afterDaysCheckbox).not.toBeInTheDocument();
        expect(daysInput).not.toBeInTheDocument();
      });
    });

    describe('Advanced options state management', () => {
      it('Advanced git options is enabled when remediationPullRequestsEnabled is enabled', async () => {
        renderComponent();

        const providerSelector = await screen.findByRole('combobox');
        fireEvent.change(providerSelector, { target: { value: 'github' } });

        let advancedOptionsSection = screen.queryByText(/Advanced Github Options/);
        expect(advancedOptionsSection).toBeInTheDocument();

        const failedChecksCheckbox = screen.getByRole('checkbox', {
          name: 'Close AutoPRs when one or more required checks fail',
        });
        const afterDaysCheckbox = screen.getByRole('checkbox', {
          name: /Close AutoPRs that have not been merged or closed after:/,
        });
        const daysInput = screen.getByPlaceholderText('Ex. 7');

        expect(failedChecksCheckbox).toBeDisabled();
        expect(afterDaysCheckbox).toBeDisabled();
        expect(daysInput).toBeDisabled();
      });
    });

    describe('Token field visibility with GitHub App feature', () => {
      it('hides token field when feature enabled and no provider selected', async () => {
        const preloadedState = clone(defaultPreloadedState);
        preloadedState.productFeatures.productFeatures['github-app-authentication'] = true;

        const configResponse = {
          ...defaultRootOrgConfigResponse,
          provider: { value: null, parentValue: null, parentName: null },
        };

        axiosMock.onGet(getCompositeSourceControlUrl(ownerType, ownerId)).reply(200, configResponse);

        renderComponent(preloadedState);

        // Wait for form to load
        await screen.findByRole('button', { name: 'Create' });

        // Token field should be hidden (look for Access Token label)
        expect(screen.queryByLabelText(/Access Token/i)).not.toBeInTheDocument();
        // GitHub App auth should also be hidden (no provider selected)
        expect(screen.queryByText(/GitHub App/)).not.toBeInTheDocument();
      });

      it('shows token field when feature disabled and no provider selected', async () => {
        const preloadedState = clone(defaultPreloadedState);
        // GitHub App feature is not in productFeatures (disabled)

        const configResponse = {
          ...defaultRootOrgConfigResponse,
          provider: { value: null, parentValue: null, parentName: null },
        };

        axiosMock.onGet(getCompositeSourceControlUrl(ownerType, ownerId)).reply(200, configResponse);

        renderComponent(preloadedState);

        // Wait for form to load
        await screen.findByRole('button', { name: 'Create' });

        // Token field should be visible when feature is disabled
        expect(screen.getByLabelText(/Access Token/i)).toBeInTheDocument();
      });

      it('requires a personal access token when feature is disabled and an existing GitHub App configuration has no token', async () => {
        const preloadedState = clone(defaultPreloadedState);

        axiosMock.onPut(getSourceControlUrl(ownerType, ownerId)).reply(200);
        axiosMock.onGet(getCompositeSourceControlUrl(ownerType, ownerId)).reply(200, {
          ...existingRootOrgConfigResponse,
          provider: { value: 'github', parentValue: null, parentName: null },
          token: { value: null, parentValue: null, parentName: null },
          authenticationType: { value: AUTHENTICATION_TYPES.GITHUB_APP, parentValue: null, parentName: null },
          githubApp: {
            value: {
              installationId: '12345',
              name: 'Root GitHub App',
              accountName: 'root-org',
            },
            parentValue: null,
            parentName: null,
          },
        });

        renderComponent(preloadedState);

        const submitButton = await screen.findByRole('button', { name: 'Update' });
        fireEvent.click(submitButton);

        expect(axiosMock.history.post.length).toBe(0);

        const tokenInput = screen.getByLabelText(/Access Token/i);
        fireEvent.change(tokenInput, { target: { value: '   ' } });
        fireEvent.click(submitButton);

        expect(axiosMock.history.put.length).toBe(0);

        fireEvent.change(tokenInput, { target: { value: 'root-local-token' } });
        fireEvent.click(submitButton);

        await waitFor(() => expect(axiosMock.history.put.length).toBe(1));
        expect(JSON.parse(axiosMock.history.put[0].data)).toMatchObject({
          provider: 'github',
          token: 'root-local-token',
          authenticationType: null,
        });
      });

      it('shows token field when non-GitHub provider selected and feature enabled', async () => {
        const preloadedState = clone(defaultPreloadedState);
        preloadedState.productFeatures.productFeatures['github-app-authentication'] = true;

        const configResponse = {
          ...defaultRootOrgConfigResponse,
          provider: { value: 'bitbucket', parentValue: null, parentName: null },
        };

        axiosMock.onGet(getCompositeSourceControlUrl(ownerType, ownerId)).reply(200, configResponse);

        renderComponent(preloadedState);

        // Wait for form to load
        await screen.findByRole('button', { name: 'Create' });

        // Token field should be visible
        expect(screen.getByLabelText(/Access Token/i)).toBeInTheDocument();
        // GitHub App auth should be hidden
        expect(screen.queryByText(/GitHub App/)).not.toBeInTheDocument();
      });
    });

    describe('GitHub App success flow', () => {
      beforeEach(() => {
        sessionStorage.clear();
      });

      it('shows create guidance for first-time root setup after returning from GitHub and does not show replacement alert', async () => {
        const backendGitHubAppResponse = {
          ...defaultRootOrgConfigResponse,
          provider: { value: 'github', parentValue: null, parentName: null },
          authenticationType: { value: 'GITHUB_APP', parentValue: null, parentName: null },
          githubApp: {
            value: {
              installationId: 'new-installation-id',
              name: 'sonatype-iq-server',
              accountName: 'test-org',
            },
            parentValue: null,
            parentName: null,
          },
        };

        axiosMock.onGet(getCompositeSourceControlUrl(ownerType, ownerId)).reply(200, backendGitHubAppResponse);

        sessionStorage.setItem(
          getScmFormStateStorageKey('organization', ROOT_ORGANIZATION_ID),
          JSON.stringify({
            githubApp: {
              value: null,
              isInherited: false,
              parentValue: null,
              parentName: null,
            },
          })
        );

        const preloadedState = clone(defaultPreloadedState);
        preloadedState.productFeatures.productFeatures['github-app-authentication'] = true;
        preloadedState.router.currentParams.githubAppSuccess = 'true';

        renderComponent(preloadedState);

        await screen.findByText('GitHub Setup Complete');
        expect(screen.getByRole('button', { name: 'Create' })).toBeVisible();

        const instructionAlert = screen.getByText(/source control page to apply this configuration/i);
        expect(instructionAlert).toHaveTextContent(
          /click create in the source control page to apply this configuration\./i
        );

        fireEvent.click(screen.getByRole('button', { name: 'Done' }));

        await waitFor(() => {
          expect(screen.queryByText('GitHub Setup Complete')).not.toBeInTheDocument();
        });
        expect(screen.queryByText(/The GitHub App was replaced successfully/i)).not.toBeInTheDocument();
      });

      it('shows update guidance and replacement alert after reconfiguring an existing root GitHub App', async () => {
        const backendGitHubAppResponse = {
          ...existingRootOrgConfigResponse,
          provider: { value: 'github', parentValue: null, parentName: null },
          authenticationType: { value: 'GITHUB_APP', parentValue: null, parentName: null },
          githubApp: {
            value: {
              installationId: 'new-installation-id',
              name: 'sonatype-iq-server',
              accountName: 'test-org',
            },
            parentValue: null,
            parentName: null,
          },
        };

        axiosMock.onGet(getCompositeSourceControlUrl(ownerType, ownerId)).reply(200, backendGitHubAppResponse);

        sessionStorage.setItem(
          getScmFormStateStorageKey('organization', ROOT_ORGANIZATION_ID),
          JSON.stringify({
            githubApp: {
              value: {
                installationId: 'old-installation-id',
                name: 'sonatype-iq-server',
                accountName: 'test-org',
              },
              isInherited: false,
              parentValue: null,
              parentName: null,
            },
          })
        );

        const preloadedState = clone(defaultPreloadedState);
        preloadedState.productFeatures.productFeatures['github-app-authentication'] = true;
        preloadedState.router.currentParams.githubAppSuccess = 'true';

        renderComponent(preloadedState);

        await screen.findByText('GitHub Setup Complete');
        expect(screen.getByRole('button', { name: 'Update' })).toBeVisible();

        const instructionAlert = screen.getByText(/source control page to apply this configuration/i);
        expect(instructionAlert).toHaveTextContent(
          /click update in the source control page to apply this configuration\./i
        );

        fireEvent.click(screen.getByRole('button', { name: 'Done' }));

        const replacedAlert = await screen.findByText(/The GitHub App was replaced successfully/i);
        expect(replacedAlert).toBeVisible();
      });
    });
  });

  describe('Organization', () => {
    beforeEach(() => {
      ownerType = 'organization';
      ownerId = ORGANIZATION_ID;
      defaultPreloadedState = {
        router: {
          currentParams: {
            organizationId: ORGANIZATION_ID,
          },
          currentState: {
            name: 'organization',
          },
        },
        productFeatures: {
          productFeatures: {
            notifications: true,
            automation: true,
            'saas-lifecycle-scm-prs-enabled': true,
          },
        },
        orgsAndPolicies: {
          root: {
            selectedOwner: {
              id: ORGANIZATION_ID,
              name: ORGANIZATION_NAME,
            },
          },
        },
      };

      axiosMock.onGet(getOrganizationUrl(ORGANIZATION_ID)).reply(200, organizationResponse);
      axiosMock.onGet(getCompositeSourceControlUrl(ownerType, ownerId)).reply(200, defaultOrgConfigResponse);
      axiosMock.onGet(getSourceControlMetricsUrl(ownerType, ownerId)).reply(200, { results: [] });
    });

    describe('initial source control configuration render', () => {
      it('renders loading spinner and shows all form elements on successful initial render with correct default configuration values', async () => {
        renderComponent();
        expect(screen.getByText('Loading…')).toBeVisible();

        const submitButton = await screen.findByRole('button', { name: 'Update' });
        const resetButton = screen.getByRole('button', { name: 'Reset' });

        const sourcePageTitle = screen.getByText('Source Control Configuration');
        const sourceSubtitle = screen.getByText(
          `Configures the integration with an external SCM for ${ORGANIZATION_NAME}`
        );
        expect(sourcePageTitle).toBeVisible();
        expect(sourceSubtitle).toBeVisible();

        await testSourceControlContainers(assertionsForOrgDefaultState);

        expect(resetButton).toBeVisible();
        expect(resetButton).toBeDisabled();
        expect(submitButton).toBeVisible();
        expect(submitButton).not.toBeDisabled();
      });

      it('shows all form elements on successful initial render with correct inherited configuration values', async () => {
        axiosMock.onGet(getCompositeSourceControlUrl(ownerType, ownerId)).reply(200, inheritedOrgConfigResponse);
        renderComponent();

        const submitButton = await screen.findByRole('button', { name: 'Update' });
        const resetButton = screen.getByRole('button', { name: 'Reset' });

        await testSourceControlContainers(assertionsForOrgInheritedState);

        expect(resetButton).toBeVisible();
        expect(resetButton).toBeDisabled();
        expect(submitButton).toBeVisible();
        expect(submitButton).not.toBeDisabled();
      });

      it('shows all form elements on successful initial render with correct existing configuration values', async () => {
        axiosMock.onGet(getCompositeSourceControlUrl(ownerType, ownerId)).reply(200, existingOrgConfigResponse);
        renderComponent();

        const submitButton = await screen.findByRole('button', { name: 'Update' });
        const resetButton = screen.getByRole('button', { name: 'Reset' });

        await testSourceControlContainers(assertionsForOrgExistingState);

        expect(resetButton).toBeVisible();
        expect(resetButton).not.toBeDisabled();
        expect(submitButton).toBeVisible();
        expect(submitButton).not.toBeDisabled();
      });

      it('renders error message and retry button when loading source control configuration fails', async () => {
        axiosMock.onGet(getCompositeSourceControlUrl(ownerType, ownerId)).reply(500, 'Error');
        renderComponent();
        expect(await screen.findByRole('alert')).toBeVisible();
        expect(screen.getByRole('button', { name: 'Retry' })).toBeVisible();
        expect(screen.getByText('An error occurred loading data. Error')).toBeVisible();
      });

      it('shows No license error, when user does not have license', async () => {
        const preloadedState = clone(defaultPreloadedState);
        preloadedState.productFeatures.productFeatures = {
          notifications: false,
          automation: false,
        };
        renderComponent(preloadedState);
        expect(await screen.findByRole('alert')).toBeVisible();
        const errorAlert = screen.getByText(SOURCE_CONTROL_UNSUPPORTED_MESSAGE);
        expect(errorAlert).toBeVisible();
      });
    });

    describe('Reset modal', () => {
      beforeEach(() => {
        axiosMock.onDelete(getSourceControlUrl(ownerType, ownerId)).reply(200);
      });

      it('shows disabled Reset button until form submitting(rely on sourceControl.id)', async () => {
        renderComponent();
        const resetBtn = await screen.findByRole('button', { name: 'Reset' });
        expect(resetBtn).toBeDisabled();
      });

      it('enables reset button and shows reset modal if user has source control configuration(sourceControl.id)', async () => {
        axiosMock.onGet(getCompositeSourceControlUrl(ownerType, ownerId)).reply(200, existingOrgConfigResponse);
        renderComponent();
        const resetBtn = await screen.findByRole('button', { name: 'Reset' });
        expect(resetBtn).not.toBeDisabled();
        fireEvent.click(resetBtn);
        const resetModalText = screen.getByText(
          `You are about to reset the Source Control configuration for ${ORGANIZATION_NAME}. This action cannot be undone.`
        );
        const submitBtn = screen.getByRole('button', { name: 'Continue' });
        const cancelBtn = screen.getByRole('button', { name: 'Cancel' });
        expect(screen.getByRole('alert')).toBeVisible();
        expect(resetModalText).toBeVisible();
        expect(submitBtn).toBeVisible();
        expect(cancelBtn).toBeVisible();
      });

      it('resets configuration while clicking on Continue in Reset modal', async () => {
        axiosMock.onGet(getCompositeSourceControlUrl(ownerType, ownerId)).reply(200, existingOrgConfigResponse);
        renderComponent();
        let resetBtn = await screen.findByRole('button', { name: 'Reset' });
        fireEvent.click(resetBtn);
        const submitModalBtn = screen.getByRole('button', { name: 'Continue' });
        fireEvent.click(submitModalBtn);
        expect(axiosMock.history.delete.length).toBe(1);
        expect(axiosMock.history.delete[0].url).toBe(getSourceControlUrl(ownerType, ownerId));
      });

      it('renders error message and retry button when resetting configuration fails', async () => {
        axiosMock.onGet(getCompositeSourceControlUrl(ownerType, ownerId)).reply(200, existingOrgConfigResponse);
        axiosMock.onDelete(getSourceControlUrl(ownerType, ownerId)).reply(500, 'Error');
        renderComponent();
        const resetBtn = await screen.findByRole('button', { name: 'Reset' });
        fireEvent.click(resetBtn);
        const submitModalBtn = screen.getByRole('button', { name: 'Continue' });
        fireEvent.click(submitModalBtn);
        expect(await screen.findByRole('alert')).toBeVisible();
        expect(screen.getByRole('button', { name: 'Retry' })).toBeVisible();
        expect(screen.getByText('An error occurred saving data. Error')).toBeVisible();
      });
    });

    describe('submitting and changing the form', () => {
      it('shows userName input field only if Azure DevOps or Bitbucket providers are chosen', async () => {
        renderComponent();
        const [providerContainer] = await screen.findAllByRole('group');
        const [, override] = within(providerContainer).getAllByRole('radio');
        fireEvent.click(override);
        const providerSelector = screen.getByRole('combobox');
        let userNameInput = screen.queryByRole('textbox', { name: 'Username' });
        expect(userNameInput).not.toBeInTheDocument();
        fireEvent.change(providerSelector, { target: { value: 'azure' } });
        userNameInput = screen.getByRole('textbox', { name: 'Username' });
        expect(userNameInput).toBeVisible();
        fireEvent.change(providerSelector, { target: { value: 'github' } });
        userNameInput = screen.queryByRole('textbox', { name: 'Username' });
        expect(userNameInput).not.toBeInTheDocument();
        fireEvent.change(providerSelector, { target: { value: 'bitbucket' } });
        userNameInput = screen.getByRole('textbox', { name: 'Username' });
        expect(userNameInput).toBeVisible();
        fireEvent.change(providerSelector, { target: { value: 'gitlab' } });
        userNameInput = screen.queryByRole('textbox', { name: 'Username' });
        expect(userNameInput).not.toBeInTheDocument();
      });

      it('enables text fields and select dropdown on override options', async () => {
        axiosMock.onGet(getCompositeSourceControlUrl(ownerType, ownerId)).reply(200, inheritedOrgConfigResponse);
        renderComponent();
        const [providerContainer, credentialsContainer, branchContainer] = await screen.findAllByRole('group');
        const [, providerOverride] = within(providerContainer).getAllByRole('radio');
        const [, credentialsOverride] = within(credentialsContainer).getAllByRole('radio');
        const [, branchOverride] = within(branchContainer).getAllByRole('radio');

        let providerSelector = within(providerContainer).getByRole('combobox');
        let tokenField = within(credentialsContainer).getByLabelText('Access Token');
        let branchField = within(branchContainer).getByRole('textbox');

        expect(providerSelector).toBeDisabled();
        expect(tokenField).toBeDisabled();
        expect(branchField).toBeDisabled();

        fireEvent.click(credentialsOverride);
        tokenField = within(credentialsContainer).getByLabelText('Access Token');
        expect(tokenField).not.toBeDisabled();

        fireEvent.click(branchOverride);
        branchField = within(branchContainer).getByRole('textbox');
        expect(branchField).not.toBeDisabled();

        fireEvent.click(providerOverride);

        providerSelector = within(providerContainer).getByRole('combobox');
        tokenField = within(credentialsContainer).getByLabelText('Access Token');

        expect(providerSelector).not.toBeDisabled();

        fireEvent.change(providerSelector, { target: { value: 'github' } });

        expect(tokenField).not.toBeDisabled();
      });

      it('submits new configuration (Post request) if there is no entity before', async () => {
        const submitData = {
          authenticationType: null,
          provider: null,
          username: null,
          token: null,
          baseBranch: null,
          remediationPullRequestsEnabled: null,
          statusChecksEnabled: true,
          pullRequestCommentingEnabled: null,
          commitStatusEnabled: null,
          sourceControlEvaluationsEnabled: null,
          sshEnabled: true,
          manualPullRequestsEnabled: null,
          innerSourceAutomatedUpdatesEnabled: null,
          closePrOnFailedChecksEnabled: null,
          closePrAfterDaysOpenEnabled: false,
          closePrAfterDays: null,
        };
        axiosMock.onPost(getSourceControlUrl(ownerType, ownerId), submitData).reply(200);
        axiosMock.onGet(getCompositeSourceControlUrl(ownerType, ownerId)).reply(200, inheritedOrgConfigResponse);
        renderComponent();
        const [, , , sshContainer] = await screen.findAllByRole('group');
        const [, enabled] = within(sshContainer).getAllByRole('radio');
        fireEvent.click(enabled);
        const submitButton = screen.getByRole('button', { name: 'Update' });
        fireEvent.click(submitButton);
        const submitting = screen.getByText('Submitting…');
        expect(submitting).toBeVisible();
        const success = await screen.findByText('Success!');
        expect(success).toBeVisible();
      });

      it('updates configuration(Put request) if there is an entity before', async () => {
        const submitData = {
          authenticationType: null,
          provider: 'github',
          username: null,
          token: '#~FAKE~SECRET~KEY~#',
          baseBranch: 'master',
          remediationPullRequestsEnabled: true,
          statusChecksEnabled: true,
          pullRequestCommentingEnabled: false,
          commitStatusEnabled: false,
          sourceControlEvaluationsEnabled: false,
          sshEnabled: true,
          manualPullRequestsEnabled: false,
          innerSourceAutomatedUpdatesEnabled: false,
          closePrOnFailedChecksEnabled: false,
          closePrAfterDaysOpenEnabled: false,
          closePrAfterDays: null,
        };
        axiosMock.onPut(getSourceControlUrl(ownerType, ownerId), submitData).reply(200);
        axiosMock.onGet(getCompositeSourceControlUrl(ownerType, ownerId)).reply(200, existingOrgConfigResponse);
        renderComponent();
        const [, , , sshContainer] = await screen.findAllByRole('group');
        const [, enabled] = within(sshContainer).getAllByRole('radio');
        fireEvent.click(enabled);
        const submitButton = screen.getByRole('button', { name: 'Update' });
        fireEvent.click(submitButton);
        const submitting = screen.getByText('Submitting…');
        expect(submitting).toBeVisible();
        await screen.findByRole('button', { name: 'Update' }, { timeout: 3000 });
      });

      it('shows validation error and prevent from submitting if field is not valid', async () => {
        axiosMock.onGet(getCompositeSourceControlUrl(ownerType, ownerId)).reply(200, inheritedOrgConfigResponse);
        renderComponent();
        const [, , sshContainer] = await screen.findAllByRole('group');
        const [, enabled] = within(sshContainer).getAllByRole('radio');
        fireEvent.click(enabled);
        const submitButton = screen.getByRole('button', { name: 'Update' });
        fireEvent.click(submitButton);
        const alerts = await screen.findAllByRole('alert');
        expect(alerts.length).toBe(2);
        const inputValidationMsg = screen.getByText('Must be non-empty');
        const footerErrorAlertMsg = screen.getByText(
          'There were validation errors. Unable to save: fields with invalid or missing data'
        );
        expect(inputValidationMsg).toBeVisible();
        expect(footerErrorAlertMsg).toBeVisible();
      });

      it('shows validation error and prevent from submitting if form is not dirty', async () => {
        axiosMock.onGet(getCompositeSourceControlUrl(ownerType, ownerId)).reply(200, inheritedOrgConfigResponse);
        renderComponent();

        const submitButton = await screen.findByRole('button', { name: 'Update' });
        fireEvent.click(submitButton);
        const footerErrorAlertMsg = screen.getByText('There were validation errors. There are no changes to save.');
        expect(footerErrorAlertMsg).toBeVisible();
      });

      it('renders error message and retry button when submitting configuration fails and clears error if provider is changed', async () => {
        const submitData = {
          authenticationType: null,
          provider: 'github',
          username: null,
          token: '#~FAKE~SECRET~KEY~#',
          baseBranch: 'master',
          remediationPullRequestsEnabled: true,
          statusChecksEnabled: true,
          pullRequestCommentingEnabled: false,
          commitStatusEnabled: false,
          sourceControlEvaluationsEnabled: false,
          sshEnabled: true,
          manualPullRequestsEnabled: false,
          innerSourceAutomatedUpdatesEnabled: false,
          closePrOnFailedChecksEnabled: false,
          closePrAfterDaysOpenEnabled: false,
          closePrAfterDays: null,
        };
        axiosMock.onPut(getSourceControlUrl(ownerType, ownerId), submitData).reply(500, 'Saving Error');
        axiosMock.onGet(getCompositeSourceControlUrl(ownerType, ownerId)).reply(200, existingOrgConfigResponse);
        renderComponent();
        const [, , , sshContainer] = await screen.findAllByRole('group');
        const [, enabled] = within(sshContainer).getAllByRole('radio');
        fireEvent.click(enabled);
        const submitButton = screen.getByRole('button', { name: 'Update' });
        fireEvent.click(submitButton);
        const submitting = screen.getByText('Submitting…');
        expect(submitting).toBeVisible();
        const alert = await screen.findByRole('alert');
        expect(alert).toBeVisible();
        expect(screen.getByRole('button', { name: 'Retry' })).toBeVisible();
        expect(alert.textContent).toMatch(/An error occurred saving data/i);

        const [providerContainer] = await screen.findAllByRole('group');
        let providerSelector = within(providerContainer).getByRole('combobox');
        fireEvent.change(providerSelector, { target: { value: 'github' } });
        expect(screen.queryByText(/An error occurred saving data/i)).not.toBeInTheDocument();
      });
    });

    describe('fields validation', () => {
      beforeEach(() => {
        axiosMock.onGet(getCompositeSourceControlUrl(ownerType, ownerId)).reply(200, existingOrgConfigResponse);
      });

      it('shows an error message if provider --Not Configured-- option was selected', async () => {
        renderComponent();
        const [providerContainer] = await screen.findAllByRole('group');
        const providerSelector = within(providerContainer).getByRole('combobox');
        fireEvent.change(providerSelector, { target: { value: 'gitlab' } });
        fireEvent.change(providerSelector, { target: { value: '' } });
        expect(within(providerContainer).getByText('Must be non-empty')).toBeVisible();
      });

      it("doesn't show an error message if provider doesn't require username, and token field has no value", async () => {
        renderComponent();
        const [providerContainer, credentialsContainer] = await screen.findAllByRole('group');
        const providerSelector = within(providerContainer).getByRole('combobox');
        fireEvent.change(providerSelector, { target: { value: 'gitlab' } });
        const tokenInput = within(credentialsContainer).getByLabelText('Access Token');
        fireEvent.change(tokenInput, { target: { value: '' } });
        expect(within(credentialsContainer).getByText('Must be non-empty')).toBeVisible();
      });

      it('shows an error message if provider requires username and username and/or token fields have no values', async () => {
        renderComponent();
        const [providerContainer, credentialsContainer] = await screen.findAllByRole('group');
        const providerSelector = within(providerContainer).getByRole('combobox');
        fireEvent.change(providerSelector, { target: { value: 'azure' } });
        const userNameInput = within(credentialsContainer).getByRole('textbox', { name: 'Username' });
        const tokenInput = within(credentialsContainer).getByLabelText('Access Token');
        fireEvent.change(userNameInput, { target: { value: 'admin' } });
        fireEvent.change(tokenInput, { target: { value: 'token' } });
        expect(within(credentialsContainer).queryByText('Must be non-empty')).not.toBeInTheDocument();
        fireEvent.change(userNameInput, { target: { value: '' } });
        fireEvent.change(tokenInput, { target: { value: '' } });
        expect(within(credentialsContainer).getAllByText('Must be non-empty').length).toBe(2);
      });

      it('shows an error message if provider requires username, and username OR token have no value', async () => {
        renderComponent();
        const [providerContainer, credentialsContainer] = await screen.findAllByRole('group');
        const providerSelector = within(providerContainer).getByRole('combobox');
        fireEvent.change(providerSelector, { target: { value: 'azure' } });
        const userNameInput = within(credentialsContainer).getByRole('textbox', { name: 'Username' });
        const tokenInput = within(credentialsContainer).getByLabelText('Access Token');
        fireEvent.change(userNameInput, { target: { value: 'admin' } });
        fireEvent.change(tokenInput, { target: { value: 'token' } });
        expect(within(credentialsContainer).queryByText('Must be non-empty')).not.toBeInTheDocument();
        fireEvent.change(tokenInput, { target: { value: '' } });
        expect(within(credentialsContainer).getByText('Must be non-empty')).toBeVisible();
        fireEvent.change(userNameInput, { target: { value: '' } });
        fireEvent.change(tokenInput, { target: { value: 'token' } });
        expect(within(credentialsContainer).getByText('Must be non-empty')).toBeVisible();
      });

      it('check required inputs', async () => {
        renderComponent();
        const [providerContainer, credentialsContainer, branchContainer] = await screen.findAllByRole('group');
        const [providerInherit] = within(providerContainer).getAllByRole('radio');
        fireEvent.click(providerInherit);

        const [, credentialsOverride] = within(credentialsContainer).getAllByRole('radio');
        const tokenInput = screen.getByLabelText('Access Token');
        fireEvent.click(credentialsOverride);

        expect(tokenInput).toBeRequired();

        const [, branchOverride] = within(branchContainer).getAllByRole('radio');
        const branchNameInput = within(branchContainer).getByRole('textbox');
        fireEvent.click(branchOverride);

        expect(branchNameInput).toBeRequired();

        const providerSelector = within(providerContainer).getByRole('combobox');
        fireEvent.change(providerSelector, { target: { value: 'azure' } });
        const userNameInput = screen.getByRole('textbox', { name: 'Username' });
        fireEvent.change(tokenInput, { target: { value: 'token123' } });

        expect(userNameInput).toBeRequired();
        expect(tokenInput).toBeRequired();
      });
    });

    describe('GitHub App Authentication', () => {
      beforeEach(() => {
        // Set up GitHub provider with overriding configuration
        axiosMock.onGet(getCompositeSourceControlUrl(ownerType, ownerId)).reply(200, {
          ...existingOrgConfigResponse,
          provider: { value: 'github', parentValue: 'azure', parentName: ROOT_ORGANIZATION_NAME },
        });
      });

      it('shows GitHub App authentication method when feature is enabled and provider is GitHub with override', async () => {
        const preloadedState = {
          ...defaultPreloadedState,
          productFeatures: {
            productFeatures: {
              ...defaultPreloadedState.productFeatures.productFeatures,
              'github-app-authentication': true,
            },
          },
        };

        renderComponent(preloadedState);

        await screen.findByRole('button', { name: 'Update' });

        // Should show GitHub App authentication options
        const authMethodFieldset = screen.getByRole('group', { name: 'Authentication Method' });
        expect(authMethodFieldset).toBeVisible();

        // Should show both authentication method options
        const githubAppRadio = within(authMethodFieldset).getByRole('radio', {
          name: 'GitHub App (Recommended)',
        });
        const patRadio = within(authMethodFieldset).getByRole('radio', { name: 'Personal Access Token' });

        expect(githubAppRadio).toBeVisible();
        expect(patRadio).toBeVisible();
      });

      it('shows standard token authentication when feature flag is disabled', async () => {
        const preloadedState = {
          ...defaultPreloadedState,
          productFeatures: {
            productFeatures: {
              ...defaultPreloadedState.productFeatures.productFeatures,
              'github-app-authentication': false,
            },
          },
        };

        renderComponent(preloadedState);

        await screen.findByRole('button', { name: 'Update' });

        // Should NOT show Authentication Method fieldset
        expect(screen.queryByRole('group', { name: 'Authentication Method' })).not.toBeInTheDocument();

        // Should show standard token input field
        const tokenInputWrapper = screen.getByTestId('token-input');
        expect(tokenInputWrapper).toBeVisible();
        const tokenInput = tokenInputWrapper.querySelector('input');
        expect(tokenInput).toHaveAttribute('type', 'password');
      });

      it('shows standard token authentication for non-GitHub providers', async () => {
        axiosMock.onGet(getCompositeSourceControlUrl(ownerType, ownerId)).reply(200, {
          ...existingOrgConfigResponse,
          provider: { value: 'azure', parentValue: null, parentName: null },
        });

        const preloadedState = {
          ...defaultPreloadedState,
          productFeatures: {
            productFeatures: {
              ...defaultPreloadedState.productFeatures.productFeatures,
              'github-app-authentication': true,
            },
          },
        };

        renderComponent(preloadedState);

        await screen.findByRole('button', { name: 'Update' });

        // Should NOT show Authentication Method fieldset for non-GitHub provider
        expect(screen.queryByRole('group', { name: 'Authentication Method' })).not.toBeInTheDocument();

        // Should show standard token input field
        const tokenInput = screen.getByTestId('token-input');
        expect(tokenInput).toBeVisible();
      });

      it('shows standard token authentication when both provider and token are inherited', async () => {
        axiosMock.onGet(getCompositeSourceControlUrl(ownerType, ownerId)).reply(200, inheritedOrgConfigResponse);

        const preloadedState = {
          ...defaultPreloadedState,
          productFeatures: {
            productFeatures: {
              ...defaultPreloadedState.productFeatures.productFeatures,
              'github-app-authentication': true,
            },
          },
        };

        renderComponent(preloadedState);

        await screen.findByRole('button', { name: 'Update' });

        // Should NOT show Authentication Method fieldset when fully inherited
        expect(screen.queryByRole('group', { name: 'Authentication Method' })).not.toBeInTheDocument();

        // Should show standard token input field
        const tokenInput = screen.getByTestId('token-input');
        expect(tokenInput).toBeVisible();
      });

      it('shows a fresh GitHub App install as a pending local change on the first return from registration', async () => {
        const preloadedState = {
          ...defaultPreloadedState,
          router: {
            ...defaultPreloadedState.router,
            currentParams: {
              ...defaultPreloadedState.router.currentParams,
              githubAppSuccess: 'true',
            },
          },
          productFeatures: {
            productFeatures: {
              ...defaultPreloadedState.productFeatures.productFeatures,
              'github-app-authentication': true,
            },
          },
        };
        const savedStateKey = getScmFormStateStorageKey(ownerType, ownerId);
        const savedState = compositeSourceControlToModel(
          {
            ...defaultOrgConfigResponse,
            provider: { value: 'github', parentValue: 'azure', parentName: ROOT_ORGANIZATION_NAME },
            authenticationType: {
              value: AUTHENTICATION_TYPES.GITHUB_APP,
              parentValue: null,
              parentName: ROOT_ORGANIZATION_NAME,
            },
          },
          false
        );

        delete savedState.token;

        sessionStorage.setItem(savedStateKey, JSON.stringify(savedState));

        axiosMock.onGet(getCompositeSourceControlUrl(ownerType, ownerId)).reply(200, {
          ...defaultOrgConfigResponse,
          githubApp: {
            value: {
              installationId: 67890,
              name: 'Pending GitHub App',
              accountName: 'pending-org',
            },
            parentValue: null,
            parentName: null,
          },
        });

        renderComponent(preloadedState);

        await screen.findByRole('button', { name: 'Update' });

        const authMethodFieldset = screen.getByRole('group', { name: 'Authentication Method' });
        expect(
          within(authMethodFieldset).getByRole('radio', {
            name: 'GitHub App (Recommended)',
          })
        ).toBeChecked();
        expect(screen.getAllByText('Pending GitHub App').length).toBeGreaterThan(0);
        expect(screen.getByRole('link', { name: 'Go to GitHub Installation Settings' })).toBeVisible();
        expect(screen.getByRole('button', { name: 'Reconfigure' })).toBeVisible();
        expect(
          screen.queryByText('GitHub App is already configured. No additional changes to save.')
        ).not.toBeInTheDocument();
        expect(sessionStorage.getItem(savedStateKey)).toBeNull();
      });

      it('does not surface an uncommitted GitHub App on a later revisit without the return context', async () => {
        axiosMock.onGet(getCompositeSourceControlUrl(ownerType, ownerId)).reply(200, {
          ...existingOrgConfigResponse,
          provider: { value: 'github', parentValue: 'azure', parentName: ROOT_ORGANIZATION_NAME },
          authenticationType: {
            value: AUTHENTICATION_TYPES.PAT,
            parentValue: null,
            parentName: ROOT_ORGANIZATION_NAME,
          },
          githubApp: {
            value: {
              installationId: '67890',
              name: 'Uncommitted GitHub App',
              accountName: 'org-account',
            },
            parentValue: null,
            parentName: null,
          },
        });

        const preloadedState = {
          ...defaultPreloadedState,
          productFeatures: {
            productFeatures: {
              ...defaultPreloadedState.productFeatures.productFeatures,
              'github-app-authentication': true,
            },
          },
        };

        renderComponent(preloadedState);

        await screen.findByRole('button', { name: 'Update' });

        const authMethodFieldset = screen.getByRole('group', { name: 'Authentication Method' });
        expect(within(authMethodFieldset).getByRole('radio', { name: 'Personal Access Token' })).toBeChecked();
        expect(within(authMethodFieldset).getByLabelText('Access Token')).toBeVisible();
        expect(screen.queryByText('Uncommitted GitHub App')).not.toBeInTheDocument();
        expect(screen.queryByRole('link', { name: 'Go to GitHub Installation Settings' })).not.toBeInTheDocument();
        expect(screen.queryByRole('button', { name: 'Reconfigure' })).not.toBeInTheDocument();
      });

      it('allows selecting Personal Access Token authentication method', async () => {
        const preloadedState = {
          ...defaultPreloadedState,
          productFeatures: {
            productFeatures: {
              ...defaultPreloadedState.productFeatures.productFeatures,
              'github-app-authentication': true,
            },
          },
        };

        renderComponent(preloadedState);

        await screen.findByRole('button', { name: 'Update' });

        const authMethodFieldset = screen.getByRole('group', { name: 'Authentication Method' });
        const patRadio = within(authMethodFieldset).getByRole('radio', { name: 'Personal Access Token' });

        fireEvent.click(patRadio);

        expect(patRadio).toBeChecked();

        // Should show Access Token input field
        const tokenInput = within(authMethodFieldset).getByLabelText('Access Token');
        expect(tokenInput).toBeVisible();
        expect(tokenInput).toHaveAttribute('type', 'password');
      });

      it('allows selecting GitHub App authentication method', async () => {
        const preloadedState = {
          ...defaultPreloadedState,
          productFeatures: {
            productFeatures: {
              ...defaultPreloadedState.productFeatures.productFeatures,
              'github-app-authentication': true,
            },
          },
        };

        renderComponent(preloadedState);

        await screen.findByRole('button', { name: 'Update' });

        const authMethodFieldset = screen.getByRole('group', { name: 'Authentication Method' });
        const githubAppRadio = within(authMethodFieldset).getByRole('radio', {
          name: 'GitHub App (Recommended)',
        });

        fireEvent.click(githubAppRadio);

        expect(githubAppRadio).toBeChecked();

        // Should show Configure button (when not configured)
        const configureButton = within(authMethodFieldset).getByRole('button', { name: 'Configure GitHub App' });
        expect(configureButton).toBeVisible();
      });

      it('pre-selects GitHub App method when authenticationType is GITHUB_APP', async () => {
        axiosMock.onGet(getCompositeSourceControlUrl(ownerType, ownerId)).reply(200, {
          ...existingOrgConfigResponse,
          provider: { value: 'github', parentValue: 'azure', parentName: ROOT_ORGANIZATION_NAME },
          authenticationType: { value: 'GITHUB_APP', parentValue: null, parentName: null },
        });

        const preloadedState = {
          ...defaultPreloadedState,
          productFeatures: {
            productFeatures: {
              ...defaultPreloadedState.productFeatures.productFeatures,
              'github-app-authentication': true,
            },
          },
        };

        renderComponent(preloadedState);

        await screen.findByRole('button', { name: 'Update' });

        const authMethodFieldset = screen.getByRole('group', { name: 'Authentication Method' });
        const githubAppRadio = within(authMethodFieldset).getByRole('radio', {
          name: 'GitHub App (Recommended)',
        });

        // GitHub App method should be pre-selected
        expect(githubAppRadio).toBeChecked();

        // Configure button should be shown (since no installation ID is configured)
        expect(within(authMethodFieldset).getByRole('button', { name: 'Configure GitHub App' })).toBeVisible();
      });
    });

    describe('Token field visibility with GitHub App feature', () => {
      it('hides token field when feature enabled and no provider (neither inherited nor local)', async () => {
        const preloadedState = clone(defaultPreloadedState);
        preloadedState.productFeatures.productFeatures['github-app-authentication'] = true;

        const configResponse = {
          ...defaultOrgConfigResponse,
          provider: { value: null, parentValue: null, parentName: null },
        };

        axiosMock.onGet(getCompositeSourceControlUrl(ownerType, ownerId)).reply(200, configResponse);

        renderComponent(preloadedState);

        // Wait for form to load
        await screen.findByRole('button', { name: 'Update' });

        // Token field should be hidden
        expect(screen.queryByTestId('token-input')).not.toBeInTheDocument();
        // GitHub App auth should also be hidden (no provider selected)
        expect(screen.queryByText(/GitHub App/)).not.toBeInTheDocument();
      });

      it('shows token field when feature enabled and provider inherited from parent', async () => {
        const preloadedState = clone(defaultPreloadedState);
        preloadedState.productFeatures.productFeatures['github-app-authentication'] = true;

        const configResponse = {
          ...defaultOrgConfigResponse,
          provider: { value: null, parentValue: 'gitlab', parentName: ROOT_ORGANIZATION_NAME },
        };

        axiosMock.onGet(getCompositeSourceControlUrl(ownerType, ownerId)).reply(200, configResponse);

        renderComponent(preloadedState);

        // Wait for form to load
        await screen.findByRole('button', { name: 'Update' });

        // Token field should be visible for non-GitHub provider (org level uses testid)
        expect(screen.getByTestId('token-input')).toBeInTheDocument();
      });

      it('shows GitHub App auth when provider overridden to GitHub', async () => {
        const preloadedState = clone(defaultPreloadedState);
        preloadedState.productFeatures.productFeatures['github-app-authentication'] = true;

        const configResponse = {
          ...defaultOrgConfigResponse,
          provider: { value: 'github', parentValue: null, parentName: null },
        };

        axiosMock.onGet(getCompositeSourceControlUrl(ownerType, ownerId)).reply(200, configResponse);

        renderComponent(preloadedState);

        // Wait for form to load
        await screen.findByRole('button', { name: 'Update' });

        // GitHub App auth should be visible
        expect(screen.getByText(/GitHub App/)).toBeInTheDocument();
        // Token field should be hidden
        expect(screen.queryByTestId('token-input')).not.toBeInTheDocument();
      });

      it('shows token field when provider overridden to non-GitHub', async () => {
        const preloadedState = clone(defaultPreloadedState);
        preloadedState.productFeatures.productFeatures['github-app-authentication'] = true;

        const configResponse = {
          ...defaultOrgConfigResponse,
          provider: { value: 'bitbucket', parentValue: null, parentName: null },
        };

        axiosMock.onGet(getCompositeSourceControlUrl(ownerType, ownerId)).reply(200, configResponse);

        renderComponent(preloadedState);

        // Wait for form to load
        await screen.findByRole('button', { name: 'Update' });

        // Token field should be visible
        expect(screen.getByTestId('token-input')).toBeInTheDocument();
        // GitHub App auth should be hidden
        expect(screen.queryByText(/GitHub App/)).not.toBeInTheDocument();
      });
    });
  });

  describe('Application', () => {
    beforeEach(() => {
      ownerType = 'application';
      ownerId = APPLICATION_ID;
      defaultPreloadedState = {
        router: {
          currentParams: {
            applicationPublicId: APPLICATION_ID,
          },
          currentState: {
            name: 'application',
          },
        },
        productFeatures: {
          productFeatures: {
            notifications: true,
            automation: true,
            'saas-lifecycle-scm-prs-enabled': true,
          },
        },
        orgsAndPolicies: {
          root: {
            selectedOwner: {
              id: APPLICATION_ID,
              name: APPLICATION_NAME,
            },
          },
        },
      };

      axiosMock.onGet(getApplicationSummaryUrl(APPLICATION_ID)).reply(200, applicationsResponse);
      axiosMock.onGet(getCompositeSourceControlUrl(ownerType, ownerId)).reply(200, defaultAppConfigResponse);
      axiosMock.onGet(getSourceControlMetricsUrl(ownerType, ownerId)).reply(200, { results: [] });
    });

    describe('initial source control configuration render', () => {
      it('renders loading spinner and shows all form elements on successful initial render with correct default configuration values', async () => {
        renderComponent();
        expect(screen.getByText('Loading…')).toBeVisible();

        const submitButton = await screen.findByRole('button', { name: 'Update' });
        const resetButton = screen.getByRole('button', { name: 'Reset' });

        const sourcePageTitle = screen.getByText('Source Control Configuration');
        const sourceSubtitle = screen.getByText(
          `Configures the integration with an external SCM for ${APPLICATION_NAME}`
        );
        expect(sourcePageTitle).toBeVisible();
        expect(sourceSubtitle).toBeVisible();

        const repositoryUrl = screen.getByRole('textbox', { name: 'Repository Clone URL' });
        expect(repositoryUrl).toBeVisible();
        expect(repositoryUrl).not.toBeDisabled();
        expect(repositoryUrl).toHaveValue('');

        await testSourceControlContainers(assertionsForAppDefaultState);

        expect(resetButton).toBeVisible();
        expect(resetButton).toBeDisabled();
        expect(submitButton).toBeVisible();
        expect(submitButton).not.toBeDisabled();
      });

      it('shows all form elements on successful initial render with correct inherited configuration values', async () => {
        axiosMock.onGet(getCompositeSourceControlUrl(ownerType, ownerId)).reply(200, inheritedAppConfigResponse);
        renderComponent();

        const submitButton = await screen.findByRole('button', { name: 'Update' });
        const resetButton = screen.getByRole('button', { name: 'Reset' });

        const repositoryUrl = screen.getByRole('textbox', { name: 'Repository Clone URL' });
        expect(repositoryUrl).toBeVisible();
        expect(repositoryUrl).not.toBeDisabled();
        expect(repositoryUrl).toHaveValue('');

        await testSourceControlContainers(assertionsForAppInheritedState);

        expect(resetButton).toBeVisible();
        expect(resetButton).toBeDisabled();
        expect(submitButton).toBeVisible();
        expect(submitButton).not.toBeDisabled();
      });

      it('shows all form elements on successful initial render with correct inherited configuration values and no token set on root', async () => {
        axiosMock.onGet(getCompositeSourceControlUrl(ownerType, ownerId)).reply(200, inheritedAppNoTokenConfigResponse);
        renderComponent();

        const submitButton = await screen.findByRole('button', { name: 'Update' });
        const resetButton = screen.getByRole('button', { name: 'Reset' });

        const repositoryUrl = screen.getByRole('textbox', { name: 'Repository Clone URL' });
        expect(repositoryUrl).toBeVisible();
        expect(repositoryUrl).not.toBeDisabled();
        expect(repositoryUrl).toHaveValue('');

        await testSourceControlContainers(assertionsForAppNoTokenInheritedState);
        const [, credentialsContainer] = await screen.findAllByRole('group');
        const [inherit, override] = within(credentialsContainer).getAllByRole('radio');
        // Inherit radio is disabled because parent has no credentials to inherit (Azure provider but no username/token)
        expect(inherit).toBeDisabled();
        expect(override).not.toBeDisabled();

        expect(resetButton).toBeVisible();
        expect(resetButton).toBeDisabled();
        expect(submitButton).toBeVisible();
        expect(submitButton).not.toBeDisabled();
      });

      it('shows all form elements on successful initial render with correct existing configuration values', async () => {
        axiosMock.onGet(getCompositeSourceControlUrl(ownerType, ownerId)).reply(200, existingAppConfigResponse);
        renderComponent();

        const submitButton = await screen.findByRole('button', { name: 'Update' });
        const resetButton = screen.getByRole('button', { name: 'Reset' });

        const repositoryUrl = screen.getByRole('textbox', { name: 'Repository Clone URL' });
        expect(repositoryUrl).toBeVisible();
        expect(repositoryUrl).not.toBeDisabled();
        expect(repositoryUrl).toHaveValue('https://github.com');

        await testSourceControlContainers(assertionsForAppExistingState);

        expect(resetButton).toBeVisible();
        expect(resetButton).not.toBeDisabled();
        expect(submitButton).toBeVisible();
        expect(submitButton).not.toBeDisabled();
      });

      it('renders error message and retry button when loading source control configuration fails', async () => {
        axiosMock.onGet(getCompositeSourceControlUrl(ownerType, ownerId)).reply(500, 'Error');
        renderComponent();
        expect(await screen.findByRole('alert')).toBeVisible();
        expect(screen.getByRole('button', { name: 'Retry' })).toBeVisible();
        expect(screen.getByText('An error occurred loading data. Error')).toBeVisible();
      });

      it('shows No license error, when user does not have license', async () => {
        const preloadedState = clone(defaultPreloadedState);
        preloadedState.productFeatures.productFeatures = {
          notifications: false,
          automation: false,
        };
        renderComponent(preloadedState);
        expect(await screen.findByRole('alert')).toBeVisible();
        const errorAlert = screen.getByText(SOURCE_CONTROL_UNSUPPORTED_MESSAGE);
        expect(errorAlert).toBeVisible();
      });
    });

    describe('Reset modal', () => {
      beforeEach(() => {
        axiosMock.onDelete(getSourceControlUrl(ownerType, ownerId)).reply(200);
      });

      it('shows disabled Reset button until form submitting(rely on sourceControl.id)', async () => {
        renderComponent();
        const resetBtn = await screen.findByRole('button', { name: 'Reset' });
        expect(resetBtn).toBeDisabled();
      });

      it('enables reset button and shows reset modal if user has source control configuration(sourceControl.id)', async () => {
        axiosMock.onGet(getCompositeSourceControlUrl(ownerType, ownerId)).reply(200, existingAppConfigResponse);
        renderComponent();
        const resetBtn = await screen.findByRole('button', { name: 'Reset' });
        expect(resetBtn).not.toBeDisabled();
        fireEvent.click(resetBtn);
        const resetModalText = screen.getByText(
          `You are about to reset the Source Control configuration for ${APPLICATION_NAME}. This action cannot be undone.`
        );
        const submitBtn = screen.getByRole('button', { name: 'Continue' });
        const cancelBtn = screen.getByRole('button', { name: 'Cancel' });
        expect(screen.getByRole('alert')).toBeVisible();
        expect(resetModalText).toBeVisible();
        expect(submitBtn).toBeVisible();
        expect(cancelBtn).toBeVisible();
      });

      it('resets configuration while clicking on Continue in Reset modal', async () => {
        axiosMock.onGet(getCompositeSourceControlUrl(ownerType, ownerId)).reply(200, existingAppConfigResponse);
        renderComponent();
        let resetBtn = await screen.findByRole('button', { name: 'Reset' });
        fireEvent.click(resetBtn);
        const submitModalBtn = screen.getByRole('button', { name: 'Continue' });
        fireEvent.click(submitModalBtn);
        expect(axiosMock.history.delete.length).toBe(1);
        expect(axiosMock.history.delete[0].url).toBe(getSourceControlUrl(ownerType, ownerId));
      });

      it('renders error message and retry button when resetting configuration fails', async () => {
        axiosMock.onGet(getCompositeSourceControlUrl(ownerType, ownerId)).reply(200, existingAppConfigResponse);
        axiosMock.onDelete(getSourceControlUrl(ownerType, ownerId)).reply(500, 'Error');
        renderComponent();
        const resetBtn = await screen.findByRole('button', { name: 'Reset' });
        fireEvent.click(resetBtn);
        const submitModalBtn = screen.getByRole('button', { name: 'Continue' });
        fireEvent.click(submitModalBtn);
        expect(await screen.findByRole('alert')).toBeVisible();
        expect(screen.getByRole('button', { name: 'Retry' })).toBeVisible();
        expect(screen.getByText('An error occurred saving data. Error')).toBeVisible();
      });
    });

    describe('submitting and changing the form', () => {
      it('shows userName input field only if Azure DevOps or Bitbucket providers are chosen', async () => {
        renderComponent();
        const [providerContainer] = await screen.findAllByRole('group');
        const [, override] = within(providerContainer).getAllByRole('radio');
        fireEvent.click(override);
        const providerSelector = screen.getByRole('combobox');
        let userNameInput = screen.queryByRole('textbox', { name: 'Username' });
        expect(userNameInput).not.toBeInTheDocument();
        fireEvent.change(providerSelector, { target: { value: 'azure' } });
        userNameInput = screen.getByRole('textbox', { name: 'Username' });
        expect(userNameInput).toBeVisible();
        fireEvent.change(providerSelector, { target: { value: 'github' } });
        userNameInput = screen.queryByRole('textbox', { name: 'Username' });
        expect(userNameInput).not.toBeInTheDocument();
        fireEvent.change(providerSelector, { target: { value: 'bitbucket' } });
        userNameInput = screen.getByRole('textbox', { name: 'Username' });
        expect(userNameInput).toBeVisible();
        fireEvent.change(providerSelector, { target: { value: 'gitlab' } });
        userNameInput = screen.queryByRole('textbox', { name: 'Username' });
        expect(userNameInput).not.toBeInTheDocument();
      });

      it('enables text fields and select dropdown on override options', async () => {
        axiosMock.onGet(getCompositeSourceControlUrl(ownerType, ownerId)).reply(200, inheritedAppConfigResponse);
        renderComponent();
        const [providerContainer, credentialsContainer, branchContainer] = await screen.findAllByRole('group');
        const [, providerOverride] = within(providerContainer).getAllByRole('radio');
        const [, credentialsOverride] = within(credentialsContainer).getAllByRole('radio');
        const [, branchOverride] = within(branchContainer).getAllByRole('radio');

        let providerSelector = within(providerContainer).getByRole('combobox');
        let tokenField = within(credentialsContainer).getByLabelText('Access Token');
        let branchField = within(branchContainer).getByRole('textbox');

        expect(providerSelector).toBeDisabled();
        expect(tokenField).toBeDisabled();
        expect(branchField).toBeDisabled();

        fireEvent.click(credentialsOverride);
        tokenField = within(credentialsContainer).getByLabelText('Access Token');
        expect(tokenField).not.toBeDisabled();

        fireEvent.click(branchOverride);
        branchField = within(branchContainer).getByRole('textbox');
        expect(branchField).not.toBeDisabled();

        fireEvent.click(providerOverride);

        providerSelector = within(providerContainer).getByRole('combobox');
        tokenField = within(credentialsContainer).getByLabelText('Access Token');

        expect(providerSelector).not.toBeDisabled();

        fireEvent.change(providerSelector, { target: { value: 'github' } });

        expect(tokenField).not.toBeDisabled();
      });

      it('submits new configuration (POST request) and does not show confirmation modal if there is no entity before', async () => {
        const submitData = {
          authenticationType: null,
          provider: null,
          username: null,
          token: null,
          baseBranch: null,
          remediationPullRequestsEnabled: null,
          statusChecksEnabled: true,
          pullRequestCommentingEnabled: null,
          commitStatusEnabled: null,
          sourceControlEvaluationsEnabled: null,
          sshEnabled: true,
          repositoryUrl: 'https://www.example.com',
          manualPullRequestsEnabled: null,
          innerSourceAutomatedUpdatesEnabled: null,
          closePrOnFailedChecksEnabled: null,
          closePrAfterDaysOpenEnabled: false,
          closePrAfterDays: null,
        };
        axiosMock.onPost(getSourceControlUrl(ownerType, ownerId), submitData).reply(200);
        axiosMock.onGet(getCompositeSourceControlUrl(ownerType, ownerId)).reply(200, inheritedAppConfigResponse);
        renderComponent();
        const [, , , sshContainer] = await screen.findAllByRole('group');
        const [, enabled] = within(sshContainer).getAllByRole('radio');
        fireEvent.click(enabled);
        const repositoryUrl = screen.getByRole('textbox', { name: 'Repository Clone URL' });
        fireEvent.change(repositoryUrl, { target: { value: 'https://www.example.com' } });
        const submitButton = screen.getByRole('button', { name: 'Update' });
        fireEvent.click(submitButton);

        // modal should not appear
        const confirmationModalText = screen.queryByText(
          `Changing the repository URL will reset source control data for ${APPLICATION_NAME}. Are you sure you want to continue?`
        );
        expect(confirmationModalText).not.toBeInTheDocument();

        // submit mask
        const submitting = screen.getByText('Submitting…');
        expect(submitting).toBeVisible();
        const success = await screen.findByText('Success!');
        expect(success).toBeVisible();
      });

      it('shows confirmation modal and updates configuration (PUT request) if there is an entity before', async () => {
        const submitData = {
          authenticationType: null,
          provider: 'github',
          username: null,
          token: '#~FAKE~SECRET~KEY~#',
          baseBranch: 'master',
          remediationPullRequestsEnabled: true,
          statusChecksEnabled: true,
          pullRequestCommentingEnabled: false,
          commitStatusEnabled: false,
          sourceControlEvaluationsEnabled: false,
          sshEnabled: true,
          repositoryUrl: 'https://www.example.com',
          manualPullRequestsEnabled: false,
          innerSourceAutomatedUpdatesEnabled: false,
          closePrOnFailedChecksEnabled: false,
          closePrAfterDaysOpenEnabled: false,
          closePrAfterDays: null,
        };
        axiosMock.onPut(getSourceControlUrl(ownerType, ownerId), submitData).reply(200);
        axiosMock.onGet(getCompositeSourceControlUrl(ownerType, ownerId)).reply(200, existingAppConfigResponse);
        renderComponent();
        const [, , , sshContainer] = await screen.findAllByRole('group');
        const [, enabled] = within(sshContainer).getAllByRole('radio');
        fireEvent.click(enabled);

        // change repo url
        const repositoryUrl = screen.getByRole('textbox', { name: 'Repository Clone URL' });
        fireEvent.change(repositoryUrl, { target: { value: 'https://www.example.com' } });
        const submitButton = screen.getByRole('button', { name: 'Update' });
        fireEvent.click(submitButton);

        // expect confirmation modal and continue
        const confirmationModalText = screen.queryByText(
          `Changing the repository URL will reset source control data for ${APPLICATION_NAME}. Are you sure you want to continue?`
        );
        expect(confirmationModalText).toBeVisible();
        const submitModalButton = screen.getByRole('button', { name: 'Continue' });
        fireEvent.click(submitModalButton);

        // submit mask
        const submitting = screen.getByText('Submitting…');
        expect(submitting).toBeVisible();
        await screen.findByRole('button', { name: 'Update' }, { timeout: 3000 });
      });

      it('shows validation error and prevent from submitting if field is not valid', async () => {
        axiosMock.onGet(getCompositeSourceControlUrl(ownerType, ownerId)).reply(200, inheritedAppConfigResponse);
        renderComponent();
        const [, , , sshContainer] = await screen.findAllByRole('group');
        const [, enabled] = within(sshContainer).getAllByRole('radio');
        fireEvent.click(enabled);
        const submitButton = await screen.findByRole('button', { name: 'Update' });
        fireEvent.click(submitButton);
        const alerts = await screen.findAllByRole('alert');
        expect(alerts.length).toBe(2);
        const inputValidationMsg = screen.getByText('Must be non-empty');
        const footerErrorAlertMsg = screen.getByText(
          'There were validation errors. Unable to save: fields with invalid or missing data'
        );
        expect(inputValidationMsg).toBeVisible();
        expect(footerErrorAlertMsg).toBeVisible();
      });

      it('shows validation error and prevent from submitting if form is not dirty', async () => {
        axiosMock.onGet(getCompositeSourceControlUrl(ownerType, ownerId)).reply(200, existingAppConfigResponse);
        renderComponent();

        const submitButton = await screen.findByRole('button', { name: 'Update' });
        fireEvent.click(submitButton);
        const footerErrorAlertMsg = screen.getByText('There were validation errors. There are no changes to save.');
        expect(footerErrorAlertMsg).toBeVisible();
      });

      it('renders error message and retry button when submitting configuration fails and clears error if provider is changed', async () => {
        const submitData = {
          authenticationType: null,
          provider: 'github',
          username: null,
          token: '#~FAKE~SECRET~KEY~#',
          baseBranch: 'master',
          remediationPullRequestsEnabled: true,
          statusChecksEnabled: true,
          pullRequestCommentingEnabled: false,
          commitStatusEnabled: false,
          sourceControlEvaluationsEnabled: false,
          sshEnabled: false,
          repositoryUrl: 'https://www.example.com',
          manualPullRequestsEnabled: false,
          innerSourceAutomatedUpdatesEnabled: false,
          closePrOnFailedChecksEnabled: false,
          closePrAfterDaysOpenEnabled: false,
          closePrAfterDays: null,
        };
        axiosMock.onPut(getSourceControlUrl(ownerType, ownerId), submitData).reply(500, 'Saving Error');
        axiosMock.onGet(getCompositeSourceControlUrl(ownerType, ownerId)).reply(200, existingAppConfigResponse);
        renderComponent();
        const repositoryUrl = await screen.findByRole('textbox', { name: 'Repository Clone URL' });
        fireEvent.change(repositoryUrl, { target: { value: 'https://www.example.com' } });
        const submitButton = screen.getByRole('button', { name: 'Update' });
        fireEvent.click(submitButton);

        // expect confirmation modal and continue
        const confirmationModalText = screen.queryByText(
          `Changing the repository URL will reset source control data for ${APPLICATION_NAME}. Are you sure you want to continue?`
        );
        expect(confirmationModalText).toBeVisible();
        const submitModalButton = screen.getByRole('button', { name: 'Continue' });
        fireEvent.click(submitModalButton);

        // submit mask
        const submitting = screen.getByText('Submitting…');
        expect(submitting).toBeVisible();
        const alert = await screen.findByRole('alert');
        expect(alert).toBeVisible();
        expect(screen.getByRole('button', { name: 'Retry' })).toBeVisible();
        expect(alert.textContent).toMatch(/An error occurred saving data/i);

        const [providerContainer] = await screen.findAllByRole('group');
        let providerSelector = within(providerContainer).getByRole('combobox');
        fireEvent.change(providerSelector, { target: { value: 'github' } });
        expect(screen.queryByText(/An error occurred saving data/i)).not.toBeInTheDocument();
      });
    });

    describe('fields validation', () => {
      beforeEach(() => {
        axiosMock.onGet(getCompositeSourceControlUrl(ownerType, ownerId)).reply(200, existingAppConfigResponse);
      });

      it('shows an error message if provider --Not Configured-- option was selected', async () => {
        renderComponent();
        const [providerContainer] = await screen.findAllByRole('group');
        const providerSelector = within(providerContainer).getByRole('combobox');
        fireEvent.change(providerSelector, { target: { value: 'gitlab' } });
        fireEvent.change(providerSelector, { target: { value: '' } });
        expect(within(providerContainer).getByText('Must be non-empty')).toBeVisible();
      });

      it("shows an error message if provider doesn't require username, and token field has no value", async () => {
        renderComponent();
        const [providerContainer, credentialsContainer] = await screen.findAllByRole('group');
        const providerSelector = within(providerContainer).getByRole('combobox');
        fireEvent.change(providerSelector, { target: { value: 'gitlab' } });
        const tokenInput = within(credentialsContainer).getByLabelText('Access Token');
        fireEvent.change(tokenInput, { target: { value: '' } });
        expect(within(credentialsContainer).getByText('Must be non-empty')).toBeVisible();
      });

      it('shows an error message if provider requires username and username and/or token fields have no values', async () => {
        renderComponent();
        const [providerContainer, credentialsContainer] = await screen.findAllByRole('group');
        const providerSelector = within(providerContainer).getByRole('combobox');
        fireEvent.change(providerSelector, { target: { value: 'azure' } });
        const userNameInput = within(credentialsContainer).getByRole('textbox', { name: 'Username' });
        const tokenInput = within(credentialsContainer).getByLabelText('Access Token');
        fireEvent.change(userNameInput, { target: { value: 'admin' } });
        fireEvent.change(tokenInput, { target: { value: 'token' } });
        expect(within(credentialsContainer).queryByText('Must be non-empty')).not.toBeInTheDocument();
        fireEvent.change(userNameInput, { target: { value: '' } });
        fireEvent.change(tokenInput, { target: { value: '' } });
        expect(within(credentialsContainer).getAllByText('Must be non-empty').length).toBe(2);
      });

      it('shows an error message if provider requires username, and username OR token have no value', async () => {
        renderComponent();
        const [providerContainer, credentialsContainer] = await screen.findAllByRole('group');
        const providerSelector = within(providerContainer).getByRole('combobox');
        fireEvent.change(providerSelector, { target: { value: 'azure' } });
        const userNameInput = within(credentialsContainer).getByRole('textbox', { name: 'Username' });
        const tokenInput = within(credentialsContainer).getByLabelText('Access Token');
        fireEvent.change(userNameInput, { target: { value: 'admin' } });
        fireEvent.change(tokenInput, { target: { value: 'token' } });
        expect(within(credentialsContainer).queryByText('Must be non-empty')).not.toBeInTheDocument();
        fireEvent.change(tokenInput, { target: { value: '' } });
        expect(within(credentialsContainer).getByText('Must be non-empty')).toBeVisible();
        fireEvent.change(userNameInput, { target: { value: '' } });
        fireEvent.change(tokenInput, { target: { value: 'token' } });
        expect(within(credentialsContainer).getByText('Must be non-empty')).toBeVisible();
      });

      it('shows an error message if repo URL is empty or invalid', async () => {
        renderComponent();
        const repositoryUrl = await screen.findByRole('textbox', { name: 'Repository Clone URL' });
        fireEvent.change(repositoryUrl, { target: { value: 'some wrong url' } });
        expect(screen.getByText('A valid HTTP(S) repository clone URL is required')).toBeVisible();
        fireEvent.change(repositoryUrl, { target: { value: '' } });
        expect(screen.getByText('Must be non-empty')).toBeVisible();
      });

      it('check required inputs', async () => {
        renderComponent();
        const [providerContainer, credentialsContainer, branchContainer] = await screen.findAllByRole('group');
        const [providerInherit] = within(providerContainer).getAllByRole('radio');
        fireEvent.click(providerInherit);

        const [, credentialsOverride] = within(credentialsContainer).getAllByRole('radio');
        const tokenInput = screen.getByLabelText('Access Token');
        fireEvent.click(credentialsOverride);

        expect(tokenInput).toBeRequired();

        const [, branchOverride] = within(branchContainer).getAllByRole('radio');
        const branchNameInput = within(branchContainer).getByRole('textbox');
        fireEvent.click(branchOverride);

        expect(branchNameInput).toBeRequired();

        const providerSelector = within(providerContainer).getByRole('combobox');
        fireEvent.change(providerSelector, { target: { value: 'azure' } });
        const userNameInput = screen.getByRole('textbox', { name: 'Username' });
        fireEvent.change(tokenInput, { target: { value: 'token123' } });

        expect(userNameInput).toBeRequired();
        expect(tokenInput).toBeRequired();
      });
    });

    describe('GitHub App Authentication', () => {
      beforeEach(() => {
        // Set up GitHub provider with overriding configuration
        axiosMock.onGet(getCompositeSourceControlUrl(ownerType, ownerId)).reply(200, {
          ...existingAppConfigResponse,
          provider: { value: 'github', parentValue: 'azure', parentName: ROOT_ORGANIZATION_NAME },
        });
      });

      it('shows GitHub App authentication method when feature is enabled and provider is GitHub with override', async () => {
        const preloadedState = {
          ...defaultPreloadedState,
          productFeatures: {
            productFeatures: {
              ...defaultPreloadedState.productFeatures.productFeatures,
              'github-app-authentication': true,
            },
          },
        };

        renderComponent(preloadedState);

        await screen.findByRole('button', { name: 'Update' });

        // Should show GitHub App authentication options
        const authMethodFieldset = screen.getByRole('group', { name: 'Authentication Method' });
        expect(authMethodFieldset).toBeVisible();

        // Should show both authentication method options
        const githubAppRadio = within(authMethodFieldset).getByRole('radio', {
          name: 'GitHub App (Recommended)',
        });
        const patRadio = within(authMethodFieldset).getByRole('radio', { name: 'Personal Access Token' });

        expect(githubAppRadio).toBeVisible();
        expect(patRadio).toBeVisible();
      });

      it('shows standard token authentication when feature flag is disabled', async () => {
        const preloadedState = {
          ...defaultPreloadedState,
          productFeatures: {
            productFeatures: {
              ...defaultPreloadedState.productFeatures.productFeatures,
              'github-app-authentication': false,
            },
          },
        };

        renderComponent(preloadedState);

        await screen.findByRole('button', { name: 'Update' });

        // Should NOT show Authentication Method fieldset
        expect(screen.queryByRole('group', { name: 'Authentication Method' })).not.toBeInTheDocument();

        // Should show standard token input field
        const tokenInputWrapper = screen.getByTestId('token-input');
        expect(tokenInputWrapper).toBeVisible();
        const tokenInput = tokenInputWrapper.querySelector('input');
        expect(tokenInput).toHaveAttribute('type', 'password');
      });

      it('shows the access-token warning and blocks submit until a token is provided when feature flag is disabled', async () => {
        axiosMock.onPost(getSourceControlUrl(ownerType, ownerId)).reply(200);
        axiosMock.onGet(getCompositeSourceControlUrl(ownerType, ownerId)).reply(200, {
          ...defaultAppConfigResponse,
          provider: { value: null, parentValue: 'github', parentName: ROOT_ORGANIZATION_NAME },
          token: { value: null, parentValue: null, parentName: ROOT_ORGANIZATION_NAME },
          baseBranch: { value: null, parentValue: 'main', parentName: ROOT_ORGANIZATION_NAME },
          authenticationType: {
            value: null,
            parentValue: AUTHENTICATION_TYPES.GITHUB_APP,
            parentName: ROOT_ORGANIZATION_NAME,
          },
          githubApp: {
            value: null,
            parentValue: {
              installationId: 67890,
              name: 'Root GitHub App',
              accountName: 'root-org',
            },
            parentName: ROOT_ORGANIZATION_NAME,
          },
        });

        const preloadedState = {
          ...defaultPreloadedState,
          productFeatures: {
            productFeatures: {
              ...defaultPreloadedState.productFeatures.productFeatures,
              'github-app-authentication': false,
            },
          },
        };

        renderComponent(preloadedState);

        expect(await screen.findByText('Access Token must be configured')).toBeVisible();

        const repositoryUrl = screen.getByRole('textbox', { name: 'Repository Clone URL' });
        fireEvent.change(repositoryUrl, { target: { value: 'https://github.com/example/app-repo.git' } });

        const submitButton = screen.getByRole('button', { name: 'Update' });
        fireEvent.click(submitButton);

        expect(axiosMock.history.post.length).toBe(0);

        const tokenInput = screen.getByTestId('token-input');
        fireEvent.change(tokenInput, { target: { value: '   ' } });
        expect(screen.getByTestId('source-control-token-warning')).toHaveTextContent('Access Token must be configured');
        fireEvent.click(submitButton);

        expect(axiosMock.history.post.length).toBe(0);

        fireEvent.change(tokenInput, { target: { value: 'app-local-token' } });
        fireEvent.click(submitButton);

        expect(await screen.findByText('Success!')).toBeVisible();
        expect(JSON.parse(axiosMock.history.post[0].data)).toMatchObject({
          repositoryUrl: 'https://github.com/example/app-repo.git',
          token: 'app-local-token',
          authenticationType: null,
        });
      });

      it('shows standard token authentication for non-GitHub providers', async () => {
        axiosMock.onGet(getCompositeSourceControlUrl(ownerType, ownerId)).reply(200, {
          ...existingAppConfigResponse,
          provider: { value: 'azure', parentValue: null, parentName: null },
        });

        const preloadedState = {
          ...defaultPreloadedState,
          productFeatures: {
            productFeatures: {
              ...defaultPreloadedState.productFeatures.productFeatures,
              'github-app-authentication': true,
            },
          },
        };

        renderComponent(preloadedState);

        await screen.findByRole('button', { name: 'Update' });

        // Should NOT show Authentication Method fieldset for non-GitHub provider
        expect(screen.queryByRole('group', { name: 'Authentication Method' })).not.toBeInTheDocument();

        // Should show standard token input field
        const tokenInput = screen.getByTestId('token-input');
        expect(tokenInput).toBeVisible();
      });

      it('shows standard token authentication when both provider and token are inherited', async () => {
        axiosMock.onGet(getCompositeSourceControlUrl(ownerType, ownerId)).reply(200, inheritedAppConfigResponse);

        const preloadedState = {
          ...defaultPreloadedState,
          productFeatures: {
            productFeatures: {
              ...defaultPreloadedState.productFeatures.productFeatures,
              'github-app-authentication': true,
            },
          },
        };

        renderComponent(preloadedState);

        await screen.findByRole('button', { name: 'Update' });

        // Should NOT show Authentication Method fieldset when fully inherited
        expect(screen.queryByRole('group', { name: 'Authentication Method' })).not.toBeInTheDocument();

        // Should show standard token input field
        const tokenInput = screen.getByTestId('token-input');
        expect(tokenInput).toBeVisible();
      });

      it('allows selecting Personal Access Token authentication method', async () => {
        const preloadedState = {
          ...defaultPreloadedState,
          productFeatures: {
            productFeatures: {
              ...defaultPreloadedState.productFeatures.productFeatures,
              'github-app-authentication': true,
            },
          },
        };

        renderComponent(preloadedState);

        await screen.findByRole('button', { name: 'Update' });

        const authMethodFieldset = screen.getByRole('group', { name: 'Authentication Method' });
        const patRadio = within(authMethodFieldset).getByRole('radio', { name: 'Personal Access Token' });

        fireEvent.click(patRadio);

        expect(patRadio).toBeChecked();

        // Should show Access Token input field
        const tokenInput = within(authMethodFieldset).getByLabelText('Access Token');
        expect(tokenInput).toBeVisible();
        expect(tokenInput).toHaveAttribute('type', 'password');
      });

      it('allows selecting GitHub App authentication method', async () => {
        const preloadedState = {
          ...defaultPreloadedState,
          productFeatures: {
            productFeatures: {
              ...defaultPreloadedState.productFeatures.productFeatures,
              'github-app-authentication': true,
            },
          },
        };

        renderComponent(preloadedState);

        await screen.findByRole('button', { name: 'Update' });

        const authMethodFieldset = screen.getByRole('group', { name: 'Authentication Method' });
        const githubAppRadio = within(authMethodFieldset).getByRole('radio', {
          name: 'GitHub App (Recommended)',
        });

        fireEvent.click(githubAppRadio);

        expect(githubAppRadio).toBeChecked();

        // Should show Configure button (when not configured)
        const configureButton = within(authMethodFieldset).getByRole('button', { name: 'Configure GitHub App' });
        expect(configureButton).toBeVisible();
      });

      it('pre-selects GitHub App method when authenticationType is GITHUB_APP', async () => {
        axiosMock.onGet(getCompositeSourceControlUrl(ownerType, ownerId)).reply(200, {
          ...existingAppConfigResponse,
          provider: { value: 'github', parentValue: 'azure', parentName: ROOT_ORGANIZATION_NAME },
          authenticationType: { value: 'GITHUB_APP', parentValue: null, parentName: null },
        });

        const preloadedState = {
          ...defaultPreloadedState,
          productFeatures: {
            productFeatures: {
              ...defaultPreloadedState.productFeatures.productFeatures,
              'github-app-authentication': true,
            },
          },
        };

        renderComponent(preloadedState);

        await screen.findByRole('button', { name: 'Update' });

        const authMethodFieldset = screen.getByRole('group', { name: 'Authentication Method' });
        const githubAppRadio = within(authMethodFieldset).getByRole('radio', {
          name: 'GitHub App (Recommended)',
        });

        // GitHub App method should be pre-selected
        expect(githubAppRadio).toBeChecked();

        // Configure button should be shown (since no installation ID is configured)
        expect(within(authMethodFieldset).getByRole('button', { name: 'Configure GitHub App' })).toBeVisible();
      });

      it('does not show reconfigure alert by default after loading', async () => {
        axiosMock.onGet(getCompositeSourceControlUrl(ownerType, ownerId)).reply(200, {
          ...existingAppConfigResponse,
          authenticationType: { value: 'GITHUB_APP', parentValue: null, parentName: null },
          githubApp: {
            value: {
              installationId: '12345',
              name: 'Test App',
              accountName: 'test-org',
            },
            parentValue: null,
            parentName: null,
          },
        });

        const preloadedState = {
          ...defaultPreloadedState,
          productFeatures: {
            productFeatures: {
              ...defaultPreloadedState.productFeatures.productFeatures,
              'github-app-authentication': true,
            },
          },
        };

        renderComponent(preloadedState);

        await screen.findByRole('button', { name: 'Update' });

        // Reconfigure alert should not be present by default
        const alert = screen.queryByText(/The GitHub App was replaced successfully/i);
        expect(alert).not.toBeInTheDocument();
      });
    });

    describe('Warning message for authentication method', () => {
      it('shows "Authentication method must be configured" when GitHub App feature is enabled', async () => {
        const preloadedState = {
          ...defaultPreloadedState,
          productFeatures: {
            productFeatures: {
              ...defaultPreloadedState.productFeatures.productFeatures,
              'github-app-authentication': true,
            },
          },
        };

        axiosMock.onGet(getCompositeSourceControlUrl(ownerType, ownerId)).reply(200, {
          ...defaultAppConfigResponse,
          provider: { value: 'github', parentValue: null, parentName: null },
          token: { value: null, parentValue: null, parentName: null },
        });

        renderComponent(preloadedState);

        await screen.findByRole('button', { name: 'Update' });

        const warningAlert = screen.getByTestId('source-control-token-warning');
        expect(warningAlert).toHaveTextContent('Authentication method must be configured');
      });

      it('shows "Access Token must be configured" when GitHub App feature is disabled', async () => {
        const preloadedState = {
          ...defaultPreloadedState,
          productFeatures: {
            productFeatures: {
              ...defaultPreloadedState.productFeatures.productFeatures,
              'github-app-authentication': false,
            },
          },
        };

        axiosMock.onGet(getCompositeSourceControlUrl(ownerType, ownerId)).reply(200, {
          ...defaultAppConfigResponse,
          provider: { value: 'github', parentValue: null, parentName: null },
          token: { value: null, parentValue: null, parentName: null },
        });

        renderComponent(preloadedState);

        await screen.findByRole('button', { name: 'Update' });

        const warningAlert = screen.getByTestId('source-control-token-warning');
        expect(warningAlert).toHaveTextContent('Access Token must be configured');
      });
    });
  });
});
