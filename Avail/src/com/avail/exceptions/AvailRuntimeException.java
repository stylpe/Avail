/**
 * AvailRuntimeException.java
 * Copyright © 1993-2012, Mark van Gulik and Todd L Smith.
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

package com.avail.exceptions;

import com.avail.annotations.NotNull;
import com.avail.descriptor.AvailObject;

/**
 * {@code AvailRuntimeException} is the root of the hierarchy of {@linkplain
 * RuntimeException runtime exceptions} that are specific to the implementation
 * of {@link AvailObject} and its numerous primitive operations.
 *
 * @author Mark van Gulik &lt;ghoul137@gmail.com&gt;
 */
public abstract class AvailRuntimeException
extends RuntimeException
{
	/** The serial version identifier. */
	private static final long serialVersionUID = -8826389456392434347L;

	/** The {@linkplain AvailErrorCode error code}. */
	private final @NotNull AvailErrorCode errorCode;

	/**
	 * Answer the {@linkplain AvailErrorCode error code}.
	 *
	 * @return The {@linkplain AvailErrorCode error code}.
	 */
	public @NotNull AvailErrorCode errorCode ()
	{
		return errorCode;
	}

	/**
	 * Answer the numeric error code as an {@linkplain AvailObject Avail
	 * object}.
	 *
	 * @return The {@linkplain AvailObject numeric error code}.
	 */
	public @NotNull AvailObject numericCode ()
	{
		return errorCode.numericCode();
	}

	/**
	 * Construct a new {@link AvailRuntimeException} with the specified
	 * {@linkplain AvailErrorCode error code}.
	 *
	 * @param errorCode
	 *        The {@linkplain AvailErrorCode error code}.
	 */
	protected AvailRuntimeException (final @NotNull AvailErrorCode errorCode)
	{
		this.errorCode = errorCode;
	}

	/**
	 * Construct a new {@link AvailRuntimeException} with the specified
	 * {@linkplain Throwable cause}.
	 *
	 * @param errorCode
	 *        The {@linkplain AvailErrorCode error code}.
	 * @param cause
	 *        The proximal {@linkplain Throwable cause} of the {@linkplain
	 *        AvailRuntimeException exception}.
	 */
	protected AvailRuntimeException (
		final @NotNull AvailErrorCode errorCode,
		final @NotNull Throwable cause)
	{
		super(cause);
		this.errorCode = errorCode;
	}
}
