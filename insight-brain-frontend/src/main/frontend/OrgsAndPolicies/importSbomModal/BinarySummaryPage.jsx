/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as PropTypes from 'prop-types';
import { useSelector } from 'react-redux';
import { NxButton, NxButtonBar, NxFooter, NxH2, NxModal, NxReadOnly } from '@sonatype/react-shared-components';

import { selectSelectedOwnerName, selectSelectedOwnerPublicId } from '../orgsAndPoliciesSelectors';
import { selectImportSbomModalSlice, selectSelectedFilename } from './importSbomModalSelectors';
import { useRouterState } from 'MainRoot/react/RouterStateContext';

export default function BinarySummaryPage({ headerId, onClose }) {
  const applicationName = useSelector(selectSelectedOwnerName);
  const applicationPublicId = useSelector(selectSelectedOwnerPublicId);
  const selectedFilename = useSelector(selectSelectedFilename);
  const { savedVersion } = useSelector(selectImportSbomModalSlice);
  const uiRouterState = useRouterState();

  const sbomOverviewHref = uiRouterState.href('sbomManager.management.view.bom', {
    applicationPublicId: applicationPublicId,
    versionId: savedVersion,
  });

  function goToSbomOverview() {
    window.open(sbomOverviewHref, '_blank');
  }

  return (
    <>
      <NxModal.Header>
        <NxH2 id={headerId}>Import Complete</NxH2>
      </NxModal.Header>
      <NxModal.Content>
        <NxReadOnly>
          <NxReadOnly.Label>File</NxReadOnly.Label>
          <NxReadOnly.Data id="import-sbom-modal-filename">{selectedFilename}</NxReadOnly.Data>

          <NxReadOnly.Label>Application Name</NxReadOnly.Label>
          <NxReadOnly.Data id="import-sbom-modal-application-name">{applicationName}</NxReadOnly.Data>

          <NxReadOnly.Label>Application Version</NxReadOnly.Label>
          <NxReadOnly.Data id="import-sbom-modal-application-version">{savedVersion || ''}</NxReadOnly.Data>
        </NxReadOnly>
      </NxModal.Content>
      <NxFooter>
        <NxButtonBar>
          <NxButton onClick={onClose}>Close</NxButton>
          <NxButton id="import-sbom-modal-summary-view-sbom" variant="primary" onClick={goToSbomOverview}>
            View SBOM
          </NxButton>
        </NxButtonBar>
      </NxFooter>
    </>
  );
}

BinarySummaryPage.propTypes = {
  headerId: PropTypes.string.isRequired,
  onClose: PropTypes.func.isRequired,
};
