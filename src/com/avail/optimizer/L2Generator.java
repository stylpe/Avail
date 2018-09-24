/*
 * L2Generator.java
 * Copyright © 1993-2018, The Avail Foundation, LLC.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * * Neither the name of the copyright holder nor the names of the contributors
 * may be used to endorse or promote products derived from this software
 * without specific prior written permission.
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

package com.avail.optimizer;

import com.avail.annotations.InnerAccess;
import com.avail.descriptor.A_BasicObject;
import com.avail.descriptor.A_ChunkDependable;
import com.avail.descriptor.A_RawFunction;
import com.avail.descriptor.A_Set;
import com.avail.descriptor.FunctionDescriptor;
import com.avail.interpreter.levelTwo.L2Chunk;
import com.avail.interpreter.levelTwo.L2Instruction;
import com.avail.interpreter.levelTwo.L2OperandDispatcher;
import com.avail.interpreter.levelTwo.L2Operation;
import com.avail.interpreter.levelTwo.operand.*;
import com.avail.interpreter.levelTwo.operation.L2_JUMP;
import com.avail.interpreter.levelTwo.operation.L2_PHI_PSEUDO_OPERATION;
import com.avail.interpreter.levelTwo.register.L2FloatRegister;
import com.avail.interpreter.levelTwo.register.L2IntRegister;
import com.avail.interpreter.levelTwo.register.L2ObjectRegister;
import com.avail.interpreter.levelTwo.register.L2Register;
import com.avail.optimizer.values.L2SemanticValue;
import com.avail.performance.Statistic;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

import static com.avail.descriptor.SetDescriptor.emptySet;
import static com.avail.performance.StatisticReport.L2_OPTIMIZATION_TIME;
import static com.avail.utility.Nulls.stripNull;
import static java.lang.Math.max;
import static java.util.Collections.singletonList;

/**
 * The {@code L2Generator} converts a level one {@linkplain FunctionDescriptor
 * function} into a {@linkplain L2Chunk level two chunk}.  It optimizes as it
 * does so, folding and inlining method invocations whenever possible.
 *
 * @author Mark van Gulik &lt;mark@availlang.org&gt;
 */
public final class L2Generator
{
	/**
	 * Don't inline dispatch logic if there are more than this many possible
	 * implementations at a call site.  This may seem so small that it precludes
	 * many fruitful opportunities, but code splitting should help eliminate all
	 * but a few possibilities at many call sites.
	 */
	static final int maxPolymorphismToInlineDispatch = 4;

	/**
	 * Use a series of instance equality checks if we're doing type testing for
	 * method dispatch code and the type is a non-meta enumeration with at most
	 * this number of instances.  Otherwise do a type test.
	 */
	static final int maxExpandedEqualityChecks = 3;

	/**
	 * An indication of the possible degrees of optimization effort.  These are
	 * arranged approximately monotonically increasing in terms of both cost to
	 * generate and expected performance improvement.
	 */
	public enum OptimizationLevel
	{
		/**
		 * Unoptimized code, interpreted via level one machinery.  Technically
		 * the current implementation only executes level two code, but the
		 * default level two chunk relies on a level two instruction that simply
		 * fetches each nybblecode and interprets it.
		 */
		UNOPTIMIZED,

		/**
		 * The initial translation into level two instructions customized to a
		 * particular raw function.  This at least should avoid the cost of
		 * fetching nybblecodes.  It also avoids looking up monomorphic methods
		 * at execution time, and can inline or even fold calls to suitable
		 * primitives.  The inlined calls to infallible primitives are simpler
		 * than the calls to fallible ones or non-primitives or polymorphic
		 * methods.  Inlined primitive attempts avoid having to reify the
		 * calling continuation in the case that they're successful, but have to
		 * reify if the primitive fails.
		 */
		FIRST_TRANSLATION,

		/**
		 * Unimplemented.  The idea is that at this level some inlining of
		 * non-primitives will take place, emphasizing inlining of function
		 * application.  Invocations of methods that take a literal function
		 * should tend very strongly to get inlined, as the potential to
		 * turn things like continuation-based conditionals and loops into mere
		 * jumps is expected to be highly profitable.
		 */
		@Deprecated
		CHASED_BLOCKS,

		/**
		 * At some point the CPU cost of interpreting the level two code will
		 * exceed the cost of generating corresponding Java bytecodes.
		 */
		@Deprecated
		NATIVE;

		/** An array of all {@link OptimizationLevel} enumeration values. */
		private static final OptimizationLevel[] all = values();

		/**
		 * Answer the {@code OptimizationLevel} for the given ordinal value.
		 *
		 * @param targetOptimizationLevel
		 *        The ordinal value, an {@code int}.
		 * @return The corresponding {@code OptimizationLevel}, failing if the
		 *         ordinal was out of range.
		 */
		public static OptimizationLevel optimizationLevel (
			final int targetOptimizationLevel)
		{
			return all[targetOptimizationLevel];
		}
	}

	/**
	 * The amount of {@linkplain OptimizationLevel effort} to apply to the
	 * current optimization attempt.
	 */
	@InnerAccess final OptimizationLevel optimizationLevel;

	/**
	 * All {@link A_ChunkDependable contingent values} for which changes should
	 * cause the current {@linkplain L2Chunk level two chunk} to be
	 * invalidated.
	 */
	@InnerAccess A_Set contingentValues = emptySet();

	/** The block at which to resume execution after a failed primitive. */
	@Nullable L2BasicBlock afterOptionalInitialPrimitiveBlock;

	/**
	 * An {@code int} used to quickly generate unique integers which serve to
	 * visually distinguish new registers.
	 */
	private int uniqueCounter = 0;

	/**
	 * Answer the next value from the unique counter.  This is only used to
	 * distinguish registers for visual debugging.
	 *
	 * @return A int.
	 */
	public int nextUnique ()
	{
		return uniqueCounter++;
	}

	/**
	 * The {@linkplain L2Chunk level two chunk} generated by {@link
	 * #createChunk(A_RawFunction)}.  It can be retrieved via {@link #chunk()}.
	 */
	private @Nullable L2Chunk chunk;

	/**
	 * The {@link L2BasicBlock} which is the entry point for a function that has
	 * just been invoked.
	 */
	final L2BasicBlock initialBlock = createBasicBlock("START");

	/** The {@link L2BasicBlock} that code is currently being generated into. */
	private @Nullable L2BasicBlock currentBlock = initialBlock;

	/**
	 * Use this {@link L2ValueManifest} to track which {@link L2Register} holds
	 * which {@link L2SemanticValue} at the current code generation point.
	 */
	final L2ValueManifest currentManifest = new L2ValueManifest();

	/**
	 * Answer the current {@link L2ValueManifest}, which tracks which {@link
	 * L2Register} holds which {@link L2SemanticValue} at the current code
	 * generation point.
	 *
	 * @return The current {@link L2ValueManifest}.
	 */
	public L2ValueManifest currentManifest ()
	{
		return currentManifest;
	}

	/** The control flow graph being generated. */
	final L2ControlFlowGraph controlFlowGraph = new L2ControlFlowGraph();

	/**
	 * An {@link L2BasicBlock} that shouldn't actually be dynamically reachable.
	 */
	@Nullable L2BasicBlock unreachableBlock = null;

	/**
	 * Add an instruction that's not supposed to be reachable.
	 */
	@InnerAccess void addUnreachableCode ()
	{
		addInstruction(L2_JUMP.instance, unreachablePcOperand());
		startBlock(createBasicBlock("an unreachable block"));
	}

	/**
	 * Answer an L2PcOperand that targets an {@link L2BasicBlock} which should
	 * never actually be dynamically reached.
	 *
	 * @return An {@link L2PcOperand} that should never be traversed.
	 */
	public L2PcOperand unreachablePcOperand ()
	{
		if (unreachableBlock == null)
		{
			unreachableBlock = createBasicBlock("UNREACHABLE");
			// Because we generate the initial code in control flow order, we
			// have to wait until later to generate the instructions.  We strip
			// out all phi information here.
		}
		return new L2PcOperand(unreachableBlock, currentManifest);
	}

	/**
	 * Answer a {@link L2WritePhiOperand} that writes to the specified
	 * {@link L2Register}.
	 *
	 * @param register
	 *        The register.
	 * @return The new register write operand.
	 */
	@SuppressWarnings("MethodMayBeStatic")
	public <R extends L2Register<T>, T extends A_BasicObject>
	L2WritePhiOperand<R, T> newPhiRegisterWriter (final R register)
	{
		return new L2WritePhiOperand<>(register);
	}

	/**
	 * Create a new {@link L2BasicBlock}.  It's initially not connected to
	 * anything, and is ignored if it is never actually added with {@link
	 * #startBlock(L2BasicBlock)}.
	 *
	 * @param name The descriptive name of the new basic block.
	 * @return The new {@link L2BasicBlock}.
	 */
	@SuppressWarnings("MethodMayBeStatic")
	public L2BasicBlock createBasicBlock (final String name)
	{
		return new L2BasicBlock(name);
	}

	/**
	 * Start code generation for the given {@link L2BasicBlock}.  This naive
	 * translator doesn't create loops, so ensure all predecessor blocks have
	 * already finished generation.
	 *
	 * <p>Also, reconcile the slot registers that were collected for each
	 * predecessor, creating an {@link L2_PHI_PSEUDO_OPERATION} if needed.</p>
	 *
	 * @param block The {@link L2BasicBlock} beginning code generation.
	 */
	public void startBlock (final L2BasicBlock block)
	{
		if (!block.isIrremovable())
		{
			if (block.predecessorEdgesCount() == 0)
			{
				currentBlock = null;
				return;
			}
			if (block.predecessorEdgesCount() == 1)
			{
				final L2PcOperand predecessorEdge =
					block.predecessorEdgesIterator().next();
				final L2BasicBlock predecessorBlock =
					predecessorEdge.sourceBlock();
				final L2Instruction jump = predecessorBlock.finalInstruction();
				if (jump.operation() == L2_JUMP.instance)
				{
					// The new block has only one predecessor, which
					// unconditionally jumps to it.  Remove the jump and
					// continue generation in the predecessor block.  Restore
					// the manifest from the jump edge.
					currentManifest.clear();
					currentManifest.populateFromIntersection(
						singletonList(predecessorEdge.manifest()), this);
					predecessorBlock.instructions().remove(
						predecessorBlock.instructions().size() - 1);
					jump.justRemoved();
					currentBlock = predecessorBlock;
					return;
				}
			}
		}
		currentBlock = block;
		controlFlowGraph.startBlock(block);
		block.startIn(this);
	}

	/**
	 * Determine whether the current block is probably reachable.  If it has no
	 * predecessors and is removable, it's unreachable, but otherwise we assume
	 * it's reachable, at least until dead code elimination.
	 *
	 * @return Whether the current block is probably reachable.
	 */
	public boolean currentlyReachable ()
	{
		return currentBlock != null && currentBlock.currentlyReachable();
	}

	/**
	 * Create and add an {@link L2Instruction} with the given {@link
	 * L2Operation} and variable number of {@link L2Operand}s.
	 *
	 * @param operation
	 *        The operation to invoke.
	 * @param operands
	 *        The operands of the instruction.
	 */
	public void addInstruction (
		final L2Operation operation,
		final L2Operand... operands)
	{
		if (currentBlock != null)
		{
			currentBlock.addInstruction(
				new L2Instruction(currentBlock, operation, operands));
		}
	}

	/**
	 * Add an {@link L2Instruction}.
	 *
	 * @param instruction
	 *        The instruction to add.
	 */
	public void addInstruction (
		final L2Instruction instruction)
	{
		if (currentBlock != null)
		{
			currentBlock.addInstruction(instruction);
		}
	}

	/**
	 * Generate a {@linkplain L2Chunk Level Two chunk} from the control flow
	 * graph.  Store it in the {@code L2Generator}, from which it can be
	 * retrieved via {@link #chunk()}.
	 *
	 * @param code
	 *        The {@link A_RawFunction} which is the source of chunk creation.
	 */
	void createChunk (
		final A_RawFunction code)
	{
		assert chunk == null;
		final List<L2Instruction> instructions = new ArrayList<>();
		controlFlowGraph.generateOn(instructions);
		final RegisterCounter registerCounter = new RegisterCounter();
		for (final L2Instruction instruction : instructions)
		{
			instruction.operandsDo(
				operand -> operand.dispatchOperand(registerCounter));
		}

		final int afterPrimitiveOffset =
			afterOptionalInitialPrimitiveBlock == null
				? stripNull(initialBlock).offset()
				: afterOptionalInitialPrimitiveBlock.offset();
		assert afterPrimitiveOffset >= 0;

		chunk = L2Chunk.allocate(
			code,
			registerCounter.objectMax + 1,
			registerCounter.intMax + 1,
			registerCounter.floatMax + 1,
			afterPrimitiveOffset,
			instructions,
			controlFlowGraph,
			contingentValues);
	}

	/**
	 * Return the {@link L2Chunk} previously created via {@link
	 * #createChunk(A_RawFunction)}.
	 *
	 * @return The chunk.
	 */
	L2Chunk chunk ()
	{
		return stripNull(chunk);
	}

	/**
	 * Construct a new {@code L2Generator}.
	 *
	 * @param optimizationLevel
	 *        The optimization level.
	 */
	L2Generator (
		final OptimizationLevel optimizationLevel)
	{
		this.optimizationLevel = optimizationLevel;
	}

	/**
	 * Statistics about final chunk generation from the optimized {@link
	 * L2ControlFlowGraph}.
	 */
	static final Statistic finalGenerationStat = new Statistic(
		"Final chunk generation", L2_OPTIMIZATION_TIME);

	public static class RegisterCounter
	implements L2OperandDispatcher
	{
		int objectMax = -1;
		int intMax = -1;
		int floatMax = -1;

		@Override
		public void doOperand (final L2CommentOperand operand) { }

		@Override
		public void doOperand (final L2ConstantOperand operand) { }

		@Override
		public void doOperand (final L2IntImmediateOperand operand) { }

		@Override
		public void doOperand (final L2FloatImmediateOperand operand) { }

		@Override
		public void doOperand (final L2PcOperand operand) { }

		@Override
		public void doOperand (final L2PrimitiveOperand operand) { }

		@Override
		public void doOperand (final L2InternalCounterOperand operand) { }

		@Override
		public void doOperand (final L2ReadIntOperand operand)
		{
			intMax = max(intMax, operand.finalIndex());
		}

		@Override
		public void doOperand (final L2ReadFloatOperand operand)
		{
			floatMax = max(floatMax, operand.finalIndex());
		}

		@Override
		public void doOperand (final L2ReadPointerOperand operand)
		{
			objectMax = max(objectMax, operand.finalIndex());
		}

		@Override
		public <
			RR extends L2ReadOperand<R, T>,
			R extends L2Register<T>,
			T extends A_BasicObject>
		void
			doOperand (final L2ReadVectorOperand<RR, R, T> operand)
		{
			for (final L2ReadOperand<?, ?> register : operand.elements())
			{
				objectMax = max(objectMax, register.finalIndex());
			}
		}

		@Override
		public void doOperand (final L2SelectorOperand operand) { }

		@Override
		public void doOperand (final L2WriteIntOperand operand)
		{
			intMax = max(intMax, operand.finalIndex());
		}

		@Override
		public void doOperand (final L2WriteFloatOperand operand)
		{
			floatMax = max(floatMax, operand.finalIndex());
		}

		@Override
		public void doOperand (final L2WritePointerOperand operand)
		{
			objectMax = max(objectMax, operand.finalIndex());
		}

		@Override
		public <R extends L2Register<T>, T extends A_BasicObject> void
			doOperand (final L2WritePhiOperand<R, T> operand)
		{
			final L2Register<?> register = operand.register();
			if (register instanceof L2ObjectRegister)
			{
				objectMax = max(objectMax, operand.finalIndex());
			}
			else if (register instanceof L2IntRegister)
			{
				intMax = max(intMax, operand.finalIndex());
			}
			else
			{
				assert register instanceof L2FloatRegister;
				floatMax = max(floatMax, operand.finalIndex());
			}
		}
	}
}
