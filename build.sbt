import sbtide.Keys.ideSkipProject

Global / onChangedBuildSource := ReloadOnSourceChanges

val SCALA_3                 = "3.7.2"
val AIRFRAME_VERSION        = "2025.1.14"
val AWS_SDK_VERSION         = "2.32.13"
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
  libraryDependencies ++=
    Seq(
      "org.wvlet.airframe" %%% "airspec" % AIRFRAME_VERSION % Test,
      // For PreDestroy, PostConstruct annotations
      "javax.annotation" % "javax.annotation-api" % "1.3.2" % Test
    ),
  testFrameworks += new TestFramework("wvlet.airspec.Framework")
)

val jsBuildSettings = Seq[Setting[?]](
  libraryDependencies ++=
    Seq(
      // For using java.util.UUID.randomUUID() in Scala.js
      ("org.scala-js" %%% "scalajs-java-securerandom" % "1.0.0" % Test).cross(
        CrossVersion.for3Use2_13
      ),
      // For using java.time.Instant in Scala.js
      ("org.scala-js" %%% "scalajs-java-time" % "1.0.0").cross(CrossVersion.for3Use2_13),
      // For scheduling with timer
      "org.scala-js" %%% "scala-js-macrotask-executor" % "1.1.1"
    )
)

val nativeBuildSettings = Seq[Setting[?]](
  // Scala Native specific settings
  libraryDependencies ++=
    Seq(
      // For using java.time libraries
      "org.ekrich" %%% "sjavatime" % "1.4.0"
    )
)

val noPublish = Seq(
  publishArtifact := false,
  publish         := {},
  publishLocal    := {},
  publish / skip  := true,
  // This must be Nil to use crossScalaVersions of individual modules in `+ projectJVM/xxxx` tasks
  crossScalaVersions := Nil,
  // Explicitly skip the doc task because protobuf related Java files causes no type found error
  Compile / doc / sources                := Seq.empty,
  Compile / packageDoc / publishArtifact := false,
  // Do not check binary compatibility for unpublished projects
  // mimaPreviousArtifacts := Set.empty
  // Skip importing aggregated projects in IntelliJ IDEA
  ideSkipProject := true
)

// Remove warning as ideSkipProject is used only for IntelliJ IDEA
Global / excludeLintKeys ++= Set(ideSkipProject)

// Root project aggregating others
lazy val root = project
  .in(file("."))
  .settings(buildSettings, name := "ai", publish / skip := true)
  .aggregate((jvmProjects ++ jsProjects ++ nativeProjects): _*)

lazy val jvmProjects: Seq[ProjectReference]    = Seq(core.jvm, agent, bedrock)
lazy val jsProjects: Seq[ProjectReference]     = Seq(core.js)
lazy val nativeProjects: Seq[ProjectReference] = Seq(core.native)

lazy val projectJVM = project
  .settings(noPublish)
  .settings(
    // Use a stable coverage directory name without containing scala version
    // coverageDataDir := target.value
  )
  .aggregate(jvmProjects: _*)

lazy val projectJS = project.settings(noPublish).aggregate(jsProjects: _*)

lazy val projectNative = project.settings(noPublish).aggregate(nativeProjects: _*)

// core library for Scala JVM, Scala.js and Scala Native
lazy val core = crossProject(JVMPlatform, JSPlatform, NativePlatform)
  .crossType(CrossType.Pure)
  .in(file("ai-core"))
  .settings(buildSettings, name := "ai-core", description := "Scala core library for AI")
  .jvmSettings(
    libraryDependencies ++=
      Seq(
        // For automatic log-rotation
        "ch.qos.logback" % "logback-core" % "1.5.18"
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
        "dev.langchain4j" % "langchain4j"         % "1.2.0" % Test,
        "dev.langchain4j" % "langchain4j-bedrock" % "1.2.0" % Test
      )
  )
  .dependsOn(agent)

lazy val integrationTest = project
  .in(file("ai-integration-test"))
  .settings(
    buildSettings,
    noPublish,
    name           := "ai-integration-test",
    description    := "Integration test for AI (LLM) applications",
    ideSkipProject := false
  )
  .dependsOn(bedrock)
