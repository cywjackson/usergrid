/*******************************************************************************
 * Copyright 2012 Apigee Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.usergrid.rest;


import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import org.apache.commons.lang.text.StrSubstitutor;

import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.springframework.web.context.support.WebApplicationContextUtils.getRequiredWebApplicationContext;
import static org.usergrid.rest.utils.CORSUtils.allowAllOrigins;
import static org.usergrid.utils.StringUtils.readClasspathFileAsString;


public class SwaggerServlet extends HttpServlet implements Filter {

    public static final String SWAGGER_BASE_PATH = "http://localhost:8080";

    public static final Logger logger = LoggerFactory.getLogger( SwaggerServlet.class );

    private static final long serialVersionUID = 1L;

    ServletContext sc;
    Properties properties;


    @Override
    public void init( ServletConfig config ) throws ServletException {
        super.init( config );
        logger.info( "init(ServletConfig config)" );
        if ( sc == null ) {
            sc = config.getServletContext();
        }
        properties = ( Properties ) getSpringBeanFromWeb( "properties" );
        loadSwagger();
    }


    @Override
    public void init( FilterConfig config ) throws ServletException {
        logger.info( "init(FilterConfig paramFilterConfig)" );
        if ( sc == null ) {
            sc = config.getServletContext();
        }
        properties = ( Properties ) getSpringBeanFromWeb( "properties" );
        loadSwagger();
    }


    public Object getSpringBeanFromWeb( String beanName ) {
        if ( sc == null ) {
            return null;
        }
        ApplicationContext appContext = getRequiredWebApplicationContext( sc );
        return appContext.getBean( beanName );
    }


    Map<String, String> pathToJson = new HashMap<String, String>();


    public String loadTempate( String template ) {
        String templateString = readClasspathFileAsString( template );
        Map<String, String> valuesMap = new HashMap<String, String>();
        String basePath = properties != null ? properties.getProperty( "swagger.basepath", SWAGGER_BASE_PATH ) :
                          SWAGGER_BASE_PATH;
        valuesMap.put( "basePath", basePath );
        StrSubstitutor sub = new StrSubstitutor( valuesMap );
        return sub.replace( templateString );
    }


    public void loadSwagger() {
        logger.info( "loadSwagger()" );
        pathToJson.put( "/resources.json", loadTempate( "/swagger/resources.json" ) );
        pathToJson.put( "/applications.json", loadTempate( "/swagger/applications.json" ) );
        pathToJson.put( "/management.json", loadTempate( "/swagger/management.json" ) );
    }


    @Override
    protected void doGet( HttpServletRequest request, HttpServletResponse response )
            throws ServletException, IOException {

        String path = request.getServletPath();

        logger.info( "Swagger request: " + path );

        handleJsonOutput( request, response );
    }


    @Override
    public void doFilter( ServletRequest request, ServletResponse response, FilterChain chain )
            throws IOException, ServletException {
        try {
            doFilter( ( HttpServletRequest ) request, ( HttpServletResponse ) response, chain );
        }
        catch ( ClassCastException e ) {
            throw new ServletException( "non-HTTP request or response" );
        }
    }


    public void doFilter( HttpServletRequest request, HttpServletResponse response, FilterChain chain )
            throws IOException, ServletException {

        if ( handleJsonOutput( request, response ) ) {
            return;
        }

        chain.doFilter( request, response );
    }


    public boolean handleJsonOutput( HttpServletRequest request, HttpServletResponse response ) throws IOException {
        String path = request.getServletPath();
        if ( isEmpty( path ) ) {
            path = request.getPathInfo();
        }
        if ( isEmpty( path ) ) {
            return false;
        }
        path = path.toLowerCase();
        if ( pathToJson.containsKey( path ) ) {
            String json = pathToJson.get( path );
            if ( json != null ) {
                allowAllOrigins( request, response );
                if ( "get".equalsIgnoreCase( request.getMethod() ) ) {
                    response.setContentType( "application/json" );
                    response.getWriter().print( json );
                    return true;
                }
            }
        }
        return false;
    }
}
