package ec.ep.dit.redmap.es.namedentity.analysis;

import java.io.IOException;
import java.io.Reader;

import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;

public class NamedEntityTokenizer extends Tokenizer {

	protected NamedEntityTokenizer(Reader input) {
		super(input);
	}

	private static final ESLogger logger = Loggers.getLogger(NamedEntityTokenizer.class);
	  
	private final CharTermAttribute termAttr = addAttribute(CharTermAttribute.class);
	private final OffsetAttribute offsetAttr = addAttribute(OffsetAttribute.class);
	private final PositionIncrementAttribute piAttr = addAttribute(PositionIncrementAttribute.class);
    
	private boolean done = false;
    private int finalOffset;
    
	@Override
	public final boolean incrementToken() throws IOException {
        if (!done) {
        	clearAttributes();
            done = true;
            int upto = 0;
            char[] buffer = termAttr.buffer();
            while (true) {
                final int length = input.read(buffer, upto, buffer.length - upto);
                if (length == -1) break;
                upto += length;
                if (upto == buffer.length)
                    buffer = termAttr.resizeBuffer(1 + buffer.length);
            }
            termAttr.setLength(upto);
            String str = termAttr.toString();
            termAttr.setEmpty();
            
            long converted = str.hashCode();
            termAttr.append(String.valueOf(converted));

            if(logger.isDebugEnabled()) {
                logger.debug(str + ">" + converted);
            }

            finalOffset = correctOffset(upto);
            offsetAttr.setOffset(correctOffset(0), finalOffset);
            return true;
        }
        return false;
	}

    @Override
    public final void end() throws IOException {
        super.end();
        // set final offset
        offsetAttr.setOffset(finalOffset, finalOffset);
    }


    @Override
    public void reset() throws IOException {
        super.reset();
        this.done = false;
    }

}
