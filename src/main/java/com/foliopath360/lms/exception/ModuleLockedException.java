package com.foliopath360.lms.exception;

/**
 * Thrown when a student tries to reach a module they have not unlocked yet,
 * i.e. a preceding module's mock test has not been passed.
 * Maps to HTTP 403 by {@link GlobalExceptionHandler}.
 */
public class ModuleLockedException extends RuntimeException {

    private final java.util.UUID moduleId;
    private final String moduleName;

    public ModuleLockedException(
            java.util.UUID moduleId, String moduleName, String message) {
        super(message);
        this.moduleId = moduleId;
        this.moduleName = moduleName;
    }

    public java.util.UUID getModuleId() {
        return moduleId;
    }

    public String getModuleName() {
        return moduleName;
    }
}
