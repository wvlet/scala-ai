package wvlet.uni.weaver

case class WeaverConfig(discriminatorFieldName: String = "@type"):
  def withDiscriminatorFieldName(name: String): WeaverConfig = copy(discriminatorFieldName = name)
