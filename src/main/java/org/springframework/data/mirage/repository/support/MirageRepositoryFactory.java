/*
 * Copyright 2011 Daisuke Miyamoto.
 * Created on 2012/05/16
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package org.springframework.data.mirage.repository.support;

import java.io.Serializable;

import jp.sf.amateras.mirage.SqlManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mirage.repository.Identifiable;
import org.springframework.data.mirage.repository.IdentifiableMirageRepository;
import org.springframework.data.mirage.repository.LogicalDeleteMirageRepository;
import org.springframework.data.mirage.repository.DefaultLogicalDeleteMirageRepository;
import org.springframework.data.mirage.repository.NoSuchSqlResourceException;
import org.springframework.data.mirage.repository.DefaultMirageRepository;
import org.springframework.data.mirage.repository.query.MirageQueryLookupStrategy;
import org.springframework.data.mirage.repository.query.QueryExtractor;
import org.springframework.data.repository.core.EntityInformation;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;
import org.springframework.data.repository.query.QueryLookupStrategy;
import org.springframework.data.repository.query.QueryLookupStrategy.Key;
import org.springframework.util.Assert;

/**
 * TODO for daisuke
 * 
 * @since 1.0
 * @version $Id$
 * @author daisuke
 */
public class MirageRepositoryFactory extends RepositoryFactorySupport {
	
	private static Logger logger = LoggerFactory.getLogger(MirageRepositoryFactory.class);
	
	private final SqlManager sqlManager;
	
	private final QueryExtractor extractor;
	
	
	/**
	 * インスタンスを生成する。
	 * 
	 * @param sqlManager {@link SqlManager}
	 * @throws IllegalArgumentException if the argument is {@code null} 
	 */
	public MirageRepositoryFactory(SqlManager sqlManager) {
		Assert.notNull(sqlManager);
		this.sqlManager = sqlManager;
		extractor = null; // TODO
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public <T, ID extends Serializable>EntityInformation<T, ID> getEntityInformation(Class<T> domainClass) {
		return (EntityInformation<T, ID>) MirageEntityInformationSupport.getMetadata(domainClass, sqlManager);
	}
	
	@Override
	protected QueryLookupStrategy getQueryLookupStrategy(Key key) {
		return MirageQueryLookupStrategy.create(sqlManager, key, extractor);
	}
	
	@Override
	protected Class<?> getRepositoryBaseClass(RepositoryMetadata metadata) {
		if (isLogicalDeleteJdbcRepository(metadata.getRepositoryInterface())) {
			return DefaultLogicalDeleteMirageRepository.class;
		} else {
			return DefaultMirageRepository.class;
		}
	}
	
	@Override
	@SuppressWarnings({
		"rawtypes",
		"unchecked"
	})
	protected Object getTargetRepository(RepositoryMetadata metadata) {
		Class<?> repositoryInterface = metadata.getRepositoryInterface();
		EntityInformation<?, Serializable> entityInformation = getEntityInformation(metadata.getDomainType());
		
		DefaultMirageRepository repos;
		if (isLogicalDeleteJdbcRepository(repositoryInterface)) {
			repos = new DefaultLogicalDeleteMirageRepository(entityInformation, sqlManager);
		} else if (isIdentifiableJdbcRepository(entityInformation)) {
			repos =
					new IdentifiableMirageRepository<Identifiable>(
							(EntityInformation<Identifiable, ? extends Serializable>) entityInformation, sqlManager);
		} else {
			repos = new DefaultMirageRepository(entityInformation, sqlManager);
		}
		try {
			String name = repositoryInterface.getSimpleName() + ".sql";
			repos.setBaseSelectSqlResource(DefaultMirageRepository.newSqlResource(repositoryInterface, name));
		} catch (NoSuchSqlResourceException e) {
			logger.debug("Repository Default SQL [{}] not found, default used.", repositoryInterface);
		}
		return repos;
	}
	
	private boolean isIdentifiableJdbcRepository(EntityInformation<?, Serializable> entityInformation) {
		return Identifiable.class.isAssignableFrom(entityInformation.getJavaType());
	}
	
	/**
	 * Returns whether the given repository interface requires a QueryDsl specific implementation to be chosen.
	 * 
	 * @param repositoryInterface
	 * @return
	 */
	private boolean isLogicalDeleteJdbcRepository(Class<?> repositoryInterface) {
		return LogicalDeleteMirageRepository.class.isAssignableFrom(repositoryInterface);
	}
}
