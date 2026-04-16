package com.hemo.backend.service;

import com.hemo.backend.dto.SearchCreateRequestDTO;
import com.hemo.backend.dto.SearchResponseDTO;
import com.hemo.backend.entity.Search;
import com.hemo.backend.entity.User;
import com.hemo.backend.exception.GlobalExceptionHandler.AppException;
import com.hemo.backend.repository.RequestRepository;
import com.hemo.backend.repository.SearchRepository;
import com.hemo.backend.repository.UserRepository;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SearchService {

    private final SearchRepository searchRepository;
    private final UserRepository userRepository;
    private final RequestRepository requestRepository;

    public SearchResponseDTO createSearch(Long userId, SearchCreateRequestDTO payload) {
        Long safeUserId = Objects.requireNonNull(userId, "userId");
        User user = userRepository.findById(safeUserId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "User not found"));

        if (requestRepository.countActiveByUser(safeUserId) > 0) {
            throw new AppException(HttpStatus.CONFLICT, "Active request exists");
        }

        Search search = new Search();
        search.setUser(user);
        search.setHospitalName(payload.getHospitalName() == null ? "" : payload.getHospitalName());
        search.setHospitalPincode(payload.getHospitalPincode() == null ? "" : payload.getHospitalPincode());
        search.setBloodGroup(payload.getBloodGroup() == null ? "" : payload.getBloodGroup());
        search.setBloodComponent(payload.getBloodComponent() == null ? "" : payload.getBloodComponent());

        Search saved = searchRepository.save(search);

        return SearchResponseDTO.builder()
            .searchId(saved.getSearchId())
            .userId(saved.getUser().getUserId())
            .hospitalName(saved.getHospitalName())
            .hospitalPincode(saved.getHospitalPincode())
            .bloodGroup(saved.getBloodGroup())
            .bloodComponent(saved.getBloodComponent())
            .createdAt(saved.getCreatedAt())
            .build();
    }
}
