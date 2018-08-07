/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { pipe } from 'ramda';
import sjcl from 'sjcl';

/**
 * Computes the SHA-256 hash of a string and returns it in hex-digit encoded form
 */
const hash = pipe(sjcl.hash.sha256.hash, sjcl.codec.hex.fromBits);
export default hash;
