/*
 * L2_MOVE_FLOAT_CONSTANT.java
 * Copyright © 1993-2018, The Avail Foundation, LLC.
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

package com.avail.interpreter.levelTwo.operation;

import com.avail.interpreter.levelTwo.L2Instruction;
import com.avail.interpreter.levelTwo.L2OperandType;
import com.avail.interpreter.levelTwo.L2Operation;
import com.avail.interpreter.levelTwo.operand.L2WriteFloatOperand;
import com.avail.interpreter.levelTwo.register.L2FloatRegister;
import com.avail.optimizer.L2Generator;
import com.avail.optimizer.RegisterSet;
import com.avail.optimizer.jvm.JVMTranslator;
import org.objectweb.asm.MethodVisitor;

import java.util.Set;

import static com.avail.descriptor.DoubleDescriptor.fromDouble;
import static com.avail.interpreter.levelTwo.L2OperandType.READ_FLOAT;
import static com.avail.interpreter.levelTwo.L2OperandType.WRITE_FLOAT;

/**
 * @author Todd L Smith &lt;todd@availlang.org&gt;
 */
public final class L2_MOVE_FLOAT_CONSTANT
extends L2Operation
{
	/**
	 * Construct an {@code L2_MOVE_FLOAT_CONSTANT}.
	 */
	private L2_MOVE_FLOAT_CONSTANT ()
	{
		super(
			READ_FLOAT.is("source"),
			WRITE_FLOAT.is("destination"));
	}

	/**
	 * Initialize the sole instance.
	 */
	public static final L2_MOVE_FLOAT_CONSTANT instance =
		new L2_MOVE_FLOAT_CONSTANT();

	@Override
	protected void propagateTypes (
		final L2Instruction instruction,
		final RegisterSet registerSet,
		final L2Generator generator)
	{
		final double constant = instruction.floatImmediateAt(0);
		final L2WriteFloatOperand destinationIntReg =
			instruction.writeFloatRegisterAt(1);

		registerSet.constantAtPut(
			destinationIntReg.register(),
			fromDouble(constant),
			instruction);
	}

	@Override
	public void toString (
		final L2Instruction instruction,
		final Set<L2OperandType> desiredTypes,
		final StringBuilder builder)
	{
		assert this == instruction.operation();
		final double constant = instruction.floatImmediateAt(0);
		final L2FloatRegister destinationFloatReg =
			instruction.writeFloatRegisterAt(1).register();

		renderPreamble(instruction, builder);
		builder.append(' ');
		builder.append(destinationFloatReg);
		builder.append(" ← ");
		builder.append(constant);
	}


	@Override
	public void translateToJVM (
		final JVMTranslator translator,
		final MethodVisitor method,
		final L2Instruction instruction)
	{
		final double constant = instruction.floatImmediateAt(0);
		final L2FloatRegister destinationFloatReg =
			instruction.writeFloatRegisterAt(1).register();

		// :: destinationInt = constant;
		translator.literal(method, constant);
		translator.store(method, destinationFloatReg);
	}
}
