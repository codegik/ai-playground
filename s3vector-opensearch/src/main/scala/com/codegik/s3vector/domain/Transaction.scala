package com.codegik.s3vector.domain

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
