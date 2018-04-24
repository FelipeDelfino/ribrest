package br.com.andrewribeiro.ribrest.services.miner;

import br.com.andrewribeiro.ribrest.exceptions.RibrestDefaultException;
import br.com.andrewribeiro.ribrest.exceptions.RibrestDefaultExceptionConstants;
import br.com.andrewribeiro.ribrest.exceptions.RibrestDefaultExceptionFactory;
import br.com.andrewribeiro.ribrest.model.IModel;
import br.com.andrewribeiro.ribrest.utils.RibrestUtils;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.glassfish.jersey.server.ContainerRequest;

/**
 *
 * @author Andrew Ribeiro
 */
public class ConcreteMiner extends AbstractMiner {

    @Override
    public void extract(ContainerRequest cr) throws RibrestDefaultException {
        try {
            super.extract(cr);
            IModel m = (IModel) fc.getModel();
            fill(m);            
        } catch (ClassCastException cce) {
            throw RibrestDefaultExceptionFactory.getRibrestDefaultException(RibrestDefaultExceptionConstants.RESOURCE_IS_NOT_IMODEL_SUBCLASS, RibrestUtils.getResourceName(fc.getModel().getClass()));
        } catch (IllegalArgumentException ex) {
            Logger.getLogger(ConcreteMiner.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            Logger.getLogger(ConcreteMiner.class.getName()).log(Level.SEVERE, null, ex);
        } catch(UnsupportedOperationException uoe){
            throw RibrestDefaultExceptionFactory.getRibrestDefaultException(RibrestDefaultExceptionConstants.RESOURCE_DOESNT_IMPLEMENTS_ABSTRACT_METHODS, RibrestUtils.getResourceName(fc.getModel().getClass()));
        } catch(RibrestDefaultException rde){
            throw rde;
        }
    }

}
