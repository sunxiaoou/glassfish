/*
 * The contents of this file are subject to the terms 
 * of the Common Development and Distribution License 
 * (the License).  You may not use this file except in
 * compliance with the License.
 * 
 * You can obtain a copy of the license at 
 * https://glassfish.dev.java.net/public/CDDLv1.0.html or
 * glassfish/bootstrap/legal/CDDLv1.0.txt.
 * See the License for the specific language governing 
 * permissions and limitations under the License.
 * 
 * When distributing Covered Code, include this CDDL 
 * Header Notice in each file and include the License file 
 * at glassfish/bootstrap/legal/CDDLv1.0.txt.  
 * If applicable, add the following below the CDDL Header, 
 * with the fields enclosed by brackets [] replaced by
 * you own identifying information: 
 * "Portions Copyrighted [year] [name of copyright owner]"
 * 
 * Copyright 2006 Sun Microsystems, Inc. All rights reserved.
 */
package com.sun.enterprise.naming;

import java.rmi.RemoteException;
import java.util.logging.Logger;

/**
 * This class is a facade for the remote and local SerialContextProvider The
 * need for this class arose from the fact that we wanted to have a way of
 * lazily initializing the Remote SerialContextProvider. The TransientContext
 * member field has to be shared across both remote and local
 * SerialContextProvider. It could have been a static variable but to avoid
 * issues of static variables with multiple threads, etc, this class has been
 * designed. The ORB needs to be initialized before the call to
 * initRemoteProvider()
 *
 * @author Sheetal Vartak
 */

public class ProviderManager {

    // This is a singleton.
    private static final ProviderManager providerManager =
            new ProviderManager();

    private TransientContext rootContext = new TransientContext();

    private SerialContextProvider localProvider;

    private boolean initRemoteProviderDone = false;

    static Logger _logger = null;

    public static ProviderManager getProviderManager() {
        return providerManager;
    }

    public TransientContext getTransientContext() {
        return rootContext;
    }

    public synchronized SerialContextProvider getLocalProvider() {
        if (localProvider == null) {
            localProvider = LocalSerialContextProviderImpl
                    .getProvider(rootContext);
        }
        return localProvider;
    }

    public synchronized void initRemoteProvider() throws RemoteException {
        if (initRemoteProviderDone == false) {
            //RemoteSerialContextProviderImpl
            //      .initSerialContextProvider(rootContext);
            initRemoteProviderDone = true;
        }
    }

}
