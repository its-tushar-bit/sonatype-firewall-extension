/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as PropTypes from 'prop-types';

import IqPopover from '../../../react/IqPopover';
import DeleteWaiverModalContainer from '../../../waivers/deleteWaiverModal/DeleteWaiverModalContainer';
import ComponentWaiversPopoverTable from './ComponentWaiversPopoverTable';
import { waiverType } from '../../../util/waiverUtils';

export default function ComponentWaiversPopover(props) {
  const {
    title,
    componentName,
    componentNameWithoutVersion,
    toggleComponentWaiversPopover,
    waivers,
    setWaiverToDelete,
    waiverToDelete,
  } = props;

  return (
    <IqPopover id="component-waivers-container" size="extra-large" onClose={toggleComponentWaiversPopover}>
      {waiverToDelete && <DeleteWaiverModalContainer />}
      <IqPopover.Header
        className="component-waivers-header"
        id="component-waivers-header"
        buttonId="component-waivers-close-btn"
        onClose={toggleComponentWaiversPopover}
        headerTitle={title || 'Component Waivers'}
      />
      <div className="component-waivers">
        <ComponentWaiversPopoverTable {...{ waivers, setWaiverToDelete, componentName, componentNameWithoutVersion }} />
      </div>
    </IqPopover>
  );
}

ComponentWaiversPopover.propTypes = {
  title: PropTypes.string,
  componentName: PropTypes.string,
  componentNameWithoutVersion: PropTypes.string,
  toggleComponentWaiversPopover: PropTypes.func.isRequired,
  waivers: PropTypes.arrayOf(PropTypes.shape(waiverType)),
  waiverToDelete: PropTypes.shape(waiverType),
  setWaiverToDelete: PropTypes.func.isRequired,
};
