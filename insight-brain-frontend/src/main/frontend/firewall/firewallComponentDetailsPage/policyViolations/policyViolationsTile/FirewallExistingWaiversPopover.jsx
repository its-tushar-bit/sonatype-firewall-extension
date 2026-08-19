/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as PropTypes from 'prop-types';

import IqPopover from 'MainRoot/react/IqPopover';
import DeleteWaiverModalContainer from 'MainRoot/waivers/deleteWaiverModal/DeleteWaiverModalContainer';
import ComponentWaiversPopoverTable from 'MainRoot/componentDetails/ViolationsTableTile/componentWaivers/ComponentWaiversPopoverTable';
import { waiverType } from 'MainRoot/util/waiverUtils';

export default function FirewallExistingWaiversPopover(props) {
  const {
    title,
    componentName,
    componentNameWithoutVersion,
    setShowComponentWaiversPopover,
    waivers,
    setWaiverToDelete,
    waiverToDelete,
  } = props;

  return (
    <IqPopover
      id="component-waivers-container"
      size="extra-large"
      onClose={() => setShowComponentWaiversPopover(false)}
    >
      {waiverToDelete && <DeleteWaiverModalContainer />}
      <IqPopover.Header
        className="component-waivers-header"
        id="component-waivers-close-btn"
        buttonId="component-waivers-close-btn"
        onClose={() => setShowComponentWaiversPopover(false)}
        headerTitle={title || 'Component Waivers'}
      />
      <div className="component-waivers">
        <ComponentWaiversPopoverTable {...{ waivers, setWaiverToDelete, componentName, componentNameWithoutVersion }} />
      </div>
    </IqPopover>
  );
}

FirewallExistingWaiversPopover.propTypes = {
  title: PropTypes.string,
  componentName: PropTypes.string,
  componentNameWithoutVersion: PropTypes.string,
  setShowComponentWaiversPopover: PropTypes.func.isRequired,
  waivers: PropTypes.arrayOf(PropTypes.shape(waiverType)),
  waiverToDelete: PropTypes.shape(waiverType),
  setWaiverToDelete: PropTypes.func.isRequired,
};
