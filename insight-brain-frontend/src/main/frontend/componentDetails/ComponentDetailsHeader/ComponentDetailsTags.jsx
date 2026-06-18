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
import VariantSelectionTag from './VariantSelectionTag';

const nothingToRender = ({ format, dependencyType, isInnerSource, labels, matchState, variantSelected }) =>
  !format &&
  (!dependencyType || dependencyType === 'unknown') &&
  !isInnerSource &&
  labels.length === 0 &&
  (!matchState || matchState !== 'similar') &&
  !variantSelected;

export const ComponentDetailsTags = ({
  format,
  dependencyType,
  isInnerSource,
  filename,
  matchState,
  variantSelected,
  labels = [],
  ...props
}) => {
  if (nothingToRender({ format, dependencyType, isInnerSource, labels, matchState, variantSelected })) {
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
      <VariantSelectionTag variantSelected={variantSelected} />
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
  variantSelected: PropTypes.bool,
};
ComponentDetailsTags.propTypes = componentDetailsTagsPropTypes;

export default ComponentDetailsTags;
