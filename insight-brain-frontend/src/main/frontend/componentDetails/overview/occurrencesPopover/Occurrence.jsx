/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as PropTypes from 'prop-types';
import { NxList } from '@sonatype/react-shared-components';

export default function Occurrence({ occurrence }) {
  const { isDependency, dirname, basename } = occurrence;

  return (
    <NxList.Item className="iq-occurrence">
      <NxList.Text>
        <span className="iq-occurrence__basename">
          {isDependency && <span>Dependency </span>}
          {basename}
        </span>{' '}
        {dirname && (
          <span className="iq-occurrence__dirname">
            <span className="iq-occurrence__location">{' located at '}</span>
            {isDependency && <span>Module </span>}
            {dirname}
          </span>
        )}
      </NxList.Text>
    </NxList.Item>
  );
}

Occurrence.propTypes = {
  occurrence: PropTypes.shape({
    isDependency: PropTypes.bool.isRequired,
    dirname: PropTypes.string,
    basename: PropTypes.string.isRequired,
  }).isRequired,
};
