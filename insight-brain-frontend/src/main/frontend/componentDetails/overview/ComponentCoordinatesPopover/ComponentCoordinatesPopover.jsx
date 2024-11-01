/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import PropTypes from 'prop-types';
import { useDispatch, useSelector } from 'react-redux';

import { IqPopover } from 'MainRoot/react/IqPopover';

import { selectShowComponentCoordinatesPopover } from '../overviewSelectors';
import { actions } from '../overviewSlice';
import { NxCopyToClipboard } from '@sonatype/react-shared-components';

import './ComponentCoordinatesPopover.scss';

export default function ComponentCoordinatesPopover(props) {
  const { displayName, componentFormat, packageUrl } = props;
  const showComponentCoordinatesPopover = useSelector(selectShowComponentCoordinatesPopover);
  const dispatch = useDispatch();
  const onClose = () => dispatch(actions.toggleShowComponentCoordinatesPopover());

  if (!showComponentCoordinatesPopover) {
    return null;
  }

  return (
    <IqPopover id="iq-component-coordinates-popover" size="medium" onClose={onClose}>
      <IqPopover.Header
        buttonId="iq-component-coordinates-popover-close-btn"
        onClose={onClose}
        headerTitle="Component Coordinates"
      />
      <dl className="nx-read-only nx-read-only--grid">
        <div className="nx-read-only__item">
          <dt className="nx-read-only__label">Type</dt>
          <dd className="nx-read-only__data">{componentFormat}</dd>
        </div>

        {displayName?.parts.map(
          ({ field, value }) =>
            field && (
              <div className="nx-read-only__item" key={field}>
                <dt className="nx-read-only__label">{field}</dt>
                <dd className="nx-read-only__data">{value}</dd>
              </div>
            )
        )}

        <NxCopyToClipboard
          className="iq-component-coordinates-copy-to-clipboard"
          label="Package URL"
          content={packageUrl}
          inputProps={{ rows: 2 }}
        />
      </dl>
    </IqPopover>
  );
}

ComponentCoordinatesPopover.propTypes = {
  displayName: PropTypes.shape({
    parts: PropTypes.array,
  }).isRequired,
  componentFormat: PropTypes.string.isRequired,
  packageUrl: PropTypes.string.isRequired,
};
