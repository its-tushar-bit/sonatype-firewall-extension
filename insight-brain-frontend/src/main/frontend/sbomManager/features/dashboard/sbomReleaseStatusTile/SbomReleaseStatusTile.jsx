/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { NxFontAwesomeIcon, NxH2, NxH3, NxMeter, NxTile, NxTooltip } from '@sonatype/react-shared-components';
import * as PropTypes from 'prop-types';
import classNames from 'classnames';
import { faInfoCircle, faCheckCircle } from '@fortawesome/pro-solid-svg-icons';
import LoadWrapper from 'MainRoot/react/LoadWrapper';
import { formatNumberLocale } from 'MainRoot/util/formatUtils';

import './SbomReleaseStatusTile.scss';

const SbomRelaseStatusMeter = (props) => {
  const { sbomCount, totalSbomCount, status } = props;

  const icon = status === 'Release Ready' ? faCheckCircle : faInfoCircle;
  const fontAwesomeClasses = classNames('sbom-manager-sbom-release-status-meter-bar__icon', {
    'sbom-manager-sbom-release-status-meter-bar__icon--red': status === 'Needs Attention',
    'sbom-manager-sbom-release-status-meter-bar__icon--orange': status === 'Partially Annotated',
    'sbom-manager-sbom-release-status-meter-bar__icon--green': status === 'Release Ready',
  });
  const meterBarClasses = classNames('sbom-manager-sbom-release-status-meter-bar__meter', {
    'sbom-manager-sbom-release-status-meter-bar__meter--partially-annotated': status === 'Partially Annotated',
    'sbom-manager-sbom-release-status-meter-bar__meter--release-ready': status === 'Release Ready',
  });

  return (
    <div className="sbom-manager-sbom-release-status-meter-bar">
      <div className="sbom-manager-sbom-release-status-meter-bar__status">
        <NxFontAwesomeIcon icon={icon} className={fontAwesomeClasses} />
        <span data-testid="sbom-release-status-meter-bar-status">{status}</span>
      </div>
      <NxMeter
        className={meterBarClasses}
        label="SBOM Release Status"
        value={sbomCount}
        max={totalSbomCount}
        data-testid="sbom-release-status-meter"
      >{`${sbomCount} out of ${totalSbomCount}`}</NxMeter>
      <div
        className="sbom-manager-sbom-release-status-meter-bar__sbom-count"
        data-testid="sbom-release-status-meter-bar-sbom-count"
      >
        {formatNumberLocale(sbomCount)}
      </div>
    </div>
  );
};

SbomRelaseStatusMeter.propTypes = {
  sbomCount: PropTypes.number.isRequired,
  totalSbomCount: PropTypes.number.isRequired,
  status: PropTypes.oneOf(['Needs Attention', 'Partially Annotated', 'Release Ready']).isRequired,
};

export default function SbomReleaseStatusTile({
  load,
  loading,
  loadError,
  releaseReadyCount,
  partiallyReadyCount,
  needsAttentionCount,
  totalSbomCount,
}) {
  return (
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
        <LoadWrapper retryHandler={load} loading={loading} error={loadError}>
          <div className="sbom-manager-sbom-release-status-tile__meter-bars">
            <SbomRelaseStatusMeter
              totalSbomCount={totalSbomCount ?? 0}
              sbomCount={needsAttentionCount ?? 0}
              status="Needs Attention"
            />
            <SbomRelaseStatusMeter
              totalSbomCount={totalSbomCount ?? 0}
              sbomCount={partiallyReadyCount ?? 0}
              status="Partially Annotated"
            />
            <SbomRelaseStatusMeter
              totalSbomCount={totalSbomCount ?? 0}
              sbomCount={releaseReadyCount ?? 0}
              status="Release Ready"
            />
          </div>
        </LoadWrapper>
      </NxTile.Content>
    </NxTile>
  );
}

SbomReleaseStatusTile.propTypes = {
  load: PropTypes.func.isRequired,
  loading: PropTypes.bool.isRequired,
  loadError: PropTypes.oneOfType([PropTypes.string, PropTypes.node, PropTypes.instanceOf(Error), PropTypes.object]),
  releaseReadyCount: PropTypes.number,
  partiallyReadyCount: PropTypes.number,
  needsAttentionCount: PropTypes.number,
  totalSbomCount: PropTypes.number,
};
