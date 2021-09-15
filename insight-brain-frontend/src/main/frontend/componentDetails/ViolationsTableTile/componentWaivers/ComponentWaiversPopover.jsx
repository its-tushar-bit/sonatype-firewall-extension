/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as PropTypes from 'prop-types';

import { NxButton, NxFontAwesomeIcon } from '@sonatype/react-shared-components';
import { faArrowToRight } from '@fortawesome/pro-solid-svg-icons';

import IqPopover from '../../../react/IqPopover';
import DeleteWaiverModalContainer from '../../../waivers/deleteWaiverModal/DeleteWaiverModalContainer';
import ComponentWaiversPopoverTable from './ComponentWaiversPopoverTable';
import { waiverType } from '../../../util/waiverUtils';

export default function ComponentWaiversPopover(props) {
  const { title, componentName, toggleComponentWaiversPopover, waivers, setWaiverToDelete, waiverToDelete } = props;

  return (
    <IqPopover id="component-waivers-container" size="automatic" onClose={toggleComponentWaiversPopover}>
      {waiverToDelete && <DeleteWaiverModalContainer />}
      <IqPopover.Header className="component-waivers-header" id="component-waivers-header">
        <div className="component-waivers-header__title">
          <h3 className="nx-h3 component-waivers-header__title-text">{title || 'Component Waivers'}</h3>
          <NxButton
            id="component-waivers-close-btn"
            onClick={toggleComponentWaiversPopover}
            variant="icon-only"
            title="Close"
            className="component-waivers-header__title-close"
          >
            <NxFontAwesomeIcon icon={faArrowToRight} />
          </NxButton>
        </div>
      </IqPopover.Header>

      <div className="component-waivers">
        <ComponentWaiversPopoverTable {...{ waivers, setWaiverToDelete, componentName }} />
      </div>
    </IqPopover>
  );
}

ComponentWaiversPopover.propTypes = {
  title: PropTypes.string,
  componentName: PropTypes.string,
  toggleComponentWaiversPopover: PropTypes.func.isRequired,
  waivers: PropTypes.arrayOf(PropTypes.shape(waiverType)),
  waiverToDelete: PropTypes.shape(waiverType),
  setWaiverToDelete: PropTypes.func.isRequired,
};
