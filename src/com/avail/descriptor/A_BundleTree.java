/**
 * A_BundleTree.java
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

import com.avail.utility.Pair;

import java.util.Collection;

/**
 * {@code A_BundleTree} is an interface that specifies the {@linkplain
 * MessageBundleTreeDescriptor message-bundle-tree}-specific operations that an
 * {@link AvailObject} must implement.  It's a sub-interface of {@link
 * A_BasicObject}, the interface that defines the behavior that all AvailObjects
 * are required to support.
 *
 * @author Mark van Gulik &lt;mark@availlang.org&gt;
 */
public interface A_BundleTree
extends A_BasicObject
{
	/**
	 * Answer the bundle tree's map of all plans.
	 *
	 * @return A map of type {bundle→{definition→plan|0..}|}.
	 */
	A_Map allParsingPlansInProgress ();

	/**
	 * Expand the bundle tree if there's anything currently unclassified in it.
	 * By postponing this until necessary, construction of the parsing rules for
	 * the grammar is postponed until actually necessary.
	 *
	 * @param module
	 *        The current module which this bundle tree is being used to parse.
	 */
	void expand (A_Module module);

	/**
	 * A grammatical restriction has been added.  Update this bundle tree to
	 * conform to the new restriction along any already-expanded paths for the
	 * given plan.  Updated the treesToVisit collection to include any new
	 * trees to visit as a consequence of visiting this tree.
	 *
	 * @param planInProgress
	 *        The {@link A_DefinitionParsingPlan} along which to update the
	 *        bundle tree (and extant successors).
	 * @param treesToVisit
	 *        A collection of {@link Pair<A_BundleTree,
	 *        A_ParsingPlanInProgress>} to visit.  Updated to include successors
	 *        of this <bundle, planInProgress>.
	 */
	void updateForNewGrammaticalRestriction (
		final A_ParsingPlanInProgress planInProgress,
		final Collection<Pair<A_BundleTree, A_ParsingPlanInProgress>>
			treesToVisit);

	/**
	 * Dispatch to the descriptor.
	 * @return
	 */
	A_Set lazyComplete ();

	/**
	 * Dispatch to the descriptor.
	 * @return
	 */
	A_Map lazyIncomplete ();

	/**
	 * @return
	 */
	A_Map lazyIncompleteCaseInsensitive ();

	/**
	 * Dispatch to the descriptor.
	 * @return
	 */
	A_Map lazyActions ();

	/**
	 * @return
	 */
	A_Map lazyPrefilterMap ();

	/**
	 * If this message bundle tree has a type filter tree, return the raw pojo
	 * holding it, otherwise {@link NilDescriptor#nil()}.
	 *
	 * @return The type filter tree pojo or nil.
	 */
	A_BasicObject lazyTypeFilterTreePojo ();

	/**
	 * Add a {@link DefinitionParsingPlanDescriptor definition parsing plan} to
	 * this bundle tree.  The corresponding bundle must already be present.
	 *
	 * @param planInProgress
	 *            The definition parsing plan to add.
	 */
	void addPlanInProgress (A_ParsingPlanInProgress planInProgress);

	/**
	 * Remove information about this {@link A_DefinitionParsingPlan definition
	 * parsing plan} from this bundle tree.
	 *
	 * @param planInProgress The parsing plan to exclude.
	 */
	void removePlanInProgress (A_ParsingPlanInProgress planInProgress);
}
