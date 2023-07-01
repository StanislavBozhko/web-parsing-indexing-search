package searchengine.dto;

import lombok.*;
import lombok.experimental.SuperBuilder;

@Getter
public class ResponseBoolean {

    boolean result;

    public ResponseBoolean(boolean result) {
        this.result = result;
    }
}
