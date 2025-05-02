Global / onChangedBuildSource := ReloadOnSourceChanges

val SCALA_3_VERSION  = "3.3.5"
val AIRFRAME_VERSION = "2025.1.10"
val AWS_SDK_VERSION  = "2.31.34"

// Common build settings
val buildSettings = Seq[Setting[?]](
  organization       := "org.wvlet",
  description        := "Scala 3 library for building AI (LLM) applications",
  scalaVersion       := SCALA_3_VERSION,
  crossPaths         := true,
  publishMavenStyle  := true,
  Test / logBuffered := false,
  // Use AirSpec for testing
  libraryDependencies ++= Seq("org.wvlet.airframe" %%% "airspec" % AIRFRAME_VERSION % Test),
  testFrameworks += new TestFramework("wvlet.airspec.Framework")
)

// Root project aggregating others
lazy val root = project
  .in(file("."))
  .settings(buildSettings, name := "ai", publish / skip := true)
  .aggregate(core, bedrock)

lazy val core = project
  .in(file("ai-core"))
  .settings(
    buildSettings,
    name        := "ai-core",
    description := "Core interface for AI (LLM) applications"
  )

lazy val bedrock = project
  .in(file("ai-bedrock"))
  .settings(
    buildSettings,
    name        := "ai-bedrock",
    description := "AWS Bedrock integration",
    libraryDependencies ++=
      Seq(
        "software.amazon.awssdk" % "bedrockruntime" % AWS_SDK_VERSION,
        // Add langchain4j as a reference implementation
        "dev.langchain4j" % "langchain4j"         % "1.0.0-rc1"   % Test,
        "dev.langchain4j" % "langchain4j-bedrock" % "1.0.0-beta4" % Test
      )
  )
  .dependsOn(core)
