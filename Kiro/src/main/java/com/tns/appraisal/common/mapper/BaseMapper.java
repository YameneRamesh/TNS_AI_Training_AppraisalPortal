package com.tns.appraisal.common.mapper;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Base mapper interface for entity-DTO conversions.
 * 
 * @param <E> Entity type
 * @param <D> DTO type
 */
public interface BaseMapper<E, D> {

    /**
     * Convert entity to DTO.
     */
    D toDto(E entity);

    /**
     * Convert DTO to entity.
     */
    E toEntity(D dto);

    /**
     * Convert list of entities to list of DTOs.
     */
    default List<D> toDtoList(List<E> entities) {
        return entities.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Convert list of DTOs to list of entities.
     */
    default List<E> toEntityList(List<D> dtos) {
        return dtos.stream()
                .map(this::toEntity)
                .collect(Collectors.toList());
    }
}
