/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import {
  NxFontAwesomeIcon,
  NxH2,
  NxLoadWrapper,
  NxProgressBar,
  NxTile,
  NxTooltip,
} from '@sonatype/react-shared-components';
import { faInfoCircle } from '@fortawesome/pro-solid-svg-icons';
import { useDispatch, useSelector } from 'react-redux';
import classNames from 'classnames';

import { formatNumberLocale } from 'MainRoot/util/formatUtils';
import { selectTotalSbomsStoredTile } from './totalSbomsStoredTileSelectors';
import { actions } from './totalSbomsStoredTileSlice';

import './TotalSbomsStoredTile.scss';

export default function TotalSbomsStoredTile() {
  const dispatch = useDispatch();
  const { loading, total, threshold, errorMessage } = useSelector(selectTotalSbomsStoredTile);
  const load = () => dispatch(actions.loadTotalSbomsStored());

  useEffect(() => {
    load();
  }, []);

  const progressBarPercentage = total && threshold ? (total / threshold) * 100 : 0;
  const progressBarClasses = classNames('sbom-manager-total-sboms-stored-tile-progress__progress-bar', {
    'sbom-manager-total-sboms-stored-tile-progress__progress-bar--orange':
      progressBarPercentage >= 75 && progressBarPercentage < 90,
    'sbom-manager-total-sboms-stored-tile-progress__progress-bar--red': progressBarPercentage >= 90,
  });

  return (
    <NxLoadWrapper retryHandler={() => load()} loading={loading} error={errorMessage}>
      <NxTile id="total-sboms-stored-tile" className="sbom-manager-total-sboms-stored-tile">
        <NxTile.Header>
          <NxTile.HeaderTitle>
            <NxH2>Total SBOMs Stored</NxH2>
            <NxTooltip title="Each application version counts toward the total SBOMs Analyzed.">
              <NxFontAwesomeIcon icon={faInfoCircle} className="sbom-manager-total-sboms-stored-tile__info-icon" />
            </NxTooltip>
          </NxTile.HeaderTitle>
        </NxTile.Header>
        <NxTile.Content>
          <div className="sbom-manager-total-sboms-stored-tile__total" data-testid="total-sboms-stored-tile-total">
            <span>{formatNumberLocale(total)}</span>
            <span>(all time)</span>
          </div>

          <div className="sbom-manager-total-sboms-stored-tile-progress">
            <div
              className="sbom-manager-total-sboms-stored-tile-progress__label"
              data-testid="total-sboms-stored-tile-progress-label"
            >
              <span>SBOM License Usage</span>
              <NxTooltip title="Shows how many SBOMs you have analyzed within the limits of your purchased license.">
                <NxFontAwesomeIcon icon={faInfoCircle} className="sbom-manager-total-sboms-stored-tile__info-icon" />
              </NxTooltip>
            </div>

            <NxProgressBar
              label="SBOM License Usage"
              className={progressBarClasses}
              value={progressBarPercentage}
              variant="full"
            />

            <div className="sbom-manager-total-sboms-stored-tile-progress__total-and-threshold">
              <div
                className="sbom-manager-total-sboms-stored-tile-progress__total"
                data-testid="total-sboms-stored-tile-progress-total"
              >
                <span>{formatNumberLocale(total)}</span>
                <span>SBOMs added</span>
              </div>
              <div
                className="sbom-manager-total-sboms-stored-tile-progress__threshold"
                data-testid="total-sboms-stored-tile-progress-threshold"
              >
                <span>{formatNumberLocale(threshold)}</span>
                <span>Threshold</span>
              </div>
            </div>
          </div>
        </NxTile.Content>
      </NxTile>
    </NxLoadWrapper>
  );
}
