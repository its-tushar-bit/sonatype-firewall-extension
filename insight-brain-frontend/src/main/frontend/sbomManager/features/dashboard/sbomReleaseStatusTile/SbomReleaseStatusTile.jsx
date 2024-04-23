/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import {
  NxFontAwesomeIcon,
  NxH2,
  NxH3,
  NxLoadWrapper,
  NxProgressBar,
  NxTile,
  NxTooltip,
} from '@sonatype/react-shared-components';
import * as PropTypes from 'prop-types';
import classNames from 'classnames';
import { faInfoCircle, faCheckCircle } from '@fortawesome/pro-solid-svg-icons';

import './SbomReleaseStatusTile.scss';

const SbomRelaseStatusProgressBar = (props) => {
  const { sbomCount, status } = props;

  const icon = status === 'Release Ready' ? faCheckCircle : faInfoCircle;
  const fontAwesomeClasses = classNames('sbom-manager-sbom-release-status-progress-bar__icon', {
    'sbom-manager-sbom-release-status-progress-bar__icon--red': status === 'Not Started',
    'sbom-manager-sbom-release-status-progress-bar__icon--orange': status === 'Partially Annotated',
    'sbom-manager-sbom-release-status-progress-bar__icon--green': status === 'Release Ready',
  });
  const progressBarClasses = classNames('sbom-manager-sbom-release-status-progress-bar__progress', {
    'sbom-manager-sbom-release-status-progress-bar__progress--partially-annotated': status === 'Partially Annotated',
    'sbom-manager-sbom-release-status-progress-bar__progress--release-ready': status === 'Release Ready',
  });

  return (
    <div className="sbom-manager-sbom-release-status-progress-bar">
      <div className="sbom-manager-sbom-release-status-progress-bar__status">
        <NxFontAwesomeIcon icon={icon} className={fontAwesomeClasses} />
        <span>{status}</span>
      </div>
      <NxProgressBar className={progressBarClasses} label="SBOM Release Status" value={20} variant="inline" />
      <div className="sbom-manager-sbom-release-status-progress-bar__sbom-count">
        {sbomCount.toLocaleString('en-US')}
      </div>
    </div>
  );
};

SbomRelaseStatusProgressBar.propTypes = {
  sbomCount: PropTypes.number.isRequired,
  status: PropTypes.oneOf(['Not Started', 'Partially Annotated', 'Release Ready']).isRequired,
};

export default function SbomReleaseStatusTile() {
  const doLoad = () => {};

  return (
    <NxLoadWrapper retryHandler={doLoad}>
      <NxTile id="sbom-release-status-tile" className="sbom-manager-sbom-release-status-tile">
        <NxTile.Header>
          <NxTile.HeaderTitle>
            <NxH2>SBOM Release Status</NxH2>
            <NxH3>(all time)</NxH3>
            <NxTooltip title="Shows breakdown of SBOMs based on the annotations completed.">
              <NxFontAwesomeIcon icon={faInfoCircle} className="sbom-manager-sbom-release-status-tile__info-icon" />
            </NxTooltip>
          </NxTile.HeaderTitle>
        </NxTile.Header>
        <NxTile.Content>
          <div className="sbom-manager-sbom-release-status-tile__progress-bars">
            <SbomRelaseStatusProgressBar sbomCount={20} status="Not Started" />
            <SbomRelaseStatusProgressBar sbomCount={20} status="Partially Annotated" />
            <SbomRelaseStatusProgressBar sbomCount={20} status="Release Ready" />
          </div>
        </NxTile.Content>
      </NxTile>
    </NxLoadWrapper>
  );
}
