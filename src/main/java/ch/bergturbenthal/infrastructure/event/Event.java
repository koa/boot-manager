package ch.bergturbenthal.infrastructure.event;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.MINIMAL_CLASS, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = AssignMacAddressToMachineEvent.class),
        @JsonSubTypes.Type(value = RemoveMacAddressFromMachineEvent.class),
        @JsonSubTypes.Type(value = RemoveMachineEvent.class),
        @JsonSubTypes.Type(value = RemovePatternEvent.class),
        @JsonSubTypes.Type(value = RemoveMachinePatternEvent.class),
        @JsonSubTypes.Type(value = RemoveMachineUUIDEvent.class),
        @JsonSubTypes.Type(value = RenameMachineEvent.class),
        @JsonSubTypes.Type(value = RenamePatternEvent.class),
        @JsonSubTypes.Type(value = ServerRequestEvent.class),
        @JsonSubTypes.Type(value = SetMachineUuidEvent.class),
        @JsonSubTypes.Type(value = UpdateMachinePatternEvent.class),
        @JsonSubTypes.Type(value = UpdatePatternEvent.class),
        @JsonSubTypes.Type(value = RemoveMachinePatternEvent.class) })
public interface Event {

}
