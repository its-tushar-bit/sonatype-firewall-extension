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
import { NxTable, NxThreatIndicator, NxOverflowTooltip, NxSmallTag, NxButton, NxFontAwesomeIcon } from '@sonatype/react-shared-components';
import { faTrashAlt } from '@fortawesome/free-solid-svg-icons';
import { faRenewSolid } from 'MainRoot/img/faRenewSolid';
import { getWaiverDaysRemaining, isWaiverAllVersionsOrExact, shouldShowUpgradeIndicator } from 'MainRoot/util/waiverUtils';
import { FIREWALL_WAIVER_DETAILS } from 'MainRoot/constants/states';
import { useSelector, useDispatch } from 'react-redux';
import { selectIsStandaloneFirewall } from 'MainRoot/reduxUiRouter/routerSelectors';
import { stateGo as stateGoAction } from 'MainRoot/reduxUiRouter/routerActions';
import { setFirewallWaiverToDelete } from 'MainRoot/firewall/waivers/firewallDashboardWaiverActions';
import { selectFirewallDashboardHasWaivePermission } from 'MainRoot/firewall/waivers/firewallDashboardWaiverSelectors';

export default function DashboardWaiversTableRow({ stateGo, waiver, page }) {
  const dispatch = useDispatch();
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

  const daysRemaining = getWaiverDaysRemaining(expiryTime, waiver.isAutoWaiver, isExpireWhenRemediationAvailable);

  const getExpiryStatusDescriptor = (days) => {
    if (days == null) return null;
    if (days < 0) return { text: 'Expired', modifier: 'critical' };
    if (days <= 7) return { text: `Expires in ${days} day${days !== 1 ? 's' : ''}`, modifier: 'critical' };
    return { text: `Expires in ${days} days`, modifier: 'muted' };
  };

  const hasWaivePermission = useSelector(selectFirewallDashboardHasWaivePermission);

  const handleRenew = (e) => {
    e.stopPropagation();
    dispatch(
      stateGoAction('firewall.renewWaiver', {
        ownerType,
        ownerId,
        waiverId,
        type: 'waiver',
        sidebarReference: 'filter',
        page: page + 1,
      })
    );
  };

  const handleDelete = (e) => {
    e.stopPropagation();
    dispatch(setFirewallWaiverToDelete(waiver));
  };

  const goToWaiverDetails = () => {
    const waiverType = waiver.isAutoWaiver ? 'autoWaiver' : 'waiver';
    const stateToGo = isStandaloneFirewall ? FIREWALL_WAIVER_DETAILS : 'waiver.details';
    dispatch(
      stateGo(stateToGo, {
        waiverId,
        ownerId,
        ownerType,
        type: waiverType,
        sidebarReference: 'filter',
        page: page + 1,
      })
    );
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
        <div className="iq-waiver-expiry-cell">
          {waiver.isAutoWaiver === true ? (
            <NxSmallTag color="green" style={{ margin: '0' }}>Auto</NxSmallTag>
          ) : (
            <div className="iq-waiver-expiry-content">
              <NxOverflowTooltip>
                <div className="nx-truncate-ellipsis">{waiverExpiryTime}</div>
              </NxOverflowTooltip>
              {getExpiryStatusDescriptor(daysRemaining) && (
                <span className={`iq-waiver-expiry-status iq-waiver-expiry-status--${getExpiryStatusDescriptor(daysRemaining).modifier}`}>
                  {getExpiryStatusDescriptor(daysRemaining).text}
                </span>
              )}
            </div>
          )}
        </div>
      </NxTable.Cell>
      <NxTable.Cell>
        <NxOverflowTooltip>
          <div>{policyName || <span>{'—'}</span>}</div>
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
        {shouldShowUpgradeIndicator(componentUpgradeAvailable, waiver) ? (
          <NxOverflowTooltip>
            <UpgradeAvailableIndicator isAbbreviated={true} />
          </NxOverflowTooltip>
        ) : (
          <span>{'—'}</span>
        )}
      </NxTable.Cell>
      {isStandaloneFirewall && hasWaivePermission ? (
        <NxTable.Cell className="iq-waiver-actions-cell" onClick={(e) => e.stopPropagation()}>
          <div className="iq-waiver-actions-cell__buttons">
            <NxButton variant="icon-only" title="Renew waiver" onClick={handleRenew} className="iq-waiver-renew-btn">
              <NxFontAwesomeIcon icon={faRenewSolid} />
            </NxButton>
            <NxButton variant="icon-only" title="Delete waiver" onClick={handleDelete} className="iq-waiver-delete-btn">
              <NxFontAwesomeIcon icon={faTrashAlt} />
            </NxButton>
          </div>
        </NxTable.Cell>
      ) : (
        <NxTable.Cell chevron />
      )}
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
  isExpireWhenRemediationAvailable: PropTypes.bool,
});

DashboardWaiversTableRow.propTypes = {
  stateGo: PropTypes.func.isRequired,
  waiver: waiverPropTypes,
  page: PropTypes.number,
};
