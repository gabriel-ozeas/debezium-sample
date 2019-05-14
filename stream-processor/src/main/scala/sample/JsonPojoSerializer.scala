package sample

import java.util

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import org.apache.kafka.common.serialization.Serializer

object JsonPojoSerializer {
  protected val OBJECT_MAPPER = new ObjectMapper().registerModule(DefaultScalaModule)
}

class JsonPojoSerializer[T] extends Serializer[T] {

  override def serialize(topic: String, data: T): Array[Byte] = {
    if (data == null) return null
    try
      JsonPojoSerializer.OBJECT_MAPPER.writeValueAsBytes(data)
    catch {
      case e: Exception =>
        throw new Exception("Error serializing JSON message", e)
    }
  }

  override def close(): Unit = {}
  override def configure(configs: util.Map[String, _], isKey: Boolean): Unit = {}
}