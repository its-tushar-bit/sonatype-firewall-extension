/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/**
 * Framework-neutral core for Agent P: domain model, run lifecycle, recommendation selection and
 * onboarding coordination, expressed behind ports that hosts implement.
 *
 * <p>
 * This module is deliberately free of Spring, JAX-RS, the AWS SDK, Quartz, source-control SDKs,
 * jOOQ and Redis clients; the {@code maven-enforcer-plugin} configuration in {@code pom.xml} makes
 * that a build failure rather than a convention. It targets Java 21 while the rest of the reactor
 * targets Java 25, because its primary consumer runs on Java 21 and a Java 21 toolchain cannot read
 * Java 25 class files.
 */
package com.sonatype.agp.core;
