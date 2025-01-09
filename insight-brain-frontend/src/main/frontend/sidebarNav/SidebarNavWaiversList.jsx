/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect, useRef } from 'react';
import * as PropTypes from 'prop-types';
import classnames from 'classnames';

import { NxThreatIndicator, NxOverflowTooltip, NxSmallTag } from '@sonatype/react-shared-components';
import ComponentDisplay from 'MainRoot/ComponentDisplay/ReactComponentDisplay';

import { UPDATE_DIMENSIONS_TIMEOUT } from 'MainRoot/util/constants';
import { displayWaiverScope, isWaiverAllVersionsOrExact } from 'MainRoot/util/waiverUtils';
import { useSelector } from 'react-redux';
import { selectIsStandaloneFirewall } from 'MainRoot/reduxUiRouter/routerSelectors';

export default function SidebarNavWaiversList({ currentWaiverId, waivers, onClick, scrollToSelection }) {
  const SCROLL_TIMEOUT = UPDATE_DIMENSIONS_TIMEOUT + 100;
  // Have to access `useRef` and `useEffect` through the React object due to testing limitations
  const selectedElementRef = useRef(null);

  const isStandaloneFirewall = useSelector(selectIsStandaloneFirewall);

  useEffect(() => {
    if (!scrollToSelection) {
      return;
    }

    const timeoutId = setTimeout(() => selectedElementRef?.current?.scrollIntoView(), SCROLL_TIMEOUT, 'sidebar-nav'); // supply a flag so we can identify this call in tests

    return () => {
      clearTimeout(timeoutId);
    };
  });

  const componentMatchStrategy = (item) => item.componentMatchStrategy || item.matcherStrategy;

  const policyWaiverId = (item) => item.id || item.policyWaiverId;

  const isItemSelected = (item) => policyWaiverId(item) === currentWaiverId;

  const listClass = (item) =>
    classnames('nx-list__item', {
      selected: isItemSelected(item),
    });

  const getFullPolicyName = (item) => {
    if (item?.isAutoWaiver) {
      return (
        <span>
          <span>≤ {item.threatLevel}</span>
          <NxSmallTag color="green">Auto</NxSmallTag>
        </span>
      );
    }
    return `${item.threatLevel} ${item.policyName}`;
  };
  const getFullOwner = (item) => {
    return !item.scope ? displayWaiverScope(item) : item.scope;
  };

  const handleClick = (item) => {
    const { ownerType, ownerId } = item;
    const waiverType = item.isAutoWaiver ? 'autoWaiver' : 'waiver';
    onClick(ownerId, ownerType, policyWaiverId(item), waiverType, isStandaloneFirewall);
  };

  const listItems = waivers.map((item) => (
    <li
      aria-selected={isItemSelected(item)}
      key={policyWaiverId(item)}
      onClick={() => handleClick(item)}
      className={listClass(item)}
      ref={isItemSelected(item) ? selectedElementRef : null}
      data-testid={isItemSelected(item) ? 'selected' : ''}
    >
      <NxThreatIndicator policyThreatLevel={item.threatLevel}></NxThreatIndicator>
      <span className="nx-list__text">{getFullPolicyName(item)}</span>
      <div className="nx-list__subtext iq-waivers-list-item-info">
        {!item.isAutoWaiver && (
          <>
            {isWaiverAllVersionsOrExact(item) ? (
              <ComponentDisplay component={item} truncate={true} matcherStrategy={componentMatchStrategy(item)} />
            ) : (
              'All components'
            )}
          </>
        )}
        <NxOverflowTooltip>
          <span className="nx-truncate-ellipsis">{getFullOwner(item)}</span>
        </NxOverflowTooltip>
      </div>
    </li>
  ));

  return <ul className="nx-list nx-list--clickable">{listItems}</ul>;
}

export const SidebarWaiverFilterDataType = PropTypes.shape({
  id: PropTypes.string.isRequired,
  policyName: PropTypes.string.isRequired,
  threatLevel: PropTypes.number.isRequired,
  ownerType: PropTypes.string.isRequired,
  ownerId: PropTypes.string.isRequired,
  ownerName: PropTypes.string.isRequired,
  componentMatchStrategy: PropTypes.string.isRequired,
});

export const SidebarWaiverDetailsDataType = PropTypes.shape({
  policyWaiverId: PropTypes.string.isRequired,
  policyName: PropTypes.string.isRequired,
  threatLevel: PropTypes.number.isRequired,
  scopeOwnerType: PropTypes.string.isRequired,
  scopeOwnerId: PropTypes.string.isRequired,
  scopeOwnerName: PropTypes.string.isRequired,
  matcherStrategy: PropTypes.string.isRequired,
});

SidebarNavWaiversList.propTypes = {
  currentWaiverId: PropTypes.string,
  waivers: PropTypes.arrayOf(PropTypes.oneOfType(SidebarWaiverFilterDataType, SidebarWaiverDetailsDataType)),
  onClick: PropTypes.func.isRequired,
  scrollToSelection: PropTypes.bool.isRequired,
};
