package org.adso.minimarket.repository.jpa;

import jakarta.persistence.LockModeType;
import org.adso.minimarket.models.product.Product;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    @Override
    <S extends Product> S save(S entity);

    boolean existsById(Long id);

    @EntityGraph(attributePaths = {"category", "images"})
    Optional<Product> findDetailedById(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Product> findForUpdateById(Long id);

    Product getById(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<Product> findForUpdateAllByIdIn(List<Long> ids);

    @EntityGraph(attributePaths = {"category", "images"})
    List<Product> findTop8ByOrderByCreatedAtDesc();
}
