package com.busan.eventplatform.event.event_cancel;

import lombok.Data;

public class EventCancelRequest {

    @Data
    public static class SaveDto {
        private String reason;
    }
}
