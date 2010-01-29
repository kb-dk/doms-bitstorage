/*
 * $Id$
 * $Revision$
 * $Date$
 * $Author$
 *
 * The DOMS project.
 * Copyright (C) 2007-2010  The State and University Library
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package dk.statsbiblioteket.doms.webservices;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.util.Enumeration;
import java.util.Properties;

/**
 * This listener retrieves the list of context params from the web.xml
 * and adds them to the ConfigCollection
 * @see dk.statsbiblioteket.doms.webservices.ConfigCollection
 */
public class ConfigContextListener implements ServletContextListener {

    public void contextInitialized(ServletContextEvent sce) {
        Properties props = new Properties();

        ServletContext servletContext = sce.getServletContext();
        Enumeration names = servletContext.getInitParameterNames();
        while (names.hasMoreElements()) {
            String name = (String) names.nextElement();
            String value = servletContext.getInitParameter(name);
            props.put(name,value);
        }
        ConfigCollection.setContextConfig(props);
    }

    public void contextDestroyed(ServletContextEvent sce) {
        Properties props = new Properties();
        ConfigCollection.setContextConfig(props);
     }

}
