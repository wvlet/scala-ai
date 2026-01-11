package wvlet.uni.agent.chat

import wvlet.uni.agent.core.DataType

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
