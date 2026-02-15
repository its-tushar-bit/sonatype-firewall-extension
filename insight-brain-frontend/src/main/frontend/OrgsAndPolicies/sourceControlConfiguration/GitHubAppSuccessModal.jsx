/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import PropTypes from 'prop-types';
import { NxButton, NxButtonBar, NxFooter, NxModal, NxH2, NxP } from '@sonatype/react-shared-components';

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
 */
const GitHubAppSuccessModal = ({
  isOpen,
  onClose,
  autoEnabledGoldenPRs,
  autoEnabledManualPRs,
  serverId,
  organizationName,
}) => {
  if (!isOpen) {
    return null;
  }

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
          Sonatype IQ Server <strong>{serverId || ''}</strong> has been installed successfully on GitHub Organization{' '}
          <strong>&quot;{organizationName || 'your organization'}&quot;</strong>.
        </NxP>

        {/* Show feature descriptions if any features were newly enabled */}
        {(autoEnabledGoldenPRs || autoEnabledManualPRs) && (
          <>
            <NxP>Sonatype Lifecycle will now automatically:</NxP>
            <div className="nx-list nx-list--bulleted">
              <ul>
                {autoEnabledGoldenPRs && goldenPRsListItem}
                {autoEnabledManualPRs && manualPRsListItem}
              </ul>
            </div>
            <NxP>
              To update this configuration or enable <strong>Automated InnerSource Updates</strong>, visit the Source
              Control Configuration page.
            </NxP>
          </>
        )}
      </NxModal.Content>
      <NxFooter>
        <NxButtonBar>
          <NxButton variant="primary" onClick={onClose}>
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
};

export default GitHubAppSuccessModal;
