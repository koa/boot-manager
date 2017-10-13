package ch.bergturbenthal.infrastructure.service.impl;

import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.UUID;

import javax.annotation.PreDestroy;
import javax.transaction.Transactional;

import org.springframework.stereotype.Service;

import ch.bergturbenthal.infrastructure.event.AssignMacAddressToMachineEvent;
import ch.bergturbenthal.infrastructure.event.DefaultPatternScope;
import ch.bergturbenthal.infrastructure.event.Event;
import ch.bergturbenthal.infrastructure.event.OnebootPatternScope;
import ch.bergturbenthal.infrastructure.event.PatternScope;
import ch.bergturbenthal.infrastructure.event.RemoveMacAddressFromMachineEvent;
import ch.bergturbenthal.infrastructure.event.RemoveMachineEvent;
import ch.bergturbenthal.infrastructure.event.RemoveMachinePatternEvent;
import ch.bergturbenthal.infrastructure.event.RemoveMachineUUIDEvent;
import ch.bergturbenthal.infrastructure.event.RemovePatternEvent;
import ch.bergturbenthal.infrastructure.event.RenameMachineEvent;
import ch.bergturbenthal.infrastructure.event.RenamePatternEvent;
import ch.bergturbenthal.infrastructure.event.ServerRequestEvent;
import ch.bergturbenthal.infrastructure.event.SetMachineUuidEvent;
import ch.bergturbenthal.infrastructure.event.UpdateMachinePatternEvent;
import ch.bergturbenthal.infrastructure.event.UpdatePatternEvent;
import ch.bergturbenthal.infrastructure.model.MacAddress;
import ch.bergturbenthal.infrastructure.service.LogService;
import ch.bergturbenthal.infrastructure.service.MachineService;
import lombok.Builder;
import lombok.Data;
import lombok.Value;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;

@Service
public class DefaultStateService implements MachineService {

    @Data
    private static class MachineData {
        private Optional<UUID>                                          machineId         = Optional.empty();
        private final Collection<MacAddress>                            knownMacAddresses = new HashSet<>();
        private final Map<UUID, PatternEntry>                           assignedPatterns  = new HashMap<>();
        private final SortedMap<Instant, Optional<RequestHistoryEntry>> requestHistory    = new TreeMap<>();
        private Optional<UUID>                                          oneShotPattern    = Optional.empty();
    }

    @Value
    @Builder(toBuilder = true)
    private static class PatternEntry {
        private PatternScope scope;
        private String       patternName;
    }

    @Value
    private static class RequestHistoryEntry {
        private UUID   patternEntry;
        private String patternName;
        private String patternData;
    }

    private final Disposable                        logSubscription;
    private final Object                            updateLock           = new Object();
    private final Map<String, MachineData>          knownNamedMachines   = new HashMap<>();
    private final Map<String, String>               availablePatterns    = new HashMap<>();
    private final Map<UUID, Collection<MacAddress>> knownMachineUuidData = new HashMap<>();
    private final Set<MacAddress>                   knownMacAddressList  = new HashSet<>();
    private final Map<UUID, Runnable>               pendingListeners     = new HashMap<>();
    private final LogService                        logService;

    public DefaultStateService(final LogService logService) {
        this.logService = logService;
        logSubscription = logService.streamEvents().subscribe(eventData -> {
            final Event event = eventData.getEvent();
            synchronized (updateLock) {
                if (event instanceof AssignMacAddressToMachineEvent) {
                    final AssignMacAddressToMachineEvent assignMacAddressToMachineEvent = (AssignMacAddressToMachineEvent) event;
                    final MacAddress macAddress = assignMacAddressToMachineEvent.getMacAddress();
                    final String machineName = assignMacAddressToMachineEvent.getMachineName();
                    knownNamedMachines.computeIfAbsent(machineName, k -> new MachineData()).getKnownMacAddresses().add(macAddress);
                } else if (event instanceof RemoveMacAddressFromMachineEvent) {
                    final RemoveMacAddressFromMachineEvent removeMacAddressFromMachineEvent = (RemoveMacAddressFromMachineEvent) event;
                    final String machineName = removeMacAddressFromMachineEvent.getMachineName();
                    final MacAddress macAddress = removeMacAddressFromMachineEvent.getMacAddress();
                    final MachineData machineData = knownNamedMachines.get(machineName);
                    if (machineData != null) {
                        machineData.getKnownMacAddresses().remove(macAddress);
                    }
                } else if (event instanceof RemoveMachineEvent) {
                    knownNamedMachines.remove(((RemoveMachineEvent) event).getMachineName());
                } else if (event instanceof RemoveMachineUUIDEvent) {
                    final String machineName = ((RemoveMachineUUIDEvent) event).getMachineName();
                    final MachineData machineData = knownNamedMachines.get(machineName);
                    if (machineData != null) {
                        machineData.setMachineId(Optional.empty());
                    }
                } else if (event instanceof RenameMachineEvent) {
                    final RenameMachineEvent renameMachineEvent = (RenameMachineEvent) event;
                    final String oldMachineName = renameMachineEvent.getOldMachineName();
                    final String newMachineName = renameMachineEvent.getNewMachineName();
                    final MachineData machineData = knownNamedMachines.get(oldMachineName);
                    if (machineData != null) {
                        knownNamedMachines.put(newMachineName, machineData);
                    }
                } else if (event instanceof RemovePatternEvent) {
                    final RemovePatternEvent removePatternEvent = (RemovePatternEvent) event;
                    final String patternName = removePatternEvent.getPatternName();
                    for (final MachineData machine : knownNamedMachines.values()) {
                        for (final Iterator<PatternEntry> iterator = machine.getAssignedPatterns().values().iterator(); iterator.hasNext();) {
                            final PatternEntry patternData = iterator.next();
                            if (patternData.getPatternName().equals(patternName)) {
                                iterator.remove();
                            }
                        }
                    }
                    availablePatterns.remove(patternName);
                } else if (event instanceof RenamePatternEvent) {
                    final RenamePatternEvent renamePatternEvent = (RenamePatternEvent) event;
                    final String oldPatternName = renamePatternEvent.getOldPatternName();
                    final String newPatternName = renamePatternEvent.getNewPatternName();
                    final String patternContent = availablePatterns.remove(oldPatternName);
                    if (patternContent != null) {
                        availablePatterns.put(newPatternName, patternContent);
                    }
                    for (final MachineData machine : knownNamedMachines.values()) {
                        for (final Entry<UUID, PatternEntry> patternEntry : machine.getAssignedPatterns().entrySet()) {
                            final PatternEntry data = patternEntry.getValue();
                            if (data.getPatternName().equals(oldPatternName)) {
                                patternEntry.setValue(data.toBuilder().patternName(newPatternName).build());
                            }
                        }
                    }
                } else if (event instanceof RemoveMachinePatternEvent) {
                    final UUID patternId = ((RemoveMachinePatternEvent) event).getPatternId();
                    for (final MachineData machine : knownNamedMachines.values()) {
                        machine.getAssignedPatterns().remove(patternId);
                    }
                } else if (event instanceof ServerRequestEvent) {
                    final ServerRequestEvent serverRequestEvent = (ServerRequestEvent) event;
                    final MacAddress macAddress = serverRequestEvent.getMacAddress();
                    final Optional<UUID> machineUuid = serverRequestEvent.getMachineUuid();
                    knownMacAddressList.add(macAddress);
                    if (machineUuid.isPresent()) {
                        knownMachineUuidData.computeIfAbsent(machineUuid.get(), k -> new HashSet<>()).add(macAddress);
                    }
                    identifyMachine(machineUuid, macAddress).ifPresent(m -> {
                        final Optional<UUID> currentPattern = findCurrentPattern(m);
                        m.getRequestHistory().put(eventData.getTimestamp(), currentPattern.map(id -> {
                            final String patternName = m.getAssignedPatterns().get(id).getPatternName();
                            final String patternData = availablePatterns.get(patternName);
                            return new RequestHistoryEntry(id, patternName, patternData);
                        }));
                        currentPattern.ifPresent(id -> {
                            final PatternEntry patternEntry = m.getAssignedPatterns().get(id);
                            if (patternEntry != null && patternEntry.getScope() instanceof OnebootPatternScope) {
                                m.getAssignedPatterns().remove(id);
                            }
                        });
                    });
                } else if (event instanceof SetMachineUuidEvent) {
                    final String machineName = ((SetMachineUuidEvent) event).getMachineName();
                    final UUID machineUuid = ((SetMachineUuidEvent) event).getMachineUuid();
                    final Optional<Collection<MacAddress>> macadressesOfMachine = Optional.ofNullable(knownMachineUuidData.get(machineUuid));
                    for (final MachineData machine : knownNamedMachines.values()) {
                        if (machine.getMachineId().isPresent() && machine.getMachineId().get().equals(machineUuid)) {
                            machine.setMachineId(Optional.empty());
                            macadressesOfMachine.ifPresent(macs -> machine.getKnownMacAddresses().removeAll(macs));
                        }
                    }
                    final MachineData newMachineData = knownNamedMachines.computeIfAbsent(machineName, k -> new MachineData());
                    newMachineData.setMachineId(Optional.of(machineUuid));
                    macadressesOfMachine.ifPresent(macs -> newMachineData.getKnownMacAddresses().addAll(macs));
                } else if (event instanceof UpdateMachinePatternEvent) {
                    final UpdateMachinePatternEvent machinePatternEvent = (UpdateMachinePatternEvent) event;
                    final String machineName = machinePatternEvent.getMachineName();
                    final UUID patternId = machinePatternEvent.getPatternId();
                    final PatternScope scope = machinePatternEvent.getScope();
                    final String machinePattern = machinePatternEvent.getMachinePattern();
                    final MachineData machineData = knownNamedMachines.computeIfAbsent(machineName, k -> new MachineData());
                    machineData.getAssignedPatterns().put(patternId, PatternEntry.builder().scope(scope).patternName(machinePattern).build());
                } else if (event instanceof UpdatePatternEvent) {
                    final String patternName = ((UpdatePatternEvent) event).getPatternName();
                    final String patternContent = ((UpdatePatternEvent) event).getPatternContent();
                    availablePatterns.put(patternName, patternContent);
                }
            }
            synchronized (pendingListeners) {
                for (final Runnable listener : pendingListeners.values()) {
                    listener.run();
                }
            }
        });
    }

    private Optional<UUID> findCurrentPattern(final MachineData m) {
        final Map<UUID, PatternEntry> assignedPatterns = m.getAssignedPatterns();
        Optional<UUID> defaultEntry = Optional.empty();
        for (final Entry<UUID, PatternEntry> entry : assignedPatterns.entrySet()) {
            final PatternScope scope = entry.getValue().getScope();
            if (scope instanceof OnebootPatternScope) {
                return Optional.of(entry.getKey());
            } else if (scope instanceof DefaultPatternScope) {
                defaultEntry = Optional.of(entry.getKey());
            } else {
                throw new IllegalStateException("unsupported scope " + scope);
            }
        }
        return defaultEntry;
    }

    private Optional<MachineData> identifyMachine(final Optional<UUID> machineUuid, final MacAddress macAddress) {
        return Optional.ofNullable(Mono.justOrEmpty(machineUuid)
                .flatMap(uuid -> Mono.justOrEmpty(
                        knownNamedMachines.values().stream().filter(m -> m.getMachineId().isPresent() && m.getMachineId().equals(uuid)).findFirst()))
                .switchIfEmpty(
                        Mono.justOrEmpty(knownNamedMachines.values().stream().filter(m -> m.getKnownMacAddresses().contains(macAddress)).findFirst()))
                .block());
    }

    @Override
    public Set<MacAddress> listFreeMacs() {
        synchronized (updateLock) {
            final HashSet<MacAddress> ret = new HashSet<>(knownMacAddressList);
            for (final MachineData machine : knownNamedMachines.values()) {
                ret.removeAll(machine.getKnownMacAddresses());
            }
            return ret;
        }
    }

    @Override
    @Transactional
    public Optional<String> processBoot(final Optional<UUID> uuid, final MacAddress macAddress) {
        synchronized (updateLock) {
            final Optional<MachineData> machine = identifyMachine(uuid, macAddress);
            if (machine.isPresent()) {
                final MachineData machineData = machine.get();
                final Optional<UUID> currentPattern = findCurrentPattern(machineData);
                final Optional<String> patternName = currentPattern.map(k -> machineData.getAssignedPatterns().get(k).getPatternName());
                logService.appendEvent(new ServerRequestEvent(macAddress, uuid, patternName));
                return patternName.flatMap(p -> Optional.ofNullable(availablePatterns.get(p)));
            }
            logService.appendEvent(new ServerRequestEvent(macAddress, uuid, Optional.empty()));
            return Optional.empty();
        }
    }

    @Override
    public Disposable registerForUpdates(final Runnable run) {
        synchronized (pendingListeners) {
            final UUID listernid = UUID.randomUUID();
            pendingListeners.put(listernid, run);
            return () -> {
                synchronized (pendingListeners) {
                    pendingListeners.remove(listernid);
                }
            };

        }
    }

    @PreDestroy
    public void unsubscribe() {
        logSubscription.dispose();
    }
}
