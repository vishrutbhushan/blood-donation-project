package com.hemo.backend.service;

import com.hemo.backend.dto.DispatchResultDTO;
import com.hemo.backend.dto.DonorSearchResponseDTO;
import com.hemo.backend.dto.RequestDTO;
import com.hemo.backend.dto.RequestSummaryDTO;
import com.hemo.backend.dto.ResponseRecordDTO;
import com.hemo.backend.entity.Request;
import com.hemo.backend.entity.Search;
import com.hemo.backend.entity.Response;
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

    @Transactional
    public RequestSummaryDTO createRequest(@NonNull Long searchId, RequestDTO dto) {
        Search search = searchRepository.findById(searchId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Search not found"));

        Long userId = search.getUser().getUserId();
        if (hasActiveRequest(userId)) {
            throw new AppException(HttpStatus.CONFLICT, "Active request exists");
        }

        int initialBatch = Math.min(20, Math.max(dto.getMatchedCount(), 0));

        Request request = new Request();
        request.setSearch(search);
        request.setBloodGroup(dto.getBloodGroup());
        request.setComponent(dto.getComponent());
        request.setUnitsRequested(dto.getUnitsRequested());
        request.setNumberOfDonorsContacted(initialBatch);
        request.setStatus("ACTIVE");

        Request saved = requestRepository.save(request);
        int sent = dispatchFromOffset(saved, 0, initialBatch);
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

        Request next = new Request();
        next.setSearch(oldRequest.getSearch());
        next.setBloodGroup(oldRequest.getBloodGroup());
        next.setComponent(oldRequest.getComponent());
        next.setUnitsRequested(oldRequest.getUnitsRequested());
        next.setNumberOfDonorsContacted(20);
        next.setStatus("ACTIVE");
        next.setParentRequest(oldRequest);

        Request saved = requestRepository.save(next);
        int sent = dispatchFromOffset(saved, 0, 20);
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
        int sent = dispatchFromOffset(request, current, 20);
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
        return responseRepository.findByUserId(userId)
                .stream()
                .map(this::toResponseRecord)
                .toList();
    }

    @Transactional(readOnly = true)
    public boolean hasActiveRequest(@NonNull Long userId) {
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

    private ResponseRecordDTO toResponseRecord(Response response) {
        return ResponseRecordDTO.builder()
                .responseId(response.getResponseId())
                .requestId(response.getRequest().getRequestId())
                .donorId(response.getDonorId())
                .donorName(response.getDonorName())
                .abhaId(response.getAbhaId())
                .phoneNumber(response.getPhoneNumber())
                .bloodGroup(response.getBloodGroup())
                .location(response.getLocation())
                .responseStatus(response.getResponseStatus())
                .respondedAt(response.getRespondedAt())
                .build();
    }

    private int dispatchFromOffset(Request request, int offset, int batchSize) {
        if (batchSize <= 0) {
            return 0;
        }

        DonorSearchResponseDTO donorBatch = donorSearchService.searchCompatibleDonors(
            request.getBloodGroup(),
            request.getSearch().getHospitalPincode(),
            offset,
            batchSize
        );

        int sent = 0;
        for (int i = 0; i < donorBatch.getDonors().size(); i++) {
            sent++;
        }

        return sent;
    }
}
