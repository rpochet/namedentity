package ec.ep.dit.redmap.es.namedentity.analysis;

import java.io.IOException;
import java.util.List;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.analysis.util.ResourceLoader;
import org.apache.lucene.analysis.util.ResourceLoaderAware;
import org.apache.lucene.util.Version;
import org.elasticsearch.client.node.NodeClient;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.inject.assistedinject.Assisted;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;
import org.elasticsearch.index.Index;
import org.elasticsearch.index.analysis.AbstractTokenFilterFactory;
import org.elasticsearch.index.analysis.Analysis;
import org.elasticsearch.index.settings.IndexSettings;

/**
 * See <a href="https://github.com/LucidWorks/auto-phrase-tokenfilter">auto-phrase-tokenfilter</a>
 * 
 * @author rpochet
 *
 */
public class AutoPhrasingTokenFilterFactory extends AbstractTokenFilterFactory
		implements ResourceLoaderAware {

	private CharArraySet phraseSets;
	private final String phraseSetFiles = null;
	private final boolean ignoreCase;
	private final boolean emitSingleTokens;

	private String replaceWhitespaceWith = null;

    @Inject
	public AutoPhrasingTokenFilterFactory(Environment env, Index index, @IndexSettings Settings indexSettings, @Assisted String name, @Assisted Settings settings, NodeClient nodeClient) {
        super(index, indexSettings, name, settings);

        List<String> phrases = Analysis.getWordList(env, settings, "phrases"); // From "phrases_path" (as a file) or "phrases" (as an array)
        
		ignoreCase = settings.getAsBoolean("ignoreCase", false);
		emitSingleTokens = settings.getAsBoolean("includeTokens", false);

		if(phrases != null) {
        	phraseSets = new CharArraySet(Version.LUCENE_CURRENT, phrases, ignoreCase);
        }
		
		String replaceWhitespaceArg = settings.get("replaceWhitespaceWith");
		if (replaceWhitespaceArg != null) {
			replaceWhitespaceWith = replaceWhitespaceArg;
		}
	}

	@Override
	public void inform(ResourceLoader loader) throws IOException {
		if (phraseSetFiles != null) {
			//phraseSets = getWordSet(loader, phraseSetFiles, ignoreCase);
		}
	}

	@Override
	public TokenStream create(TokenStream input) {
		AutoPhrasingTokenFilter autoPhraseFilter = new AutoPhrasingTokenFilter(input, phraseSets, emitSingleTokens);
		if (replaceWhitespaceWith != null) {
			autoPhraseFilter.setReplaceWhitespaceWith(new Character(
					replaceWhitespaceWith.charAt(0)));
		}
		return autoPhraseFilter;
	}

}
