/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import PropTypes from 'prop-types';
import cx from 'classnames';
import { DependencyTypeTag, ComponentLabelTag, ComponentFormatTag } from '../../react/tag';
import SbomComponentMatchStateTag from 'MainRoot/sbomManager/features/componentDetails/SbomComponentMatchStateTag';

const nothingToRender = ({ format, dependencyType, isInnerSource, labels, matchState }) =>
  !format &&
  (!dependencyType || dependencyType === 'unknown') &&
  !isInnerSource &&
  labels.length === 0 &&
  (!matchState || matchState !== 'similar');

export const ComponentDetailsTags = ({
  format,
  dependencyType,
  isInnerSource,
  filename,
  matchState,
  labels = [],
  ...props
}) => {
  if (nothingToRender({ format, dependencyType, isInnerSource, labels, matchState })) {
    return null;
  }
  const showDependencyTypeTags = (!!dependencyType && dependencyType !== 'unknown') || isInnerSource;
  return (
    <div {...props} className={cx('component-details-header__tags', props.className)}>
      {!!format && <ComponentFormatTag name={format} data-testid="component-details-tag" />}
      {showDependencyTypeTags && (
        <>
          {dependencyType && dependencyType !== 'unknown' && (
            <DependencyTypeTag type={dependencyType} data-testid="component-details-tag" />
          )}
          {isInnerSource && <DependencyTypeTag type="innerSource" data-testid="component-details-tag" />}
        </>
      )}
      {labels.length > 0 && (
        <>
          {labels.map(({ id, color, label, description }) => (
            <ComponentLabelTag key={id} color={color} description={description} data-testid="component-details-tag">
              {label}
            </ComponentLabelTag>
          ))}
        </>
      )}
      {<SbomComponentMatchStateTag filename={filename} matchState={matchState} />}
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
  filename: PropTypes.string,
  matchState: PropTypes.string,
};
ComponentDetailsTags.propTypes = componentDetailsTagsPropTypes;

export default ComponentDetailsTags;
