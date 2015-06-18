package ec.ep.dit.redmap.es.namedentity.analysis;

import java.io.IOException;
import java.util.LinkedList;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionLengthAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;
import org.apache.lucene.util.AttributeSource;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.node.NodeClient;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.ESLoggerFactory;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder.Operator;
import org.elasticsearch.index.query.QueryBuilders;

public class NamedEntityTokenFilter extends TokenFilter {
	
	private static final ESLogger logger = ESLoggerFactory.getLogger("NamedEntityTokenFilter");
	/**
	 * filler token for when positionIncrement is more than 1
	 */
	public static final String DEFAULT_FILLER_TOKEN = "_";

	/**
	 * The default string to use when joining adjacent tokens to form a shingle
	 */
	public static final String DEFAULT_TOKEN_SEPARATOR = " ";
	
	public static final Integer DEFAULT_WINDOW_LENGTH = 3;

	private final CharTermAttribute term = addAttribute(CharTermAttribute.class);
	private final OffsetAttribute offset = addAttribute(OffsetAttribute.class);
	private final PositionIncrementAttribute posIncr = addAttribute(PositionIncrementAttribute.class);
	private final PositionLengthAttribute posLen = addAttribute(PositionLengthAttribute.class);
	private final TypeAttribute type = addAttribute(TypeAttribute.class);

	// Configuration fields
	private int windowLength = 3;

	private String namedEntityIndiceName = null;
	
	private NodeClient nodeClient = null;
	
	private String tokenQueryName = "content";
	
	private String tokenQueryRawName = tokenQueryName + ".raw";
	 
	private String tokenFieldName = "content";
	

	// Transient fields
	private final StringBuilder buffer = new StringBuilder();

	private LinkedList<AttributeSource.State> states = new LinkedList<AttributeSource.State>();

	private LinkedList<WindowToken> words = new LinkedList<WindowToken>();

	private State state2emit = null;
	
	private WindowToken tokenWord = null;

	private int startOffset;

	private int endOffset;

	private int matchedEntity;

	private SearchResponse searchResponse;

	private MatchQueryBuilder queryBuilder;

	protected NamedEntityTokenFilter(TokenStream input, int windowLength, NodeClient nodeClient, String namedEntityIndiceName) {
		super(input);
		this.windowLength = windowLength;
		this.nodeClient = nodeClient;
		this.namedEntityIndiceName = namedEntityIndiceName;
	}

	@Override
	public boolean incrementToken() throws IOException {
		type.setType(TypeAttribute.DEFAULT_TYPE);
		/*if(logger.isDebugEnabled()) {
			logger.debug("---------------------------");
			logger.debug("Term {}", term);
			logger.debug("Offset {} to {}", offset.startOffset(), offset.endOffset());
			logger.debug("Pos Incr {}", posIncr.getPositionIncrement());
			logger.debug("Pos Len {}", posLen.getPositionLength());
			logger.debug("Type {}", type.type());
		}*/
		while(input.incrementToken()) {
			
			if(words.size() < windowLength) {
				
				// Add token in window
				states.addLast(captureState());
				words.addLast(new WindowToken(term.length(), term.buffer(), offset.startOffset(), offset.endOffset()));
				
				if(logger.isDebugEnabled()) {
					logger.debug("Adding words {}", words);
				}
				
			} else { 
				
				// Window is full, start
				
				// Test if window match an entity
				matchedEntity = matchEntity(); 
				if(matchedEntity > 0) {
					onMatch();
					return true;
				}
				
				// Shift tokens 
				tokenWord = words.removeFirst();
				words.addLast(new WindowToken(term.length(), term.buffer(), offset.startOffset(), offset.endOffset()));
				
				state2emit = states.removeFirst();
				states.addLast(captureState());
				
				// Emit first window token
				restoreState(state2emit);

				if(logger.isDebugEnabled()) {
					logger.debug("Emit token {}", tokenWord);
					logger.debug("Shifting words {}", words);
				}

				return true;
			}
		}
		
		// No more token
		// Emit token stored in window 
		if(words.size() > 0) {

			// Test if window match an entity
			matchedEntity = matchEntity(); 
			if(matchedEntity > 0) {
				onMatch();
				return true;
			}
			
			tokenWord = words.removeFirst();
			
			state2emit = states.removeFirst();
			restoreState(state2emit);
			
			if(logger.isDebugEnabled()) {
				logger.debug("Emit token {}", tokenWord);
				logger.debug("Remains {}", words);
			}
			return true;
		}
		
		return false;
	}

	/**
	 * Test if the current window contains entity
	 *  - test first token
	 *  - test first and second token
	 *  - ...
	 * 
	 * @return The number of word, from 0, matching an entity
	 */
	private int matchEntity() {
		this.buffer.setLength(0);
		int i = 0;
		for (WindowToken windowToken : words) {
			this.buffer.append(windowToken.termBuffer, 0, windowToken.termLength);
			
			queryBuilder = QueryBuilders.matchQuery(this.tokenQueryName, this.buffer.toString()).operator(Operator.AND);
			this.searchResponse = nodeClient.prepareSearch(this.namedEntityIndiceName)
				.setQuery(queryBuilder)
				.addField(this.tokenQueryRawName)
				.execute().actionGet();

			if(logger.isDebugEnabled()) {
				logger.debug("Searching '{}'", this.buffer.toString());
				logger.debug("Got {} hits, top score {}", 
						this.searchResponse.getHits().getHits().length, 
						this.searchResponse.getHits().getHits().length > 0 ? this.searchResponse.getHits().getAt(0).getScore() : "N.A.");
			}
			
			if(this.searchResponse.getHits().getHits().length > 0) {
				if(this.searchResponse.getHits().getAt(0).getScore() > 0.8f) {
					this.buffer.setLength(0);
					this.buffer.append(this.searchResponse.getHits().getAt(0).field(this.tokenFieldName).getValue().toString());
					return i + 1;
				}
			}
			this.buffer.append(" ");
		}
		return 0;
	}
	
	private void onMatch() {
		startOffset = words.getFirst().startOffset;
		endOffset = words.getLast().endOffset;
		
		for(int i = 0; i < matchedEntity; i++) {
			states.removeFirst();
			words.removeFirst();
		}
		
		states.addLast(captureState());
		words.addLast(new WindowToken(term.length(), term.buffer(), offset.startOffset(), offset.endOffset()));
		
		term.setLength(0);
		term.append(this.buffer, 0, this.buffer.length() - 1);
		offset.setOffset(startOffset, endOffset);
		posIncr.setPositionIncrement(matchedEntity);
		type.setType("entity");
		
		if(logger.isDebugEnabled()) {
			logger.debug("Window tokens '{}' matches", buffer);
			logger.debug("Clearing words {}", words);
		}
	}

	@Override
	public void reset() throws IOException {
		super.reset();
		this.words.clear();
		this.state2emit = null;
		this.buffer.setLength(0);
		
		if(logger.isDebugEnabled()) {
			logger.debug("================================");
		}
	}

	@Override
	public String toString() {
		return new StringBuilder()
			.append("CharTermAttribute Length = " + term.length() + ", Term = " + term.toString())
			.append("\n")
	        .append("OffsetAttribute Offset = " + offset.startOffset() + "->" + offset.endOffset())
			.append("\n")
	        .append("PositionIncrementAttribute PositionIncrement = " + posIncr.getPositionIncrement())
			.append("\n")
	        .append("PositionLengthAttribute PositionLength = " + posLen.getPositionLength())
			.append("\n")
	        .append("TypeAttribute Type = " + type.type())
        .toString();
	}
	
	class WindowToken {
		
		/*CharTermAttribute term;
		
		OffsetAttribute offset;
		
		PositionIncrementAttribute posIncr;
		
		PositionLengthAttribute posLen;
		
		TypeAttribute type;
		
		public WindowToken(CharTermAttribute term, OffsetAttribute offset, PositionIncrementAttribute posIncr, PositionLengthAttribute posLen, TypeAttribute type) {
			this.term = term;
			this.offset = offset;
			this.posIncr = posIncr;
			this.posLen = posLen;
			this.type = type;
		}*/

		private int termLength;

		private char[] termBuffer;

		private int startOffset;

		private int endOffset;

		public WindowToken(int termLength, char[] termBuffer, int startOffset, int endOffset) {
			this.termLength = termLength;
			this.termBuffer = new char[termLength];
			System.arraycopy(termBuffer, 0, this.termBuffer, 0, termLength);
			this.startOffset = startOffset;
			this.endOffset = endOffset;
		}
		
		@Override
		public String toString() {
			return new String(termBuffer);
		}
		
	}
	
}
