package com.dev.IbioScience.service.product;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.dev.IbioScience.dto.internal.InternalLargeListDTO;
import com.dev.IbioScience.dto.internal.InternalMediumListDTO;
import com.dev.IbioScience.dto.internal.InternalSmallListDTO;
import com.dev.IbioScience.repository.product.InternalCategoryLargeRepository;
import com.dev.IbioScience.repository.product.InternalCategoryMediumRepository;
import com.dev.IbioScience.repository.product.InternalCategorySmallRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class InternalCategoryQueryService {

    private final InternalCategoryLargeRepository largeRepo;
    private final InternalCategoryMediumRepository mediumRepo;
    private final InternalCategorySmallRepository smallRepo;

    public List<InternalLargeListDTO> listLarge() {
        return largeRepo.findAllWithMediumCount()
                .stream()
                .map(a -> new InternalLargeListDTO(
                        (Long) a[0],
                        (String) a[1],
                        (Long) a[2]
                ))
                .collect(Collectors.toList());
    }

    public List<InternalMediumListDTO> listMedium(Long largeId) {
        return mediumRepo.findByLargeIdWithSmallCount(largeId)
                .stream()
                .map(a -> new InternalMediumListDTO(
                        (Long) a[0],
                        (String) a[1],
                        (Long) a[2]
                ))
                .collect(Collectors.toList());
    }

    public List<InternalSmallListDTO> listSmall(Long mediumId) {
        return smallRepo.findByMediumIdWithProductCount(mediumId)
                .stream()
                .map(a -> new InternalSmallListDTO(
                        (Long) a[0],
                        (String) a[1],
                        (Long) a[2]
                ))
                .collect(Collectors.toList());
    }
}