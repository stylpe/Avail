/*
 * L2_RESTART_CONTINUATION_WITH_ARGUMENTS.java
 * Copyright © 1993-2018, The Avail Foundation, LLC.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 *  Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 *  Neither the name of the copyright holder nor the names of the contributors
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

import com.avail.descriptor.A_BasicObject;
import com.avail.descriptor.A_Continuation;
import com.avail.descriptor.A_Function;
import com.avail.descriptor.AvailObject;
import com.avail.interpreter.Interpreter;
import com.avail.interpreter.levelTwo.L2Chunk;
import com.avail.interpreter.levelTwo.L2Instruction;
import com.avail.interpreter.levelTwo.L2OperandType;
import com.avail.interpreter.levelTwo.operand.L2Operand;
import com.avail.interpreter.levelTwo.operand.L2ReadPointerOperand;
import com.avail.interpreter.levelTwo.operand.L2ReadVectorOperand;
import com.avail.interpreter.levelTwo.register.L2ObjectRegister;
import com.avail.interpreter.primitive.controlflow
	.P_RestartContinuationWithArguments;
import com.avail.optimizer.L2Generator;
import com.avail.optimizer.RegisterSet;
import com.avail.optimizer.StackReifier;
import com.avail.optimizer.jvm.JVMTranslator;
import com.avail.optimizer.jvm.ReferencedInGeneratedCode;
import org.objectweb.asm.MethodVisitor;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static com.avail.interpreter.levelTwo.L2OperandType.READ_POINTER;
import static com.avail.interpreter.levelTwo.L2OperandType.READ_VECTOR;
import static com.avail.interpreter.levelTwo.operation.L2_REIFY
	.StatisticCategory.ABANDON_BEFORE_RESTART_IN_L2;
import static com.avail.utility.Casts.cast;
import static org.objectweb.asm.Opcodes.ARETURN;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Type.*;

/**
 * Restart the given {@link A_Continuation continuation}, which already has the
 * correct program counter and level two offset (in case the {@link L2Chunk} is
 * still valid).  The function will start at the beginning, using the supplied
 * arguments, rather than the ones that were captured within the continuation.
 *
 * <p>This operation does the same thing as running {@link
 * P_RestartContinuationWithArguments}, but avoids the need for a reified
 * calling continuation.</p>
 *
 * @author Mark van Gulik &lt;mark@availlang.org&gt;
 * @author Todd L Smith &lt;todd@availlang.org&gt;
 */
public final class L2_RESTART_CONTINUATION_WITH_ARGUMENTS
extends L2ControlFlowOperation
{
	/**
	 * Construct an {@code L2_RESTART_CONTINUATION_WITH_ARGUMENTS}.
	 */
	private L2_RESTART_CONTINUATION_WITH_ARGUMENTS ()
	{
		super(
			READ_POINTER.is("continuation to restart"),
			READ_VECTOR.is("arguments"));
	}

	/**
	 * Initialize the sole instance.
	 */
	public static final L2_RESTART_CONTINUATION_WITH_ARGUMENTS instance =
		new L2_RESTART_CONTINUATION_WITH_ARGUMENTS();

	@Override
	protected void propagateTypes (
		final L2Instruction instruction,
		final List<RegisterSet> registerSets,
		final L2Generator generator)
	{
		// Do nothing; there are no destinations reached from here within the
		// current chunk.  Technically the restart might be to somewhere in the
		// current chunk, but that's not a requirement.
		assert registerSets.isEmpty();
	}

	@Override
	public boolean hasSideEffect ()
	{
		// Never remove this.
		return true;
	}

	/**
	 * Obtain an appropriate {@link StackReifier} for restarting the specified
	 * {@linkplain A_Continuation continuation}.
	 *
	 * @param interpreter
	 *        The {@link Interpreter}.
	 * @param continuation
	 *        The continuation to restart.
	 * @param arguments
	 *        The arguments with which to restart the continuation.
	 * @return The requested {@code StackReifier}.
	 */
	@SuppressWarnings("unused")
	@ReferencedInGeneratedCode
	public static StackReifier reifierToRestart (
		final Interpreter interpreter,
		final A_Continuation continuation,
		final AvailObject[] arguments)
	{
		return interpreter.abandonStackThen(
			ABANDON_BEFORE_RESTART_IN_L2.statistic,
			() ->
			{
				final A_Function function = continuation.function();
				final int numArgs = function.code().numArgs();
				assert arguments.length == numArgs;
				interpreter.argsBuffer.clear();
				Collections.addAll(interpreter.argsBuffer, arguments);
				interpreter.reifiedContinuation = continuation.caller();
				interpreter.function = function;
				interpreter.chunk = continuation.levelTwoChunk();
				interpreter.offset = continuation.levelTwoOffset();
				interpreter.returnNow = false;
				interpreter.latestResult(null);
			});
	}

	@Override
	public void toString (
		final L2Instruction instruction,
		final Set<L2OperandType> desiredTypes,
		final StringBuilder builder)
	{
		assert this == instruction.operation();
		final L2Operand continuationReg =
			instruction.readObjectRegisterAt(0);
		final L2ReadVectorOperand<
				L2ReadPointerOperand,
				L2ObjectRegister,
				A_BasicObject>
			argumentsVector = cast(instruction.operand(1));

		renderPreamble(instruction, builder);
		builder.append(' ');
		builder.append(continuationReg);
		builder.append("(");
		builder.append(argumentsVector);
		builder.append(")");
	}

	@Override
	public void translateToJVM (
		final JVMTranslator translator,
		final MethodVisitor method,
		final L2Instruction instruction)
	{
		final L2ObjectRegister continuationReg =
			instruction.readObjectRegisterAt(0).register();
		final List<L2ReadPointerOperand> argumentsVector =
			instruction.readVectorRegisterAt(1);

		// :: return L2_RESTART_CONTINUATION.reifierToRestart(
		// ::    interpreter, continuation);
		translator.loadInterpreter(method);
		translator.load(method, continuationReg);
		translator.objectArray(method, argumentsVector, AvailObject.class);
		method.visitMethodInsn(
			INVOKESTATIC,
			getInternalName(L2_RESTART_CONTINUATION_WITH_ARGUMENTS.class),
			"reifierToRestart",
			getMethodDescriptor(
				getType(StackReifier.class),
				getType(Interpreter.class),
				getType(A_Continuation.class),
				getType(AvailObject[].class)),
			false);
		method.visitInsn(ARETURN);
	}
}
