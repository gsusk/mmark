package org.adso.minimarket.repository.elastic;

import org.adso.minimarket.models.document.ProductDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for Elasticsearch.
 * Commented out to run without Elasticsearch dependency.
 */
@Repository
public interface ProductSearchRepository extends ElasticsearchRepository<ProductDocument, Long> {
}
