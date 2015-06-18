package ec.ep.dit.redmap.es.namedentity.analysis;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.charfilter.BaseCharFilter;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.search.SearchHit;

public class NamedEntityCharFilter extends BaseCharFilter {

	private final Map<Pattern, String> patterns;

	private Reader transformedInput;
    
    //private final Analyzer analyzer;

	private String namedEntityIndex;

	private Client client;

	/**
	 * 
	 * @param patterns List of Regexp pattern to look for and its replacment expression
	 * @param analyzer Analyzer to 
	 * @param client
	 * @param namedEntityIndex 
	 * @param in
	 */
	public NamedEntityCharFilter(Map<Pattern, String> patterns, Analyzer analyzer, Client client, String namedEntityIndex, Reader in) {
		super(in);
		this.client = client;
		//this.analyzer = analyzer;
		this.patterns = patterns;
		this.namedEntityIndex = namedEntityIndex;
	}

	@Override
	public int read(char[] cbuf, int off, int len) throws IOException {
		// Buffer all input on the first call.
		if (transformedInput == null) {
			fill();
		}

		return transformedInput.read(cbuf, off, len);
	}

	/**
	 * Read the underlying character-input stream. 
	 * 
	 * @throws IOException
	 */
	private void fill() throws IOException {
		StringBuilder buffered = new StringBuilder();
		char[] temp = new char[1024];
		for (int cnt = input.read(temp); cnt > 0; cnt = input.read(temp)) {
			buffered.append(temp, 0, cnt);
		}
		transformedInput = new StringReader(processAllPatterns(buffered).toString());
	}

	@Override
	public int read() throws IOException {
		if (transformedInput == null) {
			fill();
		}

		return transformedInput.read();
	}

	@Override
	protected int correct(int currentOff) {
		return Math.max(0, super.correct(currentOff));
	}

	CharSequence processAllPatterns(CharSequence input) throws IOException {
		// Process named entity stored in index 
		CharSequence output = processIndexPattern(input);
		
		// Process patterns
		for (Entry<Pattern, String> pattern : this.patterns.entrySet()) {
			output = processPattern(output, pattern.getKey(), pattern.getValue());
		}
		return output;
	}

	/**
	 * Replace entity in input and mark correction offsets.
	 * @throws IOException 
	 */
	CharSequence processIndexPattern(CharSequence input) throws IOException {
		CharSequence output = input;
		XContentBuilder xb = jsonBuilder()
            .startObject()
                .startObject("query")
                	.startObject("match_phrase")
                    	.field("content", input)
                    .endObject()
                .endObject()
            .endObject();
        SearchResponse response = client.prepareSearch(this.namedEntityIndex).setSource(xb).execute().actionGet();
        if(response.getHits().getTotalHits() > 0) {
        	SearchHit hit = response.getHits().getAt(0);
        	System.out.println(hit);
        }
		return output;
	}

	/**
	 * Replace pattern in input and mark correction offsets.
	 */
	CharSequence processPattern(CharSequence input, Pattern pattern, String replacement) {
		final Matcher m = pattern.matcher(input);

		final StringBuffer cumulativeOutput = new StringBuffer();
		int cumulative = 0;
		int lastMatchEnd = 0;
		while (m.find()) {
			final int groupSize = m.end() - m.start();
			final int skippedSize = m.start() - lastMatchEnd;
			lastMatchEnd = m.end();

			final int lengthBeforeReplacement = cumulativeOutput.length()
					+ skippedSize;
			m.appendReplacement(cumulativeOutput, replacement);
			// Matcher doesn't tell us how many characters have been appended
			// before the replacement.
			// So we need to calculate it. Skipped characters have been added as
			// part of appendReplacement.
			final int replacementSize = cumulativeOutput.length()
					- lengthBeforeReplacement;

			if (groupSize != replacementSize) {
				if (replacementSize < groupSize) {
					// The replacement is smaller.
					// Add the 'backskip' to the next index after the
					// replacement (this is possibly
					// after the end of string, but it's fine -- it just means
					// the last character
					// of the replaced block doesn't reach the end of the
					// original string.
					cumulative += groupSize - replacementSize;
					int atIndex = lengthBeforeReplacement + replacementSize;
					// System.err.println(atIndex + "!" + cumulative);
					addOffCorrectMap(atIndex, cumulative);
				} else {
					// The replacement is larger. Every new index needs to point
					// to the last
					// element of the original group (if any).
					for (int i = groupSize; i < replacementSize; i++) {
						addOffCorrectMap(lengthBeforeReplacement + i,
								--cumulative);
						// System.err.println((lengthBeforeReplacement + i) +
						// " " + cumulative);
					}
				}
			}
		}

		// Append the remaining output, no further changes to indices.
		m.appendTail(cumulativeOutput);
		return cumulativeOutput;
	}
}
