
/*
* Copyright 2011 JBoss Inc
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


package org.kie.workbench.common.services.rest;

import static org.uberfire.backend.vfs.PathFactory.newPath;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.drools.workbench.screens.testscenario.service.ScenarioTestEditorService;
import org.jboss.errai.ioc.client.api.Caller;
import org.jboss.resteasy.annotations.GZIP;
import org.kie.workbench.common.services.project.service.ProjectService;
import org.kie.workbench.common.services.project.service.model.POM;
import org.kie.workbench.common.services.rest.domain.BuildConfig;
import org.kie.workbench.common.services.rest.domain.Entity;
import org.kie.workbench.common.services.rest.domain.Group;
import org.kie.workbench.common.services.rest.domain.Repository;
import org.kie.workbench.common.services.rest.domain.Result;
import org.kie.workbench.common.services.shared.builder.BuildService;


import org.kie.commons.io.IOService;
import org.kie.commons.java.nio.file.FileSystem;
import org.uberfire.backend.FileExplorerRootService;
import org.uberfire.backend.Root;
import org.uberfire.backend.group.GroupService;
import org.uberfire.backend.repositories.RepositoryService;
import org.uberfire.backend.server.util.Paths;
//import org.uberfire.backend.vfs.Path;
import org.uberfire.backend.vfs.PathFactory;
import org.uberfire.backend.vfs.VFSService;
import org.uberfire.shared.mvp.impl.PathPlaceRequest;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import javax.ws.rs.Path;


@Path("/")
@RequestScoped
@Named
@GZIP
//@ApplicationScoped
public class ProjectResource {
   private HttpHeaders headers;

   @Context
   protected UriInfo uriInfo;

   @Inject
   protected ProjectService projectService;

   @Inject
   protected BuildService buildService;

   @Inject
   protected ScenarioTestEditorService scenarioTestEditorService;

   @Inject
   private Paths paths;

   @Inject
   @Named("ioStrategy")
   private IOService ioSystemService;

   @Inject
   GroupService groupService;

   @Inject
   RepositoryService repositoryService;

   @Inject
   VFSService vfsService;

   @Inject
   private FileExplorerRootService rootService;

   @Inject
   Event<Root> event;

   @Context
   public void setHttpHeaders(HttpHeaders theHeaders) {
       headers = theHeaders;
   }

   @POST
   @Consumes(MediaType.APPLICATION_JSON)
   @Produces(MediaType.APPLICATION_JSON)
   @Path("repositories")
   public Repository createOrCloneRepository(Repository repository) {
       System.out.println("-----createOrCloneRepository--- , repository name:" + repository.getName());

       if (repository.getRequestType() == null
               || "".equals(repository.getRequestType())
               || !("new".equals(repository.getRequestType()) || ("clone".equals(repository.getRequestType())))) {
           throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity("Repository request type can only be new or clone.").build());
       }

       final String scheme = "git";

       if("new".equals(repository.getRequestType())) {
           if (repository.getName() == null || "".equals(repository.getName())) {
               throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity("Repository name must be provided").build());
           }

           //username and password are optional
           final String alias = repository.getName();
           final Map<String, Object> env = new HashMap<String, Object>( 3 );
           env.put( "username", repository.getUserName() );
           env.put( "password", repository.getPassword() );
           env.put( "init", true );
           final String uri = scheme + "://" + alias;

           org.uberfire.backend.vfs.FileSystem v = vfsService.newFileSystem( uri, env );
           final org.uberfire.backend.vfs.Path rootPath = newPath( v, alias, uri );
           final Root newRoot = new Root( rootPath, new PathPlaceRequest( rootPath, "RepositoryEditor" ) );
           rootService.addRoot( newRoot );
           event.fire( newRoot );

           repositoryService.createRepository(scheme, repository.getName(), repository.getUserName(), repository.getPassword());

       } else if("clone".equals(repository.getRequestType())) {
           if (repository.getName() == null || "".equals(repository.getName()) || repository.getGitURL() == null || "".equals(repository.getGitURL())) {
               throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity("Repository name and GitURL must be provided").build());
           }

           final String alias = repository.getName();
           final String origin = repository.getGitURL();
           final Map<String, Object> env = new HashMap<String, Object>( 3 );
           env.put( "username", repository.getUserName() );
           env.put( "password", repository.getPassword() );
           env.put( "origin", origin );
           final String uri = scheme + "://" + alias;

           org.uberfire.backend.vfs.FileSystem v = vfsService.newFileSystem( uri, env );
           final org.uberfire.backend.vfs.Path rootPath = newPath( v, alias, uri );
           final Root newRoot = new Root( rootPath, new PathPlaceRequest( rootPath, "RepositoryEditor" ) );
           rootService.addRoot( newRoot );
           event.fire( newRoot );

           repositoryService.cloneRepository(scheme, repository.getName(), repository.getGitURL(), repository.getUserName(), repository.getPassword());
       }

       return repository;
   }

   @DELETE
   @Produces(MediaType.APPLICATION_JSON)
   @Path("repositories/{repositoryName}")
   public Result deleteRepository(
           @PathParam("repositoryName") String repositoryName) {
       System.out.println("-----deleteRepository--- , repositoryName:" + repositoryName);

       Result result = new Result();
       result.setStatus("SUCCESS");
       return result;
   }

   @POST
   @Consumes(MediaType.APPLICATION_JSON)
   @Produces(MediaType.APPLICATION_JSON)
   @Path("repositories/{repositoryName}/projects")
   public Entity createProject(
           @PathParam("repositoryName") String repositoryName, Entity project) {
       System.out.println("-----createProject--- , repositoryName:" + repositoryName + ", project name:" + project.getName());

       org.kie.commons.java.nio.file.Path repositoryPath = getRepositoryRootPath(repositoryName);

       if (repositoryPath == null) {
           throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND).entity("Repository [" + repositoryName + "] does not exist").build());
       } else {
           POM pom = new POM();
           org.uberfire.backend.vfs.Path vfsPath = projectService.newProject(paths.convert(repositoryPath, false), project.getName(), pom, "/");

           //TODO: handle errors, exceptions.

           return project;
       }
   }

   @DELETE
   @Produces(MediaType.APPLICATION_JSON)
   @Path("repositories/{repositoryName}/projects/{projectName}")
   public Result deleteProject(
           @PathParam("repositoryName") String repositoryName,
           @PathParam("projectName") String projectName) {
       System.out.println("-----deleteProject--- , repositoryName:" + repositoryName + ", project name:" + projectName);

       org.kie.commons.java.nio.file.Path repositoryPath = getRepositoryRootPath(repositoryName);

       if (repositoryPath == null) {
           throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND).entity("Repository [" + repositoryName + "] does not exist").build());
       } else {
           //TODO: Delete project. ProjectService does not have a removeProject method yet.

           Result result = new Result();
           result.setStatus("SUCCESS");
           return result;
       }
   }

   @GET
   @POST
   @Consumes(MediaType.APPLICATION_JSON)
   @Produces(MediaType.APPLICATION_JSON)
@Path("repositories/{repositoryName}/projects/{projectName}/maven/compile")
   public Result compileProject(
           @PathParam("repositoryName") String repositoryName,
           @PathParam("projectName") String projectName, BuildConfig mavenConfig) {
       System.out.println("-----compileProject--- , repositoryName:" + repositoryName + ", project name:" + projectName);

       org.kie.commons.java.nio.file.Path repositoryPath = getRepositoryRootPath(repositoryName);

       if (repositoryPath == null) {
           throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND).entity("Repository [" + repositoryName + "] does not exist").build());
       } else {
           org.uberfire.backend.vfs.Path pathToPomXML = projectService.resolvePathToPom(paths.convert(repositoryPath.resolve(projectName), false));

           if (pathToPomXML == null) {
               throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND).entity("Project [" + projectName + "] does not exist").build());
           }

           buildService.build(pathToPomXML);

           // TODO: get BuildResults

           Result result = new Result();
           result.setStatus("SUCCESS");
           return result;
       }
   }

   @GET
   @POST
   @Consumes(MediaType.APPLICATION_JSON)
   @Produces(MediaType.APPLICATION_JSON)
@Path("repositories/{repositoryName}/projects/{projectName}/maven/install")
   public Result installProject(
           @PathParam("repositoryName") String repositoryName,
           @PathParam("projectName") String projectName, BuildConfig mavenConfig) {
       System.out.println("-----installProject--- , repositoryName:" + repositoryName + ", project name:" + projectName);

       org.kie.commons.java.nio.file.Path repositoryPath = getRepositoryRootPath(repositoryName);

       if (repositoryPath == null) {
           throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND).entity("Repository [" + repositoryName + "] does not exist").build());
       } else {
           org.uberfire.backend.vfs.Path pathToPomXML = projectService.resolvePathToPom(paths.convert(repositoryPath.resolve(projectName), false));

           if (pathToPomXML == null) {
               throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND).entity("Project [" + projectName + "] does not exist").build());
           }

           buildService.buildAndDeploy(pathToPomXML);

           //TODO: get BuildResults

           Result result = new Result();
           result.setStatus("SUCCESS");
           return result;
       }
   }

   @GET
   @POST
   @Consumes(MediaType.APPLICATION_JSON)
   @Produces(MediaType.APPLICATION_JSON)
@Path("repositories/{repositoryName}/projects/{projectName}/maven/test")
   public Result testProject(
           @PathParam("repositoryName") String repositoryName,
           @PathParam("projectName") String projectName, BuildConfig config) {
       System.out.println("-----testProject--- , repositoryName:" + repositoryName + ", project name:" + projectName);

       org.kie.commons.java.nio.file.Path repositoryPath = getRepositoryRootPath(repositoryName);

       if (repositoryPath == null) {
           throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND).entity("Repository [" + repositoryName + "] does not exist").build());
       } else {
           org.uberfire.backend.vfs.Path pathToPomXML = projectService.resolvePathToPom(paths.convert(repositoryPath.resolve(projectName), false));

           if (pathToPomXML == null) {
               throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND).entity("Project [" + projectName + "] does not exist").build());
           }

           //TODO: Get session from BuildConfig or create a default session for testing if no session is provided.
           scenarioTestEditorService.runAllScenarios(pathToPomXML, "someSession");

           //TODO: Get test result. We need a sync version of runAllScenarios (instead of listening for test result using event listeners).

           Result result = new Result();
           result.setStatus("SUCCESS");
           return result;
       }
   }

   @POST
   @Consumes(MediaType.APPLICATION_JSON)
   @Produces(MediaType.APPLICATION_JSON)
@Path("repositories/{repositoryName}/projects/{projectName}/maven/deploy")
   public Result deployProject(
           @PathParam("repositoryName") String repositoryName,
           @PathParam("projectName") String projectName, BuildConfig config) {
       System.out.println("-----deployProject--- , repositoryName:" + repositoryName + ", project name:" + projectName);

       org.kie.commons.java.nio.file.Path repositoryPath = getRepositoryRootPath(repositoryName);

       if (repositoryPath == null) {
           throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND).entity("Repository [" + repositoryName + "] does not exist").build());
       } else {
           org.uberfire.backend.vfs.Path pathToPomXML = projectService.resolvePathToPom(paths.convert(repositoryPath.resolve(projectName), false));

           if (pathToPomXML == null) {
               throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND).entity("Project [" + projectName + "] does not exist").build());
           }

           buildService.buildAndDeploy(pathToPomXML);

           //TODO: get BuildResults

           Result result = new Result();
           result.setStatus("SUCCESS");
           return result;
       }
   }

   @POST
   @Consumes(MediaType.APPLICATION_JSON)
   @Produces(MediaType.APPLICATION_JSON)
   @Path("/groups")
   public Group createGroup(Group group) {
       System.out.println("-----createGroup--- , Group name:" + group.getName() + ", Group owner:" + group.getOwner());

       if (group.getName() == null || group.getOwner() == null) {
           throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity("Group name and owner must be provided").build());
       }

       groupService.createGroup(group.getName(), group.getOwner());

       return group;
   }

   @DELETE
   @Produces(MediaType.APPLICATION_JSON)
   @Path("/groups/{groupName}")
   public Result deleteGroup(@PathParam("groupName") String groupName) {
       System.out.println("-----deleteGroup--- , Group name:" + groupName);

       //TODO:GroupService does not have removeGroup method yet
       //groupService.removeGroup(groupName);
       Result result = new Result();
       result.setStatus("SUCCESS");
       return result;
   }

   public org.kie.commons.java.nio.file.Path getRepositoryRootPath(String repositoryName) {
       org.kie.commons.java.nio.file.Path repositoryRootPath = null;

       final Iterator<FileSystem> fsIterator = ioSystemService.getFileSystems().iterator();

       if ( fsIterator.hasNext() ) {
           final FileSystem fileSystem = fsIterator.next();
           System.out.println("-----FileSystem id--- :" + ((org.kie.commons.java.nio.base.FileSystemId) fileSystem).id());

           if (repositoryName.equalsIgnoreCase(((org.kie.commons.java.nio.base.FileSystemId) fileSystem).id())) {
                final Iterator<org.kie.commons.java.nio.file.Path> rootIterator = fileSystem.getRootDirectories().iterator();
                if (rootIterator.hasNext()) {
                    repositoryRootPath = rootIterator.next();
                    System.out.println("-----rootPath--- :" + repositoryRootPath);

org.kie.commons.java.nio.file.DirectoryStream<org.kie.commons.java.nio.file.Path> paths = ioSystemService
                            .newDirectoryStream(repositoryRootPath);
                    for (final org.kie.commons.java.nio.file.Path child : paths) {
                        System.out.println("-----child--- :" + child);
                    }

                    return repositoryRootPath;
                }
            }
       }

       return repositoryRootPath;
   }
}





