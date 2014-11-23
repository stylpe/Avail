/**
 * P_045_ShortCircuitHelper.java
 * Copyright © 1993-2014, The Avail Foundation, LLC.
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
package com.avail.interpreter.primitive;

import static com.avail.descriptor.TypeDescriptor.Types.*;
import static com.avail.interpreter.Primitive.Flag.*;
import java.util.*;
import com.avail.annotations.Nullable;
import com.avail.descriptor.*;
import com.avail.interpreter.*;
import com.avail.interpreter.levelTwo.register.L2ObjectRegister;
import com.avail.optimizer.L2Translator.L1NaiveTranslator;

/**
 * <strong>Primitive 45:</strong> Run the zero-argument {@linkplain
 * FunctionDescriptor function}, ignoring the leading {@linkplain
 * EnumerationTypeDescriptor#booleanObject() boolean} argument. This is used
 * for short-circuit evaluation.
 */
public final class P_045_ShortCircuitHelper extends Primitive
{
	/**
	 * The sole instance of this primitive class.  Accessed through reflection.
	 */
	public final static Primitive instance =
		new P_045_ShortCircuitHelper().init(
			2, Invokes, CannotFail);

	@Override
	public Result attempt (
		final List<AvailObject> args,
		final Interpreter interpreter,
		final boolean skipReturnCheck)
	{
		assert args.size() == 2;
		@SuppressWarnings("unused")
		final A_Atom ignoredBool = args.get(0);
		final A_Function block = args.get(1);
		assert block.code().numArgs() == 0;
		return interpreter.invokeFunction(
			block,
			Collections.<AvailObject>emptyList(),
			false);
	}

	@Override
	public A_Type returnTypeGuaranteedByVM (
		final List<? extends A_Type> argumentTypes)
	{
		final A_Type blockType = argumentTypes.get(1);
		return blockType.returnType();
	}

	/**
	 * Clear the arguments list (to correspond with the arguments being sent to
	 * the function in the second argument), then answer the register holding
	 * that function.
	 */
	@Override
	public @Nullable L2ObjectRegister foldOutInvoker (
		final List<L2ObjectRegister> args,
		final L1NaiveTranslator naiveTranslator)
	{
		assert hasFlag(Flag.Invokes);
		assert !hasFlag(Flag.CanInline);
		assert !hasFlag(Flag.CanFold);

		final L2ObjectRegister functionReg = args.get(1);
		args.clear();
		return functionReg;
	}

	@Override
	protected A_Type privateBlockTypeRestriction ()
	{
		return FunctionTypeDescriptor.create(
			TupleDescriptor.from(
				ANY.o(),
				FunctionTypeDescriptor.create(
					TupleDescriptor.empty(),
					TOP.o())),
			TOP.o());
	}
}
