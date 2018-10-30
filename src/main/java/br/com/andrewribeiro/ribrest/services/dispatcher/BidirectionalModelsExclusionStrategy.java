package br.com.andrewribeiro.ribrest.services.dispatcher;

import br.com.andrewribeiro.ribrest.core.model.Model;
import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 *
 * @author Andrew Ribeiro
 */
public class BidirectionalModelsExclusionStrategy implements ExclusionStrategy {

    public BidirectionalModelsExclusionStrategy(List<Model> models, List<String> ignoredFields) {
        this.models = models;
        this.ignoredFields = ignoredFields;
    }

    private List<Model> models;
    private List<String> ignoredFields;
    private Set<Model> repetitiveModels = new HashSet<>();

    public void removeCircularReferences() {
        models.forEach(model -> {
            repetitiveModels.clear();
            clearAllModelCircularReferences(model);
        });
    }

    private void clearAllModelCircularReferences(Model model) {
        if (isModelNull(model)) {
            return;
        }
        repetitiveModels.add(model);
        clearDirectModelCircularReferences(model);
        clearCollectionModelCircularReferences(model);
    }

    private void clearDirectModelCircularReferences(Model model) {
        model.getAllModelAttributes().forEach(attribute -> {
            attribute.setAccessible(true);
            try {
                Model modelInstance = (Model) attribute.get(model);
                if (repetitiveModels.contains(modelInstance) || modelInstance == null) {
                    attribute.set(model, null);
                } else {
                    populateRepetiveModels(modelInstance);
                    clearAllModelCircularReferences(modelInstance);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    private void clearCollectionModelCircularReferences(Model model) {
        model.getAllModelOneToManyAttributes().forEach(attribute -> {
            clearAnyCollection(model, attribute);
        });
        model.getAllModelManyToManyAttributes().forEach(attribute -> {
            clearAnyCollection(model, attribute);
        });
        
    }
    
    private void clearAnyCollection(Model model, Field attribute){
        attribute.setAccessible(true);
            try {
                Collection<Model> collectionInstance = (Collection) attribute.get(model);
                if (collectionInstance != null) {
                    collectionInstance.forEach(modelInstance -> {
                        if (repetitiveModels.contains(modelInstance) || modelInstance == null) {
                            try {
                                attribute.set(model, null);
                            } catch (IllegalArgumentException | IllegalAccessException ex) {
                                throw new RuntimeException(ex.getMessage());
                            }
                        } else {
                            populateRepetiveModels(modelInstance);
                            clearAllModelCircularReferences(modelInstance);
                        }
                    });
                }
            } catch (Exception e) {
                throw new RuntimeException(e.getMessage());
            }
    }

    private void populateRepetiveModels(Model model) {
        repetitiveModels.add(model);
    }

    private void addAllModelAttributesToRepetitiveModels(Model model) {
        repetitiveModels.addAll(model.getAllModelAttributes().stream()
                .map((attribute) -> {
                    attribute.setAccessible(true);
                    Model attributeInstance;
                    try {
                        attributeInstance = (Model) attribute.get(model);
                        return attributeInstance;
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }).collect(Collectors.toSet()));
    }

    private void addAllModelCollectionAttributesToRepetitiveModels(Model model) {
        model.getAllModelOneToManyAttributes().stream()
                .forEach(attribute -> {
                    Collection tempCollection = null;
                    if (Collection.class.isAssignableFrom(attribute.getType())) {
                        attribute.setAccessible(true);
                        try {
                            tempCollection = (Collection) attribute.get(model);
                            if (tempCollection != null) {
                                repetitiveModels.addAll((Set) tempCollection.stream().map(modelInstance -> {
                                    return modelInstance;
                                }).collect(Collectors.toSet()));
                            }
                        } catch (Exception e) {
                            throw new RuntimeException();
                        }
                    }
                });
    }

    private boolean isModelNull(Model model) {
        return model == null;
    }

    @Override
    public boolean shouldSkipField(FieldAttributes fa) {
        return ignoredFields.contains(fa.getName());
    }

    @Override
    public boolean shouldSkipClass(Class<?> type) {
        return false;
    }

}
