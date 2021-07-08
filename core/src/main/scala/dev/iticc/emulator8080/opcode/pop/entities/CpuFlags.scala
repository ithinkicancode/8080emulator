package dev.iticc.emulator8080.opcode.pop
package entities

import zio.json.JsonCodec
import zio.json.DeriveJsonCodec.gen
import zio.optics._

final case class CpuFlags(
    sign: Boolean,
    zero: Boolean,
    auxCarry: Boolean,
    parity: Boolean,
    carry: Boolean
)
object CpuFlags {
  type BoolLens = Lens[CpuFlags, Boolean]

  implicit val codec: JsonCodec[CpuFlags] = gen

  object Lenses {
    val sign: BoolLens = Lens(
      p => Right(p.sign),
      v => p => Right(p.copy(sign = v))
    )
    val zero: BoolLens = Lens(
      p => Right(p.zero),
      v => p => Right(p.copy(zero = v))
    )
    val auxCarry: BoolLens = Lens(
      p => Right(p.auxCarry),
      v => p => Right(p.copy(auxCarry = v))
    )
    val parity: BoolLens = Lens(
      p => Right(p.parity),
      v => p => Right(p.copy(parity = v))
    )
    val carry: BoolLens = Lens(
      p => Right(p.carry),
      v => p => Right(p.copy(carry = v))
    )
  }
}
