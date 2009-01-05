/*
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

package org.jboss.webbeans.bean.proxy;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;

import javassist.util.proxy.ProxyFactory;

import javax.webbeans.DefinitionException;
import javax.webbeans.manager.Bean;

import org.jboss.webbeans.CurrentManager;
import org.jboss.webbeans.util.ConcurrentCache;
import org.jboss.webbeans.util.Proxies;

/**
 * A proxy pool for holding scope adaptors (client proxies)
 * 
 * @author Nicklas Karlsson
 * 
 * @see org.jboss.webbeans.bean.proxy.ProxyMethodHandler
 */
public class ProxyPool implements Serializable
{
   
   
   /**
    * A container/cache for previously created proxies
    * 
    * @author Nicklas Karlsson
    */
   private ConcurrentCache<Bean<? extends Object>, Object> pool;

   /**
    * Constructor
    */
   public ProxyPool()
   {
      this.pool = new ConcurrentCache<Bean<? extends Object>, Object>();
   }

   /**
    * Creates a Javassist scope adaptor (client proxy) for a bean
    * 
    * Creates a Javassist proxy factory. Gets the type info. Sets the interfaces
    * and superclass to the factory. Hooks in the MethodHandler and creates the
    * proxy.
    * 
    * @param bean The bean to proxy
    * @param beanIndex The index to the bean in the manager bean list
    * @return A Javassist proxy
    * @throws InstantiationException When the proxy couldn't be created
    * @throws IllegalAccessException When the proxy couldn't be created
    */
   @SuppressWarnings("unchecked")
   private static <T> T createClientProxy(Bean<T> bean, int beanIndex) throws RuntimeException
   {
      
      try
      {
         SimpleBeanProxyMethodHandler proxyMethodHandler = new SimpleBeanProxyMethodHandler(bean, beanIndex);
         Set<Class<?>> classes = new HashSet<Class<?>>(bean.getTypes());
         classes.add(Serializable.class);
         ProxyFactory proxyFactory = Proxies.getProxyFactory(classes);
         proxyFactory.setHandler(proxyMethodHandler);
         Class<?> clazz = proxyFactory.createClass();
         return (T) clazz.newInstance();
      }
      catch (InstantiationException e)
      {
         throw new RuntimeException("Could not instantiate client proxy for " + bean, e);
      }
      catch (IllegalAccessException e)
      {
         throw new RuntimeException("Could not access bean correctly when creating client proxy for " + bean, e);
      }
   }

   /**
    * Gets a client proxy for a bean
    * 
    * Looks for a proxy in the pool. If not found, one is created and added to
    * the pool if the create argument is true.
    * 
    * @param bean The bean to get a proxy to
    * @param create Flag indicating if the proxy should be created if it does
    *           not already exist
    * @return the client proxy for the bean
    */
   @SuppressWarnings("unchecked")
   public <T> T getClientProxy(final Bean<T> bean, boolean create)
   {
      if (create)
      {
         return pool.putIfAbsent(bean, new Callable<T>()
         {

            public T call() throws Exception
            {
               int beanIndex = CurrentManager.rootManager().getBeans().indexOf(bean);
               if (beanIndex < 0)
               {
                  throw new DefinitionException(bean + " is not known to the manager");
               }
               return createClientProxy(bean, beanIndex);
            }

         });
      }
      else
      {
         return (T)pool.getValue(bean);
      }
   }

   /**
    * Gets a string representation
    * 
    * @return The string representation
    */
   @Override
   public String toString()
   {
      return "Proxy pool with " + pool.size() + " proxies";
   }

}
