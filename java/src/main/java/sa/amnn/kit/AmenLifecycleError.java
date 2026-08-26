package sa.amnn.kit;

/** Thrown locally, before any HTTP call, when an action is not valid for the deal's status. */
public class AmenLifecycleError extends IllegalStateException { public AmenLifecycleError(String m) { super(m); } }
