/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as PropTypes from 'prop-types';
import moment from 'moment';
import ComponentDisplay from 'MainRoot/ComponentDisplay/ReactComponentDisplay';
import UpgradeAvailableIndicator from 'MainRoot/react/upgradeAvailableIndicator/UpgradeAvailableIndicator';
import { NxTable, NxThreatIndicator, NxOverflowTooltip, NxSmallTag } from '@sonatype/react-shared-components';
import { isWaiverAllVersionsOrExact, shouldShowUpgradeIndicator } from 'MainRoot/util/waiverUtils';
import { FIREWALL_WAIVER_DETAILS } from 'MainRoot/constants/states';
import { useSelector } from 'react-redux';
import { selectIsStandaloneFirewall } from 'MainRoot/reduxUiRouter/routerSelectors';

export default function DashboardWaiversTableRow({ stateGo, waiver, page }) {
  const {
    id: waiverId,
    threatLevel,
    createTime,
    expiryTime,
    policyName,
    ownerId,
    ownerType,
    scope,
    componentMatchStrategy,
    componentUpgradeAvailable,
    isExpireWhenRemediationAvailable,
  } = waiver;

  const isStandaloneFirewall = useSelector(selectIsStandaloneFirewall);

  const goToWaiverDetails = () => {
    const waiverType = waiver.isAutoWaiver ? 'autoWaiver' : 'waiver';
    const stateToGo = isStandaloneFirewall ? FIREWALL_WAIVER_DETAILS : 'waiver.details';
    stateGo(stateToGo, {
      waiverId,
      ownerId,
      ownerType,
      type: waiverType,
      sidebarReference: 'filter',
      page: page + 1,
    });
  };

  const waiverCreateTime = moment(createTime).format('YYYY-MM-DD');
  const waiverExpiryTime = expiryTime
    ? moment(expiryTime).format('YYYY-MM-DD')
    : isExpireWhenRemediationAvailable
    ? 'When Remediation Available'
    : 'Never';
  return (
    <NxTable.Row key={waiverId} onClick={goToWaiverDetails} className="iq-dashboard-waiver" isClickable>
      <NxTable.Cell className="iq-threat-cell">
        <NxThreatIndicator policyThreatLevel={threatLevel} />
        <span className="nx-threat-number">{threatLevel}</span>
      </NxTable.Cell>
      <NxTable.Cell>
        <NxOverflowTooltip>
          <div className="nx-truncate-ellipsis">{waiverCreateTime}</div>
        </NxOverflowTooltip>
      </NxTable.Cell>
      <NxTable.Cell>
        <NxOverflowTooltip>
          <div>
            {waiver.isAutoWaiver === true ? (
              <NxSmallTag color="green" style={{ margin: '0' }}>
                Auto
              </NxSmallTag>
            ) : (
              waiverExpiryTime
            )}
          </div>
        </NxOverflowTooltip>
      </NxTable.Cell>
      <NxTable.Cell>
        <NxOverflowTooltip>
          <div>{policyName ? policyName : <span>{'—'}</span>}</div>
        </NxOverflowTooltip>
      </NxTable.Cell>
      <NxTable.Cell>
        <NxOverflowTooltip>
          <div className="nx-truncate-ellipsis">{scope}</div>
        </NxOverflowTooltip>
      </NxTable.Cell>
      <NxTable.Cell>
        {waiver.componentIdentifier === null ? (
          <span>{'—'}</span>
        ) : isWaiverAllVersionsOrExact(waiver) ? (
          <ComponentDisplay component={waiver} truncate={true} matcherStrategy={componentMatchStrategy} />
        ) : (
          'All Components'
        )}
      </NxTable.Cell>
      <NxTable.Cell className="iq-upgrade-cell">
        <NxOverflowTooltip>
          {shouldShowUpgradeIndicator(componentUpgradeAvailable, waiver) ? (
            <UpgradeAvailableIndicator isAbbreviated={true} />
          ) : (
            <span>{'—'}</span>
          )}
        </NxOverflowTooltip>
      </NxTable.Cell>
      <NxTable.Cell chevron />
    </NxTable.Row>
  );
}

export const waiverPropTypes = PropTypes.shape({
  id: PropTypes.string.isRequired,
  threatLevel: PropTypes.number.isRequired,
  createTime: PropTypes.oneOfType([PropTypes.number, PropTypes.string]),
  expiryTime: PropTypes.oneOfType([PropTypes.number, PropTypes.string]),
  policyName: PropTypes.string,
  ownerId: PropTypes.string.isRequired,
  ownerName: PropTypes.string,
  ownerType: PropTypes.string.isRequired,
  scope: PropTypes.string.isRequired,
  componentMatchStrategy: PropTypes.string,
  componentUpgradeAvailable: PropTypes.bool,
  isAutoWaiver: PropTypes.bool,
  componentIdentifier: PropTypes.object,
});

DashboardWaiversTableRow.propTypes = {
  stateGo: PropTypes.func.isRequired,
  waiver: waiverPropTypes,
  page: PropTypes.number,
  isStandaloneFirewall: PropTypes.bool,
};
