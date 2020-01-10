package com.pallasathenagroup.querydsl;

import com.google.common.collect.Range;
import com.querydsl.core.types.ConstantImpl;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.SimpleExpression;

public abstract class RangeExpression<X extends Comparable<?>> extends SimpleExpression<Range<X>> {

    public RangeExpression(Expression<Range<X>> mixin) {
        super(mixin);
    }

    public BooleanExpression intersects(Range<X> other) {
        return intersects(ConstantImpl.create(other));
    }

    public BooleanExpression intersects(Expression<Range<X>> other) {
        return Expressions.booleanOperation(RangeOps.INTERSECTS, mixin, other);
    }

}
