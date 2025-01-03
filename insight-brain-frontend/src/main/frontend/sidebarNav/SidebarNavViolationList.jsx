/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as PropTypes from 'prop-types';
import { NxThreatIndicator } from '@sonatype/react-shared-components';
import classnames from 'classnames';

import { UPDATE_DIMENSIONS_TIMEOUT } from '../util/constants';
import { getArtifactName } from '../util/componentNameUtils';

export default function SidebarNavViolationList(props) {
  const { currentViolationId, violations, onClick, scrollToSelection } = props;

  const SCROLL_TIMEOUT = UPDATE_DIMENSIONS_TIMEOUT + 100;

  // Have to access `useRef` and `useEffect` through the React object due to testing limitations
  const selectedElementRef = React.useRef(null);

  React.useEffect(() => {
    if (scrollToSelection) {
      const timeoutId = setTimeout(
        () => {
          if (selectedElementRef && selectedElementRef.current) {
            selectedElementRef.current.scrollIntoView();
          }
        },
        SCROLL_TIMEOUT,
        'sidebar-nav'
      ); // supply a flag so we can identify this call in tests

      return () => {
        clearTimeout(timeoutId);
      };
    }
  });

  const isItemSelected = (item) => item.policyViolationId === currentViolationId;

  const listClass = (item) =>
    classnames('nx-list__item', {
      selected: isItemSelected(item),
    });

  const getFullPolicyName = (item) => `${item.threatLevel} ${item.policyName}`;

  const listItems = violations.map((item) => (
    <li
      key={item.policyViolationId}
      onClick={() => onClick(item.policyViolationId)}
      className={listClass(item)}
      ref={isItemSelected(item) ? selectedElementRef : null}
    >
      <NxThreatIndicator policyThreatLevel={item.threatLevel}></NxThreatIndicator>
      <span className="nx-list__text">{getFullPolicyName(item)}</span>
      <div className="nx-list__subtext">{getArtifactName(item)}</div>
    </li>
  ));

  return <ul className="nx-list nx-list--clickable">{listItems}</ul>;
}

export const SidebarViolationDataType = PropTypes.shape({
  policyName: PropTypes.string.isRequired,
  policyViolationId: PropTypes.string.isRequired,
  threatLevel: PropTypes.number.isRequired,
});

SidebarNavViolationList.propTypes = {
  currentViolationId: PropTypes.string,
  violations: PropTypes.arrayOf(SidebarViolationDataType),
  onClick: PropTypes.func.isRequired,
  scrollToSelection: PropTypes.bool.isRequired,
};
