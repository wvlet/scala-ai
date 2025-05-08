package wvlet.ai.agent.chat

import wvlet.ai.agent.core.DataType

case class ToolSpec(
    name: String,
    description: String,
    parameters: List[ToolParameter],
    returnType: DataType
)

case class ToolParameter(
    name: String,
    description: String,
    dataType: DataType,
    defaultValue: Option[Any] = None
)
