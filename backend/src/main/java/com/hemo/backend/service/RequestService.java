package com.hemo.backend.service;

import com.hemo.backend.dto.DispatchResultDTO;
import com.hemo.backend.dto.DonorSearchResponseDTO;
import com.hemo.backend.dto.DonorSearchSummaryDTO;
import com.hemo.backend.dto.RequestDTO;
import com.hemo.backend.dto.RequestSummaryDTO;
import com.hemo.backend.dto.ResponseRecordDTO;
import com.hemo.backend.entity.Request;
import com.hemo.backend.exception.GlobalExceptionHandler.AppException;
import com.hemo.backend.repository.RequestRepository;
import com.hemo.backend.repository.ResponseRepository;
import com.hemo.backend.repository.SearchRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RequestService {
    private final RequestRepository requestRepository;
    private final SearchRepository searchRepository;
    private final ResponseRepository responseRepository;
    private final DonorSearchService donorSearchService;
    private final RequestDispatchService requestDispatchService;

    @Transactional
    public RequestSummaryDTO createRequest(@NonNull Long searchId, RequestDTO dto) {
        var search = searchRepository.findById(searchId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Search not found"));

        Long userId = search.getUser().getUserId();
        if (hasActiveRequestForUser(userId)) {
            throw new AppException(HttpStatus.CONFLICT, "Active request exists");
        }

        int initialBatch = Math.min(20, Math.max(dto.getMatchedCount(), 0));

        Request request = buildRequest(search, dto.getBloodGroup(), dto.getComponent(), dto.getUnitsRequested(), null);
        request.setNumberOfDonorsContacted(initialBatch);

        Request saved = requestRepository.save(request);
        int sent = requestDispatchService.dispatchBatchForRequest(saved, initialBatch);
        saved.setNumberOfDonorsContacted(sent);
        saved.setLastNotifiedAt(LocalDateTime.now());
        saved = requestRepository.save(saved);
        return toRequestSummary(saved, canReRequest(saved));
    }

    @Transactional
    public RequestSummaryDTO reRequest(@NonNull Long requestId) {
        Request oldRequest = requestRepository.findByIdWithSearchAndUser(requestId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Request not found"));

        long responseCount = responseRepository.countSince(requestId, LocalDateTime.now().minusHours(1));
        if (responseCount > 0) {
            throw new AppException(HttpStatus.CONFLICT, "Responses already received");
        }

        Request next = buildRequest(
            oldRequest.getSearch(),
            oldRequest.getBloodGroup(),
            oldRequest.getComponent(),
            oldRequest.getUnitsRequested(),
            oldRequest
        );
        next.setNumberOfDonorsContacted(20);

        Request saved = requestRepository.save(next);
        int sent = requestDispatchService.dispatchBatchForRequest(saved, 20);
        saved.setNumberOfDonorsContacted(sent);
        saved.setLastNotifiedAt(LocalDateTime.now());
        saved = requestRepository.save(saved);
        return toRequestSummary(saved, canReRequest(saved));
    }

    @Transactional
    public DispatchResultDTO dispatchNextTwenty(@NonNull Long requestId) {
        Request request = requestRepository.findByIdWithSearchAndUser(requestId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Request not found"));

        if (request.getExpiresAt() != null && !request.getExpiresAt().isAfter(LocalDateTime.now())) {
            request.setStatus("EXPIRED");
            requestRepository.save(request);
            throw new AppException(HttpStatus.CONFLICT, "Request expired");
        }

        int current = request.getNumberOfDonorsContacted() == null ? 0 : request.getNumberOfDonorsContacted();
        int start = current + 1;
        int sent = requestDispatchService.dispatchBatchForRequest(request, 20);
        int end = current + sent;

        request.setNumberOfDonorsContacted(end);
        request.setLastNotifiedAt(LocalDateTime.now());
        requestRepository.save(request);

        return new DispatchResultDTO(requestId, start, end);
    }

    @Transactional(readOnly = true)
    public List<RequestSummaryDTO> getUserRequestHistory(@NonNull Long userId) {
        return requestRepository.findByUserId(userId)
                .stream()
                .map(req -> toRequestSummary(req, canReRequest(req)))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ResponseRecordDTO> getUserResponses(@NonNull Long userId) {
        return responseRepository.findAcceptedResponsesByUserId(userId)
                .stream()
            .map(ResponseRecordMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public DonorSearchSummaryDTO previewReRequest(@NonNull Long requestId) {
        Request request = requestRepository.findByIdWithSearchAndUser(requestId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Request not found"));
        DonorSearchResponseDTO donorBatch = donorSearchService.searchCompatibleDonors(
            request.getBloodGroup(),
            request.getSearch().getHospitalPincode(),
            0,
            200,
            responseRepository.findContactedDonorIdsBySearchId(request.getSearch().getSearchId())
        );
        return donorSearchService.toSummaryDTO(donorBatch);
    }

    @Transactional(readOnly = true)
    public boolean hasActiveRequestForUser(@NonNull Long userId) {
        return requestRepository.countActiveByUser(userId) > 0;
    }

    private boolean canReRequest(Request request) {
        if (request.getExpiresAt() == null || request.getExpiresAt().isAfter(LocalDateTime.now())) {
            return false;
        }
        return responseRepository.countByRequestId(request.getRequestId()) == 0;
    }

    private RequestSummaryDTO toRequestSummary(Request request, boolean canReRequest) {
        return RequestSummaryDTO.builder()
                .requestId(request.getRequestId())
                .searchId(request.getSearch().getSearchId())
                .bloodGroup(request.getBloodGroup())
                .component(request.getComponent())
                .unitsRequested(request.getUnitsRequested())
                .numberOfDonorsContacted(request.getNumberOfDonorsContacted())
                .status(request.getStatus())
                .parentRequestId(request.getParentRequest() == null ? null : request.getParentRequest().getRequestId())
                .createdAt(request.getCreatedAt())
                .expiresAt(request.getExpiresAt())
                .lastNotifiedAt(request.getLastNotifiedAt())
                .canReRequest(canReRequest)
                .build();
    }

    

    private Request buildRequest(
            com.hemo.backend.entity.Search search,
            String bloodGroup,
            String component,
            Integer unitsRequested,
            Request parentRequest) {
        Request request = new Request();
        request.setSearch(search);
        request.setBloodGroup(bloodGroup);
        request.setComponent(component);
        request.setUnitsRequested(unitsRequested);
        request.setStatus("ACTIVE");
        request.setParentRequest(parentRequest);
        return request;
    }

}
