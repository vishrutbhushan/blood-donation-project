package com.hemo.backend.service;

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
import java.time.LocalDate;
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
        if (hasRootRequestTodayForUser(userId)) {
            throw new AppException(HttpStatus.CONFLICT, "Only one request is allowed per day");
        }

        int initialBatch = 20;

        Request request = buildRequest(search, dto.getBloodGroup(), dto.getComponent(), dto.getUnitsRequested(), null);
        request.setNumberOfDonorsContacted(initialBatch);

        Request saved = requestRepository.save(request);
        int sent = requestDispatchService.dispatchBatchForRequest(saved, initialBatch);
        saved.setNumberOfDonorsContacted(sent);
        saved.setLastNotifiedAt(LocalDateTime.now());
        saved = requestRepository.save(saved);
        return toRequestSummary(saved, reRequestState(saved));
    }

    @Transactional
    public RequestSummaryDTO reRequest(@NonNull Long requestId) {
        Request oldRequest = requestRepository.findByIdWithSearchAndUser(requestId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Request not found"));

        if (oldRequest.getParentRequest() != null) {
            throw new AppException(HttpStatus.CONFLICT, "Only one re-request is allowed for a request chain");
        }

        if (requestRepository.existsByParentRequest_RequestId(oldRequest.getRequestId())) {
            throw new AppException(HttpStatus.CONFLICT, "Only one re-request is allowed for a request chain");
        }

        LocalDateTime reRequestAvailableAt = oldRequest.getCreatedAt().plusHours(1);
        if (reRequestAvailableAt.isAfter(LocalDateTime.now())) {
            throw new AppException(HttpStatus.CONFLICT, "Please wait for 1 hour before re-requesting");
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
        return toRequestSummary(saved, reRequestState(saved));
    }

    @Transactional(readOnly = true)
    public List<RequestSummaryDTO> getUserRequestHistory(@NonNull Long userId) {
        List<Request> history = requestRepository.findByUserId(userId);
        return history.stream()
            .map(req -> toRequestSummary(req, reRequestState(req)))
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
            20,
            responseRepository.findContactedDonorIdsBySearchId(request.getSearch().getSearchId())
        );
        return donorSearchService.toSummaryDTO(donorBatch);
    }

    @Transactional(readOnly = true)
    public boolean hasActiveRequestForUser(@NonNull Long userId) {
        return requestRepository.countActiveByUser(userId) > 0;
    }

    @Transactional(readOnly = true)
    public boolean hasRootRequestTodayForUser(@NonNull Long userId) {
        return requestRepository.countRootRequestsSince(userId, LocalDate.now().atStartOfDay()) > 0;
    }

    private ReRequestState reRequestState(Request request) {
        if (request.getParentRequest() != null) {
            return new ReRequestState(false, "Only one re-request is allowed for a request chain");
        }

        if (requestRepository.existsByParentRequest_RequestId(request.getRequestId())) {
            return new ReRequestState(false, "Only one re-request is allowed for a request chain");
        }

        if (request.getCreatedAt() == null || request.getCreatedAt().plusHours(1).isAfter(LocalDateTime.now())) {
            return new ReRequestState(false, "Please wait for 1 hour before re-requesting this request.");
        }

        return new ReRequestState(true, null);
    }

    private RequestSummaryDTO toRequestSummary(Request request, ReRequestState reRequestState) {
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
                .canReRequest(reRequestState.canReRequest())
                .reRequestBlockedReason(reRequestState.reason())
                .build();
    }

    private record ReRequestState(boolean canReRequest, String reason) {}

    

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
