/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.PlaywrightException;
import com.microsoft.playwright.TimeoutError;
import com.microsoft.playwright.Tracing;
import com.microsoft.playwright.Video;
import com.microsoft.playwright.options.WaitForSelectorState;
import com.sonatype.clm.testing.playwright.pages.BasePage;
import com.sonatype.clm.testing.playwright.utils.PlaywrightTiming;
import com.sonatype.clm.testing.playwright.utils.PlaywrightWaitUtils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.rules.TestName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Shared base class for Playwright-driven UI tests, independent of any specific server flavour.
 *
 * <p>
 * Owns only the browser-side lifecycle: Playwright/Browser singleton, per-test
 * {@link BrowserContext}/{@link Page}, tracing, video, screenshot-on-failure, console/page-error
 * capture, and the generic URL navigation/refresh helpers that need a {@link Page}.
 *
 * <p>
 * Concrete server-flavoured base classes ({@link AbstractIqUiTest} today; an eventual MTIQ-UI
 * sibling) extend this class and add the server-bootstrap and session helpers (login/logout,
 * licensing, DB setup) on top.
 *
 * <p>
 * Subclasses MUST initialise {@link #baseUrlFromTest} before any {@code @BeforeEach} runs &mdash;
 * typically in a static initialiser that boots the test server &mdash; because
 * {@link #setupPlaywrightTest()} reads it when constructing the {@link BrowserContext}.
 *
 * <p>
 * Timeouts and flake policy are documented in this module's {@code README.md}
 * (Troubleshooting section) and enforced by {@code PlaywrightStabilityRulesCheck}.
 */
@ExtendWith(AbstractPlaywrightTest.PlaywrightLifecycleExtension.class)
public abstract class AbstractPlaywrightTest
{
  private static final Logger log = LoggerFactory.getLogger(AbstractPlaywrightTest.class);

  private static final Path SCREENSHOT_DIR = Paths.get("target/playwright-screenshots");

  private static final Path VIDEO_DIR = Paths.get("target/playwright-videos");

  private static final Path TRACE_DIR = Paths.get("target/playwright-traces");

  private static final Path DIAGNOSTICS_DIR = Paths.get("target/playwright-diagnostics");

  /** Cap how many browser console / page-error lines we retain per test (avoid huge CI logs). */
  private static final int MAX_BROWSER_DIAGNOSTIC_LINES = 40;

  private static final int VIEWPORT_WIDTH = 1366;

  private static final int VIEWPORT_HEIGHT = 1064;

  /**
   * Controls whether Playwright traces and per-test screenshots are persisted. Tri-state:
   * <ul>
   * <li>{@code always} &mdash; capture trace + screenshot for every test.</li>
   * <li>{@code on-failure} &mdash; capture only when the current test failed (default; matches
   * this module's {@code pom.xml} default).</li>
   * <li>{@code off} &mdash; never capture; tracing is stopped without saving.</li>
   * </ul>
   * Override with {@code -Dplaywright.trace=always|on-failure|off}.
   */
  private static final String TRACE_MODE =
      System.getProperty("playwright.trace", "on-failure").toLowerCase();

  private static final boolean TRACE_ALWAYS = "always".equals(TRACE_MODE);

  private static final boolean TRACE_ON_FAILURE = "on-failure".equals(TRACE_MODE);

  /**
   * Controls whether the test browser context records a {@code .webm} video.
   * <p>
   * Off by default &mdash; videos are large and slow tests down measurably.
   * Pass {@code -Dplaywright.video=on} (or any value other than {@code off}) to enable recording.
   * When enabled, each test produces {@code target/playwright-videos/<testName>.webm}, embedded
   * inline by the Playwright HTML report's per-test card.
   */
  private static final boolean RECORD_VIDEO =
      "on".equalsIgnoreCase(System.getProperty("playwright.video", "off"));

  // volatile is needed in addition to the volatile playwrightInitialized guard: a thread that
  // sees the guard flip via the unsynchronized fast-path read also needs a happens-before edge
  // to the writes of these two fields. Without volatile, the JMM allows a stale null read.
  private static volatile Playwright playwright;

  private static volatile Browser browser;

  private static volatile boolean playwrightInitialized = false;

  private static final Object INIT_LOCK = new Object();

  /**
   * Base URL the per-test {@link BrowserContext} is bound to. Server-flavoured subclasses
   * (e.g. {@link AbstractIqUiTest}) assign this in their static initialiser once the embedded
   * server is started.
   */
  protected static String baseUrlFromTest;

  protected BrowserContext context;

  protected Page page;

  /**
   * {@code warning} / {@code error} console messages from the page during the current test.
   *
   * <p>
   * Written from Playwright's internal dispatcher thread (via {@link Page#onConsoleMessage}) and
   * read during {@link PlaywrightLifecycleExtension}'s capture. Must be a synchronized
   * list; the cap-check + add in {@link #appendDiagnosticLine} is guarded by a
   * {@code synchronized} block to keep the two operations atomic.
   */
  private final List<String> browserConsoleWarningsAndErrors =
      Collections.synchronizedList(new ArrayList<>());

  /**
   * Uncaught page JS exceptions (via {@link Page#onPageError}).
   *
   * <p>
   * Same threading model as {@link #browserConsoleWarningsAndErrors} — synchronized list with
   * atomic cap-check + add.
   */
  private final List<String> browserPageErrors =
      Collections.synchronizedList(new ArrayList<>());

  protected TestName testName;

  /**
   * Per-test lifecycle hook: captures Playwright failure artifacts <em>while the browser
   * context is still alive</em>, then closes it.
   *
   * <p>
   * Implemented as an {@link AfterEachCallback} so it runs after the test body and any
   * {@code @AfterEach} methods while the {@link Page}/{@link BrowserContext} created in
   * {@link #setupPlaywrightTest} is still open. It reads the outcome from
   * {@link ExtensionContext#getExecutionException()} and captures against the still-open page
   * before closing it.
   *
   * <p>
   * Capture/cleanup matrix:
   * <ul>
   * <li><b>passing test</b>: {@code discardTrace()} (or {@code saveTrace()} when
   * {@code playwright.trace=always}), then close.</li>
   * <li><b>failing test</b>: screenshot + {@code saveTrace()} (when {@code on-failure} or
   * {@code always}) + failure-diagnostics attachment, then close.</li>
   * </ul>
   */
  public static final class PlaywrightLifecycleExtension
      implements AfterEachCallback
  {
    @Override
    public void afterEach(final ExtensionContext context) {
      AbstractPlaywrightTest test = (AbstractPlaywrightTest) context.getRequiredTestInstance();
      String fqMethod =
          context.getRequiredTestClass().getName() + "." + context.getRequiredTestMethod().getName();
      test.captureAndCloseAfterTest(fqMethod, context.getDisplayName(),
          context.getExecutionException().orElse(null));
    }
  }

  /**
   * Captures Playwright failure artifacts (screenshot/trace/diagnostics) for a just-run test while
   * the {@link Page}/{@link BrowserContext} is still open, then closes them. Invoked by
   * {@link PlaywrightLifecycleExtension} after the test body and {@code @AfterEach} methods.
   *
   * @param fqMethod fully-qualified {@code <class>.<method>} of the test that ran
   * @param displayName Jupiter display name (for the END log line)
   * @param failure the test failure, or {@code null} if the test passed
   */
  private void captureAndCloseAfterTest(String fqMethod, String displayName, Throwable failure) {
    // Key artifacts on the filesystem-safe FQN slug (e.g. com.foo.BarTest.testFoo) so two test
    // classes with the same method name never clobber each other's screenshots/traces.
    String testKey = safeFileName(fqMethod);
    Video recordedVideo = null;
    try {
      if (failure != null) {
        captureScreenshot(testKey);
        if (TRACE_ALWAYS || TRACE_ON_FAILURE) {
          saveTrace(testKey);
        }
        logPlaywrightFailureDiagnostics(fqMethod, failure);
      }
      else if (TRACE_ALWAYS) {
        saveTrace(testKey);
      }
      else if (TRACE_ON_FAILURE) {
        discardTrace();
      }
      if (RECORD_VIDEO && page != null && !page.isClosed()) {
        recordedVideo = page.video();
      }
    }
    catch (Exception e) {
      log.error("Error capturing Playwright artifacts for {}", testKey, e);
    }
    finally {
      // Each close() in its own try/catch — if page.close() throws (rare but observed in
      // Playwright Java when the browser crashed mid-test), context.close() must still run or
      // the BrowserContext leaks for the rest of the suite.
      BasePage.clearCurrent();
      closeQuietly("page", () -> {
        if (page != null) {
          page.close();
          page = null;
        }
      });
      closeQuietly("context", () -> {
        if (context != null) {
          context.close();
          context = null;
        }
      });
    }
    if (RECORD_VIDEO && recordedVideo != null) {
      saveVideo(testKey, recordedVideo);
    }
    log.info("=== PLAYWRIGHT END {} | passed={}", displayName, failure == null);
  }

  /**
   * Run a close action and log+swallow any thrown exception. Used to keep the lifecycle rule's
   * {@code finally} block from short-circuiting if one resource's close throws — we always want
   * every resource closed even if a previous one failed.
   */
  private void closeQuietly(String what, Runnable action) {
    try {
      action.run();
    }
    catch (Exception e) {
      log.warn("Error closing Playwright {} (continuing): {}", what, e.getMessage());
    }
  }

  /**
   * Guards {@link #addShutdownHookOnce()} against redundant registration. The static initializer
   * runs once per classloader in normal Surefire/Failsafe usage, but custom classloaders or
   * test-runner restarts can re-execute it — registering multiple shutdown hooks would each
   * try to close the same Playwright instance.
   */
  private static final AtomicBoolean shutdownHookRegistered = new AtomicBoolean(false);

  static {
    addShutdownHookOnce();
  }

  private static void addShutdownHookOnce() {
    if (!shutdownHookRegistered.compareAndSet(false, true)) {
      return;
    }
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      log.info("Shutting down Playwright via JVM shutdown hook...");
      shutdownPlaywright();
    }, "playwright-shutdown"));
  }

  private static void initializePlaywright() {
    if (playwrightInitialized) {
      return;
    }
    synchronized (INIT_LOCK) {
      if (playwrightInitialized) {
        return;
      }
      log.info("Initializing Playwright (server at: {})...", baseUrlFromTest);
      playwright = Playwright.create();
      boolean manualPause =
          Boolean.parseBoolean(System.getProperty("playwright.manualPause", "false"));
      boolean headless = Boolean.parseBoolean(System.getProperty("playwright.headless", "true"));
      int slowMo = Integer.parseInt(System.getProperty("playwright.slowMo", "0"));
      if (manualPause && headless) {
        log.warn(
            "playwright.manualPause=true — Playwright Inspector does not appear in headless mode; forcing headless=false");
        headless = false;
      }
      log.info("Playwright Chromium launch: headless={}, slowMo={}, playwright.manualPause={}",
          headless, slowMo, manualPause);
      browser = playwright.chromium()
          .launch(new BrowserType.LaunchOptions()
              .setHeadless(headless)
              .setSlowMo(slowMo));
      playwrightInitialized = true;
      log.info("Playwright initialization complete");
    }
  }

  private static void shutdownPlaywright() {
    synchronized (INIT_LOCK) {
      if (!playwrightInitialized) {
        return;
      }
      try {
        if (browser != null) {
          browser.close();
          browser = null;
        }
        if (playwright != null) {
          playwright.close();
          playwright = null;
        }
        playwrightInitialized = false;
        log.info("Playwright shutdown complete");
      }
      catch (Exception e) {
        log.error("Error during Playwright shutdown", e);
      }
    }
  }

  /**
   * Hook for opt-in authentication reuse. When a subclass returns a non-null Playwright
   * {@code storageState} JSON string, {@link #setupPlaywrightTest} seeds the per-test
   * {@link BrowserContext} with it so the test starts already authenticated and can skip the
   * UI login. Returns {@code null} by default, which seeds nothing so the context starts
   * unauthenticated.
   *
   * @return a Playwright storageState JSON string to seed, or {@code null} to start clean
   */
  protected String reusableStorageState() {
    return null;
  }

  @BeforeEach
  public void setupPlaywrightTest(TestInfo testInfo) {
    String currentTest =
        testInfo.getTestMethod().map(Method::getName).orElse(testInfo.getDisplayName());
    testName = new TestName()
    {
      @Override
      public String getMethodName() {
        return currentTest;
      }
    };
    log.info("=== PLAYWRIGHT START {}.{} | server={}", getClass().getName(), currentTest, baseUrlFromTest);

    initializePlaywright();

    Browser.NewContextOptions contextOptions = new Browser.NewContextOptions()
        .setViewportSize(VIEWPORT_WIDTH, VIEWPORT_HEIGHT)
        .setBaseURL(baseUrlFromTest)
        .setTimezoneId("UTC");
    if (RECORD_VIDEO) {
      contextOptions.setRecordVideoDir(VIDEO_DIR)
          .setRecordVideoSize(VIEWPORT_WIDTH, VIEWPORT_HEIGHT);
    }
    // A subclass may seed the new context with a captured storageState (cookies) so it starts
    // authenticated; null seeds nothing (see reusableStorageState()).
    String seedStorageState = reusableStorageState();
    if (seedStorageState != null) {
      contextOptions.setStorageState(seedStorageState);
    }
    context = browser.newContext(contextOptions);

    if (TRACE_ALWAYS || TRACE_ON_FAILURE) {
      // All three flags must be true for the trace viewer to render a useful timeline:
      // screenshots → top filmstrip of JPEG frames per action
      // snapshots → DOM/CSS snapshots so the centre panel can replay the page at each
      // step (without this the viewer renders but the viewport is blank)
      // sources → captures the Java call-site stack for each action so clicking an
      // action jumps to the test code that triggered it
      // Java-binding defaults are false; this matches @playwright/test JS defaults. Trace
      // size roughly doubles, which is acceptable since we only keep traces on failure
      // (TRACE_ON_FAILURE) — the all-passing run discards them.
      context.tracing()
          .start(new Tracing.StartOptions()
              .setScreenshots(true)
              .setSnapshots(true)
              .setSources(true));
    }

    page = context.newPage();
    BasePage.setCurrent(page);
    registerPlaywrightLifecycleListeners();
  }

  /**
   * Hooks Playwright page events to (a) log the browser's navigation and console activity so the
   * test's journey is visible in the build console, and (b) capture browser-side warnings/errors
   * into {@link #browserConsoleWarningsAndErrors} and {@link #browserPageErrors} for the failure
   * diagnostics file.
   *
   * <p>
   * Listeners are detached automatically when {@link Page#close()} runs in the lifecycle rule's
   * {@code finally} block — no manual cleanup is needed.
   */
  private void registerPlaywrightLifecycleListeners() {
    browserConsoleWarningsAndErrors.clear();
    browserPageErrors.clear();
    // Top-frame navigations only — iframed widgets (Slack feedback, embedded help, etc.) generate
    // sub-frame navigations that are noise for the test-level journey.
    page.onFrameNavigated(frame -> {
      if (frame == page.mainFrame()) {
        log.info("[playwright][nav] {}", frame.url());
      }
    });
    page.onConsoleMessage(msg -> {
      String type = msg.type();
      if ("error".equals(type) || "warning".equals(type)) {
        // Record into the diagnostics list AND, if still under the per-test cap, log so the
        // event is visible in the build console even on passing builds. A misbehaving page that
        // emits many console events stops growing both the .diag.txt file and the build log at
        // the same threshold (MAX_BROWSER_DIAGNOSTIC_LINES).
        if (appendDiagnosticLine(browserConsoleWarningsAndErrors, "[" + type + "] " + msg.text())) {
          log.warn("[playwright][browser-{}] {}", type, msg.text());
        }
      }
    });
    page.onPageError(error -> {
      if (appendDiagnosticLine(browserPageErrors, error)) {
        log.error("[playwright][browser-pageError] {}", error);
      }
    });
  }

  /**
   * Records a single browser-side diagnostic line into {@code lines}, applying the per-test cap
   * and the per-line length cap. Returns {@code true} if the line was actually added, or
   * {@code false} if the cap was already reached. Callers use the return value to decide whether
   * to also emit the line to the build log — so a flood of console events from a misbehaving
   * page stops growing both the in-memory diagnostics list and the build log at the same point.
   */
  private static boolean appendDiagnosticLine(List<String> lines, String line) {
    synchronized (lines) {
      if (lines.size() >= MAX_BROWSER_DIAGNOSTIC_LINES) {
        return false;
      }
      String oneLine = line.replace('\n', ' ').replace('\r', ' ');
      if (oneLine.length() > 500) {
        oneLine = oneLine.substring(0, 500) + "...";
      }
      lines.add(oneLine);
      return true;
    }
  }

  /**
   * Logs why a test failed while the {@link Page} is still open. Invoked from the
   * {@link #playwrightLifecycle} rule before it closes the {@link BrowserContext}, so {@code page}
   * is still valid here even though all {@code @After} methods have already run.
   *
   * <p>
   * Writes a plain-text diagnostics file to {@code target/playwright-diagnostics/<class>.<method>.diag.txt},
   * archived as a Jenkins job artifact for triage.
   */
  private void logPlaywrightFailureDiagnostics(String fqMethod, Throwable failure) {
    log.error("Playwright test failed: {}", fqMethod, failure);

    StringBuilder summary = new StringBuilder(2048);
    summary.append(fqMethod).append('\n').append(failure).append('\n');

    if (page != null && !page.isClosed()) {
      try {
        String url = page.url();
        String title = page.title();
        log.error("Playwright page at failure: url={}, title={}", url, title);
        summary.append("url=").append(url).append('\n').append("title=").append(title).append('\n');

        try {
          Object snippetObj = page.evaluate("""
              () => {
                const b = document.body;
                if (!b || !b.innerText) return '';
                const t = b.innerText.trim();
                return t.length > 800 ? t.slice(0, 800) + "…" : t;
              }
              """);
          if (snippetObj != null) {
            String snippet = snippetObj.toString().trim();
            if (!snippet.isEmpty()) {
              log.error("Playwright page body text (truncated): {}", snippet);
              summary.append("bodySnippet=").append(snippet).append('\n');
            }
          }
        }
        catch (Exception snippetEx) {
          log.debug("Could not capture page body snippet for failure log: {}", snippetEx.getMessage());
        }
      }
      catch (Exception ex) {
        log.warn("Could not read Playwright page URL/title for failure log: {}", ex.toString());
      }
    }
    else {
      log.warn("Playwright page was null or closed; skipping page state for failed test {}", fqMethod);
    }

    if (!browserConsoleWarningsAndErrors.isEmpty()) {
      log.error("Browser console (warning/error) during test:{}{}", System.lineSeparator(),
          String.join(System.lineSeparator(), browserConsoleWarningsAndErrors));
      summary.append("console:\n")
          .append(String.join("\n", browserConsoleWarningsAndErrors))
          .append('\n');
    }
    if (!browserPageErrors.isEmpty()) {
      log.error("Uncaught page JS errors during test:{}{}", System.lineSeparator(),
          String.join(System.lineSeparator(), browserPageErrors));
      summary.append("pageErrors:\n").append(String.join("\n", browserPageErrors)).append('\n');
    }

    try {
      Files.createDirectories(DIAGNOSTICS_DIR);
      Path diagFile = DIAGNOSTICS_DIR.resolve(safeFileName(fqMethod) + ".diag.txt");
      Files.writeString(diagFile, summary.toString(), StandardCharsets.UTF_8);
    }
    catch (Exception e) {
      log.debug("Could not write failure diagnostics file: {}", e.getMessage());
    }
  }

  /**
   * Persists the in-progress Playwright video to a stable on-disk path. The Playwright HTML
   * report's per-test card auto-discovers and embeds this file by name.
   *
   * <p>
   * Calls {@link Video#saveAs(Path)} which blocks until the recording is fully flushed (avoids
   * racing the muxer).
   */
  private void saveVideo(String testKey, Video video) {
    try {
      Path target = VIDEO_DIR.resolve(testKey + ".webm");
      Files.createDirectories(target.getParent());
      if (Files.exists(target)) {
        Files.delete(target);
      }
      video.saveAs(target);
      long size = Files.size(target);
      if (size <= 0) {
        log.warn("Playwright video for {} is empty (0 bytes)", testKey);
        return;
      }
      log.info("Saved Playwright video for {} ({} bytes) to {}", testKey, size, target.toAbsolutePath());
    }
    catch (Exception e) {
      log.error("Error saving Playwright video for {}", testKey, e);
    }
  }

  private void discardTrace() {
    try {
      if (context != null) {
        context.tracing().stop();
      }
    }
    catch (Exception e) {
      log.debug("Discarding trace on passing test (non-fatal): {}", e.getMessage());
    }
  }

  /**
   * Capture a full-page screenshot to {@code target/playwright-screenshots/<testKey>.png}.
   */
  private void captureScreenshot(String testKey) {
    try {
      if (page != null && !page.isClosed()) {
        Path target = SCREENSHOT_DIR.resolve(testKey + ".png");
        Files.createDirectories(target.getParent());
        page.screenshot(new Page.ScreenshotOptions().setFullPage(true).setPath(target));
        log.info("Saved Playwright screenshot for {} to {}", testKey, target.toAbsolutePath());
      }
    }
    catch (Exception e) {
      log.error("Error capturing screenshot for {}", testKey, e);
    }
  }

  /**
   * Persists the in-progress Playwright trace zip to {@code target/playwright-traces/<testKey>.zip}.
   * Open locally with:
   * {@code mvn exec:java -Dexec.classpathScope=test -Dexec.mainClass=com.microsoft.playwright.CLI
   * -Dexec.args="show-trace target/playwright-traces/<testKey>.zip"}
   */
  private void saveTrace(String testKey) {
    try {
      if (context != null) {
        Path tracePath = TRACE_DIR.resolve(testKey + ".zip");
        Files.createDirectories(tracePath.getParent());
        context.tracing().stop(new Tracing.StopOptions().setPath(tracePath));
        log.info("Saved Playwright trace for {} to {}", testKey, tracePath.toAbsolutePath());
      }
    }
    catch (Exception e) {
      log.error("Error saving trace for {}", testKey, e);
    }
  }

  /**
   * When {@code -Dplaywright.manualPause=true}, opens the Playwright Inspector and blocks until you
   * resume. Lets you drive the same browser tab manually after automated steps (e.g. login).
   * <p>
   * Typical local run (Inspector is a separate window from Chromium; use Cmd+Tab / Dock on macOS):
   * {@code PWDEBUG=1 mvn verify -Dit.test=OrganizationPlaywrightTest -Dplaywright.manualPause=true}
   * ({@code -Dplaywright.headless=false} optional — headed mode is forced when manual pause is on.)
   */
  protected void playwrightManualPauseIfEnabled() {
    if (!Boolean.parseBoolean(System.getProperty("playwright.manualPause", "false"))) {
      return;
    }
    log.warn(
        "playwright.manualPause=true — pausing; open the Playwright Inspector window (not the IQ tab) and click Resume when finished.");
    page.pause();
  }

  /**
   * Navigate to a path relative to the server base URL.
   */
  protected void playwrightNavigateTo(String path) {
    String fullUrl = path.startsWith("/")
        ? baseUrlFromTest + path.substring(1)
        : baseUrlFromTest + path;
    log.debug("playwrightNavigateTo: path='{}', fullUrl='{}'", path, fullUrl);
    page.navigate(fullUrl);
    page.waitForLoadState();
  }

  /**
   * Clears browser cookies and (when possible) storage; equivalent to Selenide hardreset().
   * <p>
   * Safe to call before any navigation: when the page is still on {@code about:blank} (or another
   * opaque/sandboxed origin) {@code window.sessionStorage}/{@code localStorage} access throws
   * {@code SecurityError}; in that case there's nothing to clear so we skip silently.
   */
  protected void playwrightHardreset() {
    context.clearCookies();
    String url = page != null ? page.url() : null;
    if (url == null || url.startsWith("about:") || url.startsWith("chrome:") || url.startsWith("data:")) {
      return;
    }
    try {
      page.evaluate("window.sessionStorage.clear()");
      page.evaluate("window.localStorage.clear()");
    }
    catch (PlaywrightException e) {
      log.debug("playwrightHardreset: storage not accessible at url='{}': {}", url, e.getMessage());
    }
  }

  /**
   * Clears cookies/storage and navigates to {@code about:blank} to ensure the page is in a clean
   * state before a test that does not want any residual navigation history.
   */
  protected void playwrightHardresetToBlank() {
    playwrightHardreset();
    try {
      page.navigate("about:blank");
    }
    catch (PlaywrightException e) {
      // SPA may fire a redirect immediately after storage clear; wait for it to settle then retry
      page.waitForLoadState();
      page.navigate("about:blank");
    }
    page.waitForLoadState();
  }

  /**
   * Navigates to {@code path} and retries until the URL contains {@code urlFragment}, up to
   * {@link PlaywrightTiming#URL_SUBSTRING_TIMEOUT_MS}.
   * <p>
   * Useful when the target page redirects or performs an async route change before settling.
   * <p>
   * <strong>Note:</strong> designed for direct/single-hop SPA hash routes. Each retry
   * re-navigates to {@code path}, which resets any in-progress redirect chain. For pages with
   * multi-hop redirects (post-action navigation, auth guards that temporarily land elsewhere),
   * prefer {@code playwrightRefreshOrOpen} followed by an explicit {@code page.waitForURL(...)}.
   */
  protected void navigateAndWaitForUrl(String path, String urlFragment) {
    PlaywrightWaitUtils.waitForCondition(
        () -> {
          if (!page.url().contains(urlFragment)) {
            playwrightNavigateTo(path);
          }
          return page.url().contains(urlFragment);
        },
        PlaywrightTiming.URL_SUBSTRING_TIMEOUT_MS,
        1_000L,
        "URL did not contain '" + urlFragment + "' within " + PlaywrightTiming.URL_SUBSTRING_TIMEOUT_MS + "ms");
  }

  /**
   * Refresh the current page.
   */
  protected void playwrightRefresh() {
    page.reload();
    page.waitForLoadState();
  }

  /**
   * Navigate to a URL if not already on it, or refresh if already there.
   * Equivalent to Selenide refreshOrOpen().
   */
  protected void playwrightRefreshOrOpen(String path) {
    String fullUrl = path.startsWith("http")
        ? path
        : path.startsWith("/")
            ? baseUrlFromTest + path.substring(1)
            : baseUrlFromTest + path;
    String currentUrl = page.url();
    String normalizedCurrentUrl = normalizeUrlForComparison(currentUrl);
    String normalizedTargetUrl = normalizeUrlForComparison(fullUrl);
    log.debug("playwrightRefreshOrOpen: path='{}', fullUrl='{}', currentUrl='{}'", path, fullUrl, currentUrl);
    if (normalizedCurrentUrl != null && normalizedCurrentUrl.equals(normalizedTargetUrl)) {
      page.reload();
    }
    else {
      page.navigate(fullUrl);
    }
    page.waitForLoadState();
    log.debug("playwrightRefreshOrOpen: after navigation, url='{}'", page.url());
  }

  private static String normalizeUrlForComparison(String url) {
    if (url == null) {
      return null;
    }
    String normalized = url;
    while (normalized.length() > 1 && normalized.endsWith("/")) {
      normalized = normalized.substring(0, normalized.length() - 1);
    }
    return normalized;
  }

  /**
   * Navigate within the SPA by changing only the hash fragment of the current URL.
   * Unlike {@link #playwrightRefreshOrOpen}, this keeps the same document and lets the
   * SPA router react to the {@code hashchange} event — useful for deep-link tests.
   *
   * @param hashFragment the hash portion of the target URL, including the leading {@code #}
   *          (e.g. {@code "#/advancedSearch?search=log4j"})
   */
  protected void playwrightSpaNavigateToHashFragment(String hashFragment) {
    String currentUrl = page.url();
    String baseUrl = currentUrl.contains("#")
        ? currentUrl.substring(0, currentUrl.indexOf('#'))
        : currentUrl;
    page.navigate(baseUrl + hashFragment);
    page.waitForLoadState();
  }

  /**
   * Navigate/refresh to a path and wait for the provided locator to become visible.
   */
  protected void playwrightOpenAndWaitForVisible(String path, Locator readyLocator) {
    playwrightRefreshOrOpen(path);
    PlaywrightWaitUtils.waitForVisible(readyLocator, PlaywrightTiming.ELEMENT_TIMEOUT_MS,
        PlaywrightTiming.POLL_INTERVAL_MS);
  }

  /**
   * Wait until the page URL contains a substring.
   *
   * <p>
   * Uses a predicate rather than a glob — hash routes ({@code #/dashboard/...}) are not matched
   * reliably by {@code **fragment**} patterns in Playwright.
   */
  protected void playwrightWaitUntilUrlContains(String urlFragment) {
    PlaywrightWaitUtils.waitForUrl(page, urlFragment);
  }

  /**
   * Wait for the NX submit mask to appear and then dismiss — the "save in progress / save done" UI
   * pattern that wraps every async form submission in the IQ Server frontend.
   *
   * <p>
   * Two phases:
   * <ol>
   * <li>Wait up to 2s for {@code .nx-submit-mask} to become {@code VISIBLE} — confirms the click
   * triggered an async submit (and we caught it before it dismissed).</li>
   * <li>Wait up to 10s for {@code .nx-submit-mask} to become {@code HIDDEN} — confirms the
   * submission completed (success or rejection).</li>
   * </ol>
   *
   * <p>
   * Both waits are inside a single try/catch because a very fast backend response can flicker
   * the mask faster than Playwright can observe it; in that case there's nothing to wait for and
   * we proceed. Replaces Selenide's {@code NxSubmitMask.seeAndWaitForDismissal()}.
   */
  protected void waitForSubmitMask() {
    Locator submitMask = page.locator(".nx-submit-mask").first();
    try {
      submitMask.waitFor(new Locator.WaitForOptions()
          .setState(WaitForSelectorState.VISIBLE)
          .setTimeout(PlaywrightTiming.SHORT_UI_CUE_MS));
      submitMask.waitFor(new Locator.WaitForOptions()
          .setState(WaitForSelectorState.HIDDEN)
          .setTimeout(PlaywrightTiming.ELEMENT_TIMEOUT_MS));
    }
    catch (TimeoutError e) {
      // The mask flickered faster than Playwright could observe — only TimeoutError
      // is caught; any other exception (browser crash, etc.) propagates normally.
      log.debug("Submit mask not detected in waitForSubmitMask (fast submission or transient DOM): {}",
          e.getMessage());
    }
  }

  /**
   * Waits for {@code .nx-submit-mask--success} to appear and then disappear, confirming the
   * submission completed with a success state.
   *
   * <p>
   * Two phases mirror {@link #waitForSubmitMask()}:
   * <ol>
   * <li>Wait up to {@code SHORT_UI_CUE_MS} for the success class to become visible — confirms the
   * save succeeded (not just that the mask appeared).</li>
   * <li>Wait up to {@code ELEMENT_TIMEOUT_MS} for it to become hidden — confirms the success
   * banner dismissed.</li>
   * </ol>
   *
   * <p>
   * Both waits are inside a single try/catch to handle fast backend responses where the success
   * state flickers before Playwright can observe it. Callers should still assert downstream
   * persisted state (e.g. reload the page and verify the saved value) to confirm the save round-
   * tripped to the server.
   */
  protected void waitForSubmitMaskSuccess() {
    Locator successMask = page.locator(".nx-submit-mask--success").first();
    try {
      successMask.waitFor(new Locator.WaitForOptions()
          .setState(WaitForSelectorState.VISIBLE)
          .setTimeout(PlaywrightTiming.SHORT_UI_CUE_MS));
      successMask.waitFor(new Locator.WaitForOptions()
          .setState(WaitForSelectorState.HIDDEN)
          .setTimeout(PlaywrightTiming.ELEMENT_TIMEOUT_MS));
    }
    catch (TimeoutError e) {
      log.debug("Submit mask success not detected in waitForSubmitMaskSuccess (fast submission or transient DOM): {}",
          e.getMessage());
    }
  }

  /**
   * Normalises a fully-qualified test name to a filesystem-safe slug by collapsing any character
   * outside {@code [A-Za-z0-9._-]} to {@code _}. Prevents two test classes with the same method
   * name from clobbering each other's screenshot/trace/video artifacts.
   */
  private static String safeFileName(String name) {
    return name.replaceAll("[^A-Za-z0-9._-]", "_");
  }

  /**
   * Read a classpath resource as a UTF-8 string. Shared helper used by tests that load XML/JSON
   * fixtures (e.g. SAML IdP metadata, HDS stub bodies). No callers in this; consumed by tests
   * added in subsequent parts of the regression suite split.
   */
  protected static String readClasspathUtf8(Class<?> contextClass, String absoluteResourcePath) {
    InputStream in = contextClass.getResourceAsStream(absoluteResourcePath);
    if (in == null) {
      throw new IllegalArgumentException(
          "Missing classpath resource: " + absoluteResourcePath
              + " (relative to " + contextClass.getName() + ")");
    }
    try (in) {
      return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }
    catch (IOException e) {
      throw new UncheckedIOException("Failed to read " + absoluteResourcePath, e);
    }
  }
}
