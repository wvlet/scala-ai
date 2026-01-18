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

import scala.annotation.StaticAnnotation

/**
  * Marks a class as a command entry point or a method as a sub-command.
  *
  * @param description
  *   Description of the command for help message
  * @param isDefault
  *   If true, this command runs when no sub-command is specified
  * @param usage
  *   One-line usage string (e.g., "[options] <file>")
  */
class command(val description: String = "", val isDefault: Boolean = false, val usage: String = "")
    extends StaticAnnotation

/**
  * Marks a parameter as a command-line option with prefix(es).
  *
  * @param prefix
  *   Comma-separated option prefixes (e.g., "-v,--verbose")
  * @param description
  *   Description for help message
  */
class option(val prefix: String = "", val description: String = "") extends StaticAnnotation

/**
  * Marks a parameter as a positional argument (no prefix).
  *
  * @param name
  *   Display name for the argument in help (defaults to parameter name)
  * @param description
  *   Description for help message
  */
class argument(val name: String = "", val description: String = "") extends StaticAnnotation
