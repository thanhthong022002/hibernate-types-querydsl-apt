package com.pallasathenagroup.querydsl;

import com.vladmihalcea.hibernate.type.array.ListArrayType;
import java.util.Collection;
import org.hibernate.engine.query.spi.HQLQueryPlan;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.jpa.TypedParameterValue;
import org.hibernate.query.internal.QueryImpl;
import org.hibernate.query.spi.QueryImplementor;

/**
 * A custom query impl to use specially for update query only
 * This allow to set List as a value, instead of `list of values`
 * This happens when allow update a field has List type, and the QueryImpl not set value the right way.
 *
 * @param <R>
 */
public class ExtendQueryImpl<R> extends QueryImpl<R> {

    public ExtendQueryImpl(SharedSessionContractImplementor producer, HQLQueryPlan hqlQueryPlan, String queryString) {
        super(producer, hqlQueryPlan, queryString);
    }

    public ExtendQueryImpl(QueryImpl query) {
        super(query.getProducer(), query.getQueryPlan(), query.getQueryString());
    }

    @Override
    public QueryImplementor setParameter(int position, Object value) {
        getProducer().checkOpen();
        if ( value instanceof TypedParameterValue) {
            final TypedParameterValue typedParameterValue = (TypedParameterValue) value;
            setParameter( position, typedParameterValue.getValue(), typedParameterValue.getType() );
        }
        else if ( value instanceof Collection && getQueryParameterBindings()
                .getBinding( position ).getBindType().getClass().isAssignableFrom(ListArrayType.class)) {
            getQueryParameterBindings().getBinding( position ).setBindValue( value );
        }
        else if ( value instanceof Collection && !isRegisteredAsBasicType( value.getClass() ) ) {
            setParameterList( getParameterMetadata().getQueryParameter( position ), (Collection) value );
        }
        else {
            getQueryParameterBindings().getBinding( position ).setBindValue( value );
        }
        return this;
    }

    private boolean isRegisteredAsBasicType(Class cl) {
        return getProducer().getFactory().getTypeResolver().basic( cl.getName() ) != null;
    }
}
