package org.zstack.core.cloudbus;

import org.zstack.header.Component;
import org.zstack.header.Service;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.exception.CloudConfigureFailException;
import org.zstack.header.message.Event;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.message.NeedReplyMessage;

import java.util.List;

public interface CloudBus extends Component {
    void send(Message msg);
    
    <T extends Message> void send(List<T> msgs);
    
    void send(NeedReplyMessage msg, CloudBusCallBack callback);

    void send(List<? extends NeedReplyMessage> msgs, CloudBusListCallBack callBack);

    void send(List<? extends NeedReplyMessage> msgs, int parallelLevel, CloudBusListCallBack callBack);

    void send(List<? extends NeedReplyMessage> msgs, int parallelLevel, CloudBusSteppingCallback callback);

    void route(List<Message> msgs);
    
    void route(Message msg);
    
    void reply(Message request, MessageReply reply);
    
    void publish(List<Event> events);
    
    void publish(Event event);
    
    MessageReply call(NeedReplyMessage msg);
    
    <T extends NeedReplyMessage> List<MessageReply> call(List<T> msg);
    
    void registerService(Service serv) throws CloudConfigureFailException;
    
    void unregisterService(Service serv);
    
    EventSubscriberReceipt subscribeEvent(CloudBusEventListener listener, Event...events);
    
    void dealWithUnknownMessage(Message msg);
    
    void replyErrorByMessageType(Message msg, Exception e);
    
    void replyErrorByMessageType(Message msg, String err);
    
    void replyErrorByMessageType(Message msg, ErrorCode err);
    
    void logExceptionWithMessageDump(Message msg, Throwable e);
    
    String makeLocalServiceId(String serviceId);

    void makeLocalServiceId(Message msg, String serviceId);

    String makeServiceIdByManagementNodeId(String serviceId, String managementNodeId);

    void makeServiceIdByManagementNodeId(Message msg, String serviceId, String managementNodeId);

    String makeTargetServiceIdByResourceUuid(String serviceId, String resourceUuid);

    void makeTargetServiceIdByResourceUuid(Message msg, String serviceId, String resourceUuid);
}
