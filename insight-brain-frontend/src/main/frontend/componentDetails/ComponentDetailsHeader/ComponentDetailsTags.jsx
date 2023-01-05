/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import PropTypes from 'prop-types';
import cx from 'classnames';
import { DependencyTypeTag, ComponentLabelTag, ComponentFormatTag } from '../../react/tag';

const nothingToRender = ({ format, dependencyType, isInnerSource, labels }) =>
  !format && (!dependencyType || dependencyType === 'unknown') && !isInnerSource && labels.length === 0;

export const ComponentDetailsTags = ({ format, dependencyType, isInnerSource, labels = [], ...props }) => {
  if (nothingToRender({ format, dependencyType, isInnerSource, labels })) {
    return null;
  }
  const showDependencyTypeTags = (!!dependencyType && dependencyType !== 'unknown') || isInnerSource;
  return (
    <div {...props} className={cx('component-details-header__tags', props.className)}>
      {!!format && <ComponentFormatTag name={format} />}
      {showDependencyTypeTags && (
        <>
          {dependencyType && dependencyType !== 'unknown' && <DependencyTypeTag type={dependencyType} />}
          {isInnerSource && <DependencyTypeTag type="innerSource" />}
        </>
      )}
      {labels.length > 0 && (
        <>
          {labels.map(({ id, color, label, description }) => (
            <ComponentLabelTag key={id} color={color} description={description}>
              {label}
            </ComponentLabelTag>
          ))}
        </>
      )}
    </div>
  );
};

export const componentDetailsTagsPropTypes = {
  className: PropTypes.string,
  format: PropTypes.string,
  isInnerSource: PropTypes.bool,
  dependencyType: PropTypes.oneOf(['direct', 'transitive', 'unknown']),
  labels: PropTypes.arrayOf(
    PropTypes.shape({
      id: PropTypes.string,
      label: PropTypes.string,
      description: PropTypes.string,
      color: PropTypes.string,
    })
  ),
};
ComponentDetailsTags.propTypes = componentDetailsTagsPropTypes;

export default ComponentDetailsTags;
