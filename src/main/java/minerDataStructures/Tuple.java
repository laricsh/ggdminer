package minerDataStructures;


import java.util.Objects;

public class Tuple<X, Y> implements Comparable<Tuple<X, Y>>{
        public X x;
        public Y y;

        public Tuple(X x, Y y) {
            this.x = x;
            this.y = y;
        }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Tuple<?, ?> tuple = (Tuple<?, ?>) o;
        return Objects.equals(x, tuple.x) &&
                Objects.equals(y, tuple.y);
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }

    @Override
    public String toString(){
            return "x:"+this.x + " y:"+this.y;
    }


    @Override
    public int compareTo(Tuple<X, Y> xyTuple) {
       return this.x.toString().compareTo(xyTuple.x.toString());
    }
}
