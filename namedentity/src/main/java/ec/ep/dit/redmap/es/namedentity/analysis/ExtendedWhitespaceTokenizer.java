package ec.ep.dit.redmap.es.namedentity.analysis;

import java.io.Reader;

import org.apache.lucene.analysis.util.CharTokenizer;
import org.apache.lucene.util.AttributeFactory;
import org.apache.lucene.util.Version;

/**
 * WhitespaceTokenizer with '(' and ')' as extra tokenizer characters.
 * 
 * @author rpochet
 *
 */
public final class ExtendedWhitespaceTokenizer extends CharTokenizer {

	/**
	 * Construct a new ExtendedWhitespaceTokenizer.
	 * 
	 * @param matchVersion
	 *            Lucene version to match See
	 *            {@link <a href="#version">above</a>}
	 * 
	 * @param in
	 *            the input to split up into tokens
	 */
	public ExtendedWhitespaceTokenizer(Version matchVersion, Reader in) {
		super(matchVersion, in);
	}

	/**
	 * Construct a new ExtendedWhitespaceTokenizer using a given
	 * {@link org.apache.lucene.util.AttributeSource.AttributeFactory}.
	 * 
	 * @param matchVersion
	 *            Lucene version to match See
	 *            {@link <a href="#version">above</a>}
	 * @param factory
	 *            the attribute factory to use for this {@link Tokenizer}
	 * @param in
	 *            the input to split up into tokens
	 */
	public ExtendedWhitespaceTokenizer(Version matchVersion,
			AttributeFactory factory, Reader in) {
		super(matchVersion, factory, in);
	}

	/**
	 * Collects only characters which do not satisfy
	 * {@link Character#isWhitespace(int)}.
	 */
	@Override
	protected boolean isTokenChar(int c) {
		return !Character.isWhitespace(c) && c != '(' && c != ')';
	}
}
