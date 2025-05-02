package wvlet.ai.chat

case class ToolSpec(
    name: String,
    description: String,
    parameters: List[ToolParameter],
    returnType: ToolReturnType
)

case class ToolReturnType(dataType: String, description: String)
case class ToolParameter(
    name: String,
    description: String,
    dataType: String,
    required: Boolean,
    defaultValue: Option[Any] = None
)
