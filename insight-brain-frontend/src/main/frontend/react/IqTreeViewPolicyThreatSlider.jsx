/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useState, Fragment } from 'react';
import classnames from 'classnames';
import * as PropTypes from 'prop-types';
import { NxTreeView, NxTreeViewChild, NxPolicyThreatSlider } from '@sonatype/react-shared-components';
import { ensureElement } from '@sonatype/react-shared-components/util/reactUtil';

export default function IqTreeViewPolicyThreatSlider(props) {
  const { id, value, onChange, disabled, children, className } = props;

  const [isOpen, toggleOpen] = useState(false),
    onToggleCollapse = () => {
      toggleOpen(!isOpen);
    };

  const wrappedTriggerContent = ensureElement(children),
    counterClasses = classnames('nx-counter', { 'nx-counter--active': !disabled }, className);

  const triggerWithCounter = (
    <Fragment>
      {wrappedTriggerContent}
      <div className={counterClasses}>
        {value[0]} – {value[1]}
      </div>
    </Fragment>
  );

  return (
    <NxTreeView
      onToggleCollapse={onToggleCollapse}
      isOpen={isOpen}
      id={id}
      triggerContent={triggerWithCounter}
      disabled={disabled}
      className="nx-collapsible-items--threat-slider"
    >
      <NxTreeViewChild>
        <NxPolicyThreatSlider value={value} onChange={onChange} />
      </NxTreeViewChild>
    </NxTreeView>
  );
}

IqTreeViewPolicyThreatSlider.propTypes = {
  value: NxPolicyThreatSlider.propTypes.value,
  className: PropTypes.string,
  onChange: PropTypes.func,
  disabled: PropTypes.bool,
  id: PropTypes.string,
  children: PropTypes.node,
};
