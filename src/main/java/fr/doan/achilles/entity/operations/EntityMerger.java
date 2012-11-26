package fr.doan.achilles.entity.operations;

import java.lang.reflect.Method;
import java.util.Map.Entry;

import net.sf.cglib.proxy.Factory;
import fr.doan.achilles.dao.GenericDao;
import fr.doan.achilles.entity.metadata.EntityMeta;
import fr.doan.achilles.entity.metadata.PropertyMeta;
import fr.doan.achilles.proxy.EntityProxyUtil;
import fr.doan.achilles.proxy.builder.EntityProxyBuilder;
import fr.doan.achilles.proxy.interceptor.JpaInterceptor;
import fr.doan.achilles.validation.Validator;

@SuppressWarnings(
{
	"rawtypes"
})
public class EntityMerger
{
	private EntityPersister persister = new EntityPersister();

	private EntityProxyBuilder interceptorBuilder = new EntityProxyBuilder();

	private EntityProxyUtil util = new EntityProxyUtil();

	@SuppressWarnings("unchecked")
	public <T, ID> T mergeEntity(T entity, EntityMeta<ID> entityMeta)
	{
		Validator.validateNotNull(entity, "proxy object");
		Validator.validateNotNull(entityMeta, "entityMeta");

		T proxy;
		if (util.isProxy(entity))
		{
			Factory factory = (Factory) entity;
			JpaInterceptor<ID> interceptor = (JpaInterceptor<ID>) factory.getCallback(0);
			GenericDao<ID> dao = entityMeta.getDao();

			for (Entry<Method, PropertyMeta<?>> entry : interceptor.getDirtyMap().entrySet())
			{
				this.persister.persistProperty(entity, interceptor.getKey(), dao, entry.getValue());
			}

			proxy = (T) interceptorBuilder.build(interceptor.getTarget(), entityMeta, interceptor.getLazyLoaded());
		}
		else
		{
			this.persister.persist(entity, entityMeta);
			proxy = (T) interceptorBuilder.build(entity, entityMeta);
		}

		return proxy;
	}
}
