package ch.bergturbenthal.infrastructure.event;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.MINIMAL_CLASS, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({ @JsonSubTypes.Type(value = DefaultPatternScope.class), @JsonSubTypes.Type(value = OnebootPatternScope.class) })
public interface PatternScope {

}
