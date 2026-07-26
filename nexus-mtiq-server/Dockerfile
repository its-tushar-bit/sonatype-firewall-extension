#
# Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
# Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
# "Sonatype" is a trademark of Sonatype, Inc.
#
# MTIQ Docker image — base-less (FROM scratch) per the DEVEX-2503 slim-image
# paved path (same strategy Repo Cloud / Firewall Pro adopted):
#
#   * The builder assembles a busybox + musl userland and the JRE
#     (openjdk25-jre-headless) into /rootfs via `apk add --root`, so a real apk
#     package database ships in the final image. Sonatype's own container
#     scanner reads that DB, so a CVE in the JRE or any OS package raises a
#     policy violation (a hand-built jlink runtime would be invisible to it).
#   * The runtime stage is FROM scratch — no operating-system base image, and
#     therefore a dramatically smaller CVE surface than the old ubuntu base.
#   * nexus-mtiq-server.jar is a shaded uber jar (Main-Class + Multi-Release), so
#     the JRE runs it directly with `java -jar`; there is no bundled JDK.
#
# Image inputs are ordinary module build outputs in target/ (nexus-mtiq-server.jar
# uber jar, otel-java-agent.jar, jvm.options) produced by `mvn install`. CI builds
# and pushes the multi-arch image (see Jenkinsfile.build); for local builds (single
# or multi-arch) see nexus-mtiq-server/README.md ("Docker builds").
ARG SONATYPE_PRIVATE_REGISTRY=sonatype.repo.sonatype.app/docker-all
# Digest-pinned (as the previous ubuntu base was) for reproducible builds; the
# tag is informational. Alpine >= 3.23 is required for the openjdk25 packages.
# Bump tag + digest together; obtain the multi-arch digest with:
#   docker buildx imagetools inspect alpine:<tag> --format '{{.Manifest.Digest}}'
ARG ALPINE_BASE_IMAGE=alpine:3.23@sha256:fd791d74b68913cbb027c6546007b3f0d3bc45125f797758156952bc2d6daf40

# ---------- Stage 1: builder — assemble a scannable rootfs + lay out the app ----------
FROM $SONATYPE_PRIVATE_REGISTRY/$ALPINE_BASE_IMAGE AS builder

# Install the userland + JRE into a staged root via apk so the apk package
# database (/lib/apk/db/installed) ships in the final scratch image and every
# component (busybox, musl, zlib, jemalloc, tini, openjdk25-jre-headless) is
# enumerable by the container scanner.
RUN set -eu; \
    mkdir -p /rootfs/etc/apk; \
    cp -a /etc/apk/repositories /rootfs/etc/apk/repositories; \
    cp -a /etc/apk/keys /rootfs/etc/apk/keys; \
    # Runtime deps beyond the JRE: curl (health/debug); git — the native git
    # binary is preferred over JGit for SCM (more capable, supports SSH) and the
    # runtime-environment telemetry shells out to `git --version`.
    apk add --root /rootfs --initdb --no-scripts --no-cache \
      busybox openjdk25-jre-headless jemalloc tini zlib curl git; \
    # Build-time guards (fail the build now instead of shipping a broken image):
    # the musl dynamic linker must be present, and the JRE must have installed to
    # the exact path the runtime stage hard-codes as JAVA_HOME (test -x = the java
    # launcher exists and is executable there).
    ls /rootfs/lib/ld-musl-*.so.1 >/dev/null; \
    test -x /rootfs/usr/lib/jvm/java-25-openjdk/bin/java; \
    # --no-scripts skips busybox's applet-symlink post-install hook; create the
    # applets the entrypoint (sh, sed) and the ECS healthcheck (wget) resolve on
    # PATH. Limiting the set keeps the post-exploitation toolkit small.
    for a in sh sed cat ls id uname wget; do ln -sf /bin/busybox /rootfs/bin/$a; done; \
    # No shadow/useradd on scratch; some JDK code calls getpwuid() and needs an entry.
    printf 'root:x:0:0:root:/root:/bin/sh\n' > /rootfs/etc/passwd; \
    printf 'root:x:0:\n' > /rootfs/etc/group; \
    # The JVM writes hsperfdata/temp before java.io.tmpdir applies.
    mkdir -p /rootfs/tmp && chmod 1777 /rootfs/tmp

# Lay the application into the rootfs. Java-free: the runtime is the apk JRE above.
ENV APP_HOME=/opt/sonatype/nexus-iq-server
RUN mkdir -p /rootfs${APP_HOME}/bin /rootfs/opt/otel /rootfs/sonatype-work
COPY target/nexus-mtiq-server.jar /rootfs${APP_HOME}/nexus-mtiq-server.jar
COPY target/jvm.options /rootfs${APP_HOME}/jvm.options
# The OTel agent is baked into the JAVA_OPTS default below, so it survives entry
# point and command overrides. A container-level JAVA_OPTS override replaces that
# default and must re-include the -javaagent flag; OTEL_JAVAAGENT_ENABLED=false
# disables instrumentation but the JVM still loads the jar, so drop the -javaagent
# flag to skip loading entirely.
COPY target/otel-java-agent.jar /rootfs/opt/otel/otel-java-agent.jar
COPY docker-entrypoint.sh /rootfs${APP_HOME}/bin/docker-entrypoint.sh
RUN chmod +x /rootfs${APP_HOME}/bin/docker-entrypoint.sh

# ---------- Stage 2: runtime — no operating-system base image ----------
FROM scratch

LABEL vendor=Sonatype \
  maintainer="MTIQ Team <mtiq-dev@sonatype.com>" \
  com.sonatype.name="MTIQ Server Docker Image"

# rootfs (busybox userland + apk DB + musl/zlib/jemalloc/tini + JRE) and the app.
COPY --from=builder /rootfs/ /

ENV JAVA_HOME=/usr/lib/jvm/java-25-openjdk
ENV PATH=/usr/lib/jvm/java-25-openjdk/bin:/usr/bin:/bin
# Alpine ships jemalloc at /usr/lib (no arch-triplet subdir, unlike Debian/Ubuntu).
ENV LD_PRELOAD=/usr/lib/libjemalloc.so.2
ENV JAVA_OPTS="-javaagent:/opt/otel/otel-java-agent.jar -Dgit.without.shell=true -Djava.util.prefs.userRoot=/sonatype-work/javaprefs -XX:+AlwaysPreTouch -XX:+CrashOnOutOfMemoryError -XshowSettings:vm"

# This is where we will store persistent data
VOLUME /sonatype-work

# Expose the ports
EXPOSE 8070
EXPOSE 8071

WORKDIR /opt/sonatype/nexus-iq-server

# tini (PID 1) reaps orphans; the entrypoint is a thin shim that assembles the
# java command from JAVA_OPTS + jvm.options and execs the JRE against the uber
# jar. tini is referenced by absolute path (/sbin/tini) — it is not on PATH, so
# the exec-form ENTRYPOINT would not otherwise resolve it. CMD is the Dropwizard
# sub-command + config path, so an ECS `command` override replaces only that,
# not the launcher.
ENTRYPOINT ["/sbin/tini", "--", "/opt/sonatype/nexus-iq-server/bin/docker-entrypoint.sh"]
CMD ["server", "/etc/mtiq-server/config.yaml"]
