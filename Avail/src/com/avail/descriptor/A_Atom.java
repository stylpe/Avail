/**
 * A_Atom.java
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
 * {@code A_Atom} is an interface that specifies the atom-specific operations
 * that an {@link AvailObject} must implement.  It's a sub-interface of {@link
 * A_BasicObject}, the interface that defines the behavior that all AvailObjects
 * are required to support.
 *
 * @author Mark van Gulik &lt;mark@availlang.org&gt;
 */
public interface A_Atom
extends A_BasicObject
{
	/**
	 * Dispatch to the descriptor.
	 *
	 * TODO[MvG] - Break this accidental polymorphism for better clarity of
	 * intention.
	 */
	@Override
	AvailObject name ();

	/**
	 * @return
	 */
	A_Module issuingModule ();

	/**
	 * Dispatch to the descriptor.
	 */
	boolean extractBoolean ();

	/**
	 * @param key
	 * @param value
	 */
	void setAtomProperty (A_Atom key, A_BasicObject value);

	/**
	 * @param key
	 * @return
	 */
	AvailObject getAtomProperty (A_Atom key);

	/**
	 * Answer the {@linkplain MessageBundleDescriptor message bundle} associated
	 * with this atom.  If the atom does not yet have a message bundle
	 * associated with it, create one for that purpose and install it.
	 *
	 * @return The atom's message bundle.
	 */
	A_Bundle bundleOrCreate ();

	/**
	 * Answer the {@linkplain MessageBundleDescriptor message bundle} associated
	 * with this atom.  If the atom does not yet have a message bundle
	 * associated with it, answer {@linkplain NilDescriptor nil}.
	 *
	 * @return The atom's message bundle or nil.
	 */
	A_Bundle bundleOrNil ();
}
