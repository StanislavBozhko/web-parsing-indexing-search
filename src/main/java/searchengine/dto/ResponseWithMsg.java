package searchengine.dto;


import lombok.Getter;

@Getter
public class ResponseWithMsg extends ResponseBoolean {

    private final String error;

    public ResponseWithMsg(boolean result, String error) {
        super(result);
        this.error = error;
    }
}
