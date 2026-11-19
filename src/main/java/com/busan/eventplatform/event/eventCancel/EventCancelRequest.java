package com.busan.eventplatform.event.eventCancel;

import lombok.Data;

public class EventCancelRequest {

    @Data
    public static class SaveDto {
        private String reason;
    }
}
