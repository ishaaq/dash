package dash.internal

import java.lang.reflect.{Method, Field}
abstract sealed class ReflectionRef {
  def help: String
}

final case class MethodRef(obj: AnyRef, method: Method) extends ReflectionRef {
  method.setAccessible(true)
  val numParams = method.getParameterTypes().length

  def help = new StringBuilder(method.getReturnType.getName).append(' ')
      .append(method.getDeclaringClass.getName).append('.').append(method.getName())
      .append('(').append(method.getParameterTypes.map(_.getName).mkString(", ")).append(')')
      .toString

  def invoke(params: Array[Any]) = params.length match {
    case 0 => method.invoke(obj)
    case _ => method.invoke(obj, params)
  }
}

final case class FieldRef(obj: AnyRef, field: Field) extends ReflectionRef {
  field.setAccessible(true)
  val value = field.get(obj)

  def help =
    new StringBuilder(field.getDeclaringClass.getName)
      .append('.').append(field.getName).append(" = ").append(value)
      .append(" (").append(field.getType.getName).append(") ")
      .toString

  val get = value;
  def set(value: Any) = field.set(obj, value);
}

final case class ReflectionRefs(obj: AnyRef, refs: Array[ReflectionRef]) extends ReflectionRef {
  def help =
    refs.zipWithIndex.map { case(ref, i) => "[" + i + "] " + ref.help }.mkString("\n")

  def invoke = refs
}
