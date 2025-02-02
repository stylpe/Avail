/*
 * L2_CREATE_MAP.kt
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

import avail.descriptor.maps.A_Map
import avail.descriptor.maps.MapDescriptor
import avail.interpreter.levelTwo.L2Instruction
import avail.interpreter.levelTwo.L2OperandType
import avail.interpreter.levelTwo.L2OperandType.READ_BOXED_VECTOR
import avail.interpreter.levelTwo.L2OperandType.WRITE_BOXED
import avail.interpreter.levelTwo.L2Operation
import avail.interpreter.levelTwo.operand.L2ReadBoxedVectorOperand
import avail.interpreter.levelTwo.operand.L2WriteBoxedOperand
import avail.optimizer.jvm.JVMTranslator
import org.objectweb.asm.MethodVisitor

/**
 * Create a map from the specified key object registers and the corresponding
 * value object registers (writing the map into a specified object register).
 *
 * @author Mark van Gulik &lt;mark@availlang.org&gt;
 * @author Todd L Smith &lt;todd@availlang.org&gt;
 */
object L2_CREATE_MAP : L2Operation(
	READ_BOXED_VECTOR.named("keys"),
	READ_BOXED_VECTOR.named("values"),
	WRITE_BOXED.named("new map"))
{
	override fun appendToWithWarnings(
		instruction: L2Instruction,
		desiredTypes: Set<L2OperandType>,
		builder: StringBuilder,
		warningStyleChange: (Boolean) -> Unit)
	{
		assert(this == instruction.operation)
		val keys =
			instruction.operand<L2ReadBoxedVectorOperand>(0)
		val values =
			instruction.operand<L2ReadBoxedVectorOperand>(1)
		val map =
			instruction.operand<L2WriteBoxedOperand>(2)
		renderPreamble(instruction, builder)
		builder.append(' ')
		builder.append(map.registerString())
		builder.append(" ← {")
		var i = 0
		val limit = keys.elements().size
		while (i < limit)
		{
			if (i > 0)
			{
				builder.append(", ")
			}
			val key = keys.elements()[i]
			val value = values.elements()[i]
			builder.append(key.registerString())
			builder.append("→")
			builder.append(value.registerString())
			i++
		}
		builder.append('}')
	}

	override fun translateToJVM(
		translator: JVMTranslator,
		method: MethodVisitor,
		instruction: L2Instruction)
	{
		val keys = instruction.operand<L2ReadBoxedVectorOperand>(0)
		val values = instruction.operand<L2ReadBoxedVectorOperand>(1)
		val map = instruction.operand<L2WriteBoxedOperand>(2)

		// :: map = MapDescriptor.emptyMap;
		MapDescriptor.emptyMapMethod.generateCall(method)
		val limit = keys.elements().size
		assert(limit == values.elements().size)
		for (i in 0 until limit)
		{
			// :: map = mapAtPuttingStatic(
			// ::    map, «keysVector[i]», «valuesVector[i]»);
			translator.load(method, keys.elements()[i].register())
			translator.load(method, values.elements()[i].register())
			A_Map.mapAtPuttingStaticMethod.generateCall(method)
		}
		// :: destinationMap = map;
		translator.store(method, map.register())
	}
}
