/*
 * L2_SET_VARIABLE_NO_CHECK.kt
 * Copyright © 1993-2020, The Avail Foundation, LLC.
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

import com.avail.descriptor.types.A_Type.Companion.isSubtypeOf
import com.avail.descriptor.types.A_Type.Companion.writeType
import com.avail.descriptor.types.VariableTypeDescriptor.Companion.mostGeneralVariableType
import com.avail.descriptor.variables.A_Variable
import com.avail.descriptor.variables.VariableDescriptor
import com.avail.exceptions.VariableSetException
import com.avail.interpreter.levelTwo.L2Instruction
import com.avail.interpreter.levelTwo.L2NamedOperandType.Purpose
import com.avail.interpreter.levelTwo.L2OperandType
import com.avail.interpreter.levelTwo.L2OperandType.PC
import com.avail.interpreter.levelTwo.L2OperandType.READ_BOXED
import com.avail.interpreter.levelTwo.L2Operation.HiddenVariable.GLOBAL_STATE
import com.avail.interpreter.levelTwo.WritesHiddenVariable
import com.avail.interpreter.levelTwo.operand.L2PcOperand
import com.avail.interpreter.levelTwo.operand.L2ReadBoxedOperand
import com.avail.optimizer.L2Generator
import com.avail.optimizer.RegisterSet
import com.avail.optimizer.jvm.JVMTranslator
import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type

/**
 * Assign a value to a [variable][VariableDescriptor] *without*
 * checking that it's of the correct type.
 *
 * @author Mark van Gulik &lt;mark@availlang.org&gt;
 * @author Todd L Smith &lt;todd@availlang.org&gt;
 */
@WritesHiddenVariable(GLOBAL_STATE::class)
object L2_SET_VARIABLE_NO_CHECK : L2ControlFlowOperation(
	READ_BOXED.named("variable"),
	READ_BOXED.named("value to write"),
	PC.named("write succeeded", Purpose.SUCCESS),
	PC.named("write failed", Purpose.OFF_RAMP))
{
	override fun propagateTypes(
		instruction: L2Instruction,
		registerSets: List<RegisterSet>,
		generator: L2Generator)
	{
		val variable = instruction.operand<L2ReadBoxedOperand>(0)
		val value = instruction.operand<L2ReadBoxedOperand>(1)
		//		final L2PcOperand success = instruction.operand(2);
//		final L2PcOperand failure = instruction.operand(3);

		// The two register sets are clones, so only cross-check one of them.
		val registerSet = registerSets[0]
		assert(registerSet.hasTypeAt(variable.register()))
		val varType = registerSet.typeAt(variable.register())
		assert(varType.isSubtypeOf(mostGeneralVariableType()))
		assert(registerSet.hasTypeAt(value.register()))
		val valueType = registerSet.typeAt(value.register())
		assert(valueType.isSubtypeOf(varType.writeType()))
	}

	override fun hasSideEffect(): Boolean = true

	override val isVariableSet: Boolean
		get() = true

	override fun appendToWithWarnings(
		instruction: L2Instruction,
		desiredTypes: Set<L2OperandType>,
		builder: StringBuilder,
		warningStyleChange: (Boolean) -> Unit)
	{
		val variable = instruction.operand<L2ReadBoxedOperand>(0)
		val value = instruction.operand<L2ReadBoxedOperand>(1)
		assert(this == instruction.operation())
		renderPreamble(instruction, builder)
		builder.append(" ↓")
		builder.append(variable.registerString())
		builder.append(" ← ")
		builder.append(value.registerString())
		renderOperandsStartingAt(instruction, 2, desiredTypes, builder)
	}

	override fun translateToJVM(
		translator: JVMTranslator,
		method: MethodVisitor,
		instruction: L2Instruction)
	{
		val variable = instruction.operand<L2ReadBoxedOperand>(0)
		val value = instruction.operand<L2ReadBoxedOperand>(1)
		val success = instruction.operand<L2PcOperand>(2)
		val failure = instruction.operand<L2PcOperand>(3)

		// :: try {
		val tryStart = Label()
		val catchStart = Label()
		method.visitTryCatchBlock(
			tryStart,
			catchStart,
			catchStart,
			Type.getInternalName(VariableSetException::class.java))
		method.visitLabel(tryStart)
		// ::    variable.setValueNoCheck(value);
		translator.load(method, variable.register())
		translator.load(method, value.register())
		A_Variable.setValueNoCheckMethod.generateCall(method)
		// ::    goto success;
		// Note that we cannot potentially eliminate this branch with a
		// fall through, because the next instruction expects a
		// VariableSetException to be pushed onto the stack. So always do the
		// jump.
		translator.jump(method, success)
		// :: } catch (VariableSetException e) {
		method.visitLabel(catchStart)
		method.visitInsn(Opcodes.POP)
		// ::    goto failure;
		translator.jump(method, instruction, failure)
		// :: }
	}
}
