package ch.bergturbenthal.infrastructure.event;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.MINIMAL_CLASS, include = JsonTypeInfo.As.PROPERTY, property = "bootType")
@JsonSubTypes({ @JsonSubTypes.Type(value = PatternBootAction.class), @JsonSubTypes.Type(value = RedirectBootAction.class) })
public interface BootAction {

}
