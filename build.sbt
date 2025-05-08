Global / onChangedBuildSource := ReloadOnSourceChanges

val SCALA_3                 = "3.7.0"
val AIRFRAME_VERSION        = "2025.1.10"
val AWS_SDK_VERSION         = "2.31.38"
val JS_JAVA_LOGGING_VERSION = "1.0.0"

// Common build settings
val buildSettings = Seq[Setting[?]](
  organization             := "org.wvlet",
  description              := "Scala 3 library for building AI (LLM) applications",
  scalaVersion             := SCALA_3,
  crossScalaVersions       := List(SCALA_3),
  crossPaths               := true,
  publishMavenStyle        := true,
  Test / parallelExecution := false,
  // Use AirSpec for testing
  libraryDependencies ++= Seq("org.wvlet.airframe" %%% "airspec" % AIRFRAME_VERSION % Test),
  testFrameworks += new TestFramework("wvlet.airspec.Framework")
)

val jsBuildSettings = Seq[Setting[?]](
  libraryDependencies ++=
    Seq(
      // For using java.util.UUID.randomUUID() in Scala.js
      ("org.scala-js" %%% "scalajs-java-securerandom" % "1.0.0" % Test).cross(
        CrossVersion.for3Use2_13
      )
    )
)

val nativeBuildSettings = Seq[Setting[?]](
  // Scala Native specific settings
)

// Root project aggregating others
lazy val root = project
  .in(file("."))
  .settings(buildSettings, name := "ai", publish / skip := true)
  .aggregate(core.jvm, core.js, core.native, agent, bedrock)

lazy val coreMacros = crossProject(JVMPlatform, JSPlatform, NativePlatform)
  .crossType(CrossType.Pure)
  .in(file("ai-core-macros"))
  .settings(
    buildSettings,
    name        := "ai-core-base",
    description := "Scal core macros project for wvlet.ai"
  )

// core library for Scala JVM, Scala.js and Scala Native
lazy val core = crossProject(JVMPlatform, JSPlatform, NativePlatform)
  .crossType(CrossType.Pure)
  .in(file("ai-core"))
  .settings(buildSettings, name := "ai-core", description := "Scala core library for AI")
  .jvmSettings(
    libraryDependencies ++=
      Seq(
        // For automatic log-rotation
        "ch.qos.logback" % "logback-core" % "1.5.8"
      )
  )
  .jsSettings(
    jsBuildSettings,
    libraryDependencies ++=
      Seq(
        ("org.scala-js" %%% "scalajs-java-logging" % JS_JAVA_LOGGING_VERSION).cross(
          CrossVersion.for3Use2_13
        )
      )
  )
  .nativeSettings(nativeBuildSettings)
  .dependsOn(coreMacros)

lazy val agent = project
  .in(file("ai-agent"))
  .settings(
    buildSettings,
    name        := "ai-agent",
    description := "Core interface for AI (LLM) applications",
    libraryDependencies ++=
      Seq(
        "org.wvlet.airframe" %% "airframe"       % AIRFRAME_VERSION,
        "org.wvlet.airframe" %% "airframe-codec" % AIRFRAME_VERSION
      )
  )

lazy val bedrock = project
  .in(file("ai-agent-bedrock"))
  .settings(
    buildSettings,
    name        := "ai-bedrock",
    description := "AWS Bedrock integration",
    libraryDependencies ++=
      Seq(
        "software.amazon.awssdk" % "bedrockruntime" % AWS_SDK_VERSION,
        // Redirect slf4j to airframe-log
        "org.slf4j" % "slf4j-jdk14" % "2.0.17",
        // Add langchain4j as a reference implementation
        "dev.langchain4j" % "langchain4j"         % "1.0.0-rc1"   % Test,
        "dev.langchain4j" % "langchain4j-bedrock" % "1.0.0-beta4" % Test
      )
  )
  .dependsOn(agent)
