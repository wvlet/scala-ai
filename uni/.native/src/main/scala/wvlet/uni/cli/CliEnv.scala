/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package wvlet.uni.cli

/**
  * Scala Native-specific CLI environment detection.
  */
object CliEnv extends CliEnvBase:
  private def env(name: String): Option[String] = Option(System.getenv(name)).filter(_.nonEmpty)

  override def colorLevel: ColorLevel =
    // Check NO_COLOR
    if env("NO_COLOR").isDefined then
      return ColorLevel.None

    // Check FORCE_COLOR
    env("FORCE_COLOR") match
      case Some("0") =>
        return ColorLevel.None
      case Some("1") =>
        return ColorLevel.Basic
      case Some("2") =>
        return ColorLevel.Ansi256
      case Some("3") =>
        return ColorLevel.TrueColor
      case Some(_) =>
        return ColorLevel.Basic
      case None => // continue

    // Check COLORTERM
    env("COLORTERM") match
      case Some("truecolor") | Some("24bit") =>
        return ColorLevel.TrueColor
      case _ => // continue

    // Check TERM
    val term = env("TERM").getOrElse("")
    if term == "dumb" then
      ColorLevel.None
    else if term.endsWith("-256color") || term.endsWith("256color") then
      ColorLevel.Ansi256
    else if term.contains("truecolor") || term.contains("24bit") then
      ColorLevel.TrueColor
    else if term.nonEmpty then
      ColorLevel.Basic
    else
      ColorLevel.None

  end colorLevel

  override def isInteractive: Boolean =
    // For Scala Native, check if we have a terminal environment
    env("TERM").isDefined

  override def isWindows: Boolean =
    // Check common Windows environment variables
    env("OS").exists(_.toLowerCase.contains("windows")) || env("WINDIR").isDefined

  override def terminalWidth: Int = env("COLUMNS").flatMap(_.toIntOption).getOrElse(80)

  override def terminalHeight: Int = env("LINES").flatMap(_.toIntOption).getOrElse(24)

end CliEnv
