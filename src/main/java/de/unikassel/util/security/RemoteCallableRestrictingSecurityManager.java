package de.unikassel.util.security;

import de.unikassel.util.serialization.RemoteCallable;

import java.security.Permission;
import java.security.PermissionCollection;

public class RemoteCallableRestrictingSecurityManager extends SecurityManager {

    private static final Class<?> REMOTE_CALLABLE_CLASS = RemoteCallable.class;
    private static final Class<?> THIS = RemoteCallableRestrictingSecurityManager.class;

    private final SecurityManager baseSecurityManger;
    private final PermissionCollection remoteCallablePermissions;

    private RemoteCallableRestrictingSecurityManager(
            SecurityManager baseSecurityManger,
            PermissionCollection remoteCallablePermissions
    ) {
        this.baseSecurityManger = baseSecurityManger;
        this.remoteCallablePermissions = remoteCallablePermissions;
    }

    @Override
    public void checkPermission(Permission permission) {
        Class<?>[] classContext = this.getClassContext();

        // Entry 0 will always be RemoteCallableRestrictingSecurityManager -> skip it
        for (int i = 1; i < classContext.length; i++) {
            Class<?> clazz = classContext[i];
            if (REMOTE_CALLABLE_CLASS.isAssignableFrom(clazz)) {
                // We are executing code from a RemoteCallable -> check permissions
                if (this.remoteCallablePermissions.implies(permission)) {
                    break;
                } else {
                    System.err.println("Aha: " + permission);
                    throw new SecurityException(
                            String.format("Permission %s is not set to be granted to RemoteCallable-code",
                                    permission)
                    );
                }
            }
            if (THIS == clazz) {
                // We somehow started a recursion -> prevent endless recursion
                return;
            }
        }

        if (this.baseSecurityManger != null) {
            // Always check the "normal" SecurityManager too
            this.baseSecurityManger.checkPermission(permission);
        }
    }

    // ----static methods-----

    private static RemoteCallableRestrictingSecurityManager installed = null;

    public static void install(PermissionCollection remoteCallablePermissions) {
        if (RemoteCallableRestrictingSecurityManager.installed != null) {
            throw new IllegalStateException("A RemoteCallableRestrictingSecurityManager has been already installed");
        }

        RemoteCallableRestrictingSecurityManager secManager = new RemoteCallableRestrictingSecurityManager(
                System.getSecurityManager(),
                remoteCallablePermissions
        );
        System.setSecurityManager(secManager);
        RemoteCallableRestrictingSecurityManager.installed = secManager;
    }

    public static void uninstall() {
        if (RemoteCallableRestrictingSecurityManager.installed == null) {
            throw new NullPointerException("No RemoteCallableRestrictingSecurityManager was installed");
        }
        System.setSecurityManager(RemoteCallableRestrictingSecurityManager.installed.baseSecurityManger);
        RemoteCallableRestrictingSecurityManager.installed = null;
    }
}
