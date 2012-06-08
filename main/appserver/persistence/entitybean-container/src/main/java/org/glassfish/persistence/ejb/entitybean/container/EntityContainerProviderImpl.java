/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

package org.glassfish.persistence.ejb.entitybean.container;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import com.sun.ejb.Container;
import com.sun.ejb.ContainerProvider;
import com.sun.logging.LogDomains;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.ejb.config.EjbContainer;
import org.glassfish.ejb.deployment.descriptor.EjbDescriptor;
import org.glassfish.ejb.deployment.descriptor.EjbEntityDescriptor;
import org.glassfish.ejb.deployment.descriptor.runtime.IASEjbExtraDescriptors;
import org.glassfish.hk2.api.IterableProvider;
import org.jvnet.hk2.annotations.Service;

@Service
public final class EntityContainerProviderImpl implements ContainerProvider {

    @Inject @Named(ServerEnvironment.DEFAULT_INSTANCE_NAME)
    private IterableProvider<EjbContainer> ejbContainerProvider;
    
    private EjbContainer ejbContainerDesc;
    
    private static final Logger _logger = 
    	LogDomains.getLogger(EntityContainerProviderImpl.class, LogDomains.EJB_LOGGER);
    
    @SuppressWarnings("unused")
    @PostConstruct
    private void postConstruct() {
        IterableProvider<EjbContainer> namedProvider = ejbContainerProvider.named(ServerEnvironment.DEFAULT_INSTANCE_NAME);
        if (namedProvider.getSize() > 0) {
            ejbContainerDesc = namedProvider.get();
        }
        else {
            ejbContainerDesc = ejbContainerProvider.get();
        }
    }
    
    public Container getContainer(EjbDescriptor ejbDescriptor,
				     ClassLoader loader) throws Exception {
        Container container = null;

        // instantiate container class
        if (ejbDescriptor instanceof EjbEntityDescriptor) {
            if (((EjbEntityDescriptor)ejbDescriptor).getIASEjbExtraDescriptors()
                    .isIsReadOnlyBean()) { 

                EjbEntityDescriptor robDesc = (EjbEntityDescriptor) ejbDescriptor;                    
                container = new ReadOnlyBeanContainer (ejbDescriptor, loader);
            } else {
                String commitOption = null;
                IASEjbExtraDescriptors iased = ((EjbEntityDescriptor)ejbDescriptor).
                        getIASEjbExtraDescriptors();
                if (iased != null) {
                    commitOption = iased.getCommitOption();    	
                }
                if (commitOption == null) {
                    commitOption = ejbContainerDesc.getCommitOption();  
                }
                if (commitOption.equals("A")) {
                    _logger.log(Level.WARNING, 
                            "entitybean.container.commit_option_A_not_supported",
                            new Object []{ejbDescriptor.getName()}
                            );
                    container = new EntityContainer(ejbDescriptor, loader);
                } else if (commitOption.equals("C")) {
                    _logger.log(Level.FINE, "Using commit option C for: " 
                            + ejbDescriptor.getName());
                    container = new CommitCEntityContainer(ejbDescriptor, loader);
                } else {
                    _logger.log(Level.FINE,"Using commit option B for: " + 
                            ejbDescriptor.getName());
                    container = new EntityContainer(ejbDescriptor, loader);
                }
            }
        }

        return container;
    }
} 
