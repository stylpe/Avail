/**
 * MessageBundleDescriptor.java
 * Copyright © 1993-2015, The Avail Foundation, LLC.
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

import static com.avail.descriptor.MessageBundleDescriptor.ObjectSlots.*;
import static com.avail.descriptor.TypeDescriptor.Types.MESSAGE_BUNDLE;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.List;
import com.avail.annotations.AvailMethod;
import com.avail.compiler.MessageSplitter;
import com.avail.compiler.ParsingOperation;
import com.avail.exceptions.MalformedMessageException;
import com.avail.serialization.SerializerOperation;
import com.avail.utility.json.JSONWriter;

/**
 * A message bundle is how a message name is bound to a {@linkplain
 * MethodDescriptor method}.  Besides the message name, the bundle also
 * contains information useful for parsing its invocations.  This information
 * includes parsing instructions which, when aggregated with other bundles,
 * forms a {@linkplain MessageBundleTreeDescriptor message bundle tree}.  This
 * allows parsing of multiple similar methods <em>in aggregate</em>, avoiding
 * the cost of repeatedly parsing the same constructs (tokens and
 * subexpressions) for different purposes.
 *
 * <p>
 * Additionally, the message bundle's {@link
 * ObjectSlots#GRAMMATICAL_RESTRICTIONS grammatical restrictions} are held here,
 * rather than with the {@linkplain MethodDescriptor method}, since these rules
 * are intended to work with the actual tokens that occur (how sends are
 * written), not their underlying semantics (what the methods do).
 * </p>
 *
 * @author Mark van Gulik &lt;mark@availlang.org&gt;
 */
public class MessageBundleDescriptor
extends Descriptor
{
	/**
	 * The layout of object slots for my instances.
	 */
	public enum ObjectSlots
	implements ObjectSlotsEnum
	{
		/**
		 * The {@linkplain MethodDescriptor method} for which this is a message
		 * bundle.  That is, if a use of this bundle is parsed, the resulting
		 * code will ultimately invoke this method.  A method may have multiple
		 * such bundles due to renaming of imports.
		 */
		METHOD,

		/**
		 * An {@linkplain AtomDescriptor atom} which is the "true name" of this
		 * bundle.  Due to import renaming, a {@linkplain MethodDescriptor
		 * method} might have multiple such names, one per bundle.
		 */
		MESSAGE,

		/**
		 * The tuple of {@linkplain StringDescriptor strings} comprising the
		 * method name's tokens. These tokens may be a single operator
		 * character, a sequence of alphanumerics, the underscore "_", an open
		 * guillemet "«", a close guillemet "»", the double-dagger "‡", the
		 * ellipsis "…", or any backquoted non-alphanumeric character "`$". Some
		 * of the parsing instructions index this tuple (e.g., to represent
		 * parsing a particular keyword). This tuple is produced by the {@link
		 * MessageSplitter}.
		 */
		MESSAGE_PARTS,

		/**
		 * The {@link MessageSplitter} that describes how to parse invocations
		 * of this message bundle.
		 */
		MESSAGE_SPLITTER_POJO,

		/**
		 * A {@linkplain SetDescriptor set} of {@linkplain
		 * GrammaticalRestrictionDescriptor grammatical restrictions} that apply
		 * to this message bundle.
		 */
		GRAMMATICAL_RESTRICTIONS,

		/**
		 * A tuple of integers that describe how to parse an invocation of this
		 * method. The integers encode parsing instructions, many of which can
		 * be executed en masse against a piece of Avail source code for
		 * multiple potential methods. This is facilitated by the incremental
		 * construction of a {@linkplain MessageBundleTreeDescriptor message
		 * bundle tree}. The instructions are produced during analysis of the
		 * method name by the {@link MessageSplitter}, which has a description
		 * of the complete instruction set.
		 */
		PARSING_INSTRUCTIONS,

		/**
		 * The {@link SetDescriptor set} of {@link
		 * DefinitionParsingPlanDescriptor definition parsing plans} that are
		 * defined for this bundle.  This should agree in size with all other
		 * renames of the same bundle (i.e., bundles attached to the same
		 * {@link MethodDescriptor method} as this), and with the method's own
		 * tuple of {@link DefinitionDescriptor definitions} and {@link
		 * MacroDefinitionDescriptor macro definitions}.
		 */
		DEFINITION_PARSING_PLANS;
	}

	/**
	 * Used for describing logical aspects of the bundle in the Eclipse
	 * debugger.
	 */
	public static enum FakeSlots
	implements ObjectSlotsEnum
	{
		/** Used for showing the parsing instructions symbolically. */
		SYMBOLIC_INSTRUCTIONS;
	}

	@Override boolean allowsImmutableToMutableReferenceInField (
		final AbstractSlotsEnum e)
	{
		return e == METHOD
			|| e == GRAMMATICAL_RESTRICTIONS
			|| e == DEFINITION_PARSING_PLANS;
	}

	@Override @AvailMethod
	void o_AddGrammaticalRestriction (
		final AvailObject object,
		final A_GrammaticalRestriction grammaticalRestriction)
	{
		if (isShared())
		{
			synchronized (object)
			{
				addGrammaticalRestriction(object, grammaticalRestriction);
			}
		}
		else
		{
			addGrammaticalRestriction(object, grammaticalRestriction);
		}
	}

	@Override @AvailMethod
	void o_AddDefinitionParsingPlan (
		final AvailObject object,
		final A_DefinitionParsingPlan plan)
	{
		if (isShared())
		{
			synchronized (object)
			{
				addDefinitionParsingPlan(object, plan);
			}
		}
		else
		{
			addDefinitionParsingPlan(object, plan);
		}
	}

	@Override @AvailMethod
	A_Method o_BundleMethod (final AvailObject object)
	{
		return object.mutableSlot(METHOD);
	}

	@Override
	A_Set o_DefinitionParsingPlans (final AvailObject object)
	{
		return object.slot(DEFINITION_PARSING_PLANS);
	}

	/**
	 * {@inheritDoc}
	 *
	 * Show the types of local variables and outer variables.
	 */
	@Override
	AvailObjectFieldHelper[] o_DescribeForDebugger (
		final AvailObject object)
	{
		final List<AvailObjectFieldHelper> fields = new ArrayList<>();
		fields.addAll(Arrays.asList(super.o_DescribeForDebugger(object)));

		final A_Tuple instructionsTuple = object.parsingInstructions();
		final List<A_String> descriptionsList = new ArrayList<>();
		for (
			int i = 1, end = instructionsTuple.tupleSize();
			i <= end;
			i++)
		{
			final int encodedInstruction = instructionsTuple.tupleIntAt(i);
			final ParsingOperation operation =
				ParsingOperation.decode(encodedInstruction);
			final int operand = operation.operand(encodedInstruction);
			final StringBuilder builder = new StringBuilder();
			builder.append(i);
			builder.append(". ");
			builder.append(operation.name());
			if (operand > 0)
			{
				builder.append(" (");
				builder.append(operand);
				builder.append(")");
				switch (operation)
				{
					case PARSE_PART:
					case PARSE_PART_CASE_INSENSITIVELY:
					{
						builder.append(" P=<");
						builder.append(
							object.messageParts().tupleAt(operand)
								.asNativeString());
						builder.append(">");
						break;
					}
					default:
						// Do nothing.
				}
			}
			descriptionsList.add(StringDescriptor.from(builder.toString()));
		}
		final A_Tuple descriptionsTuple = TupleDescriptor.fromList(descriptionsList);
		fields.add(new AvailObjectFieldHelper(
			object,
			FakeSlots.SYMBOLIC_INSTRUCTIONS,
			-1,
			descriptionsTuple));
		return fields.toArray(new AvailObjectFieldHelper[fields.size()]);
	}

	@Override @AvailMethod
	boolean o_Equals (final AvailObject object, final A_BasicObject another)
	{
		return another.traversed().sameAddressAs(object);
	}

	@Override @AvailMethod
	A_Set o_GrammaticalRestrictions (final AvailObject object)
	{
		return object.mutableSlot(GRAMMATICAL_RESTRICTIONS);
	}

	@Override @AvailMethod
	boolean o_HasGrammaticalRestrictions (final AvailObject object)
	{
		return object.mutableSlot(GRAMMATICAL_RESTRICTIONS).setSize() > 0;
	}

	@Override @AvailMethod
	int o_Hash (final AvailObject object)
	{
		return object.message().hash() ^ 0x0312CAB9;
	}

	@Override @AvailMethod
	A_Type o_Kind (final AvailObject object)
	{
		return MESSAGE_BUNDLE.o();
	}

	@Override @AvailMethod
	A_Atom o_Message (final AvailObject object)
	{
		return object.slot(MESSAGE);
	}

	@Override @AvailMethod
	A_Tuple o_MessageParts (final AvailObject object)
	{
		return object.slot(MESSAGE_PARTS);
	}

	@Override @AvailMethod
	MessageSplitter o_MessageSplitter(final AvailObject object)
	{
		final A_BasicObject splitterPojo = object.slot(MESSAGE_SPLITTER_POJO);
		return (MessageSplitter)splitterPojo.javaObject();
	}

	@Override @AvailMethod
	A_Tuple o_ParsingInstructions (final AvailObject object)
	{
		return object.slot(PARSING_INSTRUCTIONS);
	}

	@Override @AvailMethod
	void o_RemoveDefinitionParsingPlan (
		final AvailObject object,
		final A_DefinitionParsingPlan plan)
	{
		if (isShared())
		{
			synchronized (object)
			{
				removeDefinitionParsingPlan(object, plan);
			}
		}
		else
		{
			removeDefinitionParsingPlan(object, plan);
		}
	}

	@Override @AvailMethod
	void o_RemoveGrammaticalRestriction (
		final AvailObject object,
		final A_GrammaticalRestriction obsoleteRestriction)
	{
		if (isShared())
		{
			synchronized (object)
			{
				removeGrammaticalRestriction(object, obsoleteRestriction);
			}
		}
		else
		{
			removeGrammaticalRestriction(object, obsoleteRestriction);
		}
	}

	@Override @AvailMethod
	SerializerOperation o_SerializerOperation (final AvailObject object)
	{
		return SerializerOperation.MESSAGE_BUNDLE;
	}


	@Override
	void o_WriteTo (final AvailObject object, final JSONWriter writer)
	{
		writer.startObject();
		writer.write("kind");
		writer.write("message bundle");
		writer.write("method");
		object.slot(MESSAGE).atomName().writeTo(writer);
		writer.endObject();
	}

	/**
	 * Add a {@link DefinitionParsingPlanDescriptor definition parsing plan} to
	 * this bundle.  This is performed to make the bundle agree with the
	 * method's definitions and macro definitions.
	 *
	 * @param object The affected message bundle.
	 * @param plan A definition parsing plan.
	 */
	private void addDefinitionParsingPlan (
		final AvailObject object,
		final A_DefinitionParsingPlan plan)
	{
		A_Set plans = object.slot(DEFINITION_PARSING_PLANS);
		plans = plans.setWithElementCanDestroy(plan, true);
		object.setSlot(DEFINITION_PARSING_PLANS, plans.makeShared());
	}

	/**
	 * Remove a {@link DefinitionParsingPlanDescriptor definition parsing plan}
	 * from this bundle.  This is performed to make the bundle agree with the
	 * method's definitions and macro definitions.
	 *
	 * @param object The affected message bundle.
	 * @param plan A definition parsing plan.
	 */
	private void removeDefinitionParsingPlan (
		final AvailObject object,
		final A_DefinitionParsingPlan plan)
	{
		A_Set plans = object.mutableSlot(DEFINITION_PARSING_PLANS);
		assert plans.hasElement(plan);
		plans = plans.setWithoutElementCanDestroy(plan, true);
		object.setMutableSlot(DEFINITION_PARSING_PLANS, plans.makeShared());
	}

	/**
	 * Add a grammatical restriction to the specified {@linkplain
	 * MessageBundleDescriptor message bundle}.
	 *
	 * @param object The affected message bundle.
	 * @param grammaticalRestriction A grammatical restriction.
	 */
	private void addGrammaticalRestriction (
		final AvailObject object,
		final A_GrammaticalRestriction grammaticalRestriction)
	{
		A_Set restrictions = object.slot(GRAMMATICAL_RESTRICTIONS);
		restrictions = restrictions.setWithElementCanDestroy(
			grammaticalRestriction, true);
		object.setSlot(GRAMMATICAL_RESTRICTIONS, restrictions.makeShared());
	}

	/**
	 * Remove a grammatical restriction from this {@linkplain
	 * MessageBundleDescriptor message bundle}.
	 *
	 * @param object A message bundle.
	 * @param obsoleteRestriction The grammatical restriction to remove.
	 */
	private void removeGrammaticalRestriction (
		final AvailObject object,
		final A_GrammaticalRestriction obsoleteRestriction)
	{
		A_Set restrictions = object.mutableSlot(GRAMMATICAL_RESTRICTIONS);
		restrictions = restrictions.setWithoutElementCanDestroy(
			obsoleteRestriction,
			true);
		object.setMutableSlot(
			GRAMMATICAL_RESTRICTIONS, restrictions.makeShared());
	}

	@Override
	public void printObjectOnAvoidingIndent (
		final AvailObject object,
		final StringBuilder aStream,
		final IdentityHashMap<A_BasicObject, Void> recursionMap,
		final int indent)
	{
		// The existing definitions are also printed in parentheses to help
		// distinguish polymorphism from occurrences of non-polymorphic
		// homonyms.
		aStream.append("bundle \"");
		aStream.append(object.message().atomName().asNativeString());
		aStream.append("\"");
	}

	/**
	 * Create a new {@linkplain MessageBundleDescriptor message bundle} for the
	 * given message.  Add the bundle to the method's collection of {@linkplain
	 * MethodDescriptor.ObjectSlots#OWNING_BUNDLES owning bundles}.
	 *
	 * @param methodName The message name, an {@linkplain AtomDescriptor atom}.
	 * @param method The method that this bundle represents.
	 * @param splitter A MessageSplitter for this message name.
	 * @return A new {@linkplain MessageBundleDescriptor message bundle}.
	 * @throws MalformedMessageException If the message name is malformed.
	 */
	public static A_Bundle newBundle (
		final A_Atom methodName,
		final A_Method method,
		final MessageSplitter splitter)
	throws MalformedMessageException
	{
		assert methodName.isAtom();
		assert splitter.numberOfArguments() == method.numArgs();
		assert splitter.messageName().equals(methodName.atomName());

		final AvailObject splitterPojo =
			RawPojoDescriptor.identityWrap(splitter);
		final AvailObject result = mutable.create();
		result.setSlot(METHOD, method);
		result.setSlot(MESSAGE, methodName);
		result.setSlot(MESSAGE_PARTS, splitter.messageParts());
		result.setSlot(PARSING_INSTRUCTIONS, splitter.instructionsTuple());
		result.setSlot(MESSAGE_SPLITTER_POJO, splitterPojo);
		result.setSlot(GRAMMATICAL_RESTRICTIONS, SetDescriptor.empty());
		final A_Set plans = SetDescriptor.empty();
//TODO[MvG] - Finish type-filtering compiler changes
//		for (final A_Definition definition : method.definitionsTuple())
//		{
//			final A_DefinitionParsingPlan plan =
//				DefinitionParsingPlanDescriptor.createPlan(
//					result, definition);
//			plans = plans.setWithElementCanDestroy(plan, true);
//		}
		result.setSlot(DEFINITION_PARSING_PLANS, plans);
		result.makeShared();
		method.methodAddBundle(result);
		return result;
	}

	/**
	 * Construct a new {@link MessageBundleDescriptor}.
	 *
	 * @param mutability
	 *        The {@linkplain Mutability mutability} of the new descriptor.
	 */
	private MessageBundleDescriptor (final Mutability mutability)
	{
		super(mutability, ObjectSlots.class, null);
	}

	/** The mutable {@link MessageBundleDescriptor}. */
	private static final MessageBundleDescriptor mutable =
		new MessageBundleDescriptor(Mutability.MUTABLE);

	@Override
	MessageBundleDescriptor mutable ()
	{
		return mutable;
	}

	@Override
	MessageBundleDescriptor immutable ()
	{
		// There is no immutable variant.
		return shared;
	}

	/** The shared {@link MessageBundleDescriptor}. */
	private static final MessageBundleDescriptor shared =
		new MessageBundleDescriptor(Mutability.SHARED);

	@Override
	MessageBundleDescriptor shared ()
	{
		return shared;
	}
}
