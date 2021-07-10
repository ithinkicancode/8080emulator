package dev.iticc.emulator8080.opcode.pop
package entities

import zio.json.JsonCodec
import zio.json.DeriveJsonCodec.gen
import zio.optics._

final case class Cpu(
    opcode: Int,
    id: String,
    state: CpuState
) {
  def withNewState(lowByte: Int, highByte: Int): OpticResult[String, Cpu] =
    state
      .updateWith(opcode, lowByte, highByte)
      .map { newState =>
        copy(state = newState)
      }
}
object Cpu {
  implicit val codec: JsonCodec[Cpu] = gen
}
