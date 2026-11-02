package com.transportes.api.order;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Arma el filtro de búsqueda de órdenes. Solo agrega una condición por cada filtro que venga con valor,
 * así no se mandan parámetros nulos a la base de datos.
 */
public final class OrderSpecifications {

    private OrderSpecifications() {
    }

    /**
     * @param createdFrom fecha de creación mínima (inclusive)
     * @param createdTo   fecha de creación máxima (exclusiva)
     */
    public static Specification<Order> withFilters(final OrderStatus status,
                                                   final LocalDateTime createdFrom,
                                                   final LocalDateTime createdTo,
                                                   final String origin,
                                                   final String destination) {
        return new Specification<Order>() {
            @Override
            public Predicate toPredicate(Root<Order> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
                List<Predicate> predicates = new ArrayList<Predicate>();

                if (status != null) {
                    predicates.add(cb.equal(root.get("status"), status));
                }
                if (createdFrom != null) {
                    predicates.add(cb.greaterThanOrEqualTo(root.<LocalDateTime>get("createdAt"), createdFrom));
                }
                if (createdTo != null) {
                    predicates.add(cb.lessThan(root.<LocalDateTime>get("createdAt"), createdTo));
                }
                if (origin != null) {
                    predicates.add(cb.like(cb.lower(root.<String>get("origin")), contains(origin)));
                }
                if (destination != null) {
                    predicates.add(cb.like(cb.lower(root.<String>get("destination")), contains(destination)));
                }

                return cb.and(predicates.toArray(new Predicate[0]));
            }
        };
    }

    private static String contains(String text) {
        return "%" + text.toLowerCase(Locale.ROOT) + "%";
    }
}
