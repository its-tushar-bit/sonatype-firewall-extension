/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { useRouterState } from 'MainRoot/react/RouterStateContext';
import {
  NxFontAwesomeIcon,
  NxH2,
  NxTextLink,
  NxThreatIndicator,
  NxTile,
  NxTooltip,
} from '@sonatype/react-shared-components';
import { faInfoCircle } from '@fortawesome/pro-solid-svg-icons';
import { map } from 'ramda';
import moment from 'moment';

import { isNilOrEmpty } from 'MainRoot/util/jsUtil';
import LoadWrapper from 'MainRoot/react/LoadWrapper';
import { selectHighPriorityVulnerabilitiesTile } from './highPriorityVulnerabilitiesTileSelectors';
import { actions } from './highPriorityVulnerabilitiesTileSlice';

import './HighPriorityVulnerabilitiesTile.scss';

const ADVANCED_SEARCH_STATE = 'sbomManager.advancedSearch';

const THREAT_INDICATOR_MAP = {
  critical: 'critical',
  high: 'severe',
};

export default function HighPriorityVulnerabilitiesTile() {
  const dispatch = useDispatch();
  const uiRouterState = useRouterState();
  const getAdvancedSearchHref = (search) => uiRouterState.href(ADVANCED_SEARCH_STATE, { search });
  const doLoad = () => dispatch(actions.loadHighPriorityVulnerabilities());

  const { loading, loadError, vulnerabilities } = useSelector(selectHighPriorityVulnerabilitiesTile);
  const hasVulnerabilites = !isNilOrEmpty(vulnerabilities);

  useEffect(() => {
    doLoad();

    moment.defineLocale('custom', {
      relativeTime: {
        d: '%dD',
        dd: '%dD',
        M: '%dM',
        MM: '%dM',
        y: '%dY',
        yy: '%dY',
      },
    });

    moment.locale('custom');

    return () => moment.locale('en-us');
  }, []);

  const vulnerabilitiesContent = hasVulnerabilites ? (
    <ol className="sbom-manager-high-priority-vulnerabilities-tile-list">
      {map(
        (vulnerability) => (
          <li key={vulnerability.refId} className="sbom-manager-high-priority-vulnerabilities-tile-list-item">
            <div className="sbom-manager-high-priority-vulnerabilities-tile-list-item__score">
              <span
                className="sbom-manager-high-priority-vulnerabilities-tile-list-item__severity"
                data-testid="high-priority-vulnerabilities-severity"
              >
                <NxThreatIndicator threatLevelCategory={THREAT_INDICATOR_MAP[vulnerability.severityStatus]} />
                {vulnerability.severity}
              </span>
              <NxTextLink href={getAdvancedSearchHref(vulnerability.refId)}>{vulnerability.refId}</NxTextLink>
            </div>
            <div className="sbom-manager-high-priority-vulnerabilities-tile-list-item__date">
              {moment(vulnerability.createdAt).fromNow()}
            </div>
          </li>
        ),
        vulnerabilities
      )}
    </ol>
  ) : (
    <span>No Critical/High Vulnerabilities Found</span>
  );

  return (
    <NxTile id="high-priority-vulnerabilities-tile" className="sbom-manager-high-priority-vulnerabilities-tile">
      <NxTile.Header>
        <NxTile.HeaderTitle>
          <NxH2>High Priority Vulnerabilities</NxH2>
          <NxTooltip title="High severity vulnerabilities found in the most recent SBOM scans or import.">
            <NxFontAwesomeIcon
              icon={faInfoCircle}
              className="sbom-manager-high-priority-vulnerabilities-tile__info-icon"
            />
          </NxTooltip>
        </NxTile.HeaderTitle>
      </NxTile.Header>
      <NxTile.Content>
        <LoadWrapper retryHandler={() => doLoad()} loading={loading} error={loadError}>
          {vulnerabilitiesContent}
        </LoadWrapper>
      </NxTile.Content>
    </NxTile>
  );
}
