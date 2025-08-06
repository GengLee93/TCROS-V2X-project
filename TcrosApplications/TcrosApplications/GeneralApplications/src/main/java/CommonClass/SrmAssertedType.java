package CommonClass;

public enum SrmAssertedType {
    REQUESTED,UPDATE,GRANTED,REJECT, PASSED;

    @Override
    public String toString(){
        return this.name();
    }
}
