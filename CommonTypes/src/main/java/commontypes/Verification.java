package commontypes;

import java.io.Serializable;

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

}
