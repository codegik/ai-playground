package com.codegik.s3vector

import java.time.LocalDateTime

case class Transaction(
  id: String,
  amount: Double,
  merchantName: String,
  timestamp: LocalDateTime,
  description: String,
  categories: List[String]
) {
  def toSearchableText: String = {
    s"$merchantName $description ${categories.mkString(" ")}"
  }
}
