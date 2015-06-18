package ec.ep.dit.redmap.es.namedentity.analysis;

import java.io.Reader;
import java.net.URISyntaxException;

import org.apache.lucene.analysis.Tokenizer;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.inject.assistedinject.Assisted;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;
import org.elasticsearch.index.Index;
import org.elasticsearch.index.analysis.AbstractTokenizerFactory;
import org.elasticsearch.index.settings.IndexSettings;


public class NamedEntityTokenizerFactory extends AbstractTokenizerFactory {

    @Inject
    public NamedEntityTokenizerFactory(Index index,
                                @IndexSettings Settings indexSettings,
                                Environment env,
                                @Assisted String name,
                                @Assisted Settings settings) throws URISyntaxException {

        super(index, indexSettings, name, settings);

        String lexicon     = settings.get("lexicon", "mlteast-en");
        String lexiconPath = settings.get("lexicon_path", null);

        /*if (lexiconPath != null) {
            this.lemmatizer = getLemmatizer(env.resolveConfig(lexiconPath).toURI());
        } else {
            lexicon         = lexicon.contains("mlteast-") ? lexicon : "mlteast-" + lexicon;
            this.lemmatizer = getLemmatizer(lexicon);
        }*/
    }
    
    @Override
    public Tokenizer create(Reader reader) {
    	return new NamedEntityTokenizer(reader);
    }

}
