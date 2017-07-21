/**
 * P_FiberResult.java
 * Copyright © 1993-2017, The Avail Foundation, LLC.
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

package com.avail.interpreter.primitive.fibers;

import static com.avail.descriptor.TypeDescriptor.Types.*;
import static com.avail.exceptions.AvailErrorCode.*;
import static com.avail.interpreter.Primitive.Flag.*;

import java.util.List;
import com.avail.descriptor.*;
import com.avail.interpreter.*;
import com.avail.utility.*;

/**
 * <strong>Primitive:</strong> Answer the result of the specified
 * {@linkplain FiberDescriptor fiber}.
 *
 * @author Todd L Smith &lt;todd@availlang.org&gt;
 */
public final class P_FiberResult
extends Primitive
{
	/**
	 * The sole instance of this primitive class. Accessed through reflection.
	 */
	public final static Primitive instance =
		new P_FiberResult().init(
			1, CanInline);

	@Override
	public Result attempt (
		final List<AvailObject> args,
		final Interpreter interpreter,
		final boolean skipReturnCheck)
	{
		assert args.size() == 1;
		final A_Fiber fiber = args.get(0);
		final MutableOrNull<Result> result = new MutableOrNull<>();
		fiber.lock(() ->
		{
			if (!fiber.executionState().indicatesTermination()
				|| fiber.fiberResult().equalsNil())
			{
				result.value = interpreter.primitiveFailure(
					E_FIBER_RESULT_UNAVAILABLE);
			}
			else
			{
				final AvailObject fiberResult = fiber.fiberResult();
				if (!fiberResult.isInstanceOf(fiber.kind().resultType()))
				{
					result.value = interpreter.primitiveFailure(
						E_FIBER_PRODUCED_INCORRECTLY_TYPED_RESULT);
				}
				else
				{
					result.value = interpreter.primitiveSuccess(
						fiber.fiberResult());
				}
			}
		});
		return result.value();
	}

	@Override
	protected A_Type privateBlockTypeRestriction ()
	{
		return FunctionTypeDescriptor.create(
			TupleDescriptor.from(
				FiberTypeDescriptor.mostGeneralType()),
			ANY.o());
	}

	@Override
	protected A_Type privateFailureVariableType ()
	{
		return AbstractEnumerationTypeDescriptor.withInstances(
			SetDescriptor.from(
				E_FIBER_RESULT_UNAVAILABLE,
				E_FIBER_PRODUCED_INCORRECTLY_TYPED_RESULT));
	}
}
