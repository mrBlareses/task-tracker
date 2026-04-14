package org.mushroom.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.mushroom.entity.enums.OutboxEmailType;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailRequestedEvent {

    private OutboxEmailType type;
    private Map<String, Object> payload;
}