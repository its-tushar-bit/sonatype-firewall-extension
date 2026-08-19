/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import PropTypes from 'prop-types';
import { NxButton, NxButtonBar, NxFooter, NxModal, NxH2, NxP, NxInfoAlert } from '@sonatype/react-shared-components';
import { isPersonalAccount, getCleanAccountName, GITHUB_ACCOUNT_DISPLAY_LABELS } from './utils';

/**
 * Success modal shown after GitHub App setup completes
 * Displays different messages based on which PR features were auto-enabled
 *
 * @param {boolean} isOpen - Whether modal is open
 * @param {Function} onClose - Close handler
 * @param {boolean} autoEnabledGoldenPRs - Whether Golden PRs were auto-enabled
 * @param {boolean} autoEnabledManualPRs - Whether Manual PRs were auto-enabled
 * @param {string} serverId - Sonatype IQ Server ID
 * @param {string} organizationName - GitHub organization name
 * @param {string} submitBtnText - The submit button text ('Create' or 'Update')
 */
const GitHubAppSuccessModal = ({
  isOpen,
  onClose,
  autoEnabledGoldenPRs,
  autoEnabledManualPRs,
  serverId,
  organizationName,
  submitBtnText,
}) => {
  if (!isOpen) {
    return null;
  }

  // Determine if this is a personal account and get clean display name
  const isPersonal = isPersonalAccount(organizationName);
  const displayAccountName = getCleanAccountName(organizationName);

  // Extract feature descriptions to avoid duplication
  const goldenPRsListItem = (
    <li className="nx-list__item">
      <div className="nx-list__text">
        <strong>Create Golden PRs</strong>
        <br />
        Pull requests for Maven dependencies are automatically generated when the recommended version, including
        transitive dependencies, is non-breaking and safe to use.
      </div>
    </li>
  );

  const manualPRsListItem = (
    <li className="nx-list__item">
      <div className="nx-list__text">
        <strong>Recommend Manual Pull Requests</strong>
        <br />
        Adds a &quot;Create PR&quot; button in the Priorities section of Sonatype Developer that will manually trigger a
        pull request against the default branch when a suggested version change for a specific component is available.
      </div>
    </li>
  );

  return (
    <NxModal onCancel={onClose} aria-labelledby="github-app-success-modal-header">
      <NxModal.Header>
        <NxH2 id="github-app-success-modal-header">GitHub Setup Complete</NxH2>
      </NxModal.Header>
      <NxModal.Content>
        <NxP>
          Sonatype IQ Server <strong>{serverId || ''}</strong> has been installed successfully on GitHub{' '}
          {isPersonal ? GITHUB_ACCOUNT_DISPLAY_LABELS.PERSONAL : GITHUB_ACCOUNT_DISPLAY_LABELS.ORGANIZATION}{' '}
          <strong>&quot;{displayAccountName || 'your account'}&quot;</strong>.
        </NxP>

        {/* Show feature descriptions if any features were newly enabled */}
        {(autoEnabledGoldenPRs || autoEnabledManualPRs) && (
          <>
            <NxP>Automation options are enabled and ready to be applied:</NxP>
            <div className="nx-list nx-list--bulleted">
              <ul>
                {autoEnabledGoldenPRs && goldenPRsListItem}
                {autoEnabledManualPRs && manualPRsListItem}
              </ul>
            </div>
          </>
        )}
        <NxInfoAlert>
          Click <strong>{submitBtnText?.toLowerCase() || 'create/update'}</strong> in the source control page to apply
          this configuration.
        </NxInfoAlert>
      </NxModal.Content>
      <NxFooter>
        <NxButtonBar>
          <NxButton id="github-app-success-done-button" variant="primary" onClick={onClose}>
            Done
          </NxButton>
        </NxButtonBar>
      </NxFooter>
    </NxModal>
  );
};

GitHubAppSuccessModal.propTypes = {
  isOpen: PropTypes.bool.isRequired,
  onClose: PropTypes.func.isRequired,
  autoEnabledGoldenPRs: PropTypes.bool.isRequired,
  autoEnabledManualPRs: PropTypes.bool.isRequired,
  serverId: PropTypes.string,
  organizationName: PropTypes.string,
  submitBtnText: PropTypes.string.isRequired,
};

export default GitHubAppSuccessModal;
