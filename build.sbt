Global / onChangedBuildSource := ReloadOnSourceChanges

val SCALA_3_VERSION  = "3.7.0"
val AIRFRAME_VERSION = "2025.1.10"
val AWS_SDK_VERSION  = "2.31.38"

// Common build settings
val buildSettings = Seq[Setting[?]](
  organization             := "org.wvlet",
  description              := "Scala 3 library for building AI (LLM) applications",
  scalaVersion             := SCALA_3_VERSION,
  crossPaths               := true,
  publishMavenStyle        := true,
  Test / parallelExecution := false,
  // Use AirSpec for testing
  libraryDependencies ++= Seq("org.wvlet.airframe" %%% "airspec" % AIRFRAME_VERSION % Test),
  testFrameworks += new TestFramework("wvlet.airspec.Framework")
)

// Root project aggregating others
lazy val root = project
  .in(file("."))
  .settings(buildSettings, name := "ai", publish / skip := true)
  .aggregate(agent, bedrock)

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
