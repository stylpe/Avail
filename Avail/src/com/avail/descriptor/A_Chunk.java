/**
 * A_Chunk.java
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

/**
 * {@code A_Chunk} is an interface that specifies the {@linkplain
 * L2ChunkDescriptor level-two-chunk}-specific operations that an {@link
 * AvailObject} must implement.  It's a sub-interface of {@link
 * A_BasicObject}, the interface that defines the behavior that all AvailObjects
 * are required to support.
 *
 * @author Mark van Gulik &lt;mark@availlang.org&gt;
 */
public interface A_Chunk
extends A_BasicObject
{
	/**
	 * Dispatch to the descriptor.
	 */
	int index ();

	/**
	 * Dispatch to the descriptor.
	 */
	void index (int value);

	/**
	 * Dispatch to the descriptor.
	 */
	int numDoubles ();

	/**
	 * Dispatch to the descriptor.
	 */
	int numIntegers ();

	/**
	 * Dispatch to the descriptor.
	 */
	int numObjects ();

	/**
	 * Dispatch to the descriptor.
	 */
	@Deprecated
	boolean isSaved ();

	/**
	 * Dispatch to the descriptor.
	 */
	@Deprecated
	void isSaved (boolean aBoolean);

	/**
	 * Dispatch to the descriptor.
	 */
	boolean isValid ();

	/**
	 * Dispatch to the descriptor.
	 */
	A_Tuple wordcodes ();

	/**
	 * Dispatch to the descriptor.
	 */
	A_Tuple vectors ();

	/**
	 * Also defined in {@link A_RawFunction}.
	 */
	AvailObject literalAt(int subscript);
}