import sbtide.Keys.ideSkipProject

Global / onChangedBuildSource := ReloadOnSourceChanges

val SCALA_3                 = "3.7.3"
val AIRFRAME_VERSION        = "2025.1.26"
val AWS_SDK_VERSION         = "2.41.5"
val JS_JAVA_LOGGING_VERSION = "1.0.0"
val JUNIT_PLATFORM_VERSION  = "1.11.4"
val SBT_TEST_INTERFACE_VERSION = "1.0"

// Common build settings
val buildSettings = Seq[Setting[?]](
  organization             := "org.wvlet",
  description              := "Scala 3 unified utility library",
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
      "org.ekrich" %%% "sjavatime" % "1.5.0"
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
  .settings(buildSettings, name := "uni", publish / skip := true)
  .aggregate((jvmProjects ++ jsProjects ++ nativeProjects): _*)

lazy val jvmProjects: Seq[ProjectReference]    = Seq(core.jvm, agent, bedrock, unitest.jvm)
lazy val jsProjects: Seq[ProjectReference]     = Seq(core.js, unitest.js)
lazy val nativeProjects: Seq[ProjectReference] = Seq(core.native, unitest.native)

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
  .in(file("uni"))
  .settings(buildSettings, name := "uni", description := "Scala unified core library")
  .jvmSettings(
    libraryDependencies ++=
      Seq(
        // For automatic log-rotation
        "ch.qos.logback" % "logback-core" % "1.5.24"
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

// UniTest - Lightweight testing framework with AirSpec syntax
lazy val unitest = crossProject(JVMPlatform, JSPlatform, NativePlatform)
  .crossType(CrossType.Pure)
  .in(file("uni-test"))
  .settings(
    buildSettings,
    name        := "uni-test",
    description := "Lightweight testing framework with AirSpec syntax",
    // Don't use AirSpec for testing uni-test itself (avoid circular dependency)
    libraryDependencies --= Seq(
      "org.wvlet.airframe" %%% "airspec" % AIRFRAME_VERSION % Test
    ),
    testFrameworks := Seq(new TestFramework("wvlet.uni.test.spi.UniTestFramework")),
    libraryDependencies ++=
      Seq(
        "org.scala-sbt" % "test-interface" % SBT_TEST_INTERFACE_VERSION
      )
  )
  .jvmSettings(
    libraryDependencies ++=
      Seq(
        "org.junit.platform" % "junit-platform-engine"   % JUNIT_PLATFORM_VERSION % Optional,
        "org.junit.platform" % "junit-platform-launcher" % JUNIT_PLATFORM_VERSION % Optional
      )
  )
  .jsSettings(jsBuildSettings)
  .nativeSettings(nativeBuildSettings)
  .dependsOn(core)

lazy val agent = project
  .in(file("uni-agent"))
  .settings(
    buildSettings,
    name        := "uni-agent",
    description := "Core interface for agent applications",
    libraryDependencies ++=
      Seq(
        "org.wvlet.airframe" %% "airframe"       % AIRFRAME_VERSION,
        "org.wvlet.airframe" %% "airframe-codec" % AIRFRAME_VERSION
      )
  )
  .dependsOn(core.jvm)

lazy val bedrock = project
  .in(file("uni-agent-bedrock"))
  .settings(
    buildSettings,
    name        := "uni-bedrock",
    description := "AWS Bedrock integration",
    libraryDependencies ++=
      Seq(
        "software.amazon.awssdk" % "bedrockruntime" % AWS_SDK_VERSION,
        // Redirect slf4j to airframe-log
        "org.slf4j" % "slf4j-jdk14" % "2.0.17",
        // Add langchain4j as a reference implementation
        "dev.langchain4j" % "langchain4j"         % "1.10.0" % Test,
        "dev.langchain4j" % "langchain4j-bedrock" % "1.10.0" % Test
      )
  )
  .dependsOn(agent)

lazy val integrationTest = project
  .in(file("uni-integration-test"))
  .settings(
    buildSettings,
    noPublish,
    name           := "uni-integration-test",
    description    := "Integration test for agent applications",
    ideSkipProject := false
  )
  .dependsOn(bedrock)
