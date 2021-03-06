/*
 * Copyright 2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gradle.api.internal.artifacts.configurations;

import org.gradle.api.DomainObjectSet;
import org.gradle.api.UnknownDomainObjectException;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.UnknownConfigurationException;
import org.gradle.api.internal.AbstractNamedDomainObjectContainer;
import org.gradle.api.internal.DomainObjectContext;
import org.gradle.api.internal.artifacts.ConfigurationResolver;
import org.gradle.api.internal.artifacts.dsl.dependencies.ProjectFinder;
import org.gradle.api.internal.artifacts.ivyservice.moduleconverter.ConfigurationComponentMetaDataBuilder;
import org.gradle.api.internal.artifacts.ivyservice.resolutionstrategy.DefaultResolutionStrategy;
import org.gradle.api.internal.file.FileCollectionFactory;
import org.gradle.initialization.ProjectAccessListener;
import org.gradle.internal.event.ListenerManager;
import org.gradle.internal.reflect.Instantiator;

import java.util.Collection;
import java.util.Set;

public class DefaultConfigurationContainer extends AbstractNamedDomainObjectContainer<Configuration>
        implements ConfigurationContainerInternal, ConfigurationsProvider {
    public static final String DETACHED_CONFIGURATION_DEFAULT_NAME = "detachedConfiguration";
    
    private final ConfigurationResolver resolver;
    private final Instantiator instantiator;
    private final DomainObjectContext context;
    private final ListenerManager listenerManager;
    private final DependencyMetaDataProvider dependencyMetaDataProvider;
    private final ProjectAccessListener projectAccessListener;
    private final ProjectFinder projectFinder;
    private final ConfigurationComponentMetaDataBuilder configurationComponentMetaDataBuilder;
    private final FileCollectionFactory fileCollectionFactory;

    private int detachedConfigurationDefaultNameCounter = 1;

    public DefaultConfigurationContainer(ConfigurationResolver resolver,
                                         Instantiator instantiator, DomainObjectContext context, ListenerManager listenerManager,
                                         DependencyMetaDataProvider dependencyMetaDataProvider, ProjectAccessListener projectAccessListener,
                                         ProjectFinder projectFinder, ConfigurationComponentMetaDataBuilder configurationComponentMetaDataBuilder,
                                         FileCollectionFactory fileCollectionFactory) {
        super(Configuration.class, instantiator, new Configuration.Namer());
        this.resolver = resolver;
        this.instantiator = instantiator;
        this.context = context;
        this.listenerManager = listenerManager;
        this.dependencyMetaDataProvider = dependencyMetaDataProvider;
        this.projectAccessListener = projectAccessListener;
        this.projectFinder = projectFinder;
        this.configurationComponentMetaDataBuilder = configurationComponentMetaDataBuilder;
        this.fileCollectionFactory = fileCollectionFactory;
    }

    @Override
    protected Configuration doCreate(String name) {
        return instantiator.newInstance(DefaultConfiguration.class, context.absoluteProjectPath(name), name, this, resolver,
                listenerManager, dependencyMetaDataProvider, instantiator.newInstance(DefaultResolutionStrategy.class), projectAccessListener, projectFinder, configurationComponentMetaDataBuilder, fileCollectionFactory);
    }

    public Set<Configuration> getAll() {
        return this;
    }

    @Override
    public ConfigurationInternal getByName(String name) {
        return (ConfigurationInternal) super.getByName(name);
    }

    @Override
    public String getTypeDisplayName() {
        return "configuration";
    }

    @Override
    protected UnknownDomainObjectException createNotFoundException(String name) {
        return new UnknownConfigurationException(String.format("Configuration with name '%s' not found.", name));
    }

    public ConfigurationInternal detachedConfiguration(Dependency... dependencies) {
        String name = DETACHED_CONFIGURATION_DEFAULT_NAME + detachedConfigurationDefaultNameCounter++;
        DetachedConfigurationsProvider detachedConfigurationsProvider = new DetachedConfigurationsProvider();
        DefaultConfiguration detachedConfiguration = new DefaultConfiguration(
                name, name, detachedConfigurationsProvider, resolver,
                listenerManager, dependencyMetaDataProvider, new DefaultResolutionStrategy(), projectAccessListener, projectFinder, configurationComponentMetaDataBuilder, fileCollectionFactory);
        DomainObjectSet<Dependency> detachedDependencies = detachedConfiguration.getDependencies();
        for (Dependency dependency : dependencies) {
            detachedDependencies.add(dependency.copy());
        }
        detachedConfigurationsProvider.setTheOnlyConfiguration(detachedConfiguration);
        return detachedConfiguration;
    }

    /**
     * Build a formatted representation of all Configurations in this ConfigurationContainer.
     * Configuration(s) being toStringed are likely derivations of DefaultConfiguration.
     */
    public String dump() {
        StringBuilder reply = new StringBuilder();

        reply.append("Configuration of type: " + getTypeDisplayName());
        Collection<Configuration> configs = getAll();
        for (Configuration c : configs) {
            reply.append("\n  " + c.toString());
        }

        return reply.toString();
    }
}
