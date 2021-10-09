/*
 * L2_EXTRACT_TAG_ORDINAL.kt
 * Copyright © 1993-2021, The Avail Foundation, LLC.
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
package com.avail.interpreter.levelTwo.operation

import com.avail.descriptor.representation.AbstractDescriptor.Companion.staticTypeTagOrdinalMethod
import com.avail.descriptor.types.A_Type.Companion.instanceTag
import com.avail.descriptor.types.TypeTag
import com.avail.interpreter.levelTwo.L2Instruction
import com.avail.interpreter.levelTwo.L2OperandType
import com.avail.interpreter.levelTwo.L2OperandType.READ_BOXED
import com.avail.interpreter.levelTwo.L2OperandType.WRITE_INT
import com.avail.interpreter.levelTwo.L2Operation
import com.avail.interpreter.levelTwo.operand.L2IntImmediateOperand
import com.avail.interpreter.levelTwo.operand.L2ReadBoxedOperand
import com.avail.interpreter.levelTwo.operand.L2WriteIntOperand
import com.avail.optimizer.jvm.JVMTranslator
import com.avail.optimizer.reoptimizer.L2Regenerator
import org.objectweb.asm.MethodVisitor

/**
 * Extract the [TypeTag] of the given object, then extract its
 * [ordinal][Enum.ordinal] as an [Int].
 *
 * @author Mark van Gulik &lt;mark@availlang.org&gt;
 */
object L2_EXTRACT_TAG_ORDINAL : L2Operation(
	READ_BOXED.named("value"),
	WRITE_INT.named("metatag ordinal"))
{
	override fun appendToWithWarnings(
		instruction: L2Instruction,
		desiredTypes: Set<L2OperandType>,
		builder: StringBuilder,
		warningStyleChange: (Boolean) -> Unit)
	{
		assert(this == instruction.operation())
		val value = instruction.operand<L2ReadBoxedOperand>(0)
		val tagOrdinal = instruction.operand<L2WriteIntOperand>(1)
		renderPreamble(instruction, builder)
		builder
			.append(' ')
			.append(tagOrdinal.registerString())
			.append(" ← TAG(")
			.append(value.registerString())
			.append(")")
	}

	override fun generateReplacement(
		instruction: L2Instruction,
		regenerator: L2Regenerator)
	{
		val value = instruction.operand<L2ReadBoxedOperand>(0)
		val tagOrdinal = instruction.operand<L2WriteIntOperand>(1)

		// If the tag is statically deducible at this point, use the constant.
		val type = value.type()
		val baseTag = type.instanceTag
		if (baseTag.ordinal == baseTag.highOrdinal
			&& (!baseTag.isSubtagOf(TypeTag.TOP_TYPE_TAG)
				|| baseTag == TypeTag.BOTTOM_TYPE_TAG))
		{
			// This tag always applies, and it has no children, not even the
			// bottom type (which is special in the TypeTag hierarchy).
			regenerator.targetGenerator.addInstruction(
				L2_MOVE_CONSTANT.unboxedInt,
				L2IntImmediateOperand(baseTag.ordinal),
				tagOrdinal)
			return
		}
		super.generateReplacement(instruction, regenerator)
	}

	override fun translateToJVM(
		translator: JVMTranslator,
		method: MethodVisitor,
		instruction: L2Instruction)
	{
		val value = instruction.operand<L2ReadBoxedOperand>(0)
		val tagOrdinal = instruction.operand<L2WriteIntOperand>(1)

		// :: tagOrdinal = value.staticTypeTagOrdinal();
		translator.load(method, value.register())
		staticTypeTagOrdinalMethod.generateCall(method)
		translator.store(method, tagOrdinal.register())
	}
}
