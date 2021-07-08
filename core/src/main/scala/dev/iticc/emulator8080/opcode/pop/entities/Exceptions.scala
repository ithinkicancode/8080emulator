package dev.iticc.emulator8080.opcode.pop
package entities

object Exceptions {
  final case class ApplicationError(reason: String) extends RuntimeException(reason)

  final case class BadClientRequest(reason: String) extends RuntimeException(reason)

  final case class RemoteHttpCallError(reason: String) extends RuntimeException(reason)
}
