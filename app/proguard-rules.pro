# PhoneGuard R8 rules.
# Hilt, Room, WorkManager, CameraX and ML Kit ship their own consumer rules.

# Keep enum names: they are stored as text in the Room database and DataStore.
-keepclassmembers enum com.phoneguard.domain.model.** {
    <fields>;
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
