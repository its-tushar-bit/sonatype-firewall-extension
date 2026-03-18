/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.security.Provider;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;

import static java.nio.file.Files.createTempDirectory;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Factory class responsible for creating FIPS-compliant cryptographic providers. Handles both classpath-based and
 * embedded JAR-based FIPS provider loading.
 */
public final class FIPSProviderFactory
{
  private static final Logger log = getLogger(FIPSProviderFactory.class);

  public static final String FIPS_PROVIDER_CLASS_NAME = "org.bouncycastle.jcajce.provider.BouncyCastleFipsProvider";

  /**
   * Paths to FIPS JAR files embedded as resources within the shaded JAR. These locations are configured by the Maven
   * Shade Plugin's IncludeResourceTransformer, for example we do this in the insight-brain-service/pom.xml, which
   * embeds the FIPS dependencies as resources rather than shading their classes into the main JAR.
   */
  private static final String[] EMBEDDED_FIPS_JARS = {
    "lib/bc-fips.jar",
    "lib/bcpkix-fips.jar",
    "lib/bctls-fips.jar"
  };

  private static final String TEMP_DIRECTORY_NEXUS_FIPS_JARS = "nexus-fips-jars";

  private FIPSProviderFactory() {
    // Utility class
  }

  /**
   * Creates a FIPS-compliant BouncyCastle provider. First attempts to load from classpath, then falls back to embedded
   * JAR resources.
   *
   * @return FIPS {@link Provider} instance
   * @throws IllegalStateException if {@link Provider} cannot be created
   */
  public static Provider createFipsProvider() {
    try {
      // Try loading from classpath first
      Provider provider = loadFromClasspath();
      if (provider != null) {
        return provider;
      }

      // Fall back to embedded FIPS JARs
      return loadFromEmbeddedJars();
    }
    catch (IOException e) {
      throw new IllegalStateException("Failed to create FIPS provider due to I/O error", e);
    }
    catch (ReflectiveOperationException e) {
      throw new IllegalStateException("Failed to create FIPS provider due to reflection error", e);
    }
    catch (IllegalStateException e) {
      throw new IllegalStateException("Failed to create FIPS provider", e);
    }
  }

  /**
   * Load FIPS {@link Provider} from existing classpath.
   *
   * @return FIPS {@link Provider} if available on classpath, null otherwise
   */
  @SuppressWarnings("resource")
  private static Provider loadFromClasspath() {
    log.debug("Loading FIPS provider from classpath");

    try {
      // Use reflection to avoid a NoClassDefFoundError if FIPS classes not available. That type of error
      // would instantly exit the java process, while if we fail to load it through reflection we can handle it.
      Class<?> fipsProviderClass = Class.forName(FIPS_PROVIDER_CLASS_NAME);
      URL fipsJarUrl = fipsProviderClass.getProtectionDomain().getCodeSource().getLocation();

      // Use try-with-resources - we need to keep the classloader alive so FIPS classes remain available
      URLClassLoader fipsClassLoader = new URLClassLoader(new URL[]{fipsJarUrl}, null);
      Class<?> providerClass = fipsClassLoader.loadClass(FIPS_PROVIDER_CLASS_NAME);
      Provider provider = (Provider) providerClass.getConstructor().newInstance();

      log.debug("Successfully loaded FIPS provider from classpath");
      return provider;
    }
    catch (Exception ignored) {
      log.debug("FIPS JARs not available on classpath");
      return null;
    }
  }

  /**
   * Loads FIPS provider from embedded JAR resources located at {@link #EMBEDDED_FIPS_JARS}. Extracts embedded JARs to
   * temporary files and loads the {@link Provider}.
   *
   * @return FIPS {@link Provider} loaded from embedded resources
   * @throws IOException if I/O operations fail
   * @throws ReflectiveOperationException if reflection operations fail
   * @throws IllegalStateException if JAR extraction is incomplete
   */
  @SuppressWarnings("resource")
  private static Provider loadFromEmbeddedJars() throws IOException, ReflectiveOperationException, IllegalStateException {
    log.debug("Loading FIPS provider from embedded resources");

    // Extract embedded FIPS JARs to temp files (URLClassLoader can't handle nested JARs)
    File tempDir = createTempDirectory(TEMP_DIRECTORY_NEXUS_FIPS_JARS).toFile();
    tempDir.deleteOnExit();

    FIPSJarExtractionResult result = extractEmbeddedJars(tempDir);
    List<URL> fipsUrls = result.extractedUrls();

    // Check if all expected JARs were successfully extracted
    if (fipsUrls.size() < EMBEDDED_FIPS_JARS.length) {
      String expectedJars = String.join(", ", EMBEDDED_FIPS_JARS);
      String foundJars = result.successfulJars().isEmpty() ? "none" : String.join(", ", result.successfulJars());
      String missingJars = result.missingJars().isEmpty() ? "none" : String.join(", ", result.missingJars());

      throw new IllegalStateException(String.format(
          "Incomplete FIPS JAR extraction. Expected %d JARs but found %d. " +
              "Expected: [%s], Found: [%s], Missing: [%s]",
          EMBEDDED_FIPS_JARS.length, fipsUrls.size(), expectedJars, foundJars, missingJars));
    }

    // Use without try-with-resources - we need to keep the classloader alive so FIPS classes remain available
    URLClassLoader fipsClassLoader = new URLClassLoader(fipsUrls.toArray(new URL[0]), null);
    Class<?> providerClass = fipsClassLoader.loadClass(FIPS_PROVIDER_CLASS_NAME);
    Provider provider = (Provider) providerClass.getConstructor().newInstance();

    log.debug("Successfully loaded FIPS provider from {} embedded JAR(s)", fipsUrls.size());
    return provider;
  }

  /**
   * Result of embedded JAR extraction operation.
   */
  public record FIPSJarExtractionResult(
      List<URL> extractedUrls,
      List<String> successfulJars,
      List<String> missingJars)
  {
  }

  /**
   * Extracts embedded FIPS JAR resources to temporary files.
   *
   * @param tempDir - temporary directory for extracted JARs.
   * @return FipsJarExtractionResult containing URLs and success/failure information
   */
  private static FIPSJarExtractionResult extractEmbeddedJars(final File tempDir) {
    List<URL> fipsUrls = new ArrayList<>();
    List<String> successfulJars = new ArrayList<>();
    List<String> missingJars = new ArrayList<>();

    for (String jarPath : EMBEDDED_FIPS_JARS) {
      try (InputStream inputStream = FIPSProviderFactory.class.getResourceAsStream("/" + jarPath)) {
        if (inputStream == null) {
          log.debug("FIPS JAR resource not found: {}", jarPath);
          missingJars.add(jarPath);
          continue;
        }

        String jarName = jarPath.substring(jarPath.lastIndexOf('/') + 1);
        File tempJar = new File(tempDir, jarName);

        Files.copy(inputStream, tempJar.toPath());
        tempJar.deleteOnExit();

        fipsUrls.add(tempJar.toURI().toURL());
        successfulJars.add(jarPath);
        log.trace("Extracted FIPS JAR: {} -> {}", jarPath, tempJar.getAbsolutePath());
      }
      catch (IOException e) {
        log.debug("Failed to extract FIPS JAR: {}", jarPath, e);
        missingJars.add(jarPath);
      }
    }

    return new FIPSJarExtractionResult(fipsUrls, successfulJars, missingJars);
  }
}
