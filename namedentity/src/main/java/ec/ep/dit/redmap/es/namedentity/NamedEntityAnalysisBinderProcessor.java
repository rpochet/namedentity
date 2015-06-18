package ec.ep.dit.redmap.es.namedentity;

import org.elasticsearch.index.analysis.AnalysisModule;

import ec.ep.dit.redmap.es.namedentity.analysis.AutoPhrasingTokenFilterFactory;
import ec.ep.dit.redmap.es.namedentity.analysis.NamedEntityCharFilterFactory;
import ec.ep.dit.redmap.es.namedentity.analysis.NamedEntityTokenFilterFactory;
import ec.ep.dit.redmap.es.namedentity.analysis.NamedEntityTokenizerFactory;

public class NamedEntityAnalysisBinderProcessor extends AnalysisModule.AnalysisBinderProcessor {
    
	@Override
	public void processTokenizers(TokenizersBindings tokenizersBindings) {
		tokenizersBindings.processTokenizer("namedentity", NamedEntityTokenizerFactory.class);
	}
	
	@Override
	public void processCharFilters(CharFiltersBindings charFiltersBindings) {
		charFiltersBindings.processCharFilter("namedentity", NamedEntityCharFilterFactory.class);
	}
	
    @Override
    public void processTokenFilters(TokenFiltersBindings tokenFiltersBindings) {
        tokenFiltersBindings.processTokenFilter("namedentity", NamedEntityTokenFilterFactory.class);
        tokenFiltersBindings.processTokenFilter("autophrase", AutoPhrasingTokenFilterFactory.class);
    }
	
}
