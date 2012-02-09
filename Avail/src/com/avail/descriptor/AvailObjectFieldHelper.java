/**
 * AvailObjectFieldHelper.java
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

package com.avail.descriptor;

import com.avail.annotations.NotNull;

/**
 * This class assists with the presentation of {@link AvailObject}s in the
 * Eclipse debugger.  Since AvailObjects have a uniform structure consisting of
 * a descriptor, an array of AvailObjects, and an array of {@code int}s, it is
 * essential to the understanding of a hierarchy of Avail objects that they be
 * presented at the right level of abstraction, including the use of symbolic
 * names for conceptual subobjects.
 *
 * <p>
 * Eclipse is still kind of fiddly about these presentations, requiring explicit
 * manipulation through dialogs (well, maybe there's some way to hack around
 * with the Eclipse preference files).  Here are the minimum steps by which to
 * set up symbolic Avail descriptions:
 * <ol>
 * <li>Preferences... -> Debug -> Logical Structures -> Add:
 *   <ul>
 *   <li>Qualified name: com.avail.descriptor.AvailIntegerValueHelper</li>
 *   <li>Description: Hide integer value field</li>
 *   <li>Structure type: Single value</li>
 *   <li>Code: {@code new Object[0]}</li>
 *   </ul>
 * </li>
 * <li>Preferences... -> Debug -> Logical Structures -> Add:
 *   <ul>
 *   <li>Qualified name: com.avail.descriptor.AvailObject</li>
 *   <li>Description: Present Avail objects</li>
 *   <li>Structure type: Single value</li>
 *   <li>Code: {@code describeForDebugger()}</li>
 *   </ul>
 * </li>
 * <li>Preferences... -> Debug -> Logical Structures -> Add:
 *   <ul>
 *   <li>Qualified name: com.avail.descriptor.AvailObject</li>
 *   <li>Description: Present Avail objects</li>
 *   <li>Structure type: Single value</li>
 *   <li>Code: {@code describeForDebugger()}</li>
 *   </ul>
 * </li>
 * <li>Preferences... -> Debug -> Logical Structures -> Add:
 *   <ul>
 *   <li>Qualified name: com.avail.descriptor.AvailObjectFieldHelper</li>
 *   <li>Description: Present helper's value's fields instead of the helper</li>
 *   <li>Structure type: Single value</li>
 *   <li>Code: {@code value}</li>
 *   </ul>
 * </li>
 * <li>Preferences... -> Debug -> Detail Formatters -> Add:
 *   <ul>
 *   <li>Qualified type name: com.avail.descriptor.AvailObjectFieldHelper</li>
 *   <li>Description: Present helper's value's fields instead of the helper</li>
 *   <li>Detail formatter code snippet: {@code return name;}</li>
 *   <li>Enable this detail formatter: (checked)</li>
 *   <li>(after Ok) Show variable details: As the label for all variables</li>
 *   </ul>
 * </li>
 * <li>In the Debug perspective, go to the Variables view.  Select the toolbar
 * icon whose hover help is Show Logical Structure.</li>
 * </ol>
 * @author Mark van Gulik &lt;ghoul137@gmail.com&gt;
 */
public class AvailObjectFieldHelper
{
	/**
	 * The name to present for this field.
	 */
	public final String name;

	/**
	 * The actual value being presented with the given label.
	 */
	public final Object value;

	/** Construct a new {@link AvailObjectFieldHelper}.
	 *
	 * @param parentObject
	 *            The object containing the value.
	 * @param slot
	 *            The slot in which the value occurs.  Should be either
	 * @param subscriptToDisplay
	 *            The optional subscript for a repeating slot.  Uses -1 to
	 *            indicate this is not a repeating slot.
	 * @param value
	 *            The value found in that field of the object.
	 */
	public AvailObjectFieldHelper (
		final @NotNull AvailObject parentObject,
		final AbstractSlotsEnum slot,
		final int subscriptToDisplay,
		final Object value)
	{
		final StringBuilder builder = new StringBuilder();
		if (value == null)
		{
			builder.append(" = Java null");
		}
		else if (value instanceof AvailObject)
		{
			final AbstractDescriptor descriptor =
				((AvailObject)value).descriptor();
			String typeName = descriptor.getClass().getSimpleName();
			if (typeName.matches(".*Descriptor"))
			{
				typeName = typeName.substring(0, typeName.length() - 10);
			}
			if (descriptor.isMutable())
			{
				typeName = typeName + "\u2133";
			}
			builder.append(String.format(" (%s) = %s", typeName, value));
		}
		else if (value instanceof AvailIntegerValueHelper)
		{
			AbstractDescriptor.describeIntegerSlot(
				parentObject,
				((AvailIntegerValueHelper)value).intValue,
				(IntegerSlotsEnum)slot,
				builder);
		}
		else
		{
			builder.append(String.format(
				"*** UNKNOWN FIELD VALUE TYPE: %s ***",
				value.getClass().getCanonicalName()));
		}
		this.name = slot.name()
			+ (subscriptToDisplay == -1 ? "" : "[" + subscriptToDisplay + "]")
			+ builder;
		this.value = value;
	}
}
