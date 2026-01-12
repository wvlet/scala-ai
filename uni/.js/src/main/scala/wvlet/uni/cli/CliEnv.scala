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

import scala.scalajs.js
import scala.scalajs.js.Dynamic.global as g

/**
  * Scala.js-specific CLI environment detection.
  */
object CliEnv extends CliEnvBase:
  private def isNode: Boolean =
    js.typeOf(g.process) != "undefined" && js.typeOf(g.process.versions) != "undefined" &&
      js.typeOf(g.process.versions.node) != "undefined"

  private def isBrowser: Boolean =
    js.typeOf(g.window) != "undefined" && js.typeOf(g.document) != "undefined"

  private def env(name: String): Option[String] =
    if isNode then
      val value = g.process.env.selectDynamic(name)
      if js.isUndefined(value) || value == null then
        None
      else
        val str = value.toString
        if str.isEmpty then
          None
        else
          Some(str)
    else
      None

  override def colorLevel: ColorLevel =
    if isBrowser then
      // Browsers use CSS styling, not ANSI codes
      ColorLevel.None
    else if isNode then
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
      else if term.endsWith("-256color") then
        ColorLevel.Ansi256
      else if term.nonEmpty then
        ColorLevel.Basic
      else
        ColorLevel.None
    else
      ColorLevel.None

  override def isInteractive: Boolean =
    if isNode then
      val stdout = g.process.stdout
      !js.isUndefined(stdout) && !js.isUndefined(stdout.isTTY) && stdout.isTTY.asInstanceOf[Boolean]
    else
      false

  override def isWindows: Boolean =
    if isNode then
      val platform = g.process.platform.toString
      platform == "win32"
    else
      false

  override def terminalWidth: Int =
    if isNode then
      val stdout = g.process.stdout
      if !js.isUndefined(stdout) && !js.isUndefined(stdout.columns) then
        stdout.columns.asInstanceOf[Int]
      else
        80
    else
      80

  override def terminalHeight: Int =
    if isNode then
      val stdout = g.process.stdout
      if !js.isUndefined(stdout) && !js.isUndefined(stdout.rows) then
        stdout.rows.asInstanceOf[Int]
      else
        24
    else
      24

end CliEnv
