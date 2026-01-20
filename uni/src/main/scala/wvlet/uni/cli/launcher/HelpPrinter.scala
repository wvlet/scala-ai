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
package wvlet.uni.cli.launcher

import wvlet.uni.cli.{Tint, CliEnv}

/**
  * Renders help messages for CLI commands
  */
trait HelpPrinter:
  def render(
      commandName: String,
      description: String,
      usage: Option[String],
      options: Seq[CLOption],
      arguments: Seq[CLArgument],
      subCommands: Seq[(String, String)]
  ): String

object HelpPrinter:
  val default: HelpPrinter = DefaultHelpPrinter()

/**
  * Default help printer with ANSI color support
  */
class DefaultHelpPrinter extends HelpPrinter:
  override def render(
      commandName: String,
      description: String,
      usage: Option[String],
      options: Seq[CLOption],
      arguments: Seq[CLArgument],
      subCommands: Seq[(String, String)]
  ): String =
    val sb       = StringBuilder()
    val useColor = CliEnv.supportsColor

    def cyan(s: String) =
      if useColor then
        Tint.cyan(s)
      else
        s
    def brightCyan(s: String) =
      if useColor then
        Tint.brightCyan(s)
      else
        s

    // Built-in help option (always shown)
    val allOptions =
      CLOption(
        prefixes = Seq("-h", "--help"),
        description = "Display this help message",
        param = None
      ) +: options

    // Usage line
    sb.append(s"${cyan("usage")}: ")
    usage match
      case Some(u) =>
        sb.append(u)
      case None =>
        val parts = Seq.newBuilder[String]
        parts += commandName
        // Always show [options] since help is always available
        if allOptions.nonEmpty then
          parts += s"[${cyan("options")}]"
        arguments.foreach(arg => parts += s"[${arg.name}]")
        if subCommands.nonEmpty then
          parts += s"<${cyan("command")}>"
        sb.append(parts.result().mkString(" "))
    sb.append("\n")

    // Description
    if description.nonEmpty then
      sb.append(s"\n  ${description}\n")

    // Options section
    if allOptions.nonEmpty then
      sb.append(s"\n[${cyan("options")}]\n")
      val maxPrefixLen = allOptions.map(_.prefixString.length).maxOption.getOrElse(0)
      allOptions.foreach { opt =>
        val prefixStr = opt.prefixString.padTo(maxPrefixLen + 2, ' ')
        sb.append(s"  ${brightCyan(prefixStr)} ${opt.description}\n")
      }

    // Arguments section
    if arguments.nonEmpty then
      sb.append(s"\n[${cyan("arguments")}]\n")
      val maxNameLen = arguments.map(_.name.length).maxOption.getOrElse(0)
      arguments.foreach { arg =>
        val nameStr = arg.name.padTo(maxNameLen + 2, ' ')
        sb.append(s"  ${brightCyan(nameStr)} ${arg.description}\n")
      }

    // Sub-commands section
    if subCommands.nonEmpty then
      sb.append(s"\n[${cyan("commands")}]\n")
      val maxCmdLen = subCommands.map(_._1.length).maxOption.getOrElse(0)
      subCommands.foreach { case (name, desc) =>
        val cmdStr = name.padTo(maxCmdLen + 2, ' ')
        sb.append(s"  ${brightCyan(cmdStr)} ${desc}\n")
      }

    sb.toString

  end render

end DefaultHelpPrinter
