package dev.iticc.emulator8080.opcode.pop
package entities

import zio.json.JsonCodec
import zio.json.DeriveJsonCodec.gen
import zio.optics._

final case class CpuState(
    a: Int,
    b: Int,
    c: Int,
    d: Int,
    e: Int,
    h: Int,
    l: Int,
    stackPointer: Int,
    programCounter: Int,
    cycles: Long,
    flags: CpuFlags,
    interruptsEnabled: Boolean
) { self =>
  import CpuState.{ Lenses, updateByOpcode, UpdateResult }

  def updateWith(opcode: Int, lowByte: Int, highByte: Int): UpdateResult[String] = for {
    update1 <- Lenses.cycles.update(self) {
      _ + 10
    }
    update2 <- Lenses.stackPointer.update(update1) { v =>
      (v + 2) & 0xffff
    }
    result <- updateByOpcode(update2, opcode, lowByte, highByte)
  } yield result
}

object CpuState {
  type BoolLens = Lens[CpuState, Boolean]
  type IntLens = Lens[CpuState, Int]

  type UpdateResult[E] = OpticResult[E, CpuState]
  type UResult = UpdateResult[Nothing]

  implicit val codec: JsonCodec[CpuState] = gen

  object Lenses {
    val a: IntLens = Lens(
      p => Right(p.a),
      v => p => Right(p.copy(a = v))
    )
    val b: IntLens = Lens(
      p => Right(p.b),
      v => p => Right(p.copy(b = v))
    )
    val c: IntLens = Lens(
      p => Right(p.c),
      v => p => Right(p.copy(c = v))
    )
    val d: IntLens = Lens(
      p => Right(p.d),
      v => p => Right(p.copy(d = v))
    )
    val e: IntLens = Lens(
      p => Right(p.e),
      v => p => Right(p.copy(e = v))
    )
    val h: IntLens = Lens(
      p => Right(p.h),
      v => p => Right(p.copy(h = v))
    )
    val l: IntLens = Lens(
      p => Right(p.l),
      v => p => Right(p.copy(l = v))
    )
    val stackPointer: IntLens = Lens(
      p => Right(p.stackPointer),
      v => p => Right(p.copy(stackPointer = v))
    )

    val cycles: Lens[CpuState, Long] = Lens(
      p => Right(p.cycles),
      v => p => Right(p.copy(cycles = v))
    )

    val flags: Lens[CpuState, CpuFlags] = Lens(
      p => Right(p.flags),
      v => p => Right(p.copy(flags = v))
    )

    import CpuFlags.Lenses._

    val flagSign: BoolLens = flags >>> sign
    val flagZero: BoolLens = flags >>> zero
    val flagAuxCarry: BoolLens = flags >>> auxCarry
    val flagParity: BoolLens = flags >>> parity
    val flagCarry: BoolLens = flags >>> carry
  }

  def updateByOpcode(
      seed: CpuState,
      opcode: Int,
      lowByte: Int,
      highByte: Int
  ): UpdateResult[String] = {
    import Lenses._

    lazy val initialValue = Right(seed)

    opcode match {
      case 0xc1 =>
        foldWithLenses[Int](
          initialValue,
          Seq(
            b -> highByte,
            c -> lowByte
          )
        )
      case 0xd1 =>
        foldWithLenses[Int](
          initialValue,
          Seq(
            d -> highByte,
            e -> lowByte
          )
        )
      case 0xe1 =>
        foldWithLenses[Int](
          initialValue,
          Seq(
            h -> highByte,
            l -> lowByte
          )
        )
      case 0xf1 =>
        val compareWith = checkByte(lowByte) _

        foldWithLenses[Boolean](
          a.set(highByte)(seed),
          Seq(
            flagSign -> compareWith(0x80),
            flagZero -> compareWith(0x40),
            flagAuxCarry -> compareWith(0x10),
            flagParity -> compareWith(0x04),
            flagCarry -> compareWith(0x01)
          )
        )
      case _ =>
        Left("Invalid opcode")
    }
  }

  private def foldWithLenses[A](
      seed: UResult,
      updates: Seq[(Lens[CpuState, A], A)]
  ): UResult =
    updates.foldLeft[UResult](
      seed
    ) { case (Right(acc), (lens, value)) =>
      lens.set(value)(acc)
    // TODO: try getting rid of non-exhaustive warning, even though it's impossible because the left is `Nothing`
    // case (Left(_), (_, _)) =>
    }

  private def checkByte(byte: Int)(
      check: Int
  ): Boolean = (byte & check) == check
}
