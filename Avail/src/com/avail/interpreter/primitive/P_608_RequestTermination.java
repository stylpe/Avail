/**
 * P_608_RequestTermination.java
 * Copyright © 1993-2013, Mark van Gulik and Todd L Smith.
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
import static com.avail.descriptor.FiberDescriptor.InterruptRequestFlag.TERMINATION_REQUESTED;
import static com.avail.descriptor.FiberDescriptor.SynchronizationFlag.*;
import static com.avail.descriptor.FiberDescriptor.ExecutionState.SUSPENDED;
import java.util.*;
import com.avail.AvailRuntime;
import com.avail.descriptor.*;
import com.avail.interpreter.*;
import com.avail.utility.Continuation0;

/**
 * <strong>Primitive 608:</strong> Request termination of the given
 * {@linkplain FiberDescriptor fiber}. If the fiber is currently {@linkplain
 * P_610_ParkCurrentFiber parked}, then unpark it.
 */
public class P_608_RequestTermination
extends Primitive
{
	/**
	 * The sole instance of this primitive class. Accessed through reflection.
	 */
	public final static Primitive instance =
		new P_608_RequestTermination().init(
			1, CanInline, CannotFail, HasSideEffect);

	@Override
	public Result attempt (
		final List<AvailObject> args,
		final Interpreter interpreter)
	{
		assert args.size() == 1;
		final A_Fiber fiber = args.get(0);
		// This sucks, but is necessary. Just in case the fiber is joining, we
		// must acquire the join lock before attempting to acquire the fiber's
		// own lock.
		synchronized (Interpreter.joinLock)
		{
			fiber.lock(new Continuation0()
			{
				@Override
				public void value ()
				{
					switch (fiber.executionState())
					{
						case TERMINATED:
						case ABORTED:
							// Do nothing in these cases.
							break;
						case UNSTARTED:
						case RUNNING:
						case SUSPENDED:
						case INTERRUPTED:
							// Set the interrupt request flag.
							fiber.setInterruptRequestFlag(
								TERMINATION_REQUESTED);
							break;
						case PARKED:
							// Set the interrupt request flag.
							fiber.setInterruptRequestFlag(
								TERMINATION_REQUESTED);
							// Restore the parking permit (to prevent multiple
							// resumptions due to races with unpark).
							if (fiber.getAndSetSynchronizationFlag(
								PERMIT_UNAVAILABLE, false))
							{
								fiber.executionState(SUSPENDED);
								Interpreter.resumeFromPrimitive(
									AvailRuntime.current(),
									fiber,
									Result.SUCCESS,
									NilDescriptor.nil());
							}
							break;
						case JOINING:
							// Set the interrupt request flag.
							fiber.setInterruptRequestFlag(
								TERMINATION_REQUESTED);
							// If the fiber is trying to join another fiber,
							// then remove the fiber from its joinee's set of
							// joiners. Then resume it.
							final A_Fiber joinee = fiber.joinee();
							joinee.joiningFibers(
								joinee.joiningFibers()
									.setWithoutElementCanDestroy(
										fiber,
										false));
							fiber.executionState(SUSPENDED);
							Interpreter.resumeFromPrimitive(
								AvailRuntime.current(),
								fiber,
								Result.SUCCESS,
								NilDescriptor.nil());
							break;
						case ASLEEP:
							// Set the interrupt request flag.
							fiber.setInterruptRequestFlag(
								TERMINATION_REQUESTED);
							// Try to cancel the task (if any). This is best
							// effort only.
							final TimerTask task = fiber.wakeupTask();
							if (task != null)
							{
								task.cancel();
								fiber.wakeupTask(null);
							}
							fiber.executionState(SUSPENDED);
							Interpreter.resumeFromPrimitive(
								AvailRuntime.current(),
								fiber,
								Result.SUCCESS,
								NilDescriptor.nil());
							break;
						default:
							assert false;
							break;
					}
				}
			});
		}
		return interpreter.primitiveSuccess(NilDescriptor.nil());
	}

	@Override
	protected A_Type privateBlockTypeRestriction ()
	{
		return FunctionTypeDescriptor.create(
			TupleDescriptor.from(
				FiberTypeDescriptor.mostGeneralType()),
			TOP.o());
	}
}