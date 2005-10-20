/*
 * Carrot2 Project
 * Copyright (C) 2002-2005, Dawid Weiss
 * Portions (C) Contributors listed in carrot2.CONTRIBUTORS file.
 * All rights reserved.
 *
 * Refer to the full license file "carrot2.LICENSE"
 * in the root folder of the CVS checkout or at:
 * http://www.cs.put.poznan.pl/dweiss/carrot2.LICENSE
 */
package com.dawidweiss.carrot.core.local;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.pool.BasePoolableObjectFactory;
import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.PoolableObjectFactory;
import org.apache.commons.pool.impl.GenericObjectPool;

/**
 * A complete base implementation of the
 * {@link com.dawidweiss.carrot.core.local.LocalController}interface. Also
 * implements the {@link LocalControllerContext}interface.
 *
 * @author Stanislaw Osinski
 * @author Dawid Weiss
 * 
 * @version $Revision$
 */
public class LocalControllerBase implements LocalController,
    LocalControllerContext
{
    /** Stores local component pools */
    protected Map componentPools;

    /** Stores local processes */
    protected Map processes;

    /**
     * Default implementation of the {@link ProcessingResult} interface.
     * 
     * @author stachoo
     */
    protected static class Result implements ProcessingResult
    {
        /** Query result */
        private Object result;

        /** Request context for this query */
        private RequestContext requestContext;

        /**
         * @param queryResult
         * @param requestContext
         */
        public Result(Object queryResult, RequestContext requestContext)
        {
            this.result = queryResult;
            this.requestContext = requestContext;
        }

        public Object getQueryResult()
        {
            return result;
        }

        public RequestContext getRequestContext()
        {
            return requestContext;
        }
    }    
    
    /**
     * Creates a new instance of the controller.
     */
    public LocalControllerBase()
    {
        componentPools = new HashMap();
        processes = new LinkedHashMap();
    }

    public void addLocalComponentFactory(String componentId,
        final LocalComponentFactory factory)
    {
        if (componentPools.containsKey(componentId)) {
            throw new DuplicatedKeyException("Component factory with " +
                    "this id already exists: " + componentId);
        }
        
        final PoolableObjectFactory poolableObjectFactory = new BasePoolableObjectFactory()
        {
            public Object makeObject() throws Exception
            {
                LocalComponent component = factory.getInstance();
                component.init(LocalControllerBase.this);
                return component;
            }
        };

        componentPools.put(componentId, new GenericObjectPool(
            poolableObjectFactory, 5));
    }
    
    /**
     * @return Returns an array of names of component factories.
     */
    public final String[] getComponentFactoryNames() {
        final Set keys = this.componentPools.keySet();
        return (String[]) keys.toArray(new String[keys.size()]);
    }

    /**
     * Borrows a component with given <code>componentId</code> from the
     * internal component instance pool.
     * 
     * @param componentId
     * @return component instance
     * @throws MissingComponentException when no factory has been registered
     *             with given <code>componentId</code>
     * @throws Exception when other problems occur
     */
    public LocalComponent borrowComponent(String componentId)
        throws MissingComponentException, Exception
    {
        ObjectPool pool = (ObjectPool) componentPools.get(componentId);
        if (pool == null)
        {
            throw new MissingComponentException("Component missing: "
                + componentId);
        }

        return (LocalComponent) pool.borrowObject();
    }

    /**
     * Returns the <code>component</code> with given <code>componentId</code>
     * to the internal component pool.
     * 
     * @param componentId
     * @param component
     * @throws Exception when problems occur
     */
    public void returnComponent(String componentId, LocalComponent component)
        throws Exception
    {
        ((ObjectPool) componentPools.get(componentId)).returnObject(component);
    }

    public ProcessingResult query(String processId, String query,
        Map requestParameters) throws MissingProcessException, Exception
    {
        // Get the process
        LocalProcess process = (LocalProcess) processes.get(processId);
        if (process == null)
        {
            throw new MissingProcessException("Process missing: " + processId);
        }

        RequestContext requestContext = new RequestContextBase(this,
            requestParameters);

        Object queryResult;
        try
        {
            queryResult = process.query(requestContext, query);
        }
        finally
        {
            requestContext.dispose();
        }

        return new Result(queryResult, requestContext);
    }

    public void addProcess(String processId, LocalProcess localProcess)
        throws Exception
    {
        localProcess.initialize(this);
        processes.put(processId, localProcess);
    }

    public boolean isComponentFactoryAvailable(String key)
    {
        return componentPools.containsKey(key);
    }

    public Class getComponentClass(String componentId)
        throws MissingComponentException, Exception
    {
        ObjectPool pool = (ObjectPool) componentPools.get(componentId);
        if (pool == null)
        {
            throw new MissingComponentException("Component missing: "
                + componentId);
        }

        LocalComponent component = (LocalComponent) pool.borrowObject();
        Class componentClass = component.getClass();
        pool.returnObject(component);

        return componentClass;
    }

    public boolean isComponentSequenceCompatible(String keyComponentFrom,
        String keyComponentTo) throws MissingComponentException, Exception
    {
        LocalComponent from = null;
        LocalComponent to = null;

        try {
            from = this.borrowComponent(keyComponentFrom);
            to = this.borrowComponent(keyComponentTo);

            return CapabilityMatchVerifier.isCompatible(from, to);
        } finally {
            if (from != null) {
                this.returnComponent(keyComponentFrom, from);
            }

            if (to != null) {
                this.returnComponent(keyComponentTo, to);
            }
        }
    }

    public String explainIncompatibility(String keyComponentFrom, String keyComponentTo)
        throws MissingComponentException, Exception
    {
        LocalComponent from = null;
        LocalComponent to = null;

        try {
            from = this.borrowComponent(keyComponentFrom);
            to = this.borrowComponent(keyComponentTo);

            return CapabilityMatchVerifier.explain(from, to);
        } finally {
            if (from != null) {
                this.returnComponent(keyComponentFrom, from);
            }

            if (to != null) {
                this.returnComponent(keyComponentTo, to);
            }
        }
    }
    
    public List getProcessIds()
    {
        return new ArrayList(processes.keySet());
    }

    public String getProcessName(String processId)
        throws MissingProcessException
    {
        LocalProcess process = (LocalProcess) processes.get(processId);
        if (process == null)
        {
            throw new MissingProcessException("No such process: " + processId);
        }

        return process.getName();
    }

    public String getProcessDescription(String processId)
        throws MissingProcessException
    {
        LocalProcess process = (LocalProcess) processes.get(processId);
        if (process == null)
        {
            throw new MissingProcessException("No such process: " + processId);
        }

        return process.getDescription();
    }

    public String getComponentName(String componentId)
        throws MissingComponentException, Exception
    {
        LocalComponent localComponent = borrowComponent(componentId);
        String name = localComponent.getName();
        returnComponent(componentId, localComponent);

        return name;
    }

    public String getComponentDescription(String componentId)
        throws MissingComponentException, Exception
    {
        LocalComponent localComponent = borrowComponent(componentId);
        String description = localComponent.getDescription();
        returnComponent(componentId, localComponent);

        return description;
    }
}