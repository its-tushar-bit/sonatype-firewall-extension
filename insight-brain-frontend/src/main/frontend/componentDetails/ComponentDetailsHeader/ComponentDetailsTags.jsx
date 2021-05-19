/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { Fragment } from 'react';
import PropTypes from 'prop-types';
import cx from 'classnames';
import { DependencyTypeTag, ComponentLabelTag, ComponentFormatTag } from '../../react/tag';

export const ComponentDetailsTags = ({ format, dependencyType, labels = [], ...props }) => {
  if (!format && (!dependencyType || dependencyType === 'unknown') && labels.length === 0) {
    return null;
  }
  return (
    <dl {...props} className={cx('component-details-header__tags', props.className)}>
      {!!format && (
        <Fragment>
          <dt>Format</dt>
          <dd>
            <ComponentFormatTag name={format} />
          </dd>
        </Fragment>
      )}
      {!!dependencyType && dependencyType !== 'unknown' && (
        <Fragment>
          <dt>Dependancy Type</dt>
          <dd>
            <DependencyTypeTag type={dependencyType} />
          </dd>
        </Fragment>
      )}
      {labels.length > 0 && (
        <Fragment>
          <dt>Labels</dt>
          {labels.map(({ id, color, label }) => (
            <dd key={id}>
              <ComponentLabelTag color={color}>{label}</ComponentLabelTag>
            </dd>
          ))}
        </Fragment>
      )}
    </dl>
  );
};

export const propTypes = {
  className: PropTypes.string,
  format: PropTypes.string,
  dependencyType: PropTypes.oneOf(['direct', 'transitive', 'innersource', 'unknown']),
  labels: PropTypes.arrayOf(
    PropTypes.shape({
      id: PropTypes.string,
      label: PropTypes.string,
      description: PropTypes.string,
      color: PropTypes.string,
    })
  ),
};
ComponentDetailsTags.propTypes = propTypes;

export default ComponentDetailsTags;
