package info.archinnov.achilles.entity.operations;

import static info.archinnov.achilles.entity.metadata.JoinProperties.hasCascadeMerge;
import static info.archinnov.achilles.entity.metadata.PropertyType.joinPropertyType;
import info.archinnov.achilles.context.AchillesPersistenceContext;
import info.archinnov.achilles.context.CQLPersistenceContext;
import info.archinnov.achilles.entity.metadata.EntityMeta;
import info.archinnov.achilles.entity.metadata.PropertyMeta;
import info.archinnov.achilles.entity.operations.impl.CQLMergerImpl;
import info.archinnov.achilles.proxy.AchillesEntityInterceptor;
import info.archinnov.achilles.validation.Validator;

import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import com.google.common.collect.FluentIterable;

/**
 * CQLEntityMerger
 * 
 * @author DuyHai DOAN
 * 
 */
public class CQLEntityMerger implements AchillesEntityMerger
{
	private CQLEntityProxifier proxifier = new CQLEntityProxifier();
	private CQLEntityPersister persister = new CQLEntityPersister();
	private CQLMergerImpl mergerImpl = new CQLMergerImpl();

	@Override
	public <T> T merge(AchillesPersistenceContext context, T entity)
	{
		CQLPersistenceContext cqlContext = (CQLPersistenceContext) context;
		EntityMeta entityMeta = context.getEntityMeta();

		Validator.validateNotNull(entity, "Proxy object should not be null");
		Validator.validateNotNull(entityMeta, "entityMeta should not be null");

		T proxy;
		if (proxifier.isProxy(entity))
		{
			T realObject = proxifier.getRealObject(entity);
			AchillesEntityInterceptor<T> interceptor = proxifier.getInterceptor(entity);
			Map<Method, PropertyMeta<?, ?>> dirtyMap = interceptor.getDirtyMap();

			mergerImpl.merge(cqlContext, dirtyMap);

			List<PropertyMeta<?, ?>> joinPMs = FluentIterable
					.from(entityMeta.getAllMetas())
					.filter(joinPropertyType)
					.filter(hasCascadeMerge)
					.toImmutableList();

			mergerImpl.cascadeMerge(this, cqlContext, joinPMs);

			interceptor.setContext(cqlContext);
			interceptor.setTarget(realObject);
			proxy = entity;
		}
		else
		{
			if (!context.isWideRow())
			{
				this.persister.persist(context);
			}

			proxy = proxifier.buildProxy(entity, context);
		}

		return proxy;
	}

	public static class PropertyMetaComparator implements Comparator<PropertyMeta<?, ?>>
	{
		@Override
		public int compare(PropertyMeta<?, ?> arg0, PropertyMeta<?, ?> arg1)
		{
			return arg0.getPropertyName().compareTo(arg1.getPropertyName());
		}

	}
}
