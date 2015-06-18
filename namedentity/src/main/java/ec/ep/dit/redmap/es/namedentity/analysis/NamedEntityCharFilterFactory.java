package ec.ep.dit.redmap.es.namedentity.analysis;

import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.core.WhitespaceTokenizer;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.ElasticsearchIllegalArgumentException;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.inject.assistedinject.Assisted;
import org.elasticsearch.common.lucene.Lucene;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;
import org.elasticsearch.index.Index;
import org.elasticsearch.index.analysis.AbstractCharFilterFactory;
import org.elasticsearch.index.analysis.Analysis;
import org.elasticsearch.index.analysis.AnalysisSettingsRequired;
import org.elasticsearch.index.analysis.TokenizerFactory;
import org.elasticsearch.index.analysis.TokenizerFactoryFactory;
import org.elasticsearch.index.settings.IndexSettings;
import org.elasticsearch.indices.analysis.IndicesAnalysisService;

@AnalysisSettingsRequired
public class NamedEntityCharFilterFactory extends AbstractCharFilterFactory {

    private final Map<Pattern, String> patterns;
    
    private final Analyzer analyzer;
    
    private final boolean ignoreCase;

    private final Client client;
    
    private final String namedEntityIndex;

    @Inject
    public NamedEntityCharFilterFactory(Index index, @IndexSettings Settings indexSettings, Environment env, @Assisted String name, @Assisted Settings settings, IndicesAnalysisService indicesAnalysisService, Map<String, TokenizerFactoryFactory> tokenizerFactories, Client client) {
        super(index, indexSettings, name);
        /*String[] escapedTags = settings.getAsMap()Array("patterns");
        if (escapedTags.length > 0) {
            this.escapedTags = ImmutableSet.copyOf(escapedTags);
        } else {
            this.escapedTags = null;
        }*/
        
        this.ignoreCase = settings.getAsBoolean("ignore_case", false);
        
        String tokenizerName = settings.get("tokenizer", "whitespace");

        TokenizerFactoryFactory tokenizerFactoryFactory = tokenizerFactories.get(tokenizerName);
        if (tokenizerFactoryFactory == null) {
            tokenizerFactoryFactory = indicesAnalysisService.tokenizerFactoryFactory(tokenizerName);
        }
        if (tokenizerFactoryFactory == null) {
            throw new ElasticsearchIllegalArgumentException("failed to find tokenizer [" + tokenizerName + "] for synonym token filter");
        }
        final TokenizerFactory tokenizerFactory = tokenizerFactoryFactory.create(tokenizerName, settings);

        this.analyzer = new Analyzer() {
            @Override
            protected TokenStreamComponents createComponents(String fieldName, Reader reader) {
                Tokenizer tokenizer = tokenizerFactory == null ? new WhitespaceTokenizer(Lucene.ANALYZER_VERSION, reader) : tokenizerFactory.create(reader);
                TokenStream stream = ignoreCase ? new LowerCaseFilter(Lucene.ANALYZER_VERSION, tokenizer) : tokenizer;
                return new TokenStreamComponents(tokenizer, stream);
            }
        };
        
        // Extract patterns from configuration file
        this.patterns = new HashMap<Pattern, String>();        
        String[] sides = null;
        List<String> patternAndReplacements = Analysis.getWordList(env, settings, "patterns");
        if(patternAndReplacements != null) {
	        for (String patternAndReplacement : patternAndReplacements) {
	        	sides = split(patternAndReplacement, "=>");
				this.patterns.put(Pattern.compile(sides[0]), sides[1]);
			}
        } else {
        	throw new ElasticsearchException("Char filter '" + name + "' requires argument 'patterns_path'");
        }

        // Extract named enity index
        this.namedEntityIndex = settings.get("_index", "_none");
        this.client = client;
    }
    
    private static String[] split(String s, String separator) {
      ArrayList<String> list = new ArrayList<String>(2);
      StringBuilder sb = new StringBuilder();
      int pos=0, end=s.length();
      while (pos < end) {
        if (s.startsWith(separator,pos)) {
          if (sb.length() > 0) {
            list.add(sb.toString().trim());
            sb=new StringBuilder();
          }
          pos+=separator.length();
          continue;
        }

        char ch = s.charAt(pos++);
        if (ch=='\\') {
          sb.append(ch);
          if (pos>=end) break;  // ERROR, or let it go?
          ch = s.charAt(pos++);
        }

        sb.append(ch);
      }

      if (sb.length() > 0) {
        list.add(sb.toString().trim());
      }

      return list.toArray(new String[list.size()]);
    }

    @Override
    public Reader create(Reader tokenStream) {
        return new NamedEntityCharFilter(this.patterns, analyzer, this.client, this.namedEntityIndex, tokenStream);
    }
}

