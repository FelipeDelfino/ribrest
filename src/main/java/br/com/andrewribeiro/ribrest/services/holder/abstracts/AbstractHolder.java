package br.com.andrewribeiro.ribrest.services.holder.abstracts;

import br.com.andrewribeiro.ribrest.services.SearchModel;
import java.util.ArrayList;
import java.util.List;
import br.com.andrewribeiro.ribrest.services.holder.interfaces.Holder;

/**
 *
 * @author Andrew Ribeiro
 */
public abstract class AbstractHolder implements Holder {

    public AbstractHolder() {

        models = new ArrayList();
        sm = new SearchModel(5, 0);
    }

    private List models;
    private SearchModel sm;
    private Long total;

    @Override
    public List getModels() {
        return models;
    }

    @Override
    public void setModels(List models) {
        this.models = models;
    }
    
    @Override
    public SearchModel getSm() {
        return sm;
    }

    @Override
    public void setSm(SearchModel sm) {
        this.sm = sm;
    }
    
    @Override
    public Long getTotal() {
        return total;
    }

    @Override
    public void setTotal(Long total) {
        this.total = total;
    }
    
}
