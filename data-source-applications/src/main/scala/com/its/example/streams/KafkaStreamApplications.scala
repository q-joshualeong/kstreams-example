package com.its.example.streams

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication

@SpringBootApplication
class KafkaStreamApplications

object KafkaStreamApplications {
  def main(args: Array[String]): Unit = {
    SpringApplication.run(classOf[KafkaStreamApplications], args: _*)
  }
}
