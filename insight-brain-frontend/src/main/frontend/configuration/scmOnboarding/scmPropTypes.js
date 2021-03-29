/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as PropTypes from 'prop-types';

import {NxTextInput} from '@sonatype/react-shared-components';
import {pick} from 'ramda';

export const textInputPropType = PropTypes.shape(pick(['value', 'isPristine', 'validationErrors'],
    NxTextInput.propTypes));

