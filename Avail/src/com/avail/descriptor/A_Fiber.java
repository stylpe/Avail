/**
 * A_Fiber.java
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

package com.avail.descriptor;

import java.util.TimerTask;
import com.avail.annotations.Nullable;
import com.avail.descriptor.FiberDescriptor.ExecutionState;
import com.avail.descriptor.FiberDescriptor.GeneralFlag;
import com.avail.descriptor.FiberDescriptor.InterruptRequestFlag;
import com.avail.descriptor.FiberDescriptor.SynchronizationFlag;
import com.avail.descriptor.FiberDescriptor.TraceFlag;
import com.avail.interpreter.AvailLoader;
import com.avail.utility.evaluation.*;

/**
 * {@code A_Fiber} is an interface that specifies the fiber-specific operations
 * that an {@link AvailObject} must implement.  It's a sub-interface of {@link
 * A_BasicObject}, the interface that defines the behavior that all AvailObjects
 * are required to support.
 *
 * <p>The purpose for A_BasicObject and its sub-interfaces is to allow sincere type
 * annotations about the basic kinds of objects that support or may be passed as
 * arguments to various operations.  The VM is free to always declare objects as
 * AvailObject, but in cases where it's clear that a particular object must
 * always be a fiber, a declaration of A_Fiber ensures that only the basic
 * object capabilities plus fiber-like capabilities are to be allowed.</p>
 *
 * @author Mark van Gulik &lt;mark@availlang.org&gt;
 */
public interface A_Fiber
extends A_BasicObject
{
	/**
	 * @return
	 */
	@Nullable AvailLoader availLoader ();

	/**
	 * @param loader
	 */
	void availLoader (@Nullable AvailLoader loader);

	/**
	 * Dispatch to the descriptor.
	 */
	A_BasicObject breakpointBlock ();

	/**
	 * Dispatch to the descriptor.
	 */
	void breakpointBlock (AvailObject value);

	/**
	 * @param flag
	 */
	void clearGeneralFlag (GeneralFlag flag);

	/**
	 * Dispatch to the descriptor.
	 */
	A_Continuation continuation ();

	/**
	 * Dispatch to the descriptor.
	 */
	void continuation (A_Continuation value);

	/**
	 * Dispatch to the descriptor.
	 */
	ExecutionState executionState ();

	/**
	 * Dispatch to the descriptor.
	 */
	void executionState (ExecutionState value);

	/**
	 * @return
	 */
	Continuation1<Throwable> failureContinuation ();

	/**
	 * @param onFailure
	 */
	void failureContinuation (Continuation1<Exception> onFailure);

	/**
	 * Dispatch to the descriptor.
	 */
	A_Map fiberGlobals ();

	/**
	 * Dispatch to the descriptor.
	 */
	void fiberGlobals (A_Map value);

	/**
	 * @return
	 */
	A_String fiberName ();

	/**
	 * @param value
	 */
	void fiberName (A_String value);

	/**
	 * @return
	 */
	AvailObject fiberResult ();

	/**
	 * @param result
	 */
	void fiberResult (A_BasicObject result);

	/**
	 * @param flag
	 * @return
	 */
	boolean generalFlag (GeneralFlag flag);

	/**
	 * @param flag
	 * @return
	 */
	boolean getAndClearInterruptRequestFlag (InterruptRequestFlag flag);

	/**
	 * @param scheduled
	 * @param b
	 * @return
	 */
	boolean getAndSetSynchronizationFlag (
		SynchronizationFlag scheduled,
		boolean b);

	/**
	 * @param flag
	 * @return
	 */
	public boolean traceFlag (final TraceFlag flag);

	/**
	 * @param flag
	 */
	public void setTraceFlag (final TraceFlag flag);

	/**
	 * @param flag
	 */
	public void clearTraceFlag (final TraceFlag flag);

	/**
	 * @return
	 */
	A_Map heritableFiberGlobals ();

	/**
	 * @param globals
	 */
	void heritableFiberGlobals (A_Map globals);

	/**
	 * @param flag
	 * @return
	 */
	boolean interruptRequestFlag (InterruptRequestFlag flag);

	/**
	 * @return
	 */
	A_Set joiningFibers ();

	/**
	 * @param empty
	 */
	void joiningFibers (A_Set empty);

	/**
	 * Answer this fiber's current priority.
	 * @return The priority.
	 */
	int priority ();

	/**
	 * Change this fiber's current priority.
	 * @param value The new priority.
	 */
	void priority (int value);

	/**
	 * @return
	 */
	Continuation1<AvailObject> resultContinuation ();

	/**
	 * @param onSuccess
	 */
	void resultContinuation (Continuation1<AvailObject> onSuccess);

	/**
	 * @param flag
	 */
	void setGeneralFlag (GeneralFlag flag);

	/**
	 * @param flag
	 */
	void setInterruptRequestFlag (InterruptRequestFlag flag);

	/**
	 * @return
	 */
	@Nullable TimerTask wakeupTask ();

	/**
	 * @param task
	 */
	void wakeupTask (@Nullable TimerTask task);

	/**
	 * Record access of the specified {@linkplain VariableDescriptor variable}
	 * by this {@linkplain FiberDescriptor fiber}.
	 *
	 * @param var
	 *        A variable.
	 * @param wasRead
	 *        {@code true} if the variable was read, {@code false} otherwise.
	 */
	public void recordVariableAccess (
		final A_Variable var,
		final boolean wasRead);

	/**
	 * Answer the {@linkplain SetDescriptor set} of {@linkplain
	 * VariableDescriptor variables} that were read before written. Only
	 * variables still live are included in this set; the {@linkplain
	 * TraceFlag#TRACE_VARIABLE_READS_BEFORE_WRITES trace mechanism}
	 * retains variables only weakly.
	 *
	 * @return The requested variables.
	 */
	public A_Set variablesReadBeforeWritten ();

	/**
	 * Answer the {@linkplain SetDescriptor set} of {@linkplain
	 * VariableDescriptor variables} that were written. Only variables still
	 * live are included in this set; the {@linkplain
	 * TraceFlag#TRACE_VARIABLE_READS_BEFORE_WRITES trace mechanism}
	 * retains variables only weakly.
	 *
	 * @return The requested variables.
	 */
	public A_Set variablesWritten ();

	/**
	 * Ensure the specified {@linkplain Continuation1 action} is invoked with
	 * this fiber's reified {@linkplain ContinuationDescriptor continuation} as
	 * soon as it's available.
	 *
	 * @param whenReified
	 */
	void whenContinuationIsAvailableDo (
		Continuation1<A_Continuation> whenReified);

	/**
	 * Extract the current set of {@linkplain Continuation1 actions} to perform
	 * when this fiber is next reified.  Replace it with the empty set.
	 *
	 * @return The set of outstanding actions, prior to clearing it.
	 */
	A_Set getAndClearReificationWaiters ();

	/**
	 * Record, for debugging, the fact that the specified primitive is being
	 * attempted.
	 *
	 * @param primitiveNumber The primitive being attempted.
	 */
	void recordLatestPrimitive (short primitiveNumber);
}
