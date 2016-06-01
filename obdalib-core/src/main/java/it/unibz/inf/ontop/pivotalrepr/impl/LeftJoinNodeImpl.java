package it.unibz.inf.ontop.pivotalrepr.impl;

import it.unibz.inf.ontop.model.ImmutableExpression;
import it.unibz.inf.ontop.model.ImmutableTerm;
import it.unibz.inf.ontop.model.ImmutableSubstitution;
import it.unibz.inf.ontop.model.impl.ImmutabilityTools;
import it.unibz.inf.ontop.pivotalrepr.*;

import java.util.Optional;

public class LeftJoinNodeImpl extends JoinLikeNodeImpl implements LeftJoinNode {

    private static final String LEFT_JOIN_NODE_STR = "LJ";

    public LeftJoinNodeImpl(Optional<ImmutableExpression> optionalJoinCondition) {
        super(optionalJoinCondition);
    }

    @Override
    public void acceptVisitor(QueryNodeVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public LeftJoinNode clone() {
        return new LeftJoinNodeImpl(getOptionalFilterCondition());
    }

    @Override
    public LeftJoinNode acceptNodeTransformer(HomogeneousQueryNodeTransformer transformer) throws QueryNodeTransformationException {
        return transformer.transform(this);
    }

    @Override
    public LeftJoinNode changeOptionalFilterCondition(Optional<ImmutableExpression> newOptionalFilterCondition) {
        return new LeftJoinNodeImpl(newOptionalFilterCondition);
    }

    @Override
    public SubstitutionResults<LeftJoinNode> applyAscendingSubstitution(
            ImmutableSubstitution<? extends ImmutableTerm> substitution,
            QueryNode descendantNode, IntermediateQuery query) {
        if (isFromRightBranch(descendantNode, query)) {
            /**
             * Stops the propagation
             */
            return new SubstitutionResultsImpl<>(integrateSubstitutionAsLeftJoinCondition(substitution));
        }
        /**
         * Left-branch
         */
        else {
            return applyDescendingSubstitution(substitution);
        }
    }

    /**
     * TODO: explain
     */
    private LeftJoinNode integrateSubstitutionAsLeftJoinCondition(
            ImmutableSubstitution<? extends ImmutableTerm> substitution) {
        if (substitution.isEmpty()) {
            return clone();
        }

        ImmutableExpression newEqualities = substitution.convertIntoBooleanExpression().get();

        Optional<ImmutableExpression> optionalFormerCondition = getOptionalFilterCondition();
        ImmutableExpression newFilterCondition;
        if (optionalFormerCondition.isPresent()) {
            newFilterCondition = ImmutabilityTools.foldBooleanExpressions(
                    optionalFormerCondition.get(), newEqualities).get();
        }
        else {
            newFilterCondition = newEqualities;
        }
        return new LeftJoinNodeImpl(Optional.of(newFilterCondition));
    }

    @Override
    public SubstitutionResults<LeftJoinNode> applyDescendingSubstitution(
            ImmutableSubstitution<? extends ImmutableTerm> substitution) {
        LeftJoinNode newNode = new LeftJoinNodeImpl(transformOptionalBooleanExpression(substitution,
                getOptionalFilterCondition()));
        return new SubstitutionResultsImpl<>(newNode, substitution);
    }

    @Override
    public boolean isSyntacticallyEquivalentTo(QueryNode node) {
        return (node instanceof LeftJoinNode)
                && ((LeftJoinNode) node).getOptionalFilterCondition().equals(this.getOptionalFilterCondition());
    }

    @Override
    public NodeTransformationProposal acceptNodeTransformer(HeterogeneousQueryNodeTransformer transformer) {
        return transformer.transform(this);
    }

    @Override
    public String toString() {
        return LEFT_JOIN_NODE_STR + getOptionalFilterString();
    }

    /**
     * TODO: explain
     *
     * TODO: move it to the NonCommutativeOperatorNodeImpl when the latter will be created.
     */
    protected boolean isFromRightBranch(QueryNode descendantNode, IntermediateQuery query) {

        Optional<QueryNode> optionalCurrentNode = Optional.of(descendantNode);

        while (optionalCurrentNode.isPresent()) {
            QueryNode currentNode = optionalCurrentNode.get();
            Optional<QueryNode> optionalAncestor = query.getParent(currentNode);

            if (optionalAncestor.isPresent() && (optionalAncestor.get() == this)) {
                Optional<ArgumentPosition> optionalPosition = query.getOptionalPosition(this, currentNode);
                if (optionalPosition.isPresent()) {
                    switch(optionalPosition.get()) {
                        case LEFT:
                            return false;
                        case RIGHT:
                            return true;
                        default:
                            throw new RuntimeException("Unexpected position: " + optionalPosition.get());
                    }
                }
                else {
                    throw new RuntimeException("Inconsistent tree: no argument position after " + this);
                }
            }
            else {
                optionalCurrentNode = optionalAncestor;
            }
        }
        throw new IllegalArgumentException(descendantNode.toString() +  " is not a descendant of " + this);
    }
}