package wvlet.ai.surface

trait ParameterBase extends Serializable:
  def name: String
  def surface: Surface

  def call(obj: Any, x: Any*): Any

trait Parameter extends ParameterBase:
  def index: Int
  def name: String

  /**
    * Surface for representing this parameter type
    */
  def surface: Surface

  /**
    * Returns true if this parameter has @required annotation
    */
  def isRequired: Boolean

  /**
    * Returns true if this parameter has @required annotation
    */
  def isSecret: Boolean

  /**
    * Get this parameter value from a given object x
    */
  def get(x: Any): Any

  override def call(obj: Any, x: Any*): Any = get(obj)

  /**
    * Get the default value of this parameter. For example the default value of x in class A(x:Int =
    * 10) is 10
    *
    * @return
    */
  def getDefaultValue: Option[Any]

end Parameter
