/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createSelector } from 'reselect';
import { prop } from 'ramda';

/**
 * Selectors for GitHub App configuration Redux slice.
 *
 * These selectors provide access to GitHub App state including:
 * - Setup errors
 * - Registration progress
 * - Form state (account type, organization name)
 */

/**
 * Root selector for GitHub App configuration slice
 */
export const selectGitHubAppConfigurationSlice = prop('gitHubAppConfiguration');

/**
 * Selects the setup error message if any
 * @returns {string|null} Error message or null if no error
 */
export const selectGitHubAppSetupError = createSelector(selectGitHubAppConfigurationSlice, prop('setupError'));

/**
 * Selects whether GitHub App registration is currently in progress
 * @returns {boolean} True if registration is in progress, false otherwise
 */
export const selectIsGitHubAppRegistrationInProgress = createSelector(
  selectGitHubAppConfigurationSlice,
  prop('registrationInProgress')
);

/**
 * Selects the form state for the GitHub App registration modal
 * @returns {Object} Form state containing accountType and organizationName
 */
export const selectFormState = createSelector(selectGitHubAppConfigurationSlice, prop('formState'));

/**
 * Selects the account type from form state
 * @returns {string} 'organization' or 'personal'
 */
export const selectAccountType = createSelector(selectFormState, prop('accountType'));

/**
 * Selects the organization name RSC state from form state
 * @returns {Object} NxTextInput state object with value, trimmedValue, isPristine, validationErrors, etc.
 */
export const selectOrganizationName = createSelector(selectFormState, prop('organizationName'));

/**
 * Selects whether the GitHub App registration modal is open
 * @returns {boolean} True if modal is open, false otherwise
 */
export const selectIsModalOpen = createSelector(selectGitHubAppConfigurationSlice, prop('isModalOpen'));
