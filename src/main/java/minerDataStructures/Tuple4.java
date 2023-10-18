package minerDataStructures;

import java.util.Objects;

public class Tuple4<Type> {

    public Type v1;
    public Type v2;
    public Type v3;
    public Type v4;

    public Tuple4(Type v1, Type v2, Type v3, Type v4){
        this.v1 = v1;
        this.v2 = v2;
        this.v3 = v3;
        this.v4 = v4;
    }

    public String valueToString(Type str){
        try{
            String s = str.toString();
            return s;
        }catch (NullPointerException e){
            return "";
        }
    }

    @Override
    public String toString() {
       return "(" + valueToString(v1) + "," + valueToString(v2) + "," + valueToString(v3) + "," + valueToString(v4) + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Tuple4<?> tuple4 = (Tuple4<?>) o;
        return Objects.equals(v1, tuple4.v1) &&
                Objects.equals(v2, tuple4.v2) &&
                Objects.equals(v3, tuple4.v3) &&
                Objects.equals(v4, tuple4.v4);
    }

    @Override
    public int hashCode() {
        return Objects.hash(v1, v2, v3, v4);
    }
}
