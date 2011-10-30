package dash.internal

import java.lang.reflect.{Method, Field}

object ReflectionHelper {
  def reflect(obj: AnyRef, property: String): ReflectionRef = {
    val clazz: Class[_] = obj.getClass
    val fieldRefs = findFields(clazz, property).map(FieldRef(obj, _))
    val methodRefs = findMethods(clazz, property).map(MethodRef(obj, _))

    (fieldRefs.size, methodRefs.size) match {
      case (0, 0) => throw new Exception("Cannot resolve ref: " + property)
      case (1, 0) => fieldRefs(0)
      case (0, 1) => methodRefs(0)
      case _ => ReflectionRefs(obj, (fieldRefs ++ methodRefs).toArray)
    }
  }

  def reflectStatic(className: String, property: String): ReflectionRef = {
    val clazz = Class.forName(className)
    val fieldRefs = findFields(clazz, property).map(FieldRef(null, _))
    val methodRefs = findMethods(clazz, property).map(MethodRef(null, _))

    (fieldRefs.size, methodRefs.size) match {
      case (0, 0) => throw new Exception("Cannot resolve ref: " + property)
      case (1, 0) => fieldRefs(0)
      case (0, 1) => methodRefs(0)
      case _ => ReflectionRefs(null, (fieldRefs ++ methodRefs).toArray)
    }
  }

  private def findMethods(clazz: Class[_], methodName: String): List[Method] =
      findX(clazz, {_.getDeclaredMethods}, {method: Method => method.getName == methodName})

  private def findFields(clazz: Class[_], fieldName: String): List[Field] =
      findX(clazz, {_.getDeclaredFields}, {field: Field => field.getName == fieldName})

  private def findX[X](clazz: Class[_], extract: Class[_] => Array[X], fltr: X => Boolean): List[X] = {
    val localX: List[X] = extract(clazz).filter(fltr).toList
    clazz.getSuperclass match {
      case null => localX
      case superClass => localX ++ findX(superClass, extract, fltr)
    }
  }
}
