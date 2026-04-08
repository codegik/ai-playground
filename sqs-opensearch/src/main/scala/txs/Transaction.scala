package txs

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.scala.DefaultScalaModule

case class Transaction(
  id: String,
  amount: Double,
  currency: String,
  timestamp: Long
)

object Transaction:
  private val mapper = new ObjectMapper()
  mapper.registerModule(DefaultScalaModule)
  mapper.enable(SerializationFeature.INDENT_OUTPUT)

  def toJson(tx: Transaction): String =
    mapper.writeValueAsString(tx)

  def fromJson(json: String): Transaction =
    mapper.readValue(json, classOf[Transaction])
