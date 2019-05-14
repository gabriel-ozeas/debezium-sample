package sample

import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.kafka.common.serialization.Deserializer
import java.io.IOException
import java.util

import com.fasterxml.jackson.module.scala.DefaultScalaModule

import scala.reflect.ClassTag

object JsonHybridDeserializer {
  val DBZ_CDC_EVENT_PAYLOAD_FIELD = "payload"

  private val OBJECT_MAPPER = new ObjectMapper().registerModule(DefaultScalaModule)

  def apply[T](implicit ct: ClassTag[T]): JsonHybridDeserializer[T] = new JsonHybridDeserializer(ct.runtimeClass.asInstanceOf[Class[T]])
}

class JsonHybridDeserializer[T](clazz: Class[T]) extends Deserializer[T] {

  override def deserialize(topic: String, bytes: Array[Byte]): T = {
    if (bytes == null)
      null
    //step1: try to directly deserialize expected class
    try
      JsonHybridDeserializer.OBJECT_MAPPER.readValue[T](bytes, clazz)
    catch {
      case e1: IOException =>
        //step2: try to deserialize from DBZ json event
        try {
          val temp: Option[Map[Any, Any]] = JsonHybridDeserializer.OBJECT_MAPPER
            .readValue(new String(bytes), classOf[Map[Any, Any]])
            .get(JsonHybridDeserializer.DBZ_CDC_EVENT_PAYLOAD_FIELD).asInstanceOf[Option[Map[Any, Any]]]

          JsonHybridDeserializer.OBJECT_MAPPER.readValue(JsonHybridDeserializer.OBJECT_MAPPER.writeValueAsBytes(temp.get), clazz)
        } catch {
          case e2: IOException =>
            throw new Exception(e2)
        }
    }
  }

  override def close(): Unit = {}
  override def configure(configs: util.Map[String, _], isKey: Boolean): Unit = {}
}