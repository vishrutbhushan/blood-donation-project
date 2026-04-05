package com.hemo.backend.service;

import org.springframework.stereotype.Service;
import com.hemo.backend.dto.RequestDTO;
import com.hemo.backend.entity.Request;
import com.hemo.backend.entity.Search;
import com.hemo.backend.exception.ResourceNotFoundException;
import com.hemo.backend.repository.RequestRepository;
import com.hemo.backend.repository.SearchRepository;
import lombok.RequiredArgsConstructor;
import java.util.List;
import java.util.Map;



@Service
@RequiredArgsConstructor
public class RequestService {

    private final RequestRepository requestRepository;
    private final SearchRepository searchRepository;

    public Request createRequest(Long searchId, RequestDTO dto) {

        Search search = searchRepository.findById(searchId)
                .orElseThrow(() -> new ResourceNotFoundException("Search not found"));

        Request request = new Request();
        request.setSearch(search);
        request.setBloodGroup(dto.getBloodGroup());
        request.setComponent(dto.getComponent());
        request.setUnitsRequested(dto.getUnitsRequested());

        return requestRepository.save(request);
    }

    public Request retryRequest(Long requestId) {

        Request old = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Request not found"));

        Request newReq = new Request();
        newReq.setSearch(old.getSearch());
        newReq.setBloodGroup(old.getBloodGroup());
        newReq.setComponent(old.getComponent());
        newReq.setParentRequest(old);

        return requestRepository.save(newReq);
    }

    public List<Map<String, Object>> getUserRequestHistory(Long userId) {

    return requestRepository.findBySearchUserUserId(userId)
            .stream()
            .map(r -> Map.<String, Object>of(
                    "request_id", r.getRequestId(),
                    "blood_group", r.getBloodGroup(),
                    "component", r.getComponent(),
                    "status", r.getStatus()
            ))
            .toList();
}

}
