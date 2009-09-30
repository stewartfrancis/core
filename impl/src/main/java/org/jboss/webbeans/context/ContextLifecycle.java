/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2009 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.webbeans.context;

import org.jboss.webbeans.Container;
import org.jboss.webbeans.bootstrap.api.Lifecycle;
import org.jboss.webbeans.bootstrap.api.Service;
import org.jboss.webbeans.context.api.BeanStore;
import org.jboss.webbeans.context.api.helpers.ConcurrentHashMapBeanStore;
import org.jboss.webbeans.log.LogProvider;
import org.jboss.webbeans.log.Logging;

/**
 * An implementation of the Web Beans lifecycle that supports restoring
 * and destroying all the built in contexts
 * 
 * @author Pete Muir
 * 
 */
public class ContextLifecycle implements Lifecycle, Service
{
   
   public static LogProvider log = Logging.getLogProvider(ContextLifecycle.class);

   private final AbstractApplicationContext applicationContext;
   private final AbstractApplicationContext singletonContext;
   private final SessionContext sessionContext;
   private final ConversationContext conversationContext;
   private final RequestContext requestContext;
   private final DependentContext dependentContext;
   
   public ContextLifecycle(AbstractApplicationContext applicationContext, AbstractApplicationContext singletonContext, SessionContext sessionContext, ConversationContext conversationContext, RequestContext requestContext, DependentContext dependentContext)
   {
      this.applicationContext = applicationContext;
      this.singletonContext = singletonContext;
      this.sessionContext = sessionContext;
      this.conversationContext = conversationContext;
      this.requestContext = requestContext;
      this.dependentContext = dependentContext;
   }

   public void restoreSession(String id, BeanStore sessionBeanStore)
   {
      log.trace("Restoring session " + id);
      sessionContext.setBeanStore(sessionBeanStore);
      sessionContext.setActive(true);
   }

   public void endSession(String id, BeanStore sessionBeanStore)
   {
      log.trace("Ending session " + id);
      sessionContext.setActive(true);
      sessionContext.destroy();
      sessionContext.setBeanStore(null);
      sessionContext.setActive(false);
   }

   public void beginRequest(String id, BeanStore requestBeanStore)
   {
      log.trace("Starting request " + id);
      requestContext.setBeanStore(requestBeanStore);
      requestContext.setActive(true);
      dependentContext.setActive(true);
   }

   public void endRequest(String id, BeanStore requestBeanStore)
   {
      log.trace("Ending request " + id);
      requestContext.setBeanStore(requestBeanStore);
      dependentContext.setActive(false);
      requestContext.destroy();
      requestContext.setActive(false);
      requestContext.setBeanStore(null);
   }
   
   public boolean isRequestActive()
   {
      return singletonContext.isActive() && applicationContext.isActive() && requestContext.isActive() && dependentContext.isActive();
   }
   
   public boolean isApplicationActive()
   {
      return singletonContext.isActive() && applicationContext.isActive() && dependentContext.isActive();
   }
   
   public boolean isConversationActive()
   {
      return singletonContext.isActive() && applicationContext.isActive() && sessionContext.isActive() && conversationContext.isActive() && dependentContext.isActive();
   }
   
   public boolean isSessionActive() 
   {
      return singletonContext.isActive() && applicationContext.isActive() && sessionContext.isActive() && dependentContext.isActive();
   }
   

   public void beginApplication(BeanStore applicationBeanStore)
   {
      log.trace("Starting application");
      applicationContext.setBeanStore(applicationBeanStore);
      applicationContext.setActive(true);
      singletonContext.setBeanStore(new ConcurrentHashMapBeanStore());
      singletonContext.setActive(true);
   }
   
   public void endApplication()
   {
      log.trace("Ending application");
      applicationContext.destroy();
      applicationContext.setActive(false);
      applicationContext.setBeanStore(null);
      singletonContext.destroy();
      singletonContext.setActive(false);
      singletonContext.setBeanStore(null);
      Container.instance().cleanup();
   }
   
   public void cleanup() 
   {
      dependentContext.cleanup();
      requestContext.cleanup();
      conversationContext.cleanup();
      sessionContext.cleanup();
      singletonContext.cleanup();
      applicationContext.cleanup();
   }
   
   public AbstractApplicationContext getApplicationContext()
   {
      return applicationContext;
   }
   
   public AbstractApplicationContext getSingletonContext()
   {
      return singletonContext;
   }
   
   public SessionContext getSessionContext()
   {
      return sessionContext;
   }
   
   public ConversationContext getConversationContext()
   {
      return conversationContext;
   }
   
   public RequestContext getRequestContext()
   {
      return requestContext;
   }
   
   public DependentContext getDependentContext()
   {
      return dependentContext;
   }

}
