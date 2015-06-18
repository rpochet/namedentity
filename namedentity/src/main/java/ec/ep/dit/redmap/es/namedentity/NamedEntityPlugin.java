package ec.ep.dit.redmap.es.namedentity;

import org.elasticsearch.common.inject.Module;
import org.elasticsearch.index.analysis.AnalysisModule;
import org.elasticsearch.index.query.IndexQueryParserModule;
import org.elasticsearch.index.similarity.SimilarityModule;
import org.elasticsearch.indices.query.IndicesQueriesModule;
import org.elasticsearch.plugins.AbstractPlugin;

import ec.ep.dit.redmap.es.namedentity.similarity.CountMatchingTermSimilarityProvider;

public class NamedEntityPlugin extends AbstractPlugin {

    @Override 
    public String name() {
        return "namedentity";
    }

    @Override 
    public String description() {
        return "Named entity analysis support";
    }

    @Override 
    public void processModule(Module module) {
        if (module instanceof AnalysisModule) {
            AnalysisModule analysisModule = (AnalysisModule) module;
            analysisModule.addProcessor(new NamedEntityAnalysisBinderProcessor());
        }
    }
    
    public void onModule(IndicesQueriesModule module) {
    	module.addQuery(NamedEntityQueryParser.class);
    }
    
    /*public void onModule(IndexQueryParserModule module) {
        module.addQueryParser(name(), NamedEntityQueryParser.class);
    }*/
    
    public void onModule(SimilarityModule module) {
        module.addSimilarity(name(), CountMatchingTermSimilarityProvider.class);
    }
    
}
