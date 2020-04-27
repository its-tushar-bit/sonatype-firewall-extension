/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as PropTypes from 'prop-types';
import NxThreatBar from '@sonatype/react-shared-components/components/NxThreatBar/NxThreatBar';
import classnames from 'classnames';

export default function SidebarNavViolationList(props) {
  const {
    currentViolationId,
    violations,
    onClick
  } = props;

  const listClass = (item) => classnames('nx-list__item', {
    selected: item.policyViolationId === currentViolationId
  });

  const listItems = violations.map((item) =>
    <li key = {item.policyViolationId}
        onClick={() => onClick(item.policyViolationId)}
        className={listClass(item)}>
      <NxThreatBar policyThreatLevel={item.threatLevel}></NxThreatBar>
      <span className="iq-threat-number iq-threat-number--sidebar-nav">{item.threatLevel}</span>
      <span className="test-sidebar-nav-violation-policy-name">{item.policyName}</span>
    </li>
  );

  return (
    <ul>
      {listItems}
    </ul>
  );
}

SidebarNavViolationList.propTypes = {
  currentViolationId: PropTypes.string,
  violations: PropTypes.arrayOf(
      PropTypes.shape({
        policyName: PropTypes.string.isRequired,
        policyViolationId: PropTypes.string.isRequired,
        threatLevel: PropTypes.number.isRequired
      })
  ),
  onClick: PropTypes.func.isRequired
};
