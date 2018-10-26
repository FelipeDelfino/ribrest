package br.com.andrewribeiro.ribrest.services.miner;

import br.com.andrewribeiro.ribrest.core.exceptions.RibrestDefaultException;
import br.com.andrewribeiro.ribrest.services.dtos.FlowContainer;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;
import org.glassfish.jersey.server.ContainerRequest;
import br.com.andrewribeiro.ribrest.core.model.Model;
import java.lang.reflect.ParameterizedType;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import br.com.andrewribeiro.ribrest.core.annotations.RibrestWontFill;
import br.com.andrewribeiro.ribrest.core.exceptions.RibrestDefaultExceptionConstants;
import br.com.andrewribeiro.ribrest.core.exceptions.RibrestDefaultExceptionFactory;
import br.com.andrewribeiro.ribrest.utils.RibrestUtils;

/**
 *
 * @author Andrew Ribeiro
 */
public abstract class AbstractMiner implements Miner {

    @Inject
    protected FlowContainer flowContainer;

    protected List accepts;
    private List ignored;

    @Override
    public void extractDataFromRequest(ContainerRequest containerRequest) throws RibrestDefaultException {
        flowContainer.setRequestMaps(getRequestMaps(containerRequest));
    }

    @Override
    public void mineRequest(ContainerRequest containerRequest) {
        flowContainer.setRequestMaps(getRequestMaps(containerRequest));
        Model model = null;
        try {
            model = flowContainer.getModel();

            ModelExplorer modelExplorer = new ModelExplorer(model);
            modelExplorer.fillModelWithData(flowContainer.getRequestMaps());
        } catch (UnsupportedOperationException ex) {
            throw RibrestDefaultExceptionFactory.getRibrestDefaultException(RibrestDefaultExceptionConstants.RESOURCE_DOESNT_IMPLEMENT_ABSTRACT_METHODS, RibrestUtils.getResourceName(model.getClass()));
        } catch (Exception e) {
        }
    }

    @Override
    public List extractIgnoredFields() {
        ignored = ignored != null ? ignored : new ArrayList();
        accepts = accepts != null ? accepts : new ArrayList();

        ignored.removeAll(accepts);

        return new ArrayList(ignored);
    }

    private RequestMaps getRequestMaps(ContainerRequest containerRequest) {
        Form form = containerRequest.readEntity(Form.class);
        MultivaluedMap<String, String> formMap = form.asMap();

        UriInfo u = containerRequest.getUriInfo();
        MultivaluedMap<String, String> queryMap = u.getQueryParameters();

        MultivaluedMap<String, String> pathMap = u.getPathParameters();

        MultivaluedMap<String, String> headerMap = containerRequest.getHeaders();

        accepts = queryMap != null ? queryMap.get("accepts") : new ArrayList();
        accepts = accepts != null ? accepts : new ArrayList();

        Map<String, Integer> limitAndOffset = new QueryMiner().extractLimitAndOffset(queryMap);

        flowContainer.getHolder().getSm().setLimit(limitAndOffset.get("limit"));
        flowContainer.getHolder().getSm().setOffset(limitAndOffset.get("offset"));

        return new RequestMaps(formMap, queryMap, pathMap, headerMap);
    }

    private void fillAttribute(FieldHelper fieldHelper) throws IllegalArgumentException, IllegalAccessException, InstantiationException {
        if (fieldHelper.attribute.getType() == String.class
                || fieldHelper.attribute.getType() == Long.class) {
        } else if (fieldHelper.attribute.isAnnotationPresent(OneToOne.class)) {
            fieldHelper.fillEntityAttribute();
        } else if (fieldHelper.attribute.isAnnotationPresent(OneToMany.class)) {
            fieldHelper.fillManyEntityAttribute();
        } else if (fieldHelper.attribute.isAnnotationPresent(ManyToOne.class) && !fieldHelper.attribute.isAnnotationPresent(RibrestWontFill.class)) {
            fieldHelper.fillEntityAttribute();
        }
    }

    private void fillChildModel(Model childModel, String parentAttributeName) throws IllegalArgumentException, IllegalAccessException, InstantiationException {
        for (Field attribute : childModel.getAllModelOneToOneNotMappedByAttributes()) {
            fillAttribute(new FieldHelper(attribute, childModel, parentAttributeName + "." + attribute.getName()));
        }
    }

    class FieldHelper {

        public FieldHelper(Field attribute, Model model, String parameterName) {
            this.attribute = attribute;
            this.model = model;
            this.parameterName = parameterName;
        }

        Field attribute;
        Model model;
        String parameterName;
        Object parameterValue;

        void fillEntityAttribute() throws InstantiationException, IllegalAccessException {
            parameterValue = attribute.getType().newInstance();
            fillChildModel((Model) parameterValue, attribute.getName());
            fill();
        }

        void fillManyEntityAttribute() throws InstantiationException, IllegalAccessException {
            Collection collection = getCollectionInstance();
            Class collectionType = extractCollectionTypedClass();
            getChildrenIds().forEach(stringId -> {
                try {
                    Model model = (Model) collectionType.newInstance();
                    model.setId(Long.parseLong(stringId));
                    fillInverseAttributeInRelationship(model);
                    collection.add(model);
                } catch (Exception e) {
                    e.toString();
                }
            });
            parameterValue = collection;
            fill();
        }

        void fillInverseAttributeInRelationship(Model model) {
            model.getAllModelManyToOneAttributes()
                    .forEach(attribute -> {
                        if (attribute.getType().getSimpleName().equals(this.model.getClass().getSimpleName())) {
                            attribute.setAccessible(true);
                            try {
                                attribute.set(model, this.model);
                            } catch (IllegalArgumentException | IllegalAccessException ex) {
                                throw new RuntimeException(ex.toString());
                            }
                        }
                    });
        }

        void fill() throws IllegalArgumentException, IllegalAccessException {
            attribute.setAccessible(true);
            attribute.set(model, parseBeforeSetParameterValue(parameterValue));
        }

        Object parseBeforeSetParameterValue(Object parameterValue) {
            if (attribute.getType() == Long.class && parameterValue != null) {
                parameterValue = Long.parseLong((String) parameterValue);
            }

            return parameterValue;
        }

        private Class extractCollectionTypedClass() {
            Class collectionType = null;
            if (attributeIsACollection()) {
                ParameterizedType type = (ParameterizedType) attribute.getGenericType();
                collectionType = (Class) type.getActualTypeArguments()[0];
            }

            return collectionType;
        }

        private boolean attributeIsACollection() {
            return Collection.class.isAssignableFrom(attribute.getType());
        }

        private boolean attributeIsASet() {
            return Set.class.isAssignableFrom(attribute.getType());
        }

        private boolean attributeIsList() {
            return List.class.isAssignableFrom(attribute.getType());
        }

        private List<String> getChildrenIds() {
            return null;
        }

        private Collection getCollectionInstance() {
            Collection collection;
            if (attributeIsASet()) {
                collection = new HashSet();
            } else if (attributeIsList()) {
                collection = new ArrayList();
            } else {
                collection = null;
            }

            return collection;
        }
    }
}
