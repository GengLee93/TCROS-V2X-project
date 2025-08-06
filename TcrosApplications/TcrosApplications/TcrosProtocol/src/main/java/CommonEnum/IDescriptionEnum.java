package CommonEnum;

import com.fasterxml.jackson.annotation.JsonValue;
public interface IDescriptionEnum<T> {
    T getId();
    String getDescription();

    @JsonValue
    default T getJsonValue() {
        return getId();
    }
}
