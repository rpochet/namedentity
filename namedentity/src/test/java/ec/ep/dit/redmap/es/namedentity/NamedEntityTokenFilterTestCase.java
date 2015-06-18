package ec.ep.dit.redmap.es.namedentity;

import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import org.apache.lucene.analysis.BaseTokenStreamTestCase;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.standard.StandardTokenizerFactory;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionLengthAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;
import org.apache.lucene.analysis.util.TokenizerFactory;
import org.elasticsearch.client.node.NodeClient;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;
import org.elasticsearch.index.Index;

import ec.ep.dit.redmap.es.namedentity.analysis.NamedEntityTokenFilterFactory;

public class NamedEntityTokenFilterTestCase extends BaseTokenStreamTestCase {

	/**
	 * With hyphenation-only, you can get a lot of nonsense tokens. This can be
	 * controlled with the min/max subword size.
	 */
	public void testNamedEntityTokenFilter() throws Exception {
		
		StringReader inputText = new StringReader("This is a TEST string");
	    Map<String, String> param = new HashMap<>();
	    param.put("luceneMatchVersion", "LUCENE_49");

	    TokenizerFactory stdTokenFact = new StandardTokenizerFactory(param);
	    Tokenizer tokenizer = stdTokenFact.create(inputText);

	    Index index = new Index("reference");
		Settings indexSettings = ImmutableSettings.builder().build();
		Settings settings = ImmutableSettings.builder().build();
		NodeClient nodeClient = null;
		Environment env = new Environment();
		String name = "namedentity";
		NamedEntityTokenFilterFactory fact = new NamedEntityTokenFilterFactory(env, index, indexSettings, name , settings, nodeClient);
	    TokenStream tokenStream = fact.create(tokenizer);

	    CharTermAttribute termAtt = (CharTermAttribute) tokenStream.getAttribute(CharTermAttribute.class);
	    OffsetAttribute offsetAtt = (OffsetAttribute) tokenStream.getAttribute(OffsetAttribute.class);
	    PositionIncrementAttribute posIncrAtt = (PositionIncrementAttribute) tokenStream.getAttribute(PositionIncrementAttribute.class);
	    PositionLengthAttribute posLenAtt = (PositionLengthAttribute) tokenStream.getAttribute(PositionLengthAttribute.class);
	    TypeAttribute typeAtt = (TypeAttribute) tokenStream.getAttribute(TypeAttribute.class);
	    
	    tokenStream.reset();

	    while (tokenStream.incrementToken()) {
	        System.out.println("CharTermAttribute Length = " + termAtt.length());
	        System.out.println(termAtt.toString());
	        System.out.println("OffsetAttribute Offset = " + offsetAtt.startOffset() + " > " + offsetAtt.endOffset());
	        System.out.println("PositionIncrementAttribute PositionIncrement = " + posIncrAtt.getPositionIncrement());
	        System.out.println("PositionLengthAttribute PositionLength = " + posLenAtt.getPositionLength());
	        System.out.println("TypeAttribute Type = " + typeAtt.type());
	        System.err.println("-------------------------------------------");
	    }

	    tokenStream.end();
	    tokenStream.close();

	}

	/*public void testReset() throws Exception {
		String[] dict = { "Rind", "Fleisch", "Draht", "Schere", "Gesetz",
				"Aufgabe", "Ãœberwachung" };

		Tokenizer wsTokenizer = new WhitespaceTokenizer(TEST_VERSION_CURRENT,
				new StringReader("RindfleischÃ¼berwachungsgesetz"));
		DictionaryCompoundWordTokenFilter tf = new DictionaryCompoundWordTokenFilter(
				TEST_VERSION_CURRENT, wsTokenizer, dict,
				CompoundWordTokenFilterBase.DEFAULT_MIN_WORD_SIZE,
				CompoundWordTokenFilterBase.DEFAULT_MIN_SUBWORD_SIZE,
				CompoundWordTokenFilterBase.DEFAULT_MAX_SUBWORD_SIZE, false);

		CharTermAttribute termAtt = tf.getAttribute(CharTermAttribute.class);
		assertTrue(tf.incrementToken());
		assertEquals("RindfleischÃ¼berwachungsgesetz", termAtt.toString());
		assertTrue(tf.incrementToken());
		assertEquals("Rind", termAtt.toString());
		wsTokenizer.reset(new StringReader("RindfleischÃ¼berwachungsgesetz"));
		tf.reset();
		assertTrue(tf.incrementToken());
		assertEquals("RindfleischÃ¼berwachungsgesetz", termAtt.toString());
	}*/

}