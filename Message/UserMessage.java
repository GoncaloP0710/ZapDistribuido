package Message;

import java.math.BigInteger;
import java.time.LocalDateTime;

import dtos.NodeDTO;
import dtos.UserDTO;

public class UserMessage extends Message {

    private byte[] message;
    private MessageStatus msgStatus;
    private LocalDateTime time;
    private UserDTO senderDTO;
    private UserDTO receiverDTO; // Only known once the message is received

    public UserMessage(MessageType messageType, UserDTO senderDTO, BigInteger reciverHash, byte[] message) {
        super(messageType, reciverHash);
        this.senderDTO = senderDTO;
        this.msgStatus = MessageStatus.SENDING;
        this.time = LocalDateTime.now();
        this.message = message;
    }

    public UserDTO getReceiverDTO(){
        return this.receiverDTO;
    }

    public void setReceiverDTO(UserDTO receiverDTO){
        this.receiverDTO = receiverDTO;
    }

    public byte[] getMessage(){
        return this.message;
    }
}
