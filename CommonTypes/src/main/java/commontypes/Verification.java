package commontypes;

import java.io.Serializable;
import java.util.Objects;

public class Verification implements Serializable {
    private int receiverWts;

    private String receiverSignature;

    public int getReceiverWts() {
        return receiverWts;
    }

    public void setReceiverWts(int receiverWts) {
        this.receiverWts = receiverWts;
    }

    public String getReceiverSignature() {
        return receiverSignature;
    }

    public void setReceiverSignature(String receiverSignature) {
        this.receiverSignature = receiverSignature;
    }

    public Verification(int wts, String receiverSignature){
        this.receiverSignature = receiverSignature;
        this.receiverWts = wts;
    }

    @Override
    public boolean equals(Object obj) {

        // check if the reference is the same
        if(obj == this)
            return true;

        // check if its the same class
        if( obj == null || obj.getClass()!= getClass())
            return false;

        return Objects.equals(this.getReceiverSignature(), ((Verification) obj).getReceiverSignature()) &&
                Objects.equals(this.getReceiverWts(), ((Verification) obj).getReceiverWts());
    }
}
