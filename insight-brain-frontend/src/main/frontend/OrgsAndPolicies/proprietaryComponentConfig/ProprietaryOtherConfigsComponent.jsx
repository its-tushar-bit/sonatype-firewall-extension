/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { isEmpty } from 'ramda';
import { NxH3, NxList } from '@sonatype/react-shared-components';
import * as PropTypes from 'prop-types';

export default function ProprietaryOtherConfigsComponent(props) {
  const { innerEl } = props;
  const { proprietaryConfig = {} } = innerEl;
  const isPackagesNotEmpty = !isEmpty(proprietaryConfig.packages);
  const isRegexesNotEmpty = !isEmpty(proprietaryConfig.regexes);

  return (
    <>
      {(isRegexesNotEmpty || isPackagesNotEmpty) && (
        <div className="inherited-proprietary-component-matchers">
          <NxH3 id={`inherited-proprietary-component-matchers-title-${proprietaryConfig.ownerId}`}>
            Inherited from {innerEl.ownerName}
          </NxH3>
          <NxList aria-labelledby={`inherited-proprietary-component-matchers-title-${proprietaryConfig.ownerId}`}>
            {isRegexesNotEmpty &&
              proprietaryConfig.regexes.map((name) => (
                <NxList.Item key={name}>
                  <NxList.Text>{name}</NxList.Text>
                  <NxList.Subtext>RegEx</NxList.Subtext>
                </NxList.Item>
              ))}
            {isPackagesNotEmpty &&
              proprietaryConfig.packages.map((name) => (
                <NxList.Item key={name}>
                  <NxList.Text>{name}</NxList.Text>
                  <NxList.Subtext>Package</NxList.Subtext>
                </NxList.Item>
              ))}
          </NxList>
        </div>
      )}
    </>
  );
}

ProprietaryOtherConfigsComponent.propTypes = {
  innerEl: PropTypes.shape({
    ownerName: PropTypes.string.isRequired,
    proprietaryConfig: PropTypes.shape({
      regexes: PropTypes.arrayOf(PropTypes.string),
      packages: PropTypes.arrayOf(PropTypes.string),
    }),
  }),
};
