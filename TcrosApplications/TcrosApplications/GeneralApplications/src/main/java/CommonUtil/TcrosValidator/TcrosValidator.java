package CommonUtil.TcrosValidator;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class TcrosValidator {
    private TcrosValidator(){}
    private static final Validator validator;

    static {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }
    public static <T> List<String> validate(T message){
        Set<ConstraintViolation<T>> violations = validator.validate(message);
        List<String> causeList = new ArrayList<>();
        if (!violations.isEmpty()) {
            for (ConstraintViolation<T> violation : violations) {
                causeList.add(violation.getRootBeanClass().toString() + "." + violation.getPropertyPath() + " " + violation.getMessage());
            }
        }
        return causeList;
    }
}
