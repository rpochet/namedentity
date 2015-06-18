package ec.ep.dit.redmap.es.namedentity.analysis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.lucene.analysis.TokenStream;
import org.elasticsearch.client.node.NodeClient;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.inject.assistedinject.Assisted;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;
import org.elasticsearch.index.Index;
import org.elasticsearch.index.analysis.AbstractTokenFilterFactory;
import org.elasticsearch.index.analysis.Analysis;
import org.elasticsearch.index.analysis.TokenFilterFactory;
import org.elasticsearch.index.settings.IndexSettings;

public class NamedEntityTokenFilterFactory extends AbstractTokenFilterFactory {

    private final Factory factory;

    @Inject
    public NamedEntityTokenFilterFactory(Environment env, Index index, @IndexSettings Settings indexSettings, @Assisted String name, @Assisted Settings settings, NodeClient nodeClient) {
        super(index, indexSettings, name, settings);
        Integer windowLength = settings.getAsInt("window-length", NamedEntityTokenFilter.DEFAULT_WINDOW_LENGTH);

        Boolean outputUnigrams = settings.getAsBoolean("output_unigrams", true);
        Boolean outputUnigramsIfNoShingles = settings.getAsBoolean("output_unigrams_if_no_shingles", false);
        String tokenSeparator = settings.get("token_separator", NamedEntityTokenFilter.DEFAULT_TOKEN_SEPARATOR);
        String fillerToken = settings.get("filler_token", NamedEntityTokenFilter.DEFAULT_FILLER_TOKEN);
        Map<Pattern, String> patterns = new HashMap<Pattern, String>();        
        // Extract patterns from configuration file
        String[] sides = null;
        List<String> patternAndReplacements = Analysis.getWordList(env, settings, "patterns");
        if(patternAndReplacements != null) {
	        for (String patternAndReplacement : patternAndReplacements) {
	        	sides = split(patternAndReplacement, "=>");
				patterns.put(Pattern.compile(sides[0]), sides[1]);
			}
        }
    
        String namedEntityIndex = settings.get("_index", NamedEntityTokenFilter2.DEFAULT_NAMEDENTITY_INDEX);
        factory = new Factory(name, windowLength, outputUnigrams, outputUnigramsIfNoShingles, tokenSeparator, fillerToken, patterns, namedEntityIndex, nodeClient);
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
    public TokenStream create(TokenStream tokenStream) {
        return factory.create(tokenStream);
    }


    public Factory getInnerFactory() {
        return this.factory;
    }

    public static final class Factory implements TokenFilterFactory {
    	
        private final int windowLength;

        private final boolean outputUnigrams;

        private final boolean outputUnigramsIfNoShingles;

        private final String tokenSeparator;
        
        private final String fillerToken;

        private final String name;

        private Map<Pattern, String> patterns;
		
        private String namedEntityIndiceName;

		private final NodeClient nodeClient;

		Factory(String name, int windowLength, boolean outputUnigrams, boolean outputUnigramsIfNoShingles, String tokenSeparator, String fillerToken, Map<Pattern, String> patterns, String namedEntityIndiceName, NodeClient nodeClient) {
            this.windowLength = windowLength;
            this.namedEntityIndiceName = namedEntityIndiceName;
            this.nodeClient = nodeClient;
            
            this.outputUnigrams = outputUnigrams;
            this.outputUnigramsIfNoShingles = outputUnigramsIfNoShingles;
            this.tokenSeparator = tokenSeparator;
            //this.minShingleSize = minShingleSize;
            this.fillerToken = fillerToken;
            this.name = name;
            this.patterns = patterns;
        }

        public TokenStream create(TokenStream tokenStream) {
        	NamedEntityTokenFilter filter = new NamedEntityTokenFilter(tokenStream, windowLength, nodeClient, namedEntityIndiceName);
        	//NamedEntityTokenFilter filter = new NamedEntityTokenFilter(tokenStream, maxShingleSize, maxShingleSize);
            //filter.setOutputUnigrams(outputUnigrams);
            /*filter.setOutputUnigramsIfNoShingles(outputUnigramsIfNoShingles);
            filter.setTokenSeparator(tokenSeparator);
            filter.setFillerToken(fillerToken);
            filter.setPatterns(patterns);
            filter.setNamedEntityIndex(namedEntityIndex);
            filter.setNodeClient(nodeClient);*/
            return filter;
        }

        public boolean getOutputUnigrams() {
            return outputUnigrams;
        }

        public boolean getOutputUnigramsIfNoShingles() {
            return outputUnigramsIfNoShingles;
        }

        @Override
        public String name() {
            return name;
        }
    }
}
