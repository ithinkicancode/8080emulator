
ThisBuild / scalaVersion := Versions.Scala

val baseNamespace = "dev.iticc.emulator8080.opcode.pop"

lazy val root = project.in(file(".")).aggregate(core)

lazy val core = {
  project
    .in(file("core"))
    .enablePlugins(
      NativeImagePlugin
    )
    .settings(
      name := "8080emu",
      version := "0.0.1",
      Compile / mainClass := Option(s"$baseNamespace.Main"),
      nativeImageVersion := "20.2.0",
      nativeImageJvm := "graalvm",
      nativeImageOptions ++= List(
        "--verbose",
        "--no-fallback",
        "--no-server",
        "--allow-incomplete-classpath",
        "--no-fallback",
        "--report-unsupported-elements-at-runtime",
        "--install-exit-handlers",
        "-H:+ReportExceptionStackTraces",
        "-H:+RemoveSaturatedTypeFlows",
        "-H:+TraceClassInitialization",
        "-H:IncludeResources='.*'",
        "--initialize-at-run-time=" + List(
          "io.netty.channel.epoll.Epoll",
          "io.netty.channel.epoll.Native",
          "io.netty.channel.epoll.EpollEventLoop",
          "io.netty.channel.epoll.EpollEventArray",
          "io.netty.channel.DefaultFileRegion",
          "io.netty.channel.kqueue.KQueueEventArray",
          "io.netty.channel.kqueue.KQueueEventLoop",
          "io.netty.channel.kqueue.Native",
          "io.netty.channel.unix.Errors",
          "io.netty.channel.unix.IovArray",
          "io.netty.channel.unix.Limits",
          "io.netty.util.internal.logging.Log4JLogger",
          "io.netty.util.AbstractReferenceCounted",
          "io.netty.channel.kqueue.KQueue",
          "org.slf4j.LoggerFactory",
        ).mkString(",")
      ),
    )
    .settings(
      scalacOptions ++= compilerOptions,
      libraryDependencies ++= Seq(
        "dev.zio" %% "zio" % Versions.Zio,
        "dev.zio" %% "zio-json" % Versions.ZioJson,
        "io.d11" %% "zhttp" % Versions.ZioHttp,
        "dev.zio" %% "zio-optics" % Versions.ZioOptics,
      )
    )
}

lazy val compilerOptions = Seq(
  "-target:jvm-1.8",
  "-Xsource:3",
  "-encoding", "utf-8",                // Specify character encoding used by source files.
  "-explaintypes",                     // Explain type errors in more detail.
  "-deprecation",                      // Emit warning and location for usages of deprecated APIs.
  "-feature",                          // Emit warning and location for usages of features that should be imported explicitly.
  "-language:existentials",            // Existential types (besides wildcard types) can be written and inferred
  "-Ymacro-annotations",
  "-language:higherKinds",             // Allow higher-kinded types
  "-unchecked",                        // Enable additional warnings where generated code depends on assumptions.
  "-Xcheckinit",                       // Wrap field accessors to throw an exception on uninitialized access.
  "-Xlint:deprecation",                // Enable linted deprecations.
  "-Wdead-code",                       // Warn when dead code is identified.
  "-Wunused",
)
