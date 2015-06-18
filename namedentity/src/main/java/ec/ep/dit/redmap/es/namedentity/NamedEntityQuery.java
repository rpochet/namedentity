package ec.ep.dit.redmap.es.namedentity;

import org.elasticsearch.index.query.QueryParseContext;
import org.elasticsearch.index.search.MultiMatchQuery;

public class NamedEntityQuery extends MultiMatchQuery {

	public NamedEntityQuery(QueryParseContext parseContext) {
		super(parseContext);
		// TODO Auto-generated constructor stub
	}

}
