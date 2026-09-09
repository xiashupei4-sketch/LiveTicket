package com.liveticket.ticket.controller;

import com.liveticket.common.api.ApiResponse;
import com.liveticket.ticket.service.TicketService;
import com.liveticket.ticket.vo.TicketSkuVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Tickets", description = "票档")
@RestController
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;

    @Operation(summary = "演出票档列表")
    @GetMapping("/api/events/{eventId}/tickets")
    public ApiResponse<List<TicketSkuVO>> listByEvent(@PathVariable Long eventId) {
        return ApiResponse.ok(ticketService.listByEventId(eventId));
    }
}
