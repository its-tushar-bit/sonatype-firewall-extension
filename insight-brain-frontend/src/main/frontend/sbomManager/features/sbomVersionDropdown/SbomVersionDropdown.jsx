/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { NxDropdown, useToggle } from '@sonatype/react-shared-components';
import { useRouterState } from 'MainRoot/react/RouterStateContext';
import * as PropTypes from 'prop-types';

export default function SbomVersionDropdown(props) {
  const { publicAppId, sbomVersions, currentSbomVersion } = props;
  const sbomVersionsExceptCurrent = sbomVersions.filter((sbomVersion) => sbomVersion !== currentSbomVersion);
  const [isOpen, onToggleCollapse] = useToggle(false);
  const uiRouterState = useRouterState();

  return (
    <NxDropdown
      className="sbom-manager-sbom-version-dropdown"
      label={
        <>
          Viewing: <span>{currentSbomVersion}</span>
        </>
      }
      isOpen={isOpen}
      onToggleCollapse={onToggleCollapse}
    >
      {sbomVersionsExceptCurrent.length > 0 ? (
        sbomVersionsExceptCurrent.map((sbomVersion) => (
          <a
            href={uiRouterState.href('sbomManager.management.view.bom', {
              applicationPublicId: publicAppId,
              versionId: sbomVersion,
            })}
            className="nx-dropdown-button"
            key={sbomVersion}
          >
            {sbomVersion}
          </a>
        ))
      ) : (
        <button onClick={() => {}} className="disabled nx-dropdown-button">
          No other SBOMs found
        </button>
      )}
    </NxDropdown>
  );
}

SbomVersionDropdown.propTypes = {
  publicAppId: PropTypes.string.isRequired,
  sbomVersions: PropTypes.array.isRequired,
  currentSbomVersion: PropTypes.string.isRequired,
};
