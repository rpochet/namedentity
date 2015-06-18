package ec.ep.dit.redmap.es.namedentity.similarity;

import org.apache.lucene.search.similarities.DefaultSimilarity;
import org.apache.lucene.search.similarities.Similarity;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.inject.assistedinject.Assisted;
import org.elasticsearch.index.similarity.AbstractSimilarityProvider;

public class CountMatchingTermSimilarityProvider extends AbstractSimilarityProvider {

    private final Similarity similarity;

    @Inject
	public CountMatchingTermSimilarityProvider(@Assisted String name) {
		super(name);
		this.similarity = new DefaultSimilarity() {
			@Override
			public float coord(int overlap, int maxOverlap) {
				return 1f / maxOverlap;
			}
			@Override
			public float idf(long docFreq, long numDocs) {
				return 1f;
			}
			@Override
			public float queryNorm(float sumOfSquaredWeights) {
				return 1f;
			}
			@Override
			public float tf(float freq) {
				return freq == 0f ? 0f : 1f;
			}
		};
	}

    /**
     * {@inheritDoc}
     */
    @Override
    public Similarity get() {
        return this.similarity;
    }
	
}
