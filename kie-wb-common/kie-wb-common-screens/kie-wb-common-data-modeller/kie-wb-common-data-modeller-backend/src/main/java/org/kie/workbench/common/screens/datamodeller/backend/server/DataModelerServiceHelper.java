/**
 * Copyright 2012 JBoss Inc
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

package org.kie.workbench.common.screens.datamodeller.backend.server;

import org.kie.workbench.common.screens.datamodeller.model.AnnotationDefinitionTO;
import org.kie.workbench.common.screens.datamodeller.model.AnnotationMemberDefinitionTO;
import org.kie.workbench.common.screens.datamodeller.model.AnnotationTO;
import org.kie.workbench.common.screens.datamodeller.model.DataModelTO;
import org.kie.workbench.common.screens.datamodeller.model.DataObjectTO;
import org.kie.workbench.common.screens.datamodeller.model.ObjectPropertyTO;
import org.kie.workbench.common.services.datamodeller.core.*;
import org.kie.workbench.common.services.datamodeller.core.impl.*;

import java.util.ArrayList;
import java.util.List;

public class DataModelerServiceHelper {

    public static DataModelerServiceHelper getInstance() {
        return new DataModelerServiceHelper();
    }

    public DataModel to2Domain(DataModelTO dataModelTO) {
        DataModel dataModel = ModelFactoryImpl.getInstance().newModel();
        List<DataObjectTO> dataObjects = dataModelTO.getDataObjects();
        DataObject dataObject;

        if (dataObjects != null) {
            for (DataObjectTO dataObjectTO  : dataObjects) {
                dataObject = dataModel.addDataObject(dataObjectTO.getPackageName(), dataObjectTO.getName());
                to2Domain(dataObjectTO, dataObject);
            }
        }
        return dataModel;
    }

    public DataModelTO domain2To(DataModel dataModel, Integer status) {
        DataModelTO dataModelTO = new DataModelTO();
        List<DataObject> dataObjects = new ArrayList<DataObject>();
        List<DataObject> externalDataObjects = new ArrayList<DataObject>();
        List<String> externalClasses = new ArrayList<String>();

        dataObjects.addAll(dataModel.getDataObjects());
        externalDataObjects.addAll(dataModel.getDataObjects(ObjectSource.DEPENDENCY));

        DataObjectTO dataObjectTO;

        if (dataObjects != null) {
            for (DataObject dataObject  : dataObjects) {
                dataObjectTO = new DataObjectTO(dataObject.getName(), dataObject.getPackageName(), dataObject.getSuperClassName());
                if (status != null) {
                    dataObjectTO.setStatus(status);
                }
                domain2To(dataObject, dataObjectTO);
                dataModelTO.getDataObjects().add(dataObjectTO);
            }
        }

        for (DataObject externalDataObject : externalDataObjects) {
            if (!externalClasses.contains(externalDataObject.getClassName())) {
                externalClasses.add(externalDataObject.getClassName());
            }
        }
        dataModelTO.setExternalClasses(externalClasses);

        return dataModelTO;
    }

    public void domain2To(DataObject dataObject, DataObjectTO dataObjectTO) {
        dataObjectTO.setName(dataObject.getName());
        dataObjectTO.setOriginalClassName(dataObject.getClassName());
        dataObjectTO.setSuperClassName(dataObject.getSuperClassName());
        List<ObjectProperty> properties = new ArrayList<ObjectProperty>();
        properties.addAll(dataObject.getProperties().values());

        List<ObjectPropertyTO> propertiesTO = new ArrayList<ObjectPropertyTO>();
        PropertyTypeFactory typeFactory = PropertyTypeFactoryImpl.getInstance();

        //process type level annotations
        for (Annotation annotation : dataObject.getAnnotations()) {
            AnnotationTO annotationTO = domain2To(annotation);
            if (annotationTO != null) {
                dataObjectTO.addAnnotation(annotationTO);
            }
        }

        ObjectPropertyTO propertyTO;
        for (ObjectProperty property : properties) {
            propertyTO = new ObjectPropertyTO(property.getName(), property.getClassName(), property.isMultiple(), typeFactory.isBasePropertyType(property.getClassName()), property.getBag());
            propertiesTO.add(propertyTO);
            //process member level annotations.
            for (Annotation annotation : property.getAnnotations()) {
                AnnotationTO annotationTO = domain2To(annotation);
                if (annotationTO != null) {
                    propertyTO.addAnnotation(annotationTO);
                }
            }
        }

        dataObjectTO.setProperties(propertiesTO);
    }

    public void to2Domain(DataObjectTO dataObjectTO, DataObject dataObject) {
        dataObject.setName(dataObjectTO.getName());
        List<ObjectPropertyTO> properties = dataObjectTO.getProperties();
        dataObject.setSuperClassName(dataObjectTO.getSuperClassName());

        //process type level annotations.
        for (AnnotationTO annotationTO : dataObjectTO.getAnnotations()) {
            Annotation annotation = to2Domain(annotationTO);
            if (annotation != null) {
                dataObject.addAnnotation(annotation);
            }
        }

        if (properties != null) {
            ObjectProperty property;
            for (ObjectPropertyTO propertyTO : properties) {
                property = dataObject.addProperty(propertyTO.getName(), propertyTO.getClassName(), propertyTO.isMultiple(), propertyTO.getBag());
                //process member level annotations.
                for (AnnotationTO annotationTO : propertyTO.getAnnotations()) {
                    Annotation annotation = to2Domain(annotationTO);
                    if (annotation != null) {
                        property.addAnnotation(annotation);
                    }
                }
            }
        }
    }

    public Annotation to2Domain(AnnotationTO annotationTO) {
        AnnotationDefinition annotationDefinition = to2Domain(annotationTO.getAnnotationDefinition());
        Annotation annotation = new AnnotationImpl(annotationDefinition);
        Object memberValue;
        for (AnnotationMemberDefinition memberDefinition : annotationDefinition.getAnnotationMembers()) {
            memberValue = annotationTO.getValue(memberDefinition.getName());
            if (memberValue != null) {
                annotation.setValue(memberDefinition.getName(), memberValue);
            }
        }
        return annotation;
    }

    public AnnotationDefinition to2Domain(AnnotationDefinitionTO annotationDefinitionTO) {
        AnnotationDefinitionImpl annotationDefinition = new AnnotationDefinitionImpl(annotationDefinitionTO.getName(), annotationDefinitionTO.getClassName(), annotationDefinitionTO.getShortDescription(), annotationDefinitionTO.getDescription(), annotationDefinitionTO.isObjectAnnotation(), annotationDefinitionTO.isPropertyAnnotation());
        AnnotationMemberDefinition memberDefinition;
        for (AnnotationMemberDefinitionTO memberDefinitionTO : annotationDefinitionTO.getAnnotationMembers()) {
            memberDefinition = new AnnotationMemberDefinitionImpl(memberDefinitionTO.getName(), memberDefinitionTO.getClassName(), memberDefinitionTO.isEnum(), memberDefinitionTO.getDefaultValue(), memberDefinitionTO.getShortDescription(), memberDefinitionTO.getDescription());
            annotationDefinition.addMember(memberDefinition);
        }
        return annotationDefinition;
    }

    public AnnotationTO domain2To(Annotation annotation) {
        AnnotationDefinitionTO annotationDefinitionTO = domain2To(annotation.getAnnotationDefinition());
        AnnotationTO annotationTO = new AnnotationTO(annotationDefinitionTO);
        Object memberValue;
        for (AnnotationMemberDefinitionTO memberDefinitionTO : annotationDefinitionTO.getAnnotationMembers()) {
            memberValue = annotation.getValue(memberDefinitionTO.getName());
            if (memberValue != null) {
                annotationTO.setValue(memberDefinitionTO.getName(), memberValue);
            }
        }
        return annotationTO;
    }

    public AnnotationDefinitionTO domain2To(AnnotationDefinition annotationDefinition) {

        AnnotationDefinitionTO annotationDefinitionTO = new AnnotationDefinitionTO(annotationDefinition.getName(), annotationDefinition.getClassName(), annotationDefinition.getShortDescription(), annotationDefinition.getDescription(), annotationDefinition.isObjectAnnotation(), annotationDefinition.isPropertyAnnotation());
        AnnotationMemberDefinitionTO memberDefinitionTO;
        for (AnnotationMemberDefinition memberDefinition : annotationDefinition.getAnnotationMembers()) {
            memberDefinitionTO = new AnnotationMemberDefinitionTO(memberDefinition.getName(), memberDefinition.getClassName(), memberDefinition.isPrimitiveType(), memberDefinition.isEnum(), memberDefinition.defaultValue(), memberDefinition.getShortDescription(), memberDefinition.getDescription());
            annotationDefinitionTO.addMember(memberDefinitionTO);
        };

        return annotationDefinitionTO;
    }
}
