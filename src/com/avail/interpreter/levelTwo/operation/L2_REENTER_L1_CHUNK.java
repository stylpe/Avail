/**
 * L2_REENTER_L1_CHUNK.java
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
package com.avail.interpreter.levelTwo.operation;

import static com.avail.interpreter.Interpreter.*;
import static com.avail.interpreter.levelTwo.L1InstructionStepper.*;
import static com.avail.interpreter.levelTwo.register.FixedRegister.*;
import com.avail.descriptor.A_Continuation;
import com.avail.interpreter.Interpreter;
import com.avail.interpreter.levelTwo.*;

/**
 * Arrive here by returning from a called method into unoptimized (level
 * one) code.  Explode the current continuation's slots into the registers
 * that level one expects.
 */
public class L2_REENTER_L1_CHUNK extends L2Operation
{
	/**
	 * Initialize the sole instance.
	 */
	public final static L2Operation instance =
		new L2_REENTER_L1_CHUNK().init();

	@Override
	public void step (
		final L2Instruction instruction,
		final Interpreter interpreter)
	{
		final A_Continuation continuation = interpreter.pointerAt(CALLER);
		final int numSlots = continuation.numArgsAndLocalsAndStack();
		for (int i = 1; i <= numSlots; i++)
		{
			interpreter.pointerAtPut(
				argumentOrLocalRegister(i),
				continuation.stackAt(i));
		}
		interpreter.integerAtPut(pcRegister(), continuation.pc());
		interpreter.integerAtPut(
			stackpRegister(),
			argumentOrLocalRegister(continuation.stackp()));
		interpreter.pointerAtPut(FUNCTION, continuation.function());
		interpreter.pointerAtPut(CALLER, continuation.caller());
	}

	@Override
	public boolean hasSideEffect ()
	{
		return true;
	}
}
