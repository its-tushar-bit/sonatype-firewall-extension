/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import axios from 'axios';

import { pathSet } from '../util/jsUtil';

// Http cache-buster interceptor
axios.interceptors.request.use((config) => pathSet(['params', 'timestamp'], Date.now(), config));
