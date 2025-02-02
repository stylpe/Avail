/*
 * L2_SAVE_ALL_AND_PC_TO_INT.kt
 * Copyright © 1993-2022, The Avail Foundation, LLC.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * * Neither the name of the copyright holder nor the names of the contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package avail.interpreter.levelTwo.operation

import avail.descriptor.functions.ContinuationRegisterDumpDescriptor
import avail.interpreter.levelTwo.L2Instruction
import avail.interpreter.levelTwo.L2NamedOperandType.Purpose.REFERENCED_AS_INT
import avail.interpreter.levelTwo.L2NamedOperandType.Purpose.SUCCESS
import avail.interpreter.levelTwo.L2OperandType
import avail.interpreter.levelTwo.L2OperandType.PC
import avail.interpreter.levelTwo.L2OperandType.WRITE_BOXED
import avail.interpreter.levelTwo.L2OperandType.WRITE_INT
import avail.interpreter.levelTwo.L2Operation
import avail.interpreter.levelTwo.operand.L2PcOperand
import avail.interpreter.levelTwo.operand.L2WriteBoxedOperand
import avail.interpreter.levelTwo.operand.L2WriteIntOperand
import avail.optimizer.jvm.JVMTranslator
import org.objectweb.asm.MethodVisitor

/**
 * Extract the given "reference" edge's target level two offset as an [Int],
 * then follow the fall-through edge.  The int value will be used in the
 * fall-through code to assemble a continuation, which, when returned into, will
 * start at the reference edge target.  Note that the L2 offset of the reference
 * edge is not known until just before JVM code generation.
 *
 * This is a special operation, in that during final JVM code generation it
 * saves all objects in a register dump ([ContinuationRegisterDumpDescriptor]),
 * and the [L2_ENTER_L2_CHUNK] at the reference target will restore them.
 *
 * @author Mark van Gulik &lt;mark@availlang.org&gt;
 */
object L2_SAVE_ALL_AND_PC_TO_INT : L2Operation(
	PC.named("reference", REFERENCED_AS_INT),
	WRITE_INT.named("L2 address", SUCCESS),
	WRITE_BOXED.named("register dump", SUCCESS),
	PC.named("fall-through", SUCCESS))
{
	override fun targetEdges(instruction: L2Instruction): List<L2PcOperand>
	{
		assert(this == instruction.operation)
		return listOf(instruction.operand(0), instruction.operand(3))
	}

	override val hasSideEffect get() = true

	override val altersControlFlow get() = true

	/**
	 * Answer true if this instruction leads to multiple targets, *multiple* of
	 * which can be reached.  This is not the same as a branch, in which only
	 * one will be reached for any circumstance of reaching this instruction.
	 * In particular, the `L2_SAVE_ALL_AND_PC_TO_INT` instruction jumps
	 * to its fall-through label, but after reification has saved the live
	 * register state, it gets restored again and winds up traversing the other
	 * edge.
	 *
	 * This is an important distinction, in that this type of instruction
	 * should act as a barrier against redundancy elimination.  Otherwise an
	 * object with identity (i.e., a variable) created in the first branch won't
	 * be the same as the one produced again in the second branch.
	 *
	 * Also, we must treat as always-live-in to this instruction any values
	 * that are used in *either* branch, since they'll both be taken.
	 *
	 * @return
	 *   Whether multiple branches may be taken following the circumstance of
	 *   arriving at this instruction.
	 */
	override val goesMultipleWays: Boolean
		get() = true

	override fun appendToWithWarnings(
		instruction: L2Instruction,
		desiredTypes: Set<L2OperandType>,
		builder: StringBuilder,
		warningStyleChange: (Boolean) -> Unit)
	{
		assert(this == instruction.operation)
		val target = instruction.operand<L2PcOperand>(0)
		val targetAsInt = instruction.operand<L2WriteIntOperand>(1)
		val registerDump = instruction.operand<L2WriteBoxedOperand>(2)
		//		final L2PcOperand fallThrough = instruction.operand(3);
		renderPreamble(instruction, builder)
		builder.append(' ')
		builder.append(targetAsInt)
		builder.append(" ← address of label $[")
		builder.append(target.targetBlock().name())
		builder.append("]")
		if (target.offset() != -1)
		{
			builder.append("(=").append(target.offset()).append(")")
		}
		builder.append(",\n\tdump registers ")
		builder.append(registerDump)
	}

	override fun translateToJVM(
		translator: JVMTranslator,
		method: MethodVisitor,
		instruction: L2Instruction)
	{
		assert(this == instruction.operation)
		val target = instruction.operand<L2PcOperand>(0)
		val targetAsInt = instruction.operand<L2WriteIntOperand>(1)
		val registerDump = instruction.operand<L2WriteBoxedOperand>(2)
		val fallThrough = instruction.operand<L2PcOperand>(3)
		target.createAndPushRegisterDumpArrays(translator, method)
		// :: [AvailObject[], long[]]
		ContinuationRegisterDumpDescriptor.createRegisterDumpMethod
			.generateCall(method)
		// :: [registerDump]
		translator.store(method, registerDump.register())
		// :: []
		translator.intConstant(method, target.offset())
		translator.store(method, targetAsInt.register())

		// Jump is usually elided.
		translator.jump(method, instruction, fallThrough)
	}

	/**
	 * From the given [L2Instruction], extract the [edge][L2PcOperand] that
	 * indicates the L2 offset to capture as an [Int] in the second argument.
	 * The conversion of the edge to an int occurs very late, in
	 * [translateToJVM], as does the decision about which registers should be
	 * captured in the register dump – and restored when the [L2_ENTER_L2_CHUNK]
	 * at the referenced edge's target is reached.
	 *
	 * @param instruction
	 *   The instruction from which to extract the reference edge.
	 * @return
	 *   The referenced [edge][L2PcOperand].
	 */
	fun referenceOf(instruction: L2Instruction): L2PcOperand
	{
		assert(instruction.operation is L2_SAVE_ALL_AND_PC_TO_INT)
		return instruction.operand(0)
	}
}
