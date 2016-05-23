/*
 * Copyright 2016 the original author or authors.
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

package org.gradle.plugins.ide.eclipse.model.internal;

import com.google.common.collect.Lists;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ModuleVersionIdentifier;
import org.gradle.plugins.ide.eclipse.model.AbstractClasspathEntry;
import org.gradle.plugins.ide.eclipse.model.AbstractLibrary;
import org.gradle.plugins.ide.eclipse.model.EclipseClasspath;
import org.gradle.plugins.ide.eclipse.model.FileReference;
import org.gradle.plugins.ide.eclipse.model.Library;
import org.gradle.plugins.ide.eclipse.model.Variable;
import org.gradle.plugins.ide.internal.IdeDependenciesExtractor;
import org.gradle.plugins.ide.internal.resolver.model.IdeExtendedRepoFileDependency;
import org.gradle.plugins.ide.internal.resolver.model.IdeLocalFileDependency;
import org.gradle.plugins.ide.internal.resolver.model.IdeProjectDependency;
import org.gradle.plugins.ide.internal.resolver.model.UnresolvedIdeRepoFileDependency;
import org.gradle.util.DeprecationLogger;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class DependenciesCreator {

    protected final IdeDependenciesExtractor dependenciesExtractor;
    protected final EclipseClasspath classpath;

    public DependenciesCreator(EclipseClasspath classpath) {
        this.dependenciesExtractor = new IdeDependenciesExtractor();
        this.classpath = classpath;
    }

    protected Collection<Configuration> getPlusConfig() {
        return classpath.getPlusConfigurations();
    }

    protected Collection<Configuration> getMinusConfigs() {
        return classpath.getMinusConfigurations();
    }


    public List<AbstractClasspathEntry> createDependencyEntries() {
        List<AbstractClasspathEntry> result = Lists.newArrayList();
        result.addAll(createProjectDependencies());
        if (!classpath.isProjectDependenciesOnly()) {
            result.addAll(createLibraryDependencies());
        }
        return result;
    }

    public Collection<UnresolvedIdeRepoFileDependency> unresolvedExternalDependencies() {
        return dependenciesExtractor.unresolvedExternalDependencies(getPlusConfig(), classpath.getMinusConfigurations());
    }

    private List<AbstractClasspathEntry> createProjectDependencies() {
        ArrayList<AbstractClasspathEntry> projects = Lists.newArrayList();

        Collection<IdeProjectDependency> projectDependencies = dependenciesExtractor.extractProjectDependencies(classpath.getProject(), getPlusConfig(), getMinusConfigs());
        for (IdeProjectDependency projectDependency : projectDependencies) {
            projects.add(new ProjectDependencyBuilder().build(projectDependency));
        }
        return projects;
    }

    private List<AbstractClasspathEntry> createLibraryDependencies() {
        ArrayList<AbstractClasspathEntry> libraries = Lists.newArrayList();
        boolean downloadSources = classpath.isDownloadSources();
        boolean downloadJavadoc = classpath.isDownloadJavadoc();

        Collection<IdeExtendedRepoFileDependency> repoFileDependencies = dependenciesExtractor.extractRepoFileDependencies(classpath.getProject().getDependencies(), getPlusConfig(), getMinusConfigs(), downloadSources, downloadJavadoc);
        for (IdeExtendedRepoFileDependency dependency : repoFileDependencies) {
            libraries.add(createLibraryEntry(dependency.getFile(), dependency.getSourceFile(), dependency.getJavadocFile(), dependency.getDeclaredConfiguration(), classpath, dependency.getId()));
        }

        Collection<IdeLocalFileDependency> localFileDependencies = dependenciesExtractor.extractLocalFileDependencies(getPlusConfig(), getMinusConfigs());
        for (IdeLocalFileDependency it : localFileDependencies) {
            libraries.add(createLibraryEntry(it.getFile(), null, null, it.getDeclaredConfiguration(), classpath, null));
        }

        return libraries;
    }

    private static AbstractLibrary createLibraryEntry(File binary, File source, File javadoc, final String declaredConfigurationName, EclipseClasspath classpath, ModuleVersionIdentifier id) {
        FileReferenceFactory referenceFactory = classpath.getFileReferenceFactory();

        FileReference binaryRef = referenceFactory.fromFile(binary);
        FileReference sourceRef = referenceFactory.fromFile(source);
        FileReference javadocRef = referenceFactory.fromFile(javadoc);

        final AbstractLibrary out = binaryRef.isRelativeToPathVariable() ? new Variable(binaryRef) : new Library(binaryRef);

        out.setJavadocPath(javadocRef);
        out.setSourcePath(sourceRef);
        out.setExported(false);
        DeprecationLogger.whileDisabled(new Runnable() {
            @Override
            public void run() {
                out.setDeclaredConfigurationName(declaredConfigurationName);
            }
        });
        out.setModuleVersion(id);
        return out;
    }
}
