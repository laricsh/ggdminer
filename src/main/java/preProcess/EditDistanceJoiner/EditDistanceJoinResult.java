package preProcess.EditDistanceJoiner;


/***
This code belongs to the Tsinghua Database Group
Under the project available at: https://github.com/lispc/EditDistanceClusterer
 */

public class EditDistanceJoinResult implements Comparable<EditDistanceJoinResult> {
    public String src;
    public String dst;
    public int similarity;
    @Override
    public int hashCode() {
        return src.hashCode() * dst.hashCode();
    }
    @Override
    public boolean equals(Object obj) {
        if (obj == null ) {
            return false;
        }
        if (!(obj instanceof EditDistanceJoinResult)) {
            return false;
        }
        EditDistanceJoinResult other = (EditDistanceJoinResult)obj;
        if (other == this) {
            return true;
        }
        return src.equals(other.src) && dst.equals(other.dst);
    }

    @Override
    public int compareTo(EditDistanceJoinResult editDistanceJoinResult) {
        if(similarity == editDistanceJoinResult.similarity){
            return 0;
        }else if(similarity > editDistanceJoinResult.similarity){
            return 1;
        }else return -1;
    }


}