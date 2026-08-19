/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as PropTypes from 'prop-types';
import { useSelector } from 'react-redux';
import {
  NxButton,
  NxButtonBar,
  NxFooter,
  NxH2,
  NxModal,
  NxReadOnly,
  NxSmallThreatCounter,
} from '@sonatype/react-shared-components';

import { selectSelectedOwnerPublicId, selectSelectedOwnerName } from '../orgsAndPoliciesSelectors';
import { selectImportSbomModalSlice } from './importSbomModalSelectors';
import { useRouterState } from 'MainRoot/react/RouterStateContext';

export default function SbomSummaryPage({ headerId, onClose }) {
  const applicationName = useSelector(selectSelectedOwnerName);
  const applicationPublicId = useSelector(selectSelectedOwnerPublicId);
  const { sbomSummary, savedVersion } = useSelector(selectImportSbomModalSlice);
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
        <NxReadOnly className="import-sbom-modal__summary-grid">
          <NxReadOnly.Label>Total Components:</NxReadOnly.Label>
          <NxReadOnly.Data id="import-sbom-modal-summary-total-components">
            {sbomSummary.totalComponents}
          </NxReadOnly.Data>

          <NxReadOnly.Label>Total Vulnerabilities:</NxReadOnly.Label>
          <NxReadOnly.Data id="import-sbom-modal-summary-total-vulnerabilities">
            <NxSmallThreatCounter
              id="import-sbom-modal-summary-total-vulnerabilities"
              criticalCount={sbomSummary.criticalVulnerabilities || 0}
              severeCount={sbomSummary.highVulnerabilities || 0}
              moderateCount={sbomSummary.mediumVulnerabilities || 0}
              lowCount={sbomSummary.lowVulnerabilities || 0}
              maxDigits={Infinity}
            />
          </NxReadOnly.Data>
        </NxReadOnly>
        <NxReadOnly>
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

SbomSummaryPage.propTypes = {
  headerId: PropTypes.string.isRequired,
  onClose: PropTypes.func.isRequired,
};
