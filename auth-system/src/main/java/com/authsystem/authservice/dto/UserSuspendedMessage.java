package com.authsystem.authservice.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class UserSuspendedMessage extends BaseEvent {

    private Long userId;
    private int suspendedDays;
    private String suspendedBy;
}
